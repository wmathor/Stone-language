语言处理器的第一个组成部分是词法分析器（lexer）。程序的源代码最初只是一长串字符串，这样的字符串很难处理，语言处理器通常会首先将字符串中的字符以单词为单位分组，切割成多个子字符串。这就是词法分析
### Token对象
下面是某个程序中的一行代码
```
while i < 10 {
```
词法分析会把它拆分为下面这样的字符串
```
"while" "i" "<" "10" "{"
```
这句代码被分割为了5个字符串。通常把词法分析的结果称为单词（token）

词法分析将筛选出程序的解释与执行必须的成分。单词之间的空白或注释都会在这一阶段被去除，例如
```
i = i + 1 // increment
i=i+1
```
这两行代码词法分析的结果相同，都是下面5个单词：
```
"i" "=" "i" "+" "1"
```
经过词法分析之后，程序员便无需再处理代码的注释，也不用考虑单词之间是否含有空白符。

分割后得到的单词是用代码清单3.1中的Token对象表示，这种对象除了记录该单词对应的字符串，还会保存单词的类型、单词所处位置的行号等信息

Stone语言含有标识符、整型字面量和字符串字面量这三种类型的单词，每种单词都定义了对应的Token类的子类。每种子类都覆盖了Token类的isIdentifier（如果是标识符则为真）、isNumber（如果是整型字面量则为真）及isString（如果是字符串字面量则为真）方法，并根据具体类型返回相应的值

此外，Stone语言还定义了一个特别的单词Token.EOF（end of file），用于表示程序结束。类似的还有Token.EOL（end of line），用于表示换行符

**代码清单3.1 Token.java**
```java
package Stone;

public abstract class Token { // abstract class
	public static final Token EOF = new Token(-1) {}; // end of file
	public static final String EOL = "\\n";           // end of line
	private int lineNumber;                           // token number

	protected Token(int line) {
		lineNumber = line;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public boolean isIdentifier() {
		return false;
	}

	public boolean isNumber() {
		return false;
	}

	public boolean isString() {
		return false;
	}

	public int getNumber() {
		throw new StoneException("not number token");
	}

	public String getText() {
		return "";
	}
}
```
**代码清单3.2 StoneException.java**
```java
package Stone;

import Stone.ast.ASTree;

public class StoneException extends RuntimeException {
	public StoneException(String m) {
		super(m);
	}

	public StoneException(String m, ASTree t) { // Overload function
		super(m + " " + t.location);
	}
}
```
#### 标识符（identifier）
标识符指的是变量名、函数名或类名等名称。此外，`+`或`-`等运算符及括号也属于标识符。**标点符号与保留字也都作为标识符处理。**（很重要）

#### 整型字面量（integer literal）
读者可能会把它与程序执行过程中赋值给变量的整数值混同，因此这里使用整型字面量的名称，用于指代整数值的字符序列。

例如，Java语言就支持0x1f这样的16进制数表示。这种4个字符组成的字符串也是整型字面量。用一个整数值来表示的话，即31。
#### 字符串字面量（String literal）
字符串字面量（string literal)是一串用于表示字符串的字符序列。与Java
等语言一样，被双引号（"）括起来的字符序列就是一个字符串字面量。例如，字符串字面量`"Java"`表示的就是字符串值Java

双引号之间可以使用`\n`、`\"`与`\\`这三种类型的转义字符。它们分别表示换行符、双引号符和反斜杠

### 通过正则表达式定义单词
借助正则表达式库能简单地实现词法分析器。下表列出的记号在大多数情况下都能使用。例如，`.*\.java`指的是以`.java`结束的任意长度的字符串模式。`.*\.`由两部分组成，`.*`表示由任意字符组成的任意长度的字符串模式，`\.`表示与句点字符相匹配的字符串模式。`(java|javax)\..*`则表示由`java.`或`javax.`起始的任意长度的字符串模式

**表3.1 正则表达式的元字符**

