package chap7;

import javassist.gluonj.util.Loader;

public class ClosureRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(ClosureInterpreter.class, args, ClosureEvaluator.class);
	}
}