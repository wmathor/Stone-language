package chap13;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class VmRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(VmInterpreter.class, args, VmEvaluator.class, NativeEvaluator.class);
	}
}