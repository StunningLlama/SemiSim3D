// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.probe;

import electrodynamics.Renderer;
import electrodynamics.Simulation;
import electrodynamics.Renderer.VectorView;
import electrodynamics.units.Units;
import electrodynamics.util.Utils;

public class LineProbe extends Probe {
	public LineProbe(int mx, int my, int mz) {
		super(mx, my, mz);
		x1 = mx;
		x2 = mx;
		y1 = my;
		y2 = my;
		z1 = mz;
		z2 = mz;
		calculateDefaultLabelCoords();
	}

	public int x1 = 0;
	public int y1 = 0;
	public int z1 = 0;
	
	public int x2 = 0;
	public int y2 = 0;
	public int z2 = 0;

	public VectorView vectorname = VectorView.NONE;
	
	@Override
	public void reset() {
		value = 0;
	}

	@Override
	public void calculateDefaultLabelCoords() {
		labelcoord.x = (x1+x2)/2;
		labelcoord.y = (y1+y2)/2;
		labelcoord.z = (z1+z2)/2;
	}

	@Override
	public void measure(Simulation e, boolean savedatapoint) {
		double[][][][] vf = {null, null, null};
		e.computeVectorField(vf, vectorname);
		double[][][] vf_x = vf[0];
		double[][][] vf_y = vf[1];
		double[][][] vf_z = vf[2];
		
		if (vf_x == null || vf_y == null || vf_z == null) return;
		
		int n = 0;
		int m = 0;
		int l_min = 0;
		int l_max = 0;

		if (x1 != x2) {
			n = y1;
			m = z1;
			l_min = Math.min(x1, x2);
			l_max = Math.max(x1, x2);
		} else if (y1 != y2) {
			n = x1;
			m = z1;
			l_min = Math.min(y1, y2);
			l_max = Math.max(y1, y2);
		} else if (z1 != z2) {
			n = x1;
			m = y1;
			l_min = Math.min(z1, z2);
			l_max = Math.max(z1, z2);
		} else {
			return;
		}

		double J = 0;
		for (int l = l_min; l < l_max; l++) {
			if (x1 != x2) {
				J += vf_x[l][n][m]*e.ds;
			} else if (y1 != y2) {
				J += vf_y[n][l][m]*e.ds;
			} else if (z1 != z2) {
				J += vf_y[n][m][l]*e.ds;
			}
		}
		
		value = J;
		if (savedatapoint) data.addData(value, e.time);
	}
	
	@Override
	public boolean isMouseHovering(int mx, int my, int mz) {
		return Utils.length(x1-mx, y1-my, z1-mz) < 3 || Utils.length(x2-mx, y2-my, z2-mz) < 3;
	}
	
	@Override
	public LineProbe clone() {
		LineProbe p = null;
		p = (LineProbe) super.clone();
		p.data = data.clone();
		p.labelcoord = labelcoord.clone();
		return p;
	}

	public double accumCurrent(int x0, int y0, int x1, int y1, double[][] Jx, double[][] Jy, double ds) {
		int dx = x1-x0;
		int dy = y1-y0;
		if (dx == 1 && dy == 0) {
			return -Jy[x0+1][y0]*ds;
		} else if (dx == -1 && dy == 0) {
			return Jy[x0][y0]*ds;
		} else if (dx == 0 && dy == 1) {
			return Jx[x0][y0+1]*ds;
		} else if (dx == 0 && dy == -1) {
			return -Jx[x0][y0]*ds;
		} else if (dx == 1 && dy == 1) {
			return (Jx[x0][y0+1] - Jy[x0+1][y0+1])*ds;
		} else if (dx == 1 && dy == -1) {
			return (-Jx[x0][y0] - Jy[x0+1][y0-1])*ds;
		} else if (dx == -1 && dy == 1) {
			return (+Jx[x0][y0+1] + Jy[x0][y0+1])*ds;
		} else if (dx == -1 && dy == -1) {
			return (-Jx[x0][y0] + Jy[x0][y0-1])*ds;
		}
		return 0;
	}
	
