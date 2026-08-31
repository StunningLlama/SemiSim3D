package electrodynamics.script;

public class CodeException extends RuntimeException {
	private static final long serialVersionUID = 2662454885274139289L;
	public Bounds bounds;
	public CodeException(String msg, Bounds bounds)
	{
		super(msg);
		this.bounds = bounds;
	}
}