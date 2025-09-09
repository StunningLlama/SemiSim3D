package electrodynamics;

import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import electrodynamics.util.Utils;
import electrodynamics.util.Vector3;

public class Controls implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, ActionListener, AdjustmentListener {
	Electrodynamics e;

	Material[][][] selection;
	Material[][][] clipboard;

	/* Keyboard controls */

	boolean advanceframe = false;
	boolean clear = false;
	boolean reset = false;
	boolean save = false;
	boolean load = false;
	boolean debugging = false;
	boolean cut = false;
	boolean copy = false;
	boolean paste = false;
	boolean delete = false;
	boolean shift_down = false;
	boolean ctrl_down = false;
	boolean alt_down = false;


	/* Mouse controls */

	PointerInfo pointerinfo = MouseInfo.getPointerInfo();
	boolean mouse_pressed = false;
	boolean mouse_pressed_prev = false;
	boolean modifier_pressed = false;
	boolean moving_selection = false;
	boolean dragging_selection = false;
	boolean brush_changed = false;

	int mousebutton = 0;
	int mx = 0;
	int my = 0;
	int mz = 0;
	int mx_start = 0;
	int my_start = 0;
	int mz_start = 0;

	int mx_normal = 0;
	int my_normal = 0;
	int mz_normal = 0;

	int mx_index = 0;
	int my_index = 0;
	int mz_index = 0;
	int mx_start_index = 0;
	int my_start_index = 0;
	int mz_start_index = 0;


	double mx_realspace = 0;
	double my_realspace = 0;
	double mz_realspace = 0;

	double mxp_realspace = 0;
	double myp_realspace = 0;
	double mzp_realspace = 0;
	double mx_start_realspace = 0;
	double my_start_realspace = 0;
	double mz_start_realspace = 0;

	int delta_mx_index = 0;
	int delta_my_index = 0;
	int delta_mz_index = 0;

	boolean EMF_selected = false;
	double max_EMF = 5e5;

	Brush prev_brush;
	double brushsize = 0;
	int prev_EMF_setting = 0;
	BoundaryCondition prev_boundary = null;

	boolean[][][] under_brush;
	boolean[][][] selected;
	boolean[][][] selected_EMF;

	int text_x = 0;
	int text_y = 0;
	boolean texting = false;

	Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
	Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

	public Controls(Electrodynamics e) {
		this.e = e;
	}
	
	public void initializeGrid(double ds, int x_resolution, int y_resolution, int z_resolution) {

		selection = new Material[e.nx][e.ny][e.nz];
		clipboard = new Material[e.nx][e.ny][e.nz];
		
		under_brush = new boolean[e.nx][e.ny][e.nz];
		selected = new boolean[e.nx][e.ny][e.nz];
		selected_EMF = new boolean[e.nx][e.ny][e.nz];
		
		text_x = 0;
		text_y = 0;
		texting = false;
	}
	
