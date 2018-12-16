只要通过语法分析得到抽象语法树，剩下的就简单了，只要从根结点开始遍历至叶节点，并计算各节点的内容即可，这就是解释器的基本实现原理

### eval方法与环境对象
要根据得到的抽象语法树来执行程序，各个语法树节点对象的类都需要具备eval方法。eval是evaluate（求值）的缩写。eval方法将计算与以该节点为根的子树对应的语句、表达式及子表达式，并返回执行结果

eval方法将递归调用该节点的子节点的eval方法，并根据它们的返回值计算自身的返回值，最后将结果返回给调用者。不同节点对返回值的计算方式不同，因此，各个节点的类需要覆盖各自的eval方法。也就是说，不同类型的节点的类，对eval方法有着不同的定义

例如，下图显示了调用`+`运算符对应节点对象的eval方法后的计算流程。下面是eval方法的简化版本（实际的方法会更复杂一些）

![](https://s1.ax1x.com/2018/12/16/FdMDVs.png#shadow)
该节点含有两个子节点，对应节点对象的eval方法将被依次调用。左侧left()对应的子节点的eval方法将返回13，右侧right()对应的子节点的eval方法将返回x*2的计算结果。将两侧eval方法的返回值相加，就能得到+运算符的计算结果。该结果将成为+节点的eval方法的返回值
```java
public Object eval(Environment env) {
    Object left = left().eval(env);
    Object right = right().eval(env);
    return (Integer) left + (Integer)right
}
```
左侧叶节点对象的eval方法如下所示
```java
public Object eval (Environment env) {
    return value();
}
```
value()将返回该对象表示的整型字面量13

通常，如果一个子节点还含有子节点，它的eval方法将递归调用其子节点的eval方法，这种调用方式类似于深度优先树节点搜索算法

Stone语言这类支持变量的程序设计语言会将环境对象（environment）作为参数传递给eval方法。环境对象指的是一种用于记录变量名称与值的对应关系的数据结构，以哈希表的形式实现，当程序中出现新的变量时，由该变量名称与初始值构成的键值对将被添加至哈希表，之后再次遇到这一变量时，程序将搜索哈希表并取得其值。如果要赋新值给该变量，程序将会把原有变量对应的值进行更新

**代码清单6.1 环境对象的接口Environment.java**
```java
package chap6;

public interface Environment {
	void put(String name, Object value);
	Object get(String name);
}
```

**代码清单6.2 环境对象的类BasicEnv.java**
```java
package chap6;
import java.util.HashMap;

public class BasicEnv implements Environment {
	protected HashMap<String,Object> values;
	public BasicEnv() {
		values = new HashMap<String,Object>();
	}
	public void put(String name, Object value) {
		values.put(name, value);
	}

	public Object get(String name) {
		return values.get(name);
	}
}
```
代码清单6.1与代码清单6.2是环境对象的实现。环境对象通过哈希表为变量的名称与值建立了对应关系。put方法用于添加新的键值对，get方法则能够以名称为键搜索哈希表

### 各种类型的eval方法
代码清单6.3总结了抽象语法树的节点类中新添加的eva1方法。所有的类共用一个父类AsTree，首先需要为这个类添加抽象方法eva1，之后各个子类再分别覆盖这一方法。eva1方法的参数是环境对象，即Environment对象。方法的返回值是一个object类型的计算结果

**代码清单6.3 新增的eval方法（BasicEvaluator.java）**
```java
package chap6;
import java.util.List;
import Stone.StoneException;
import Stone.Token;
import Stone.ast.*;
import javassist.gluonj.*;

public class BasicEvaluator {
	public static final int TRUE = 1;
	public static final int FALSE = 0;

	@Reviser
	public static abstract class ASTreeEx extends ASTree {
		public abstract Object eval(Environment env);
	}

	@Reviser
	public static class ASTListEx extends ASTList {
		public ASTListEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			throw new StoneException("cannot eval: " + toString(), this);
		}
	}

	@Reviser
	public static class NumberEx extends NumberLiteral {
		public NumberEx(Token t) {
			super(t);
		}

		public Object eval(Environment e) {
			return value();
		}
	}

	@Reviser
	public static class StringEx extends StringLiteral {
		public StringEx(Token t) {
			super(t);
		}

		public Object eval(Environment e) {
			return value();
		}
	}

	@Reviser
	public static class NameEx extends Name {
		public NameEx(Token t) {
			super(t);
		}

		public Object eval(Environment env) {
			Object value = env.get(name());
			if (value != null)
				return value;
			throw new StoneException("undefined name:" + name(), this);
		}
	}

	@Reviser
	public static class NegativeEx extends NegativeExpr {
		public NegativeEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			Object v = ((ASTreeEx) operand()).eval(env);
			if (v instanceof Integer)
				return new Integer(-((Integer) v).intValue());
			throw new stoneException("bad type for-", this);
		}
	}

	@Reviser
	public static class BinaryEx extends BinaryExpr {
		public BinaryEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			String op = operator();
			if ("=".equals(op)) {
				Object right = ((ASTreeEx) right()).eval(env);
				return computeAssign(env, right);
			} else {
				Object left = ((ASTreeEx) left()).eval(env);
				Object right = ((ASTreeEx) right()).eval(env);
				return computeOp(left, op, right);
			}
		}

		protected Object computeAssign(Environment env, Object rvalue) {
			ASTree l = left();
			if (l instanceof Name) {
				env.put(((Name) l).name(), rvalue);
				return rvalue;
			} else
				throw new stoneException("bad assignment", this);
		}

		protected Object computeop(Object left, String op, Object right) {
			if (left instanceof Integer && right instanceof Integer)
				return computeNumber((Integer) left, op, (Integer) right);
			else if (op.equals("+"))
				return String.valueOf(left) + String.valueOf(right);
			else if (op.equals("==")) {
				if (left == null)
					return right == null ? TRUE : FALSE;
				else
					return left.equals(right) ? TRUE : FALSE;
			} else
				throw new StoneException("bad type", this);
		}

		protected Object computeNumber(Integer left, String op, Integer right) {
			int a = left.intValue();
			int b = right.intValue();
			if (op.equals("+"))
				return a + b;
			else if (op.equals("-"))
				return a - b;
			else if (op.equals("*"))
				return a * b;
			else if (op.equals("/"))
				return a / b;
			else if (op.equals("%"))
				return a % b;
			else if (op.equals("=="))
				return a == b ? TRUE : FALSE;
			else if (op.equals(">"))
				return a > b ? TRUE : FALSE;
			else if (op.equals("<"))
				return a < b ? TRUE : FALSE;
			else
				throw new StoneException("bad operator", this);
		}
	}

	@Reviser
	public static class IfEx extends IfStmnt {
		public IfEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			Object c = ((ASTreeEx) condition()).eval(env);
			if (c instanceof Integer && ((Integer) c).intValue() != FALSE)
				return ((ASTreeEx) thenBlock()).eval(env);
			else {
				ASTree b = elseBlock();
				if (b == null)
					return 0;
				else
					return ((ASTreeEx) b).eval(env);
			}
		}
	}

	@Reviser
	public static class WhileEx extends WhileStmnt {
		public WhileEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			Object result = 0;
			for (;;) {
				Object c = ((ASTreeEx) condition()).eval(env);
				if (c instanceof Integer && ((Integer) c).intValue() == FALSE)
					return result;
				else
					result = ((ASTreeEx) body()).eval(env);
			}
		}
	}
}
```
环境是一个Environment对象，它将调用自身的get方法来查找变量。如果变量名不存在，就表示程序尚未定义该变量，此时系统将抛出一个异常

大部分eval方法都会在节点包含子节点时递归调用子节点的eval方法。例如，单目减法运算表达式的节点对象是一个Negative类的对象。该节点含有一个子节点，用于表示减号右侧的子表达式。该对象的operand方法能够获得这一子节点。Negative类的eval方法将会递归调用子节点的eval方法，改变返回值的正负号之后，再将该值作为自身的返回值返回

含有`=`运算符的赋值表达式是一个例外，它不会递归调用子节点的eval方法。双目运算符的节点对象是一个BinaryExpr类的对象。BinaryExpr类的eval方法在遇到`=`运算符时将做特殊处理

赋值表达式的右侧的值能够由eval方法计算得到，左侧则不行。左侧的值需要由一种名为左值（L-value）的特殊表达式计算。左值是右侧的值的赋值对象，无法通过eva1方法算得

在赋值表达式左侧不是一个变量时，Stone语言将报运行错误，反之则会通过特殊的方式计算左值。计算得到的左值将更新环境中的数据。不过，并不是说表达式中包含变量时解释器就一定会以左值形式计算该变量。在普通的表达式（例如赋值表达式右侧的子表达式）中出现变量时，解释器将调用eval方法计算该变量的值。此时调用的是Name类的eval方法。解释器将查找环境，返回与变量名对应的值

抽象语法树的节点也能表示if语句或while语句之类的语句。这类节点的eval方法将返回最后执行的代码块的计算结果，即最后调用的代码块的eval方法的返回值。我们来看一下IfStmnt类与WhileStmnt类的eva1方法。可以看到，代码块的计算结果就是最后执行的语句（或表达式）的计算结果。在Stone语言中，程序无论执行哪种类型的语句，都能得到对应的计算结果

具体来讲，以IfStmnt类的eval方法为例，它将首先调用condition方法，对返回的子节点递归调用eval方法。最终得到的返回值即是if语句中条件表达式的结算结果。根据该结果，程序将选择执行对应的代码块，并调用所执行代码块的eval方法。该eval方法的返回值是代码块中最后一条语句的计算结果，它也是IfStmnt类的eval方法的返回值

### 关于GluonJ
在GluonJ中，标有@Reviser的类称为修改器（reviser）。修改器看起来和子类很相似，实则不然，它将直接修改（revise）所继承的类的定义

代码清单6.3中，BasicEvaluator类是一个标有@Reviser的修改器。不过由于它没有继承其他类，因此没有修改任何的类。该类内部嵌套定义多个子类，这些修改器将直接修改其他的类的定义。BasicEvaluator修改器用于将内部的多个修改器打包为一个整体

BasicEvaluator类中嵌套的子类也都是标有@Reviser的修改器。这些修改器继承了其他的类，并能直接修改那些类的定义。嵌套的子类修改器必须以static方式定义。如果需要修改的类包含构造函数，修改器必须提供具有相同签名的构造函数。如果需要修改的类含有多个签名不同的构造函数，修改器必须提供同样多个构造函数

代码清单6.3中出现的第一个修改器ASTreeEx向AsTree类添加了一个Abstract方法eval。ASTreeEx中的Ex指的是extend

其他的修改器分别向AsTree类的各个子类添加了eval方法。例如，ASTListEx类是一个ASTList类的修改器。ASTListEx向ASTList类添加了一个eval方法。因此，尽管代码清单4.2中原本的AsTList类没有定义eval方法，解释器也能够对AsTList对象调用eval方法

修改器在调用由修改器添加的方法时，必须进行数据类型转换。例如，代码清单6.3中NegativeEx修改器的eval方法为了获取操作数的值，将像下面这样调用操作数的eval方法
```java
Object v = ((ASTreeEx)operand()).eval(env);
```
这里，operand方法的返回值为AsTree类型。由于AsTree类的eval方法由AsTreeEx修改器添加，因此必须像上面这样将其转换为ASTreeEx类型之后才能调用eval方法

### 执行程序
eval方法是Stone语言解释器的核心。完成了eval方法的实现之后，解释器只要读取程序并调用eval方法，就能执行Stone 语言程序

代码清单6.4是解释器的主体程序。解释器通过对话框读取程序后，词法分析器与语法分析器将构造抽象语法树，调用eva1方法来获取计算结果并显示

由于Stone语言的解释器使用了GluonJ，因此必须在启动时执行代码清单6.5中的程序。该程序将用修改器修改相关的类，最后执行解释器。代码清单6.5中Loader类的run方法将调用它的第1个参数接收的类的main方法，执行程序。run方法的第2个参数是一个运行参数，将直接传递给第1个参数收到的main方法。第3个参数是执行程序所需的修改器，它是一个可变长参数，能指定任意多个修改器。所有指定的修改器都完成修改后，main方法将被调用

**代码清单6.4 Stone语言的解释器 BasicInterpreter.java**
```java
package chap6;
import Stone.*;
import Stone.ast.ASTree;
import Stone.ast.NullStmnt;

public class BasicInterpreter {
    public static void main(String[] args) throws ParseException {
        run(new BasicParser(), new BasicEnv());
    }
    public static void run(BasicParser bp, Environment env)
        throws ParseException
    {
        Lexer lexer = new Lexer(new CodeDialog());
        while (lexer.peek(0) != Token.EOF) {
            ASTree t = bp.parse(lexer);
            if (!(t instanceof NullStmnt)) {
                Object r = ((BasicEvaluator.ASTreeEx)t).eval(env);
                System.out.println("=> " + r);
            }
        }
    }
}
```

**代码清单6.5 解释器启动程序 Runner.java**
```java
package chap6;
import javassist.gluonj.util.Loader;

public class Runner {
    public static void main(String[] args) throws Throwable {
        Loader.run(BasicInterpreter.class, args, BasicEvaluator.class);
    }
}
```

**代码清单6.6 Stone语言示例**
```
sum = 0
i = 1
while i < 10 {
    sum = sum + i
    i = i + 1
}
sum
```
程序执行结果如下图所示，在程序代码之后将显示多行计算结果。之所以会这样，是因为每一条语句都会在执行后输出结果。第3行显示的是整个while语句的计算结果。最后一行是sum的值，即1至9相加的和
![](https://s1.ax1x.com/2018/12/16/FwyeyD.gif#shadow)
### 第六天思维导图
![](https://s1.ax1x.com/2018/12/16/FwRGqO.png#shadow)
