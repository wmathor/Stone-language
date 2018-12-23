package chap11;
import chap6.BasicEvaluator;
import chap6.Environment;
import chap8.Natives;
import Stone.BasicParser;
import Stone.ClosureParser;
import Stone.CodeDialog;
import Stone.Lexer;
import Stone.ParseException;
import Stone.Token;
import Stone.ast.ASTree;
import Stone.ast.NullStmnt;

public class EnvOptInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new ClosureParser(), new Natives().environment(new ResizableArrayEnv()));
	}

	public static void run(BasicParser bp, Environment env) throws ParseException {
		Lexer lexer = new Lexer(new CodeDialog());
		while (lexer.peek(0) != Token.EOF) {
			ASTree t = bp.parse(lexer);
			if (!(t instanceof NullStmnt)) {
				((EnvOptimizer.ASTreeOptEx) t).lookup(((EnvOptimizer.EnvEx2) env).symbols());
				Object r = ((BasicEvaluator.ASTreeEx) t).eval(env);
				System.out.println("=> " + r);
			}
		}
	}
}