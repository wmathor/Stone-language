### 减少内存占用
第9天的实现使用了环境来表现Stone语言中的对象。环境中不仅含有由字段名与相应的值组成的键值对，还记录了由方法名与Function对象组成的键值对。这种实现的内存利用率很低

JavaScript每个对象都能拥有不同方法的语言，这种实现方式较为合适。然而，Stone语言中同一个类的对象只能具有相同的方法。因此，语言处理器没有必要在环境中记录由方法名与Function对象组成的键值对

基于以上原因，今天的优化中，同一个类的对象将共享方法（见下图）。Stone语言将为每个方法创建一个ClassInfo对象，用于记录与方法相关的信息。用于表示Stone语言对象的stoneObject对象包含ClassInfo对象的引用，当语言处理器需要获取方法调用的相关信息时，将查找该ClassInfo对象中的内容。这样一来，Stoneobject对象仅需记录字段信息，每个单独的stoneobject对象使用的内存量也相应减少

![](https://s1.ax1x.com/2018/12/22/FyGx8U.png#shadow)

今天的优化方法和第十一天一样，通过数组实现环境

根据上述讨论重新定义ClassInfo类与StoneObject类。为了区分今天和第九天的定义，重新定义的类的名称之前会加上Opt

**代码清单12.1 OptClassInfo.java**
```java
package chap12;
import java.util.ArrayList;
import Stone.ast.ClassStmnt;
import Stone.ast.DefStmnt;
import chap11.Symbols;
import chap12.ObjOptimizer.DefStmntEx2;
import chap6.Environment;
import chap9.ClassInfo;

public class OptClassInfo extends ClassInfo {
	protected Symbols methods, fields;
	protected DefStmnt[] methodDefs;

	public OptClassInfo(ClassStmnt cs, Environment env, Symbols methods, Symbols fields) {
		super(cs, env);
		this.methods = methods;
		this.fields = fields;
		this.methodDefs = null;
	}

	public int size() {
		return fields.size();
	}

	@Override
	public OptClassInfo superClass() {
		return (OptClassInfo) superClass;
	}

	public void copyTo(Symbols f, Symbols m, ArrayList<DefStmnt> mlist) {
		f.append(fields);
		m.append(methods);
		for (DefStmnt def : methodDefs)
			mlist.add(def);
	}

	public Integer fieldIndex(String name) {
		return fields.find(name);
	}

	public Integer methodIndex(String name) {
		return methods.find(name);
	}

	public Object method(OptStoneObject self, int index) {
		DefStmnt def = methodDefs[index];
		return new OptMethod(def.parameters(), def.body(), environment(), ((DefStmntEx2) def).locals(), self);
	}

	public void setMethods(ArrayList<DefStmnt> methods) {
		methodDefs = methods.toArray(new DefStmnt[methods.size()]);
	}
}
```

**代码清单12.2 OptStoneObject.java**
```java
package chap12;
import chap12.OptStoneObject.AccessException;

public class OptStoneObject {
	public static class AccessException extends Exception {
	}

	protected OptClassInfo classInfo;
	protected Object[] fields;

	public OptStoneObject(OptClassInfo ci, int size) {
		classInfo = ci;
		fields = new Object[size];
	}

	public OptClassInfo classInfo() {
		return classInfo;
	}

	public Object read(String name) throws AccessException {
		Integer i = classInfo.fieldIndex(name);
		if (i != null)
			return fields[i];
		else {
			i = classInfo.methodIndex(name);
			if (i != null)
				return method(i);
		}
		throw new AccessException();
	}

	public void write(String name, Object value) throws AccessException {
		Integer i = classInfo.fieldIndex(name);
		if (i == null)
			throw new AccessException();
		else
			fields[i] = value;
	}

	public Object read(int index) {
		return fields[index];
	}

	public void write(int index, Object value) {
		fields[index] = value;
	}

	public Object method(int index) {
		return classInfo.method(this, index);
	}
}
```

**代码清单12.3 OptMethod.java**
```java
package chap12;
import Stone.ast.BlockStmnt;
import Stone.ast.ParameterList;
import chap11.ArrayEnv;
import chap11.OptFunction;
import chap6.Environment;

public class OptMethod extends OptFunction {
	OptStoneObject self;

	public OptMethod(ParameterList parameters, BlockStmnt body, Environment env, int memorySize, OptStoneObject self) {
		super(parameters, body, env, memorySize);
		this.self = self;
	}

	@Override
	public Environment makeEnv() {
		ArrayEnv e = new ArrayEnv(size, env);
		e.put(0, 0, self);
		return e;
	}
}
```
### 定义lookup方法
lookup方法能查找class语句中与由大括号括起的类定义体对应的抽象语法树，向Symbols对象添加该类种所有的方法名与字段名，同时为它们分配保存位置。lookup方法执行结束后，语言处理器将能通过Symbols对象获得方法名与字段名一览

下图是lookup方法在查找大括号括起的类定义体时使用的Symbo1s对象。这4个对象通过outer字段连接，分别记录了不同类型的名称。除了最后一个，这些对象都属于Symbo1s类的子类

代码清单12.4是图中第一个出现的SymbolThis对象的定义。它用于记录在类定义体中有效的局部变量的名称。然而，与函数不同，类定义体中新增的名称并非局部变量，而是字段名称，因此该作用域内的有效局部变量就只有一个this。SymbolThis对象仅会记录this的信息

下面这两个MemberSymbols对象分别用于记录字段名与方法名。代码清单12.5是它们的定义。如果lookup方法在类定义中遇到了用于定义方法的def语句，就会直接将该方法名称添加至第二个Membersymbols对象中

![](https://s1.ax1x.com/2018/12/22/FyJTJK.png#shadow)

**代码清单12.4 SymbolThis.java**
```java
package chap12;
import Stone.StoneException;
import chap11.Symbols;

public class SymbolThis extends Symbols {
	public static final String NAME = "this";

	public SymbolThis(Symbols outer) {
		super(outer);
		add(NAME);
	}

	@Override
	public int putNew(String key) {
		throw new StoneException("fatal");
	}

	@Override
	public Location put(String key) {
		Location loc = outer.put(key);
		if (loc.nest >= 0)
			loc.nest++;
		return loc;
	}
}
```

**代码清单12.5 MemberSymbols.java**
```java
package chap12;
import chap11.Symbols;

public class MemberSymbols extends Symbols {
	public static int METHOD = -1;
	public static int FIELD = -2;
	protected int type;

	public MemberSymbols(Symbols outer, int type) {
		super(outer);
		this.type = type;
	}

	@Override
	public Location get(String key, int nest) {
		Integer index = table.get(key);
		if (index == null)
			if (outer == null)
				return null;
			else
				return outer.get(key, nest);
		else
			return new Location(type, index.intValue());
	}

	@Override
	public Location put(String key) {
		Location loc = get(key, 0);
		if (loc == null)
			return new Location(type, add(key));
		else
			return loc;
	}
}
```
### 整合所有修改并执行
**代码清单12.6 ObjOptimizer.java**
```java
package chap12;
import java.util.ArrayList;
import java.util.List;
import static javassist.gluonj.GluonJ.revise;
import javassist.gluonj.*;
import Stone.*;
import Stone.ast.*;
import chap6.Environment;
import chap6.BasicEvaluator;
import chap6.BasicEvaluator.ASTreeEx;
import chap7.FuncEvaluator.PrimaryEx;
import chap11.ArrayEnv;
import chap11.EnvOptimizer;
import chap11.Symbols;
import chap11.EnvOptimizer.ASTreeOptEx;
import chap11.EnvOptimizer.EnvEx2;
import chap11.EnvOptimizer.ParamsEx;
import chap12.OptStoneObject.AccessException;

@Require(EnvOptimizer.class)
@Reviser
public class ObjOptimizer {
	@Reviser
	public static class ClassStmntEx extends ClassStmnt {
		public ClassStmntEx(List<ASTree> c) {
			super(c);
		}

		public void lookup(Symbols syms) {
		}

		public Object eval(Environment env) {
			Symbols methodNames = new MemberSymbols(((EnvEx2) env).symbols(), MemberSymbols.METHOD);
			Symbols fieldNames = new MemberSymbols(methodNames, MemberSymbols.FIELD);
			OptClassInfo ci = new OptClassInfo(this, env, methodNames, fieldNames);
			((EnvEx2) env).put(name(), ci);
			ArrayList<DefStmnt> methods = new ArrayList<DefStmnt>();
			if (ci.superClass() != null)
				ci.superClass().copyTo(fieldNames, methodNames, methods);
			Symbols newSyms = new SymbolThis(fieldNames);
			((ClassBodyEx) body()).lookup(newSyms, methodNames, fieldNames, methods);
			ci.setMethods(methods);
			return name();
		}
	}

	@Reviser
	public static class ClassBodyEx extends ClassBody {
		public ClassBodyEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env) {
			for (ASTree t : this)
				if (!(t instanceof DefStmnt))
					((ASTreeEx) t).eval(env);
			return null;
		}

		public void lookup(Symbols syms, Symbols methodNames, Symbols fieldNames, ArrayList<DefStmnt> methods) {
			for (ASTree t : this) {
				if (t instanceof DefStmnt) {
					DefStmnt def = (DefStmnt) t;
					int oldSize = methodNames.size();
					int i = methodNames.putNew(def.name());
					if (i >= oldSize)
						methods.add(def);
					else
						methods.set(i, def);
					((DefStmntEx2) def).lookupAsMethod(fieldNames);
				} else
					((ASTreeOptEx) t).lookup(syms);
			}
		}
	}

	@Reviser
	public static class DefStmntEx2 extends EnvOptimizer.DefStmntEx {
		public DefStmntEx2(List<ASTree> c) {
			super(c);
		}

		public int locals() {
			return size;
		}

		public void lookupAsMethod(Symbols syms) {
			Symbols newSyms = new Symbols(syms);
			newSyms.putNew(SymbolThis.NAME);
			((ParamsEx) parameters()).lookup(newSyms);
			((ASTreeOptEx) revise(body())).lookup(newSyms);
			size = newSyms.size();
		}
	}

	@Reviser
	public static class DotEx extends Dot {
		public DotEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment env, Object value) {
			String member = name();
			if (value instanceof OptClassInfo) {
				if ("new".equals(member)) {
					OptClassInfo ci = (OptClassInfo) value;
					ArrayEnv newEnv = new ArrayEnv(1, ci.environment());
					OptStoneObject so = new OptStoneObject(ci, ci.size());
					newEnv.put(0, 0, so);
					initObject(ci, so, newEnv);
					return so;
				}
			} else if (value instanceof OptStoneObject) {
				try {
					return ((OptStoneObject) value).read(member);
				} catch (AccessException e) {
				}
			}
			throw new StoneException("bad member access: " + member, this);
		}

		protected void initObject(OptClassInfo ci, OptStoneObject obj, Environment env) {
			if (ci.superClass() != null)
				initObject(ci.superClass(), obj, env);
			((ClassBodyEx) ci.body()).eval(env);
		}
	}

	@Reviser
	public static class NameEx2 extends EnvOptimizer.NameEx {
		public NameEx2(Token t) {
			super(t);
		}

		@Override
		public Object eval(Environment env) {
			if (index == UNKNOWN)
				return env.get(name());
			else if (nest == MemberSymbols.FIELD)
				return getThis(env).read(index);
			else if (nest == MemberSymbols.METHOD)
				return getThis(env).method(index);
			else
				return ((EnvEx2) env).get(nest, index);
		}

		@Override
		public void evalForAssign(Environment env, Object value) {
			if (index == UNKNOWN)
				env.put(name(), value);
			else if (nest == MemberSymbols.FIELD)
				getThis(env).write(index, value);
			else if (nest == MemberSymbols.METHOD)
				throw new StoneException("cannot update a method: " + name(), this);
			else
				((EnvEx2) env).put(nest, index, value);
		}

		protected OptStoneObject getThis(Environment env) {
			return (OptStoneObject) ((EnvEx2) env).get(0, 0);
		}
	}

	@Reviser
	public static class AssignEx extends BasicEvaluator.BinaryEx {
		public AssignEx(List<ASTree> c) {
			super(c);
		}

		@Override
		protected Object computeAssign(Environment env, Object rvalue) {
			ASTree le = left();
			if (le instanceof PrimaryExpr) {
				PrimaryEx p = (PrimaryEx) le;
				if (p.hasPostfix(0) && p.postfix(0) instanceof Dot) {
					Object t = ((PrimaryEx) le).evalSubExpr(env, 1);
					if (t instanceof OptStoneObject)
						return setField((OptStoneObject) t, (Dot) p.postfix(0), rvalue);
				}
			}
			return super.computeAssign(env, rvalue);
		}

		protected Object setField(OptStoneObject obj, Dot expr, Object rvalue) {
			String name = expr.name();
			try {
				obj.write(name, rvalue);
				return rvalue;
			} catch (AccessException e) {
				throw new StoneException("bad member access: " + name, this);
			}
		}
	}
}
```

**代码清单12.7 ObjOptInterpreter.java**
```java
package chap12;
import Stone.ClassParser;
import Stone.ParseException;
import chap11.EnvOptInterpreter;
import chap11.ResizableArrayEnv;
import chap8.Natives;

public class ObjOptInterpreter extends EnvOptInterpreter {
    public static void main(String[] args) throws ParseException {
        run(new ClassParser(),
            new Natives().environment(new ResizableArrayEnv()));
    }
}
```

**代码清单12.8 ObjOptRunner.java**
```java
package chap12;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class ObjOptRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(ObjOptInterpreter.class, args, ObjOptimizer.class, NativeEvaluator.class);
	}
}
```
### 内联缓存
如果要对非this对象进行方法调用或字段引用，今天介绍的1ookup方法将不再有效，执行速度无法得到提升。这样一来，语言处理器在调用函数或引用字段时，就不得不通过名称来查找环境，从哈希表中获取对应的值，使执行速度大幅下降

为了缓解这一问题，我们将使用一种名为内联缓存（inline cache）的方法。首先，我们假设程序需要引用某个对象的字段。正如之前所讲，只有在实际执行后语言处理器才能确定该对象的类型。不过，根据经验可知，同一位置出现的对象通常是同一种类型。即使是采用面向对象思想写成的程序也是如此。我们能够利用这一规律优化处理器的性能。语言处理器可以在执行程序的同时查找字段的保存位置，并将该结果与对象所属的类型结对保存。之后，如果再次执行同一段程序，语言处理器将首先判断对象的类型，如果与之前相同，则直接使用上次的查找结果，减少了因查找保存位置造成的性能下降

代码清单12.9中的InlineCache修改器实现了内联缓存机制。它将覆盖Dot类的eval方法与BinaryExpr类的setField方法。此外，它还会为每个类添加用于实现缓存功能的classInfo字段与index字段（其中，Dot类还会额外新增一个isField字段）

Dot类的eval方法用于读取字段的值，或充当方法调用表达式。setField方法用于处理赋值表达式左侧是某个对象的字段时的情况。这里的setField方法正是代码清单12.6中由AssignEx修改器定义的setField方法。这两个方法都会将由修改器添加的classInfo字段的值，与当前正被执行方法调用或字段引用的对象类型进行比较。如果相同，则再次利用上一次保存的值

经过InlineCache修改器的修改后，解释器将支持内联缓存功能。解释器本身的程序与代码清单12.7中的相同，不过，应用了修改器的解释器需要通过代码清单12.10中的启动程序运行。它与代码清单12.10及更早的代码清单12.8中的启动程序大同小异，唯一的区别在于启动程序中的run方法将接收不同类型的修改器

**代码清单12.9 InlineCache.java**
```java
package chap12;
import java.util.List;
import Stone.StoneException;
import Stone.ast.ASTree;
import Stone.ast.Dot;
import chap6.Environment;
import javassist.gluonj.*;

@Require(ObjOptimizer.class)
@Reviser
public class InlineCache {
	@Reviser
	public static class DotEx2 extends ObjOptimizer.DotEx {
		protected OptClassInfo classInfo = null;
		protected boolean isField;
		protected int index;

		public DotEx2(List<ASTree> c) {
			super(c);
		}

		@Override
		public Object eval(Environment env, Object value) {
			if (value instanceof OptStoneObject) {
				OptStoneObject target = (OptStoneObject) value;
				if (target.classInfo() != classInfo)
					updateCache(target);
				if (isField)
					return target.read(index);
				else
					return target.method(index);
			} else
				return super.eval(env, value);
		}

		protected void updateCache(OptStoneObject target) {
			String member = name();
			classInfo = target.classInfo();
			Integer i = classInfo.fieldIndex(member);
			if (i != null) {
				isField = true;
				index = i;
				return;
			}
			i = classInfo.methodIndex(member);
			if (i != null) {
				isField = false;
				index = i;
				return;
			}
			throw new StoneException("bad member access: " + member, this);
		}
	}

	@Reviser
	public static class AssignEx2 extends ObjOptimizer.AssignEx {
		protected OptClassInfo classInfo = null;
		protected int index;

		public AssignEx2(List<ASTree> c) {
			super(c);
		}

		@Override
		protected Object setField(OptStoneObject obj, Dot expr, Object rvalue) {
			if (obj.classInfo() != classInfo) {
				String member = expr.name();
				classInfo = obj.classInfo();
				Integer i = classInfo.fieldIndex(member);
				if (i == null)
					throw new StoneException("bad member access: " + member, this);
				index = i;
			}
			obj.write(index, rvalue);
			return rvalue;
		}
	}
}
```

**代码清单12.10 InlineRunner.java**
```java
package chap12;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class InlineRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(ObjOptInterpreter.class, args, InlineCache.class, NativeEvaluator.class);
	}
}
```

**代码清单12.11 测试斐波那契数的计算时间（面向对象版本）**
```
class Fib {
    fib0 = 0
    fib1 = 1
    def fib (n) {
        if n == 0 {
            fib0
        } else {
            if n == 1 {
                this.fib1
            } else {
                fib(n - 1) + this.fib(n - 2)
            }
        }
    }
}
t = currentTime()
f = Fib.new
f.fib 33
print currentTime() - t + " msec"
```
运行效果如下：
![](https://s1.ax1x.com/2018/12/22/FyYBOH.gif#shadow)

可以看到优化后执行所需的时间约6秒，第九天没优化的执行所需时间约8秒
### 第十二天的思维导图
![](https://s1.ax1x.com/2018/12/22/FyYWp8.png#shadow)
