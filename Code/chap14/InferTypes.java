package chap14;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import Stone.ast.ASTree;
import javassist.gluonj.Reviser;
import chap14.TypeInfo.UnknownType;

@Reviser
public class InferTypes {
	@Reviser
	public static class TypeInfoEx extends TypeInfo {
		@Override
		public void assertSubtypeOf(TypeInfo type, TypeEnv tenv, ASTree where) throws TypeException {
			if (type.isUnknownType())
				((UnknownTypeEx) type.toUnknownType()).assertSupertypeOf(this, tenv, where);
			else
				super.assertSubtypeOf(type, tenv, where);
		}

		@Override
		public TypeInfo union(TypeInfo right, TypeEnv tenv) {
			if (right.isUnknownType())
				return right.union(this, tenv);
			else
				return super.union(right, tenv);
		}

		@Override
		public TypeInfo plus(TypeInfo right, TypeEnv tenv) {
			if (right.isUnknownType())
				return right.plus(this, tenv);
			else
				return super.plus(right, tenv);
		}
	}

	@Reviser
	public static class UnknownTypeEx extends TypeInfo.UnknownType {
		protected TypeInfo type = null;

		public boolean resolved() {
			return type != null;
		}

		public void setType(TypeInfo t) {
			type = t;
		}

		@Override
		public TypeInfo type() {
			return type == null ? ANY : type;
		}

		@Override
		public void assertSubtypeOf(TypeInfo t, TypeEnv tenv, ASTree where) throws TypeException {
			if (resolved())
				type.assertSubtypeOf(t, tenv, where);
			else
				((TypeEnvEx) tenv).addEquation(this, t);
		}

		public void assertSupertypeOf(TypeInfo t, TypeEnv tenv, ASTree where) throws TypeException {
			if (resolved())
				t.assertSubtypeOf(type, tenv, where);
			else
				((TypeEnvEx) tenv).addEquation(this, t);
		}

		@Override
		public TypeInfo union(TypeInfo right, TypeEnv tenv) {
			if (resolved())
				return type.union(right, tenv);
			else {
				((TypeEnvEx) tenv).addEquation(this, right);
				return right;
			}
		}

		@Override
		public TypeInfo plus(TypeInfo right, TypeEnv tenv) {
			if (resolved())
				return type.plus(right, tenv);
			else {
				((TypeEnvEx) tenv).addEquation(this, INT);
				return right.plus(INT, tenv);
			}
		}
	}

	@Reviser
	public static class TypeEnvEx extends TypeEnv {
		public static class Equation extends ArrayList<UnknownType> {
		}

		protected List<Equation> equations = new LinkedList<Equation>();

		public void addEquation(UnknownType t1, TypeInfo t2) {
			// assert t1.unknown() == true
			if (t2.isUnknownType())
				if (((UnknownTypeEx) t2.toUnknownType()).resolved())
					t2 = t2.type();
			Equation eq = find(t1);
			if (t2.isUnknownType())
				eq.add(t2.toUnknownType());
			else {
				for (UnknownType t : eq)
					((UnknownTypeEx) t).setType(t2);
				equations.remove(eq);
			}
		}

		protected Equation find(UnknownType t) {
			for (Equation e : equations)
				if (e.contains(t))
					return e;
			Equation e = new Equation();
			e.add(t);
			equations.add(e);
			return e;
		}
	}
}