package org.pointerless.vdmj.remote.annotations.tc;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCWebGUIAnnotation extends TCAnnotation {
	private static final Logger logger = LoggerFactory.getLogger(TCWebGUIAnnotation.class);

	public final static Map<String, List<TCWebGUIAnnotation>> moduleMap = new HashMap<>();
	private String staticWebLocation;
	private String nickname;

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
			List<TCWebGUIAnnotation> current = moduleMap.getOrDefault(module.name.getName(), new ArrayList<>());
			current.add(this);
			moduleMap.put(module.name.getName(), current);
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

}
