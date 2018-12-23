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