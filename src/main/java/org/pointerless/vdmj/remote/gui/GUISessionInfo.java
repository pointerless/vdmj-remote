package org.pointerless.vdmj.remote.gui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.UUID;

@Data
public class GUISessionInfo {

	private final UUID id;

	private String accessURL;

	private String displayName;

	private String hostedPath;

	@JsonIgnore
	private int port;

	public GUISessionInfo(UUID id, int port, String hostedPath, String displayName){
		this.id = id;
		this.port = port;
		this.hostedPath = hostedPath;
		this.displayName = displayName;
		this.accessURL = "http://127.0.0.1:"+port+"/";
	}

}
