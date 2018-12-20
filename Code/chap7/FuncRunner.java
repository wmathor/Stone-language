package chap7;
import javassist.gluonj.util.Loader;

public class FuncRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(FuncInterpreter.class, args, FuncEvaluator.class);
	}
}