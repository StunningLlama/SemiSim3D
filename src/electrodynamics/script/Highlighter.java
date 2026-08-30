package electrodynamics.script;

import java.awt.Color;

public interface Highlighter {
	public void resetHighlight();
	public void markText(Bounds bounds, Color color);
	public void markError(Bounds bounds, Color color, String error_message);
	
	public default Color getKeywordColor() { return COL_KEYWORD; }
	public default Color getNumberColor() { return COL_NUMBER; }
	public default Color getStringColor() { return COL_STRING; }
	public default Color getFunctionColor() { return COL_FUNCTION; }
	public default Color getVariableColor() { return COL_VARIABLE; }
	public default Color getOperatorColor() { return COL_OPERATOR; }
	public default Color getCommentColor() { return COL_COMMENT; }
	public default Color getDefaultColor() { return COL_DEFAULT; }

	public default Color getDefaultBackgroundColor() { return COL_BG_DEFAULT; }
	public default Color getErrorColor() { return COL_ERROR; }
	
	//TODO Dark mode
	static Color COL_KEYWORD = new Color(0xa41400);
	static Color COL_NUMBER = new Color(0x0251a9);
	static Color COL_STRING = new Color(0x1b92a8);
	static Color COL_FUNCTION = new Color(0xbe7109);
	static Color COL_VARIABLE = new Color(0x87258a);
	static Color COL_OPERATOR = new Color(0x000000);
	static Color COL_COMMENT = new Color(0x8a7f72);
	static Color COL_DEFAULT = new Color(0x000000);

	static Color COL_BG_DEFAULT = new Color(0x00ffffff, true);
	static Color COL_ERROR = new Color(0xffaeae);
}
