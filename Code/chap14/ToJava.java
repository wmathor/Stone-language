package chap14;
import java.util.ArrayList;
import java.util.List;
import chap11.ArrayEnv;
import chap11.EnvOptimizer;
import chap6.Environment;
import chap7.FuncEvaluator;
import Stone.StoneException;
import Stone.Token;
import Stone.ast.*;
import javassist.gluonj.Require;
import javassist.gluonj.Reviser;
import static javassist.gluonj.GluonJ.revise;

@Require(TypeChecker.class)
@Reviser
public class ToJava {
	public static final String METHOD = "m";
	public static final String LOCAL = "v";
	public static final String ENV = "env";
	public static final String RESULT = "res";
	public static final String ENV_TYPE = "chap11.ArrayEnv";

	public static String translateExpr(ASTree ast, TypeInfo from, TypeInfo to) {
		return translateExpr(((ASTreeEx) ast).translate(null), from, to);
	}

	public static String translateExpr(String expr, TypeInfo from, TypeInfo to) {
		from = from.type();
		to = to.type();
		if (from == TypeInfo.INT) {
			if (to == TypeInfo.ANY)
				return "new Integer(" + expr + ")";
			else if (to == TypeInfo.STRING)
				return "Integer.toString(" + expr + ")";
		} else if (from == TypeInfo.ANY)
			if (to == TypeInfo.STRING)
				return expr + ".toString()";
			else if (to == TypeInfo.INT)
				return "((Integer)" + expr + ").intValue()";
		return expr;
	}

	public static String returnZero(TypeInfo to) {
		if (to.type() == TypeInfo.ANY)
			return RESULT + "=new Integer(0);";
		else
			return RESULT + "=0;";
	}

	@Reviser
	public static interface EnvEx3 extends EnvOptimizer.EnvEx2 {
		JavaLoader javaLoader();
	}

	@Reviser
	public static class ArrayEnvEx extends ArrayEnv {
		public ArrayEnvEx(int size, Environment out) {
			super(size, out);
		}

		protected JavaLoader jloader = new JavaLoader();

		public JavaLoader javaLoader() {
			return jloader;
		}
	}

	@Reviser
	public static abstract class ASTreeEx extends ASTree {
		public String translate(TypeInfo result) {
			return "";
		}
	}

	@Reviser
	public static class NumberEx extends NumberLiteral {
		public NumberEx(Token t) {
			super(t);
		}

		public String translate(TypeInfo result) {
			return Integer.toString(value());
		}
	}

	@Reviser
	public static class StringEx extends StringLiteral {
		public StringEx(Token t) {
			super(t);
		}

		public String translate(TypeInfo result) {
			StringBuilder code = new StringBuilder();
			String literal = value();
			code.append('"');
			for (int i = 0; i < literal.length(); i++) {
				char c = literal.charAt(i);
				if (c == '"')
					code.append("\\\"");
				else if (c == '\\')
					code.append("\\\\");
				else if (c == '\n')
					code.append("\\n");
				else
					code.append(c);
			}
			code.append('"');
			return code.toString();
		}
	}

	@Reviser
	public static class NameEx3 extends TypeChecker.NameEx2 {
		public NameEx3(Token t) {
			super(t);
		}

		public String translate(TypeInfo result) {
			if (type.isFunctionType())
				return JavaFunction.className(name()) + "." + METHOD;
			else if (nest == 0)
				return LOCAL + index;
			else {
				String expr = ENV + ".get(0," + index + ")";
				return translateExpr(expr, TypeInfo.ANY, type);
			}
		}

		public String translateAssign(TypeInfo valueType, ASTree right) {
			if (nest == 0)
				return "(" + LOCAL + index + "=" + translateExpr(right, valueType, type) + ")";
			else {
				String value = ((ASTreeEx) right).translate(null);
				return "chap14.Runtime.write" + type.toString() + "(" + ENV + "," + index + "," + value + ")";
			}
		}
	}

	@Reviser
	public static class NegativeEx extends NegativeExpr {
		public NegativeEx(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			return "-" + ((ASTreeEx) operand()).translate(null);
		}
	}

	@Reviser
	public static class BinaryEx2 extends TypeChecker.BinaryEx {
		public BinaryEx2(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			String op = operator();
			if ("=".equals(op))
				return ((NameEx3) left()).translateAssign(rightType, right());
			else if (leftType.type() != TypeInfo.INT || rightType.type() != TypeInfo.INT) {
				String e1 = translateExpr(left(), leftType, TypeInfo.ANY);
				String e2 = translateExpr(right(), rightType, TypeInfo.ANY);
				if ("==".equals(op))
					return "chap14.Runtime.eq(" + e1 + "," + e2 + ")";
				else if ("+".equals(op)) {
					if (leftType.type() == TypeInfo.STRING || rightType.type() == TypeInfo.STRING)
						return e1 + "+" + e2;
					else
						return "chap14.Runtime.plus(" + e1 + "," + e2 + ")";
				} else
					throw new StoneException("bad operator", this);
			} else {
				String expr = ((ASTreeEx) left()).translate(null) + op + ((ASTreeEx) right()).translate(null);
				if ("<".equals(op) || ">".equals(op) || "==".equals(op))
					return "(" + expr + "?1:0)";
				else
					return "(" + expr + ")";
			}
		}
	}

	@Reviser
	public static class BlockEx2 extends TypeChecker.BlockEx {
		public BlockEx2(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			ArrayList<ASTree> body = new ArrayList<ASTree>();
			for (ASTree t : this)
				if (!(t instanceof NullStmnt))
					body.add(t);
			StringBuilder code = new StringBuilder();
			if (result != null && body.size() < 1)
				code.append(returnZero(result));
			else
				for (int i = 0; i < body.size(); i++)
					translateStmnt(code, body.get(i), result, i == body.size() - 1);
			return code.toString();
		}

