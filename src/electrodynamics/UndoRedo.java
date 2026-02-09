// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

public class UndoRedo {
	
	public List<Snapshot> prev_states = new ArrayList<Snapshot>();
	public int undoredo_pointer = 0;
	public int history_size = 0;
	
	public UndoRedo(int history_size) {
		this.history_size = history_size;
	}

	public void resetUndoHistory(Electrodynamics e) {
		undoredo_pointer = 0;
		prev_states.clear();

		Snapshot state = new Snapshot();
		state.store(e);
		prev_states.add(state);
	}

	public void undo(Electrodynamics e) {
		undoredo_pointer--;
		if (undoredo_pointer < 0) undoredo_pointer = 0;

		if (undoredo_pointer >= 0 && undoredo_pointer < prev_states.size()) {
			prev_states.get(undoredo_pointer).load(e);
		}
	}

	public void redo(Electrodynamics e) {
		undoredo_pointer++;
		if (undoredo_pointer >= prev_states.size()) undoredo_pointer = prev_states.size()-1;

		if (undoredo_pointer >= 0 && undoredo_pointer < prev_states.size())
			prev_states.get(undoredo_pointer).load(e);
	}
	
	public void captureState(Electrodynamics e) {
		int i = undoredo_pointer+1;
		
		while (i < prev_states.size()) {
			prev_states.remove(i);
		}
		
		undoredo_pointer++;
		Snapshot state = new Snapshot();
		state.store(e);
		prev_states.add(state);

		while (prev_states.size() > history_size)
		{
			prev_states.remove(0);
			undoredo_pointer--;
		}
	}
}

class Snapshot {
	int resolution;
	double width;
	double time;
	
	boolean gui_paused;
	boolean gui_tooltip;
	boolean gui_text_bg;
	boolean gui_elem_colors;
	boolean gui_interface;
	int gui_simspeed;
	int gui_simspeed_2;
	int gui_brightness;
	int gui_brightness_vec ;
	String description;
	ScalarView gui_view;
	VectorView gui_view_vec;
	VectorMode gui_view_vec_mode;
	BoundaryCondition gui_bc;
	int gui_parameter1;
	RenderMode gui_3d_view;
	int gui_zslice;
	float pitch;
	float yaw;
	float zoom;
	int gui_parallax;
	boolean gui_rotate;
	
	double[][][] ex;
	double[][][] ey;
	double[][][] ez;
	double[][][] hx;
	double[][][] hy;
	double[][][] hz;
	double[][][] bx;
	double[][][] by;
	double[][][] bz;
	double[][][] rho_c;
	double[][][] rho_n;
	double[][][] rho_p;
	double[][][] rho_back;
	double[][][] rho_free;
	double[][][] jx_c;
	double[][][] jy_c;
	double[][][] jz_c;
	double[][][] jx_n;
	double[][][] jy_n;
	double[][][] jz_n;
	double[][][] jx_p;
	double[][][] jy_p;
	double[][][] jz_p;
	Material[][][] materials;
	
	List<VoltageProbe> voltageprobes;
	List<CurrentProbe> currentprobes;
	VoltageProbe ground;
	
	public void store(Electrodynamics e) {
		//resolution = e.resolution;
		//width = e.width;
		time = e.time;
		gui_paused = e.opts.gui_paused.isSelected();
		gui_tooltip = e.opts.gui_tooltip.isSelected();
		gui_text_bg = e.opts.gui_text_bg.isSelected();
		gui_elem_colors = e.opts.gui_elem_colors.isSelected();
		gui_simspeed = e.opts.gui_simspeed.getValue();
		gui_simspeed_2 = e.opts.gui_simspeed_2.getValue();
		gui_brightness = e.opts.gui_brightness.getValue();
		gui_brightness_vec = e.opts.gui_brightness_vec.getValue();
		description = e.opts.textPane.getText();
		gui_view = (ScalarView) e.opts.gui_view.getSelectedItem();
		gui_view_vec = (VectorView) e.opts.gui_view_vec.getSelectedItem();
		gui_view_vec_mode = (VectorMode) e.opts.gui_view_vec_mode.getSelectedItem();
		gui_bc = (BoundaryCondition) e.opts.gui_bc.getSelectedItem();
		gui_parameter1 = e.opts.gui_parameter1.getValue();
		gui_3d_view = (RenderMode) e.opts.gui_3d_view.getSelectedItem();
		gui_zslice = e.opts.gui_slice.getValue();
		pitch = e.renderer.pitch;
		yaw = e.renderer.yaw;
		zoom = e.renderer.scale;
		gui_parallax = e.opts.gui_parallax.getValue();
		gui_rotate = e.opts.gui_rotate.isSelected();

		ex = copy(e.Ex);
		ey = copy(e.Ey);
		ez = copy(e.Ez);
		hx = copy(e.Hx);
		hy = copy(e.Hy);
		hz = copy(e.Hz);
		bx = copy(e.Bx);
		by = copy(e.By);
		bz = copy(e.Bz);
		rho_c = copy(e.rho_abs);
		rho_n = copy(e.rho_n);
		rho_p = copy(e.rho_p);
		rho_back = copy(e.rho_back);
		rho_free = copy(e.rho_free);
		jx_c = copy(e.Jx_abs);
		jy_c = copy(e.Jy_abs);
		jz_c = copy(e.Jz_abs);
		jx_n = copy(e.Jx_n);
		jy_n = copy(e.Jy_n);
		jz_n = copy(e.Jz_n);
		jx_p = copy(e.Jx_p);
		jy_p = copy(e.Jy_p);
		jz_p = copy(e.Jz_p);
		materials = copy(e.materials);
		voltageprobes = new ArrayList<VoltageProbe>(e.voltageprobes);
		currentprobes = new ArrayList<CurrentProbe>(e.currentprobes);
		ground = e.ground;
	}


