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