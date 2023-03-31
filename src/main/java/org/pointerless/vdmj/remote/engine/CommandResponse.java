package org.pointerless.vdmj.remote.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class CommandResponse {

	private String message;

	@JsonIgnore
	private Integer responseCode = null;

	public static CommandResponse success(String response){
		CommandResponse commandResponse = new CommandResponse();
		commandResponse.message = response;
		return commandResponse;
	}

	public static CommandResponse error(String error, Integer responseCode){
		CommandResponse commandResponse = new CommandResponse();
		commandResponse.responseCode = responseCode;
		commandResponse.message = error;
		return commandResponse;
	}

}
