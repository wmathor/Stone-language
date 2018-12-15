package Stone.ast;

import java.util.Iterator;
import java.util.List;

public class ASTList extends ASTree {
	protected List<ASTree> children;

	public ASTList(List<ASTree> t) {
		children = t;
	}

	@Override
	public ASTree child(int i) {
		return children.get(i);
	}

	@Override
	public int numChildren() {
		return children.size();
	}

	@Override
	public Iterator<ASTree> children() {
		return children.iterator();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(')');
		String sep = "";
		for (ASTree t : children) {
			builder.append(sep);
			sep = " ";
			builder.append(t.toString());
		}
		return builder.append(')').toString();
	}
	
	@Override
	public String location() {
		for(ASTree t : children) {
			String s = t.location();
			if (s != null)
				return s;
		}
		return null;
	}

}
