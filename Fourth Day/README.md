
语言处理器在词法分析阶段将程序分割为单词后，将开始构造抽象语法树。抽象语法树（AST，Abstract Syntax Tree）是一种用于表示程序结构的树形结构。语法分析的主要任务是分析单词之间的关系，如判断哪些单词属于同一个表达式或语句，以及处理左右括号（单词）的配对等问题。这一阶段还会检查程序中是否含有语法错误。
### 抽象语法树的定义
我们试着用抽象语法树来表示下面的Stone语言程序
```
13 + x * 2
```
只要将这个程序理解为算式13+x&#042;2，即13与（x&#042;2）的和即可。下图是它的对象形式表示。
![](https://s1.ax1x.com/2018/12/15/FU5CRI.png#shadow)
图中的矩形表示对象。矩形上半部分显示的是类名

BinaryExpr对象用于表示双目运算表达式。双目运算指的是四则运算等一些通过左值和右值计算新值的运算

图中含有两个BinaryExpr对象，其中一个用于表示乘法运算x&#042;2，另一个用于表示加法运算13加x&#042;2

表达式x*2左侧的x是一个变量名，因此能用Name对象来表示。右侧的2是一个整型字面量，因此以NumberLitera1对象表示
![](https://s1.ax1x.com/2018/12/15/FU5ksf.png#shadow)
抽象语法树优势可能难以理解，因此可以像上图那样改写语法树，将所有的字段都写在矩形中。这样一来，各个对象与字段的关系将更加清晰

抽象语法树是一种去除了多余信息的抽象树形结构。例如，拿
```
(13+x) * 2
```
这样一个表达式来说，它与之前的例子不同，包含了括号。这段程序的抽象语法树如下图所示。叶节点和中间的节点都不含括号
![](https://s1.ax1x.com/2018/12/15/FU5Mzq.png#shadow)

13+x是乘法的左值，必须在做乘法计算之前算好。即图的抽象语法树中不含括号。因此，程序中的括号等信息不必出现在抽象语法树中。除了括号，句尾的分号等无关紧要的单词通常也不会出现在抽象语法树中。

下图是抽象语法树的节点类。为保持程序简洁，抽象语法树所有的节点类都是ASTree的子类。之后还会进一步详述。ASTLeaf类和ASTList类是AsTree的直接子类。ASTLeaf是叶节点（不含树枝的节点）的父类，ASTList是含有树枝的节点对象的父类，其他的类都是ASTList 或ASTLeaf类的子类。
![](https://s1.ax1x.com/2018/12/15/FU5NFJ.png#shadow)

NumberLitera1与Name类用于表示叶节点，BinaryExpr类用于表示含有树枝的节点，它们分别是上述两个类的子类。

只要抽象语法树的节点不是叶节点，它就含有若干个树枝，并与其他节点相连。这些与树枝另一端相连的节点称为子节点（child）

**ASTree类的主要方法**

function | mean
---|---
ASTLeaf child(int i) | 返回第i个子节点
int numChildren() | 返回子节点的个数（没有返回0）
Iterator<ASTree> children() | 返回一个用于遍历子节点的iterator

此外，ASTree类还含有1ocation方法与iterator方法。location方法将返回一个用于表示抽象语法树节点在程序内所处位置的字符串。iterator方法与children方法功能相同，它是一个适配器，在将ASTree类转为Iterable类型时将会用到该方法

ASTLeaf是叶节点对象的类，叶节点对象没有子节点，因此numChildren方法将始终返回0，children方法将返回一个与空集合关联的Iterator对象。

ASTList是非叶节点对象的类，可能含有多个子节点（即AsTree对象）。ASTList类含有一个children字段，它是一种ArrayList对象，用于保存子节点的集合

抽象语法树的叶节点不含子节点，因此ASTLeaf类没有children字段。不过它含有token字段。本书规定抽象语法树的叶节点必须与对应的单词关联。token字段保存了对应的Token对象

NumberLiteral和Name类具有名为value及name的字段。在实际实现时，这些字段并非由各个类直接定义，而是通过ASTLeaf类的token字段完成这一工作。例如，NumberLitera1含有一个表示与之对应的整型字面量的单词，这个Token对象实际由ASTLeaf类的token字段保存。NumberLiteral类的value方法将从这个token字段中取得该整型字面量并返回。Name类的实现方式也类似

BinaryExpr类同样也有1eft和right这两个字段，不过在实际实现时，这两个字段并不直接在BinaryExpr类中定义，而是通过其父类ASTList类的children字段定义。如代码清单4.6所示，BinaryExpr类不含left及right字段，而是提供了1eft与right方法。这些方法能够分别从children字段保存的ASTree对象中选取，并返回对应的左子节点与右子节点

BinaryExpr类也没有用于保存运算符的operator字段。运算符本身是独立的节点（ASTLeaf对象），作为BinaryExpr对象的子节点存在。也就是说，BinaryExpr对象含有左值、右值及运算符这三种子节点。虽然BinaryExpr类没有operator字段，却提供了operator方法。该方法将从与运算符对应的ASTLeaf对象中获取单词，并返回其中的字符串。

**代码清单4.1 ASTree.java**
```java
package Stone.ast;

import java.util.Iterator;

public abstract class ASTree implements Iterable<ASTree> {
	public abstract ASTree child(int i);

	public abstract int numChildren();

	public abstract Iterator<ASTree> children();

	public abstract String location();

	public Iterator<ASTree> iterator() {
		return children();
	}
}
```

**代码清单4.2 ASTLeaf.java**
```java
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
```

**代码清单4.3 ASTList.java**
```java
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
```

**代码清单4.4 NumberLiteral.java**
```java
package Stone.ast;

import Stone.Token;

public class NumberLiteral extends ASTLeaf {

	public NumberLiteral(Token t) {
		super(t);
	}

	public int value() {
		return token().getNumber();
	}
}
```

**代码清单4.5 Name.java**
```java
package Stone.ast;

import Stone.Token;

public class Name extends ASTLeaf {

	public Name(Token t) {
		super(t);
	}

	public String name() {
		return token().getText();
	}
}
```

**代码清单4.6 BinaryExpr.java**
```java
package Stone.ast;

import java.util.List;

public class BinaryExpr extends ASTList {
	public BinaryExpr(List<ASTree> t) {
		super(t);
	}

	public ASTree left() {
		return child(0);
	}

	public String operator() {
		return ((ASTLeaf) child(1)).token().getText();
	}
	
	public ASTree right() {
		return child(2);
	}
}
```
### BNF
**代码清单4.7 通过BNF来表示语法的例子**
```
factor:     NUMBER | "(" expression ")"
term:       factor { ("*" | "/") factor }
expression: term { ("+" | "-") term }
```
要构造抽象语法树，语言处理器首先要知道将会接收哪些单词序列，并确定希望构造出怎样的抽象语法树。通常，这些设定由程序设计语言的语法决定

语法规定了单词的组合规则，例如，双目运算表达式应该由哪些单词组成，或是if语句应该具有怎样的结构等。而程序设计语言的语法通常会包含诸如if语句的执行方式，或通过extends继承类时将执行哪些处理等规则。不过，这里仅会判断语句从哪个单词开始，中途能够出现哪些单词，又是由哪个单词结束

举例来讲，我们来看一下一条仅包含整型字面量与四则运算的表达式。代码清单4.7采用了一种名为BNF（Backus-Naur Form，巴科斯范式）的书写方式



mode | mean
---|---
{ pat } | 模式pat至少重复0次
[ pat ] | 与重复出现0次或1次的模式pat匹配
pat&#124;pat2 | 与pat1或pat2匹配
() | 将括号内视为一个完整的模式

代码清单4.7第1行的规则中，factor（因子）意指与右侧模式匹配的单词序列。`:`左侧出现的诸如factor这样的符号称为非终结符或元变量

与非终结符相对的是终结符，它们是一些事先规定好的符号，表示各种单词。在代码清单4.7中，NUMBER这种由大写字母组成的名称，以及由双引号`"`括起的诸如`(`的符号就是终结符。NUMBER表示任意一个整型字面量单词，`(`表示一个内容为左括号的单词

在代码清单4.7第1行的规则中，factor能表示NUMBER（1个整型字面量单词），或由左括号、expression（表达式）及右括号依次排列而成的单词序列。expression是一个非终结符，第3行对其下了定义。因此，由左括号、与expression匹配的单词序列，及右括号这些单词组成的单词序列能与factor模式匹配

如果：右侧的模式中仅含有终结符，BNF与正则表达式没有什么区别。此时，两者唯一的不同仅在于具体是以单词为单位检查匹配还是以字符为单位检查

另一方面，如果右侧含有类似于expression这样的非终结符，与该部分匹配的单词序列必须与另外定义的expression模式匹配。

代码清单4.7第2行中的term（项）表示一种由factor与运算符&#042;或/构成的序列，其中factor至少出现一次，运算符则必须夹在两个factor之间。由于{}括起来的模式将至少重复出现0次，因此，第2行的规则直译过来就是：与模式term匹配的内容，或是一个与factor相匹配的单词序列，或是在一个与factor相匹配的单词序列之后，由运算符*或/以及factor构成的组合再重复若干次得到的序列。

第3行的规则也是类似。expression表示一种由term（对term对应的单词序列）与运算符+或-构成的序列，其中term至少出现一次，运算符则必须夹在两个term之间。结合所有这些规则，可以发现与模式expression匹配的就是通常的四则运算表达式，只不过单词的排列顺序做了修改。也就是说，与该模式匹配的单词序列就是一个expression。反之，如果单词序列与模式expression不匹配，则会发生语法错误（syntax error）

下图与代码清单4.7表示的是相同的语法。图中的圆圈表示终结符，矩形表示非终结符。箭头的分支与合并表示模式的循环出现或“or”的含义

![](https://s1.ax1x.com/2018/12/15/FaSDXD.png#shadow)

那么接下来看一个具体的例子
```
13 + 4 * 2
```
在经过词法分析后将得到如下的单词序列
```
NUMBER "+" NUMBER "*" NUMBER
```
如下图所示，该单词序列的局部与非终结符factor及term的模式匹配，整个序列则明显与模式expression匹配。整型字面量13与factor匹配的同时也与term匹配。根据语法规则，单独的整型字面量单词能与factor匹配，单个factor又能与term匹配
![](https://s1.ax1x.com/2018/12/15/Fapl4I.png#shadow)

下面这个包含括号的表达式也能与模式expression匹配
```
(13 + 4) * 2
```
根据语法规则，括号中的`13+4`与模式expression匹配，括号括起的`(13+4)`与模式factor匹配。因此，整个乘法表达式与模式term匹配。一个term又与模式expression匹配

### 语法分析与抽象语法树
语法分析用于查找与模式匹配的单词序列。查找得到的单词序列是一个具有特定含义的单词组。分组后的单词能继续与其他单词组一起做模式匹配，组成更大的分组

下图是根据代码清单4.7中的四则运算规则，对13+4*2进行语法分析后得到的结果，以及根据该结果构造的抽象语法树。图的左上方是语法分析的结果，右下方是构造的抽象语法树，正好上下颠倒。抽象语法树中的13或+等节点表示与相应单词对应的叶节点。可以看到，语法规则中出现的终结符都是抽象语法树的叶节点。非终结符term与factor也是抽象语法树的节点
![](https://s1.ax1x.com/2018/12/15/FapI8x.png#shadow)
### 第四天的思维导图
![](https://s1.ax1x.com/2018/12/15/Fa96JI.png#shadow)