	public void handleMouseInput() {
		Brush brush = (Brush) e.opts.gui_brush.getSelectedItem();
		BrushShape brushshape = (BrushShape) e.opts.gui_brush_1.getSelectedItem();

		boolean pressing = false;
		boolean releasing = false;

		if (mouse_pressed) {
			if (!mouse_pressed_prev) {
				pressing = true;
				e.renderer.canvas.requestFocus();
				if ((Brush.isMaterialModifyingBrush(brush) && !shift_down) || brush == Brush.SELECT)
					e.opts.gui_paused.setSelected(true);
			}
		} else {
			if (mouse_pressed_prev) {
				releasing = true;
			}
		}
		mouse_pressed_prev = mouse_pressed;


		if (!mouse_pressed && !releasing) {

			if (ctrl_down /*|| shift_down*/) {
				if (!modifier_pressed && Brush.isMaterialModifyingBrush(brush)) {
					modifier_pressed = true;
					prev_brush = brush;

					if (ctrl_down) {
						e.opts.gui_brush.setSelectedItem(Brush.FILL);
						brush = (Brush) e.opts.gui_brush.getSelectedItem();
					}
					/*else if (shift_down) {
						opts.gui_brush.setSelectedItem(Brush.LINE);
						brush = (Brush) opts.gui_brush.getSelectedItem();
					}*/
					//r.requestFocus();
				}
			} else {
				if (modifier_pressed) {
					modifier_pressed = false;
					e.opts.gui_brush.setSelectedItem(Brush.DRAW);
					brush = (Brush) e.opts.gui_brush.getSelectedItem();
					//r.requestFocus();
				}
			}
		}


		if (!e.renderer.threeD_mode) {
			mx_normal = 0;
			my_normal = 0;
			mz_normal = 0;
		}

		mx_realspace = Math.round((mx-1)/(double)e.renderer.scalefactor - 0.5 + mx_normal)*e.ds;
		my_realspace = Math.round((my-1)/(double)e.renderer.scalefactor - 0.5 + my_normal)*e.ds;
		mz_realspace = Math.round((mz-1)/(double)e.renderer.scalefactor - 0.5 + mz_normal)*e.ds;

		mx_start_realspace =  Math.round((mx_start-1)/(double)e.renderer.scalefactor - 0.5 + mx_normal)*e.ds;
		my_start_realspace = Math.round((my_start-1)/(double)e.renderer.scalefactor - 0.5 + my_normal)*e.ds;
		mz_start_realspace = Math.round((mz_start-1)/(double)e.renderer.scalefactor - 0.5 + mz_normal)*e.ds;

		mx_index = (int)Math.round((mx-1)/(double)e.renderer.scalefactor - 0.5);
		my_index = (int)Math.round((my-1)/(double)e.renderer.scalefactor - 0.5);
		mz_index = (int)Math.round((mz-1)/(double)e.renderer.scalefactor - 0.5);

		mx_start_index = (int)Math.round((mx_start-1)/(double)e.renderer.scalefactor - 0.5);
		my_start_index = (int)Math.round((my_start-1)/(double)e.renderer.scalefactor - 0.5);
		mz_start_index = (int)Math.round((mz_start-1)/(double)e.renderer.scalefactor - 0.5);

		if (mx_index < 0) mx_index = 0;
		if (my_index < 0) my_index = 0;
		if (mz_index < 0) mz_index = 0;
		if (mx_index >= e.nx) mx_index = e.nx-1;
		if (my_index >= e.ny) my_index = e.ny-1;
		if (mz_index >= e.nz) mz_index = e.nz-1;

		if (mx_start_index < 0) mx_start_index = 0;
		if (my_start_index < 0) my_start_index = 0;
		if (mz_start_index < 0) mz_start_index = 0;
		if (mx_start_index >= e.nx) mx_start_index = e.nx-1;
		if (my_start_index >= e.ny) my_start_index = e.ny-1;
		if (mz_start_index >= e.nz) mz_start_index = e.nz-1;

		e.opts.gui_stepsizelbl.setText("Step size: " + Utils.getSI(e.dt, "s"));
		e.opts.gui_stepslbl.setText("Steps/frame: " + e.opts.gui_simspeed_2.getValue());

		brushsize = (e.min_width/500)*(Math.pow(10.0, e.opts.gui_brushsize.getValue()/500.0) + e.opts.gui_brushsize.getValue()/100.0);
		e.opts.lblBrushSize.setText("Brush size: " + (int)Math.ceil(brushsize/e.ds));

		if (!Brush.isMaterialModifyingBrush(brush))
		{
			e.opts.gui_parameter2.setVisible(false);
			e.opts.gui_parameter2_text.setVisible(false);
			e.opts.gui_parameter2_text.setText("");
		}


		if (Brush.isMaterialModifyingBrush(brush) && brush != Brush.FILL) {
			e.opts.gui_brush_1.setVisible(true);
			e.opts.gui_brush_highlight.setVisible(true);
			e.opts.gui_brushsize.setVisible(true);
			e.opts.lblBrushSize.setVisible(true);
		} else {
			e.opts.gui_brush_1.setVisible(false);
			e.opts.gui_brush_highlight.setVisible(false);
			e.opts.gui_brushsize.setVisible(false);
			e.opts.lblBrushSize.setVisible(false);
		}


		if (Brush.isMaterialModifyingBrush(brush) && brush != Brush.ERASE) {
			e.opts.gui_material.setVisible(true);
		} else {
			e.opts.gui_material.setVisible(false);
		}

		if (brush_changed) {
			if (!(brush == Brush.SELECT || brush == Brush.FLOODSELECT)) {
				for (int i = 0; i < e.nx; i++)
				{
					for (int j = 0; j < e.ny; j++)
					{
						for (int k = 0; k < e.nz; k++)
						{
							selected[i][j][k] = false;
						}
					}
				}
			}

			if (brush != Brush.INTERACT) {
				for (int i = 0; i < e.nx; i++)
				{
					for (int j = 0; j < e.ny; j++)
					{
						for (int k = 0; k < e.nz; k++)
						{
							selected_EMF[i][j][k] = false;
						}
					}
				}
				EMF_selected = false;
			}

			brush_changed = false;
		}

		boolean update = false;

		if (cut || copy) {
			int i_min = e.nx-1;
			int j_min = e.ny-1;
			int k_min = e.nz-1;
			for (int i = 0; i < e.nx; i++)
			{
				for (int j = 0; j < e.ny; j++)
				{
					for (int k = 0; k < e.nz; k++)
					{
						clipboard[i][j][k].erase();
						if (selected[i][j][k] && e.materials[i][j][k].type != MaterialType.VACUUM) {
							if (i < i_min) i_min = i;
							if (j < j_min) j_min = j;
							if (k < k_min) k_min = k;
						}
					}
				}
			}
			for (int i = 0; i < e.nx; i++)
			{
				for (int j = 0; j < e.ny; j++)
				{
					for (int k = 0; k < e.nz; k++)
					{
						if (selected[i][j][k] && e.materials[i][j][k].type != MaterialType.VACUUM) {
							clipboard[i-i_min][j-j_min][k-k_min] = e.materials[i][j][k].clone();
							if (cut) {
								e.materials[i][j][k].erase();
							}
						}
						if (cut) {
							selected[i][j][k] = false;
						}
					}
				}
			}

			if (cut) update = true;

			cut = false;
			copy = false;
		}

		if (paste) {
			for (int i = 0; i < e.nx; i++)
			{
				for (int j = 0; j < e.ny; j++)
				{
					for (int k = 0; k < e.nz; k++)
					{
						selected[i][j][k] = false;
					}
				}
			}

			for (int i = 0; i < e.nx; i++)
			{
				for (int j = 0; j < e.ny; j++)
				{
					for (int k = 0; k < e.nz; k++)
					{
						selection[i][j][k] = clipboard[i][j][k].clone();
					}
				}
			}
			moving_selection = true;
			dragging_selection = false;
			paste = false;
			update = true;
		}

		if (delete) {
			for (int i = 0; i < e.nx; i++)
			{
				for (int j = 0; j < e.ny; j++)
				{
					for (int k = 0; k < e.nz; k++)
					{
						if (selected[i][j][k] && e.materials[i][j][k].type != MaterialType.VACUUM) {
							e.materials[i][j][k].erase();
						}
					}
				}
			}
			delete = false;
			update = true;
		}

		switch(brush) {
		case DRAW:
		case LINE:
		case REPLACE:
		case ERASE:
		case FILL:

			if (pressing && shift_down) {
				e.renderer.pitch_start =  e.renderer.pitch;
				e.renderer.yaw_start = e.renderer.yaw;

				break;
			}  else if (mouse_pressed && shift_down) {
				e.renderer.pitch = e.renderer.pitch_start + 2*(float)(my_3d - my_3d_start)/e.renderer.imgheight;
				e.renderer.yaw = e.renderer.yaw_start - 2*(float)(mx_3d - mx_3d_start)/e.renderer.imgwidth;

				if (e.renderer.pitch > Math.PI/2) e.renderer.pitch = (float)Math.PI/2;
				if (e.renderer.pitch < -Math.PI/2) e.renderer.pitch = -(float)Math.PI/2;

				break;
			}

			if ((mousebutton == MouseEvent.BUTTON2 || alt_down) && pressing) {
				e.opts.gui_material.setSelectedItem(e.materials[mx_index][my_index][mz_index].type);
			}

			double angle = 0;
			MaterialType mat = (MaterialType) e.opts.gui_material.getSelectedItem();

			if (mat == MaterialType.EMF) {
				e.opts.gui_parameter2.setVisible(true);
				e.opts.gui_parameter2_text.setVisible(true);

				int directionval = e.opts.gui_parameter2.getValue()/6;
				if (directionval == 0) {
					e.opts.gui_parameter2_text.setText("EMF direction: Up");
					angle = -Math.PI/2;
				}
				if (directionval == 1) {
					e.opts.gui_parameter2_text.setText("EMF direction: Right");
					angle = 0;
				}
				if (directionval == 2) {
					e.opts.gui_parameter2_text.setText("EMF direction: Down");
					angle = Math.PI/2;
				}
				if (directionval == 3) {
					e.opts.gui_parameter2_text.setText("EMF direction: Left");
					angle = Math.PI;
				}
				//opts.gui_parameter2_text.setText("Brush orientation: " + directionval*(360/24) + " deg");
				//angle = Math.PI * directionval/12.0;
			} else {
				e.opts.gui_parameter2.setVisible(false);
				e.opts.gui_parameter2_text.setVisible(false);
				e.opts.gui_parameter2_text.setText("");
			}


			if (mousebutton == MouseEvent.BUTTON3 || brush == Brush.ERASE)
				mat = MaterialType.VACUUM;

			if (!(mousebutton == MouseEvent.BUTTON2 || alt_down)) {
				if (brush == Brush.LINE) {
					if (releasing) {
						drawMaterialLine(mx_start_realspace, my_start_realspace, mz_start_realspace, mx_realspace, my_realspace, mz_realspace, brush, brushshape, mat, brushsize, angle);
					}
				} else if (brush == Brush.FILL) {
					if (pressing) {
						floodFillSet(mx_index, my_index, mz_index, e.materials[mx_index][my_index][mz_index].type, mat, angle);
					}
				} else if (!e.renderer.threeD_mode && mouse_pressed || e.renderer.threeD_mode && pressing) {
					drawMaterialLine(mxp_realspace, myp_realspace, mzp_realspace, mx_realspace, my_realspace, mz_realspace, brush, brushshape, mat, brushsize, angle);
				}
			}

			if (Brush.isBrushShapeImportant(brush)) {
				for (int i = 0; i < e.nx; i++)
				{
					for (int j = 0; j < e.ny; j++)
					{
						for (int k = 0; k < e.nz; k++)
						{
							if (e.opts.gui_brush_highlight.isSelected()) {
								double cx = 0;
								double cy = 0;
								double cz = 0;
								cx = i*e.ds;
								cy = j*e.ds;
								cz = k*e.ds;

								double px = (cx-mx_realspace);
								double py = (cy-my_realspace);
								double pz = (cz-mz_realspace);
								double r = 0;

								if (brushshape == BrushShape.CIRCLE)
									r = Math.sqrt(px*px+py*py+pz*pz);
								else if (brushshape == BrushShape.SQUARE)
									r = Math.max(Math.max(Math.abs(px), Math.abs(py)), Math.abs(pz));
								under_brush[i][j][k] = (r <= brushsize);
							}
							else {
								under_brush[i][j][k] = false;
							}
						}
					}
				}
			}

			break;

		case INTERACT:
			if (e.materials[mx_index][my_index][mz_index].type == MaterialType.EMF || e.materials[mx_index][my_index][mz_index].type == MaterialType.SWITCH)
				updateCursor(HAND_CURSOR);
			else
				updateCursor(DEFAULT_CURSOR);

			if (pressing) {
				boolean turn_on_EMF = !selected_EMF[mx_index][my_index][mz_index];

				for (int i = 0; i < e.nx; i++)
				{
					for (int j = 0; j < e.ny; j++)
					{
						for (int k = 0; k < e.nz; k++)
						{
							selected_EMF[i][j][k] = false;
						}
					}
				}
				EMF_selected = false;

				if (e.materials[mx_index][my_index][mz_index].type == MaterialType.EMF && turn_on_EMF) {
					floodFillSelectEMF(mx_index, my_index, mz_index, true);
					EMF_selected = true;
					int setting = (int)(Math.round(50*e.materials[mx_index][my_index][mz_index].emf/max_EMF));
					e.opts.gui_parameter3.setValue(setting);
					prev_EMF_setting = setting;
				}

				if (e.materials[mx_index][my_index][mz_index].type == MaterialType.SWITCH) {
					floodFillToggleSwitch(mx_index, my_index, mz_index, 1-e.materials[mx_index][my_index][mz_index].activated);
					update = true;
				}

				e.renderer.pitch_start =  e.renderer.pitch;
				e.renderer.yaw_start = e.renderer.yaw;
			}  else if (mouse_pressed) {
				e.renderer.pitch = e.renderer.pitch_start + 2*(float)(my_3d - my_3d_start)/e.renderer.imgheight;
				e.renderer.yaw = e.renderer.yaw_start - 2*(float)(mx_3d - mx_3d_start)/e.renderer.imgwidth;

				if (e.renderer.pitch > Math.PI/2) e.renderer.pitch = (float)Math.PI/2;
				if (e.renderer.pitch < -Math.PI/2) e.renderer.pitch = -(float)Math.PI/2;
			}
			break;
		case FLOODSELECT:
		case SELECT:
			if (pressing) {
				if (brush == Brush.FLOODSELECT && !moving_selection) {
					floodFillSelect(mx_index, my_index, mz_index, e.materials[mx_index][my_index][mz_index].type, !selected[mx_index][my_index][mz_index]);
				}
				else if (moving_selection && !dragging_selection) {
					for (int i = 0; i < e.nx; i++)
					{
						for (int j = 0; j < e.ny; j++)
						{
							for (int k = 0; k < e.nz; k++)
							{
								int si = i-delta_mx_index;
								int sj = j-delta_my_index;
								int sk = k-delta_mz_index;
								if (si >= 0 && sj >= 0 && sk >= 0 && si < e.nx && sj < e.ny && sk < e.nz && selection[si][sj][sk].type != MaterialType.VACUUM) {
									e.materials[i][j][k].erase();
									e.materials[i][j][k] = selection[si][sj][sk].clone();
									selected[i][j][k] = true;
								}
							}
						}
					}
					moving_selection = false;
					dragging_selection = false;
				} else if (!moving_selection && selected[mx_index][my_index][mz_index]) {
					for (int i = 0; i < e.nx; i++)
					{
						for (int j = 0; j < e.ny; j++)
						{
							for (int k = 0; k < e.nz; k++)
							{
								selection[i][j][k].erase();
								if (selected[i][j][k]) {
									selection[i][j][k] = e.materials[i][j][k].clone();
									selected[i][j][k] = false;
									e.materials[i][j][k].erase();
								}
							}
						}
					}
					moving_selection = true;
					dragging_selection = true;
					delta_mx_index = 0;
					delta_my_index = 0;
					delta_mz_index = 0;
				}
			} else if (mouse_pressed) {
				if (brush != Brush.FLOODSELECT) {
					if (dragging_selection) {
						delta_mx_index = mx_index - mx_start_index;
						delta_my_index = my_index - my_start_index;
						delta_mz_index = mz_index - mz_start_index;
					} else {
						int mx0 = Math.min(mx_start_index, mx_index);
						int my0 = Math.min(my_start_index, my_index);
						int mz0 = Math.min(mz_start_index, mz_index);
						int mx1 = Math.max(mx_start_index, mx_index);
						int my1 = Math.max(my_start_index, my_index);
						int mz1 = Math.max(mz_start_index, mz_index);

						for (int i = 0; i < e.nx; i++)
						{
							for (int j = 0; j < e.ny; j++)
							{
								for (int k = 0; k < e.nz; k++)
								{
									if (i >= mx0 && i <= mx1 && j >= my0 && j <= my1 && k >= mz0 && k <= mz1)
										selected[i][j][k] = true;
									else
										selected[i][j][k] = false;
								}
							}
						}
					}
				}
			} else if (releasing) {
				if (brush != Brush.FLOODSELECT) {
					delta_mx_index = mx_index - mx_start_index;
					delta_my_index = my_index - my_start_index;
					delta_mz_index = mz_index - mz_start_index;
					if (dragging_selection) {
						for (int i = 0; i < e.nx; i++)
						{
							for (int j = 0; j < e.ny; j++)
							{
								for (int k = 0; k < e.nz; k++)
								{
									int si = i-delta_mx_index;
									int sj = j-delta_my_index;
									int sk = k-delta_mz_index;
									if (si >= 0 && sj >= 0 && sk >= 0 && si < e.nx && sj < e.ny && sk < e.nz && selection[si][sj][sk].type != MaterialType.VACUUM) {
										e.materials[i][j][k].erase();
										e.materials[i][j][k] = selection[si][sj][sk].clone();
										selected[i][j][k] = true;
									}
								}
							}
						}
						moving_selection = false;
						dragging_selection = false;
					} else {
						if (delta_mx_index == 0 && delta_my_index == 0 && delta_mz_index == 0) {
							for (int i = 0; i < e.nx; i++)
							{
								for (int j = 0; j < e.ny; j++)
								{
									for (int k = 0; k < e.nz; k++)
									{
										selected[i][j][k] = false;
									}
								}
							}
						}
					}
				}
			} else {
				delta_mx_index = mx_index;
				delta_my_index = my_index;
				delta_mz_index = mz_index;
			}
			break;
		case CURRENT:
			if (!RenderMode.is3d((RenderMode) e.opts.gui_3d_view.getSelectedItem())) {
				if (pressing) {
					CurrentProbe p = new CurrentProbe();
					p.x1 = mx_start_index;
					p.y1 = my_start_index;
					p.z1 = mz_start_index;
					p.x2 = mx_index;
					p.y2 = my_index;
					p.z2 = mz_index;
					p.normal_x = e.renderer.slice_x;
					p.normal_y = e.renderer.slice_y;
					p.normal_z = e.renderer.slice_z;
					e.currentprobes.add(p);
				} else if (mouse_pressed) {
					e.currentprobes.get(e.currentprobes.size()-1).x2 = mx_index;
					e.currentprobes.get(e.currentprobes.size()-1).y2 = my_index;
					e.currentprobes.get(e.currentprobes.size()-1).z2 = mz_index;
				}
			}
			break;
		case VOLTAGE:
			if (pressing) {
				VoltageProbe p = new VoltageProbe();
				p.x = mx_start_index;
				p.y = my_start_index;
				p.z = mz_start_index;
				e.voltageprobes.add(p);
			} else if (mouse_pressed) {
				e.voltageprobes.get(e.voltageprobes.size()-1).x = mx_index;
				e.voltageprobes.get(e.voltageprobes.size()-1).y = my_index;
				e.voltageprobes.get(e.voltageprobes.size()-1).z = mz_index;
			}
			break;
		case DELETEPROBE:
			updateCursor(DEFAULT_CURSOR);
			int i = 0;
			while(i < e.voltageprobes.size()) {
				VoltageProbe p = e.voltageprobes.get(i);
				if (Utils.length(p.x-mx_index, p.y-my_index, p.z-mz_index) < 3) {
					updateCursor(HAND_CURSOR);
					if (pressing) {
						e.voltageprobes.remove(i);
						i--;
					}
				}
				i++;
			}

			i = 0;
			while(i < e.currentprobes.size()) {
				CurrentProbe p = e.currentprobes.get(i);
				if (Utils.length(p.x1-mx_index, p.y1-my_index, p.z1-mz_index) < 3 || Utils.length(p.x2-mx_index, p.y2-my_index, p.z2-mz_index) < 3) {
					updateCursor(HAND_CURSOR);
					if (pressing) {
						e.currentprobes.remove(i);
						i--;
					}
				}
				i++;
			}

			if (e.ground != null && Utils.length(e.ground.x-mx_index, e.ground.y-my_index, e.ground.z-mz_index) < 3) {
				updateCursor(HAND_CURSOR);
				if (pressing) {
					e.ground = null;
				}
			}

			break;
		case GROUND:
			if (pressing) {
				if (e.ground == null)
					e.ground = new VoltageProbe();
				e.ground.x = mx_start_index;
				e.ground.y = my_start_index;
				e.ground.z = mz_start_index;
			} else if (mouse_pressed) {
				e.ground.x = mx_index;
				e.ground.y = my_index;
				e.ground.z = mz_index;
			}
			break;
		}

		setEMFs();

		if (releasing || (BoundaryCondition)e.opts.gui_bc.getSelectedItem() != prev_boundary || update) {
			e.updateAllMaterials();
			e.multigridSolve(true, false);
		}

		prev_boundary = (BoundaryCondition)e.opts.gui_bc.getSelectedItem();

		mxp_realspace = mx_realspace;
		myp_realspace = my_realspace;
		mzp_realspace = mz_realspace;
	}

