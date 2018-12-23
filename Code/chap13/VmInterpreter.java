package chap13;
import Stone.FuncParser;
import Stone.ParseException;
import chap11.EnvOptInterpreter;
import chap8.Natives;

public class VmInterpreter extends EnvOptInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new FuncParser(), new Natives().environment(new StoneVMEnv(100000, 100000, 1000)));
	}
}