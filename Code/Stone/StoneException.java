package Stone;

import Stone.ast.ASTree;

public class StoneException extends RuntimeException {
	public StoneException(String m) {
		super(m);
	}

	public StoneException(String m, ASTree t) { // Overload function
		super(m + " " + t.location());
	}
}
