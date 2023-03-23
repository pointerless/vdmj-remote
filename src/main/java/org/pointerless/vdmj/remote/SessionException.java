package org.pointerless.vdmj.remote;

public class SessionException extends Exception{

	public SessionException(String message){
		super("Session Exception: "+message);
	}

}
