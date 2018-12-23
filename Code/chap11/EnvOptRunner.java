package chap11;
import chap8.NativeEvaluator;
import javassist.gluonj.util.Loader;

public class EnvOptRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(EnvOptInterpreter.class, args, EnvOptimizer.class, NativeEvaluator.class);
	}
}