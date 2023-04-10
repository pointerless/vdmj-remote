package org.pointerless.vdmj.remote;

import com.beust.ah.A;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Heartbeat server, sends back valid requests with the same id
 */
@Slf4j
public class Heartbeat implements Runnable{
	private static final ObjectMapper mapper = SerializationHelper.getObjectMapper();
	public static int threadPoolSize = 8;

	private final ServerSocket server;
	private final int port;
	private final ExecutorService pool;

	@Data
	public static final class HeartbeatRequest {

		public HeartbeatRequest(){
			this.requestId = UUID.randomUUID();
		}

		public HeartbeatRequest(UUID requestId){
			this.requestId = requestId;
		}

		private UUID requestId;
	}

	@Data
	public static final class HeartbeatResponse {

		public HeartbeatResponse(){
			this.requestId = UUID.randomUUID();
		}

		public HeartbeatResponse(UUID requestId){
			this.requestId = requestId;
		}

		private UUID requestId;
		private boolean alive = true;
	}

	@Slf4j
	private static final class HeartbeatHandler implements Runnable {
		private final Socket socket;

		private final PrintWriter out;
		private final BufferedReader in;

		private boolean running = true;

		public HeartbeatHandler(Socket socket) throws IOException {
			this.socket = socket;
			this.out = new PrintWriter(socket.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}

		@Override
		public void run() {
			while(!Thread.currentThread().isInterrupted() && !socket.isClosed() && running){
				try {
					String line = in.readLine();
					if(line == null) continue;
					HeartbeatRequest reqRes = mapper.readValue(line, HeartbeatRequest.class);
					out.println(mapper.writeValueAsString(new HeartbeatResponse(reqRes.requestId)));
					out.flush();
				} catch (IOException e) {
					if(running) {
						log.debug("Could not (de)serialize heartbeat request: "+e.getMessage());
						out.println("false");
						out.flush();
					}
				}
			}
		}
	}

	public Heartbeat(int port) throws IOException {
		server = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
		this.pool = Executors.newFixedThreadPool(threadPoolSize);
		this.port = server.getLocalPort();
		log.info("Heartbeat server created on address: "+server.getLocalSocketAddress().toString());
	}

	@Override
	public void run() {
		log.info("Heartbeat server running on address: "+server.getLocalSocketAddress().toString());
		while(!Thread.currentThread().isInterrupted()){
			Socket socket = null;
			try {
				socket = server.accept();
				log.info("Heartbeat server got connection from: "+socket.getRemoteSocketAddress().toString());
				pool.execute(new HeartbeatHandler(socket));
			}catch (RejectedExecutionException e){
				if(socket != null) {
					try {
						socket.close();
						log.debug("Closed socket due to: "+e.getMessage());
					} catch (IOException ex) {
						log.error("Socket un-closeable: "+ex.getMessage());
					}
				}
			} catch (IOException e) {
				log.error("Could not accept/handle socket: "+e.getMessage());
				throw new RuntimeException(e);
			}
		}
	}

	public void stop(){
		this.pool.shutdownNow().forEach(runnable -> {
			HeartbeatHandler handler = (HeartbeatHandler) runnable;
			handler.running = false;
		});
	}

	public int getPort() {
		return port;
	}
}
