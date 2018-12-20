package chap7;
import Stone.ClosureParser;
import Stone.ParseException;
import chap6.BasicInterpreter;

public class ClosureInterpreter extends BasicInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new ClosureParser(),new NestedEnv());
	}
}