package chap7;
import Stone.FuncParser;
import Stone.ParseException;
import chap6.BasicInterpreter;

public class FuncInterpreter extends BasicInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new FuncParser(),new NestedEnv());
	}
}