package electrodynamics;

public enum MaterialType
{

	EMF					("Voltage source (Adjustable)",			230, 216, 46, 230),
	AC_EMF				("AC voltage source (Adjustable)",		230, 150, 216, 230),
	SWITCH				("Switch",								194, 194, 194, 120),
	METAL				("Metal",								153, 153, 153, 120),
	METAL_HIGH_C		("Conductive metal",					191, 191, 191, 120),
	METAL_LOW_C			("Resistive metal",						94, 94, 94, 120),
	METAL_HIGH_W		("High workfunction metal",				163, 116, 116, 120),
	METAL_LOW_W			("Low workfunction metal",				116, 121, 163, 120),
	SEMI				("Intrinsic semiconductor",				207,  161, 212, 120),
	SEMI_P_TYPE			("P-type semiconductor",				191,  74,  34, 120),
	SEMI_N_TYPE			("N-type semiconductor",				 84, 123, 191, 120),
	SEMI_HEAVY_P_TYPE	("Heavily doped P-type semiconductor",	204,  41,  41, 120),
	SEMI_HEAVY_N_TYPE	("Heavily doped N-type semiconductor",	 39,  52, 194, 120),
	SEMI_LIGHT_P_TYPE	("Lightly doped P-type semiconductor",	201, 131,  73, 120),
	SEMI_LIGHT_N_TYPE	("Lightly doped N-type semiconductor",	137, 188, 204, 120),
	DIELECTRIC			("Dielectric",							 81, 171,  51, 120),
	FERROMAGNET			("Ferromagnet",							116, 50, 117, 120),
	POS_CHARGE			("Positive static charge",				116, 50, 50, 120),
	NEG_CHARGE			("Negative static charge",				50, 50, 117, 120),
	DECO				("Decoration",							255, 255, 255, 255),
	ABSORBER			("Absorber",							 50,  50,  50),
	VACUUM				("Vacuum",								 20,  20,  20);

	String name;
	int color_r;
	int color_g;
	int color_b;
	int color_grayscale;

	MaterialType(String name, int r, int g, int b) {
		this.name = name;
		color_r = r;
		color_g = g;
		color_b = b;
		color_grayscale = (int)(0.7*Math.max(Math.max(r, g), b));
	}

	MaterialType(String name, int r, int g, int b, int grayscale_brightness) {
		this.name = name;
		color_r = r;
		color_g = g;
		color_b = b;
		color_grayscale = grayscale_brightness;
	}

	public static boolean isConducting(MaterialType material) {
		return (material == MaterialType.EMF
		|| material == MaterialType.AC_EMF
		|| material == MaterialType.SWITCH
		|| material == MaterialType.METAL
		|| material == MaterialType.METAL_HIGH_W
		|| material == MaterialType.METAL_LOW_W
		|| material == MaterialType.METAL_HIGH_C
		|| material == MaterialType.METAL_LOW_C);
	}

	public static boolean isSemiconducting(MaterialType material) {
		return (material == MaterialType.SEMI_P_TYPE
		|| material == MaterialType.SEMI_N_TYPE
		|| material == MaterialType.SEMI
		|| material == MaterialType.SEMI_HEAVY_P_TYPE
		|| material == MaterialType.SEMI_HEAVY_N_TYPE
		|| material == MaterialType.SEMI_LIGHT_P_TYPE
		|| material == MaterialType.SEMI_LIGHT_N_TYPE);
	}

	@Override
	public String toString() {
		return "Material: " + name;
	}
}