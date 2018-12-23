package Stone;
import static Stone.Parser.rule;
import Stone.ast.*;

public class TypedParser extends FuncParser {
	Parser typeTag = rule(TypeTag.class).sep(":").identifier(reserved);
	Parser variable = rule(VarStmnt.class).sep("var").identifier(reserved).maybe(typeTag).sep("=").ast(expr);

	public TypedParser() {
		reserved.add(":");
		param.maybe(typeTag);
		def.reset().sep("def").identifier(reserved).ast(paramList).maybe(typeTag).ast(block);
		statement.insertChoice(variable);
	}
}