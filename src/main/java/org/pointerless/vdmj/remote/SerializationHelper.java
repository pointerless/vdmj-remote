package org.pointerless.vdmj.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.gui.OutputSession;

public class SerializationHelper {

	public static ObjectMapper getObjectMapper(){
		ObjectMapper objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(Command.class, new Command.CommandSerializer());
		module.addSerializer(OutputSession.OutputSessionInfo.class,
				new OutputSession.OutputSessionInfo.OutputSessionInfoSerializer());
		objectMapper.registerModule(module);
		return objectMapper;
	}

}
