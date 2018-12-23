package chap14;
import Stone.BasicParser;
import Stone.CodeDialog;
import Stone.Lexer;
import Stone.Token;
import Stone.TypedParser;
import Stone.ParseException;
import Stone.ast.ASTree;
import Stone.ast.NullStmnt;
import chap11.EnvOptimizer;
import chap11.ResizableArrayEnv;
import chap6.BasicEvaluator;
import chap6.Environment;

public class TypedInterpreter {
	public static void main(String[] args) throws ParseException, TypeException {
		TypeEnv te = new TypeEnv();
		run(new TypedParser(), new TypedNatives(te).environment(new ResizableArrayEnv()), te);
	}

	public static void run(BasicParser bp, Environment env, TypeEnv typeEnv) throws ParseException, TypeException {
		Lexer lexer = new Lexer(new CodeDialog());
		while (lexer.peek(0) != Token.EOF) {
			ASTree tree = bp.parse(lexer);
			if (!(tree instanceof NullStmnt)) {
				((EnvOptimizer.ASTreeOptEx) tree).lookup(((EnvOptimizer.EnvEx2) env).symbols());
				TypeInfo type = ((TypeChecker.ASTreeTypeEx) tree).typeCheck(typeEnv);
				Object r = ((BasicEvaluator.ASTreeEx) tree).eval(env);
				System.out.println("=> " + r + " : " + type);
			}
		}
	}
}