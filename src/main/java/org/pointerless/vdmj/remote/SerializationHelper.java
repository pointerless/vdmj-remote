package org.pointerless.vdmj.remote;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.rest.OutputSessionInfo;

public class SerializationHelper {

	public static ObjectMapper getObjectMapper(){
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(Command.class, new Command.CommandSerializer());
		module.addSerializer(OutputSessionInfo.class,
				new OutputSessionInfo.OutputSessionInfoSerializer());
		objectMapper.registerModule(module);
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
		return objectMapper;
	}

}
