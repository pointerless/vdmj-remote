package org.pointerless.vdmj.remote.engine;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Data;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Data
public class Command {

	public static final class CommandSerializer extends StdSerializer<Command> {

		public CommandSerializer() {
			this(null);
		}

		public CommandSerializer(Class<Command> s) {
			super(s);
		}

		@Override
		public void serialize(Command command, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
			command.onExecutionFinished();
			jsonGenerator.writeStartObject();
			jsonGenerator.writeStringField("id", command.id.toString());
			jsonGenerator.writeStringField("command", command.command);
			jsonGenerator.writeStringField("response", command.response.getMessage());
			jsonGenerator.writeBooleanField("error", command.isError());
			jsonGenerator.writeStringField("requested", command.requested.toString());
			jsonGenerator.writeStringField("queued", command.queued.toString());
			jsonGenerator.writeStringField("executionStart", command.executionStart.toString());
			jsonGenerator.writeStringField("executionFinished", command.executionFinished.toString());
			jsonGenerator.writeEndObject();
		}

	}

	private final UUID id = UUID.randomUUID();

	private final String command;

	private CommandResponse response;

	private boolean error = false;

	private Instant requested;

	private Instant queued;

	private Instant executionStart;

	private Instant executionFinished;

	public Command(String command) {
		this.command = command;
		this.requested = Instant.now();
	}

	public Command(){
		this.command = "";
	}

	public void onQueued(){
		this.queued = Instant.now();
	}

	public void onExecutionStart(){
		this.executionStart = Instant.now();
	}

	public void onExecutionFinished(){
		this.executionFinished = Instant.now();
	}

}
