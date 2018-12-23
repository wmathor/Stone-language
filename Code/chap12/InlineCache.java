package chap12;
import java.util.List;
import Stone.StoneException;
import Stone.ast.ASTree;
import Stone.ast.Dot;
import chap6.Environment;
import javassist.gluonj.*;

@Require(ObjOptimizer.class)
@Reviser
public class InlineCache {
	@Reviser
	public static class DotEx2 extends ObjOptimizer.DotEx {
		protected OptClassInfo classInfo = null;
		protected boolean isField;
		protected int index;

		public DotEx2(List<ASTree> c) {
			super(c);
		}

		@Override
		public Object eval(Environment env, Object value) {
			if (value instanceof OptStoneObject) {
				OptStoneObject target = (OptStoneObject) value;
				if (target.classInfo() != classInfo)
					updateCache(target);
				if (isField)
					return target.read(index);
				else
					return target.method(index);
			} else
				return super.eval(env, value);
		}

		protected void updateCache(OptStoneObject target) {
			String member = name();
			classInfo = target.classInfo();
			Integer i = classInfo.fieldIndex(member);
			if (i != null) {
				isField = true;
				index = i;
				return;
			}
			i = classInfo.methodIndex(member);
			if (i != null) {
				isField = false;
				index = i;
				return;
			}
			throw new StoneException("bad member access: " + member, this);
		}
	}

	@Reviser
	public static class AssignEx2 extends ObjOptimizer.AssignEx {
		protected OptClassInfo classInfo = null;
		protected int index;

		public AssignEx2(List<ASTree> c) {
			super(c);
		}

		@Override
		protected Object setField(OptStoneObject obj, Dot expr, Object rvalue) {
			if (obj.classInfo() != classInfo) {
				String member = expr.name();
				classInfo = obj.classInfo();
				Integer i = classInfo.fieldIndex(member);
				if (i == null)
					throw new StoneException("bad member access: " + member, this);
				index = i;
			}
			obj.write(index, rvalue);
			return rvalue;
		}
	}
}