package org.pointerless.vdmj.remote.rest;

import lombok.extern.slf4j.Slf4j;
import org.pointerless.vdmj.remote.GlobalProperties;
import org.pointerless.vdmj.remote.SessionException;
import spark.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public abstract class OutputSession {

	protected OutputSessionInfo info;

	private Service http;

	public OutputSession(OutputSessionInfo info){
		this.info = info;
	}

	private void hostNoFolder(String reason){
		http.staticFiles.location(GlobalProperties.hostErrorResourceFolder);
		http.get("reason", (request, response) -> reason);
	}

	private void staticSetup(){
		if(this.info.getStaticHostType() == OutputSessionInfo.StaticHostType.INTERNAL){
			if(this.getClass().getClassLoader().getResource(this.info.getStaticHostPath()) == null){
				hostNoFolder("No such host-able resource: "+this.info.getStaticHostPath());
			}else {
				http.staticFiles.location(this.info.getStaticHostPath());
			}
		}else if(this.info.getStaticHostType() == OutputSessionInfo.StaticHostType.EXTERNAL){
			if(!Files.exists(Path.of(this.info.getStaticHostPath()))){
				hostNoFolder("No such host-able folder: "+this.info.getStaticHostPath());
			}else {
				http.externalStaticFileLocation(this.info.getStaticHostPath());
			}
		}
	}

	public final void startSession(){
		http = Service.ignite();
		http.ipAddress(GlobalProperties.hostname);
		http.port(this.info.getPort());

		this.staticSetup();

		http.get("/running", (request, response) -> true);

		this.run(http);
	}

	public void stopSession(){
		this.http.stop();
	}

	protected OutputSessionInfo getInfo() {
		return this.info;
	}

	public final void awaitInitialization(){
		this.http.awaitInitialization();
	}

	public final void awaitStop(){
		this.http.awaitStop();
	}

	protected void run(Service http){

		// Allow CORS

		http.options("/*", (req, res) -> {
			String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
			}

			String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
			if (accessControlRequestMethod != null) {
				res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
			}

			return "OK";
		});

		http.before((req, res) -> {
			res.header("Access-Control-Allow-Origin", "*");
			res.header("Access-Control-Allow-Headers", "*");
			res.type("application/json");
		});

		// Deal with default thrown exception
		http.exception(SessionException.class, (e, request, response) -> {
			response.type("text/plain");
			response.body(e.getMessage());
			response.status(400);
		});

		// Don't return until routes registered;
		http.awaitInitialization();
	}

}
