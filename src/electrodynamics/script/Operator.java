package electrodynamics.script;

public class Operator {
	public int precedence;
	public Association assoc;
	public Function func;
	public Operator(int p, Association a, Function o)
	{
		precedence = p;
		assoc = a;
		func = o;
	}
	
	public enum Association {
		LEFT, RIGHT
	}
}