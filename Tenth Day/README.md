数组（array）是几乎所有程序设计语言都会提供的一种基本的语法功能。本章将为Stone语言增加对数组的支持。与Java语言的数组相同，Stone语言的数组长度无法中途修改

### 扩展语法分析器
如果Stone语言支持数组，就能写出下面这样的程序
```
a = [2,3,4]
print a[1]
a[1] = "three"
print "a[1]: " + a[1]
b = [["one",1],["two",2]]
print b[1][0] + ": " + b[1][1]
```
数组中的元素无需保持类型一致。以第5行的代码为例，一个数组中能同时以字符串与整数作为元素。数组也能以另一个数组作为元素，数组b就是一个例子。与Java语言的数组一样，Stone语言也通过以数组作为元素的数组来表现多维数组。例如，数组b的第1个元素表示数组["two"，2]，因此b[1][0]将表示数组中第1个元素的第0个元素，即"two"

为了是程序支持数组，需要拓展语法规则。代码清单10.1是扩展后的语法规则。其中仅摘选与第7天代码清单7.1中不同的非终结符

**代码清单10.1 与数组相关的语法规则**
```
elements -> expr { "," expr }
primary -> ( "[" [ elements ] "]" | "(" expr ")" | NUMBER | IDENTIFIER | STRING ) { postfix }
postfix -> "(" [ args ] ")" | "[" expr "]"
```
首先需要为整型字面量及标识符（即变量名）等最基本的表达式构成元素添加数组字面量的支持。数组字面量由数组元素的初始值序列及两侧的中括号[]组成。数组元素的初始值序列由非终结符elements表示。字面量是一种表达式，它的计算或执行结果表示对应字符序列的值或对象。由于[2，3，4]的计算结果是一个含有元素2、3、4的数组，因此它也属于一种字面量

此外，为了让解释器能够引用数组元素，非终结符postfix的语法规则也需要做相应的修改。修改后，标识符（或数组字面量）之后要能够后接由中括号[]括起的下标。经过这一修改，postfix不仅能用于表示实参序列，还会支持数组下标

**代码清单10.2 ArrayParser.java**
```java
package Stone;
import static Stone.Parser.rule;
import javassist.gluonj.Reviser;
import Stone.ast.*;

@Reviser public class ArrayParser extends FuncParser {
	Parser elements = rule(ArrayLiteral.class).ast(expr).repeat(rule().sep(",").ast(expr));

	public ArrayParser() {
		reserved.add("]");
		primary.insertChoice(rule().sep("[").maybe(elements).sep("]"));
		postfix.insertChoice(rule(ArrayRef.class).sep("[").ast(expr).sep("]"));
	}
}
```

**代码清单10.3 ArrayLiteral.java**
```java
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
```
接下来我们根据新的语法规则来扩展语法分析器。代码清单10.2是需要使用的修改器。代码清单10.3与代码清单10.4是抽象语法树中新增的节点类，该修改器需要借助它们实现

代码清单10.2的修改器将为第7章代码清单7.2中的FuncParser类添加elements字段，并在构造函数中更新相应的处理

修改器首先在构造函数内，通过add方法向由reserved字段表示的哈希表中添加了右中括号]。于是，]不会再被识别为标识符。如果忘了添加这个符号，解释器将无法在语法分析阶段把它处理为一种分隔符，也就无法顺利运行。此外，primary与postfix也都分别通过insertChoice方法添加了对新语法规则的支持

**代码清单10.4 ArrayRef.java**
```java
package Stone.ast;
import java.util.List;

public class ArrayRef extends Postfix {

	public ArrayRef(List<ASTree> c) {
		super(c);
	}

	public ASTree index() {
		return child(0);
	}

	public String toString() {
		return "[" + index() + "]";
	}
}
```
### 仅通过修改器来实现数组
我们只要为抽象语法树中新增的节点类添加eval方法，就能让Stone语言支持数组。代码清单10.5是用于添加eval方法的修改器的具体实现

