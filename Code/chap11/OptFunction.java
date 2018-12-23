package chap11;
import Stone.ast.BlockStmnt;
import Stone.ast.ParameterList;
import chap6.Environment;
import chap7.Function;

public class OptFunction extends Function {
	protected int size;

	public OptFunction(ParameterList parameters, BlockStmnt body, Environment env, int memorySize) {
		super(parameters, body, env);
		size = memorySize;
	}

	@Override
	public Environment makeEnv() {
		return new ArrayEnv(size, env);
	}
}