package org.pointerless.vdmj.remote.engine;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.plugins.VDMJ;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Handles VDMJ IO in a thread-safe manner.
 */
public class VDMJHandler implements Runnable {

	private final PairedPipedIOStream in;
	private final PairedPipedIOStream out;
	private final PairedPipedIOStream err;

	private String startupString = null;

	private final CommandQueue inputQueue = new CommandQueue();
	private final CommandQueue outputQueue = new CommandQueue();

	Reader receive;
	Reader error;
	Writer send;

	String[] args;

	Thread commandRunner;

	public VDMJHandler(String[] args) throws IOException {
		Console.charset = StandardCharsets.UTF_8;

		in = new PairedPipedIOStream();
		out = new PairedPipedIOStream();
		err = new PairedPipedIOStream();

		Console.out = new ConsolePrintWriter(new PrintStream(out.getOutputStream()));
		Console.in = new BufferedReader(new InputStreamReader(in.getInputStream()));
		Console.err = new ConsolePrintWriter(new PrintStream(err.getOutputStream()));

		this.args = args;
		this.receive = new InputStreamReader(out.getInputStream(), StandardCharsets.UTF_8);
		this.error = new InputStreamReader(err.getInputStream(), StandardCharsets.UTF_8);
		this.send = new OutputStreamWriter(in.getOutputStream(), StandardCharsets.UTF_8);

		this.commandRunner = new Thread(this::commandHandler);
		this.commandRunner.setDaemon(true);
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
				command.setResponse(CommandResponse.success(this.readReceive()));
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
			if(!this.commandRunner.isAlive()){
				command.setError(true);
				command.setResponse(CommandResponse.error("VDMJ Stopped running", 400));
			}
		} while (this.outputQueue.peek() == null || this.outputQueue.peek().getId() != command.getId());
		return this.outputQueue.take();
	}

	private String pickupError() throws IOException {
		//TODO
		return "";
	}

	private String readReceive() throws IOException {
		StringBuilder strOut = new StringBuilder();
		try{
			int i;
			boolean newLined = false;
			while((i = receive.read()) != -1){
				char c = (char)i;
				if(c == '\n') newLined = true;
				else if(newLined && c == '>'){
					//noinspection ResultOfMethodCallIgnored
					receive.read();
					break;
				}else if(newLined){
					newLined = false;
				}
				strOut.append(c);
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		out.getOutputStream().flush();
		return strOut.toString();
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
		if(this.startupString != null) return;
		this.startupString = this.readReceive();
	}

	public String getStartupString() {
		return this.startupString;
	}

	@Override
	public void run() {
		try {
			VDMJ.main(args);
		}catch (Exception e){
			System.exit(-1);
		}
	}

}
