// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.probe;

import electrodynamics.Renderer;
import electrodynamics.Simulation;
import electrodynamics.Renderer.ScalarView;
import electrodynamics.units.Units;
import electrodynamics.util.Utils;

public class PointProbe extends Probe {
	public PointProbe(int mx, int my, int mz) {
		super(mx, my, mz);
		x = mx;
		y = my;
		z = mz;
		calculateDefaultLabelCoords();
	}

	public int x = 0;
	public int y = 0;
	public int z = 0;

	public ScalarView scalarname = ScalarView.NONE;
	public double[][][] scalarfield = new double[][][] {{{0}}};
	
	@Override
	public void reset() {
		value = 0;
	}

	@Override
	public void calculateDefaultLabelCoords() {
		labelcoord.x = x;
		labelcoord.y = y;
		labelcoord.z = z;
	}

	@Override
	public void measure(Simulation e, boolean savedatapoint) {
		e.computeScalarField(scalarfield, x, y, z, scalarname, 0, 0);
		value = scalarfield[0][0][0];
		if (savedatapoint) data.addData(value, e.time);
	}
	
	@Override
	public boolean isMouseHovering(int mx, int my, int mz) {
		return Utils.length(x-mx, y-my, z-mz) < 3;
	}
	
	@Override
	public PointProbe clone() {
		PointProbe p = null;
		p = (PointProbe) super.clone();
		p.data = data.clone();
		p.labelcoord = labelcoord.clone();
		return p;
	}
	
	@Override
	public void flip_x() {
		x = -x;
		labelcoord.flip_x();
	}

	@Override
	public void flip_y() {
		y = -y;
		labelcoord.flip_y();
	}

	@Override
	public void flip_z() {
		z = -z;
		labelcoord.flip_z();
	}

	@Override
	public void rot_x() {
		int tmp1 = y;
		int tmp2 = z;
		z = tmp1;
		y = -tmp2;
		labelcoord.rot_x();
	}

	@Override
	public void rot_y() {
		int tmp1 = z;
		int tmp2 = x;
		x = tmp1;
		z = -tmp2;
		labelcoord.rot_y();
	}

	@Override
	public void rot_z() {
		int tmp1 = x;
		int tmp2 = y;
		y = tmp1;
		x = -tmp2;
		labelcoord.rot_z();
	}

	@Override
	public void translate(int dx, int dy, int dz) {
		x += dx;
		y += dy;
		z += dz;
		labelcoord.translate(dx, dy, dz);
	}
	
	@Override
	public boolean intersects(int xmin, int ymin, int zmin, int xmax, int ymax, int zmax) {
		return (x >= xmin && x <= xmax && y >= ymin && y <= ymax && z >= zmin && z <= zmax);
	}
	
	@Override
	public boolean checkInBounds(Simulation e) {
		return (x >= 0 && x < e.nx && y >= 0 && y < e.ny && z >= 0 && z < e.nz);
	}

	@Override
	public void drag(int mx, int my, int mz) {
		x = mx;
		y = my;
		z = mz;
		calculateDefaultLabelCoords();
	}

	@Override
	public void draw(Renderer r) {
		r.setalphaBG(1);
		r.setalphaFG(1.0);
		r.setColorFloat(0.5f, 1.0f, 1.0f);
		r.drawPixelRectangle(x-1, y-1, z-1, 3, 3, 3);
	}
	
	@Override
	public double getXcenter() {
		return x;
	}

	@Override
	public double getYcenter() {
		return y;
	}

	@Override
	public double getZcenter() {
		return z;
	}

	@Override
	public String getText(Units units) {
		return shorthand + name + " = " + units.toString_fixedsigfigs(value, quantity);

	}
}