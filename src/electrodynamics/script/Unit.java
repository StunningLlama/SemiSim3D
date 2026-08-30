package electrodynamics.script;

public class Unit {
	UnitType type;
	Operator op = null;
	Function func = null;
	String chars = "";
	Object value = null;
	Bounds bounds = null;

	@Override
	public String toString() {
		if (op != null)
			return chars + " (Op, " + op.precedence + ", " + op.assoc.toString() + ");";
		else if (func != null) {
			return chars + " (Func);";
		} else {
			return chars + " (" + type.toString() + ");";
		}
	}
	
	public enum UnitType {
		LEFT_PAREN, RIGHT_PAREN, COMMA, OPERATOR, FUNCTION, CONSTANT, NUMBER, STRING, VARIABLE
	}
}