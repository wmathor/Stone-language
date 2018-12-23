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