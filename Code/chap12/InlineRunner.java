package chap12;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class InlineRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(ObjOptInterpreter.class, args, InlineCache.class, NativeEvaluator.class);
	}
}