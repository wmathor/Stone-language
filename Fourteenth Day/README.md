前一天我们借助Java语言设计了一种专用的虚拟机，用于执行中间代码。从内部来看，该虚拟机通过Java语言的Object类型来表示所有类型的值，整数也将由Integer对象表现。不可否认，这是程序执行速度较慢的一个重要原因

今天我们将利用静态数据类型，尽可能以int类型的值来表示整数值。同时，我们将不再使用专用的虚拟机，而直接使用Java虚拟机。抽象语法树需要预先转换为Java二进制代码，不过，由于整数改为int表示，转换得到的Java二进制代码执行效率也会较高
### 指定变量类型
支持静态数据类型的程序设计语言特点是，它需要在声明变量与参数的同时指定它们的数据类型。如果语言支持类型推论，一部分甚至大部分的类型指定就能省略。不过，能省略并不表示它们就不需要指定数据类型

静态语言有一些缺点。例如，即使有需要，数据类型不同的变量值之间也无法相互赋值，而日某此恋量的类型可能较为复杂，不易理解。不过，静态类型语言也有一些重要的优点
- 通过数据类型检查，它能在一定程度上确保程序的正确性
- 静态数据类型信息有助于提高程序的执行速度

我们先来改进Stone语言的语法，使它能够支持数据类型声明。首先增加的是var语句。它用于定义一个新的变量，并指定该变量的初始值与数据类型
```
var x : Int = 7
```
上面的语句对变量x做了定义，它是一个Int类型的变量，初始值为7.需要注意的是，变量声明时初始值不得省略。变量名之后跟有冒号与数据类型都可以省略
```
var x = 7
```
规定，var语句支持声明Int、String与Any三种类型，Int类型表示整数，String类型表示字符串。Any同时包含整数与字符串这两种类型。也就是说，一个整数值既可以由Int表示，也能由Any类型表示，字符串也是如此

Any类型的变量能够被赋以Int或string类型的值，反之则不行

虽然Any类型也包含整数，但它只支持有限的算术操作。Any类型的值只能进行+运算。除此以外，-（减法）等运算都无法用于Any类型的值。例如，对于Any类型的变量x，即使它的值为整数3，表达式x-1依然会引起数据类型错误。这是因为，变量x属于Any类型并不能确保它的值一定是一个整数

除了var语句，我们还将为函数定义语句添加参数及返回值的数据类型指定功能。例如
```
def inc(n: int) : int {
    n + 1
}
```
函数inc接收一个Int类型的参数n，并返回一个Int类型的返回值。右括号）之后跟着的返回值的类型。与var语句类似，冒号：与后接的数据类型名称用于指定参数或返回值的类型，它们同样能够省略

代码清单14.1列出了对Stone语法规则的一些修改，经过这些修改，Stone语言将能支持上述数据类型指定功能。与var语句对应的非终结符是variable。同时，我们需要为statement增加variable这一可能情况。此外，param与def的定义也需要修改，以支持非终结符type_tag

代码清单14.2是与修改后的语法规则对应的语法分析器程序。代码清单14.3与代码清单14.4是新定义的抽象语法树节点的对象类型

为了执行新增的var语句，我们通过代码清单14.5所示的修改器为Varstmnt类添加了eva1方法

**代码清单14.1 与数据类型相关的语法规则**
```
type_tag -> ":" IDENTIFIER
variable -> "var" IDENTIFIER [ type_tag ] "=" expr
param -> IDENTIFIER [ type_tag ]
def -> "def" IDENTIFIER param_list [ type_tag ] block
statment -> variable | "if" ... | "while" ... | simple
```

**代码清单14.2 TypedParser.java**
```java
package Stone;
import static Stone.Parser.rule;
import Stone.ast.*;

public class TypedParser extends FuncParser {
	Parser typeTag = rule(TypeTag.class).sep(":").identifier(reserved);
	Parser variable = rule(VarStmnt.class).sep("var").identifier(reserved).maybe(typeTag).sep("=").ast(expr);

	public TypedParser() {
		reserved.add(":");
		param.maybe(typeTag);
		def.reset().sep("def").identifier(reserved).ast(paramList).maybe(typeTag).ast(block);
		statement.insertChoice(variable);
	}
}
```

**代码清单14.3 VarStmnt.java**
```java
package Stone.ast;
import java.util.List;

public class VarStmnt extends ASTList {
	public VarStmnt(List<ASTree> c) {
		super(c);
	}

	public String name() {
		return ((ASTLeaf) child(0)).token().getText();
	}

	public TypeTag type() {
		return (TypeTag) child(1);
	}

	public ASTree initializer() {
		return child(2);
	}

	public String toString() {
		return "(var " + name() + " " + type() + " " + initializer() + ")";
	}
}
```

**代码清单14.4 TypeTag.java**
```java
package Stone.ast;
import java.util.List;

public class TypeTag extends ASTList {
	public static final String UNDEF = "<Undef>";

	public TypeTag(List<ASTree> c) {
		super(c);
	}

	public String type() {
		if (numChildren() > 0)
			return ((ASTLeaf) child(0)).token().getText();
		else
			return UNDEF;
	}

	public String toString() {
		return ":" + type();
	}
}
```

**代码清单14.5 TypedEvaluator.java**
```java
package chap14;
import java.util.List;
import javassist.gluonj.*;
import Stone.ast.*;
import chap11.EnvOptimizer;
import chap11.Symbols;
import chap11.EnvOptimizer.ASTreeOptEx;
import chap6.Environment;
import chap6.BasicEvaluator.ASTreeEx;

@Require(EnvOptimizer.class)
@Reviser
public class TypedEvaluator {
	@Reviser
	public static class DefStmntEx extends EnvOptimizer.DefStmntEx {
		public DefStmntEx(List<ASTree> c) {
			super(c);
		}

		public TypeTag type() {
			return (TypeTag) child(2);
		}

		@Override
		public BlockStmnt body() {
			return (BlockStmnt) child(3);
		}

		@Override
		public String toString() {
			return "(def " + name() + " " + parameters() + " " + type() + " " + body() + ")";
		}
	}

	@Reviser
	public static class ParamListEx extends EnvOptimizer.ParamsEx {
		public ParamListEx(List<ASTree> c) {
			super(c);
		}

		@Override
		public String name(int i) {
			return ((ASTLeaf) child(i).child(0)).token().getText();
		}

		public TypeTag typeTag(int i) {
			return (TypeTag) child(i).child(1);
		}
	}

	@Reviser
	public static class VarStmntEx extends VarStmnt {
		protected int index;

		public VarStmntEx(List<ASTree> c) {
			super(c);
		}

		public void lookup(Symbols syms) {
			index = syms.putNew(name());
			((ASTreeOptEx) initializer()).lookup(syms);
		}

		public Object eval(Environment env) {
			Object value = ((ASTreeEx) initializer()).eval(env);
			((EnvOptimizer.EnvEx2) env).put(0, index, value);
			return value;
		}
	}
}
```
### 通过数据类型检查发现错误
为了实现数据类型检查功能，我们要为抽象语法树的节点类添加typeCheck方法。该方法能够对数据类型进行检查，它与用于执行程序的eval方法非常类似，都会从抽象语法树的根节点开始递归调用自身，完成整棵语法树的遍历

