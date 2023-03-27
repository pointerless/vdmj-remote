package org.pointerless.vdmj.remote;

import com.beust.jcommander.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Data;
import org.pointerless.vdmj.remote.annotations.tc.TCWebGUIAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.*;

import static spark.Spark.*;


/**
 * An Extended Version of the VDMJ Main process to allow RPC
 * interactions
 */
public class ExtendedMain {
	private static final Logger logger = LoggerFactory.getLogger(ExtendedMain.class);
	private final ObjectMapper objectMapper;

	private final VDMJHandler handler;

	private final Thread handlerThread;

	private final Map<Output, GUIOutputSession> outputSessionMap = new HashMap<>();

	private final Set<Output> outputs = new HashSet<>();

	private final int serverPort;

	@Data
	public static class Args {

		@Parameter(names = {"--source", "-s"}, description = "Source as a string")
		private String source;

		@Parameter(names = {"--sourcePath"}, description = "Source as a path")
		private String sourcePath;

		@Parameter(names = {"--type", "-t"}, description = "Source type ([vdmrt, vdmsl, vdmpp])", required = true)
		private String sourceType;

		@Parameter(names = {"--port", "-p"}, description = "Port to bind on, if 0 random port will be used")
		private int serverPort = 0;

		@Parameter(names = {"--help", "-h"}, description = "Print this help dialogue", help = true)
		private boolean help = false;

	}

	public ExtendedMain(VDMJHandler handler, int serverPort) throws IOException {
		this.serverPort = serverPort;
		this.handler = handler;
		handlerThread = new Thread(handler);
		handlerThread.start();
		handler.pickupStartupString();

		TCWebGUIAnnotation.moduleMap.forEach((module, annotations) ->
				annotations.forEach(annotation -> outputs.add(Output.gui(module, annotation))));
		this.objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(Command.class, new Command.CommandSerializer());
		this.objectMapper.registerModule(module);
	}

	public static void main(String[] argv) throws IOException {

		Args args = new Args();
		JCommander commander = null;

		try {
			commander = JCommander.newBuilder()
					.addObject(args)
					.build();
			commander.setProgramName("VDMJ Remote Session");
			commander.parse(argv);
		}catch(ParameterException e){
			if (commander != null) {
				commander.usage();
			}else{
				throw new RuntimeException("Could not parse parameters");
			}
			return;
		}


		if(args.help){
			commander.usage();
			return;
		}

		if(!Objects.equals(args.sourceType, "vdmsl") &&
				!Objects.equals(args.sourceType, "vdmrt") && !Objects.equals(args.sourceType, "vdmpp")){
			System.err.println("Type not in [vdmrt, vdmsl, vdmpp]");
			commander.usage();
			return;
		}

		String pathToSource = args.sourcePath;

		if(args.source != null){
			File sourceFile = Files.createTempFile("source", "vdm").toFile();
			try(PrintStream sourceFileOutput = new PrintStream(sourceFile)){
				sourceFileOutput.print(args.source);
			}catch(Exception e){
				throw new RuntimeException("Could not create file from second arg: '"+args.source+"'");
			}
			pathToSource = sourceFile.getAbsolutePath();
		}else if(pathToSource == null){
			System.err.println("Need either source or source path");
			commander.usage();
			return;
		}

		String[] vdmjArgs = {
				"-i", "-annotations", /*"-exceptions", ??*/ "-"+args.sourceType, pathToSource
		};

		VDMJHandler handler = new VDMJHandler(vdmjArgs);

		ExtendedMain extendedMain = new ExtendedMain(handler, args.serverPort);
		extendedMain.run();

	}

	public static Integer getRandomPort() throws SessionException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}catch (IOException e){
			throw new SessionException("Could not find port to bind on: "+e.getMessage());
		}
	}

	private void run(){
		ipAddress("127.0.0.1");
		port(serverPort);

		staticFileLocation("cli");

		post("/exec", (request, response) -> {
			response.type("application/json");
			Command out = this.handler.runCommand(request.body());
			return objectMapper.writeValueAsString(out);
		});

		get("/outputs", (request, response) -> {
			response.type("application/json");
			return objectMapper.writeValueAsString(outputs);
		});

		post("/startOutput", (request, response) -> {
			try{
				Output output = objectMapper.readValue(request.body(), Output.class);
				if(!outputs.contains(output)){
					throw new SessionException("Could not find output: "+objectMapper.writeValueAsString(output));
				}
				response.type("application/json");
				GUISessionInfo info = new GUISessionInfo(output.getId(), getRandomPort(),
						output.getProperties().get("location"),
						output.getModule()+": "+output.getProperties().get("nickname"));
				GUIOutputSession outputSession = new GUIOutputSession(info, handler);
				outputSessionMap.put(output, outputSession);
				outputSession.run();
				return objectMapper.writeValueAsString(info);
			}catch(JsonProcessingException jsonProcessingException){
				throw new SessionException("Could not process output JSON: "+jsonProcessingException.getMessage());
			}
		});

		get("/startup", (request, response) -> this.handler.getStartupString());

		exception(SessionException.class, (e, request, response) -> {
			response.body(e.getMessage());
			response.status(400);
		});

	}

}