元字符 | 含义
---|---
. | 与任意字符匹配
[0-9] | 与0至9中的某个数字匹配
[^0-9] | 与0至9这些数字之外的的某个字符匹配
pat* | 模式pat至少重复出现0次
pat+ | 模式pat至少重复出现1次
pat? | 模式pat出现0次或1次
pat1\|pat2 | 与模式pat1或模式pat2匹配
() | 将括号内视为一个完整的模式
\c | 与单个字符c（元字符*或.等）匹配

首先来定义整型字面量
```
[0-9]+
```
然后定义标识符
```
[A-Z_a-z][A-Z_a-z0-9]*
```
以字母、下划线开头，之后仅包含有字母、数字、下划线的就是标识符的定义规则

前面我们说Stone语言的标识符包括各类符号，因此下面才是真正完整的正则表达式
```
[A-Z_a-z][A-Z_a-z0-9]*|==|<=|>=|&&|\|\||\p{Punct}
```
最后的`\p{Punct}`表示与任意一个符号字符匹配。模式`\|\|`将会匹配`||`。由于`|`是正则表达式的元字符，因此在使用时必须添加反斜线转义

最后需要定义的是字符串字面量。由于不得不处理各种转义字符，字符串字面量的定义略微复杂
```
"(\\"|\\\\|\\n|[^"])*"
```
首先，从整体上看，这是一个"(pat)*"形式的模式，即双引号内是一个与pat重复出现至少0次的结果匹配的字符串。其中，模式pat与`\"`、`\\`、`\n`匹配，同时也与除`"`之外任意一个字符匹配
### 借助java.util.regex设计词法分析器
例如，下面的字符串
```
https://wmathor.com/
```
与正则表达式
```
http://(.+)/
```
匹配。Java语言能够获取与括号中的模式匹配的字符串`wmathor.com`。如果模式包含多个括号，各个括号内的子字符串都能被分别取得。

每一对左右括号都对应了与其包围的模式相匹配的字符串。要利用这一功能设计词法分析器，首先要准备一个下面这样的正则表达式
```
\s*((//.*)|(pat1)|(pat2)|pat3)?
```
其中，pat1是与整型字面量匹配的正则表达式，pat2与字符串字面量匹配，pat3与标识符匹配。起始的`\s`与空字符匹配，`\s*`与0个或以上的空字符匹配。模式`//.*`匹配由`//`开始的任意长度的字符串，用于匹配注释。于是，上述正则表达式能匹配**任意个空白字符及连在其后的注释、整型字面量、字符串字面量或标识符**。又因它以`?`结尾，所以仅由任意多个空白符组成的字符串也能与该模式匹配

执行词法分析时，语言处理器会逐行读取源代码，从各行开头起检查内容是否与该正则表达式匹配，并在检查完后获取与正则表达式括号内的模式相匹配的字符串

如果匹配的字符串是一句注释，则对应于左起第2个左括号，从第三个左括号起对应的都是null。如果匹配的字符串是一个整型字面量，则对应左起第3个左括号，第2个和第4个左括号与null对应......

只要像这样检查一下哪一个括号对应的不是null，就能知道行首出现的是哪种类型的单词。之后再继续用正则表达式匹配剩余部分，就能得到下一个单词。不断重复该过程，词法分析器就能获得由源代码分割而得的所有单词

代码清单3.3与代码清单3.4是一个实际的词法分析程序。Lexer类就是一个词法分析器。Lexer对象的构造函数接受一个java.io.Reader对象，它能根据需要逐行读取源代码

Java语言的字符串字面量中，反斜杠与双引号必须分别以`\\`与`\"`的形式转义。因此，字符串中将包含大量的反斜杠

read与peek是Lexer中主要的两个方法。read方法可以从源代码头部开始逐一获取单词。read方法返回的是一个新的单词

peek方法则用于预读。peek(i)将返回read方法即将返回的单词之后的第i个单词。通过peek方法，词法分析器就能事先知道在调用read方法时将获得什么单词

如果所有单词都已读取，read方法和peek方法都将返回Token.EOF。这是一个特殊的Token对象，用于表示程序结束

在词法分析后需要执行的是语法分析。语法分析将一边获取单词一边构造抽象语法树，在中途发现构造有误时，需要退回若干个单词，重新构造语法树，为了支持这种操作，语言处理器必须要能够取消之前的几次read方法调用，并还原先前的结果，不过这样执行效率会受到影响，因此这里准备了peek方法

