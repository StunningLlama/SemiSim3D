// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Electromagnetic Simulation which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;

import com.jogamp.opengl.util.awt.TextRenderer;

import electrodynamics.util.PeriodicTask;
import electrodynamics.util.Timer;
import electrodynamics.util.Utils;
import electrodynamics.util.Vector3;

public class Renderer extends PeriodicTask {
	Electrodynamics e;
	

	JPanel imgpanel;
	RenderCanvas canvas;
	/* 3D graphics */
	Renderer3D renderer_left_eye;
	Renderer3D renderer_right_eye;

	
	/* Graphics */

	float[][][] image_r;
	float[][][] image_g;
	float[][][] image_b;
	boolean[][][] solid;
	float col_r = 0;
	float col_g = 0;
	float col_b = 0;
	double alphaBG = 0;
	double alphaFG = 0;
	boolean slice_x = false;
	boolean slice_y = false;
	boolean slice_z = true;
	Random rand = new Random();

	ArrayList<Text> texts = new ArrayList<>();
	Font bigfont = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
	Font regularfont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	int scalefactor;
	int imgwidth = 0;
	int imgheight = 0;
	int targetframerate = 60;
	int frameduration = 1000/targetframerate;
	
	
	
	

	float pitch = (float)Math.PI/6;
	float yaw = (float)Math.PI/4;
	float pitch_start = 0;
	float yaw_start = 0;
    float scale = 24f;
	boolean threeD_mode = true;
	

	Timer FPStimer = new Timer("Graphics FPS", 10, true);
	Timer t5 = new Timer("Graphics", 20, true);
	

	public Renderer(Electrodynamics e) {
		this.e = e;

		canvas = new RenderCanvas(e);
		canvas.setFocusable(true);
	}
	
	@Override
	public void run() {
		if (!threeD_mode) {
			e.rwLock.readLock().lock();
			try {
				t5.start();
				render();
				canvas.repaint();
				t5.stop();

				FPStimer.stop();
				FPStimer.start();
			} catch (Exception e1) {
				SemiSim3D.displayErrorMessage(e1);
			}
			finally {
				e.rwLock.readLock().unlock();
			}
		}
		
        SemiSim3D.instance.threadPool.schedule(this, nextDelay(frameduration), TimeUnit.MILLISECONDS);
	}
	
	public void create3dCanvas() {
		renderer_left_eye = new Renderer3D(e);
		renderer_left_eye.isMainCanvas = true;
		renderer_right_eye = new Renderer3D(e);
	}
	
	public void initializeGrid(double ds, int x_resolution, int y_resolution, int z_resolution) {
		image_r = new float[e.nx][e.ny][e.nz];
		image_g = new float[e.nx][e.ny][e.nz];
		image_b = new float[e.nx][e.ny][e.nz];
		solid = new boolean[e.nx][e.ny][e.nz];
		scalefactor = (int)(768.0/Math.max(Math.max(e.nx, e.ny), e.nz));
		imgwidth = 768;
		imgheight = 768;
	}
	
	public void set3Dmode() {
		e.rwLock.writeLock().lock();
		try {
			RenderMode mode = (RenderMode) e.opts.gui_3d_view.getSelectedItem();
			threeD_mode = RenderMode.is3d(mode);

			e.opts.gui_slice.setVisible(!threeD_mode);
			e.opts.gui_slicelabel.setVisible(!threeD_mode);
			slice_x = (mode == RenderMode.SLICE_X);
			slice_y = (mode == RenderMode.SLICE_Y);
			slice_z = (mode == RenderMode.SLICE_Z);
			generateCanvas();
			e.opts.pack();
			int tmp = e.opts.gui_slice.getValue();
			e.opts.gui_slice.setValue(0);
			e.opts.gui_slice.setValue(1);
			e.opts.gui_slice.setValue(tmp);

			imgpanel.remove(canvas);
			imgpanel.remove(renderer_left_eye.canvas);
			imgpanel.remove(renderer_right_eye.canvas);

			renderer_left_eye.animator.stop();
			renderer_right_eye.animator.stop();

			if (threeD_mode) {
				imgpanel.add(renderer_left_eye.canvas);
				renderer_left_eye.animator.start();

				if (RenderMode.isStereoscopic(mode)) {
					e.opts.gui_parallax.setVisible(true);
					e.opts.gui_parallaxlabel.setVisible(true);
					renderer_right_eye.animator.start();
					imgpanel.add(renderer_right_eye.canvas);
				} else {
					e.opts.gui_parallax.setVisible(false);
					e.opts.gui_parallaxlabel.setVisible(false);
				}

				updateParallax();
				e.opts.pack();
			} else {
				imgpanel.add(canvas);
				e.opts.pack();
			}
		} finally {
			e.rwLock.writeLock().unlock();
		}
	}
	
	public void updateParallax() {
		if (RenderMode.isStereoscopic((RenderMode)e.opts.gui_3d_view.getSelectedItem())) {
			if ((RenderMode)e.opts.gui_3d_view.getSelectedItem() == RenderMode.THREED_STEREO) {
				renderer_left_eye.eye_offset = e.opts.gui_parallax.getValue()/2f;
				renderer_right_eye.eye_offset = -e.opts.gui_parallax.getValue()/2f;
			} else {
				renderer_left_eye.eye_offset = -e.opts.gui_parallax.getValue()/2f;
				renderer_right_eye.eye_offset = e.opts.gui_parallax.getValue()/2f;
			}
		} else {
			renderer_left_eye.eye_offset = 0;
			renderer_right_eye.eye_offset = 0;
		}

		e.opts.gui_parallaxlabel.setText("Parallax = " + e.opts.gui_parallax.getValue() + " deg");
	}
	
	public void generateCanvas() {
		if (slice_x) {
			imgwidth = (int)Math.ceil(scalefactor*e.ny);
			imgheight = (int)Math.ceil(scalefactor*e.nz);
			e.opts.gui_slice.setMaximum(e.opts.gui_slice.getVisibleAmount() + e.nx - 1);
		} else if (slice_y) {
			imgwidth = (int)Math.ceil(scalefactor*e.nx);
			imgheight = (int)Math.ceil(scalefactor*e.nz);
			e.opts.gui_slice.setMaximum(e.opts.gui_slice.getVisibleAmount() + e.ny - 1);
		} else if (slice_z) {
			imgwidth = (int)Math.ceil(scalefactor*e.nx);
			imgheight = (int)Math.ceil(scalefactor*e.ny);
			e.opts.gui_slice.setMaximum(e.opts.gui_slice.getVisibleAmount() + e.nz - 1);
		} else {
			return;
		}

		canvas.setPreferredSize(new Dimension(imgwidth, imgheight));
		e.screen = (BufferedImage) e.opts.createImage(imgwidth, imgheight);
	}

	
	public void drawPixelRectangle(int x, int y, int z, int wx, int wy, int wz) {
		for (int i = x; i < x+wx; i++) {
			for (int j = y; j < y+wy; j++) {
				for (int k = z; k < z+wz; k++) {
					solid[i][j][k] |= true;
					setPixel(i, j, k);
				}
			}
		}
	}

