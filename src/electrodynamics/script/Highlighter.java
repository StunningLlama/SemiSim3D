package electrodynamics.script;

import java.awt.Color;

public interface Highlighter {
	public void resetHighlight();
	public void markText(Bounds bounds, Color color);
	public void markError(Bounds bounds, Color color, String error_message);
	
	public default Color getKeywordColor() { return COL_KEYWORD.light_color; }
	public default Color getNumberColor() { return COL_NUMBER.light_color; }
	public default Color getStringColor() { return COL_STRING.light_color; }
	public default Color getFunctionColor() { return COL_FUNCTION.light_color; }
	public default Color getVariableColor() { return COL_VARIABLE.light_color; }
	public default Color getOperatorColor() { return COL_OPERATOR.light_color; }
	public default Color getCommentColor() { return COL_COMMENT.light_color; }
	public default Color getDefaultColor() { return COL_DEFAULT.light_color; }

	public default Color getDefaultBackgroundColor() { return COL_BG_DEFAULT.light_color; }
	public default Color getErrorColor() { return COL_ERROR.light_color; }
	
	static ThemedColor COL_KEYWORD = new ThemedColor(0xa41400);
	static ThemedColor COL_NUMBER = new ThemedColor(0x0251a9);
	static ThemedColor COL_STRING = new ThemedColor(0x1b92a8);
	static ThemedColor COL_FUNCTION = new ThemedColor(0xbe7109);
	static ThemedColor COL_VARIABLE = new ThemedColor(0x87258a);
	static ThemedColor COL_OPERATOR = new ThemedColor(0x000000);
	static ThemedColor COL_COMMENT = new ThemedColor(0x8a7f72);
	static ThemedColor COL_DEFAULT = new ThemedColor(0x000000);

	static ThemedColor COL_BG_DEFAULT = new ThemedColor(0x00ffffff, true);
	static ThemedColor COL_ERROR = new ThemedColor(0xffaeae);
	
	public class ThemedColor {
		public Color light_color;
		public Color dark_color;

		public ThemedColor(int rgb) {
			Color col = new Color(rgb);
			light_color = col;
			float[] hsb = new float[3];
			Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), hsb);
			dark_color = Color.getHSBColor(hsb[0], hsb[1], Math.max(0.85f, 1f-hsb[2]));
		}
		
		public ThemedColor(int argb, boolean alpha) {
			Color col = new Color(argb, alpha);
			light_color = col;
			float[] hsb = new float[3];
			Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), hsb);
			dark_color = Color.getHSBColor(hsb[0], hsb[1], Math.max(0.85f, 1f-hsb[2]));
		}
	}
}
