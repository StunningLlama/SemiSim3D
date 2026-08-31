package electrodynamics.script;

public abstract interface Function {
	public default String getHelpText() {
		return "";
	}

	public interface NullaryOperator extends Function {
		public double operate();
	}
	
	public interface UnaryOperator extends Function {
		public double operate(double a);
	}
	
	public interface BinaryOperator extends Function {
		public double operate(double b, double a);
	}
	
	public interface TernaryOperator extends Function {
		public double operate(double b, double a, double c);
	}

	public interface NaryFunction extends Function {
		public int get_n_args();
		public Object operate(Object[] args);
	}
}