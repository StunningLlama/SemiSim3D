// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.probe;

import electrodynamics.Renderer;
import electrodynamics.Renderer.ScalarView;
import electrodynamics.Simulation;
import electrodynamics.units.Units;

public class VolumeProbe extends Probe {
	public VolumeProbe(int mx, int my, int mz) {
		super(mx, my, mz);
		x1 = mx;
		x2 = mx;
		y1 = my;
		y2 = my;
		z1 = mz;
		z2 = mz;
		calculateDefaultLabelCoords();
	}

	public int x1;
	public int y1;
	public int z1;

	public int x2;
	public int y2;
	public int z2;

	public ScalarView scalarname = ScalarView.NONE;
	public double[][][] scalarfield = new double[][][] {{{0}}};
	
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
		double Q = 0;

		int n_min = 0;
		int n_max = 0;
		int m_min = 0;
		int m_max = 0;
		int l_min = 0;
		int l_max = 0;

		n_min = Math.min(x1, x2);
		n_max = Math.max(x1, x2);
		m_min = Math.min(y1, y2);
		m_max = Math.max(y1, y2);
		l_min = Math.min(z1, z2);
		l_max = Math.max(z1, z2);
		
		if (scalarfield.length != n_max-n_min+1 || scalarfield[0].length != m_max-m_min+1 || scalarfield[0][0].length != l_max-l_min+1) {
			scalarfield = new double[n_max-n_min+1][m_max-m_min+1][l_max-l_min+1];
		}
		
		e.computeScalarField(scalarfield, n_min, m_min, l_min, scalarname);

		for (int n = n_min; n <= n_max; n++) {
			for (int m = m_min; m <= m_max; m++) {
				for (int l = l_min; l <= l_max; l++) {
					Q += scalarfield[n-n_min][m-m_min][l-l_min]*(e.ds*e.ds*e.ds);
				}
			}
		}
		
		value = Q;
		if (savedatapoint) data.addData(value, e.time);
	}
	
	@Override
	public boolean isMouseHovering(int mx, int my, int mz) {
		return mx >= Math.min(x1, x2) && mx <= Math.max(x1, x2) && my >= Math.min(y1, y2) && my <= Math.max(y1, y2) && mz >= Math.min(z1, z2) && mz <= Math.max(z1, z2);
	}
	
	@Override
	public VolumeProbe clone() {
		VolumeProbe p = null;
		p = (VolumeProbe) super.clone();
		p.data = data.clone();
		p.labelcoord = labelcoord.clone();
		return p;
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
		return (x1 >= xmin && x1 <= xmax && y1 >= ymin && y1 <= ymax && z1 >= zmin && z1 <= zmax) || (x2 >= xmin && x2 <= xmax && y2 >= ymin && y2 <= ymax || z2 >= zmin || z2 <= zmax);
	}

	@Override
	public boolean checkInBounds(Simulation e) {
		return (x1 >= 0 && x1 < e.nx && y1 >= 0 && y1 < e.ny) && (x2 >= 0 && x2 < e.nx && y2 >= 0 && y2 < e.ny);
	}

	@Override
	public void drag(int mx, int my, int mz) {
		x2 = mx;
		y2 = my;
		z2 = mz;
		calculateDefaultLabelCoords();
	}

	@Override
	public void draw(Renderer r) {
		r.setalphaBG(1);
		r.setalphaFG(1.0);
		r.setColorFloat(0.5f, 1.0f, 1.0f);

		r.setalphaFG(0.3);
		r.setColorFloat(0.5f, 1.0f, 1.0f);
		r.drawPixelRectangle(Math.min(x1,x2), Math.min(y1,y2), Math.min(z1,z2), Math.abs(x2-x1)+1, Math.abs(y2-y1)+1, Math.abs(z2-z1)+1);
	}

	@Override
	public String getText(Units units) {
		return shorthand + name + " = " + units.toString_fixedsigfigs(value, quantity);
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
}