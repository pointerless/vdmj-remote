package org.pointerless.vdmj.remote.gui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.pointerless.vdmj.remote.SessionException;
import org.pointerless.vdmj.remote.engine.VDMJHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.UUID;

@Data
public class Output {

	private UUID id = UUID.randomUUID();

	private String module;

	private String type;

	private String location;

	private String displayName;

	@JsonIgnore
	private Class<? extends OutputSession> sessionClass;

	public OutputSession toSession(VDMJHandler handler, int port) throws SessionException {
		OutputSessionInfo info = new OutputSessionInfo();
		info.setPort(port);
		info.setStaticHostType(OutputSessionInfo.StaticHostType.EXTERNAL);
		info.setStaticHostPath(location);
		info.setProperty("displayName", displayName);
		try {
			return sessionClass.getConstructor(OutputSessionInfo.class, VDMJHandler.class).newInstance(info, handler);
		}catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e){
			throw new SessionException(e.getMessage());
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Output output = (Output) o;
		return id.equals(output.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
