package org.pointerless.vdmj.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import spark.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GUIOutputSession{
	private final ObjectMapper objectMapper;

	private final GUISessionInfo info;
	private final VDMJHandler vdmjHandler;

	public GUIOutputSession(GUISessionInfo info, VDMJHandler handler){
		this.info = info;
		this.vdmjHandler = handler;



		this.objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(Command.class, new Command.CommandSerializer());
		this.objectMapper.registerModule(module);
	}

	public void run() {
		Service http = Service.ignite();

		http.ipAddress("127.0.0.1");
		http.port(info.getPort());

		if(!Files.exists(Paths.get(this.info.getHostedPath()))){
			http.staticFiles.location("no-folder-html");
		} else {
			http.externalStaticFileLocation(info.getHostedPath());
		}

		http.post("/exec", (request, response) -> {
			response.type("application/json");
			Command out = this.vdmjHandler.runCommand(request.body());
			return objectMapper.writeValueAsString(out);
		});

	}
}
