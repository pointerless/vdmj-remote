package org.pointerless.vdmj.remote;

import com.fujitsu.vdmj.plugins.VDMJ;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class VDMJHandler implements Runnable {

	private static final int pipeSize = 2 << 15;

	public static final PrintStream consoleOut = System.out;
	public static final InputStream consoleIn = System.in;

	private final InputStream vdmjOut;
	private final OutputStream vdmjIn;

	private final InputStream vdmjInternalInput;
	private final OutputStream vdmjInternalOutput;

	private String startupString;

	private final CommandQueue inputQueue = new CommandQueue();
	private final CommandQueue outputQueue = new CommandQueue();

	Reader receive;
	Writer send;

	String[] args;

	Thread commandRunner;

	public VDMJHandler(String[] args) {
		vdmjOut = new PipedInputStream(pipeSize);
		vdmjInternalInput = new PipedInputStream(pipeSize);

		try {
			vdmjIn = new PipedOutputStream((PipedInputStream) vdmjInternalInput);
			vdmjInternalOutput = new PipedOutputStream((PipedInputStream) vdmjOut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		System.setOut(new PrintStream(vdmjInternalOutput));
		System.setIn(vdmjInternalInput);

		this.args = args;
		this.receive = new InputStreamReader(vdmjOut, StandardCharsets.UTF_8);
		this.send = new OutputStreamWriter(vdmjIn, StandardCharsets.UTF_8);

		this.commandRunner = new Thread(this::commandHandler);
		this.commandRunner.start();
	}

	private void commandHandler() {
		while(true){
			try {
				Command command;
				synchronized(this.inputQueue){
					this.inputQueue.wait();
					command = this.inputQueue.take();
				}
				this.writeSend(command.getCommand());
				command.setResponse(this.readReceive());
				synchronized (this.outputQueue){
					this.outputQueue.put(command);
					this.outputQueue.notifyAll();
				}
			}catch(InterruptedException interruptedException){
				System.err.println("Command handler interrupted: "+interruptedException.getMessage());
				System.exit(-1);
			}catch (IOException ioException){
				System.err.println("Command handler IO Exception: "+ioException.getMessage());
				System.exit(-1);
			}
		}
	}

	public Command runCommand(String commandText) throws InterruptedException {
		Command command = new Command(commandText);
		synchronized (this.inputQueue){
			this.inputQueue.put(command);
			this.inputQueue.notify();
		}
		do {
			synchronized (this.outputQueue){
				this.outputQueue.wait(10);
			}
		} while (this.outputQueue.peek() == null || this.outputQueue.peek().getId() != command.getId());
		return this.outputQueue.take();
	}

	private String readReceive() throws IOException {
		StringBuilder out = new StringBuilder();
		try{
			int i;
			boolean newLined = false;
			while((i = receive.read()) != -1){
				char c = (char)i;
				if(c == '\n') newLined = true;
				else if(newLined && c == '>'){
					receive.read();
					break;
				}else if(newLined){
					newLined = false;
				}
				out.append(c);
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		vdmjInternalOutput.flush();
		return out.toString();
	}

	private void writeSend(String toWrite){
		try{
			send.write(toWrite);
			send.write(System.getProperty("line.separator"));
			send.flush();
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public void pickupStartupString() throws IOException {
		this.startupString = this.readReceive();
	}

	public String getStartupString() {
		return this.startupString;
	}

	@Override
	public void run() {
		VDMJ.main(args);
	}
}