ArrayRef类的eval方法将首先对下标表达式调用eval方法，计算下标的值。之后，它将从参数value指向的object类型数组中获取与该下标对应的元素的值并返回。这里的eval方法覆盖了第7章代码清单7.7中由FuncEvaluator修改器为Postfix类添加的eva1方法

数组也可能出现在赋值表达式的左侧，我们需要覆盖BinaryExpr类的computeAssign方法来处理这种情况。该方法最初在第6章代码清单6.3中由修改器添加

代码清单10.6是解释器的启动程序，它将整合并执行修改后的程序。由于数组功能完全由修改器实现，因此这次我们不需要对解释器作修改。代码清单10.6直接使用了上一章代码清单9.9中的解释器

**代码清单10.5 ArrayEvaluator.java**
```java
package chap10;
import java.util.List;
import javassist.gluonj.*;
import Stone.ArrayParser;
import Stone.StoneException;
import Stone.ast.*;
import chap6.Environment;
import chap6.BasicEvaluator;
import chap6.BasicEvaluator.ASTreeEx;
import chap7.FuncEvaluator;
import chap7.FuncEvaluator.PrimaryEx;

@Require({FuncEvaluator.class, ArrayParser.class})
@Reviser public class ArrayEvaluator {
    @Reviser public static class ArrayLitEx extends ArrayLiteral {
        public ArrayLitEx(List<ASTree> list) { super(list); }
        public Object eval(Environment env) {
            int s = numChildren();
            Object[] res = new Object[s];
            int i = 0;
            for (ASTree t: this)
                res[i++] = ((ASTreeEx)t).eval(env);
            return res;
        }
    }
    @Reviser public static class ArrayRefEx extends ArrayRef {
        public ArrayRefEx(List<ASTree> c) { super(c); }
        public Object eval(Environment env, Object value) {
            if (value instanceof Object[]) {
                Object index = ((ASTreeEx)index()).eval(env);
                if (index instanceof Integer)
                    return ((Object[])value)[(Integer)index];
            }

            throw new StoneException("bad array access", this);
        }
    }
    @Reviser public static class AssignEx extends BasicEvaluator.BinaryEx {
        public AssignEx(List<ASTree> c) { super(c); }
        @Override
        protected Object computeAssign(Environment env, Object rvalue) {
            ASTree le = left();
            if (le instanceof PrimaryExpr) {
                PrimaryEx p = (PrimaryEx)le;
                if (p.hasPostfix(0) && p.postfix(0) instanceof ArrayRef) {
                    Object a = ((PrimaryEx)le).evalSubExpr(env, 1);
                    if (a instanceof Object[]) {
                        ArrayRef aref = (ArrayRef)p.postfix(0);
                        Object index = ((ASTreeEx)aref.index()).eval(env);
                        if (index instanceof Integer) {
                            ((Object[])a)[(Integer)index] = rvalue;
                            return rvalue;
                        }
                    }
                    throw new StoneException("bad array access", this);
                }
            }
            return super.computeAssign(env, rvalue);
        }
    }       
}
```

**ArrayRunner.java**
```java
package chap10;
import javassist.gluonj.util.Loader;
import chap7.ClosureEvaluator;
import chap8.NativeEvaluator;
import chap9.ClassEvaluator;
import chap9.ClassInterpreter;

public class ArrayRunner {
    public static void main(String[] args) throws Throwable {
        Loader.run(ClassInterpreter.class, args, ClassEvaluator.class,
                   ArrayEvaluator.class, NativeEvaluator.class,
                   ClosureEvaluator.class);
    }
}
```
运行结果如下：
![](https://s1.ax1x.com/2018/12/20/Fry360.gif#shadow)
### 第十天的思维导图
![](https://s1.ax1x.com/2018/12/20/FrcXyq.png#shadow)
