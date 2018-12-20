package chap9;
import chap7.ClosureEvaluator;
import chap8.NativeEvaluator;
import javassist.gluonj.util.Loader;

public class ClassRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(ClassInterpreter.class, args, ClassEvaluator.class,NativeEvaluator.class,ClosureEvaluator.class);
	}
}