	@Override
	public void flip_x() {
		x1 = -x1;
		x2 = -x2;
		labelcoord.flip_x();
	}

	@Override
	public void flip_y() {
		y1 = -y1;
		y2 = -y2;
		labelcoord.flip_y();
	}

	@Override
	public void flip_z() {
		z1 = -z1;
		z2 = -z2;
		labelcoord.flip_z();
	}

	@Override
	public void rot_x() {
		int tmp1 = y1;
		int tmp2 = z1;
		z1 = tmp1;
		y1 = -tmp2;
		tmp1 = y2;
		tmp2 = z2;
		z2 = tmp1;
		y2 = -tmp2;
		labelcoord.rot_x();
	}

	@Override
	public void rot_y() {
		int tmp1 = z1;
		int tmp2 = x1;
		x1 = tmp1;
		z1 = -tmp2;
		tmp1 = z2;
		tmp2 = x2;
		x2 = tmp1;
		z2 = -tmp2;
		labelcoord.rot_y();
	}

	@Override
	public void rot_z() {
		int tmp1 = x1;
		int tmp2 = y1;
		y1 = tmp1;
		x1 = -tmp2;
		tmp1 = x2;
		tmp2 = y2;
		y2 = tmp1;
		x2 = -tmp2;
		labelcoord.rot_z();
	}
	
	@Override
	public void translate(int dx, int dy, int dz) {
		x1 += dx;
		y1 += dy;
		z1 += dz;
		x2 += dx;
		y2 += dy;
		z2 += dz;
		labelcoord.translate(dx, dy, dz);
	}
	
	@Override
	public boolean intersects(int xmin, int ymin, int zmin, int xmax, int ymax, int zmax) {
		return (x1 >= xmin && x1 <= xmax && y1 >= ymin && y1 <= ymax && z1 >= zmin && z1 <= zmax) || (x2 >= xmin && x2 <= xmax && y2 >= ymin && y2 <= ymax && z2 >= zmin && z2 <= zmax);
	}
	
	@Override
	public boolean checkInBounds(Simulation e) {
		return (x1 >= 0 && x1 < e.nx && y1 >= 0 && y1 < e.ny) && (x2 >= 0 && x2 < e.nx && y2 >= 0 && y2 < e.ny) && (z2 >= 0 && z2 < e.nz && z2 >= 0 && z2 < e.nz);
	}

	@Override
	public void drag(int mx, int my, int mz) {
		if (z2 != z1) {
			x2 = x1;
			y2 = y1;
			z2 = mz;
		} else if (x2 != x1) {
			y2 = y1;
			z2 = z1;
			x2 = mx;
		} else if (y2 != y1) {
			x2 = x1;
			z2 = z1;
			y2 = my;
		} else {
			x2 = mx;
			y2 = my;
			z2 = mz;
		}
		
		calculateDefaultLabelCoords();
	}

	@Override
	public void draw(Renderer r) {
		r.setalphaBG(1);
		r.setalphaFG(1.0);
		r.setColorFloat(0.5f, 1.0f, 1.0f);
		r.drawPixelRectangle(x1-1, y1-1, z1-1, 3, 3, 3);
		r.drawPixelRectangle(x2-1, y2-1, z2-1, 3, 3, 3);

		r.setalphaFG(0.3);
		r.setColorFloat(0.5f, 1.0f, 1.0f);
		r.drawPixelLine(x1, y1, z1, x2, y2, z2);
	}
	
	@Override
	public double getXcenter() {
		return 0.5*(x1+x2);
	}

	@Override
	public double getYcenter() {
		return 0.5*(y1+y2);
	}

	@Override
	public double getZcenter() {
		return 0.5*(z1+z2);
	}

	@Override
	public String getText(Units units) {
		return shorthand + name + " = " + units.toString_fixedsigfigs(value, quantity);
	}
}