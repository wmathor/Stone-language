package Stone.ast;
import java.util.List;

public class VarStmnt extends ASTList {
	public VarStmnt(List<ASTree> c) {
		super(c);
	}

	public String name() {
		return ((ASTLeaf) child(0)).token().getText();
	}

	public TypeTag type() {
		return (TypeTag) child(1);
	}

	public ASTree initializer() {
		return child(2);
	}

	public String toString() {
		return "(var " + name() + " " + type() + " " + initializer() + ")";
	}
}