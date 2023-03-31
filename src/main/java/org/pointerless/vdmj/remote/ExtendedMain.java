package org.pointerless.vdmj.remote;

import com.beust.jcommander.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import org.pointerless.vdmj.remote.gui.MainOutputSession;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * An Extended Version of the VDMJ Main process to allow RPC
 * interactions
 */
@Slf4j
public class ExtendedMain {
	private final VDMJHandler handler;

	private final Thread handlerThread;

	private final MainOutputSession mainOutputSession;

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
		this.handler = handler;
		handlerThread = new Thread(handler);
		handlerThread.start();
		handler.pickupStartupString();

		mainOutputSession = MainOutputSession.mainOutputSession(handler, serverPort);

		mainOutputSession.startSession();
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

		if(args.serverPort == 0){
			args.serverPort = MainOutputSession.getRandomPort();
		}

		ExtendedMain main = new ExtendedMain(handler, args.serverPort);
	}

}
