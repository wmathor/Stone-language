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