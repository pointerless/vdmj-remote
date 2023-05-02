package org.pointerless.vdmj.remote.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pointerless.vdmj.remote.SerializationHelper;
import org.pointerless.vdmj.remote.SessionException;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.engine.CommandResponse;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import spark.Service;

/**
 * VDMJOutputSession, simply hosts the VDMJHandler as a REST API
 */
public class VDMJOutputSession extends OutputSession {

	protected VDMJHandler handler;

	protected static final ObjectMapper objectMapper = SerializationHelper.getObjectMapper();

	/**
	 * Constructor
	 * @param info OutputSessionInfo to use for setup
	 * @param handler VDMJHandler to open to API
	 */
	public VDMJOutputSession(OutputSessionInfo info, VDMJHandler handler){
		super(info);
		this.handler = handler;
	}

	/**
	 * Adds necessary routes then passes Service object up the call chain
	 * @param http Service instance to use (see OutputSession.startSession for normal use)
	 */
	@Override
	protected void run(Service http){
		http.post("/exec", (request, response) -> {
			response.type("application/json");
			Command out = outFilter(this.handler.runCommand(inFilter(request.body())));
			return objectMapper.writeValueAsString(out);
		});

		http.exception(InterruptedException.class, (e, request, response) -> {
			response.type("text/plain");
			response.body(e.getMessage());
			response.status(400);
		});

		super.run(http);
	}

	private static Command inFilter(String reqBody) throws SessionException {
		if(reqBody.trim().matches("^(?:q|quit)\\s*.*")){
			throw new SessionException("Cannot quit via terminal, please use REST request");
		}

		return new Command(reqBody);
	}

	private static Command outFilter(Command command) throws SessionException{
		if(command.getCommand().trim().matches("help")){
			StringBuilder newMessage = new StringBuilder();
			for(String line : command.getResponse().getMessage().split("\n")){
				if(!line.trim().matches("\\[q]uit.*")){
					newMessage.append(line).append("\n");
				}
			}
			command.getResponse().setMessage(newMessage.toString());
		}else if(command.getResponse().getMessage().trim().matches("^Unknown\\scommand\\s'.*'\\.\\s.*")){
			throw new SessionException(command.getResponse().getMessage());
		}

		return command;
	}

}
