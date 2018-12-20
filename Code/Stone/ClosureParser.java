package Stone;
import static Stone.Parser.rule;
import Stone.ast.Fun;

public class ClosureParser extends FuncParser {
	public ClosureParser() {
		primary.insertChoice(rule(Fun.class).sep("fun").ast(paramList).ast(block));
	}
}