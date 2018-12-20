package Stone.ast;
import java.util.List;

public class Arguments extends Postfix {
	public Arguments(List<ASTree> c) {
		super(c);
	}

	public int size() {
		return numChildren();
	}
}