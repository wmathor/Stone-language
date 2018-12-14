package Stone;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
	private static String regexInt = "[0-9]+";
	private static String regexIdentifier = "[A-Z_a-z][A-Z_a-z0-9]*|==|<=|>=|&&|\\|\\||\\p{Punct}";
	private static String regexString = "\"(\\\\\"|\\\\\\\\|\\\\n|[^\"])*\"";
	public static String regexPat = "\\s*((//.*)|" + regexInt + "|(" + regexString + ")" + "|" + regexIdentifier + ")?";
	private Pattern pattern = Pattern.compile(regexPat);
	private ArrayList<Token> queue = new ArrayList<Token>();
	private boolean hasMore; // Mark if there are still characters
	private LineNumberReader reader; // Tracking character input stream for tracking line numbers

	public Lexer(Reader r) { // Pass in a Reader object
		hasMore = true;
		reader = new LineNumberReader(r);
	}

	public Token read() throws ParseException {
		if (fillQueue(0))
			return queue.remove(0);
		return Token.EOF;
	}

	public Token peek(int i) throws ParseException {
		if (fillQueue(i))
			return queue.get(i);
		else
			return Token.EOF;
	}

	private boolean fillQueue(int i) throws ParseException {
		while (i >= queue.size())
			if (hasMore)
				readLine();
			else
				return false;
		return true;
	}

	protected void readLine() throws ParseException {
		String line;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			throw new ParseException(e);
		}
		if (line == null) {
			hasMore = false;
			return;
		}
		int lineNo = reader.getLineNumber(); // Get the current line number
		Matcher matcher = pattern.matcher(line);
		matcher.useTransparentBounds(true).useAnchoringBounds(false); // About this code you can view the API
		int pos = 0;
		int endPos = line.length();
		while (pos < endPos) {
			matcher.region(pos, endPos); // Sets the limits of this matcher's region
			if (matcher.lookingAt()) { // Attempts to match the input sequence, starting at the beginning of the region, against the pattern
				addToken(lineNo, matcher);
				pos = matcher.end(); // Returns the offset after the last matching character
			} else
				throw new ParseException("bad token at line " + lineNo);
		}
		queue.add(new IdToken(lineNo, Token.EOL));
	}

	protected void addToken(int lineNo, Matcher matcher) {
		String m = matcher.group(1); // Returns the input subsequence captured by the given 1 during the previous match operation
		if (m != null) // if not a space
			if (matcher.group(2) == null) { // if not a comment
				Token token;
				if (matcher.group(3) != null) // is NumToken
					token = new NumToken(lineNo, Integer.parseInt(m));
				else if (matcher.group(4) != null) // is StrToken
					token = new StrToken(lineNo, toStringLiteral(m));
				else // is IdToken
					token = new IdToken(lineNo, m);
				queue.add(token);
			}
	}

	protected String toStringLiteral(String s) {
		StringBuilder sb = new StringBuilder();
		int len = s.length() - 1;
		for (int i = 1; i < len; i++) {
			char c = s.charAt(i);
			if (c == '\\' && i + 1 < len) { // Determine if it is an escape character '\'
				int c2 = s.charAt(i + 1);
				if (c2 == '"' || c2 == '\\') // \" | \\
					c = s.charAt(++i);
				else if (c2 == 'n') { // \n
					++i;
					c = '\n';
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}

	protected static class NumToken extends Token {
		private int value;

		protected NumToken(int line, int value) {
			super(line);
			this.value = value;
		}

		public boolean isNumber() {
			return true;
		}

		public String getText() {
			return Integer.toString(value);
		}

		public int getNumber() {
			return value;
		}
	}

	protected static class IdToken extends Token {
		private String text;

		protected IdToken(int line, String text) {
			super(line);
			this.text = text;
		}

		public boolean isIdentifier() {
			return true;
		}

		public String getText() {
			return text;
		}
	}

	protected static class StrToken extends Token {
		private String literal;

		StrToken(int line, String literal) {
			super(line);
			this.literal = literal;
		}

		public boolean isString() {
			return true;
		}

		public String getText() {
			return literal;
		}
	}
}
