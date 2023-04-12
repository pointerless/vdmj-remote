package org.pointerless.vdmj.remote.ipc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

@Data
public class IPCLog {

	private IPCLogType type;
	private String message;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Map<String, String> properties = null;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private IPCLogErrorLevel errorLevel;

	public static IPCLog start(String message){
		IPCLog item = new IPCLog();
		item.type = IPCLogType.START;
		item.message = message;
		return item;
	}

	public static IPCLog stop(String message){
		IPCLog item = new IPCLog();
		item.type = IPCLogType.STOP;
		item.message = message;
		return item;
	}

	public static IPCLog event(String message){
		IPCLog item = new IPCLog();
		item.type = IPCLogType.EVENT;
		item.message = message;
		return item;
	}

	public static IPCLog event(String message, Map<String, String> properties){
		IPCLog item = new IPCLog();
		item.type = IPCLogType.EVENT;
		item.message = message;
		item.properties = properties;
		return item;
	}

	public static IPCLog minorError(String message){
		IPCLog item = new IPCLog();
		item.type = IPCLogType.ERROR;
		item.errorLevel = IPCLogErrorLevel.MINOR;
		item.message = message;
		return item;
	}

	public static IPCLog majorError(String message){
		IPCLog item = new IPCLog();
		item.type = IPCLogType.ERROR;
		item.errorLevel = IPCLogErrorLevel.MAJOR;
		item.message = message;
		return item;
	}

	public static IPCLog fatalError(String message){
		IPCLog item = new IPCLog();
		item.type = IPCLogType.ERROR;
		item.errorLevel = IPCLogErrorLevel.FATAL;
		item.message = message;
		return item;
	}

}
