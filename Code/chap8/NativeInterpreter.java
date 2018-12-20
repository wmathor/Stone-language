package chap8;
import Stone.ClosureParser;
import Stone.ParseException;
import chap6.BasicInterpreter;
import chap7.NestedEnv;

public class NativeInterpreter extends BasicInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new ClosureParser(),new Natives().environment(new NestedEnv()));
	}
}