	public void drawMaterialLine(double x1, double y1, double z1, double x2, double y2, double z2, Brush brush, BrushShape brushshape, MaterialType mat, double brushsize, double EMF_angle) {
		Vector3 a = new Vector3(0, 0, 0);
		Vector3 b = new Vector3(0, 0, 0);
		Vector3 p = new Vector3(0, 0, 0);
		Vector3 ab = new Vector3(0, 0, 0);
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					double cx = 0;
					double cy = 0;
					double cz = 0;
					cx = i*e.ds;
					cy = j*e.ds;
					cz = k*e.ds;

					a.initialize(x1, y1, z1);
					b.initialize(x2, y2, z2);
					p.initialize(cx, cy, cz);
					p.addmult(a, -1);
					ab.copy(b);
					ab.addmult(a, -1);

					double l2 = ab.dot(ab);
					if (l2 == 0)
						l2 = 1;
					double t = Utils.clamp(p.dot(ab)/l2, 0, 1);
					ab.scalarmult(t);
					p.addmult(ab, -1);
					double r = 0;
					if (brushshape == BrushShape.CIRCLE)
						r = Math.sqrt(p.dot(p));
					else if (brushshape == BrushShape.SQUARE)
						r = Math.max(Math.max(Math.abs(p.x), Math.abs(p.y)), Math.abs(p.z));
					if (r <= brushsize) {
						if (mat == MaterialType.VACUUM) {
							e.materials[i][j][k].erase();
						} else if (e.materials[i][j][k].type == MaterialType.VACUUM || brush == Brush.REPLACE) {
							e.materials[i][j][k].erase();
							e.initializeMaterial(i, j, k, mat);
							if (mat == MaterialType.EMF) e.materials[i][j][k].emf_direction = EMF_angle;
						}
					}
				}
			}
		}
	}

	public void updateCursor(Cursor c) {
		e.renderer.canvas.setCursor(c);
		e.renderer.renderer_left_eye.canvas.setCursor(c);
		e.renderer.renderer_right_eye.canvas.setCursor(c);
	}

	public void setEMFs() {
		int EMF_setting = e.opts.gui_parameter3.getValue();
		double new_EMF = max_EMF*EMF_setting/50.0;

		if (e.opts.gui_brush.getSelectedItem() == Brush.INTERACT && EMF_selected) {
			e.opts.gui_parameter3.setVisible(true);
			e.opts.gui_parameter3_text.setVisible(true);
			e.opts.gui_parameter3_text.setText("EMF: " + Utils.getSI(new_EMF, "V/m"));
		} else {
			e.opts.gui_parameter3.setVisible(false);
			e.opts.gui_parameter3_text.setVisible(false);
			e.opts.gui_parameter3_text.setText("");
		}

		if (EMF_setting != prev_EMF_setting && EMF_selected) {
			for (int i = 0; i < e.nx; i++)
			{
				for (int j = 0; j < e.ny; j++)
				{
					for (int k = 0; k < e.nz; k++)
					{
						if (e.materials[i][j][k].type == MaterialType.EMF && selected_EMF[i][j][k]) {
							e.materials[i][j][k].emf = new_EMF;
						}
					}
				}
			}

			e.updateJustEMFs();
		}

		prev_EMF_setting = EMF_setting;
	}
	

	class FloodFillCoordinate {
		int i;
		int j;
		int k;

		public FloodFillCoordinate(int i, int j, int k) {
			this.i = i;
			this.j = j;
			this.k = k;
		}
	}

	public void floodFillSet(int i, int j, int k, MaterialType old_mat, MaterialType new_mat, double EMF_angle) {
		if (old_mat == new_mat)
			return;

		Queue<FloodFillCoordinate> queue = new LinkedList<>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < e.nx && coord.j >= 0 && coord.j < e.ny && coord.k >= 0 && coord.k < e.nz && e.materials[coord.i][coord.j][coord.k].type == old_mat && e.materials[coord.i][coord.j][coord.k].type != new_mat) {
				e.materials[coord.i][coord.j][coord.k].erase();
				e.initializeMaterial(coord.i, coord.j, coord.k, new_mat);
				if (new_mat == MaterialType.EMF) e.materials[coord.i][coord.j][coord.k].emf_direction = EMF_angle;
				queue.add(new FloodFillCoordinate(coord.i-1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i+1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j-1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j+1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k-1));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k+1));
			}
		}
	}

	public void floodFillSelect(int i, int j, int k, MaterialType mat, boolean select) {
		Queue<FloodFillCoordinate> queue = new LinkedList<>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < e.nx && coord.j >= 0 && coord.j < e.ny && coord.k >= 0 && coord.k < e.nz && e.materials[coord.i][coord.j][coord.k].type == mat && selected[coord.i][coord.j][coord.k] != select) {
				selected[coord.i][coord.j][coord.k] = select;
				queue.add(new FloodFillCoordinate(coord.i-1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i+1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j-1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j+1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k-1));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k+1));
			}
		}
	}

	public void floodFillSelectEMF(int i, int j, int k, boolean select) {
		Queue<FloodFillCoordinate> queue = new LinkedList<>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < e.nx && coord.j >= 0 && coord.j < e.ny && coord.k >= 0 && coord.k < e.nz && e.materials[coord.i][coord.j][coord.k].type == MaterialType.EMF && selected_EMF[coord.i][coord.j][coord.k] != select) {
				selected_EMF[coord.i][coord.j][coord.k] = select;
				queue.add(new FloodFillCoordinate(coord.i-1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i+1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j-1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j+1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k-1));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k+1));
			}
		}
	}

	public void floodFillToggleSwitch(int i, int j, int k, int active) {
		Queue<FloodFillCoordinate> queue = new LinkedList<>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < e.nx && coord.j >= 0 && coord.j < e.ny && coord.k >= 0 && coord.k < e.nz && e.materials[coord.i][coord.j][coord.k].type == MaterialType.SWITCH && e.materials[coord.i][coord.j][coord.k].activated != active) {
				e.materials[coord.i][coord.j][coord.k].activated = active;
				queue.add(new FloodFillCoordinate(coord.i-1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i+1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j-1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j+1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k-1));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k+1));
			}
		}
	}

	public void addKeyBinds(JPanel contentPane) {
		InputMap map = contentPane.getInputMap(JComponent.WHEN_FOCUSED);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), key_pause);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), key_pause);
		contentPane.getActionMap().put(key_pause, key_pause);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), key_frame);
		contentPane.getActionMap().put(key_frame, key_frame);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), key_dbg);
		contentPane.getActionMap().put(key_dbg, key_dbg);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), key_changebrush);
		contentPane.getActionMap().put(key_changebrush, key_changebrush);


		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), key_cut);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK), key_cut);
		contentPane.getActionMap().put(key_cut, key_cut);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), key_copy);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), key_copy);
		contentPane.getActionMap().put(key_copy, key_copy);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), key_paste);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), key_paste);
		contentPane.getActionMap().put(key_paste, key_paste);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), key_delete);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), key_delete);
		contentPane.getActionMap().put(key_delete, key_delete);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), key_color);
		contentPane.getActionMap().put(key_color, key_color);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), key_scalar_view);
		contentPane.getActionMap().put(key_scalar_view, key_scalar_view);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0), key_vector_view);
		contentPane.getActionMap().put(key_vector_view, key_vector_view);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), key_tooltip);
		contentPane.getActionMap().put(key_tooltip, key_tooltip);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0), key_textbg);
		contentPane.getActionMap().put(key_textbg, key_textbg);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), key_3d);
		contentPane.getActionMap().put(key_3d, key_3d);


		map = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK), key_shift);
		contentPane.getActionMap().put(key_shift, key_shift);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true), key_shift_up);
		contentPane.getActionMap().put(key_shift_up, key_shift_up);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, InputEvent.CTRL_DOWN_MASK), key_ctrl);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_META, InputEvent.META_DOWN_MASK), key_ctrl);
		contentPane.getActionMap().put(key_ctrl, key_ctrl);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 0, true), key_ctrl_up);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_META, 0, true), key_ctrl_up);
		contentPane.getActionMap().put(key_ctrl_up, key_ctrl_up);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK), key_alt);
		contentPane.getActionMap().put(key_alt, key_alt);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0, true), key_alt_up);
		contentPane.getActionMap().put(key_alt_up, key_alt_up);


	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent ev) {
		if (!shift_down)
			e.opts.gui_brushsize.setValue(e.opts.gui_brushsize.getValue() - (int)(10*ev.getPreciseWheelRotation()));
		else
			e.renderer.scale *= Math.exp(-(int)(10*ev.getPreciseWheelRotation())/100.0);
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource() instanceof javax.swing.Timer)
			e.run();
		else if (ev.getSource() == e.opts.gui_reset)
			clear = true;
		else if (ev.getSource() == e.opts.gui_resetall)
			reset = true;
		else if (ev.getSource() == e.opts.gui_save)
			save = true;
		else if (ev.getSource() == e.opts.gui_open)
			load = true;
		else if (ev.getSource() == e.opts.gui_help)
			try {
				File helpfile = new File("README.html");
				java.awt.Desktop.getDesktop().browse(helpfile.toURI());
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		else if (ev.getSource() == e.opts.gui_editdesc) {
			e.opts.textPane.setEditable(!e.opts.textPane.isEditable());
		} else if (ev.getSource() == e.opts.gui_view) {
			//updateMiscFields = true;
		} else if (ev.getSource() == e.opts.gui_view_vec) {
			//updateMiscFields = true;
		} else if (ev.getSource() == e.opts.gui_brush) {
			brush_changed = true;
		} else if (ev.getSource() == e.opts.gui_3d_view) {
			e.renderer.set3Dmode();
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent e) {
		mouse_pressed = true;
		mousebutton = e.getButton();

		setMousePos(e.getX(), e.getY());

		mx_start = mx;
		my_start = my;
		mz_start = mz;

		mx_3d = e.getX();
		my_3d = e.getY();

		mx_3d_start = mx_3d;
		my_3d_start = my_3d;
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		mouse_pressed = false;
		update3dCursor = true;
	}

	int mx_3d;
	int my_3d;

	int mx_3d_start;
	int my_3d_start;

	@Override
	public void mouseDragged(MouseEvent e) {
		setMousePos(e.getX(), e.getY());

		mx_3d = e.getX();
		my_3d = e.getY();

		update3dCursor = true;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		setMousePos(e.getX(), e.getY());

		mx_3d = e.getX();
		my_3d = e.getY();

		update3dCursor = true;
	}

	public void setMousePos(int eventX, int eventY) {
		if (!e.renderer.threeD_mode) {
			if (e.renderer.slice_x) {
				my = eventX;
				mz = e.renderer.imgheight - 1 - eventY;
				mx = indexToCoord(e.opts.gui_slice.getValue());
			} else if (e.renderer.slice_y) {
				mx = eventX;
				mz = e.renderer.imgheight - 1 - eventY;
				my = indexToCoord(e.opts.gui_slice.getValue());
			} else if (e.renderer.slice_z) {
				mx = eventX;
				my = e.renderer.imgheight - 1 - eventY;
				mz = indexToCoord(e.opts.gui_slice.getValue());
			}
		}
	}

	boolean update3dCursor = false;

	public int indexToCoord(int n) {
		return (int)(e.renderer.scalefactor*(n + 0.5) + 1);
	}

	@SuppressWarnings("serial")
	private Action key_pause = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			e.opts.gui_paused.setSelected(!e.opts.gui_paused.isSelected());
		}
	};

	@SuppressWarnings("serial")
	private Action key_frame = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			advanceframe = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_dbg = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			debugging = !debugging;
		}
	};

	@SuppressWarnings("serial")
	private Action key_changebrush = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			e.opts.gui_brush_1.setSelectedIndex((e.opts.gui_brush_1.getSelectedIndex()+1)%2);
		}
	};

	@SuppressWarnings("serial")
	private Action key_shift = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			shift_down = true;
		}
	};
	@SuppressWarnings("serial")
	private Action key_shift_up = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			shift_down = false;
		}
	};
	@SuppressWarnings("serial")
	private Action key_ctrl = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			ctrl_down = true;
		}
	};
	@SuppressWarnings("serial")
	private Action key_ctrl_up = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			ctrl_down = false;
		}
	};

	@SuppressWarnings("serial")
	private Action key_cut = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			cut = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_copy = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			copy = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_paste = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			paste = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_delete = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			delete = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_color = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			e.opts.gui_elem_colors.setSelected(!e.opts.gui_elem_colors.isSelected());
		}
	};

	ScalarView prev_scalar_view = ScalarView.NONE;
	VectorView prev_vector_view = VectorView.NONE;

	@SuppressWarnings("serial")
	private Action key_scalar_view = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			if (e.opts.gui_view.getSelectedItem() == ScalarView.NONE)
				e.opts.gui_view.setSelectedItem(prev_scalar_view);
			else
			{
				prev_scalar_view = (ScalarView) e.opts.gui_view.getSelectedItem();
				e.opts.gui_view.setSelectedItem(ScalarView.NONE);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action key_vector_view = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			if (e.opts.gui_view_vec.getSelectedItem() == VectorView.NONE)
				e.opts.gui_view_vec.setSelectedItem(prev_vector_view);
			else
			{
				prev_vector_view = (VectorView) e.opts.gui_view_vec.getSelectedItem();
				e.opts.gui_view_vec.setSelectedItem(VectorView.NONE);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action key_tooltip = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			e.opts.gui_tooltip.setSelected(!e.opts.gui_tooltip.isSelected());
		}
	};

	@SuppressWarnings("serial")
	private Action key_textbg = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			e.opts.gui_text_bg.setSelected(!e.opts.gui_text_bg.isSelected());
		}
	};

	@SuppressWarnings("serial")
	private Action key_alt = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			alt_down = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_alt_up = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			alt_down = false;
		}
	};

	@SuppressWarnings("serial")
	private Action key_3d = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (texting) return;
			//set3Dmode(threeD_mode);
		}
	};
	
	@Override
	public void keyTyped(KeyEvent e) {
		if (texting) {
			/*if (Font7x5.getCharacter(e.getKeyChar()) != null) {
				for (int i = 0; i < 5; i++) {
					for (int j = 0; j < 7; j++) {
						if (text_x+i+1 >= 0 && text_x+i+1 < nx && text_y+j >= 0 && text_y+j < ny
								&& Font7x5.getPixel(e.getKeyChar(), 4-i, j) == 1 && materials[text_x+i+1][text_y+j].type == MaterialType.VACUUM) {
							initializeMaterial(text_x+i+1, text_y+j, MaterialType.DECO);
						}
					}
				}
				text_x += 6;
			}
			updateAllMaterials();*/
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (texting) {
			/*if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
				text_x -= 6;
				for (int i = 0; i < 5; i++) {
					for (int j = 0; j < 7; j++) {
						if (text_x+i+1 >= 0 && text_x+i+1 < nx && text_y+j >= 0 && text_y+j < ny
								&& materials[text_x+i+1][text_y+j].type == MaterialType.DECO) {
							materials[text_x+i+1][text_y+j].erase();
						}
					}
				}
			}
			updateAllMaterials();*/
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent ev) {
		if (ev.getSource() == e.opts.gui_slice) {
			if (e.renderer.slice_x) {
				e.opts.gui_slicelabel.setText("Slice: x = " + Utils.getSI(e.opts.gui_slice.getValue()*e.ds, "m"));
			} else if (e.renderer.slice_y) {
				e.opts.gui_slicelabel.setText("Slice: y = " + Utils.getSI(e.opts.gui_slice.getValue()*e.ds, "m"));
			} else if (e.renderer.slice_z) {
				e.opts.gui_slicelabel.setText("Slice: z = " + Utils.getSI(e.opts.gui_slice.getValue()*e.ds, "m"));
			}
		}
		if (ev.getSource() == e.opts.gui_parallax) {
			e.renderer.updateParallax();
		}
	}
}

enum Brush {
	INTERACT("Interact"),
	DRAW("Draw"),
	VOLTAGE("Add voltage probe"),
	CURRENT("Add current probe"),
	GROUND("Add ground"),
	DELETEPROBE("Delete probe"),
	REPLACE("Replace"),
	LINE("Line"),
	FILL("Fill"),
	ERASE("Eraser"),
	SELECT("Select & Move"),
	FLOODSELECT("Select region");

	String name;
	Brush(String name)
	{
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public static boolean isMaterialModifyingBrush(Brush brush) {
		return (brush == Brush.DRAW
		|| brush == Brush.LINE
		|| brush == Brush.REPLACE
		|| brush == Brush.ERASE
		|| brush == Brush.FILL);
	}

	public static boolean isBrushShapeImportant(Brush brush) {
		return (brush == Brush.DRAW
		|| brush == Brush.LINE
		|| brush == Brush.REPLACE
		|| brush == Brush.ERASE);
	}
}

enum BrushShape {
	CIRCLE("Circle brush"),
	SQUARE("Square brush");

	String name;
	BrushShape(String name)
	{
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
