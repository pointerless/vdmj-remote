package org.pointerless.vdmj.remote.ipc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.text.html.Option;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IPCIO implements AutoCloseable{
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final Thread readingThread;

	private final BufferedReader reader;
	private final PrintWriter writer;

	private final BlockingQueue<IPCInstruction> queue = new LinkedBlockingQueue<>();

	protected IPCIO(BufferedReader reader, PrintWriter writer) {
		this.reader = reader;
		this.writer = writer;
		this.readingThread = new Thread(this::runReading, "IPCIO-Read-Thread");
		this.readingThread.setDaemon(true);
		this.readingThread.start();
	}

	public void write(IPCLog ipcLog){
		synchronized (this.writer){
			try {
				this.writer.println(objectMapper.writeValueAsString(ipcLog));
				this.writer.flush();
			} catch (JsonProcessingException e){
				throw new IllegalArgumentException("Could not write given IPCLog: "+e.getMessage());
			}
		}
	}

	public Optional<IPCInstruction> pollForInstructions(long timeout, TimeUnit unit){
		synchronized (this.queue){
			try {
				return Optional.ofNullable(this.queue.poll(timeout, unit));
			}catch (InterruptedException e){
				return Optional.empty();
			}
		}
	}

	public Optional<IPCInstruction> pollForInstructions(){
		synchronized (this.queue){
			return Optional.ofNullable(this.queue.poll());
		}
	}

	public IPCInstruction takeInstruction() throws InterruptedException {
		synchronized (this.queue){
			return this.queue.take();
		}
	}

	private void runReading(){
		while(!this.readingThread.isInterrupted()){
			try {
				String line = reader.readLine();
				if (line != null && !line.isEmpty()) {
					IPCInstruction instruction = objectMapper.readValue(line, IPCInstruction.class);
					this.queue.add(instruction);
				}
			} catch (SocketException e){
				if(!e.getMessage().contains("Socket closed")){
					throw new RuntimeException(e);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void close() throws Exception {
		this.readingThread.interrupt();
		IPCIOFactory.closed(this);
	}
}