	public void load(Electrodynamics e) {
		SwingUtilities.invokeLater(() -> {
			e.rwLock.writeLock().lock();
			try {
				//int resolution_tmp = resolution;
				//double width_tmp = width;
				e.time = time;
				e.opts.gui_paused.setSelected(gui_paused);
				e.opts.gui_tooltip.setSelected(gui_tooltip);
				e.opts.gui_text_bg.setSelected(gui_text_bg);
				e.opts.gui_elem_colors.setSelected(gui_elem_colors);
				e.opts.gui_simspeed.setValue(gui_simspeed);
				e.opts.gui_simspeed_2.setValue(gui_simspeed_2);
				e.opts.gui_brightness.setValue(gui_brightness);
				e.opts.gui_brightness_vec.setValue(gui_brightness_vec);
				e.opts.textPane.setText(description);
				e.opts.gui_view.setSelectedItem(gui_view);
				e.opts.gui_view_vec.setSelectedItem(gui_view_vec);
				e.opts.gui_view_vec_mode.setSelectedItem(gui_view_vec_mode);
				e.opts.gui_bc.setSelectedItem(gui_bc);
				e.opts.gui_parameter1.setValue(gui_parameter1);
				e.opts.gui_3d_view.setSelectedItem(gui_3d_view);
				e.opts.gui_slice.setValue(gui_zslice);
				e.renderer.pitch = pitch;
				e.renderer.yaw = yaw;
				e.renderer.scale = zoom;
				e.opts.gui_parallax.setValue(gui_parallax);
				e.opts.gui_rotate.setSelected(gui_rotate);

				//e.setSize(resolution_tmp, width_tmp);
				//e.resetFields(true);

				e.Ex = copy(ex); 
				e.Ey = copy(ey); 
				e.Ez = copy(ez);
				e.Hx = copy(hx);
				e.Hy = copy(hy);
				e.Hz = copy(hz);
				e.Bx = copy(bx);
				e.By = copy(by);
				e.Bz = copy(bz);

				e.rho_abs = copy(rho_c);
				e.rho_n = copy(rho_n);
				e.rho_p = copy(rho_p);
				e.rho_back = copy(rho_back);
				e.rho_free = copy(rho_free);

				e.Jx_abs = copy(jx_c);
				e.Jy_abs = copy(jy_c);
				e.Jz_abs = copy(jz_c);
				e.Jx_n = copy(jx_n);
				e.Jy_n = copy(jy_n);
				e.Jz_n = copy(jz_n);
				e.Jx_p = copy(jx_p);
				e.Jy_p = copy(jy_p);
				e.Jz_p = copy(jz_p);

				e.materials = copy(materials);

				e.voltageprobes = new ArrayList<VoltageProbe>(voltageprobes);
				e.currentprobes = new ArrayList<CurrentProbe>(currentprobes);

				e.ground = ground;

				e.opts.textPane.setEditable(false);
				e.opts.textPane.setCaretPosition(0);
				e.updateAllMaterials();
				e.calcMiscFields(true);

				e.renderer.imgpanel.requestFocus();
			}
			finally {
				e.rwLock.writeLock().unlock();
			}
		});
	}
	
	public static double[][][] copy(double[][][] arr) {
	    if (arr == null) {
	        return null;
	    }

	    final double[][][] result = new double[arr.length][][];
	    for (int i = 0; i < arr.length; i++) {
	    	result[i] = new double[arr[i].length][];
	    	for (int j = 0; j < arr[i].length; j++) {
	    		result[i][j] = Arrays.copyOf(arr[i][j], arr[i][j].length);
		    }
	    }
	    return result;
	}
	
	public static Material[][][] copy(Material[][][] arr) {
	    if (arr == null) {
	        return null;
	    }

	    final Material[][][] result = new Material[arr.length][][];
	    for (int i = 0; i < arr.length; i++) {
	        result[i] = new Material[arr[i].length][];
		    for (int j = 0; j < arr[i].length; j++) {
		    	result[i][j] = new Material[arr[i][j].length];
		    	for (int k = 0; k < arr[i][j].length; k++) {
		    		result[i][j][k] = arr[i][j][k].clone();
		    	}
		    }
	    }
	    return result;
	}
}
