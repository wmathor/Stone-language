在使用中间代码解释器时，我们要事先将抽象语法树转换为中间代码。简单来说，中间代码是一种虚拟的机器语言，因此，中间代码的转换方法，其实与编译器将抽象语法树转换为真正的机器语言时采用的方法大体相同

### 中间代码与机器语言
之间利用抽象语法树，语言处理器需要在节点之间往返操作，这是一件费时的工作。因此，如果语言处理器能够实现计算遍历顺序，并以此重新排列节点，执行开销就可能有所降低

通常，语言处理器不会直接将重新排列的抽象语法树节点作为中间代码使用。如果直接保存抽象语法树的节点，多余的无用信息是一种空间上的浪费，因此，我们需要设计一种虚拟的机器语言，并将各个节点转换为与该节点运算逻辑对应的机器语言。大多数语言处理器使用的中间语言都是这种转换后的代码（见下图）

![](https://s1.ax1x.com/2018/12/22/FyN9Kg.png#shadow)
### Stone虚拟机
今天设计的中间代码解释器称为Stone虚拟机。它处理的中间语言称为虚拟机器语言

Stone虚拟机由若干个通用寄存器与内存组成。内存分为四个区域，分别是栈（stack）区、堆（heap）区、程序代码区与文字常量区。虚拟机器语言保存于程序代码区，字符串字面量保存干文字常量区

从实际的机器语言的角度来看，计算机能大致分为两种设备，即内存与寄存器访问器，内存是一个巨大的byte数组，寄存器访问器则用于对若干个通用寄存器进行读写操作。这里暂不考虑其他的输入输出设备。byte类型用于表示8位二进制整数，相当于Java语言中的1字节

内存虽然是一个byte数组，但它也能处理其他类型的值。实际的程序通常需要处理32位整数、浮点小数或字符串等各种类型的值，而这些值都将通过8位整数值的组合表现。例如，32位整数将以8位为一组分解成4组，并分别保存至内存，即byte数组的元素中。这种保存方式称为编码（encode）。因此，编译器在从内存中读取这些值时需要进行相应的解码（decode）处理

![](https://s1.ax1x.com/2018/12/22/FyNMqJ.png#shadow)

处理器将从指定位置开始依次读取用于表示内存的数组元素，并根据元素的值执行相应的操作。例如，如果数组元素的值为1，处理器将对寄存器的值求和；如果为2，则从内存连续读取4个元素，将它们解码为一个32位整数后，再保存至寄存器中。这些用于表示操作类型的数字称为机器语言指令。为了实现if语句等条件判断逻辑，机器语言指令还支持根据不同的条件读取相应地址的指令

对于有些机器语言指令，仅凭1个byte数组元素（即1字节）无法完全表现所要实行的操作内容。例如，在执行四则运算时，除了运算类型，机器语言指令还必须标明需要进行计算的寄存器。因此，大部分机器语言指令将通过多个连续元素值的组合（即若干字节）来表现需要执行的操作。这也是一种编码处理

通常，从机器语言的角度来看，实际的内存是一个byte类型的数组。为了简化设计，在Stone虚拟机中仅有程序代码区由byte数组实现，栈区和堆区都是Object类型的数组，文字常量区则是String类型的数组。通用寄存器的值也以object类型表示。因此，虚拟机在向内存保存各种类型的值时，不必对值进行编码或解码，虚拟机器语言的程序实现得到了简化

Stone虚拟机除了通用寄存器外还提供了pc、fp、sp和ret这四个寄存器。它们都能保存int类型的整数值。pc是程序计数器。若pc的值为i，虚拟机将执行程序代码区从前端数起的第i个元素中保存的机器语言指令。fp与sp分别是帧指针（frame pointer）与栈指针（stack pointer），它们都用于管理栈区。ret用于函数的调用操作

表13.1是虚拟机器语言指令一览

Machine Language | Mean
---|---
iconst int32 reg | 将整数值int32保存至reg
bconst int8 reg | 将整数值int8保存至reg
sconst int16 reg | 将字符串常量区的第int16个字符串字面量保存至reg
move src dest | 在栈与寄存器，或寄存器之间进行值赋值操作（src与dest可以实reg或int8）
gmove src dest | 在堆与寄存器之间进行值赋值操作（src与dest可以是reg或int16）
ifzero reg int16 | 如果reg的值为0，则跳转至int16分支
goto int16 | 强制跳转至int16分支
call reg int8 | 调用函数reg，该函数将调用int18个参数（同时，call之后的指令地址将被保存至ret寄存器）
return | 跳转至reg寄存器存储的分支地址
save int8 | 将寄存器的值转移至栈中，并更改寄存器fp与sp的值
restore int8 | 还原之前转移至栈中的寄存器值
neg reg | 反转reg中保存的值的正负号
add a b | 计算a + b后保存至a
sub a b | 计算a - b后保存至a
mul a b | 计算a × b后保存至a
div a b | 计算a ÷ b后保存至a
rem a b | 计算a ÷ b的余数后将余数保存至a
equal a b | 如果a = b则将a赋值为1，否则赋值为0
move a b | 如果a > b则将a赋值为1，否则赋值为0
less a b | 如果a < b则将a赋值为1，否则赋值为0

**代码清单13.1 Opcode.java**
```java
package chap13;
import Stone.StoneException;

public class Opcode {
	public static final byte ICONST = 1; // load an integer
	public static final byte BCONST = 2; // load an 8bit (1byte) integer
	public static final byte SCONST = 3; // load a character string
	public static final byte MOVE = 4; // move a value
	public static final byte GMOVE = 5; // move a value (global variable)
	public static final byte IFZERO = 6; // branch if false
	public static final byte GOTO = 7; // always branch
	public static final byte CALL = 8; // call a function
	public static final byte RETURN = 9; // return
	public static final byte SAVE = 10; // save all registers
	public static final byte RESTORE = 11; // restore all registers
	public static final byte NEG = 12; // arithmetic negation
	public static final byte ADD = 13; // add
	public static final byte SUB = 14; // subtract
	public static final byte MUL = 15; // multiply
	public static final byte DIV = 16; // divide
	public static final byte REM = 17; // remainder
	public static final byte EQUAL = 18; // equal
	public static final byte MORE = 19; // more than
	public static final byte LESS = 20; // less than

	public static byte encodeRegister(int reg) {
		if (reg > StoneVM.NUM_OF_REG)
			throw new StoneException("too many registers required");
		else
			return (byte) -(reg + 1);
	}

	public static int decodeRegister(byte operand) {
		return -1 - operand;
	}

	public static byte encodeOffset(int offset) {
		if (offset > Byte.MAX_VALUE)
			throw new StoneException("too big byte offset");
		else
			return (byte) offset;
	}

	public static short encodeShortOffset(int offset) {
		if (offset < Short.MIN_VALUE || Short.MAX_VALUE < offset)
			throw new StoneException("too big short offset");
		else
			return (short) offset;
	}

	public static int decodeOffset(byte operand) {
		return operand;
	}

	public static boolean isRegister(byte operand) {
		return operand < 0;
	}

	public static boolean isOffset(byte operand) {
		return operand >= 0;
	}
}
```

**代码清单13.2 HeapMemory.java**
```java
package chap13;

public interface HeapMemory {
	Object read(int index);

	void write(int index, Object v);
}
```

**代码清单13.3 StoneVM.java**
```java
package chap13;
import static chap13.Opcode.*;
import chap8.NativeFunction;
import Stone.StoneException;
import Stone.ast.ASTree;
import Stone.ast.ASTList;
import java.util.ArrayList;

public class StoneVM {
	protected byte[] code;
	protected Object[] stack;
	protected String[] strings;
	protected HeapMemory heap;

	public int pc, fp, sp, ret;
	protected Object[] registers;
	public final static int NUM_OF_REG = 6;
	public final static int SAVE_AREA_SIZE = NUM_OF_REG + 2;

	public final static int TRUE = 1;
	public final static int FALSE = 0;

	public StoneVM(int codeSize, int stackSize, int stringsSize, HeapMemory hm) {
		code = new byte[codeSize];
		stack = new Object[stackSize];
		strings = new String[stringsSize];
		registers = new Object[NUM_OF_REG];
		heap = hm;
	}

	public Object getReg(int i) {
		return registers[i];
	}

	public void setReg(int i, Object value) {
		registers[i] = value;
	}

	public String[] strings() {
		return strings;
	}

	public byte[] code() {
		return code;
	}

	public Object[] stack() {
		return stack;
	}

	public HeapMemory heap() {
		return heap;
	}

	public void run(int entry) {
		pc = entry;
		fp = 0;
		sp = 0;
		ret = -1;
		while (pc >= 0)
			mainLoop();
	}

	protected void mainLoop() {
		switch (code[pc]) {
		case ICONST:
			registers[decodeRegister(code[pc + 5])] = readInt(code, pc + 1);
			pc += 6;
			break;
		case BCONST:
			registers[decodeRegister(code[pc + 2])] = (int) code[pc + 1];
			pc += 3;
			break;
		case SCONST:
			registers[decodeRegister(code[pc + 3])] = strings[readShort(code, pc + 1)];
			pc += 4;
			break;
		case MOVE:
			moveValue();
			break;
		case GMOVE:
			moveHeapValue();
			break;
		case IFZERO: {
			Object value = registers[decodeRegister(code[pc + 1])];
			if (value instanceof Integer && ((Integer) value).intValue() == 0)
				pc += readShort(code, pc + 2);
			else
				pc += 4;
			break;
		}
		case GOTO:
			pc += readShort(code, pc + 1);
			break;
		case CALL:
			callFunction();
			break;
		case RETURN:
			pc = ret;
			break;
		case SAVE:
			saveRegisters();
			break;
		case RESTORE:
			restoreRegisters();
			break;
		case NEG: {
			int reg = decodeRegister(code[pc + 1]);
			Object v = registers[reg];
			if (v instanceof Integer)
				registers[reg] = -((Integer) v).intValue();
			else
				throw new StoneException("bad operand value");
			pc += 2;
			break;
		}
		default:
			if (code[pc] > LESS)
				throw new StoneException("bad instruction");
			else
				computeNumber();
			break;
		}
	}

	protected void moveValue() {
		byte src = code[pc + 1];
		byte dest = code[pc + 2];
		Object value;
		if (isRegister(src))
			value = registers[decodeRegister(src)];
		else
			value = stack[fp + decodeOffset(src)];
		if (isRegister(dest))
			registers[decodeRegister(dest)] = value;
		else
			stack[fp + decodeOffset(dest)] = value;
		pc += 3;
	}

	protected void moveHeapValue() {
		byte rand = code[pc + 1];
		if (isRegister(rand)) {
			int dest = readShort(code, pc + 2);
			heap.write(dest, registers[decodeRegister(rand)]);
		} else {
			int src = readShort(code, pc + 1);
			registers[decodeRegister(code[pc + 3])] = heap.read(src);
		}
		pc += 4;
	}

	protected void callFunction() {
		Object value = registers[decodeRegister(code[pc + 1])];
		int numOfArgs = code[pc + 2];
		if (value instanceof VmFunction && ((VmFunction) value).parameters().size() == numOfArgs) {
			ret = pc + 3;
			pc = ((VmFunction) value).entry();
		} else if (value instanceof NativeFunction && ((NativeFunction) value).numOfParameters() == numOfArgs) {
			Object[] args = new Object[numOfArgs];
			for (int i = 0; i < numOfArgs; i++)
				args[i] = stack[sp + i];
			stack[sp] = ((NativeFunction) value).invoke(args, new ASTList(new ArrayList<ASTree>()));
			pc += 3;
		} else
			throw new StoneException("bad function call");
	}

	protected void saveRegisters() {
		int size = decodeOffset(code[pc + 1]);
		int dest = size + sp;
		for (int i = 0; i < NUM_OF_REG; i++)
			stack[dest++] = registers[i];
		stack[dest++] = fp;
		fp = sp;
		sp += size + SAVE_AREA_SIZE;
		stack[dest++] = ret;
		pc += 2;
	}

	protected void restoreRegisters() {
		int dest = decodeOffset(code[pc + 1]) + fp;
		for (int i = 0; i < NUM_OF_REG; i++)
			registers[i] = stack[dest++];
		sp = fp;
		fp = ((Integer) stack[dest++]).intValue();
		ret = ((Integer) stack[dest++]).intValue();
		pc += 2;
	}

	protected void computeNumber() {
		int left = decodeRegister(code[pc + 1]);
		int right = decodeRegister(code[pc + 2]);
		Object v1 = registers[left];
		Object v2 = registers[right];
		boolean areNumbers = v1 instanceof Integer && v2 instanceof Integer;
		if (code[pc] == ADD && !areNumbers)
			registers[left] = String.valueOf(v1) + String.valueOf(v2);
		else if (code[pc] == EQUAL && !areNumbers) {
			if (v1 == null)
				registers[left] = v2 == null ? TRUE : FALSE;
			else
				registers[left] = v1.equals(v2) ? TRUE : FALSE;
		} else {
			if (!areNumbers)
				throw new StoneException("bad operand value");
			int i1 = ((Integer) v1).intValue();
			int i2 = ((Integer) v2).intValue();
			int i3;
			switch (code[pc]) {
			case ADD:
				i3 = i1 + i2;
				break;
			case SUB:
				i3 = i1 - i2;
				break;
			case MUL:
				i3 = i1 * i2;
				break;
			case DIV:
				i3 = i1 / i2;
				break;
			case REM:
				i3 = i1 % i2;
				break;
			case EQUAL:
				i3 = i1 == i2 ? TRUE : FALSE;
				break;
			case MORE:
				i3 = i1 > i2 ? TRUE : FALSE;
				break;
			case LESS:
				i3 = i1 < i2 ? TRUE : FALSE;
				break;
			default:
				throw new StoneException("never reach here");
			}
			registers[left] = i3;
		}
		pc += 3;
	}

	public static int readInt(byte[] array, int index) {
		return (array[index] << 24) | ((array[index + 1] & 0xff) << 16) | ((array[index + 2] & 0xff) << 8)
				| (array[index + 3] & 0xff);
	}

	public static int readShort(byte[] array, int index) {
		return (array[index] << 8) | (array[index + 1] & 0xff);
	}
}
```
### 通过栈实现环境
首先，我们通过堆区来实现用于记录全局变量的环境。全局变量只需使用一个环境，因此，我们将直接使用整个堆区。Stone虚拟机使用的堆区实体，是一个由代码清单13.2中的HeapMemory接口实现的对象。该接口的read与write方法能够以数组的形式操作对象。代码清单13.4中的StoneVMEnv类的对象用于表示堆区。该类继承了第11章代码清单11.2中的ResizableArrayEnv类，并实现了HeapMemory接口（见下图）

![](https://s1.ax1x.com/2018/12/22/FydYR0.png#shadow)

**代码清单13.4 StoneVMEnv.java**
```java
package chap13;
import chap11.ResizableArrayEnv;

public class StoneVMEnv extends ResizableArrayEnv implements HeapMemory {
    protected StoneVM svm;
    protected Code code;
    public StoneVMEnv(int codeSize, int stackSize, int stringsSize) {
        svm = new StoneVM(codeSize, stackSize, stringsSize, this);
        code = new Code(svm);
    }
    public StoneVM stoneVM() { return svm; }
    public Code code() { return code; }
    public Object read(int index) { return values[index]; }
    public void write(int index, Object v) { values[index] = v; }
}
```
### 寄存器的使用
各个节点将根据该规定转换为虚拟机器语言，并依次排序。最终，整棵抽象语法树都将被转换为虚拟机器语言。例如，下图是由表达式(7 + x) * y转换得到的虚拟机器语言。图的左侧是抽象语法树，右侧是与之对应的虚拟机器语言

![](https://s1.ax1x.com/2018/12/22/FydyJ1.png#shadow)

上图中，与节点7对应的虚拟机器语言为bconst 7 r0。它将把整数7保存至寄存器r0中

接下来看一下节点+该如何转换为虚拟机器语言。首先，由左侧的节点7转换得到的虚拟机器语言将被写入程序代码区。在这条虚拟机器语言之后，紧跟着的是由右侧的节点x转换得到的虚拟机器语言。在执行左侧的虚拟机器语言后，该语句的计算结果必须作为表达式的中间结果，保存至寄存器r0，因此，在将右侧转换为虚拟机器语言时，寄存器r0将处于占用状态。于是，根据以上规定，右侧的虚拟机器语言的计算结果将保存于寄存器r1而非r0中

将与左右两侧节点对应的虚拟机器语言写入程序代码区后，语言处理器将接着写入加法指令add。add指令将把保存了中间结果（即左侧的计算结果）的寄存器r0与保存了上一个计算结果（即右侧的计算结果）的寄存器r1相加，并保存计算结果至寄存器r0中

符合规定的虚拟机器语言片段将随着语法树的遍历递归生成，并依次写入程序代码区，通过这种方式，语言处理器能够轻松地将抽象语法树转换为机器语言。对于上面的例子，语言处理器只要在节点+的虚拟机器语言后，继续将节点y的虚拟机器语言写入程序代码区，最后再写入乘法指令mul，就完成了整棵抽象语法树的机器语言转换。整个转换过程的要点在于，用于保存中间计算结果的寄存器必须处于占用状态，不能为之后的虚拟机器语言所用

虽然语言处理器能够通过上述方法轻松地将抽象语法树转换为机器语言，但转换得到的机器语言性能较差。该版本的机器语言存在不少不足，寄存器的使用也有些冗繁。现假定我们要对表达式7 + x + x进行转换。此时，最佳的虚拟机器语言如下
```
bconst 7 r0
move 变量x的值 r1
add r0 r1
add r0 r1
```
然而，根据以上介绍的方法，我们将得到一个更加冗长的转换结果。虚拟机器语言中将包含两条move指令，两次执行变量x的值至寄存器的复制操作
### 引用变量的值
在虚拟机器语言中，move或gmove指令能够将变量的值复制至寄存器中。move指令的格式如下
```
move 3 r1
```
他将把局部变量的值复制到第1个寄存器r1中。实际复制的值是栈区的第fp+3个元素的值。他也是在当前环境中从前往后数第3个局部变量的值

反之。下面的指令能够将寄存器r1中保存的值复制给同一个局部变量
```
move r1 3
```
另一个gmove方法则用于全局变量的复制，它可以将全局变量的值复制到寄存器中
```
gmove 3 r1
```
这条机器语言能够将堆区从前往后数第3个元素的值复制至寄存器r1中。如果替换3与r1的位置，就能反过来将寄存器的值复制到堆区中

由于在确定变量值的保存位置时，变量所属的作用域也会一并记录，因此我们能够根据作用域区分目标变量是一个局部变量还是全局变量。只要遵循该规律，语言处理器就能在进行虚拟机器语言转换时确定应当使用move还是gmove
### if语句与while语句
语言处理器能够通过分支指令将表示if语句与while语句的抽象语法树节点转换为虚拟机器语言。Stone 虚拟机提供了ifzero与goto这两种分支指令

Stone虚拟机将始终执行程序计数器（寄存器pc）当前指向的机器语言指令。如果pc的值为50，虚拟机将执行程序代码区中从前往后数的第50个元素保存的指令

在执行完一条指令后，pc值将自动增加与该指令长度相同的量，以指向下一条指令。因此，Stone虚拟机能够依次执行各条指令。同时，pc的值能够由分支指令更改。ifzero能在指定寄存器的值为0时将某个整数值加至寄存器pc，goto则能强制增加pc的值。pc值的增加量中不包括被执行的分支指令的长度。如果pc值的增加量为正，程序将向前跳转，如果为负则向后（反方向）跳转。我们将寄存器pc的这一用于实现分支跳转的增加量称为偏移量

if语句对应的抽象语法树节点的虚拟机器语言转换过程如下图所示。由if语句的条件表达式转换得到的虚拟机器语言将在执行后把结果保存至寄存器r0中。在Stone语言中，只有0为假（false），其他值都为真（true），因此ifzero指令能够在条件表达式的值为假时，跳转执行else代码块中的语句

![](https://s1.ax1x.com/2018/12/22/Fyww1P.png#shadow)

while语句对应的抽象语法树节点的虚拟机器语言转换过程如下图所示

![](https://s1.ax1x.com/2018/12/22/FywhcV.png#shadow)
### 函数的定义与调用
在讨论如何将函数体转换为虚拟机器语言之前，首先要考虑函数调用所需的实参与返回值的传递方式，以及栈帧的切换机制等问题。在为实际的处理器设计机器语言时也要考虑这些规则，人们通常将它们称为调用惯例（calling convention）

函数的调用方需要将实参保存至栈区中。也就是说，该调用方需要直接把实参保存至被调用函数使用的栈帧内。函数的参数将始终保存在栈帧前端，且虚拟机能够事先确定函数的参数与局部变量在栈帧中的保存位置（见下图）。如果参数不止一个，它们将依次存入栈帧前端

![](https://s1.ax1x.com/2018/12/22/FywO91.png#shadow)

Stone虚拟机能够通过call指令执行函数调用。函数的调用方将首先把实参保存至被调用函数的栈帧中。被调用函数的栈帧与调用方的栈帧相邻，因此只要知道调用方栈帧的大小，寄存器就能确定实参的保存位置。假设函数调用方栈帧的大小为s，第i（i≥1）个参数将保存于栈区的第fp + s + i - 1个元素中
### 转换为虚拟机器语言
Stone语言处理器将在执行过程中以对话的形式获取程序输入，之后先把它转换为抽象语法树，再进一步将抽象语法树转换为虚拟机器语言并执行。因此虚拟机器语言的转换时间也包含在执行时间中。由于这个原因，我们不需要将整个程序都转换为虚拟机器语言，仅需转换函数的定义部分

def语句用于定义函数，DefStmnt类是与之对应的抽象语法树节点类，该类的eval方法和原先一样，将返回一个表示函数的对象。与此同时，虚拟机会将函数体转换为虚拟机器语言，并把用于表示函数的对象记录至虚拟机器语言的前端。代码清单13.5是用于表示函数的对象的类定义。它是第7天代码清单7.8中的Function类的子类。entry字段表示虚拟机器语言前端所处的位置

**代码清单13.5 VmFunction.java**
```java
package chap13;
import Stone.ast.BlockStmnt;
import Stone.ast.ParameterList;
import chap6.Environment;
import chap7.Function;

public class VmFunction extends Function {
	protected int entry;

	public VmFunction(ParameterList parameters, BlockStmnt body, Environment env, int entry) {
		super(parameters, body, env);
		this.entry = entry;
	}

	public int entry() {
		return entry;
	}
}
```
抽象语法树各节点类的compile方法将实际执行把抽象语法树转换为虚拟机器语言的操作。该方法与eval及lookup方法类似，它也会一边依次遍历抽象语法树的节点，一边生成虚拟机器语言

代码清单13.6是抽象语法树各个米的compile方法。compile方法将接收一个code对象作为参数。代码清单13.7是该对象的类定义。该对象用于保存虚拟机器语言转换过程中必需的信息。例如，Stone虚拟机的引用（svm）、当前正在转换的函数的栈帧大小（framesize），以及当前正在使用的寄存器数量（nextReg）等信息都将通过Code对象保存

**代码清单13.6 VmEvaluator.java**
```java
package chap13;
import java.util.List;
import Stone.StoneException;
import Stone.Token;
import chap11.EnvOptimizer;
import chap6.Environment;
import chap6.BasicEvaluator.ASTreeEx;
import chap7.FuncEvaluator;
import javassist.gluonj.*;
import static chap13.Opcode.*;
import static javassist.gluonj.GluonJ.revise;
import Stone.ast.*;

@Require(EnvOptimizer.class)
@Reviser
public class VmEvaluator {
	@Reviser
	public static interface EnvEx3 extends EnvOptimizer.EnvEx2 {
		StoneVM stoneVM();

		Code code();
	}

	@Reviser
	public static abstract class ASTreeVmEx extends ASTree {
		public void compile(Code c) {
		}
	}

	@Reviser
	public static class ASTListEx extends ASTList {
		public ASTListEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			for (ASTree t : this)
				((ASTreeVmEx) t).compile(c);
		}
	}

	@Reviser
	public static class DefStmntVmEx extends EnvOptimizer.DefStmntEx {
		public DefStmntVmEx(List<ASTree> c) {
			super(c);
		}

		@Override
		public Object eval(Environment env) {
			String funcName = name();
			EnvEx3 vmenv = (EnvEx3) env;
			Code code = vmenv.code();
			int entry = code.position();
			compile(code);
			((EnvEx3) env).putNew(funcName, new VmFunction(parameters(), body(), env, entry));
			return funcName;
		}

		public void compile(Code c) {
			c.nextReg = 0;
			c.frameSize = size + StoneVM.SAVE_AREA_SIZE;
			c.add(SAVE);
			c.add(encodeOffset(size));
			((ASTreeVmEx) revise(body())).compile(c);
			c.add(MOVE);
			c.add(encodeRegister(c.nextReg - 1));
			c.add(encodeOffset(0));
			c.add(RESTORE);
			c.add(encodeOffset(size));
			c.add(RETURN);
		}
	}

	@Reviser
	public static class ParamsEx2 extends EnvOptimizer.ParamsEx {
		public ParamsEx2(List<ASTree> c) {
			super(c);
		}

		@Override
		public void eval(Environment env, int index, Object value) {
			StoneVM vm = ((EnvEx3) env).stoneVM();
			vm.stack()[offsets[index]] = value;
		}
	}

	@Reviser
	public static class NumberEx extends NumberLiteral {
		public NumberEx(Token t) {
			super(t);
		}

		public void compile(Code c) {
			int v = value();
			if (Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE) {
				c.add(BCONST);
				c.add((byte) v);
			} else {
				c.add(ICONST);
				c.add(v);
			}
			c.add(encodeRegister(c.nextReg++));
		}
	}

	@Reviser
	public static class StringEx extends StringLiteral {
		public StringEx(Token t) {
			super(t);
		}

		public void compile(Code c) {
			int i = c.record(value());
			c.add(SCONST);
			c.add(encodeShortOffset(i));
			c.add(encodeRegister(c.nextReg++));
		}
	}

	@Reviser
	public static class NameEx2 extends EnvOptimizer.NameEx {
		public NameEx2(Token t) {
			super(t);
		}

		public void compile(Code c) {
			if (nest > 0) {
				c.add(GMOVE);
				c.add(encodeShortOffset(index));
				c.add(encodeRegister(c.nextReg++));
			} else {
				c.add(MOVE);
				c.add(encodeOffset(index));
				c.add(encodeRegister(c.nextReg++));
			}
		}

		public void compileAssign(Code c) {
			if (nest > 0) {
				c.add(GMOVE);
				c.add(encodeRegister(c.nextReg - 1));
				c.add(encodeShortOffset(index));
			} else {
				c.add(MOVE);
				c.add(encodeRegister(c.nextReg - 1));
				c.add(encodeOffset(index));
			}
		}
	}

	@Reviser
	public static class NegativeEx extends NegativeExpr {
		public NegativeEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			((ASTreeVmEx) operand()).compile(c);
			c.add(NEG);
			c.add(encodeRegister(c.nextReg - 1));
		}
	}

	@Reviser
	public static class BinaryEx extends BinaryExpr {
		public BinaryEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			String op = operator();
			if (op.equals("=")) {
				ASTree l = left();
				if (l instanceof Name) {
					((ASTreeVmEx) right()).compile(c);
					((NameEx2) l).compileAssign(c);
				} else
					throw new StoneException("bad assignment", this);
			} else {
				((ASTreeVmEx) left()).compile(c);
				((ASTreeVmEx) right()).compile(c);
				c.add(getOpcode(op));
				c.add(encodeRegister(c.nextReg - 2));
				c.add(encodeRegister(c.nextReg - 1));
				c.nextReg--;
			}
		}

		protected byte getOpcode(String op) {
			if (op.equals("+"))
				return ADD;
			else if (op.equals("-"))
				return SUB;
			else if (op.equals("*"))
				return MUL;
			else if (op.equals("/"))
				return DIV;
			else if (op.equals("%"))
				return REM;
			else if (op.equals("=="))
				return EQUAL;
			else if (op.equals(">"))
				return MORE;
			else if (op.equals("<"))
				return LESS;
			else
				throw new StoneException("bad operator", this);
		}
	}

	@Reviser
	public static class PrimaryVmEx extends FuncEvaluator.PrimaryEx {
		public PrimaryVmEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			compileSubExpr(c, 0);
		}

		public void compileSubExpr(Code c, int nest) {
			if (hasPostfix(nest)) {
				compileSubExpr(c, nest + 1);
				((ASTreeVmEx) revise(postfix(nest))).compile(c);
			} else
				((ASTreeVmEx) operand()).compile(c);
		}
	}

	@Reviser
	public static class ArgumentsEx extends Arguments {
		public ArgumentsEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			int newOffset = c.frameSize;
			int numOfArgs = 0;
			for (ASTree a : this) {
				((ASTreeVmEx) a).compile(c);
				c.add(MOVE);
				c.add(encodeRegister(--c.nextReg));
				c.add(encodeOffset(newOffset++));
				numOfArgs++;
			}
			c.add(CALL);
			c.add(encodeRegister(--c.nextReg));
			c.add(encodeOffset(numOfArgs));
			c.add(MOVE);
			c.add(encodeOffset(c.frameSize));
			c.add(encodeRegister(c.nextReg++));
		}

		public Object eval(Environment env, Object value) {
			if (!(value instanceof VmFunction))
				throw new StoneException("bad function", this);
			VmFunction func = (VmFunction) value;
			ParameterList params = func.parameters();
			if (size() != params.size())
				throw new StoneException("bad number of arguments", this);
			int num = 0;
			for (ASTree a : this)
				((ParamsEx2) params).eval(env, num++, ((ASTreeEx) a).eval(env));
			StoneVM svm = ((EnvEx3) env).stoneVM();
			svm.run(func.entry());
			return svm.stack()[0];
		}
	}

	@Reviser
	public static class BlockEx extends BlockStmnt {
		public BlockEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			if (this.numChildren() > 0) {
				int initReg = c.nextReg;
				for (ASTree a : this) {
					c.nextReg = initReg;
					((ASTreeVmEx) a).compile(c);
				}
			} else {
				c.add(BCONST);
				c.add((byte) 0);
				c.add(encodeRegister(c.nextReg++));
			}
		}
	}

	@Reviser
	public static class IfEx extends IfStmnt {
		public IfEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			((ASTreeVmEx) condition()).compile(c);
			int pos = c.position();
			c.add(IFZERO);
			c.add(encodeRegister(--c.nextReg));
			c.add(encodeShortOffset(0));
			int oldReg = c.nextReg;
			((ASTreeVmEx) thenBlock()).compile(c);
			int pos2 = c.position();
			c.add(GOTO);
			c.add(encodeShortOffset(0));
			c.set(encodeShortOffset(c.position() - pos), pos + 2);
			ASTree b = elseBlock();
			c.nextReg = oldReg;
			if (b != null)
				((ASTreeVmEx) b).compile(c);
			else {
				c.add(BCONST);
				c.add((byte) 0);
				c.add(encodeRegister(c.nextReg++));
			}
			c.set(encodeShortOffset(c.position() - pos2), pos2 + 1);
		}
	}

	@Reviser
	public static class WhileEx extends WhileStmnt {
		public WhileEx(List<ASTree> c) {
			super(c);
		}

		public void compile(Code c) {
			int oldReg = c.nextReg;
			c.add(BCONST);
			c.add((byte) 0);
			c.add(encodeRegister(c.nextReg++));
			int pos = c.position();
			((ASTreeVmEx) condition()).compile(c);
			int pos2 = c.position();
			c.add(IFZERO);
			c.add(encodeRegister(--c.nextReg));
			c.add(encodeShortOffset(0));
			c.nextReg = oldReg;
			((ASTreeVmEx) body()).compile(c);
			int pos3 = c.position();
			c.add(GOTO);
			c.add(encodeShortOffset(pos - pos3));
			c.set(encodeShortOffset(c.position() - pos2), pos2 + 2);
		}
	}
}
```

**代码清单13.7 Code.java**
```java
package chap13;

public class Code {
	protected StoneVM svm;
	protected int codeSize;
	protected int numOfStrings;
	protected int nextReg;
	protected int frameSize;

	public Code(StoneVM stoneVm) {
		svm = stoneVm;
		codeSize = 0;
		numOfStrings = 0;
	}

	public int position() {
		return codeSize;
	}

	public void set(short value, int pos) {
		svm.code()[pos] = (byte) (value >>> 8);
		svm.code()[pos + 1] = (byte) value;
	}

	public void add(byte b) {
		svm.code()[codeSize++] = b;
	}

	public void add(short i) {
		add((byte) (i >>> 8));
		add((byte) i);
	}

	public void add(int i) {
		add((byte) (i >>> 24));
		add((byte) (i >>> 16));
		add((byte) (i >>> 8));
		add((byte) i);
	}

	public int record(String s) {
		svm.strings()[numOfStrings] = s;
		return numOfStrings++;
	}
}
```
### 通过虚拟机执行
最后，看一下通过Stone虚拟机执行程序的解释器与它的启动程序

**代码清单13.8 VmInterpreter.java**
```java
package chap13;
import Stone.FuncParser;
import Stone.ParseException;
import chap11.EnvOptInterpreter;
import chap8.Natives;

public class VmInterpreter extends EnvOptInterpreter {
	public static void main(String[] args) throws ParseException {
		run(new FuncParser(), new Natives().environment(new StoneVMEnv(100000, 100000, 1000)));
	}
}
```

**代码清单13.9 VmRunner.java**
```java
package chap13;
import javassist.gluonj.util.Loader;
import chap8.NativeEvaluator;

public class VmRunner {
	public static void main(String[] args) throws Throwable {
		Loader.run(VmInterpreter.class, args, VmEvaluator.class, NativeEvaluator.class);
	}
}
```
### 第十三天的思维导图
![](https://s1.ax1x.com/2018/12/22/Fy0Q4s.png#shadow)
