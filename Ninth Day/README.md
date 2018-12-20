今天我们为Stone语言添加类与对象的支持
### 设计用于操作类与对象的语法
再添加的类与对象的处理功能后，下面的Stone语言就能被正确执行了
```
class Position {
    x = y = 0
    def move(nx,ny) {
        x = nx; y = ny
    }
}
p = Position.new
p.move(3, 4)
p.x = 10
print p.x + p.y
```
类名后接.new组成的代码表示创建一个对象。为简化实现，这里规定Stone语言无法定义带参数的构造函数

如果希望继承其他的类，只需在类名之后接着写上extends即可。例如，下面的代码能够定义一个及程序Position类的子类Pos3D
```
class Pos3D extends Position {
    z = 0
    def set(nx,ny,nz) {
        x = nx;y = ny;z = nz
    }
}
p = Pos3D.new
p.move(3,4)
print p.x
p.set(5,6,7)
print p.z
```
Stone语言不支持方法重载
### 实现类所需的语法规则
代码清单9.1是与类相关的语法规则修改，这里只显示了代码清单7.1和代码清单7.13的不同之处。其中，非终结符postfix与program的定义发生了变化，同时语法规则中新增了一些其他的非终结符

非终结符class_body表示由大括号{}括起的由分号或换行符分割组成的若干个member。非终结符postfix经过修改，支持基于句点`.`的方法调用与字段访问

代码清单9.2是根据代码清单9.1的语法规则更新的语法分析器程序。代码清单9.3、代码清单9.4与代码清单9.5是其中用到的类定义

**代码清单9.1 与类相关的语法规则**
```
member -> def | simple
class_body -> "{" [ member ] {(";" | EOL) [ member ]} "}"
defclass -> "class" IDENTIFIER [ "extends" IDENTIFIER ] class_body
postfix -> "." IDENTIFIER | "(" [ args ] ")"
program -> [ defclass | def | statement ] (";" | EOL)
```

**代码清单9.2 支持类的语法分析器ClassPraser.java**
```java
package Stone;
import static Stone.Parser.rule;
import Stone.ast.ClassBody;
import Stone.ast.ClassStmnt;
import Stone.ast.Dot;

public class ClassParser extends ClosureParser {
    Parser member = rule().or(def, simple);
    Parser class_body = rule(ClassBody.class).sep("{").option(member)
                            .repeat(rule().sep(";", Token.EOL).option(member))
                            .sep("}");
    Parser defclass = rule(ClassStmnt.class).sep("class").identifier(reserved)
                          .option(rule().sep("extends").identifier(reserved))
                          .ast(class_body);
    public ClassParser() {
        postfix.insertChoice(rule(Dot.class).sep(".").identifier(reserved));
        program.insertChoice(defclass);
    }
}
```
### 实现eval方法
下一步，需要为新增的抽象语法树的类添加eval方法。代码清单9.6是所需的修改器

首先，修改器为用于类定义的class语句添加了eval方法。class语句以class一词起始，它对应的非终结符是defclass，在抽象语法树中以ClassStmnt（代码清单9.4）类的形式表现。ClassStmnt类新增的eval方法将创建一个class语句定义类的名称。之后，解释器通过.new从环境中获取类的信息。例如
```
class Position { 省略 }
```
这条语句能够创建一个ClassInfo对象，该对象保存了Stone语言中Position类的定义信息。对象在创建后，将与类名Position一起添加至环境中

**代码清单9.3 ClassBody.java**
```java
package Stone.ast;
import java.util.List;

public class ClassBody extends ASTList {

	public ClassBody(List<ASTree> c) {
		super(c);
	}
}
```

**代码清单9.4 ClassStmnt.java**
```java
package Stone.ast;
import java.util.List;

public class ClassStmnt extends ASTList {

	public ClassStmnt(List<ASTree> c) {
		super(c);
	}

	public String name() {
		return ((ASTLeaf) child(0)).token().getText();
	}

	public String superClass() {
		if (numChildren() < 3)
			return null;
		else
			return ((ASTLeaf) child(1)).token().getText();
	}

	public ClassBody body() {
		return (ClassBody) child(numChildren() - 1);
	}

	public String toStirng() {
		String parent = superClass();
		if (parent == null)
			parent = "*";
		return "(class " + name() + " " + parent + " " + body() + ")";
	}
}
```

**Dot.java**
```java
package Stone.ast;
import java.util.List;

public class Dot extends Postfix {

	public Dot(List<ASTree> c) {
		super(c);
	}

	public String name() {
		return ((ASTLeaf) child(0)).token().getText();
	}

	public String toString() {
		return "." + name();
	}
}
```

