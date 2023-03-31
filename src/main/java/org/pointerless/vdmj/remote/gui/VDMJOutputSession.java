package org.pointerless.vdmj.remote.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pointerless.vdmj.remote.SerializationHelper;
import org.pointerless.vdmj.remote.engine.Command;
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
			Command out = this.handler.runCommand(request.body());
			return objectMapper.writeValueAsString(out);
		});

		http.exception(InterruptedException.class, (e, request, response) -> {
			response.type("text/plain");
			response.body(e.getMessage());
			response.status(400);
		});

		super.run(http);
	}

}
