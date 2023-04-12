package org.pointerless.vdmj.remote;

import com.beust.jcommander.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import org.pointerless.vdmj.remote.ipc.IPCIOFactory;
import org.pointerless.vdmj.remote.rest.MainOutputSession;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * An Extended Version of the VDMJ Main process to allow RPC
 * interactions
 */
@Slf4j
public class RemoteMain {
	private final VDMJHandler handler;

	private final Thread handlerThread;

	private final MainOutputSession mainOutputSession;

	@Data
	public static class Args {

		@Parameter(names = {"--sourcePath", "-s"}, description = "Source as a path, dir/files", required = true)
		private String sourcePath;

		@Parameter(names = {"--type", "-t"}, description = "Source type ([vdmrt, vdmsl, vdmpp])", required = true)
		private String sourceType;

		@Parameter(names = {"--port", "-p"}, description = "Port to bind on, if 0 random port will be used")
		private int serverPort = 0;

		@Parameter(names = {"--ipcAddress", "-i"}, description = "Address to connect to for JSON communication with" +
				" parent process, must match '<hostname>:<port>'")
		private String ipcAddress;

		//@Parameter(names = {"--heartbeat"}, description = "Socket to connect heartbeat client to, if 0 will be random")
		//private int heartbeatPort = 0;

		@Parameter(names = {"--help", "-h"}, description = "Print this help dialogue", help = true)
		private boolean help = false;

	}

	public RemoteMain(VDMJHandler handler, int serverPort) throws IOException {
		this.handler = handler;
		handlerThread = new Thread(handler, "VDMJ-Handler-Thread");
		handlerThread.setDaemon(true);
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

		if(pathToSource == null){
			System.err.println("Need either source or source path");
			commander.usage();
			return;
		}

		String[] vdmjArgs = {
				"-i", "-annotations", /*"-exceptions", ??*/ "-"+args.sourceType, pathToSource
		};

		if(args.ipcAddress != null){
			if(args.ipcAddress.isEmpty()){
				System.err.println("IPC Address must not be empty if being used");
				commander.usage();
				System.exit(-1);
			}
			if(args.ipcAddress.split(":").length != 2){
				System.err.println("IPC Address must match '<hostname>:<port>'");
				commander.usage();
				System.exit(-1);
			}

			Socket socket = new Socket(args.ipcAddress.split(":")[0],
					Integer.parseInt(args.ipcAddress.split(":")[1]));

			if(socket.isConnected()){
				IPCIOFactory.createSocketIPCIO(socket);
			}else{
				System.err.println("IPC could not connect");
				System.exit(-1);
			}
		}else{
			IPCIOFactory.createIPCIO(new BufferedReader(new InputStreamReader(System.in)), new PrintWriter(System.out));
		}


		VDMJHandler handler = new VDMJHandler(vdmjArgs);

		if(args.serverPort == 0){
			args.serverPort = MainOutputSession.getRandomPort();
		}

		RemoteMain main = new RemoteMain(handler, args.serverPort);
	}

}
