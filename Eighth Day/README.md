我们还没有为Stone语言实现类似于Java语言中system.out.print1n的函数，因此程序还无法输出字符串显示。本章将继续扩展Stone语言，使它能够在程序中调用Java语言中的static方法

### 原生函数
Java语言提供了名为原生方法的功能，用于调用C语言等其他一些语言写成的函数。我们将为Stone语言添加类似的功能，让他能够调用由Java语言写成的函数

原生函数将由Arguments类的eval方法调用。代码清单8.1是用于改写Arguments类的eval方法的修改器。这个名为NativeArgEx的修改器标有extendsArgumentsEx一句，它修改的是Arguments类。ArgumentsEx是第7天代码清单7.7中定义的另一个修改器。这里的修改器继承了另一个修改器

通过这次修改，Arguments类eval方法将首先判断参数value是否为NativeFunction对象。参数value是一个由函数调用表达式的函数名得到的对象。eval方法之前返回的总是Function对象。如果参数是一个NativeFunction对象，eval方法将在计算实参序列并保存至数组args之后，调用NativeFunction对象的invoke来执行目标函数。如果参数不是NativeFunction对象，解释器将执行通常的函数调用

**代码清单8.1 NativeEvaluator.java**
```java
package chap8;
import java.util.List;
import Stone.StoneException;
import Stone.ast.ASTree;
import chap6.Environment;
import chap6.BasicEvaluator.ASTreeEx;
import chap7.FuncEvaluator;
import javassist.gluonj.*;

@Require(FuncEvaluator.class)
@Reviser
public class NativeEvaluator {
	@Reviser
	public static class NativeArgEx extends FuncEvaluator.ArgumentsEx {
		public NativeArgEx(List<ASTree> c) {
			super(c);
		}

		public Object eval(Environment callerEnv, Object value) {
			if (!(value instanceof NativeFunction))
				return super.eval(callerEnv, value);
			NativeFunction func = (NativeFunction) value;
			int nparams = func.numOfParameters();
			if (size() != nparams)
				throw new StoneException("bad number of arguments", this);
			Object[] args = new Object[nparams];
			int num = 0;
			for (ASTree a : this) {
				ASTreeEx ae = (ASTreeEx) a;
				args[num++] = ae.eval(callerEnv);
			}
			return func.invoke(args, this);
		}
	}
}
```
代码清单8.2是NativeFunction类。如果函数是一个原生函数，程序将在开始执行前创建NativeFunction类的对象，将由函数名与相应对象组成的名值对添加至环境中。该类的invoke方法将以参数args为参数调用Java语言的static方法

Method对象的invoke方法用于执行它表示的Java语言方法。invoke的第1个参数是执行该方法的对象。如果被执行的是一个static方法，该参数则为null。invoke的第2个参数用于保存传递给方法的实参序列

**代码清单8.2 NativeFunction.java**
```java
package chap8;
import java.lang.reflect.Method;
import Stone.StoneException;
import Stone.ast.ASTree;

public class NativeFunction {
	protected Method method;
	protected String name;
	protected int numParams;

	public NativeFunction(String n, Method m) {
		name = n;
		method = m;
		numParams = m.getParameterTypes().length;
	}

	public String toString() {
		return "<native:" + hashCode() + ">";
	}

	public int numOfParameters() {
		return numParams;
	}

	public Object invoke(Object[] args, ASTree tree) {
		try {
			return method.invoke(null, args);
		} catch (Exception e) {
			throw new StoneException("bad native function call: " + name, tree);
		}
	}
}
```
代码清单8.3中的程序会在执行前创建NativeFunction对象，并添加至环境中。其中，Natives类的environment方法将在调用后返回一个含有原生函数的环境

代码清单8.3向环境添加了print函数、read函数、1ength函数、toInt函数以及currentTime函数。关于这些原生函数的用途，请参见Natives类中的同名static方法

代码清单8.4与代码清单8.5分别是解释器程序及其启动程序。代码清单8.4中的解释器将首先调用Natives类的environment方法，创建一个包含原生函数的环境
### 编写使用原生函数的程序
在支持使用原生函数之后，Stone语言终于能够写出更加像样的程序了。例如代码清单8.6能够计算15的斐波那契数，并显示计算所花的时间

**代码清单8.3 Natives.java**
```java
package chap8;
import java.lang.reflect.Method;
import javax.swing.JOptionPane;
import Stone.StoneException;
import chap6.Environment;

public class Natives {
    public Environment environment(Environment env) {
        appendNatives(env);
        return env;
    }
    protected void appendNatives(Environment env) {
        append(env, "print", Natives.class, "print", Object.class);
        append(env, "read", Natives.class, "read");
        append(env, "length", Natives.class, "length", String.class);
        append(env, "toInt", Natives.class, "toInt", Object.class);
        append(env, "currentTime", Natives.class, "currentTime");
    }
    protected void append(Environment env, String name, Class<?> clazz,
                          String methodName, Class<?> ... params) {
        Method m;
        try {
            m = clazz.getMethod(methodName, params);
        } catch (Exception e) {
            throw new StoneException("cannot find a native function: "
                                     + methodName);
        }
        env.put(name, new NativeFunction(methodName, m));
    }

    // native methods
    public static int print(Object obj) {
        System.out.println(obj.toString());
        return 0;
    }
    public static String read() {
        return JOptionPane.showInputDialog(null);
    }
    public static int length(String s) { return s.length(); }
    public static int toInt(Object value) {
        if (value instanceof String)
            return Integer.parseInt((String)value);
        else if (value instanceof Integer)
            return ((Integer)value).intValue();
        else
            throw new NumberFormatException(value.toString());
    }
    private static long startTime = System.currentTimeMillis();
    public static int currentTime() {
        return (int)(System.currentTimeMillis() - startTime);
    }
}
```

**代码清单8.4 NativeInterpreter.java**
```java
package chap8;
import Stone.ClosureParser;
import Stone.ParseException;
import chap6.BasicInterpreter;
import chap7.NestedEnv;

public class NativeInterpreter extends BasicInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new ClosureParser(),new Natives().environment(new NestedEnv()));
	}
}
```

**代码清单8.5 NativeRunner.java**
```java
package chap8;
import javassist.gluonj.util.Loader;
import chap7.ClosureEvaluator;

public class NativeRunner {
    public static void main(String[] args) throws Throwable {
        Loader.run(NativeInterpreter.class, args, NativeEvaluator.class,
                   ClosureEvaluator.class);
    }
}
```

**代码清单8.6 计算斐波那契数列所需的时间**
```
def fib (n) {
    if n < 2 {
        n
    } else {
        fib (n - 1) + fib (n - 2)
    }
}
t = currentTime()
fib 15
print currentTime() - t + " msec"
```
运行效果如下
![](https://s1.ax1x.com/2018/12/19/FDYL5T.gif#shadow)

从上面运行效果来开，一开始计算大约需要9ms，之后变为2ms。可见Java虚拟机的动态编译能够提高程序的执行效率
### 第八天的思维导图
![](https://s1.ax1x.com/2018/12/19/FD0WeH.png#shadow)