接下来需要添加一个新的eval方法，使程序能够通过句点.进行实现方法调用与字段访问。相应的抽象语法树是一个Dot类（代码清单9.5）。Dot类是Postfix的一个子类。Dot类的eval方法由PrimaryExpr类的evalSubExpr方法直接调用，PrimaryExpr类的eval方法会通过evalsubExpr方法来获取调用结果

修改器向Dot类添加的eval方法需要两个参数。其中一个是环境，另一个是句点左侧的计算结果。如果句点右侧是new，句点表达式将用于创建一个对象。其中句点左侧是需要创建的类，它的计算结果是一个ClassInfo对象。eval方法将根据该ClassInfo对象提供的信息创建对象并返回

代码清单9.6中的AssignEx修改器实现了字段赋值功能。该修改器继承于BinaryEx，同时，BinaryEx本身也是一个修改器（第6章代码清单6.3）。AssignEx修改器将修改BinaryExpr类。AssignEx修改器覆盖了由BinaryEx修改器添加的computeAssign方法，使字段的赋值功能得以实现

经过AssignEx修改器修改的computeAssign方法将在赋值运算的左侧为一个字段时调用stoneobject的write方法，执行赋值操作。如果不是，它将通过super调用原先的computeAssign方法

在为字段赋值时必须注意的是，赋值运算的左侧并不一定总是单纯的字段名称。例如，字段可以通过下面的方式表现
```
table.get().next.x = 3
```
解释器将首先调用变量table所指对象的get方法，再将返回对象中next字段指向的对象包含的字段x赋值为3。其中，仅有.x将计算运算符的左值并赋值，table.get().next仍以通常方式计算最右侧的值。computeAssign方法通过内部的evalsubExpr方法执行这一计算。赋值给变量t的返回值同时也是上面例子中table.get().next的右值计算结果

**代码清单9.6 ClassEvaluator.java**
```java
package chap9;
import java.util.List;
import Stone.StoneException;
import Stone.ast.*;
import chap6.BasicEvaluator.ASTreeEx;
import chap6.BasicEvaluator;
import chap6.Environment;
import chap7.FuncEvaluator;
import chap7.FuncEvaluator.EnvEx;
import chap7.FuncEvaluator.PrimaryEx;
import chap7.NestedEnv;
import chap9.StoneObject.AccessException;
import javassist.gluonj.*;

@Require(FuncEvaluator.class)
@Reviser public class ClassEvaluator {
	@Reviser public static class ClassStmntEx extends ClassStmnt {
		public ClassStmntEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			ClassInfo ci = new ClassInfo(this, env);
			((EnvEx) env).put(name(), ci);
			return name();
		}
	}

	@Reviser public static class ClassBodyEx extends ClassBody {
		public ClassBodyEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			for (ASTree t : this)
				((ASTreeEx) t).eval(env);
			return null;
		}
		
		@Reviser public static class DotEx extends Dot {
			public DotEx(List<ASTree> c) {
				super(c);
			}
			
			public Object eval(Environment env,Object value) {
				String member = name();
				if (value instanceof ClassInfo) {
					if ("new".equals(member)) {
						ClassInfo ci = (ClassInfo)value;
						NestedEnv e = new NestedEnv(ci.environment);
						StoneObject so = new StoneObject(e);
						e.putNew("this", so);
						initObject(ci,e);
						return so;
					}
				} else if (value instanceof StoneObject) {
					try {
						return ((StoneObject)value).read(member);
					} catch (AccessException e) {}
				}
				throw new StoneException("bad member access: " + member,this);
			}
			
			protected void initObject(ClassInfo ci,Environment env) {
				if (ci.superClass() != null)
					initObject(ci.superClass(),env);
				((ClassBodyEx)ci.body()).eval(env);
			}
		}
		@Reviser public static class AssignEx extends BasicEvaluator.BinaryEx {
			public AssignEx(List<ASTree> c) {
				super(c);
			}
			
			protected Object computeAssign(Environment env,Object rvalue) {
				ASTree le = left();
				if (le instanceof PrimaryExpr) {
					PrimaryEx p = (PrimaryEx) le;
					if (p.hasPostfix(0) && p.postfix(0) instanceof Dot) {
						Object t = ((PrimaryEx)le).evalSubExpr(env, 1);
						if (t instanceof StoneObject)
							return setField((StoneObject)t,(Dot)p.postfix(0),rvalue);
					}
				}
				return super.computeAssign(env, rvalue);
			}
			
			protected Object setField(StoneObject obj,Dot expr,Object rvalue) {
				String name = expr.name();
				try {
					obj.write(name,rvalue);
					return rvalue;
				} catch (AccessException e) {
					throw new StoneException("bad member access " + location() + ": " + name);
				}
			}
		}
	}
}
```

