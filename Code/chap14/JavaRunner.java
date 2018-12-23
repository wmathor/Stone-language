package chap14;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class JavaRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(TypedInterpreter.class, args, ToJava.class, InferFuncTypes.class, NativeEvaluator.class);
	}
}