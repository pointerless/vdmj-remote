package org.pointerless.vdmj.remote.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.pointerless.vdmj.remote.GlobalProperties;
import org.pointerless.vdmj.remote.SessionException;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import org.pointerless.vdmj.remote.engine.annotations.RemoteOutputRegistry;
import org.pointerless.vdmj.remote.engine.annotations.VDMJRemoteOutputAnnotation;
import org.pointerless.vdmj.remote.ipc.*;
import spark.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main Output object for VDMJ Remote, requires VDMJHandler
 * and port, setting rest up from GlobalProperties
 */
@Slf4j
public class MainOutputSession extends VDMJOutputSession {

	private final IPCIO ipcio;
	private final Thread ipcInstructionThread;

	private Map<UUID, Output> outputs = new HashMap<>();
	private final Map<Output, OutputSession> outputSessionMap = new HashMap<>();

	/**
	 * Private constructor
	 *
	 * @param info    OutputSessionInfo config object
	 * @param handler VDMJHandler to use as backend
	 */
	private MainOutputSession(OutputSessionInfo info, VDMJHandler handler) {
		super(info, handler);
		ipcio = IPCIOFactory.getIPCIO();
		ipcInstructionThread = new Thread(this::ipcInstructionHandler, "Main-Output-IPCInstruction-Thread");
		ipcInstructionThread.setDaemon(true);
		ipcInstructionThread.start();
		refreshOutputs();
	}

	private void ipcInstructionHandler(){
		while(!Thread.currentThread().isInterrupted()){
			ipcio.pollForInstructions(100, TimeUnit.MILLISECONDS).ifPresent(ipcInstruction -> {
				if(ipcInstruction.getType() != null) {
					switch (ipcInstruction.getType()) {
						case VDMJ:
							if (ipcInstruction.getCommand() != null) {
								try {
									ipcio.write(IPCLog.event(objectMapper.writeValueAsString(
											this.handler.runCommand(ipcInstruction.getCommand().getCommand()))
									));
								} catch (InterruptedException | JsonProcessingException e) {
									ipcio.write(IPCLog.event("Command failed: " + e.getMessage()));
								}
							}
							break;
						case SESSION:
							if (ipcInstruction.getSessionInstruction() != null) {
								switch (ipcInstruction.getSessionInstruction()) {
									case STOP:
										this.stopSession();
										break;
									case RELOAD:
										try {
											this.runReload();
											this.ipcio.write(IPCLog.event("Successfully reloaded"));
										} catch (InterruptedException e) {
											this.ipcio.write(IPCLog.event("Could not reload: " + e.getMessage()));
										}
										break;
								}
							}
							break;
						case HEARTBEAT:
							ipcio.write(IPCLog.event(ipcInstruction.getMessage()));
							break;
					}
				}else{
					ipcio.write(IPCLog.event("Invalid message"));
				}
			});
		}
	}

	@Override
	public void stopSession() {
		super.stopSession();
		this.ipcio.write(IPCLog.stop("Exited gracefully"));
		this.ipcInstructionThread.interrupt();
		try {
			this.ipcio.close();
		}catch (Exception e){
			log.debug("Closing IPCIO threw exception: "+e.getMessage());
		}
	}

	private void refreshOutputs() {
		Set<Output> outputSet = RemoteOutputRegistry.getOutputSet().stream()
				.map(VDMJRemoteOutputAnnotation::convertToOutput)
				.collect(Collectors.toSet());

		// Get old that aren't in new
		Set<Output> toRemove = outputs.values().stream()
				.filter(old -> outputSet.stream().noneMatch(newOutput -> newOutput.fullyEquals(old)))
				.collect(Collectors.toSet());

		// Get new that aren't in old
		Set<Output> toAdd = outputSet.stream()
						.filter(newOutput -> outputs.values().stream().noneMatch(old -> old.fullyEquals(newOutput)))
								.collect(Collectors.toSet());
		// Remove old that aren't in new
		toRemove.forEach(remove -> {
			if(outputSessionMap.containsKey(remove)) {
				outputSessionMap.remove(remove).stopSession();
			}
			outputs.remove(remove.getId());
		});
		// Add new that aren't in old
		toAdd.forEach(add -> outputs.put(add.getId(), add));
	}

	/**
	 * Create MainOutputSession from previously defined properties
	 *
	 * @param handler The VDMJHandler to use as backend
	 * @param port    The port to host on
	 * @return A fully prepared instance of MainOutputSession ready to call startSession()
	 */
	public static MainOutputSession mainOutputSession(VDMJHandler handler, int port) {
		OutputSessionInfo info = new OutputSessionInfo();
		info.setPort(port);
		info.setStaticHostType(OutputSessionInfo.StaticHostType.INTERNAL);
		info.setStaticHostPath(GlobalProperties.mainHostResourceFolder);
		info.setProperty("displayName", "Main Session");
		return new MainOutputSession(info, handler);
	}

	/**
	 * Gets a random port not currently occupied on the system
	 *
	 * @return A port number that can be hosted on
	 * @throws RuntimeException No ports available
	 */
	public static Integer getRandomPort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException("Could not find port to bind on: " + e.getMessage());
		}
	}

	/**
	 * Adds necessary routes then passes Service object up the call chain
	 *
	 * @param http Service instance to use (see OutputSession.startSession for normal use)
	 */
	@Override
	protected void run(Service http) {

		// Lists the Outputs available
		http.get("/outputs", (request, response) -> {
			response.type("application/json");
			return objectMapper.writeValueAsString(outputs.values());
		});

		// Starts an Output passed as request body
		http.post("/startOutput", (request, response) -> {
			try {
				response.type("application/json");
				Output output = objectMapper.readValue(request.body(), Output.class);
				if (!outputs.containsKey(output.getId())) {
					throw new SessionException("Could not find output: " + objectMapper.writeValueAsString(output));
				} else if (outputSessionMap.containsKey(output)) {
					return objectMapper.writeValueAsString(outputSessionMap.get(output).getInfo());
				}
				output = outputs.get(output.getId());
				OutputSession outputSession = output.toSession(handler, getRandomPort());
				outputSessionMap.put(output, outputSession);
				outputSession.startSession();
				return objectMapper.writeValueAsString(outputSession.getInfo());
			} catch (JsonProcessingException jsonProcessingException) {
				throw new SessionException("Could not process output JSON: " + jsonProcessingException.getMessage());
			}
		});

		// Stops an Output passed as request body
		http.post("/stopOutput", (request, response) -> {
			try {
				Output output = objectMapper.readValue(request.body(), Output.class);
				if (!outputs.containsKey(output.getId())) {
					throw new SessionException("Could not find output: " + objectMapper.writeValueAsString(output));
				} else if (outputSessionMap.containsKey(output)) {
					output = outputs.get(output.getId());
					outputSessionMap.remove(output).stopSession();
				}
				return true;
			} catch (JsonProcessingException jsonProcessingException) {
				throw new SessionException("Could not process output JSON: " + jsonProcessingException.getMessage());
			}
		});

		// Get the string printed by VDMJ on startup
		http.get("/startup", (request, response) -> this.handler.getStartupString());

		http.post("/reload", (request, response) -> {
			response.type("application/json");
			return objectMapper.writeValueAsString(this.runReload());
		});

		super.run(http);
		ipcio.write(IPCLog.start("VDMJ-Remote is running"));
	}

	private Command runReload() throws InterruptedException {
		RemoteOutputRegistry.clear();
		Command out = this.handler.runCommand("reload");
		this.handler.setStartupString(out.getResponse().getMessage());
		if(!out.isError()) this.refreshOutputs();
		return out;
	}
}
