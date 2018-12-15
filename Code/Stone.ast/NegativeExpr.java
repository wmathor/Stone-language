package Stone.ast;
import java.util.List;

public class NegativeExpr extends ASTList {

	public NegativeExpr(List<ASTree> t) {
		super(t);
	}

	public ASTree operand() {
		return child(0);
	}

	public String toString() {
		return "-" + operand();
	}
}