	public void setPixel(int i, int j, int k) {
		if (i < 0 || j < 0  || k < 0 || i >= e.nx || j >= e.ny || k > e.nz)
			return;

		image_r[i][j][k] = (float)(image_r[i][j][k]*alphaBG + col_r*alphaFG);
		image_g[i][j][k] = (float)(image_g[i][j][k]*alphaBG + col_g*alphaFG);
		image_b[i][j][k] = (float)(image_b[i][j][k]*alphaBG + col_b*alphaFG);
	}

	public void stampPixelData() {
		e.t9.start();
		int[] imgData = ((DataBufferInt)e.screen.getRaster().getDataBuffer()).getData();

		if (slice_z) {
			int scansize = e.nx*scalefactor;
			int k_slice = e.opts.gui_slice.getValue();
			for (int x = 0; x < e.nx*scalefactor; x++) {
				for (int y = 0; y < e.ny*scalefactor; y++) {
					int i = x/scalefactor;
					int j = e.ny-1-y/scalefactor;
					double scale = 1f/max(image_r[i][j][k_slice], image_g[i][j][k_slice], image_b[i][j][k_slice], 1f);
					int rgb = clamp((int)(256*image_r[i][j][k_slice]*scale), 0, 255) << 16
					| clamp((int)(256*image_g[i][j][k_slice]*scale), 0, 255) << 8
					| clamp((int)(256*image_b[i][j][k_slice]*scale), 0, 255);
					imgData[x + y*scansize] = rgb;
				}
			}
		}
		if (slice_x) {
			int scansize = e.ny*scalefactor;
			int i_slice = e.opts.gui_slice.getValue();
			for (int x = 0; x < e.ny*scalefactor; x++) {
				for (int y = 0; y < e.nz*scalefactor; y++) {
					int j = x/scalefactor;
					int k = e.nz-1-y/scalefactor;
					double scale = 1f/max(image_r[i_slice][j][k], image_g[i_slice][j][k], image_b[i_slice][j][k], 1f);
					int rgb = clamp((int)(256*image_r[i_slice][j][k]*scale), 0, 255) << 16
					| clamp((int)(256*image_g[i_slice][j][k]*scale), 0, 255) << 8
					| clamp((int)(256*image_b[i_slice][j][k]*scale), 0, 255);
					imgData[x + y*scansize] = rgb;
				}
			}
		}

		if (slice_y) {
			int scansize = e.nx*scalefactor;
			int j_slice = e.opts.gui_slice.getValue();
			for (int x = 0; x < e.nx*scalefactor; x++) {
				for (int y = 0; y < e.nz*scalefactor; y++) {
					int i = x/scalefactor;
					int k = e.nz-1-y/scalefactor;
					double scale = 1f/max(image_r[i][j_slice][k], image_g[i][j_slice][k], image_b[i][j_slice][k], 1f);
					int rgb = clamp((int)(256*image_r[i][j_slice][k]*scale), 0, 255) << 16
					| clamp((int)(256*image_g[i][j_slice][k]*scale), 0, 255) << 8
					| clamp((int)(256*image_b[i][j_slice][k]*scale), 0, 255);
					imgData[x + y*scansize] = rgb;
				}
			}
		}

		e.t9.stop();
	}

