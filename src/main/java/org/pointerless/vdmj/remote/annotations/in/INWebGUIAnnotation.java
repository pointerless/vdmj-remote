package org.pointerless.vdmj.remote.annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.CPUValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Vector;

public class INWebGUIAnnotation extends INAnnotation {
	private static Context guiContext = null;
	private static final Logger logger = LoggerFactory.getLogger(INWebGUIAnnotation.class);


	public INWebGUIAnnotation(TCIdentifierToken name, INExpressionList args) {
		super(name, args);
		logger.info("IN GUI Created");
	}

	@Override
	protected void doInit(Context ctxt) {
		super.doInit(ctxt);
		logger.info("CTXT Init");
	}

	public static void doInit(){
		logger.info("IN GUI Init");
		Context root = Interpreter.getInstance().getInitialContext();
		guiContext = new StateContext(LexLocation.ANY, "@GUI initialization", root, null);
		guiContext.setThreadState(CPUValue.vCPU);

		List<ContextException> problems = new Vector<>();
		List<INAnnotation> guis = getInstances(INWebGUIAnnotation.class);
		int retries = guis.size();

		for(INAnnotation gui : guis){
			logger.info("Found GUI annotation: "+gui.toString());
		}
	}

}
