package org.pointerless.vdmj.remote.gui;

import lombok.Data;
import org.pointerless.vdmj.remote.engine.VDMJHandler;
import org.pointerless.vdmj.remote.engine.annotations.VDMJRemoteOutputAnnotation;
import org.reflections.Reflections;

import java.util.Set;
import java.util.UUID;

@Data
public class Output {

	private UUID id = UUID.randomUUID();

	private String module;

	private String type;

	private String location;

	private String displayName;

	public OutputSession toSession(VDMJHandler handler, int port){
		OutputSession.OutputSessionInfo info = new OutputSession.OutputSessionInfo();
		info.setPort(port);
		info.setStaticHostType(OutputSession.OutputSessionInfo.StaticHostType.EXTERNAL);
		info.setStaticHostPath(location);
		info.setProperty("displayName", displayName);
		return new VDMJOutputSession(info, handler);
	}

	public static Set<Output> getAllOutputAnnotationInstances() {
		Reflections reflections = new Reflections();
		Set<Class<? extends VDMJRemoteOutputAnnotation>> subTypes = reflections.getSubTypesOf(VDMJRemoteOutputAnnotation.class);
		return null;
	}

}
