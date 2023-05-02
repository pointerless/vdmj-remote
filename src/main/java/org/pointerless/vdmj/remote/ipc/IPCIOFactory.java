package org.pointerless.vdmj.remote.ipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class IPCIOFactory {

	private static IPCIO ipcio = null;

	public static IPCIO getIPCIO(){
		if(ipcio == null){
			throw new RuntimeException("IPCIO not created for default get");
		}
		return ipcio;
	}

	protected static void closed(IPCIO closedIPCIO){
		if(ipcio == closedIPCIO){
			ipcio = null;
		}
	}

	public static boolean available(){
		return ipcio != null;
	}

	public static IPCIO createIPCIO(BufferedReader reader, PrintWriter writer){
		if(ipcio != null){
			throw new RuntimeException("IPCIO already created");
		}
		ipcio = new IPCIO(reader, writer);
		return ipcio;
	}

	public static IPCIO createSocketIPCIO(Socket socket) throws IOException {
		if(ipcio != null){
			throw new RuntimeException("IPCIO already created");
		}
		ipcio = new SocketIPCIO(socket, new BufferedReader(new InputStreamReader(socket.getInputStream())),
				new PrintWriter(socket.getOutputStream()));
		return ipcio;
	}

}
