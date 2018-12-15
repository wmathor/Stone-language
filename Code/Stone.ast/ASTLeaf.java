package Stone.ast;

import java.util.ArrayList;
import java.util.Iterator;

import Stone.Token;

public class ASTLeaf extends ASTree {
	private static ArrayList<ASTree> empty = new ArrayList<ASTree>();
	protected Token token;

	public ASTLeaf(Token token) {
		this.token = token;
	}

	@Override
	public ASTree child(int i) {
		throw new IndexOutOfBoundsException();
	}

	@Override
	public int numChildren() {
		return 0;
	}

	@Override
	public Iterator<ASTree> children() {
		return empty.iterator();
	}

	public String toString() {
		return token.getText();
	}

	@Override
	public String location() {
		return "at line " + token.getLineNumber();
	}

	public Token token() {
		return token;
	}
}
