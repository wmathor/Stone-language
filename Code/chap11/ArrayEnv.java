package chap11;
import Stone.StoneException;
import chap11.EnvOptimizer.EnvEx2;
import chap6.Environment;

public class ArrayEnv implements Environment {
	protected Object[] values;
	protected Environment outer;

	public ArrayEnv(int size, Environment out) {
		values = new Object[size];
		outer = out;
	}

	public Symbols symbols() {
		throw new StoneException("no symbols");
	}

	public Object get(int nest, int index) {
		if (nest == 0)
			return values[index];
		else if (outer == null)
			return null;
		else
			return ((EnvEx2) outer).get(nest - 1, index);
	}

	public void put(int nest, int index, Object value) {
		if (nest == 0)
			values[index] = value;
		else if (outer == null)
			throw new StoneException("no outer environment");
		else
			((EnvEx2) outer).put(nest - 1, index, value);
	}

	public Object get(String name) {
		error(name);
		return null;
	}

	public void put(String name, Object value) {
		error(name);
	}

	public void putNew(String name, Object value) {
		error(name);
	}

	public Environment where(String name) {
		error(name);
		return null;
	}

	public void setOuter(Environment e) {
		outer = e;
	}

	private void error(String name) {
		throw new StoneException("cannot access by name: " + name);
	}
}