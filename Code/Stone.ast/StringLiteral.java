package Stone.ast;
import Stone.Token;

public class StringLiteral extends ASTLeaf {

	public StringLiteral(Token token) {
		super(token);
	}

	public String value() {
		return token().getText();
	}
}
