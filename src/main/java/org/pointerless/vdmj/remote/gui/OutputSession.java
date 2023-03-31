package org.pointerless.vdmj.remote.gui;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pointerless.vdmj.remote.GlobalProperties;
import org.pointerless.vdmj.remote.SessionException;
import spark.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class OutputSession {
	@Data
	public static final class OutputSessionInfo{

		@Slf4j
		public static final class OutputSessionInfoSerializer extends StdSerializer<OutputSessionInfo> {

			public OutputSessionInfoSerializer(){ this(null); }

			public OutputSessionInfoSerializer(Class<OutputSessionInfo> s) {
				super(s);
			}

			@Override
			public void serialize(OutputSessionInfo info, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
				jsonGenerator.writeStartObject();
				jsonGenerator.writeStringField("accessURL", "http://" + GlobalProperties.hostname +
						":" + info.port);
				info.properties.forEach((k, v) -> {
					try {
						jsonGenerator.writeStringField(k, v);
					} catch (IOException e) {
						log.error("Could not write info property: " + e);
					}
				});
				jsonGenerator.writeEndObject();
			}

		}

		public enum StaticHostType{
			NOHOST,
			INTERNAL,
			EXTERNAL
		}

		private int port;
		private String staticHostPath;
		private StaticHostType staticHostType;

		private Map<String, String> properties = new HashMap<>();

		public void setProperty(String key, String value){
			this.properties.put(key, value);
		}

		public String getProperty(String key){
			return this.properties.get(key);
		}

		public String removePropertyIfExists(String key){
			return this.properties.remove(key);
		}

	}

	protected OutputSessionInfo info;

	private Service http;

	public OutputSession(OutputSessionInfo info){
		this.info = info;
	}

	public final void startSession(){
		http = Service.ignite();
		http.ipAddress(GlobalProperties.hostname);
		http.port(this.info.port);

		if(this.info.staticHostType == OutputSessionInfo.StaticHostType.INTERNAL){
			http.staticFiles.location(this.info.staticHostPath);
		}else if(this.info.staticHostType == OutputSessionInfo.StaticHostType.EXTERNAL){
			http.externalStaticFileLocation(this.info.staticHostPath);
		}

		http.get("/running", (request, response) -> true);

		this.run(http);
	}

	public final void stopSession(){
		this.http.stop();
	}

	protected OutputSessionInfo getInfo() {
		return this.info;
	}


	protected void run(Service http){

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
