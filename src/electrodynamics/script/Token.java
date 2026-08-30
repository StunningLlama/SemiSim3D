package electrodynamics.script;

public class Token {
	String chars;
	TokenType type;
	Bounds bounds;
	
	public Token(String chars, TokenType type) {
		this.chars = chars;
		this.type = type;
	}
	
	public Token(String chars, TokenType type, int i_start, int i_end) {
		this.chars = chars;
		this.type = type;
		this.bounds = new Bounds(i_start, i_end);
	}
	
	@Override
	public String toString() {
		return chars + ":" + type.toString();
	}
	
	public boolean isComment() {
		return type == TokenType.PUNCTUATION && chars.equals("#");
	}
	
	public enum TokenType {
		NAME, NUMBER, PUNCTUATION, STRING, UNKNOWN;
		
		public static TokenType fromInt(int i) {
			if (i == 1)
				return NUMBER;
			else if (i == 2)
				return NAME;
			else if (i == 3 || i == 4)
				return PUNCTUATION;
			else
				return UNKNOWN;
		}
	}
}