		protected void translateStmnt(StringBuilder code, ASTree tree, TypeInfo result, boolean last) {
			if (isControlStmnt(tree))
				code.append(((ASTreeEx) tree).translate(last ? result : null));
			else if (last && result != null)
				code.append(RESULT).append('=').append(translateExpr(tree, type, result)).append(";\n");
			else if (isExprStmnt(tree))
				code.append(((ASTreeEx) tree).translate(null)).append(";\n");
			else
				throw new StoneException("bad expression statement", this);
		}

		protected static boolean isExprStmnt(ASTree tree) {
			if (tree instanceof BinaryExpr)
				return "=".equals(((BinaryExpr) tree).operator());
			return tree instanceof PrimaryExpr || tree instanceof VarStmnt;
		}

		protected static boolean isControlStmnt(ASTree tree) {
			return tree instanceof BlockStmnt || tree instanceof IfStmnt || tree instanceof WhileStmnt;
		}
	}

	@Reviser
	public static class IfEx extends IfStmnt {
		public IfEx(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			StringBuilder code = new StringBuilder();
			code.append("if(");
			code.append(((ASTreeEx) condition()).translate(null));
			code.append("!=0){\n");
			code.append(((ASTreeEx) thenBlock()).translate(result));
			code.append("} else {\n");
			ASTree elseBk = elseBlock();
			if (elseBk != null)
				code.append(((ASTreeEx) elseBk).translate(result));
			else if (result != null)
				code.append(returnZero(result));
			return code.append("}\n").toString();
		}
	}

	@Reviser
	public static class WhileEx extends WhileStmnt {
		public WhileEx(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			String code = "while(" + ((ASTreeEx) condition()).translate(null) + "!=0){\n"
					+ ((ASTreeEx) body()).translate(result) + "}\n";
			if (result == null)
				return code;
			else
				return returnZero(result) + "\n" + code;
		}
	}

	@Reviser
	public static class DefStmntEx3 extends TypeChecker.DefStmntEx2 {
		public DefStmntEx3(List<ASTree> c) {
			super(c);
		}

		@Override
		public Object eval(Environment env) {
			String funcName = name();
			JavaFunction func = new JavaFunction(funcName, translate(null), ((EnvEx3) env).javaLoader());
			((EnvEx3) env).putNew(funcName, func);
			return funcName;
		}

		public String translate(TypeInfo result) {
			StringBuilder code = new StringBuilder("public static ");
			TypeInfo returnType = funcType.returnType;
			code.append(javaType(returnType)).append(' ');
			code.append(METHOD).append("(chap11.ArrayEnv ").append(ENV);
			for (int i = 0; i < funcType.parameterTypes.length; i++) {
				code.append(',').append(javaType(funcType.parameterTypes[i])).append(' ').append(LOCAL).append(i);
			}
			code.append("){\n");
			code.append(javaType(returnType)).append(' ').append(RESULT).append(";\n");
			for (int i = funcType.parameterTypes.length; i < size; i++) {
				TypeInfo t = bodyEnv.get(0, i);
				code.append(javaType(t)).append(' ').append(LOCAL).append(i);
				if (t.type() == TypeInfo.INT)
					code.append("=0;\n");
				else
					code.append("=null;\n");
			}
			code.append(((ASTreeEx) revise(body())).translate(returnType));
			code.append("return ").append(RESULT).append(";}");
			return code.toString();
		}

		protected String javaType(TypeInfo t) {
			if (t.type() == TypeInfo.INT)
				return "int";
			else if (t.type() == TypeInfo.STRING)
				return "String";
			else
				return "Object";
		}
	}

	@Reviser
	public static class PrimaryEx2 extends FuncEvaluator.PrimaryEx {
		public PrimaryEx2(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			return translate(0);
		}

		public String translate(int nest) {
			if (hasPostfix(nest)) {
				String expr = translate(nest + 1);
				return ((PostfixEx) postfix(nest)).translate(expr);
			} else
				return ((ASTreeEx) operand()).translate(null);
		}
	}

	@Reviser
	public static abstract class PostfixEx extends Postfix {
		public PostfixEx(List<ASTree> c) {
			super(c);
		}

		public abstract String translate(String expr);
	}

	@Reviser
	public static class ArgumentsEx extends TypeChecker.ArgumentsEx {
		public ArgumentsEx(List<ASTree> c) {
			super(c);
		}

		public String translate(String expr) {
			StringBuilder code = new StringBuilder(expr);
			code.append('(').append(ENV);
			for (int i = 0; i < size(); i++)
				code.append(',').append(translateExpr(child(i), argTypes[i], funcType.parameterTypes[i]));
			return code.append(')').toString();
		}

		public Object eval(Environment env, Object value) {
			if (!(value instanceof JavaFunction))
				throw new StoneException("bad function", this);
			JavaFunction func = (JavaFunction) value;
			Object[] args = new Object[numChildren() + 1];
			args[0] = env;
			int num = 1;
			for (ASTree a : this)
				args[num++] = ((chap6.BasicEvaluator.ASTreeEx) a).eval(env);
			return func.invoke(args);
		}
	}

	@Reviser
	public static class VarStmntEx3 extends TypeChecker.VarStmntEx2 {
		public VarStmntEx3(List<ASTree> c) {
			super(c);
		}

		public String translate(TypeInfo result) {
			return LOCAL + index + "=" + translateExpr(initializer(), valueType, varType);
		}
	}
}