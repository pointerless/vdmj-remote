package org.pointerless.vdmj.remote.ipc;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketIPCIO extends IPCIO implements AutoCloseable{

	Socket socket;

	protected SocketIPCIO(Socket socket, BufferedReader reader, PrintWriter writer){
		super(reader, writer);
		this.socket = socket;
	}

	@Override
	public void close() throws Exception {
		super.close();
		this.socket.shutdownInput();
		this.socket.shutdownOutput();
		this.socket.close();
		System.out.println("Closed socket");
	}
}
