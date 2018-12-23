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