**代码清单9.7 ClassInfo.java**
```java
package chap9;
import Stone.StoneException;
import Stone.ast.ClassBody;
import Stone.ast.ClassStmnt;
import chap6.Environment;

public class ClassInfo {
	protected ClassStmnt definition;
	protected Environment environment;
	protected ClassInfo superClass;

	public ClassInfo(ClassStmnt cs, Environment env) {
		definition = cs;
		environment = env;
		Object obj = env.get(cs.superClass());
		if (obj == null)
			superClass = null;
		else if (obj instanceof ClassInfo)
			superClass = (ClassInfo) obj;
		else
			throw new StoneException("unkonw super class: " + cs.superClass(), cs);
	}
	
	public String name() {
		return definition.name();
	}
	
	public ClassInfo superClass() {
		return superClass;
	}
	
	public ClassBody body() {
		return definition.body();
	}
	
	public Environment environment() {
		return environment;
	}
	
	public String toString() {
		return "<class " + name() + ">";
	}
}
```

**代码清单9.8 StoneObject.java**
```java
package chap9;
import chap6.Environment;
import chap7.FuncEvaluator.EnvEx;

public class StoneObject {
	public static class AccessException extends Exception {
	}

	protected Environment env;

	public StoneObject(Environment e) {
		env = e;
	}

	public String toString() {
		return "<object:" + hashCode() + ">";
	}

	public Object read(String member) throws AccessException {
		return getEnv(member).get(member);
	}

	public void write(String member, Object value) throws AccessException {
		((EnvEx) getEnv(member)).putNew(member, value);
	}

	protected Environment getEnv(String member) throws AccessException {
		Environment e = ((EnvEx) env).where(member);
		if (e != null && e == env)
			return e;
		else
			throw new AccessException();
	}
}
```
### 通过闭包表示对象
从实现的角度来看，如何设计StoneObject对象的内部结构才是最重要的。也就是说，如何通过Java语言的对象来表现Stone语言的对象。其实，实现的方式多种多样，我们将利用环境能够保存字段值的特性来表示对象

StoneObject对象主要应保存Stone语言中对象包含的字段值，可以说它是字段名称与字段值的对应关系表。从这个角度来看，环境作为变量名称与变量值的对应关系表，与对象的作用非常类似

如果将对象视作一种环境，就很容易实现对该对象自身（也就是Java语言中this指代的对象）的方法调用与字段访问。方法调用与字段访问可以通过this.x实现，其中，指代自身的this.能够省略。下面是一个例子
```
class Positon {
    x = y = 0
    def move(nx,ny) {
        x = ny;y = ny
    }
}
```
move方法内的x乍看是一个局部变量，其实它是this.x的省略形式，表示x字段。这类x的实现比较麻烦。如果将move方法的定义视作函数定义，x与y都属于自由变量（自由变量指的是函数参数及局部变量以外的函数）。参数nx与ny则是局部变量

如果方法内部存在x这样的自由变量，该变量就必须指向（绑定）在方法外部定义的字段。这与闭包的机制类似。例如，下面的函数position将返回一个闭包
```
def position () {
    x = y = 0
    fun (nx,ny) {
        x = ny;y = ny
    }
}
```
此时，position函数的局部变量x将赋值给返回的闭包中的变量x（与x绑定）。对比两者即可发现，闭包与方法都会将内部的变量名与外部的变量（字段）绑定

在通过.new创建新的StoneObject对象时，解释器将首先创建新的环境。StoneObject对象将保存该环境，并向该环境添加由名称this与自身组成的键值对

![](https://s1.ax1x.com/2018/12/20/FrafER.png#shadow)

### 运行包含类的程序
至此，Stone语言已经可以支持类与对象的使用。与之前一样，最后将要介绍的是解释器主体程序与相应的启动程序。参见代码清单9.9与代码清单9.10

**代码清单9.9 ClassInterperter.java**
```java
package chap9;
import Stone.ClassParser;
import Stone.ParseException;
import chap6.BasicInterpreter;
import chap7.NestedEnv;
import chap8.Natives;

public class ClassInterpreter extends BasicInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new ClassParser(), new Natives().environment(new NestedEnv()));
	}
}
```

**代码清单9.10 ClassRunner.java**
```java
package chap9;
import chap7.ClosureEvaluator;
import chap8.NativeEvaluator;
import javassist.gluonj.util.Loader;

public class ClassRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(ClassInterpreter.class, args, ClassEvaluator.class,NativeEvaluator.class,ClosureEvaluator.class);
	}
}
```
运行效果如下：
![](https://s1.ax1x.com/2018/12/20/FrBtGq.gif#shadow)
### 第九天的思维导图
![](https://s1.ax1x.com/2018/12/20/FrDEOU.png#shadow)
