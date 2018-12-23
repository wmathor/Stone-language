package chap14;
import Stone.StoneException;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class JavaLoader {
	protected ClassLoader loader;
	protected ClassPool cpool;

	public JavaLoader() {
		cpool = new ClassPool(null);
		cpool.appendSystemPath();
		loader = new ClassLoader(this.getClass().getClassLoader()) {
		};
	}

	public Class<?> load(String className, String method) {
		// System.out.println(method);
		CtClass cc = cpool.makeClass(className);
		try {
			cc.addMethod(CtMethod.make(method, cc));
			return cc.toClass(loader, null);
		} catch (CannotCompileException e) {
			throw new StoneException(e.getMessage());
		}
	}
}