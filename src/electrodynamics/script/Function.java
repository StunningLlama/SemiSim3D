package electrodynamics.script;

public abstract interface Function {
	public default String getHelpText() {
		return "";
	}
	
	public interface BinaryOperator extends Function {
		public double operate(double b, double a);
	}

	public interface UnaryOperator extends Function {
		public double operate(double a);
	}

	public interface NaryFunction extends Function {
		public int get_n_args();
		public Object operate(Object[] args);
	}
}