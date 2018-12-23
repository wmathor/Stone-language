package chap14;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class TypedRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(TypedInterpreter.class, args, TypeChecker.class, NativeEvaluator.class);
	}
}