// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.probe;

import electrodynamics.Renderer;
import electrodynamics.Simulation;
import electrodynamics.Renderer.ScalarView;
import electrodynamics.Renderer.VectorView;
import electrodynamics.units.Quantity;
import electrodynamics.units.Units;
import electrodynamics.util.OctahedralAction;

public abstract class Probe implements Cloneable, OctahedralAction {
	public static int data_size = 100;
	
	public LabelCoord labelcoord = new LabelCoord();
	public ProbeData data = new ProbeData();
	public boolean selected = false;
	public double value = 0;
	public Quantity quantity = Quantity.DIMENSIONLESS;
	public String shorthand = "";
	public String name = "";
	public boolean custom = false;

	public Probe(int mx, int my, int mz) {};
	public abstract void reset();
	public abstract void calculateDefaultLabelCoords();
	public abstract void measure(Simulation e, boolean savedatapoint);
	public abstract boolean isMouseHovering(int mx, int my, int mz);
	public abstract boolean intersects(int xmin, int ymin, int zmin, int xmax, int ymax, int zmax);
	public abstract boolean checkInBounds(Simulation e);
	public abstract void drag(int mx, int my, int mz);
	public abstract void draw(Renderer r);
	public abstract double getXcenter();
	public abstract double getYcenter();
	public abstract double getZcenter();
	public abstract String getText(Units units);
	public void prepareForSave() {};
	
	@Override
	public String toString() {
		return shorthand + name;
	}
	
	@Override
	public Probe clone() {
		try {
			return (Probe) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//TODO
	public void eliminateNulls() {
		if (data == null)
			data = new ProbeData();
		if (labelcoord == null)
			labelcoord = new LabelCoord();
		if (quantity == null)
			quantity = Quantity.DIMENSIONLESS;
		
		if (this instanceof VoltageProbe) {
			shorthand = "V";
			quantity = Quantity.ELECTRIC_POTENTIAL;
		} else if (this instanceof ChargeProbe) {
			quantity = Quantity.CHARGE;
			shorthand = "Q";
			((ChargeProbe)this).scalarname = ScalarView.CHARGE;
			((ChargeProbe)this).scalarfield = new double[][][] {{{0}}};
		} else if (this instanceof CurrentProbe) {
			shorthand = "I";
			quantity = Quantity.ELECTRIC_CURRENT;
			((CurrentProbe)this).vectorname = VectorView.TOTAL_CURRENT;
		} else if (this instanceof FluxProbe) {
			quantity = Quantity.MAGNETIC_FLUX;
			shorthand = "Φ";
			((FluxProbe)this).vectorname = VectorView.B_FIELD;
		} else if (this instanceof Ground) {
			shorthand = "V";
			quantity = Quantity.ELECTRIC_POTENTIAL;
		}
	}
	
	public class LabelCoord implements Cloneable, OctahedralAction {
		public int x = -1;
		public int y = -1;
		public int z = -1;
		
	    @Override
	    public LabelCoord clone() {
	        try {
				return (LabelCoord) super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
	    }
	    
		@Override
		public void flip_x() {
			x = -x;
		}

		@Override
		public void flip_y() {
			y = -y;
		}

		@Override
		public void flip_z() {
			z = -z;
		}

		@Override
		public void rot_x() {
			int tmp1 = y;
			int tmp2 = z;
			z = tmp1;
			y = -tmp2;
		}

		@Override
		public void rot_y() {
			int tmp1 = z;
			int tmp2 = x;
			x = tmp1;
			z = -tmp2;
		}

		@Override
		public void rot_z() {
			int tmp1 = x;
			int tmp2 = y;
			y = tmp1;
			x = -tmp2;
		}

		@Override
		public void translate(int dx, int dy, int dz) {
			x += dx;
			y += dy;
			z += dz;
		}
	}
	
	public class ProbeData implements Cloneable {
		public int data_size = Probe.data_size;
		public double[] data = new double[data_size];
		public double[] time = new double[data_size];
		
		public ProbeData() {
			resetData();
		}
		
		public void resetData() {
			for (int i = 0; i < data_size; i++) {
				data[i] = Double.NaN;
				time[i] = Double.NaN;
			}
		}
		
		public void fixWeirdIssue() {
			if (data == null)
				data = new double[data_size];
			if (time == null)
				time = new double[data_size];
			resetData();
		}
		
		public void addData(double datapoint, double timepoint) {
			for (int i = 0; i < data_size - 1; i++) {
				data[i] = data[i+1];
				time[i] = time[i+1];
			}
			data[data_size-1] = datapoint;
			time[data_size-1] = timepoint;
		}
		
	    @Override
	    public ProbeData clone() {
	        ProbeData dat = null;
			try {
				dat = (ProbeData) super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
	        dat.data = data.clone();
	        dat.time = time.clone();
	        return dat;
	    }
	}
}
