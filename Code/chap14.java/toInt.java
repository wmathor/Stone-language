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