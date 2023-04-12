package org.pointerless.vdmj.remote.ipc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.pointerless.vdmj.remote.engine.Command;
import org.pointerless.vdmj.remote.rest.SessionInstruction;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IPCInstruction {

	@JsonInclude
	private IPCInstructionType type;

	private String message;

	private Command command;

	private SessionInstruction sessionInstruction;


}
