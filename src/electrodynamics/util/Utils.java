// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Electromagnetic Simulation which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.util;

public class Utils {
	public static int nx;
	public static int ny;
	public static int nz;
	
	public static double length(double x, double y) {
		return Math.sqrt(x*x+y*y);
	}
	

	public static double length(double x, double y, double z) {
		return Math.sqrt(x*x+y*y+z*z);
	}

	public static String getSI(double quantity, String unit, double lowerbound) {
		return getSI(Math.abs(quantity) < lowerbound? 0 : quantity, unit);
	}
	
	public static String getSI(double quantity, String unit) {
		if (!Double.isFinite(quantity))
			return Double.toString(quantity) + " " + unit;

		double mag = Math.abs(quantity);
		String precision = "%.2f";
		if (mag < 1E-27)
			return "0 " + unit;
		else if (mag < 1E-21)
			return String.format(precision, quantity*1e24) + " y" + unit;
		else if (mag < 1E-18)
			return String.format(precision, quantity*1e21) + " z" + unit;
		else if (mag < 1E-15)
			return String.format(precision, quantity*1e18) + " a" + unit;
		else if (mag < 1E-12)
			return String.format(precision, quantity*1e15) + " f" + unit;
		else if (mag < 1E-9)
			return String.format(precision, quantity*1e12) + " p" + unit;
		else if (mag < 1E-6)
			return String.format(precision, quantity*1e9) + " n" + unit;
		else if (mag < 1E-3)
			return String.format(precision, quantity*1e6) + " \u00b5" + unit;
		else if (mag < 1)
			return String.format(precision, quantity*1e3) + " m" + unit;
		else if (mag < 1E3)
			return String.format(precision, quantity) + " " + unit;
		else if (mag < 1E6)
			return String.format(precision, quantity*1e-3) + " k" + unit;
		else if (mag < 1E9)
			return String.format(precision, quantity*1e-6) + " M" + unit;
		else if (mag < 1E12)
			return String.format(precision, quantity*1e-9) + " G" + unit;
		else if (mag < 1E15)
			return String.format(precision, quantity*1e-12) + " P" + unit;
		else if (mag < 1E18)
			return String.format(precision, quantity*1e-15) + " E" + unit;
		else if (mag < 1E21)
			return String.format(precision, quantity*1e-18) + " Z" + unit;
		else if (mag < 1E27)
			return String.format(precision, quantity*1e-21) + " Y" + unit;
		else
			return "infinity " + unit;
	}
	
	public static double clamp(double val, double min, double max) {
		if ((val != val) || (val < min)) return min;
		if (val > max) return max;
		return val;
	}
	
	public static double logmean(double x, double y)
	{
		if (x <= 0 || y <= 0)
			return 0;

		//My approximation
		if (Math.abs((x-y)/(x+y)) <  1e-3)
			return (2/3.0)*Math.sqrt(x*y) + (1/6.0)*(x+y);

		return (x-y)/FastLog.log(x/y);
		//return (x-y)/Math.log(x/y);
	}
	
	public static double bilinearinterp(double[][] array, double x, double y, int nx, int ny) {
		int xfloor = (int)Math.floor(x);
		int yfloor = (int)Math.floor(y);
		double fx = x - xfloor;
		double fy = y - yfloor;
		if (Math.abs(x-Math.round(x)) < 1e-6 && Math.abs(y-Math.round(y)) < 1e-6) {
			int i = (int)Math.round(x);
			int j = (int)Math.round(y);
			if (i < 0) i = 0;
			if (j < 0) j = 0;
			if (i >= nx) i = nx - 1;
			if (j >= ny) j = ny - 1;
			return array[i][j];
		}

		if (xfloor < 0) {
			xfloor = 0;
			fx = 0.0;
		} else if (xfloor >= nx - 1) {
			xfloor = nx - 2;
			fx = 1.0;
		}
		if (yfloor < 0) {
			yfloor = 0;
			fy = 0.0;
		} else if (yfloor >= ny - 1) {
			yfloor = ny - 2;
			fy = 1.0;
		}
		double va = array[xfloor][yfloor]*(1.0-fx) + array[xfloor+1][yfloor]*fx;
		double vb = array[xfloor][yfloor+1]*(1.0-fx) + array[xfloor+1][yfloor+1]*fx;

		return va*(1.0-fy) + vb*fy;
	}
	
	public static double bilinearinterp(double[][][] array, double x, double y, double z) {
		int i0 = (int)Math.floor(x);
		int j0 = (int)Math.floor(y);
		int k0 = (int)Math.floor(z);
		int i1 = i0+1;
		int j1 = j0+1;
		int k1 = k0+1;

		if (i0 < 0) i0 = 0;
		if (j0 < 0) j0 = 0;
		if (k0 < 0) k0 = 0;

		if (i0 >= nx) i0 = nx-1;
		if (j0 >= ny) j0 = ny-1;
		if (k0 >= nz) k0 = nz-1;

		if (i1 < 0) i1 = 0;
		if (j1 < 0) j1 = 0;
		if (k1 < 0) k1 = 0;

		if (i1 >= nx) i1 = nx-1;
		if (j1 >= ny) j1 = ny-1;
		if (k1 >= nz) k1 = nz-1;

		double fx = x-i0;
		double fy = y-j0;
		double fz = z-k0;

		double x0y0 = (1-fz)*array[i0][j0][k0] + fz*array[i0][j0][k1];
		double x1y0 = (1-fz)*array[i1][j0][k0] + fz*array[i1][j0][k1];
		double x0y1 = (1-fz)*array[i0][j1][k0] + fz*array[i0][j1][k1];
		double x1y1 = (1-fz)*array[i1][j1][k0] + fz*array[i1][j1][k1];

		double x0 = (1-fy)*x0y0 + fy*x0y1;
		double x1 = (1-fy)*x1y0 + fy*x1y1;

		return x0*(1-fx)+x1*fx;
	}
	
	public static double bilinearinterp_geometric(double[][] array, double x, double y, int nx, int ny) {
		int xfloor = (int)Math.floor(x);
		int yfloor = (int)Math.floor(y);
		double fx = x - xfloor;
		double fy = y - yfloor;
		if (Math.abs(x-Math.round(x)) < 1e-6 && Math.abs(y-Math.round(y)) < 1e-6) {
			int i = (int)Math.round(x);
			int j = (int)Math.round(y);
			if (i < 0) i = 0;
			if (j < 0) j = 0;
			if (i >= nx) i = nx - 1;
			if (j >= ny) j = ny - 1;
			return array[i][j];
		}

		if (xfloor < 0) {
			xfloor = 0;
			fx = 0.0;
		} else if (xfloor >= nx - 1) {
			xfloor = nx - 2;
			fx = 1.0;
		}
		if (yfloor < 0) {
			yfloor = 0;
			fy = 0.0;
		} else if (yfloor >= ny - 1) {
			yfloor = ny - 2;
			fy = 1.0;
		}
		double va = Math.log(Math.abs(array[xfloor][yfloor]))*(1.0-fx) + Math.log(Math.abs(array[xfloor+1][yfloor]))*fx;
		double vb = Math.log(Math.abs(array[xfloor][yfloor+1]))*(1.0-fx) + Math.log(Math.abs(array[xfloor+1][yfloor+1]))*fx;

		return Math.exp(va*(1.0-fy) + vb*fy);
	}
}
