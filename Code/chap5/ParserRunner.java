package chap5;
import Stone.ast.ASTree;
import Stone.*;

public class ParserRunner {
	public static void main(String[] args) throws ParseException {
		Lexer l = new Lexer(new CodeDialog());
		BasicParser bp = new BasicParser();
		while (l.peek(0) != Token.EOF) {
			ASTree ast = bp.parse(l);
			System.out.println("=> " + ast.toString());
		}
	}
}
