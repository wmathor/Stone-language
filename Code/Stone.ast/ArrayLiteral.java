package Stone.ast;
import java.util.List;

public class ArrayLiteral extends ASTList {

	public ArrayLiteral(List<ASTree> c) {
		super(c);
	}

	public int size() {
		return numChildren();
	}
}