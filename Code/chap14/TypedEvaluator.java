package chap14;
import java.util.List;
import javassist.gluonj.*;
import Stone.ast.*;
import chap11.EnvOptimizer;
import chap11.Symbols;
import chap11.EnvOptimizer.ASTreeOptEx;
import chap6.Environment;
import chap6.BasicEvaluator.ASTreeEx;

@Require(EnvOptimizer.class)
@Reviser
public class TypedEvaluator {
	@Reviser
	public static class DefStmntEx extends EnvOptimizer.DefStmntEx {
		public DefStmntEx(List<ASTree> c) {
			super(c);
		}

		public TypeTag type() {
			return (TypeTag) child(2);
		}

		@Override
		public BlockStmnt body() {
			return (BlockStmnt) child(3);
		}

		@Override
		public String toString() {
			return "(def " + name() + " " + parameters() + " " + type() + " " + body() + ")";
		}
	}

	@Reviser
	public static class ParamListEx extends EnvOptimizer.ParamsEx {
		public ParamListEx(List<ASTree> c) {
			super(c);
		}

		@Override
		public String name(int i) {
			return ((ASTLeaf) child(i).child(0)).token().getText();
		}

		public TypeTag typeTag(int i) {
			return (TypeTag) child(i).child(1);
		}
	}

	@Reviser
	public static class VarStmntEx extends VarStmnt {
		protected int index;

		public VarStmntEx(List<ASTree> c) {
			super(c);
		}

		public void lookup(Symbols syms) {
			index = syms.putNew(name());
			((ASTreeOptEx) initializer()).lookup(syms);
		}

		public Object eval(Environment env) {
			Object value = ((ASTreeEx) initializer()).eval(env);
			((EnvOptimizer.EnvEx2) env).put(0, index, value);
			return value;
		}
	}
}