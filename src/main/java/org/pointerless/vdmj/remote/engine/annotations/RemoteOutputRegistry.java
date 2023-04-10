package org.pointerless.vdmj.remote.engine.annotations;

import java.util.HashSet;
import java.util.Set;

public class RemoteOutputRegistry {

	private static final Set<VDMJRemoteOutputAnnotation> outputSet = new HashSet<>();

	public static void register(VDMJRemoteOutputAnnotation outputAnnotation){
		outputSet.add(outputAnnotation);
	}

	public static Set<VDMJRemoteOutputAnnotation> getOutputSet(){
		return outputSet;
	}

	public static void clear(){
		outputSet.clear();
	}

}
