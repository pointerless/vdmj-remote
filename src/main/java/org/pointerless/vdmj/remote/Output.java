package org.pointerless.vdmj.remote;

import lombok.Data;
import org.pointerless.vdmj.remote.annotations.tc.TCWebGUIAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class Output {

	private UUID id = UUID.randomUUID();

	private String module;

	private String type;

	private Map<String, String> properties = new HashMap<>();

	public static Output gui(String module, TCWebGUIAnnotation annotation){
		Output output = new Output();
		output.module = module;
		output.type = "GUI";
		output.properties.put("location", annotation.getStaticWebLocation());
		output.properties.put("nickname", annotation.getNickname());
		output.properties.put("displayName", module+": "+annotation.getNickname());
		return output;
	}

}
