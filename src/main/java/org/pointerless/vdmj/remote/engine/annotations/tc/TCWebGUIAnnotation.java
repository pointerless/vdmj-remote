package org.pointerless.vdmj.remote.engine.annotations.tc;

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import org.pointerless.vdmj.remote.engine.annotations.RemoteOutputRegistry;
import org.pointerless.vdmj.remote.engine.annotations.VDMJRemoteOutputAnnotation;
import org.pointerless.vdmj.remote.gui.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCWebGUIAnnotation extends TCAnnotation implements VDMJRemoteOutputAnnotation {
	private static final Logger logger = LoggerFactory.getLogger(TCWebGUIAnnotation.class);

	private String staticWebLocation;
	private String nickname;
	private String moduleName;

	private static String usage(){
		return "\n--@WebGUI(<nickname>, <static web location> \nmodule <module name>" ;
	}

	public TCWebGUIAnnotation(TCIdentifierToken name, TCExpressionList args) {
		super(name, args);
	}

	@Override
	public void doInit(Environment globals){
		this.nickname = this.args.get(0).toString().replaceAll("\"", "");
		this.staticWebLocation = this.args.get(1).toString().replaceAll("\"", "");
	}

	public String getStaticWebLocation(){
		return staticWebLocation;
	}

	public String getNickname(){
		return this.nickname;
	}

	@Override
	public void tcBefore(TCModule module){
		if(this.args.isEmpty()){
			name.report(6009, "@WebGUI requires at least two arguments: "+usage());
		}else {
			this.moduleName = module.name.getName();
			RemoteOutputRegistry.register(this);
		}
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope) {
		name.report(6009, "@WebGUI is only applicable to modules: "+usage());
	}

	@Override
	public void tcBefore(TCClassDefinition clazz) {
		name.report(6009, "@WebGUI is only applicable to modules: "+usage());
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope) {
		name.report(6009, "@WebGUI is only applicable to modules: "+usage());
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope) {
		name.report(6009, "@WebGUI is only applicable to modules: "+usage());
	}

	@Override
	public Output convertToOutput() {
		Output output = new Output();
		output.setModule(moduleName);
		output.setType("GUI");
		output.setLocation(staticWebLocation);
		output.setDisplayName(moduleName+": "+nickname);
		return output;
	}

}