peek方法可以实现获知之后将会取得的单词，以此避免撤销抽象语法树的构造。当遇到分支路线时，不是先随意选取一条，行不通再原路返回改走另一条，而是险费一番周折，判断前路是否正确，在确信没有问题时才真正继续

要使用peek方法，词法分析器需要在读取代码并获取单词后，将这些单词暂时保存在一个名为queue的ArrayList中。之后peek与read方法会根据需要从中取值并返回。由read方法读取的单词会从queue中删除

readLine方法是实际从每一行中读取单词的方法。由于正则表达式已经实现编译为Pattern对象，因此能调用matcher方法来获得一个用于实际检查匹配的Matcher对象。词法分析器一边通过region方法限定该对象检查匹配的范围，以便通过lookingAt方法在检查范围内进行正则表达式匹配。之后，词法分析器将使用group方法来获取与各个括号对应的子字符串。end方法用于取得匹配部分的结束位置，词法分析器将从那里开始继续执行下一次lookingAt方法调用

代码清单3.3最后的NumToken、IdToken、StrToken类是Token类的子类。他们分别对应不同类型的单词

**代码清单3.3 词法分析器Lexer.java**
```java
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
```
**代码清单3.4 异常ParseException.java**
```java
package Stone;

import java.io.IOException;

public class ParseException extends Exception {
	public ParseException(Token t) {
		this("", t);
	}

	public ParseException(String msg, Token t) {
		super("syntax error around " + location(t) + ". " + msg);
	}

	private static String location(Token t) {
		if (t == Token.EOF)
			return "the last line";
		return "\"" + t.getText() + "\" at line " + t.getLineNumber();
	}

	public ParseException(IOException e) {
		super(e);
	}

	public ParseException(String msg) {
		super(msg);
	}
}
```
### 词法分析器试运行
今天最后我们将尝试运行由代码清单3.3的Lexer类实现的词法分析器。相应的main方法见代码清单3.5。他将对输入的字符串做词法分析，并逐行显示分析得到的每一个单词
![](https://s1.ax1x.com/2018/12/14/FU0bFS.gif#shadow)

代码清单3.6中的CodeDialog对象是Lexer类构造函数中的参数。CodeDialog是java.io.Reader的子类。Lexer在调用read方法从该对象中读取字符串时，界面上将显示一个对话框，用户输入的文本将成为read方法的返回值。其中使用到的Java Swing界面并不是重点，读者无需了解，照做就行，

**代码清单3.5 LexerRunner.java**
```java
package chap3;

import Stone.*;

public class LexerRunner {
	public static void main(String[] args) throws ParseException {
		Lexer l = new Lexer(new CodeDialog());
		for (Token t; (t = l.read()) != Token.EOF;)
			System.out.println("=> " + t.getText());
	}
}
```

**代码清单3.6 CodeDialog.java**
```java
package Stone;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class CodeDialog extends Reader {
	private String buffer = null;
	private int pos = 0;

	public int read(char[] cbuf, int off, int len) throws IOException {
		if (buffer == null) {
			String in = showDialog();
			if (in == null)
				return -1;
			else {
				print(in);
				buffer = in + "\n";
				pos = 0;
			}
		}

		int size = 0;
		int length = buffer.length();
		while (pos < length && size < len)
			cbuf[off + size++] = buffer.charAt(pos++);
		if (pos == length)
			buffer = null;
		return size;
	}

	protected void print(String s) {
		System.out.println(s);
	}

	public void close() throws IOException {
	}

	protected String showDialog() {
		JTextArea area = new JTextArea(20, 40);
		JScrollPane pane = new JScrollPane(area);
		int result = JOptionPane.showOptionDialog(null, pane, "Input", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, null, null);
		if (result == JOptionPane.OK_OPTION)
			return area.getText();
		return null;
	}

	public static Reader file() throws FileNotFoundException {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
			return new BufferedReader(new FileReader(chooser.getSelectedFile()));
		throw new FileNotFoundException("no file specified");
	}
}
```
### 第三天的思维导图
![](https://s1.ax1x.com/2018/12/14/FUDKun.png#shadow)