以x-2为例，调用根节点对象的typeCheck方法将返回该表达式的数据类型。变量x的数据类型由typeCheck方法的数据类型环境参数决定。eval方法的环境参数是由变量名与对应的值组成的键值对，typeCheck方法的数据类型环境则是由变量名与对应的数据类型组成的键值对

类型指派规则看似复杂，其实它的核心是怎样根据各条表达式的子表达式类型，计算该表达式自身的类型。正如eval方法实现了表达式值的计算，typeCheck方法将实现对各个抽象法树节点类的数据类型的计算

例如，对于单目减法运算表达式-(x&#042;2)，其子表达式(x&#042;2)是-运算符的操作数，必须为Int类型。于是，整个单目运算表达式也将是Int类型。这就是类型指派规则

双目运算表达式的类型指派规则稍有些复杂。首先，如果运算符两侧的子表达式都是Int类型，整个双目运算表达式也将是Int类型。对于`+`表达式，如果两侧都是String类型，整个表达式则是String类型，否则为Any类型。这使`+`运算符能够用于字符串连接运算。还有一种特殊情况是`=`运算符。对于`=`运算符，如果左右两侧的类型一致，整个赋值表达式的类型将与子表达式相同。如果左侧是一个新出现的变量，尚无特定的数据类型，该变量将被指定为与右侧相同的类型

代码清单14.6是用于添加typeCheck方法的修改器。代码清单14.7是用于表示数据类型环境的TypeEnv类的定义。typeCheck方法在遇到类型错误时将抛出TypeException异常。代码清单14.8是该异常的定义

代码清单14.9中，TypeInfo类的对象表示数据类型。该类定义了ANY、INT与STRING这三个static字段。它们都是TypeInfo类型的值，表示各自对应的数据类型。此外，该类还定义了UnknownType与Functionrype这两个嵌套子类。前者表示程序省略了类型指定，并暂且采用了与ANY相同的实现逻辑

第二个嵌套类FunctionType用于表示函数的类型。函数的类型通过参数序列的类型与返回值的类型表现。例如，假设某个函数将接收Int类型与Any类型的参数，并返回string类型的返回值。这样一来，我们就可以称它是一个“依次接收Int与Any类型的参数且返回值为string类型”的函数

抽象语法树各节点类的typeCheck方法在计算类型时，将首先递归调用子表达式的typeCheck方法。如果没有子表达式，则调用TypeInfo对象的assertSubtypeOf方法，确认它是否满足类型指派规则的前提条件。例如，在检查由=运算符构成的赋值表达式时，typeCheck方法将像下面这样，对表示左侧类型的type与表示右侧类型的valueType调用assertSubtypeOf方法

**代码清单14.6 TypeChecker.java**
```java
package chap14;
import java.util.List;
import chap7.FuncEvaluator;
import chap11.EnvOptimizer;
import Stone.Token;
import static javassist.gluonj.GluonJ.revise;
import Stone.ast.*;
import javassist.gluonj.*;

@Require(TypedEvaluator.class)
@Reviser
public class TypeChecker {
	@Reviser
	public static abstract class ASTreeTypeEx extends ASTree {
		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			return null;
		}
	}

	@Reviser
	public static class NumberEx extends NumberLiteral {
		public NumberEx(Token t) {
			super(t);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			return TypeInfo.INT;
		}
	}

	@Reviser
	public static class StringEx extends StringLiteral {
		public StringEx(Token t) {
			super(t);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			return TypeInfo.STRING;
		}
	}

	@Reviser
	public static class NameEx2 extends EnvOptimizer.NameEx {
		protected TypeInfo type;

		public NameEx2(Token t) {
			super(t);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			type = tenv.get(nest, index);
			if (type == null)
				throw new TypeException("undefined name: " + name(), this);
			else
				return type;
		}

		public TypeInfo typeCheckForAssign(TypeEnv tenv, TypeInfo valueType) throws TypeException {
			type = tenv.get(nest, index);
			if (type == null) {
				type = valueType;
				tenv.put(0, index, valueType);
				return valueType;
			} else {
				valueType.assertSubtypeOf(type, tenv, this);
				return type;
			}
		}
	}

	@Reviser
	public static class NegativeEx extends NegativeExpr {
		public NegativeEx(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			TypeInfo t = ((ASTreeTypeEx) operand()).typeCheck(tenv);
			t.assertSubtypeOf(TypeInfo.INT, tenv, this);
			return TypeInfo.INT;
		}
	}

	@Reviser
	public static class BinaryEx extends BinaryExpr {
		protected TypeInfo leftType, rightType;

		public BinaryEx(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			String op = operator();
			if ("=".equals(op))
				return typeCheckForAssign(tenv);
			else {
				leftType = ((ASTreeTypeEx) left()).typeCheck(tenv);
				rightType = ((ASTreeTypeEx) right()).typeCheck(tenv);
				if ("+".equals(op))
					return leftType.plus(rightType, tenv);
				else if ("==".equals(op))
					return TypeInfo.INT;
				else {
					leftType.assertSubtypeOf(TypeInfo.INT, tenv, this);
					rightType.assertSubtypeOf(TypeInfo.INT, tenv, this);
					return TypeInfo.INT;
				}
			}
		}

		protected TypeInfo typeCheckForAssign(TypeEnv tenv) throws TypeException {
			rightType = ((ASTreeTypeEx) right()).typeCheck(tenv);
			ASTree le = left();
			if (le instanceof Name)
				return ((NameEx2) le).typeCheckForAssign(tenv, rightType);
			else
				throw new TypeException("bad assignment", this);
		}
	}

	@Reviser
	public static class BlockEx extends BlockStmnt {
		TypeInfo type;

		public BlockEx(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			type = TypeInfo.INT;
			for (ASTree t : this)
				if (!(t instanceof NullStmnt))
					type = ((ASTreeTypeEx) t).typeCheck(tenv);
			return type;
		}
	}

	@Reviser
	public static class IfEx extends IfStmnt {
		public IfEx(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			TypeInfo condType = ((ASTreeTypeEx) condition()).typeCheck(tenv);
			condType.assertSubtypeOf(TypeInfo.INT, tenv, this);
			TypeInfo thenType = ((ASTreeTypeEx) thenBlock()).typeCheck(tenv);
			TypeInfo elseType;
			ASTree elseBk = elseBlock();
			if (elseBk == null)
				elseType = TypeInfo.INT;
			else
				elseType = ((ASTreeTypeEx) elseBk).typeCheck(tenv);
			return thenType.union(elseType, tenv);
		}
	}

	@Reviser
	public static class WhileEx extends WhileStmnt {
		public WhileEx(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			TypeInfo condType = ((ASTreeTypeEx) condition()).typeCheck(tenv);
			condType.assertSubtypeOf(TypeInfo.INT, tenv, this);
			TypeInfo bodyType = ((ASTreeTypeEx) body()).typeCheck(tenv);
			return bodyType.union(TypeInfo.INT, tenv);
		}
	}

	@Reviser
	public static class DefStmntEx2 extends TypedEvaluator.DefStmntEx {
		protected TypeInfo.FunctionType funcType;
		protected TypeEnv bodyEnv;

		public DefStmntEx2(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			TypeInfo[] params = ((ParamListEx2) parameters()).types();
			TypeInfo retType = TypeInfo.get(type());
			funcType = TypeInfo.function(retType, params);
			TypeInfo oldType = tenv.put(0, index, funcType);
			if (oldType != null)
				throw new TypeException("function redefinition: " + name(), this);
			bodyEnv = new TypeEnv(size, tenv);
			for (int i = 0; i < params.length; i++)
				bodyEnv.put(0, i, params[i]);
			TypeInfo bodyType = ((ASTreeTypeEx) revise(body())).typeCheck(bodyEnv);
			bodyType.assertSubtypeOf(retType, tenv, this);
			return funcType;
		}
	}

	@Reviser
	public static class ParamListEx2 extends TypedEvaluator.ParamListEx {
		public ParamListEx2(List<ASTree> c) {
			super(c);
		}

		public TypeInfo[] types() throws TypeException {
			int s = size();
			TypeInfo[] result = new TypeInfo[s];
			for (int i = 0; i < s; i++)
				result[i] = TypeInfo.get(typeTag(i));
			return result;
		}
	}

	@Reviser
	public static class PrimaryEx2 extends FuncEvaluator.PrimaryEx {
		public PrimaryEx2(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			return typeCheck(tenv, 0);
		}

		public TypeInfo typeCheck(TypeEnv tenv, int nest) throws TypeException {
			if (hasPostfix(nest)) {
				TypeInfo target = typeCheck(tenv, nest + 1);
				return ((PostfixEx) postfix(nest)).typeCheck(tenv, target);
			} else
				return ((ASTreeTypeEx) operand()).typeCheck(tenv);
		}
	}

	@Reviser
	public static abstract class PostfixEx extends Postfix {
		public PostfixEx(List<ASTree> c) {
			super(c);
		}

		public abstract TypeInfo typeCheck(TypeEnv tenv, TypeInfo target) throws TypeException;
	}

	@Reviser
	public static class ArgumentsEx extends Arguments {
		protected TypeInfo[] argTypes;
		protected TypeInfo.FunctionType funcType;

		public ArgumentsEx(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv, TypeInfo target) throws TypeException {
			if (!(target instanceof TypeInfo.FunctionType))
				throw new TypeException("bad function", this);
			funcType = (TypeInfo.FunctionType) target;
			TypeInfo[] params = funcType.parameterTypes;
			if (size() != params.length)
				throw new TypeException("bad number of arguments", this);
			argTypes = new TypeInfo[params.length];
			int num = 0;
			for (ASTree a : this) {
				TypeInfo t = argTypes[num] = ((ASTreeTypeEx) a).typeCheck(tenv);
				t.assertSubtypeOf(params[num++], tenv, this);
			}
			return funcType.returnType;
		}
	}

	@Reviser
	public static class VarStmntEx2 extends TypedEvaluator.VarStmntEx {
		protected TypeInfo varType, valueType;

		public VarStmntEx2(List<ASTree> c) {
			super(c);
		}

		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			if (tenv.get(0, index) != null)
				throw new TypeException("duplicate variable: " + name(), this);
			varType = TypeInfo.get(type());
			tenv.put(0, index, varType);
			valueType = ((ASTreeTypeEx) initializer()).typeCheck(tenv);
			valueType.assertSubtypeOf(varType, tenv, this);
			return varType;
		}
	}
}
```

**代码清单14.7 TypeEnv.java**
```java
package chap14;
import java.util.Arrays;
import Stone.StoneException;

public class TypeEnv {
	protected TypeEnv outer;
	protected TypeInfo[] types;

	public TypeEnv() {
		this(8, null);
	}

	public TypeEnv(int size, TypeEnv out) {
		outer = out;
		types = new TypeInfo[size];
	}

	public TypeInfo get(int nest, int index) {
		if (nest == 0)
			if (index < types.length)
				return types[index];
			else
				return null;
		else if (outer == null)
			return null;
		else
			return outer.get(nest - 1, index);
	}

	public TypeInfo put(int nest, int index, TypeInfo value) {
		TypeInfo oldValue;
		if (nest == 0) {
			access(index);
			oldValue = types[index];
			types[index] = value;
			return oldValue; // may be null
		} else if (outer == null)
			throw new StoneException("no outer type environment");
		else
			return outer.put(nest - 1, index, value);
	}

	protected void access(int index) {
		if (index >= types.length) {
			int newLen = types.length * 2;
			if (index >= newLen)
				newLen = index + 1;
			types = Arrays.copyOf(types, newLen);
		}
	}
}
```

**代码清单14.8 TypeException.java**
```java
package chap14;
import Stone.ast.ASTree;

public class TypeException extends Exception {
	public TypeException(String msg, ASTree t) {
		super(msg + " " + t.location());
	}
}
```

**代码清单14.9 TypeInfo.java**
```java
package chap14;
import Stone.ast.ASTree;
import Stone.ast.TypeTag;

public class TypeInfo {
	public static final TypeInfo ANY = new TypeInfo() {
		@Override
		public String toString() {
			return "Any";
		}
	};
	public static final TypeInfo INT = new TypeInfo() {
		@Override
		public String toString() {
			return "Int";
		}
	};
	public static final TypeInfo STRING = new TypeInfo() {
		@Override
		public String toString() {
			return "String";
		}
	};

	public TypeInfo type() {
		return this;
	}

	public boolean match(TypeInfo obj) {
		return type() == obj.type();
	}

	public boolean subtypeOf(TypeInfo superType) {
		superType = superType.type();
		return type() == superType || superType == ANY;
	}

	public void assertSubtypeOf(TypeInfo type, TypeEnv env, ASTree where) throws TypeException {
		if (!subtypeOf(type))
			throw new TypeException("type mismatch: cannot convert from " + this + " to " + type, where);
	}

	public TypeInfo union(TypeInfo right, TypeEnv tenv) {
		if (match(right))
			return type();
		else
			return ANY;
	}

	public TypeInfo plus(TypeInfo right, TypeEnv tenv) {
		if (INT.match(this) && INT.match(right))
			return INT;
		else if (STRING.match(this) || STRING.match(right))
			return STRING;
		else
			return ANY;
	}

	public static TypeInfo get(TypeTag tag) throws TypeException {
		String tname = tag.type();
		if (INT.toString().equals(tname))
			return INT;
		else if (STRING.toString().equals(tname))
			return STRING;
		else if (ANY.toString().equals(tname))
			return ANY;
		else if (TypeTag.UNDEF.equals(tname))
			return new UnknownType();
		else
			throw new TypeException("unknown type " + tname, tag);
	}

	public static FunctionType function(TypeInfo ret, TypeInfo... params) {
		return new FunctionType(ret, params);
	}

	public boolean isFunctionType() {
		return false;
	}

	public FunctionType toFunctionType() {
		return null;
	}

	public boolean isUnknownType() {
		return false;
	}

	public UnknownType toUnknownType() {
		return null;
	}

	public static class UnknownType extends TypeInfo {
		@Override
		public TypeInfo type() {
			return ANY;
		}

		@Override
		public String toString() {
			return type().toString();
		}

		@Override
		public boolean isUnknownType() {
			return true;
		}

		@Override
		public UnknownType toUnknownType() {
			return this;
		}
	}

	public static class FunctionType extends TypeInfo {
		public TypeInfo returnType;
		public TypeInfo[] parameterTypes;

		public FunctionType(TypeInfo ret, TypeInfo... params) {
			returnType = ret;
			parameterTypes = params;
		}

		@Override
		public boolean isFunctionType() {
			return true;
		}

		@Override
		public FunctionType toFunctionType() {
			return this;
		}

		@Override
		public boolean match(TypeInfo obj) {
			if (!(obj instanceof FunctionType))
				return false;
			FunctionType func = (FunctionType) obj;
			if (parameterTypes.length != func.parameterTypes.length)
				return false;
			for (int i = 0; i < parameterTypes.length; i++)
				if (!parameterTypes[i].match(func.parameterTypes[i]))
					return false;
			return returnType.match(func.returnType);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (parameterTypes.length == 0)
				sb.append("Unit");
			else
				for (int i = 0; i < parameterTypes.length; i++) {
					if (i > 0)
						sb.append(" * ");
					sb.append(parameterTypes[i]);
				}
			sb.append(" -> ").append(returnType);
			return sb.toString();
		}
	}
}
```
### 运行程序时执行类型检查
至此，Stone语言处理器已经能够支持类型检查，我们先来试一下在运行程序时执行类型检查。代码清单14.10是新的解释器。它与之前的解释器大同小异，唯一的区别在于会在调用eval方法执行输入的程序之前，通过typecheck方法对数据类型执行检查。也就是说，该解释器会依次对输入的程序调用lookup、typeCheck与eval方法。此外，在最终显示eval方法的返回值时，它将同时显示typeCheck方法的返回值，即eval方法返回值的数据类型

代码清单14.11是该解释器的启动程序。它应用了Typechecker修改器（及其依赖的其他修改器）

在向环境添加原生函数时，我们还要向数据类型环境添加该原生函数的数据类型。代码清单14.12中的程序能实现这一处理。代码清单14.10中的解释器没有像之前那样使用第八天代码清单8.3中的Natives类，将使用代码清单14.13的程序。代码清单14.13~代码清单14.17是新增的原生函数。与之前不同，这些原生函数各自具有单独的类，且函数本身都被命名为m。这是为了之后将Stone语言的程序转换为Java二进制代码而做的准备

启动解释器，试着执行一下程序
```
def fact(n: Int) : Int {
    if n > 1 {
        n * fact(n - 1)
    } else {
        1
    }
}
fact 5
```
执行结果如下所示：
```
=> fact : Int -> Int
=> 120 : Int
```
第1行定义了函数fact，并表明它是一个接收Int类型参数，返回Int类型结果的函数。最后一行以5为参数调用了fact，得到一个Int类型的返回值120

**代码清单14.10 TypedInterpreter.java**
```java
package chap14;
import Stone.BasicParser;
import Stone.CodeDialog;
import Stone.Lexer;
import Stone.Token;
import Stone.TypedParser;
import Stone.ParseException;
import Stone.ast.ASTree;
import Stone.ast.NullStmnt;
import chap11.EnvOptimizer;
import chap11.ResizableArrayEnv;
import chap6.BasicEvaluator;
import chap6.Environment;

public class TypedInterpreter {
	public static void main(String[] args) throws ParseException, TypeException {
		TypeEnv te = new TypeEnv();
		run(new TypedParser(), new TypedNatives(te).environment(new ResizableArrayEnv()), te);
	}

	public static void run(BasicParser bp, Environment env, TypeEnv typeEnv) throws ParseException, TypeException {
		Lexer lexer = new Lexer(new CodeDialog());
		while (lexer.peek(0) != Token.EOF) {
			ASTree tree = bp.parse(lexer);
			if (!(tree instanceof NullStmnt)) {
				((EnvOptimizer.ASTreeOptEx) tree).lookup(((EnvOptimizer.EnvEx2) env).symbols());
				TypeInfo type = ((TypeChecker.ASTreeTypeEx) tree).typeCheck(typeEnv);
				Object r = ((BasicEvaluator.ASTreeEx) tree).eval(env);
				System.out.println("=> " + r + " : " + type);
			}
		}
	}
}
```

**代码清单14.11 TypedRunner.java**
```java
package chap14;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class TypedRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(TypedInterpreter.class, args, TypeChecker.class, NativeEvaluator.class);
	}
}
```

**代码清单14.12 TypedNatives.java**
```java
package chap14;
import chap6.Environment;
import chap8.Natives;
import chap11.EnvOptimizer.EnvEx2;

public class TypedNatives extends Natives {
	protected TypeEnv typeEnv;

	public TypedNatives(TypeEnv te) {
		typeEnv = te;
	}

	protected void append(Environment env, String name, Class<?> clazz, String methodName, TypeInfo type,
			Class<?>... params) {
		append(env, name, clazz, methodName, params);
		int index = ((EnvEx2) env).symbols().find(name);
		typeEnv.put(0, index, type);
	}

	protected void appendNatives(Environment env) {
		append(env, "print", chap14.java.print.class, "m", TypeInfo.function(TypeInfo.INT, TypeInfo.ANY), Object.class);
		append(env, "read", chap14.java.read.class, "m", TypeInfo.function(TypeInfo.STRING));
		append(env, "length", chap14.java.length.class, "m", TypeInfo.function(TypeInfo.INT, TypeInfo.STRING),
				String.class);
		append(env, "toInt", chap14.java.toInt.class, "m", TypeInfo.function(TypeInfo.INT, TypeInfo.ANY), Object.class);
		append(env, "currentTime", chap14.java.currentTime.class, "m", TypeInfo.function(TypeInfo.INT));
	}
}
```

**代码清单14.13 currentTime.java**
```java
package chap14.java;
import chap11.ArrayEnv;

public class currentTime {
	private static long startTime = System.currentTimeMillis();

	public static int m(ArrayEnv env) {
		return m();
	}

	public static int m() {
		return (int) (System.currentTimeMillis() - startTime);
	}
}
```

**代码清单14.14 length.java**
```java
package chap14.java;
import chap11.ArrayEnv;

public class length {
	public static int m(ArrayEnv env, String s) {
		return m(s);
	}

	public static int m(String s) {
		return s.length();
	}
}
```

**代码清单14.15 print.java**
```java
package chap14.java;
import chap11.ArrayEnv;

public class print {
	public static int m(ArrayEnv env, Object obj) {
		return m(obj);
	}

	public static int m(Object obj) {
		System.out.println(obj.toString());
		return 0;
	}
}
```

**代码清单14.16 read.java**
```java
package chap14.java;
import chap11.ArrayEnv;
import javax.swing.JOptionPane;

public class read {
	public static String m(ArrayEnv env) {
		return m();
	}

	public static String m() {
		return JOptionPane.showInputDialog(null);
	}
}
```

**代码清单14.17 toInt.java**
```java
package chap14.java;
import chap11.ArrayEnv;

public class toInt {
	public static int m(ArrayEnv env, Object value) {
		return m(value);
	}

	public static int m(Object value) {
		if (value instanceof String)
			return Integer.parseInt((String) value);
		else if (value instanceof Integer)
			return ((Integer) value).intValue();
		else
			throw new NumberFormatException(value.toString());
	}
}
```
### 对类型省略的变量进行类型推论
之前，如果没有明确指定变量或参数的类型，我们将默认它们是Any类型。然而，Any类型的值无法进行减法与乘法计算，非常不便

为此，如果没有明确指定数据类型，我们就需要调查该变量或参数的使用方式，推测恰当的数据类型。这就是类型推论（type inference）。例如，如果某个变量出现在减法表达式的左侧或右侧，我们就能推测出它是一个Int类型的变量。之后，如果数据类型省略，我们将暂时把它记为Unknown类型（类型不明的类型），并通过类型推论确定它具体是什么类型

类型推论算法与类型检查算法大同小异。在执行类型检查时，语言处理器常需要确认-运算符左右两侧的子表达式是否都是Int类型，如果不是Int类型就会发生类型错误。不难想象，为了避免发生类型错误，我们需要将Unknown类型的子表达式视为Int类型处理。这样一来，最初是Unknown类型的值将随着类型检查的进行，逐渐被指定为具体的数据类型

对于上面的减法表达式，我们可以很容易地推测出Unknown具体指代的类型，但是有时，要确定一个值的类型并非易事。例如，在下面的赋值表达式中，变量x与y都没有被指定类型
```
x = y
```
这时，变量y要么与x的类型相同，要么是x类型的子类。然而，如果无法确定具体的类型，仅凭这些条件，我们无法推测出更加具体的结果

因此，我们只能推迟类型推论处理，等待获取进一步的信息。我们需要暂时记录这条赋值表达式中包含的信息，通常，这些信息以方程式的形式表现。首先，对于没有明确指定且尚不能推测出数据类型的变量与参数，我们将以tx、ty等变量表示。于是，该赋值表达式包含的信息就与下面的式子等价
```
ty = tx
```
这里的<表示子类关系。将数据类型信息以方程式的形式表现之后，类型推论的适用范围将更广。例如，如果在执行类型检查时遇到形如x-1的表达式，我们可以通过同样的思路，用下面的方程式表示其中包含的类型条件
```
tx = Int
```
减法两侧必须都是Int类型的值。连立两个方程式可得ty≤Int，又由于Int不含子类，因此可知tr、th都是Int类型的值

不难发现，类型推论的本质其实就是连立含有数据类型条件的方程式后求解。该连立方程式的解就是各个Unknown类型恋量的具体数据类型

有些方程式可能含有多个解，我们无法据此确定某个变量的具体类型。方便起见，对于这种情况，Stone 语言将把变量指定为Any类型。例如，下面的函数id将在接收参数x后直接返回x的值，我们设参数x的类型为tx，函数id返回值的类型为tid，并连立方程式，解得tx ≤ tid
```
def id(x) {
    x
}
```

代码清单14.18中的修改器将修改与类型推论处理相关的类。它修改了TypeEnv类、TypeInfo类及其子类UnknownType类。在此之前，TypeInfo类的assertSubtypeOf方法用于确认两种类型是否相同，或是否具有子类关系，如果不符合则抛出异常。修改后，如果遇到Unknown类型的值，该方法将为这两种类型建立方程式，并添加至数据类型环境TypeEnv对象中

要最终完成类型推论的实现，除了代码清单14.18中提供的，我们还需要再使用一个修改器。对于函数内部的局部变量或参数，如果仅凭函数内部的类型推论，无法确定Unknown类型的具体结果，Stone语言将默认采用Any类型。代码清单14.19中的修改器实现了这一逻辑。该修改器覆盖了DefStmnt类的typeCheck方法，在函数体的类型检查结束时，尚且无法确定具体数据类型的Unknown类型全都置为了Any类型

代码清单14.20是解释器的启动程序。为了支持类型推论功能，它在运行Stone语言解释器前将首先应用代码清单14.18与代码清单14.19中的修改器。代码清单14.20虽然没有显式地应用InferTypes修改器，但应用了InferFuncTypes修改器。由于InferFuncTypes修改器通过@Require隐式地应用了InferTypes等修改器，因此它们都会被一起应用于新的解释器

**代码清单14.18 InferTypes.java**
```java
package chap14;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import Stone.ast.ASTree;
import javassist.gluonj.Reviser;
import chap14.TypeInfo.UnknownType;

@Reviser
public class InferTypes {
	@Reviser
	public static class TypeInfoEx extends TypeInfo {
		@Override
		public void assertSubtypeOf(TypeInfo type, TypeEnv tenv, ASTree where) throws TypeException {
			if (type.isUnknownType())
				((UnknownTypeEx) type.toUnknownType()).assertSupertypeOf(this, tenv, where);
			else
				super.assertSubtypeOf(type, tenv, where);
		}

		@Override
		public TypeInfo union(TypeInfo right, TypeEnv tenv) {
			if (right.isUnknownType())
				return right.union(this, tenv);
			else
				return super.union(right, tenv);
		}

		@Override
		public TypeInfo plus(TypeInfo right, TypeEnv tenv) {
			if (right.isUnknownType())
				return right.plus(this, tenv);
			else
				return super.plus(right, tenv);
		}
	}

	@Reviser
	public static class UnknownTypeEx extends TypeInfo.UnknownType {
		protected TypeInfo type = null;

		public boolean resolved() {
			return type != null;
		}

		public void setType(TypeInfo t) {
			type = t;
		}

		@Override
		public TypeInfo type() {
			return type == null ? ANY : type;
		}

		@Override
		public void assertSubtypeOf(TypeInfo t, TypeEnv tenv, ASTree where) throws TypeException {
			if (resolved())
				type.assertSubtypeOf(t, tenv, where);
			else
				((TypeEnvEx) tenv).addEquation(this, t);
		}

		public void assertSupertypeOf(TypeInfo t, TypeEnv tenv, ASTree where) throws TypeException {
			if (resolved())
				t.assertSubtypeOf(type, tenv, where);
			else
				((TypeEnvEx) tenv).addEquation(this, t);
		}

		@Override
		public TypeInfo union(TypeInfo right, TypeEnv tenv) {
			if (resolved())
				return type.union(right, tenv);
			else {
				((TypeEnvEx) tenv).addEquation(this, right);
				return right;
			}
		}

		@Override
		public TypeInfo plus(TypeInfo right, TypeEnv tenv) {
			if (resolved())
				return type.plus(right, tenv);
			else {
				((TypeEnvEx) tenv).addEquation(this, INT);
				return right.plus(INT, tenv);
			}
		}
	}

	@Reviser
	public static class TypeEnvEx extends TypeEnv {
		public static class Equation extends ArrayList<UnknownType> {
		}

		protected List<Equation> equations = new LinkedList<Equation>();

		public void addEquation(UnknownType t1, TypeInfo t2) {
			// assert t1.unknown() == true
			if (t2.isUnknownType())
				if (((UnknownTypeEx) t2.toUnknownType()).resolved())
					t2 = t2.type();
			Equation eq = find(t1);
			if (t2.isUnknownType())
				eq.add(t2.toUnknownType());
			else {
				for (UnknownType t : eq)
					((UnknownTypeEx) t).setType(t2);
				equations.remove(eq);
			}
		}

		protected Equation find(UnknownType t) {
			for (Equation e : equations)
				if (e.contains(t))
					return e;
			Equation e = new Equation();
			e.add(t);
			equations.add(e);
			return e;
		}
	}
}
```

**代码清单14.19 InferFuncTypes.java**
```java
package chap14;
import java.util.List;
import chap14.TypeInfo.FunctionType;
import chap14.TypeInfo.UnknownType;
import chap14.InferTypes.UnknownTypeEx;
import Stone.ast.ASTree;
import javassist.gluonj.Require;
import javassist.gluonj.Reviser;

@Require({ TypeChecker.class, InferTypes.class })
@Reviser
public class InferFuncTypes {
	@Reviser
	public static class DefStmntEx3 extends TypeChecker.DefStmntEx2 {
		public DefStmntEx3(List<ASTree> c) {
			super(c);
		}

		@Override
		public TypeInfo typeCheck(TypeEnv tenv) throws TypeException {
			FunctionType func = super.typeCheck(tenv).toFunctionType();
			for (TypeInfo t : func.parameterTypes)
				fixUnknown(t);
			fixUnknown(func.returnType);
			return func;
		}

		protected void fixUnknown(TypeInfo t) {
			if (t.isUnknownType()) {
				UnknownType ut = t.toUnknownType();
				if (!((UnknownTypeEx) ut).resolved())
					((UnknownTypeEx) ut).setType(TypeInfo.ANY);
			}
		}
	}
}
```

**代码清单14.20 InferRunner.java**
```java
package chap14;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class InferRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(TypedInterpreter.class, args, InferFuncTypes.class, NativeEvaluator.class);
	}
}
```
### Java二进制代码转换
获得了静态数据类型信息之后，我们将利用现有的库，实现Java二进制代码的转换

为了对Java二进制代码进行操作，本章采用了一种名为Javassist的库。该库能够在程序执行过程中创建并载入新的类，并调用其中的方法。由于新增方法的定义能以Java源代码的形式传递给Javassist，因此我们无需在程序中生成Java二进制代码，非常方便。Javassist能自动编译接收的源代码，并将其转换为二进制代码

代码清单14.21中的程序能够通过Javassist来定义新的方法。JavaLoader类的1oad方法将在接收类名与方法的定义后，对方法进行定义，并生成二进制代码，最后载入Java虚拟机。1oad方法的返回值是该类的Class对象

**代码清单14.21 JavaLoader.java**
```java
package chap14;
import Stone.StoneException;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class JavaLoader {
	protected ClassLoader loader;
	protected ClassPool cpool;

	public JavaLoader() {
		cpool = new ClassPool(null);
		cpool.appendSystemPath();
		loader = new ClassLoader(this.getClass().getClassLoader()) {
		};
	}

	public Class<?> load(String className, String method) {
		// System.out.println(method);
		CtClass cc = cpool.makeClass(className);
		try {
			cc.addMethod(CtMethod.make(method, cc));
			return cc.toClass(loader, null);
		} catch (CannotCompileException e) {
			throw new StoneException(e.getMessage());
		}
	}
}
```

代码清单14.22中的JavaFunction类的对象用于表示函数

**代码清单14.22 JavaFunction.java**
```java
package chap14;
import Stone.StoneException;
import chap7.Function;

public class JavaFunction extends Function {
	protected String className;
	protected Class<?> clazz;

	public JavaFunction(String name, String method, JavaLoader loader) {
		super(null, null, null);
		className = className(name);
		clazz = loader.load(className, method);
	}

	public static String className(String name) {
		return "chap14.java." + name;
	}

	public Object invoke(Object[] args) {
		try {
			return clazz.getDeclaredMethods()[0].invoke(null, args);
		} catch (Exception e) {
			throw new StoneException(e.getMessage());
		}
	}
}
```

由于使用Javassist，因此与其说是将抽象语法树转换为Java二进制代码，不如所是将它转换为Java源代码。例如，假设定义了一个Stone语言函数fact
```
def fact(n) {
    if n < 2 {
        1
    } else {
        n * fact(n - 1)
    }
}
```
该函数将被转换为名为chap14.java.fact的类中的一个static方法
```java
public static int m(chap11.ArrayEnv env,int v0) {
    int res;
    if ((v0 < 2 ? 1 : 0) != 0) {
        res = 1;
    } else {
        res = (v0 * chap14.java.fact.m(env, (v0 - v1)));
    }
    return res;
}
```
chap14.java.fact类仅包含一个方法m。m这个方法名并没有什么特殊含义。该方法的第一个参数env是一个用于引用全局变量的环境，不过在该例中，fact函数不需要使用这个环境。m方法的第二个参数vo是fact函数的参数。在转换为Java语言的方法后，if语句有一条稍显冗长的条件表达式。该表达式由fact函数的定义直接翻译

代码清单14.23中的修改器用于将函数转换为Java二进制代码。修改器起始处的ranslateExpr与returnZero是两个辅助方法，需由其他方法调用。EnvEx3与ArrayEnvEx修改器将向环境中添加新的字段，并通过它们保存JavaLoader对象

在Java语言的转换过程中，Stone语言的Int、string与Any类型分别对应Java中的int、string与object类型。由于Stone语言的语法与Java较为相近，所以转换逻辑并不复杂，只需为每个变量添加静态数据类型即可

**代码清单14.23 ToJava.java**
```java
package chap14;
import java.util.ArrayList;
import java.util.List;
import chap11.ArrayEnv;
import chap11.EnvOptimizer;
import chap6.Environment;
import chap7.FuncEvaluator;
import Stone.StoneException;
import Stone.Token;
import Stone.ast.*;
import javassist.gluonj.Require;
import javassist.gluonj.Reviser;
import static javassist.gluonj.GluonJ.revise;

@Require(TypeChecker.class)
@Reviser
public class ToJava {
	public static final String METHOD = "m";
	public static final String LOCAL = "v";
	public static final String ENV = "env";
	public static final String RESULT = "res";
	public static final String ENV_TYPE = "chap11.ArrayEnv";

	public static String translateExpr(ASTree ast, TypeInfo from, TypeInfo to) {
		return translateExpr(((ASTreeEx) ast).translate(null), from, to);
	}

	public static String translateExpr(String expr, TypeInfo from, TypeInfo to) {
		from = from.type();
		to = to.type();
		if (from == TypeInfo.INT) {
			if (to == TypeInfo.ANY)
				return "new Integer(" + expr + ")";
			else if (to == TypeInfo.STRING)
				return "Integer.toString(" + expr + ")";
		} else if (from == TypeInfo.ANY)
			if (to == TypeInfo.STRING)
				return expr + ".toString()";
			else if (to == TypeInfo.INT)
				return "((Integer)" + expr + ").intValue()";
		return expr;
	}

	public static String returnZero(TypeInfo to) {
		if (to.type() == TypeInfo.ANY)
			return RESULT + "=new Integer(0);";
		else
			return RESULT + "=0;";
	}

	@Reviser
	public static interface EnvEx3 extends EnvOptimizer.EnvEx2 {
		JavaLoader javaLoader();
	}

	@Reviser
	public static class ArrayEnvEx extends ArrayEnv {
		public ArrayEnvEx(int size, Environment out) {
			super(size, out);
		}

		protected JavaLoader jloader = new JavaLoader();

		public JavaLoader javaLoader() {
			return jloader;
		}
	}

	@Reviser
	public static abstract class ASTreeEx extends ASTree {
		public String translate(TypeInfo result) {
			return "";
		}
	}

	@Reviser
	public static class NumberEx extends NumberLiteral {
		public NumberEx(Token t) {
			super(t);
		}

		public String translate(TypeInfo result) {
			return Integer.toString(value());
		}
	}

	@Reviser
	public static class StringEx extends StringLiteral {
		public StringEx(Token t) {
			super(t);
		}

		public String translate(TypeInfo result) {
			StringBuilder code = new StringBuilder();
			String literal = value();
			code.append('"');
			for (int i = 0; i < literal.length(); i++) {
				char c = literal.charAt(i);
				if (c == '"')
					code.append("\\\"");
				else if (c == '\\')
					code.append("\\\\");
				else if (c == '\n')
					code.append("\\n");
				else
					code.append(c);
			}
			code.append('"');
			return code.toString();
		}
	}

	@Reviser
	public static class NameEx3 extends TypeChecker.NameEx2 {
		public NameEx3(Token t) {
			super(t);
		}

		public String translate(TypeInfo result) {
			if (type.isFunctionType())
				return JavaFunction.className(name()) + "." + METHOD;
			else if (nest == 0)
				return LOCAL + index;
			else {
				String expr = ENV + ".get(0," + index + ")";
				return translateExpr(expr, TypeInfo.ANY, type);
			}
		}

		public String translateAssign(TypeInfo valueType, ASTree right) {
			if (nest == 0)
				return "(" + LOCAL + index + "=" + translateExpr(right, valueType, type) + ")";
			else {
				String value = ((ASTreeEx) right).translate(null);
				return "chap14.Runtime.write" + type.toString() + "(" + ENV + "," + index + "," + value + ")";
			}
		}
	}

	@Reviser
	public static class NegativeEx extends NegativeExpr {
		public NegativeEx(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			return "-" + ((ASTreeEx) operand()).translate(null);
		}
	}

	@Reviser
	public static class BinaryEx2 extends TypeChecker.BinaryEx {
		public BinaryEx2(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			String op = operator();
			if ("=".equals(op))
				return ((NameEx3) left()).translateAssign(rightType, right());
			else if (leftType.type() != TypeInfo.INT || rightType.type() != TypeInfo.INT) {
				String e1 = translateExpr(left(), leftType, TypeInfo.ANY);
				String e2 = translateExpr(right(), rightType, TypeInfo.ANY);
				if ("==".equals(op))
					return "chap14.Runtime.eq(" + e1 + "," + e2 + ")";
				else if ("+".equals(op)) {
					if (leftType.type() == TypeInfo.STRING || rightType.type() == TypeInfo.STRING)
						return e1 + "+" + e2;
					else
						return "chap14.Runtime.plus(" + e1 + "," + e2 + ")";
				} else
					throw new StoneException("bad operator", this);
			} else {
				String expr = ((ASTreeEx) left()).translate(null) + op + ((ASTreeEx) right()).translate(null);
				if ("<".equals(op) || ">".equals(op) || "==".equals(op))
					return "(" + expr + "?1:0)";
				else
					return "(" + expr + ")";
			}
		}
	}

	@Reviser
	public static class BlockEx2 extends TypeChecker.BlockEx {
		public BlockEx2(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			ArrayList<ASTree> body = new ArrayList<ASTree>();
			for (ASTree t : this)
				if (!(t instanceof NullStmnt))
					body.add(t);
			StringBuilder code = new StringBuilder();
			if (result != null && body.size() < 1)
				code.append(returnZero(result));
			else
				for (int i = 0; i < body.size(); i++)
					translateStmnt(code, body.get(i), result, i == body.size() - 1);
			return code.toString();
		}

		protected void translateStmnt(StringBuilder code, ASTree tree, TypeInfo result, boolean last) {
			if (isControlStmnt(tree))
				code.append(((ASTreeEx) tree).translate(last ? result : null));
			else if (last && result != null)
				code.append(RESULT).append('=').append(translateExpr(tree, type, result)).append(";\n");
			else if (isExprStmnt(tree))
				code.append(((ASTreeEx) tree).translate(null)).append(";\n");
			else
				throw new StoneException("bad expression statement", this);
		}

		protected static boolean isExprStmnt(ASTree tree) {
			if (tree instanceof BinaryExpr)
				return "=".equals(((BinaryExpr) tree).operator());
			return tree instanceof PrimaryExpr || tree instanceof VarStmnt;
		}

		protected static boolean isControlStmnt(ASTree tree) {
			return tree instanceof BlockStmnt || tree instanceof IfStmnt || tree instanceof WhileStmnt;
		}
	}

	@Reviser
	public static class IfEx extends IfStmnt {
		public IfEx(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			StringBuilder code = new StringBuilder();
			code.append("if(");
			code.append(((ASTreeEx) condition()).translate(null));
			code.append("!=0){\n");
			code.append(((ASTreeEx) thenBlock()).translate(result));
			code.append("} else {\n");
			ASTree elseBk = elseBlock();
			if (elseBk != null)
				code.append(((ASTreeEx) elseBk).translate(result));
			else if (result != null)
				code.append(returnZero(result));
			return code.append("}\n").toString();
		}
	}

	@Reviser
	public static class WhileEx extends WhileStmnt {
		public WhileEx(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			String code = "while(" + ((ASTreeEx) condition()).translate(null) + "!=0){\n"
					+ ((ASTreeEx) body()).translate(result) + "}\n";
			if (result == null)
				return code;
			else
				return returnZero(result) + "\n" + code;
		}
	}

	@Reviser
	public static class DefStmntEx3 extends TypeChecker.DefStmntEx2 {
		public DefStmntEx3(List<ASTree> c) {
			super(c);
		}

		@Override
		public Object eval(Environment env) {
			String funcName = name();
			JavaFunction func = new JavaFunction(funcName, translate(null), ((EnvEx3) env).javaLoader());
			((EnvEx3) env).putNew(funcName, func);
			return funcName;
		}

		public String translate(TypeInfo result) {
			StringBuilder code = new StringBuilder("public static ");
			TypeInfo returnType = funcType.returnType;
			code.append(javaType(returnType)).append(' ');
			code.append(METHOD).append("(chap11.ArrayEnv ").append(ENV);
			for (int i = 0; i < funcType.parameterTypes.length; i++) {
				code.append(',').append(javaType(funcType.parameterTypes[i])).append(' ').append(LOCAL).append(i);
			}
			code.append("){\n");
			code.append(javaType(returnType)).append(' ').append(RESULT).append(";\n");
			for (int i = funcType.parameterTypes.length; i < size; i++) {
				TypeInfo t = bodyEnv.get(0, i);
				code.append(javaType(t)).append(' ').append(LOCAL).append(i);
				if (t.type() == TypeInfo.INT)
					code.append("=0;\n");
				else
					code.append("=null;\n");
			}
			code.append(((ASTreeEx) revise(body())).translate(returnType));
			code.append("return ").append(RESULT).append(";}");
			return code.toString();
		}

		protected String javaType(TypeInfo t) {
			if (t.type() == TypeInfo.INT)
				return "int";
			else if (t.type() == TypeInfo.STRING)
				return "String";
			else
				return "Object";
		}
	}

	@Reviser
	public static class PrimaryEx2 extends FuncEvaluator.PrimaryEx {
		public PrimaryEx2(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			return translate(0);
		}

		public String translate(int nest) {
			if (hasPostfix(nest)) {
				String expr = translate(nest + 1);
				return ((PostfixEx) postfix(nest)).translate(expr);
			} else
				return ((ASTreeEx) operand()).translate(null);
		}
	}

	@Reviser
	public static abstract class PostfixEx extends Postfix {
		public PostfixEx(List<ASTree> c) {
			super(c);
		}

		public abstract String translate(String expr);
	}

	@Reviser
	public static class ArgumentsEx extends TypeChecker.ArgumentsEx {
		public ArgumentsEx(List<ASTree> c) {
			super(c);
		}

		public String translate(String expr) {
			StringBuilder code = new StringBuilder(expr);
			code.append('(').append(ENV);
			for (int i = 0; i < size(); i++)
				code.append(',').append(translateExpr(child(i), argTypes[i], funcType.parameterTypes[i]));
			return code.append(')').toString();
		}

		public Object eval(Environment env, Object value) {
			if (!(value instanceof JavaFunction))
				throw new StoneException("bad function", this);
			JavaFunction func = (JavaFunction) value;
			Object[] args = new Object[numChildren() + 1];
			args[0] = env;
			int num = 1;
			for (ASTree a : this)
				args[num++] = ((chap6.BasicEvaluator.ASTreeEx) a).eval(env);
			return func.invoke(args);
		}
	}

	@Reviser
	public static class VarStmntEx3 extends TypeChecker.VarStmntEx2 {
		public VarStmntEx3(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			return LOCAL + index + "=" + translateExpr(initializer(), valueType, varType);
		}
	}
}
```

**代码清单14.24 RunTime.java**
```java
package chap14;
import chap11.ArrayEnv;

public class Runtime {
	public static int eq(Object a, Object b) {
		if (a == null)
			return b == null ? 1 : 0;
		else
			return a.equals(b) ? 1 : 0;
	}

	public static Object plus(Object a, Object b) {
		if (a instanceof Integer && b instanceof Integer)
			return ((Integer) a).intValue() + ((Integer) b).intValue();
		else
			return a.toString().concat(b.toString());
	}

	public static int writeInt(ArrayEnv env, int index, int value) {
		env.put(0, index, value);
		return value;
	}

	public static String writeString(ArrayEnv env, int index, String value) {
		env.put(0, index, value);
		return value;
	}

	public static Object writeAny(ArrayEnv env, int index, Object value) {
		env.put(0, index, value);
		return value;
	}
}
```
### 综合所有修改再次运行程序
代码清单14.25是最新版的启动程序。该启动程序在运行Stone语言时将同时执行类型检查、类型推论以及Java二进制代码的转换

**代码清单14.25 JavaRunner.java**
```java
package chap14;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class JavaRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(TypedInterpreter.class, args, ToJava.class, InferFuncTypes.class, NativeEvaluator.class);
	}
}
```
运行效果如下：
![](https://s1.ax1x.com/2018/12/23/FybWdg.gif#shadow)
相比较前几天的优化，今天的优化速度提升非常明显，前几天执行fib 33至少都要6秒，今天只用2ms
### 第十四天的思维导图
![](https://s1.ax1x.com/2018/12/23/FybXo4.png#shadow)