	public void drawLine(int x0, int y0, int x1, int y1, boolean draw_starting_point) {
		y0 = imgheight - 1 - y0;
		y1 = imgheight - 1 - y1;
		try {
			int[] imgData = ((DataBufferInt)e.screen.getRaster().getDataBuffer()).getData();
			int scansize = scalefactor*e.nx;

			int dy = y1 - y0;
			int dx = x1 - x0;
			float t = (float) 0.5;

			int rgb;
			int r;
			int g;
			int b;

			if (draw_starting_point) {
				rgb = imgData[x0+y0*scansize];
				r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
				g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
				b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
				if (r > 255)
					r = 255;
				if (g > 255)
					g = 255;
				if (b > 255)
					b = 255;
				imgData[x0+y0*scansize] =  255<<24 | r << 16 | g << 8 | b;
			}


			if (Math.abs(dx) > Math.abs(dy)) {
				float m = (float) dy / (float) dx;
				t += y0;
				dx = (dx < 0) ? -1 : 1;
				m *= dx;
				while (x0 != x1) {
					x0 += dx;
					t += m;

					rgb =  imgData[x0+((int)t)*scansize];
					r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
					g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
					b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
					if (r > 255)
						r = 255;
					if (g > 255)
						g = 255;
					if (b > 255)
						b = 255;
					imgData[x0+((int)t)*scansize] = 255<<24 | r << 16 | g << 8 | b;

				}
			} else {
				float m = (float) dx / (float) dy;
				t += x0;
				dy = (dy < 0) ? -1 : 1;
				m *= dy;
				while (y0 != y1) {
					y0 += dy;
					t += m;

					rgb =  imgData[((int)t)+y0*scansize];
					r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
					g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
					b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
					if (r > 255)
						r = 255;
					if (g > 255)
						g = 255;
					if (b > 255)
						b = 255;
					imgData[((int)t)+y0*scansize] = 255<<24 | r << 16 | g << 8 | b;
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return;
		}
	}

	public void drawPixelLine(int x0, int y0, int z0, int x1, int y1, int z1) {
		int dx = Math.abs(x1-x0), sx = x0<x1 ? 1 : -1;
		int dy = Math.abs(y1-y0), sy = y0<y1 ? 1 : -1;
		int dz = Math.abs(z1-z0), sz = z0<z1 ? 1 : -1;
		int dm = Math.max(Math.max(dx,dy),dz), i = dm;
		x1 = y1 = z1 = dm/2;

		while (true) {
			solid[x0][y0][z0] |= true;
			setPixel(x0,y0,z0);
			if (i == 0) break;
			i--;
			x1 -= dx; if (x1 < 0) { x1 += dm; x0 += sx; }
			y1 -= dy; if (y1 < 0) { y1 += dm; y0 += sy; }
			z1 -= dz; if (z1 < 0) { z1 += dm; z0 += sz; }
		}
	}

	public void setColor(int r, int g, int b) {
		col_r = r/255f;
		col_g = g/255f;
		col_b = b/255f;
	}

	public void setColorFloat(float r, float g, float b) {
		if (!(r+b+g < Float.MAX_VALUE)) {
			col_r = 0;
			col_g = 0;
			col_b = 0;
			return;
		}
		float scale = 1f/max(r, g, b, 1f);
		col_r = Math.max(r*scale, 0);
		col_g = Math.max(g*scale, 0);
		col_b = Math.max(b*scale, 0);
	}

	public float max(float x, float y, float z) {
		return Math.max(Math.max(x, y), z);
	}

	public float max(float x, float y, float z, float w) {
		return Math.max(Math.max(Math.max(x, y), z), w);
	}

	public float min(float x, float y, float z) {
		return Math.min(Math.min(x, y), z);
	}

	public float min(float x, float y, float z, float w) {
		return Math.min(Math.min(Math.min(x, y), z), w);
	}

	public int clamp(int val, int min, int max) {
		if (val < min) return min;
		if (val > max) return max;
		return val;
	}

	public double clamp(double val, double min, double max) {
		if ((val != val) || (val < min)) return min;
		if (val > max) return max;
		return val;
	}

	public void setalphaBG(double alpha) {
		alphaBG = alpha;
	}

	public void setalphaFG(double alpha) {
		alphaFG = alpha;
	}
	
	public void generatePixelData() {
		for (int i = 0; i < e.nx; i++) {
			for (int j = 0; j < e.ny; j++) {
				for (int k = 0; k < e.nz; k++) {
					image_r[i][j][k] = 0;
					image_g[i][j][k] = 0;
					image_b[i][j][k] = 0;
					solid[i][j][k] = false;
				}
			}
		}

		/* Draw pixels */

		setalphaBG(0);
		setalphaFG(1);

		Brush brush = (Brush) e.opts.gui_brush.getSelectedItem();

		if (e.opts.gui_elem_colors.isSelected()) {
			for (int i = 0; i < e.nx; i++) {
				for (int j = 0; j < e.ny; j++) {
					for (int k = 0; k < e.nz; k++) {
						setColor(e.materials[i][j][k].type.color_r, e.materials[i][j][k].type.color_g, e.materials[i][j][k].type.color_b);

						setPixel(i, j, k);

						solid[i][j][k] = e.materials[i][j][k].type != MaterialType.VACUUM;
					}
				}
			}
		} else {
			for (int i = 0; i < e.nx; i++) {
				for (int j = 0; j < e.ny; j++) {
					for (int k = 0; k < e.nz; k++) {
						setColor(e.materials[i][j][k].type.color_grayscale, e.materials[i][j][k].type.color_grayscale, e.materials[i][j][k].type.color_grayscale);
						setPixel(i, j, k);

						solid[i][j][k] = e.materials[i][j][k].type != MaterialType.VACUUM;
					}
				}
			}
		}

		if (e.controls.moving_selection) {
			if (e.opts.gui_elem_colors.isSelected()) {
				for (int i = 0; i < e.nx; i++) {
					for (int j = 0; j < e.ny; j++) {
						for (int k = 0; k < e.nz; k++) {
							int si = i-e.controls.delta_mx_index;
							int sj = j-e.controls.delta_my_index;
							int sk = k-e.controls.delta_mz_index;
							if (si >= 0 && sj >= 0 && sk >= 0 && si < e.nx && sj < e.ny && sk < e.nz && e.controls.selection[si][sj][sk].type != MaterialType.VACUUM) {
								setColor(e.controls.selection[si][sj][sk].type.color_r, e.controls.selection[si][sj][sk].type.color_g, e.controls.selection[si][sj][sk].type.color_b);
								setPixel(i, j, k);

								solid[i][j][k] |= e.controls.selection[si][sj][sk].type != MaterialType.VACUUM;
							}
						}
					}
				}
			}else {
				for (int i = 0; i < e.nx; i++) {
					for (int j = 0; j < e.ny; j++) {
						for (int k = 0; k < e.nz; k++) {
							int si = i-e.controls.delta_mx_index;
							int sj = j-e.controls.delta_my_index;
							int sk = k-e.controls.delta_mz_index;
							if (si >= 0 && sj >= 0 && sk >= 0 && si < e.nx && sj < e.ny && sk < e.nz && e.controls.selection[si][sj][sk].type != MaterialType.VACUUM) {
								setColor(e.controls.selection[si][sj][sk].type.color_grayscale, e.controls.selection[si][sj][sk].type.color_grayscale, e.controls.selection[si][sj][sk].type.color_grayscale);
								setPixel(i, j, k);

								solid[i][j][k] |= e.controls.selection[si][sj][sk].type != MaterialType.VACUUM;
							}
						}
					}
				}
			}
		}

		double scalingconstant = 0.1*Math.pow(10.0, e.opts.gui_brightness.getValue()/10.0)/((ScalarView) e.opts.gui_view.getSelectedItem()).scale;
		if ((ScalarView) e.opts.gui_view.getSelectedItem() != ScalarView.NONE) {
			setalphaBG(1.0);
			setalphaFG(1.0);
			for (int i = e.absorber_width; i < e.nx-e.absorber_width; i++) {
				for (int j = e.absorber_width; j < e.ny-e.absorber_width; j++) {
					for (int k = e.absorber_width; k < e.nz-e.absorber_width; k++) {
						float v = 0;
						double vx = 0;
						double vy = 0;
						double vz = 0;
						switch ((ScalarView) e.opts.gui_view.getSelectedItem()) {
						case NONE:
							setColorFloat(0, 0, 0);
							break;
						case E_FIELD:
							vx = 0.5*(e.Ex[i][j][k]+e.Ex[i-1][j][k]);
							vy = 0.5*(e.Ey[i][j][k]+e.Ey[i][j-1][k]);
							vz = 0.5*(e.Ez[i][j][k]+e.Ez[i][j][k-1]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant), 0);
							break;
						case D_FIELD:
							vx = 0.5*(e.Dx[i][j][k]+e.Dx[i-1][j][k]);
							vy = 0.5*(e.Dy[i][j][k]+e.Dy[i][j-1][k]);
							vz = 0.5*(e.Dz[i][j][k]+e.Dz[i][j][k-1]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant), 0);
							break;
						case B_FIELD:
							vx = 0.25*(e.Bx[i][j+1][k+1]+e.Bx[i][j][k+1]+e.Bx[i][j+1][k]+e.Bx[i][j][k]);
							vy = 0.25*(e.By[i+1][j][k+1]+e.By[i][j][k+1]+e.By[i+1][j][k]+e.By[i][j][k]);
							vz = 0.25*(e.Bz[i+1][j+1][k]+e.Bz[i][j+1][k]+e.Bz[i+1][j][k]+e.Bz[i][j][k]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant), 0);
							break;
						case H_FIELD:
							vx = 0.25*(e.Hx[i][j][k]+e.Hx[i][j-1][k]+e.Hx[i][j][k-1]+e.Hx[i][j-1][k-1]);
							vy = 0.25*(e.Hy[i][j][k]+e.Hy[i-1][j][k]+e.Hy[i][j][k-1]+e.Hy[i-1][j][k-1]);
							vz = 0.25*(e.Hz[i][j][k]+e.Hz[i-1][j][k]+e.Hz[i][j-1][k]+e.Hz[i-1][j-1][k]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant), 0);
							break;
						case CURRENT:
							vx = 0.5*(e.Jx_free[i][j][k]+e.Jx_free[i-1][j][k]);
							vy = 0.5*(e.Jy_free[i][j][k]+e.Jy_free[i][j-1][k]);
							vz = 0.5*(e.Jz_free[i][j][k]+e.Jz_free[i][j][k-1]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant), 0);
							break;
						case POTENTIAL:
							v = (float)(e.phi[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case CHARGE:
							v = (float)(e.rho_free[i][j][k]*scalingconstant);
							float rc = Math.min(Math.max(v, 0), 1);
							float bc = Math.min(Math.max(-v, 0), 1);
							float gc = Math.min(rc, bc);

							setalphaBG(1-0.5*gc);
							//setalphaFG(gc);
							setColorFloat(rc, gc, bc);
							break;
						case BACKGROUND_CHARGE:
							v = (float)(e.rho_back[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case ELECTRON_CHARGE:
							v = (float)(e.rho_n[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case HOLE_CHARGE:
							v = (float)(e.rho_p[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case COMBINED_CHARGE:
							rc = (float)(clamp(0.2*Math.log(e.rho_p[i][j][k]*scalingconstant), 0, 1));
							bc = (float)(clamp(0.2*Math.log(-e.rho_n[i][j][k]*scalingconstant), 0, 1));
							gc = Math.min(rc, bc);
							double it = Math.max(rc, bc);
							setalphaBG(1-0.5*gc);
							setalphaFG(0.6*it);
							setColorFloat(rc, gc, bc);
							break;
						case ENERGY:
							v = (float)(e.u[i][j][k]*scalingconstant);
							setColorFloat(0, v, 0);
							break;
						case ENTROPY:
							v = (float)(e.S[i][j][k]*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case HEAT:
							v = (float)(e.Q[i][j][k]*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case ELECTRON_POTENTIAL:
							v = (float)(e.conducting[i][j][k]*(e.F_n[i][j][k]/e.q_n+e.phi[i][j][k]-e.W_semi/e.eVtoJ)*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case HOLE_POTENTIAL:
							v = (float)(e.conducting[i][j][k]*(e.F_p[i][j][k]/e.q_p+e.phi[i][j][k]-e.W_semi/e.eVtoJ)*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case DEBUG:
							v = (float)(e.debug[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case DEBUG2:
							v = (float)(e.debug2[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case RECOMBINATION:
							v = (float)(-e.G[i][j][k]*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case AVERAGE_POTENTIAL:
							v = (float)(e.F[i][j][k]*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case LIGHT:
							v = (float)(-e.materials[i][j][k].semiconducting*e.G[i][j][k]*scalingconstant);
							setColorFloat(v, v, v);
							break;
						}
						setPixel(i, j, k);
					}
				}
			}
		}

		boolean highlight = (e.opts.gui_brush_highlight.isSelected() && Brush.isBrushShapeImportant(brush));
		for (int i = 0; i < e.nx; i++) {
			for (int j = 0; j < e.ny; j++) {
				for (int k = 0; k < e.nz; k++) {
					if ((e.materials[i][j][k].type == MaterialType.EMF || e.materials[i][j][k].type == MaterialType.AC_EMF) && i > 0 && j > 0 && k > 0 && i < e.nx-1 && j < e.ny-1 && k < e.nz-1) {
						MaterialType type = e.materials[i][j][k].type;
						setalphaBG(0.25);
						setalphaFG(0.75);

						int offset = 0;
						if (e.controls.selected_EMF[i][j][k])
							offset = 60*(2*((i+j+k)%2)-1);

						if (!threeD_mode && (e.materials[i+1][j][k].type != type
						|| e.materials[i-1][j][k].type != type
						|| e.materials[i][j+1][k].type != type
						|| e.materials[i][j-1][k].type != type))
						{
							offset = -30;
						}

						int delta_r = type.color_r+offset;
						int delta_g = type.color_g+offset;
						int delta_b = type.color_b+offset;
						setColor(delta_r, delta_g, delta_b);

						setPixel(i, j, k);
					} else if (!(e.materials[i][j][k].activated == 1)) {
						setalphaBG(0.25);
						setalphaFG(0.75);
						setColor(0, 0, 0);
						setPixel(i, j, k);
					}

					if (e.controls.selected[i][j][k] || (highlight && e.controls.under_brush[i][j][k]))
					{
						setalphaBG(0.75);
						setalphaFG(0.25);

						int s = e.controls.selected[i][j][k]? 1:0;
						int h = (highlight && e.controls.under_brush[i][j][k])? 1:0;
						int delta_r = 256*s + 256*h;
						int delta_g = 100*s + 256*h;
						int delta_b = 256*s + 256*h;

						setColor(delta_r, delta_g, delta_b);

						setPixel(i, j, k);
					}
				}

			}
		}


		setalphaBG(1);
		setalphaFG(1);
		setColorFloat(0.7f, 0.7f, 0.7f);

		if (brush == Brush.LINE && e.controls.mouse_pressed) {
			drawPixelLine(e.controls.mx_start_index, e.controls.my_start_index, e.controls.mz_start_index, e.controls.mx_index, e.controls.my_index, e.controls.mz_index);
		}


		setalphaFG(1.0);
		setColorFloat(0.5f, 1.0f, 1.0f);

		for (VoltageProbe p: e.voltageprobes) {
			drawPixelRectangle(p.x, p.y, p.z, 1, 1, 1);
		}

		for (CurrentProbe p: e.currentprobes) {
			drawPixelRectangle(Math.min(p.x1, p.x2), Math.min(p.y1, p.y2), Math.min(p.z1, p.z2), Math.abs(p.x1-p.x2)+1, Math.abs(p.y1-p.y2)+1, Math.abs(p.z1-p.z2)+1);
		}

		if (e.ground != null) {
			drawPixelRectangle(e.ground.x-1, e.ground.y-1, e.ground.z-1, 1, 1, 1);
		}

		/*setalphaFG(0.8);
		setColorFloat(1.0f, 1.0f, 1.0f);

		if (texting) {
			drawPixelLine(text_x, text_y, text_x, text_y+7);
		}*/
	}

	public void drawVectors() {
		/* Draw vectors */

		if ((VectorView) e.opts.gui_view_vec.getSelectedItem() != VectorView.NONE) {
			setalphaBG(1.0);

			double arrowlength = 25.0/scalefactor;

			double vectorscalingconstant = Math.pow(10.0, e.opts.gui_brightness_vec.getValue()/5.0)/((VectorView) e.opts.gui_view_vec.getSelectedItem()).scale;

			VectorMode vector_display_mode = (VectorMode)e.opts.gui_view_vec_mode.getSelectedItem();

			rand.setSeed(4);

			int density = 25;

			double randomness = 0;

			if (vector_display_mode == VectorMode.ARROWS) {
				randomness = 0.5;
			} else if (vector_display_mode == VectorMode.LINES) {
				randomness = 0.75;
			}

			double[][][] vf_x = null;
			double[][][] vf_y = null;
			double[][][] vf_z = null;

			double[][][] vf_px = null;
			double[][][] vf_py = null;

        	double grid_offset = -0.5;
        	double dual_offset = 0;

			switch ((VectorView) e.opts.gui_view_vec.getSelectedItem()) {
			case NONE:
				break;
        	case B_FIELD:
        		vf_x = e.Bx;
        		vf_y = e.By;
        		vf_z = e.Bz;
        		grid_offset = 0;
        		dual_offset = 0.5;
        		break;
			case H_FIELD:
				vf_x = e.Hx;
				vf_y = e.Hy;
				vf_z = e.Hz;
        		grid_offset = 0;
        		dual_offset = -0.5;
				break;
			case E_FIELD:
				vf_x = e.Ex;
				vf_y = e.Ey;
				vf_z = e.Ez;
				break;
			case D_FIELD:
				vf_x = e.Dx;
				vf_y = e.Dy;
				vf_z = e.Dz;
				break;
			case ELECTRON_CURRENT:
				vf_x = e.Jx_n;
				vf_y = e.Jy_n;
				vf_z = e.Jz_n;
				break;
			case HOLE_CURRENT:
				vf_x = e.Jx_p;
				vf_y = e.Jy_p;
				vf_z = e.Jz_p;
				break;
			case TOTAL_CURRENT:
				vf_x = e.Jx_free;
				vf_y = e.Jy_free;
				vf_z = e.Jz_free;
				break;
			case POYNTING:
				vf_x = e.Sx;
				vf_y = e.Sy;
				vf_z = e.Sz;
				break;
			case EMF:
				vf_x = e.emfx;
				vf_y = e.emfy;
				vf_z = e.emfz;
				break;
			}

			int npx = 0;
			int npy = 0;

			if (slice_x) {
				vf_px = vf_y;
				vf_py = vf_z;
				npx = e.ny;
				npy = e.nz;
			} else if (slice_y) {
				vf_px = vf_x;
				vf_py = vf_z;
				npx = e.nx;
				npy = e.nz;
			} else if (slice_z) {
				vf_px = vf_x;
				vf_py = vf_y;
				npx = e.nx;
				npy = e.ny;
			}

			int slice = e.opts.gui_slice.getValue();


			Vector3 ctr = new Vector3(0,0,0);
			Vector3 arrow = new Vector3(0,0,0);
			Vector3 tip1 = new Vector3(0,0,0);
			Vector3 tip2 = new Vector3(0,0,0);
			Vector3 body1 = new Vector3(0,0,0);
			Vector3 body2 = new Vector3(0,0,0);

			for (int pi = 0; pi < density; pi++) {
				for (int pj = 0; pj < density; pj++) {

					//double x = (nx-1)*(i+0.5)/50;
					//double y = (ny-1)*(j+0.5)/50;
					double px = npx*(pi+randomness*(rand.nextFloat()-0.5))/density;
					double py = npy*(pj+randomness*(rand.nextFloat()-0.5))/density;


					if (vector_display_mode == VectorMode.LINES && vf_px != null) {
						for (int sign = -1; sign <= 1; sign += 2) {

							double prevpx = px;
							double prevpy = py;
							double dpx = 0;
							double dpy = 0;

							for (int m = 0; m < 10; m++) {
								if (slice_x) {
									dpx = Utils.bilinearinterp(vf_px, slice+dual_offset, prevpx+grid_offset, prevpy+dual_offset);
									dpy = Utils.bilinearinterp(vf_py, slice+dual_offset, prevpx+dual_offset, prevpy+grid_offset);
								} else if (slice_y) {
									dpx = Utils.bilinearinterp(vf_px, prevpx+grid_offset, slice+dual_offset, prevpy+dual_offset);
									dpy = Utils.bilinearinterp(vf_py, prevpx+dual_offset, slice+dual_offset, prevpy+grid_offset);
								} else if (slice_z) {
									dpx = Utils.bilinearinterp(vf_px, prevpx+grid_offset, prevpy+dual_offset, slice+dual_offset);
									dpy = Utils.bilinearinterp(vf_py, prevpx+dual_offset, prevpy+grid_offset, slice+dual_offset);
								}

								double fieldmagnitude = Math.sqrt(dpx*dpx+dpy*dpy);
								setalphaFG(0.1*Math.sqrt(1/(0.1*m*m+1.0)*vectorscalingconstant*fieldmagnitude));

								if (fieldmagnitude != 0) {
									dpx /= fieldmagnitude;
									dpy /= fieldmagnitude;
								}

								//setcol(fieldmagnitude, fieldmagnitude, fieldmagnitude, 30);
								setColorFloat(1.0f, 1.0f, 1.0f);

								double nextx = prevpx + dpx*arrowlength*0.25*sign;
								double nexty = prevpy + dpy*arrowlength*0.25*sign;

								drawLine((int)((prevpx+0.5)*scalefactor), (int)((prevpy+0.5)*scalefactor), (int)((nextx+0.5)*scalefactor), (int)((nexty+0.5)*scalefactor), m == 0 && sign == 1);

								prevpx = nextx;
								prevpy = nexty;
							}
						}
					} else if (vector_display_mode == VectorMode.ARROWS && vf_px != null) {
						ctr.x = px+0.5;
						ctr.y = py+0.5;

						if (slice_x) {
							arrow.x = Utils.bilinearinterp(vf_px, slice+dual_offset, px+grid_offset, py+dual_offset);
							arrow.y = Utils.bilinearinterp(vf_py, slice+dual_offset, px+dual_offset, py+grid_offset);
						} else if (slice_y) {
							arrow.x = Utils.bilinearinterp(vf_px,px+grid_offset, slice+dual_offset, py+dual_offset);
							arrow.y = Utils.bilinearinterp(vf_py,px+dual_offset, slice+dual_offset, py+grid_offset);
						} else if (slice_z) {
							arrow.x = Utils.bilinearinterp(vf_px,px+grid_offset, py+dual_offset, slice+dual_offset);
							arrow.y = Utils.bilinearinterp(vf_py,px+dual_offset, py+grid_offset, slice+dual_offset);
						}

						double fieldmagnitude = Math.max(0.1, vectorscalingconstant*Math.sqrt(arrow.dot(arrow)));
						arrow.normalize();
						tip1.copy(arrow);
						tip2.copy(arrow);
						tip1.rotate_z(Math.PI*5.0/6.0);
						tip2.rotate_z(Math.PI*7.0/6.0);

						body1.copy(ctr);
						body1.addmult(arrow, -0.5*arrowlength);
						body2.copy(ctr);
						body2.addmult(arrow, 0.5*arrowlength);
						tip1.scalarmult(0.35*arrowlength);
						tip1.add(body2);
						tip2.scalarmult(0.35*arrowlength);
						tip2.add(body2);
						setColorFloat((float)fieldmagnitude, (float)fieldmagnitude, (float)fieldmagnitude);
						setalphaFG(0.1*Math.sqrt(fieldmagnitude));
						drawLine((int)(body1.x*scalefactor), (int)(body1.y*scalefactor), (int)(body2.x*scalefactor), (int)(body2.y*scalefactor), true);
						drawLine((int)(body2.x*scalefactor), (int)(body2.y*scalefactor), (int)(tip1.x*scalefactor), (int)(tip1.y*scalefactor), false);
						drawLine((int)(body2.x*scalefactor), (int)(body2.y*scalefactor), (int)(tip2.x*scalefactor), (int)(tip2.y*scalefactor), false);
					}
				}
			}
		}
	}
	
	public void render() {
		
		Graphics2D g = (Graphics2D) e.screen.getGraphics();
		
		generatePixelData();

		stampPixelData();

		drawVectors();

		g.setRenderingHint(
		RenderingHints.KEY_TEXT_ANTIALIASING,
		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		texts.clear();
		generateText(g, texts);
		drawStrings(g);
	}


	public void generateText(Graphics g, ArrayList<Text> texts) {
		/* Draw text */

		for (VoltageProbe p: e.voltageprobes) {
			if (e.ground != null)
				draw3dStringWithBackground("V = " + getSI(p.potential - e.ground.potential, "V"), p.x, p.y, p.z, true, false, texts);
			else
				draw3dStringWithBackground("V = " + getSI(p.potential, "V"), p.x, p.y, p.z, true, false, texts);
		}

		if (e.ground != null)
			draw3dStringWithBackground("Ground = " + getSI(e.ground.potential - e.ground.potential, "V"), e.ground.x, e.ground.y, e.ground.z, true, false, texts);

		for (CurrentProbe p: e.currentprobes) {
			draw3dStringWithBackground("I = " + getSI(p.current*e.depth, "A"), (p.x1+p.x2)/2, (p.y1+p.y2)/2, (p.z1+p.z2)/2, true, false, texts);
		}

		{
			double mx_t = e.controls.mx_index;
			double my_t = e.controls.my_index;
			double mz_t = e.controls.mz_index;
			//double mx_t = (mouseX/(double)scalefactor);
			//double my_t = (mouseY/(double)scalefactor);
			int mi = e.controls.mx_index;
			int mj = e.controls.my_index;
			int mk = e.controls.mz_index;

			if (mi < 0)
				mi = 0;
			if (mi >= e.nx-1)
				mi = e.nx-2;
			if (mj < 0)
				mj = 0;
			if (mj >= e.ny-1)
				mj = e.ny-2;
			if (mk < 0)
				mk = 0;
			if (mk >= e.nz-1)
				mk = e.nz-2;

			Material mat = e.materials[mi][mj][mk];

			int vspacing = 12;
			int voffset = 1 + e.controls.my_3d;
			int hoffset = 5 + e.controls.mx_3d+15;

			if (e.opts.gui_tooltip.isSelected()) {
				if (voffset + 22*vspacing > imgheight) {
					voffset = voffset - ((voffset + 22*vspacing) - imgheight);
				}
				if (hoffset + 120 > imgwidth) {
					hoffset = hoffset - ((hoffset + 120) - imgwidth);
				}
			}

			String name = "Material: " + mat.type.name + (mat.modified? " (Modified)" : "");

			drawBig2dStringWithBackground(name, hoffset, voffset + 1*vspacing, texts);
			if (e.opts.gui_tooltip.isSelected()) {
				voffset = voffset+3;
				int line = 2;
				drawTwoColumnString("E" , 							getSI(getFieldMagnitude(e.Ex, e.Ey, e.Ez, mx_t, my_t, mz_t), "V/m"), hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("D" , 							getSI(getFieldMagnitude(e.Dx, e.Dy, e.Dz, mx_t, my_t, mz_t), "C/m^2"), hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("B" , 							getSI(getDualFieldMagnitude(e.Bx, e.By, e.Bz, mx_t, my_t, mz_t), "T"),	hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("H" , 							getSI(getDualFieldMagnitude(e.Hx, e.Hy, e.Hz, mx_t, my_t, mz_t), "A/m"),	hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("\u03d5" , 						getSI(Utils.bilinearinterp(e.phi,mx_t, my_t, mz_t), "V"),					hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("\u03b5/\u03b5\u2080" , 		getSI(mat.eps_r, ""),										hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("\u03bc/\u03bc\u2080" , 		getSI(mat.mu_r, ""),											hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("\u03c1\u2099" , 				getSI(Utils.bilinearinterp(e.rho_n,mx_t, my_t, mz_t), "C/m^3"),			hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("\u03c1\u209A" , 				getSI(Utils.bilinearinterp(e.rho_p,mx_t, my_t, mz_t), "C/m^3"),			hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("\u03c1\u2080",					getSI(Utils.bilinearinterp(e.rho_back,mx_t, my_t, mz_t), "C/m^3"),		hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("\u03c1" , 						getSI(Utils.bilinearinterp(e.rho_free,mx_t, my_t, mz_t), "C/m^3"),		hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("J\u2099" ,						getSI(getFieldMagnitude(e.Jx_n, e.Jy_n, e.Jz_n, mx_t, my_t, mz_t), "A/m^2"),			hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("J\u209A" , 					getSI(getFieldMagnitude(e.Jx_p, e.Jy_p, e.Jz_p, mx_t, my_t, mz_t), "A/m^2"),			hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("J" , 							getSI(getFieldMagnitude(e.Jx_free, e.Jy_free, e.Jz_free, mx_t, my_t, mz_t), "A/m^2"),	hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("F\u2099" , 					getSI(Utils.bilinearinterp(e.F_n,mx_t, my_t, mz_t)/e.q_n+Utils.bilinearinterp(e.phi,mx_t, my_t, mz_t)-e.W_semi/e.eVtoJ, "V"),	hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("F\u209a" , 					getSI(Utils.bilinearinterp(e.F_p,mx_t, my_t, mz_t)/e.q_p+Utils.bilinearinterp(e.phi,mx_t, my_t, mz_t)-e.W_semi/e.eVtoJ, "V"),	hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("x" , 							getSI(mx_t*e.ds, "m"),								hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("y" , 							getSI(my_t*e.ds, "m"),								hoffset, voffset + line*vspacing, texts); line++;
				drawTwoColumnString("z" , 							getSI(mz_t*e.ds, "m"),								hoffset, voffset + line*vspacing, texts); line++;
			}
		}

		int vspacing = 13;
		int voffset = 3;
		int hoffset = 5;
		int line = 1;
		draw2dStringWithBackground("Time: " + getSI(e.time, "s"), hoffset, voffset + line*vspacing, texts); line++;
		if (e.opts.gui_paused.isSelected())
		{
			draw2dStringWithBackground("Paused", hoffset, voffset + line*vspacing, texts); line++;
		}
		if (e.sign_violation) {
			draw2dStringWithBackground("Warning: Numerical instability detected. Please decrease timestep.", hoffset, voffset + line*vspacing, texts); line++;
		}
		if (e.controls.debugging) {
			long total = Runtime.getRuntime().totalMemory();
			long used  = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			draw2dStringWithBackground("Used memory " + Utils.getSI(used, "B"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground("Total memory " + Utils.getSI(total, "B"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground(e.t4.getName() + " " + Utils.getSI(e.t4.getAverageTime(), "s"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground(t5.getName() + " " + Utils.getSI(t5.getAverageTime(), "s"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground(e.t6.getName() + " " + Utils.getSI(e.t6.getAverageTime()*e.opts.gui_simspeed_2.getValue(), "s"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground(e.t7.getName() + " " + Utils.getSI(e.t7.getAverageTime(), "s"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground(e.t8.getName() + " " + Utils.getSI(e.t8.getAverageTime(), "s"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground(FPStimer.getName() + " " + Utils.getSI(1/FPStimer.getAverageTime(), "Hz"), hoffset, voffset + line*vspacing, texts); line++;
			draw2dStringWithBackground(e.simFPStimer.getName() + " " + Utils.getSI(1/e.simFPStimer.getAverageTime(), "Hz"), hoffset, voffset + line*vspacing, texts); line++;
		}
	}

	double getFieldMagnitude(double[][][] vx, double[][][] vy, double[][][] vz, double x, double y, double z) {
		return length(Utils.bilinearinterp(vx, x-0.5 , y, z), Utils.bilinearinterp(vy, x, y-0.5, z), Utils.bilinearinterp(vz, x, y, z-0.5));
	}

	double getDualFieldMagnitude(double[][][] vx, double[][][] vy, double[][][] vz, double x, double y, double z) {
		return length(Utils.bilinearinterp(vx, x, y-0.5, z-0.5), Utils.bilinearinterp(vy, x-0.5, y, z-0.5), Utils.bilinearinterp(vz, x-0.5, y-0.5, z));
	}

	public void drawStrings(Graphics g) {
		if (g != null) {
			if (e.opts.gui_text_bg.isSelected())
			{
				for (Text text : texts) {
					if (text.hasBackground) {
						if (text.big)
							g.setFont(bigfont);
						else
							g.setFont(regularfont);

						int width = Math.max(text.minwidth, g.getFontMetrics().stringWidth(text.text)+8);
						int height = g.getFontMetrics().getHeight()+4;
						int x = text.x-3;
						int y = text.y-height+6;

						g.setColor(Color.GRAY);
						g.fillRect(x-2, y-2, width+4, height+4);
					}
				}

				for (Text text : texts) {
					if (text.hasBackground) {
						if (text.big)
							g.setFont(bigfont);
						else
							g.setFont(regularfont);

						int width = Math.max(text.minwidth, g.getFontMetrics().stringWidth(text.text)+8);
						int height = g.getFontMetrics().getHeight()+4;
						int x = text.x-3;
						int y = text.y-height+6;

						g.setColor(Color.BLACK);
						g.fillRect(x, y, width, height);
					}
				}
			}

			for (Text text : texts) {
				if (text.big)
					g.setFont(bigfont);
				else
					g.setFont(regularfont);

				g.setColor(Color.DARK_GRAY);
				g.drawString(text.text, text.x+1, text.y+1);
				g.setColor(Color.WHITE);
				g.drawString(text.text, text.x, text.y);
			}
		}
	}

	public void draw2dStringWithBackground(String str1, int x, int y, ArrayList<Text> texts) {
		texts.add(new Text(str1, x, y, 0, false, true, false));
	}

	public void drawBig2dStringWithBackground(String str1, int x, int y, ArrayList<Text> texts) {
		texts.add(new Text(str1, x, y, 0, true, true, false));
	}


	public void draw3dStringWithBackground(String str1, int x, int y, int z, boolean hasBackground, boolean isBig, ArrayList<Text> texts) {
		texts.add(new Text(str1, x, y, z, hasBackground, isBig, true));
	}

	public void drawTwoColumnString(String str1, String str2, int x, int y, ArrayList<Text> texts) {
		texts.add(new Text(String.format("%-10s", str1), x, y, 0, false, true, false));
		texts.add(new Text(str2, x+40, y, 0, false, true, false));
		texts.get(texts.size()-2).minwidth = 80;
		texts.get(texts.size()-1).minwidth = 80;
	}

	public double length(double x, double y) {
		return Math.sqrt(x*x+y*y);
	}

	public double length(double x, double y, double z) {
		return Math.sqrt(x*x+y*y+z*z);
	}

	public static String getSI(double quantity, String unit) {
		if (!Double.isFinite(quantity))
			return Double.toString(quantity) + " " + unit;

		double mag = Math.abs(quantity);
		String precision = "%.2f";
		if (mag < 1E-18)
			return "0 " + unit;
		else if (mag < 1E-15)
			return String.format("%.2f", quantity*1e15) + " f" + unit;
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
		else
			return String.format(precision, quantity*1e-12) + " T" + unit;
	}
}

enum ScalarView {
	NONE("No scalar overlay", 0),
	E_FIELD("View E field magnitude", 1e4),
	D_FIELD("View D field magnitude", 1e4*8.85e-12),
	B_FIELD("View B field magnitude", 1e-5),
	H_FIELD("View H field magnitude", 1e-5/1.26e-6),
	CHARGE("View \u03c1: Net charge density", 1),
	CURRENT("View J: Total current magnitude", 1e8),
	POTENTIAL("View \u03d5: Electric scalar potential", 0.1),
	ENERGY("View u: Electromagnetic energy density", 1),
	ELECTRON_CHARGE("View \u03c1\u2099: Electron charge density", 1),
	HOLE_CHARGE("View \u03c1\u209A: Hole charge density", 1),
	COMBINED_CHARGE("View: Combined electron+hole charge density", 1),
	BACKGROUND_CHARGE("View \u03c1\u2080: Background charge density", 1),
	HEAT("View Q: Heat dissipation", 1),
	ENTROPY("View s: Entropy generation (Free energy dissipation)", 1),
	ELECTRON_POTENTIAL("View F\u2099: Electron chemical potential (quasi Fermi level)", 0.1),
	HOLE_POTENTIAL("View F\u209A: Hole chemical potential (quasi Fermi level)", 0.1),
	AVERAGE_POTENTIAL("View F: Average electrochemical potential", 0.1),
	RECOMBINATION("View R: Recombination rate", 1e-30),
	LIGHT("View: Emitted light", 1e-30),
	DEBUG("Debug", 1),
	DEBUG2("Debug 2", 1);

	String name;
	double scale; //Typical order of magnitude of the quantity

	ScalarView(String name, double scale)
	{
		this.name = name;
		this.scale = scale;
	}

	@Override
	public String toString() {
		return name;
	}
}

enum VectorView {
	NONE("No vector overlay", 0),
	E_FIELD("View E field", 1e4),
	D_FIELD("View D field", 1e4*8.85e-12),
	B_FIELD("View B field", 1e-5),
	H_FIELD("View H field", 1e-5/1.26e-6),
	ELECTRON_CURRENT("View J\u2099: Electron current", 1e8),
	HOLE_CURRENT("View J\u209A: Hole current", 1e8),
	TOTAL_CURRENT("View J: Total current", 1e8),
	EMF("View \u2130: External electromotive force", 1e4),
	POYNTING("View S: Poynting vector", 1);

	String name;
	double scale;

	VectorView(String name, double scale)
	{
		this.name = name;
		this.scale = scale;
	}

	@Override
	public String toString() {
		return name;
	}
}

enum VectorMode {
	ARROWS("Show vectors"),
	LINES("Show lines");

	String name;
	VectorMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}

enum RenderMode {
	SLICE_X("2D x cross-section"),
	SLICE_Y("2D y cross-section"),
	SLICE_Z("2D z cross-section"),
	THREED("3D orthographic"),
	THREED_FIELDS_ONLY("3D orthographic (fields only)"),
	THREED_TRANSLUCENT("3D orthographic (transparent)"),
	THREED_PERSPECTIVE("3D perspective"),
	THREED_PERSPECTIVE_FIELDS_ONLY("3D perspective (fields only)"),
	THREED_PERSPECTIVE_TRANSLUCENT("3D perspective (transparent)"),
	THREED_STEREO("3D stereoscopic (cross-eye)"),
	THREED_STEREO_INV("3D stereoscopic (parallel)");

	String name;
	RenderMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public static boolean is3d(RenderMode mode) {
		return (mode == THREED || mode == THREED_FIELDS_ONLY || mode == THREED_TRANSLUCENT
		|| mode == THREED_PERSPECTIVE || mode == THREED_PERSPECTIVE_FIELDS_ONLY || mode == THREED_PERSPECTIVE_TRANSLUCENT
		|| mode == THREED_STEREO || mode == THREED_STEREO_INV);
	}

	public static boolean isOrthographic(RenderMode mode) {
		return (mode == THREED || mode == THREED_FIELDS_ONLY || mode == THREED_TRANSLUCENT);
	}

	public static boolean isPerspective(RenderMode mode) {
		return (mode == THREED_PERSPECTIVE || mode == THREED_PERSPECTIVE_FIELDS_ONLY || mode == THREED_PERSPECTIVE_TRANSLUCENT
		|| mode == THREED_STEREO || mode == THREED_STEREO_INV);
	}

	public static boolean isStereoscopic(RenderMode mode) {
		return (mode == THREED_STEREO || mode == THREED_STEREO_INV);
	}
}

class Text {
	String text;
	int x;
	int y;
	int z;
	int minwidth;
	boolean big;
	boolean hasBackground;
	boolean is3D;

	public Text(String text, int x, int y, int z, boolean big, boolean hasBackground, boolean is3D) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.z = z;
		this.big = big;
		this.hasBackground = hasBackground;
		this.is3D = is3D;
		minwidth = 0;
	}
}
