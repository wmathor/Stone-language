package chap12;
import Stone.ast.BlockStmnt;
import Stone.ast.ParameterList;
import chap11.ArrayEnv;
import chap11.OptFunction;
import chap6.Environment;

public class OptMethod extends OptFunction {
	OptStoneObject self;

	public OptMethod(ParameterList parameters, BlockStmnt body, Environment env, int memorySize, OptStoneObject self) {
		super(parameters, body, env, memorySize);
		this.self = self;
	}

	@Override
	public Environment makeEnv() {
		ArrayEnv e = new ArrayEnv(size, env);
		e.put(0, 0, self);
		return e;
	}
}