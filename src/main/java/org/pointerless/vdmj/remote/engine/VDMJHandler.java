package org.pointerless.vdmj.remote.engine;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.plugins.VDMJ;
import lombok.extern.slf4j.Slf4j;
import org.pointerless.vdmj.remote.ipc.IPCIO;
import org.pointerless.vdmj.remote.ipc.IPCIOFactory;
import org.pointerless.vdmj.remote.ipc.IPCLog;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Handles VDMJ IO in a thread-safe manner.
 */
@Slf4j
public class VDMJHandler implements Runnable {

	private final IPCIO ipcio;

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
		this.ipcio = IPCIOFactory.getIPCIO();

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

		this.commandRunner = new Thread(this::commandHandler, "VDMJ-Handler-Command-Runner");
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

	public Command runCommand(Command command) throws InterruptedException {
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

	public Command runCommand(String commandText) throws InterruptedException {
		Command command = new Command(commandText);
		return this.runCommand(command);
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
				else if(c == '>' && (newLined || strOut.length() == 0)){
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

	public void setStartupString(String startupString){
		this.startupString = startupString;
	}

	@Override
	public void run() {
		try {
			VDMJ.main(args);
		}catch (Exception e){
			ipcio.write(IPCLog.fatalError("VDMJ thread exited with the following error: "+e.getMessage()));
			log.error("VDMJ thread exited with the following error: "+e.getMessage());
			System.exit(-1);
		}
	}

}
