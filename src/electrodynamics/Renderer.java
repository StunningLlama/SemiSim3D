// Copyright (c) Brandon Li 2025-2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import electrodynamics.Controls.Brush;
import electrodynamics.plot.Plot;
import electrodynamics.probe.Ground;
import electrodynamics.probe.Probe;
import electrodynamics.units.Quantity;
import electrodynamics.util.ArrowDrawer;
import electrodynamics.util.DistributionSampler;
import electrodynamics.util.FastList;
import electrodynamics.util.FastRandom;
import electrodynamics.util.PeriodicTask;
import electrodynamics.util.Timer;
import electrodynamics.util.Utils;
import electrodynamics.util.Vector;

public class Renderer extends PeriodicTask {
	Simulation e;
	
	/* Graphics */
	
	public BufferedImage img_back;
	public BufferedImage img_front;
	public int[] imgData;
	public int[] depth_buf;
	public double[][][] scalarfield;
	public double[][][] gradscalarfield;
	public float[][][] image_r;
	public float[][][] image_g;
	public float[][][] image_b;
	public float col_r = 0;
	public float col_g = 0;
	public float col_b = 0;
	public float alphaBG = 0;
	public float alphaFG = 0;

	public ArrayList<Text> texts = new ArrayList<>();
	public int scalefactor;
	public double scalefactor_real;
	public int imgwidth = 0;
	public int imgheight = 0;
	public double targetframerate = 60;
	public double frameduration = 1000/targetframerate;
	
	private double t_prev = 0;
	private double delta_t = 0;

	public ArrayList<Dot> dots = new ArrayList<>();
	public FastList<ChargeCarrierDot> ccdots = new FastList<>();
	public int numdots = 2000;
	public double rho_n_max = 0;
	public double rho_p_max = 0;
	public double C_prev = 0;
	public int carrier_diffusion_warning_timer = 0;

	public int probetexttimer = 0;
	private boolean synchronized_show_carriers;
	private boolean synchronized_update_carriers;
	private boolean synchronized_render;
	private VectorMode synchronized_vector_display_mode;
	private ScalarMode synchronized_scalar_display_mode;
	private ScalarView synchronized_scalar_view;
	private VectorView synchronized_vector_view;
	
	private float scalingconstant;
	private float scalar_offset;
	public boolean display_relative_voltage = false;
	public boolean disp_mat_name = false;
	public boolean drawCrosshairGuides = false;
	public String achievement_name = "";
	public int achievement_timer = 0;
	public String screenshot_name = "";
	public int screenshot_timer = 0;
	
	public int rx;
	public int ry;
	
	/* 3D */
	public JPanel imgpanel;
	public Renderer3D renderer_left_eye;
	public Renderer3D renderer_right_eye;
	boolean[][][] opaque;
	boolean[][][] translucent;
	boolean[][][] solid;
	boolean slice_x = false;
	boolean slice_y = false;
	boolean slice_z = true;
	float pitch = -(float)Math.PI/6;
	float yaw = (float)(Math.PI*5/4);
    float scale = 24f;
    float cam_x = 96*(float)Math.sqrt(3/2.0)/2;
    float cam_y = 96*(float)Math.sqrt(3/2.0)/2;
    float cam_z = 96/2;
	boolean threeD_mode = true;
	

	/* Multithreading */
	
	private CyclicBarrier graphics_start_barrier = new CyclicBarrier(SemiSim.n_threads + 1);
	private CyclicBarrier graphics_mid_barrier = new CyclicBarrier(SemiSim.n_threads);
	private CyclicBarrier graphics_end_barrier = new CyclicBarrier(SemiSim.n_threads + 1);

	/* Electron and hole dots */
	
	public DistributionSampler rho_p_dist = new DistributionSampler();
	public DistributionSampler rho_n_dist = new DistributionSampler();
	public DistributionSampler rho_G_dist = new DistributionSampler();
	private FastRandom frand = new FastRandom();
	public double cc_default_dot_density;	// How many electron/hole dots to draw, in (C/m)^-1
	public double tau;						// How long a dot stays on the screen
	public double tau_events;				// How long a flash stays
	
	/* Performance profiling */
	
	Timer FPStimer = new Timer("Graphics FPS", 10, true);
	Timer t5 = new Timer("Graphics", 20, true);
	
	public Renderer(Simulation e) {
		this.e = e;
		imgpanel = new JPanel();
		imgpanel.setLayout(new GridLayout(1,2));
		imgpanel.setPreferredSize(new Dimension(768, 768));
	}
	
	public void setResolution() {
		scalarfield = new double[e.nx][e.ny][e.nz];
		gradscalarfield = new double[e.nx][e.ny][e.nz];
		image_r = new float[e.nx][e.ny][e.nz];
		image_g = new float[e.nx][e.ny][e.nz];
		image_b = new float[e.nx][e.ny][e.nz];
		opaque = new boolean[e.nx][e.ny][e.nz];
		translucent = new boolean[e.nx][e.ny][e.nz];
		solid = new boolean[e.nx][e.ny][e.nz];

		rho_p_dist.init(e.nx, e.ny, e.nz);
		rho_n_dist.init(e.nx, e.ny, e.nz);
		rho_G_dist.init(e.nx, e.ny, e.nz);
		
		setCanvasSize();
		
		resetChargeDots();
	}
	
	int project_x(int x, int y, int z) {
		if (slice_x) {
			return y;
		} else if (slice_y) {
			return x;
		} else if (slice_z) {
			return x;
		} else {
			return 0;
		}
	}
	
	int project_y(int x, int y, int z) {
		if (slice_x) {
			return z;
		} else if (slice_y) {
			return z;
		} else if (slice_z) {
			return y;
		} else {
			return 0;
		}
	}
	
	int project_z(int x, int y, int z) {
		if (slice_x) {
			return x;
		} else if (slice_y) {
			return y;
		} else if (slice_z) {
			return z;
		} else {
			return 0;
		}
	}
	
	double project_x(double x, double y, double z) {
		if (slice_x) {
			return y;
		} else if (slice_y) {
			return x;
		} else if (slice_z) {
			return x;
		} else {
			return 0;
		}
	}
	
	double project_y(double x, double y, double z) {
		if (slice_x) {
			return z;
		} else if (slice_y) {
			return z;
		} else if (slice_z) {
			return y;
		} else {
			return 0;
		}
	}
	
	double project_z(double x, double y, double z) {
		if (slice_x) {
			return x;
		} else if (slice_y) {
			return y;
		} else if (slice_z) {
			return z;
		} else {
			return 0;
		}
	}
	
	int embed_x(int x, int y, int z) {
		if (slice_x) {
			return z;
		} else if (slice_y) {
			return x;
		} else if (slice_z) {
			return x;
		} else {
			return 0;
		}
	}
	
	int embed_y(int x, int y, int z) {
		if (slice_x) {
			return x;
		} else if (slice_y) {
			return z;
		} else if (slice_z) {
			return y;
		} else {
			return 0;
		}
	}
	
	int embed_z(int x, int y, int z) {
		if (slice_x) {
			return y;
		} else if (slice_y) {
			return y;
		} else if (slice_z) {
			return z;
		} else {
			return 0;
		}
	}
	
	double embed_x(double x, double y, double z) {
		if (slice_x) {
			return z;
		} else if (slice_y) {
			return x;
		} else if (slice_z) {
			return x;
		} else {
			return 0;
		}
	}
	
	double embed_y(double x, double y, double z) {
		if (slice_x) {
			return x;
		} else if (slice_y) {
			return z;
		} else if (slice_z) {
			return y;
		} else {
			return 0;
		}
	}
	
	double embed_z(double x, double y, double z) {
		if (slice_x) {
			return y;
		} else if (slice_y) {
			return y;
		} else if (slice_z) {
			return z;
		} else {
			return 0;
		}
	}
	
	public int getSlice() {
		int slice = e.opts.gui_slice.getValue();
		if (slice < 0) slice = 0;
		int max = project_z(e.nx, e.ny, e.nz);
		if (slice >= max)
			slice = max-1;
		return slice;
	}
	
	public void setCanvasSize() {
		RenderMode mode = e.controls.rendermode.getOption();
		threeD_mode = RenderMode.is3d(mode);
		slice_x = (mode == RenderMode.SLICE_X);
		slice_y = (mode == RenderMode.SLICE_Y);
		slice_z = (mode == RenderMode.SLICE_Z);

		rx = e.renderer.project_x(e.nx, e.ny, e.nz);
		ry = e.renderer.project_y(e.nx, e.ny, e.nz);

		scalefactor_real = Math.min(imgpanel.getWidth()/(double)rx, imgpanel.getHeight()/(double)ry);
		scalefactor = (int)scalefactor_real;
		if (scalefactor < 1) scalefactor = 1;
		int imgwidth_new = (int)Math.ceil(scalefactor*rx);
		int imgheight_new = (int)Math.ceil(scalefactor*ry);
		e.opts.gui_slice.setMaximum(project_z(e.nx, e.ny, e.nz) + e.opts.gui_slice.getVisibleAmount() - 1);
		
		if (!threeD_mode && (imgwidth_new != imgwidth || imgheight_new != imgheight)) {
			imgwidth = imgwidth_new;
			imgheight = imgheight_new;
			img_back = (BufferedImage) e.opts.createImage(imgwidth, imgheight);
			img_front = (BufferedImage) e.opts.createImage(imgwidth, imgheight);
			imgData = ((DataBufferInt)img_back.getRaster().getDataBuffer()).getData();
			depth_buf = new int[imgData.length];
		}
		
		e.controls.resetZoom();
		
		if (e.controls.update3dmode) {
			set3Dmode();
			e.controls.update3dmode = false;
		}
	}
	
	public void create3dCanvas() {
		renderer_left_eye = new Renderer3D(e);
		renderer_left_eye.isMainCanvas = true;
		renderer_right_eye = new Renderer3D(e);
	}
	
	public void resetChargeDots() {
		t_prev = e.time;
		
		dots.clear();
		for (int i = 0; i < numdots; i++) {
			dots.add(new Dot(0, 0, 0, 0, 0));
		}
		
		ccdots.clear();
		C_prev = 0;
	}
	
	public void set3Dmode() {
		e.rwLock.writeLock().lock();
		try {
			renderer_left_eye.animator.pause();
			renderer_right_eye.animator.pause();
			
			imgpanel.remove(e.canvas);
			imgpanel.remove(renderer_left_eye.canvas);
			imgpanel.remove(renderer_right_eye.canvas);

			e.opts.gui_slice.setVisible(!threeD_mode);
			e.opts.gui_slicelabel.setVisible(!threeD_mode);
			
			int tmp = getSlice();
			e.opts.gui_slice.setValue(0);
			e.opts.gui_slice.setValue(1);
			e.opts.gui_slice.setValue(tmp);

			RenderMode mode = e.controls.rendermode.getOption();
			if (threeD_mode) {
				imgpanel.add(renderer_left_eye.canvas);
				renderer_left_eye.animator.resume();
				if (!renderer_left_eye.animator.isStarted()) renderer_left_eye.animator.start();

				if (RenderMode.isStereoscopic(mode)) {
					e.opts.gui_parallax.setVisible(true);
					e.opts.gui_parallaxlabel.setVisible(true);
					renderer_right_eye.animator.resume();
					if (!renderer_right_eye.animator.isStarted()) renderer_right_eye.animator.start();
					imgpanel.add(renderer_right_eye.canvas);
				} else {
					e.opts.gui_parallax.setVisible(false);
					e.opts.gui_parallaxlabel.setVisible(false);
				}

				updateParallax();
			} else {
				imgpanel.add(e.canvas);
			}
		} finally {
			e.rwLock.writeLock().unlock();
		}
	}
	
	public void updateParallax() {
		if (RenderMode.isStereoscopic(e.controls.rendermode.getOption())) {
			if (e.controls.rendermode.getOption() == RenderMode.THREED_STEREO) {
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
	
	public void setalphaBG(double alpha) {
		alphaBG = (float)alpha;
	}

	public void setalphaFG(double alpha) {
		alphaFG = (float)alpha;
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
	
	// Wavelength to RGB
	double[] lambda_r = {0.23822, 0.24973, 0.26128, 0.26553, 0.26476, 0.2536, 0.23768, 0.22023, 0.20389, 0.18888, 0.169, 0.12949, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.19944, 0.4376, 0.58059, 0.68286, 0.76516, 0.82701, 0.88226, 0.95151, 1.0112, 1.0442, 1.0729, 1.1021, 1.1058, 1.086, 1.0531, 1.0138, 0.95956, 0.89531, 0.83074, 0.76591, 0.69323};
	double[] lambda_g = {0.19971, 0.17932, 0.14919, 0.12037, 0.093617, 0.09458, 0.10426, 0.11275, 0.11462, 0.11727, 0.12455, 0.14908, 0.1933, 0.25787, 0.32737, 0.38708, 0.44274, 0.49603, 0.54306, 0.59225, 0.64252, 0.69595, 0.74892, 0.79799, 0.84018, 0.86705, 0.88432, 0.89322, 0.89768, 0.89173, 0.87677, 0.86048, 0.83734, 0.81313, 0.78437, 0.75141, 0.70945, 0.65984, 0.60119, 0.54323, 0.47406, 0.38514, 0.29591, 0.21448, 0.12878, 0, 0, 0, 0, 0, 0.064417};
	double[] lambda_b = {0.36184, 0.45077, 0.54787, 0.62678, 0.69213, 0.72851, 0.7625, 0.8006, 0.84243, 0.87363, 0.90106, 0.91545, 0.92739, 0.92995, 0.90334, 0.85833, 0.79228, 0.6955, 0.58576, 0.4834, 0.39609, 0.31148, 0.22847, 0.14139, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.062112, 0.10835, 0.13623, 0.15637, 0.17185, 0.18456, 0.19388, 0.20082, 0.20595, 0.20985};

	public void setColorWavelength(double lambda, double intensity) {
		col_r = (float)(interp(lambda_r, (lambda-400)/5)*intensity);
		col_g = (float)(interp(lambda_g, (lambda-400)/5)*intensity);
		col_b = (float)(interp(lambda_b, (lambda-400)/5)*intensity);
	}
	
	public double interp(double[] y, double x) {
		int i = (int) x;
		double f = x - i;
		if (i < 0 || x < 0)
			return y[0];
		if (i >= y.length-1)
			return y[y.length-1];
		return (1-f)*y[i] + f*y[i+1];
	}

	public void setPixel(int i, int j, int k) {
		if (i < 0 || j < 0  || k < 0 || i >= e.nx || j >= e.ny || k >= e.nz)
			return;

		image_r[i][j][k] = (float)(image_r[i][j][k]*alphaBG + col_r*alphaFG);
		image_g[i][j][k] = (float)(image_g[i][j][k]*alphaBG + col_g*alphaFG);
		image_b[i][j][k] = (float)(image_b[i][j][k]*alphaBG + col_b*alphaFG);
	}
	
	public void setPixelTranslucent(int i, int j, int k) {
		if (i < 0 || j < 0  || k < 0 || i >= e.nx || j >= e.ny || k >= e.nz)
			return;

		image_r[i][j][k] = (float)(image_r[i][j][k]*alphaBG + col_r*alphaFG);
		image_g[i][j][k] = (float)(image_g[i][j][k]*alphaBG + col_g*alphaFG);
		image_b[i][j][k] = (float)(image_b[i][j][k]*alphaBG + col_b*alphaFG);
		
		translucent[i][j][k] |= true;
	}
	
	public boolean inBounds(int i, int j, int k) {
		return !(i < 0 || j < 0  || k < 0 || i >= e.nx || j >= e.ny || k >= e.nz);
	}

	public void drawPixelLine(int x0, int y0, int z0, int x1, int y1, int z1) {
		int dx = Math.abs(x1-x0), sx = x0<x1 ? 1 : -1;
		int dy = Math.abs(y1-y0), sy = y0<y1 ? 1 : -1;
		int dz = Math.abs(z1-z0), sz = z0<z1 ? 1 : -1;
		int dm = Math.max(Math.max(dx,dy),dz), i = dm;
		x1 = y1 = z1 = dm/2;

		while (true) {
			setPixel(x0,y0,z0);
			if (inBounds(x0, y0, z0))
				translucent[x0][y0][z0] |= true;
			if (i == 0) break;
			i--;
			x1 -= dx; if (x1 < 0) { x1 += dm; x0 += sx; }
			y1 -= dy; if (y1 < 0) { y1 += dm; y0 += sy; }
			z1 -= dz; if (z1 < 0) { z1 += dm; z0 += sz; }
		}
	}

	public void drawPixelRectangle(int x, int y, int z, int wx, int wy, int wz) {
		for (int i = x; i < x+wx; i++) {
			for (int j = y; j < y+wy; j++) {
				for (int k = z; k < z+wz; k++) {
					setPixel(i, j, k);
					if (inBounds(i, j, k))
						translucent[i][j][k] |= true;
				}
			}
		}
	}

	public void drawPixel(int x, int y) {
		drawPixel(x, y, col_r, col_g, col_b, alphaFG, alphaBG);
	}

	public void drawPixel(int x, int y, float col_r, float col_g, float col_b, float alphaFG, float alphaBG) {
		y = imgheight - 1 - y;
		int rgb =  imgData[x+y*imgwidth];
		int r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
		int g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
		int b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
		if (r > 255)
			r = 255;
		if (g > 255)
			g = 255;
		if (b > 255)
			b = 255;
		imgData[x+y*imgwidth] = 255<<24 | r << 16 | g << 8 | b;
	}

	public void drawPixelWithContrast(int x, int y, float col_r, float col_g, float col_b, float alphaFG, float alphaBG) {
		y = imgheight - 1 - y;
		int rgb =  imgData[x+y*imgwidth];
		if (((rgb>>16)&255) + ((rgb>>8)&255) + ((rgb)&255) > 512) {
			alphaBG = 1-alphaFG;
			alphaFG = 0;
		}

		int r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
		int g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
		int b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
		if (r > 255)
			r = 255;
		if (g > 255)
			g = 255;
		if (b > 255)
			b = 255;
		imgData[x+y*imgwidth] = 255<<24 | r << 16 | g << 8 | b;
	}

	public void drawRectangle(int x, int y, int w, int h) {
		drawRectangle(x, y, w, h, col_r, col_g, col_b, alphaFG, alphaBG);
	}

	public void drawRectangle(int x, int y, int w, int h, float col_r, float col_g, float col_b, float alphaFG, float alphaBG) {
		y = imgheight - y - h;

		if (x >= 0 && y >= 0 && x+w < imgwidth && y+h < imgheight) {

			for (int i = x; i < x+w; i++) {
				for (int j = y; j < y+h; j++) {
					int rgb =  imgData[i+j*imgwidth];
					int r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
					int g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
					int b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
					if (r > 255)
						r = 255;
					if (g > 255)
						g = 255;
					if (b > 255)
						b = 255;
					imgData[i+j*imgwidth] = 255<<24 | r << 16 | g << 8 | b;
				}
			}
		}
	}
	
	public void drawRectangle(int x, int y, int w, int h, float col_r, float col_g, float col_b, float alphaFG, float alphaBG, int depth) {
		y = imgheight - y - h;

		if (x >= 0 && y >= 0 && x+w < imgwidth && y+h < imgheight) {

			for (int i = x; i < x+w; i++) {
				for (int j = y; j < y+h; j++) {
					int rgb =  imgData[i+j*imgwidth];
					int r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
					int g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
					int b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
					if (r > 255)
						r = 255;
					if (g > 255)
						g = 255;
					if (b > 255)
						b = 255;
					if (depth >= depth_buf[i+j*imgwidth]) {
						imgData[i+j*imgwidth] = 255<<24 | r << 16 | g << 8 | b;
						depth_buf[i+j*imgwidth] = depth;
					}
				}
			}
		}
	}

	public void drawLine(int x0, int y0, int x1, int y1, boolean draw_starting_point) {
		drawLine(x0, y0, x1, y1, draw_starting_point, col_r, col_g, col_b, alphaFG, alphaBG);
	}

	public void drawLine(int x0, int y0, int x1, int y1, boolean draw_starting_point, float col_r, float col_g, float col_b, float alphaFG, float alphaBG) {
		y0 = imgheight - 1 - y0;
		y1 = imgheight - 1 - y1;
		try {
			int dy = y1 - y0;
			int dx = x1 - x0;
			float t = (float) 0.5;

			int rgb;
			int r;
			int g;
			int b;

			if (draw_starting_point) {
				rgb = imgData[x0+y0*imgwidth];
				r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
				g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
				b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
				if (r > 255)
					r = 255;
				if (g > 255)
					g = 255;
				if (b > 255)
					b = 255;
				imgData[x0+y0*imgwidth] =  255<<24 | r << 16 | g << 8 | b;
			}


			if (Math.abs(dx) > Math.abs(dy)) {
				float m = (float) dy / (float) dx;
				t += y0;
				dx = (dx < 0) ? -1 : 1;
				m *= dx;
				while (x0 != x1) {
					x0 += dx;
					t += m;

					rgb =  imgData[x0+((int)t)*imgwidth];
					r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
					g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
					b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
					if (r > 255)
						r = 255;
					if (g > 255)
						g = 255;
					if (b > 255)
						b = 255;
					imgData[x0+((int)t)*imgwidth] = 255<<24 | r << 16 | g << 8 | b;

				}
			} else {
				float m = (float) dx / (float) dy;
				t += x0;
				dy = (dy < 0) ? -1 : 1;
				m *= dy;
				while (y0 != y1) {
					y0 += dy;
					t += m;

					rgb =  imgData[((int)t)+y0*imgwidth];
					r = ((int)(((rgb>>16)&255)*alphaBG + 255*col_r*alphaFG));
					g = ((int)(((rgb>>8)&255)*alphaBG + 255*col_g*alphaFG));
					b = ((int)(((rgb)&255)*alphaBG + 255*col_b*alphaFG));
					if (r > 255)
						r = 255;
					if (g > 255)
						g = 255;
					if (b > 255)
						b = 255;
					imgData[((int)t)+y0*imgwidth] = 255<<24 | r << 16 | g << 8 | b;
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return;
		}
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

	public double bump(double x, double w) {
		if (x <= 0 || x >= 1) return 0;
		if (x <= (1-w) && x >= w) return 1;

		double y = x/w;
		if (x > (1-w))
			y = (1.0-x)/w;

		return 3*y*y - 2*y*y*y;
	}

	
	@Override
	public void run() {

		if (!this.threeD_mode) {
			e.rwLock.readLock().lock();
			try {
				t5.start();
				drawPixels();
				drawOverlay(true);
				//drawText();
				//Graphics2D g = img_back.createGraphics();
				//drawText(g);
				copyImage(img_back, img_front);
				e.canvas.repaint();
				t5.stop();

				FPStimer.stop();
				FPStimer.start();
			} catch (Exception e1) {
				SemiSim.displayErrorMessage(e1);
			}
			finally {
				e.rwLock.readLock().unlock();
			}
		}

        SemiSim.instance.threadPool.schedule(this, nextDelay(frameduration), TimeUnit.MILLISECONDS);
	}
	
	BufferedImage copyImage(BufferedImage src, BufferedImage dst) {
	    if (src.getType() != dst.getType() || 
	        src.getWidth() != dst.getWidth() || 
	        src.getHeight() != dst.getHeight()) {
	        throw new IllegalArgumentException("Images must be same size and type");
	    }

	    DataBuffer srcBuffer = src.getRaster().getDataBuffer();
	    DataBuffer dstBuffer = dst.getRaster().getDataBuffer();

	    if (srcBuffer instanceof DataBufferInt && dstBuffer instanceof DataBufferInt) {
	        int[] srcData = ((DataBufferInt) srcBuffer).getData();
	        int[] dstData = ((DataBufferInt) dstBuffer).getData();
	        System.arraycopy(srcData, 0, dstData, 0, srcData.length);
	    } else {
	        // Fallback if not int-packed
	        int[] temp = src.getRaster().getPixels(0, 0, src.getWidth(), src.getHeight(), (int[]) null);
	        dst.getRaster().setPixels(0, 0, dst.getWidth(), dst.getHeight(), temp);
	    }
	    return dst;
	}

	void drawPixels() {

		for (int i = 0; i < e.nx; i++) {
			for (int j = 0; j < e.ny; j++) {
				for (int k = 0; k < e.nz; k++) {
					image_r[i][j][k] = 0;
					image_g[i][j][k] = 0;
					image_b[i][j][k] = 0;
					opaque[i][j][k] = false;
					translucent[i][j][k] = false;
					solid[i][j][k] = false;
				}
			}
		}

		setalphaBG(0);
		setalphaFG(1);

		Brush brush = (Brush) e.opts.gui_brush.getSelectedItem();
		//boolean showborders = e.opts.menu_borders.isSelected();

		if (e.opts.menu_elem_colors.isSelected()) {
			for (int i = 0; i < e.nx; i++) {
				for (int j = 0; j < e.ny; j++) {
					for (int k = 0; k < e.nz; k++) {
						setColor(e.materials[i][j][k].type.color_r, e.materials[i][j][k].type.color_g, e.materials[i][j][k].type.color_b);

						double alpha = 1.0;
						/*if (showborders && i > 0 && j > 0 && i < e.nx-1 && j < e.ny-1) {
							if (e.materials[i+1][j].type != e.materials[i][j][k].type || e.materials[i][j+1].type != e.materials[i][j][k].type)
								alpha -= 0.1;
							if (e.materials[i-1][j].type != e.materials[i][j][k].type || e.materials[i][j-1].type != e.materials[i][j][k].type)
								alpha += 0.1;
						}*/

						setalphaFG(alpha);

						setPixel(i, j, k);
						opaque[i][j][k] = e.materials[i][j][k].type != MaterialType.VACUUM;
						solid[i][j][k] |= e.materials[i][j][k].type != MaterialType.VACUUM;
					}
				}
			}
		} else {
			for (int i = 0; i < e.nx; i++) {
				for (int j = 0; j < e.ny; j++) {
					for (int k = 0; k < e.nz; k++) {
						setColor(e.materials[i][j][k].type.color_grayscale, e.materials[i][j][k].type.color_grayscale, e.materials[i][j][k].type.color_grayscale);

						double alpha = 1.0;
						/*if (showborders && i > 0 && j > 0 && i < e.nx-1 && j < e.ny-1) {
							if (e.materials[i+1][j].type != e.materials[i][j][k].type || e.materials[i][j+1].type != e.materials[i][j][k].type)
								alpha -= 0.1;
							if (e.materials[i-1][j].type != e.materials[i][j][k].type || e.materials[i][j-1].type != e.materials[i][j][k].type)
								alpha += 0.1;
						}*/

						setalphaFG(alpha);

						setPixel(i, j, k);
						opaque[i][j][k] = e.materials[i][j][k].type != MaterialType.VACUUM;
						solid[i][j][k] |= e.materials[i][j][k].type != MaterialType.VACUUM;
					}
				}
			}
		}

		if (e.controls.moving_selection) {
			int offset = e.controls.dragging_selection? 0 : 1;
			if (e.opts.menu_elem_colors.isSelected()) {
				for (int i = 0; i < e.nx; i++) {
					for (int j = 0; j < e.ny; j++) {
						for (int k = 0; k < e.nz; k++) {
							int si = i-e.controls.delta_mx + offset*e.controls.selection.i_max/2;
							int sj = j-e.controls.delta_my + offset*e.controls.selection.j_max/2;
							int sk = k-e.controls.delta_mz + offset*e.controls.selection.k_max/2;
							if (si >= 0 && sj >= 0 && sk >= 0 && si < e.nx && sj < e.ny && sk < e.nz && e.controls.selection.mat[si][sj][sk].m.type != MaterialType.VACUUM) {
								setColor(e.controls.selection.mat[si][sj][sk].m.type.color_r, e.controls.selection.mat[si][sj][sk].m.type.color_g, e.controls.selection.mat[si][sj][sk].m.type.color_b);
								setPixel(i, j, k);
								translucent[i][j][k] = true;
							}
						}
					}
				}
			} else {
				for (int i = 0; i < e.nx; i++) {
					for (int j = 0; j < e.ny; j++) {
						for (int k = 0; k < e.nz; k++) {
							int si = i-e.controls.delta_mx + offset*e.controls.selection.i_max/2;
							int sj = j-e.controls.delta_my + offset*e.controls.selection.j_max/2;
							int sk = k-e.controls.delta_mz + offset*e.controls.selection.k_max/2;
							if (si >= 0 && sj >= 0 && sk >= 0 && si < e.nx && sj < e.ny && sk < e.nz && e.controls.selection.mat[si][sj][sk].m.type != MaterialType.VACUUM) {
								setColor(e.controls.selection.mat[si][sj][sk].m.type.color_grayscale, e.controls.selection.mat[si][sj][sk].m.type.color_grayscale, e.controls.selection.mat[si][sj][sk].m.type.color_grayscale);
								setPixel(i, j, k);
								translucent[i][j][k] = true;
							}
						}
					}
				}
			}
		}

		ScalarView scalarview = e.controls.scalarview.getOption();
		ScalarMode scalarmode = e.controls.scalarmode.getOption();

		scalingconstant = (float) (10*Math.pow(10.0, e.opts.gui_brightness.getValue()/10.0)/scalarview.getScalingConstant(e));
		scalar_offset = 0;

		if (scalarview != ScalarView.NONE && scalarmode != ScalarMode.NONE) {
			switch (scalarview) {
			case ELECTRON_POTENTIAL:
			case HOLE_POTENTIAL:
				scalar_offset = -(float) e.global_voltage_offset;
				break;
			case ELECTRON_VOLTAGE:
			case HOLE_VOLTAGE:
				scalar_offset = (float) e.global_voltage_offset;
				break;
			default:
				break;
			}

			setalphaBG(1.0);
			setalphaFG(1.0);

			e.computeScalarField(scalarfield, 0, 0, 0, scalarview);

			if (e.hasGround() && display_relative_voltage) {
				Ground ground = e.getGround();

				if (scalarview == ScalarView.ELECTRON_VOLTAGE || scalarview == ScalarView.HOLE_VOLTAGE || scalarview == ScalarView.AVERAGE_POTENTIAL) {
					scalar_offset = (float) scalarfield[ground.x][ground.y][ground.z];
				}

				if (!Double.isFinite(scalar_offset))
					scalar_offset = 0;
			}

			ScalarView.ColorScheme colorscheme = scalarview.colorscheme;

			if (e.opts.menu_colormap.isSelected())
			{
				int i1 = (int) (e.nx*0.85);
				int i2 = (int) (e.nx*0.92);

				int j1 = (int) (e.ny*0.82);
				int j2 = (int) (e.ny*0.92);

				int k1 = (int) (e.nz*0.82);
				int k2 = (int) (e.nz*0.92);


				for (int i = i1; i <= i2; i++) {
					for (int j = j1; j <= j2; j++) {
						for (int k = k1; k <= k2; k++) {
							double t = 0;
							if (colorscheme == ScalarView.ColorScheme.RED_BLUE || colorscheme == ScalarView.ColorScheme.CYAN_YELLOW || scalarview == ScalarView.CHARGE) {
								t = 2*((j2-j)/(double)(j2-j1)-0.5);
								scalarfield[i][j][k] = t/scalingconstant + scalar_offset;
							} else if (colorscheme == ScalarView.ColorScheme.WHITE || colorscheme == ScalarView.ColorScheme.GREEN) {
								t = (j2-j)/(double)(j2-j1);
								scalarfield[i][j][k] = t/scalingconstant + scalar_offset;
							} else if (scalarview == ScalarView.LIGHT) {
								t = (j2-j)/(double)(j2-j1);
								scalarfield[i][j][k] = -(400+(650-400)*t)*1e50;
							}
						}
					}
				}
			}
		}

		if (scalarmode == ScalarMode.CONTOUR_COLORS || scalarmode == ScalarMode.CONTOUR) {
			for (int i = 1; i < e.nx-1; i++) {
				for (int j = 1; j < e.ny-1; j++) {
					for (int k = 1; k < e.nz-1; k++) {
						double gx = scalarfield[i+1][j][k]-scalarfield[i-1][j][k];
						double gy = scalarfield[i][j+1][k]-scalarfield[i][j-1][k];
						double gz = scalarfield[i][j][k+1]-scalarfield[i][j][k-1];
						gradscalarfield[i][j][k] = Utils.length((Double.isFinite(gx))? gx : 0, (Double.isFinite(gy))? gy : 0, (Double.isFinite(gz))? gz : 0)/(2*e.ds);
					}
				}
			}
		}


		if (scalarmode == ScalarMode.COLORS || scalarmode == ScalarMode.CONTOUR_COLORS) {
			ScalarView.ColorScheme colorscheme = e.controls.scalarview.getOption().colorscheme;

			if (colorscheme == ScalarView.ColorScheme.RED_BLUE) {
				for (int i = 1; i < e.nx-1; i++) {
					for (int j = 1; j < e.ny-1; j++) {
						for (int k = 1; k < e.nz-1; k++) {
							float v = (float) (scalarfield[i][j][k]-scalar_offset)*scalingconstant;
							setColorFloat(v, 0, -v);
							setPixel(i, j, k);
						}
					}
				}
			} else if (colorscheme == ScalarView.ColorScheme.CYAN_YELLOW) {
				for (int i = 1; i < e.nx-1; i++) {
					for (int j = 1; j < e.ny-1; j++) {
						for (int k = 1; k < e.nz-1; k++) {
							float v = (float) (scalarfield[i][j][k]-scalar_offset)*scalingconstant;
							setColorFloat(v, Math.abs(v), -v);
							setPixel(i, j, k);
						}
					}
				}
			} else if (colorscheme == ScalarView.ColorScheme.GREEN) {
				for (int i = 1; i < e.nx-1; i++) {
					for (int j = 1; j < e.ny-1; j++) {
						for (int k = 1; k < e.nz-1; k++) {
							float v = (float) (scalarfield[i][j][k]-scalar_offset)*scalingconstant;
							setColorFloat(0, Math.abs(v), 0);
							setPixel(i, j, k);
						}
					}
				}
			} else if (colorscheme == ScalarView.ColorScheme.WHITE) {
				for (int i = 1; i < e.nx-1; i++) {
					for (int j = 1; j < e.ny-1; j++) {
						for (int k = 1; k < e.nz-1; k++) {
							float v = (float) (scalarfield[i][j][k]-scalar_offset)*scalingconstant;
							setColorFloat(v, v, v);
							setPixel(i, j, k);
						}
					}
				}
			} else if (colorscheme == ScalarView.ColorScheme.OTHER) {
				if (scalarview == ScalarView.COMBINED_CHARGE) {
					for (int i = 1; i < e.nx-1; i++) {
						for (int j = 1; j < e.ny-1; j++) {
							for (int k = 1; k < e.nz-1; k++) {
								float rc = (float)(clamp(0.2*Math.log(e.rho_p[i][j][k]*scalingconstant), 0, 1));
								float bc = (float)(clamp(0.2*Math.log(-e.rho_n[i][j][k]*scalingconstant), 0, 1));
								float gc = Math.min(rc, bc);
								double it = Math.max(rc, bc);
								setalphaBG(1-0.5*gc);
								setalphaFG(0.6*it);
								setColorFloat(rc, gc, bc);
								setPixel(i, j, k);
							}
						}
					}
				}  else if (scalarview == ScalarView.CHARGE) {
					for (int i = 1; i < e.nx-1; i++) {
						for (int j = 1; j < e.ny-1; j++) {
							for (int k = 1; k < e.nz-1; k++) {
								float rc = Math.min(Math.max((float) scalarfield[i][j][k]*scalingconstant, 0), 1);
								float bc = Math.min(Math.max(-(float) scalarfield[i][j][k]*scalingconstant, 0), 1);
								float gc = Math.min(rc, bc);

								setalphaBG(1-0.5*gc);
								setColorFloat(rc, gc, bc);
								setPixel(i, j, k);
							}
						}
					}
				} else if (scalarview == ScalarView.LIGHT) {
					for (int i = 1; i < e.nx-1; i++) {
						for (int j = 1; j < e.ny-1; j++) {
							for (int k = 1; k < e.nz-1; k++) {
								if (scalarfield[i][j][k] < 0) {
									setColorWavelength(-scalarfield[i][j][k]/1e50, 1); //The color scale
									setPixel(i, j, k);
								} else if (scalarfield[i][j][k] > 0) {
									double hc = 1.986e-25;
									double Emax = (e.materials[i][j][k].Ec - e.materials[i][j][k].Ev) + 0.5/e.beta; // Peak emission energy
									double lambda_nm = 1e9*hc/Emax;
									setColorWavelength(lambda_nm, scalarfield[i][j][k]*scalingconstant);
									setPixel(i, j, k);
								}
							}
						}
					}
				}
			}
		}

		boolean showbrush = Brush.isBrushShapeImportant(brush);
		boolean highlight = e.opts.gui_brush_highlight.isSelected();
		for (int i = 0; i < e.nx; i++) {
			for (int j = 0; j < e.ny; j++) {
				for (int k = 0; k < e.nz; k++) {
					MaterialType type = e.materials[i][j][k].type;
					if (type.isInteractable() && i > 0 && j > 0 && k > 0 && i < e.nx-1 && j < e.ny-1 && k < e.nz-1) {
						//int ci = 0;
						//int cj = 0;
						int shading = 2*((i+j+k)%2)+1;

						setalphaBG(0.25);
						setalphaFG(0.75);

						int offset = 10*shading;
						if (e.controls.selected_EMF[i][j][k])
							offset = 50*shading-30;

						/*if (e.materials[i+1][j][k].type != type || e.materials[i-1][j][k].type != type || e.materials[i][j+1][k].type != type || e.materials[i][j-1][k].type != type || e.materials[i][j][k+1].type != type || e.materials[i][j][k-1].type != type)
						{
							offset = -30;
							if (e.materials[i][j][k].activated == 0) {
								offset -= 100;
							}
						} else*/
						if (e.materials[i][j][k].activated == 0) {
							offset -= 200;
						}


						int delta_r = type.color_r+offset;
						int delta_g = type.color_g+offset;
						int delta_b = type.color_b+offset;
						setColor(delta_r, delta_g, delta_b);

						setPixel(i, j, k);
					}

					translucent[i][j][k] |= e.controls.selected[i][j][k];
					
					if (e.controls.selected[i][j][k] || (showbrush && highlight && e.controls.under_brush[i][j][k]))
					{
						setalphaBG(0.75);
						setalphaFG(0.25);

						int s = e.controls.selected[i][j][k]? 1:0;
						int h = (showbrush && highlight && e.controls.under_brush[i][j][k])? 1:0;
						int delta_r = 256*s + 256*h;
						int delta_g = 100*s + 256*h;
						int delta_b = 256*s + 256*h;

						setColor(delta_r, delta_g, delta_b);

						setPixel(i, j, k);
					}

					if (showbrush && !highlight && e.controls.under_brush[i][j][k]) {
						if (i > 0 && j > 0 && k > 0 && i < e.nx-1 && j < e.ny-1 && k < e.nz-1 &&
							!(e.controls.under_brush[i-1][j][k] && e.controls.under_brush[i+1][j][k] && e.controls.under_brush[i][j-1][k] && e.controls.under_brush[i][j+1][k] && e.controls.under_brush[i][j][k-1] && e.controls.under_brush[i][j][k+1]))
						{
							setalphaBG(0.25);
							setalphaFG(0.75);

							if ((image_r[i][j][k] + image_g[i][j][k] + image_b[i][j][k])/3.0 < 0.75)
								setColor(256, 256, 256);
							else
								setColor(50, 50, 50);
							setPixel(i, j, k);
						}
					}
					
					if (showbrush) {
						translucent[i][j][k] |= e.controls.under_brush[i][j][k];
					}

					if (e.L[i][j][k] > 0)
					{
						setalphaBG(0.5);
						setalphaFG(0.75);
						setColorFloat(1, 1, 1);
						setPixel(i, j, k);
					}
				}
			}
		}


		setalphaBG(1);
		setalphaFG(1);
		setColorFloat(0.7f, 0.7f, 0.7f);

		if (Brush.drawLine(brush) && (e.controls.mouse_pressed_prev_left || e.controls.mouse_pressed_prev_right)) {
			drawPixelLine(e.controls.mx_start, e.controls.my_start, e.controls.mz_start, e.controls.mx, e.controls.my, e.controls.mz);
		}

		setalphaFG(0.5);
		if ((brush == Brush.ZOOM && e.controls.mouse_pressed_prev_left
				|| brush == Brush.RECTANGLE && (e.controls.mouse_pressed_prev_left || e.controls.mouse_pressed_prev_right))) {
			

			int mx0 = Math.min(e.controls.mx_start, e.controls.mx);
			int my0 = Math.min(e.controls.my_start, e.controls.my);
			int mz0 = Math.min(e.controls.mz_start, e.controls.mz);
			int mx1 = Math.max(e.controls.mx_start, e.controls.mx);
			int my1 = Math.max(e.controls.my_start, e.controls.my);
			int mz1 = Math.max(e.controls.mz_start, e.controls.mz);

			for (int i = mx0; i <= mx1; i++)
			{
				for (int j = my0; j <= my1; j++)
				{
					for (int k = mz0; k <= mz1; k++)
					{
						setPixelTranslucent(i, j, k);
					}
				}
			}
		}

		if (brush == Brush.RECTANGLE) {
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz, e.controls.mx+3, e.controls.my, e.controls.mz);
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz, e.controls.mx-3, e.controls.my, e.controls.mz);
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz, e.controls.mx, e.controls.my+3, e.controls.mz);
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz, e.controls.mx, e.controls.my-3, e.controls.mz);
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz, e.controls.mx, e.controls.my, e.controls.mz+3);
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz, e.controls.mx, e.controls.my, e.controls.mz-3);
		}


		if (drawCrosshairGuides) {
			drawPixelLine(e.controls.mx-4, e.controls.my, e.controls.mz, 0, e.controls.my, e.controls.mz);
			drawPixelLine(e.controls.mx+4, e.controls.my, e.controls.mz, e.nx-1, e.controls.my, e.controls.mz);
			drawPixelLine(e.controls.mx, e.controls.my-4, e.controls.mz, e.controls.mx, 0, e.controls.mz);
			drawPixelLine(e.controls.mx, e.controls.my+4, e.controls.mz, e.controls.mx, e.ny-1, e.controls.mz);
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz-4, e.controls.mx, e.controls.my, 0);
			drawPixelLine(e.controls.mx, e.controls.my, e.controls.mz+4, e.controls.mx, e.controls.my, e.nz-1);
			setPixelTranslucent(e.controls.mx, e.controls.my, e.controls.mz);
		}
		setalphaFG(1);


		if (e.opts.menu_interface.isSelected())
		{
			if (e.opts.menu_probes.isSelected())
			{
				for (Probe p0 : e.probes) {
					p0.draw(this);
				}

				if (e.controls.moving_selection) {
					int offset = e.controls.dragging_selection? 0 : 1;	
					for (Probe p0 : e.controls.selection.probes) {		
						p0.translate(e.controls.delta_mx-offset*e.controls.selection.i_max/2, e.controls.delta_my-offset*e.controls.selection.j_max/2, e.controls.delta_mz-offset*e.controls.selection.k_max/2);
						p0.draw(this);
						p0.translate(-e.controls.delta_mx+offset*e.controls.selection.i_max/2, -e.controls.delta_my+offset*e.controls.selection.j_max/2, -e.controls.delta_mz+offset*e.controls.selection.k_max/2);
					}
				}
			}

			for (Plot p: e.plots) {
				if (p.frame.isVisible()) {
					if (p.path != null)
						p.path.draw(this);
				}
			}

			if (e.controls.plotpath != null)
				e.controls.plotpath.draw(this);
		}

		setalphaFG(0.8);
		setColorFloat(1.0f, 1.0f, 1.0f);

		if (e.controls.texting && (System.currentTimeMillis() % 1000) < 500) {
			int i = 0;
			for (int j = 0; j < 7; j++) {
				int scx = e.controls.text_x+i;
				int scy = e.controls.text_y+j;
				int scz = e.controls.text_z;

				int fi = e.renderer.embed_x(scx, scy, scz);
				int fj = e.renderer.embed_y(scx, scy, scz);
				int fk = e.renderer.embed_z(scx, scy, scz);

				setPixel(fi, fj, fk);
			}
		}
	}
	
	synchronized void drawOverlay(boolean render) {
		delta_t = e.time - t_prev;
		t_prev = e.time;

		synchronized_show_carriers = e.opts.gui_carriers.isSelected();
		synchronized_update_carriers = !e.opts.gui_paused.isSelected() || delta_t > 0;
		synchronized_vector_display_mode = e.controls.vectormode.getOption();
		synchronized_scalar_display_mode = e.controls.scalarmode.getOption();
		synchronized_scalar_view = e.controls.scalarview.getOption();
		synchronized_vector_view = e.controls.vectorview.getOption();
		synchronized_render = render;
		
		try {
			if (SemiSim.instance.graphics_threads.size() == SemiSim.n_threads) {
				graphics_start_barrier.await();
				graphics_end_barrier.await();
			}
		} catch (InterruptedException | BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
	
	void drawText(Graphics2D g) {
		clearStrings();

		//Graphics2D g = (Graphics2D) img_back.getGraphics();
		if (g != null) {
			g.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}

		if (e.opts.menu_probes.isSelected()) {
			for (Probe p: e.probes) {
				Text text = draw3dString(p.getText(e.units), p.labelcoord.x, p.labelcoord.y, p.labelcoord.z);
				text.isMonospaced = true;
				text.isHorizontalCentered = true;
				text.isVerticalCentered = true;
			}

			drawStrings(g);
			if (g != null) startNewStringLayer();
		}
		
		/*ScalarView scalarview = e.controls.scalarview.getOption();
		ScalarMode scalarmode = e.controls.scalarmode.getOption();
		if (scalarview != ScalarView.NONE && scalarmode != ScalarMode.NONE) {
			ScalarView.ColorScheme colorscheme = scalarview.colorscheme;

			if (e.opts.menu_colormap.isSelected())
			{
				int i1 = (int) (e.nx*0.85);
				int i2 = (int) (e.nx*0.92);

				int j1 = (int) (e.ny*0.81);
				int j2 = (int) (e.ny*0.93);

				double tmin = 0;
				double tmax = 1;
				
				if (!(colorscheme == ScalarView.ColorScheme.GREEN || colorscheme == ScalarView.ColorScheme.WHITE))
				{
					tmin = 2*(tmin-0.5);
					tmax = 2*(tmax-0.5);
				}
				if (scalarview == ScalarView.LIGHT) {
					Text text = drawStringSimCoords("≥ " + e.units.toString(400e-9, Quantity.LENGTH), (i1+i2)/2, j2, g);
					text.isMonospaced = true;
					text.isHorizontalCentered = true;
					text = drawStringSimCoords("≤ " + e.units.toString(650e-9, Quantity.LENGTH), (i1+i2)/2, j1, g);
					text.isMonospaced = true;
					text.isHorizontalCentered = true;
					text.isBottomJustified = true;
				} else {
					Text text = drawStringSimCoords(e.units.toString(tmin/scalingconstant+scalar_offset, scalarview.unit), (i1+i2)/2, j2, g);
					text.isMonospaced = true;
					text.isHorizontalCentered = true;
					text = drawStringSimCoords(e.units.toString(tmax/scalingconstant+scalar_offset, scalarview.unit), (i1+i2)/2, j1, g);
					text.isMonospaced = true;
					text.isHorizontalCentered = true;
					text.isBottomJustified = true;
				}
				//TODO
			}
		}*/

		{
			int mx = e.controls.mx;
			int my = e.controls.my;
			int mz = e.controls.mz;

			if (mx < 0)
				mx = 0;
			if (mx >= e.nx)
				mx = e.nx-1;
			if (my < 0)
				my = 0;
			if (my >= e.ny)
				my = e.ny-1;
			if (mz < 0)
				mz = 0;
			if (mz >= e.nz)
				mz = e.nz-1;

			Material mat = e.materials[mx][my][mz];

			int vspacing = Text.fontsize;
			int voffset = 1 + (int)(e.controls.my_screen);
			int hoffset = 5 + (int)(e.controls.mx_screen)+15;
			//int voffset = 1 + (int)(e.controls.my*scalefactor);
			//int hoffset = 5 + (int)(e.controls.mx*scalefactor)+15;

			if (e.opts.menu_tooltip.isSelected()) {
				if (voffset + 22*vspacing > imgpanel.getHeight()) {
					voffset = voffset - ((voffset + 22*vspacing) - imgpanel.getHeight());
				}
				if (hoffset + 120 > imgpanel.getWidth()) {
					hoffset = hoffset - ((hoffset + 120) - imgpanel.getWidth());
				}
			}

			if (e.opts.menu_materialname.isSelected()) {
				if (disp_mat_name) {
					String name = "Material: " + mat.getDisplayName();
					Text text = drawString(name, hoffset, voffset);
					text.isBig = true;
				} else {
					String name = mat.getDisplayName();
					Text text = drawString(name, imgpanel.getWidth(), 0);
					text.isRightJustified = true;
				}
			}

			if (e.opts.menu_tooltip.isSelected()) {
				voffset = voffset+3;
				int line = 2;
				drawTwoColumnString("E" , 							e.units.toString(Utils.getFieldMagnitude(e.Ex, e.Ey, e.Ez, mx, my, mz), Quantity.ELECTRIC_FIELD, 1e-6), 				hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("B" , 							e.units.toString(Utils.getDualFieldMagnitude(e.Bx, e.By, e.Bz, mx, my, mz), Quantity.MAGNETIC_FLUX_DENSITY, 1e-9),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03d5" , 						e.units.toString(e.phi[mx][my][mz], Quantity.ELECTRIC_POTENTIAL, 1e-6),				hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u2130" , 						e.units.toString(mat.emf, Quantity.ELECTRIC_FIELD, 1e-6),					hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03b5/\u03b5\u2080" , 		e.units.toString(mat.eps_r, Quantity.DIMENSIONLESS),							hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03bc/\u03bc\u2080" , 		e.units.toString(mat.mu_r, Quantity.DIMENSIONLESS),							hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1\u2099" , 				e.units.toString(e.rho_n[mx][my][mz], Quantity.CHARGE_DENSITY, 1e-9),		hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1\u209A" , 				e.units.toString(e.rho_p[mx][my][mz], Quantity.CHARGE_DENSITY, 1e-9),		hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1\u2080",					e.units.toString(e.rho_back[mx][my][mz], Quantity.CHARGE_DENSITY, 1e-9),		hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1" , 						e.units.toString(e.rho_free[mx][my][mz], Quantity.CHARGE_DENSITY, 1e-9),		hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("J\u2099" ,						e.units.toString(Utils.getFieldMagnitude(e.Jx_n, e.Jy_n, e.Jz_n, mx, my, mz), Quantity.CURRENT_DENSITY, 1),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("J\u209A" , 					e.units.toString(Utils.getFieldMagnitude(e.Jx_p, e.Jy_p, e.Jz_p, mx, my, mz), Quantity.CURRENT_DENSITY, 1),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("J" , 							e.units.toString(Utils.getFieldMagnitude(e.Jx_free, e.Jy_free, e.Jz_free, mx, my, mz), Quantity.CURRENT_DENSITY, 1),		hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("F\u2099" , 					e.units.toString(-e.mu_n[mx][my][mz]/e.q_n, Quantity.ELECTRIC_POTENTIAL, 1e-9),						hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("F\u209a" , 					e.units.toString(-e.mu_p[mx][my][mz]/e.q_p, Quantity.ELECTRIC_POTENTIAL, 1e-9),						hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("V" , 							e.units.toString(e.V_avg[mx][my][mz], Quantity.ELECTRIC_POTENTIAL, 1e-9),				hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("x" , 							e.units.toString(mx*e.ds, Quantity.LENGTH),							hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("y" , 							e.units.toString(my*e.ds, Quantity.LENGTH),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("z" , 							e.units.toString(mz*e.ds, Quantity.LENGTH),			hoffset, voffset + line*vspacing); line++;
			}

			drawStrings(g);
			if (g != null) startNewStringLayer();
		}

		int vspacing = Text.fontsize+1;
		int voffset = 0;
		int hoffset = 6;
		int line = 0;
		
		if (e.opts.menu_time.isSelected()) {
			drawString("Time: " + e.units.toString(e.time, Quantity.TIME), hoffset, voffset + line*vspacing); line++;
			double pct = 100/(e.simFPStimer.getAverageTime()*e.renderer.targetframerate);
			drawString("Steps/s: " + e.units.toString(e.opts.gui_simspeed_2.getValue()/e.simFPStimer.getAverageTime(), Quantity.DIMENSIONLESS) + " (" + String.format("%.0f", pct) + "%)", hoffset, voffset + line*vspacing); line++;

			String sv_a = e.controls.scalarmode.getOption() != ScalarMode.NONE? (e.controls.scalarmode.getOption().shorthand + ": " + e.controls.scalarview.getOption().shorthand) : "";
			String sv_b = e.controls.vectormode.getOption() != VectorMode.NONE? (e.controls.vectormode.getOption().shorthand + ": " + e.controls.vectorview.getOption().shorthand) : "";
			String sv = Arrays.asList(sv_a, sv_b).stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(" / "));
			drawString(sv, hoffset, voffset + line*vspacing); line++;
			if (e.opts.gui_paused.isSelected())
			{
				drawString("Paused", hoffset, voffset + line*vspacing); line++;
			}
			if (e.sign_violation_timer > 0) {
				e.sign_violation_timer--;
				drawString("Warning: Negative density detected.", hoffset, voffset + line*vspacing); line++;
			}
			if (e.instability_timer > 0) {
				e.instability_timer--;
				drawString("Warning: Numerical instability detected.", hoffset, voffset + line*vspacing); line++;
			}
		}
		
		if (e.controls.debugging) {
			long total = Runtime.getRuntime().totalMemory();
			long used  = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			drawString("Used memory " + e.units.toString(used, Quantity.INFORMATION), hoffset, voffset + line*vspacing); line++;
			drawString("Total memory " + e.units.toString(total, Quantity.INFORMATION), hoffset, voffset + line*vspacing); line++;
			drawString(e.t4.getName() + " " + e.units.toString(e.t4.getAverageTime(), Quantity.TIME), hoffset, voffset + line*vspacing); line++;
			drawString(t5.getName() + " " + e.units.toString(t5.getAverageTime(), Quantity.TIME), hoffset, voffset + line*vspacing); line++;
			drawString(e.t6.getName() + " " + e.units.toString(e.t6.getAverageTime()*e.opts.gui_simspeed_2.getValue(), Quantity.TIME), hoffset, voffset + line*vspacing); line++;
			drawString(e.t7.getName() + " " + e.units.toString(e.t7.getAverageTime(), Quantity.TIME), hoffset, voffset + line*vspacing); line++;
			drawString(e.t8.getName() + " " + e.units.toString(e.t8.getAverageTime(), Quantity.TIME), hoffset, voffset + line*vspacing); line++;
			drawString(FPStimer.getName() + " " + e.units.toString(1/FPStimer.getAverageTime(), Quantity.FREQUENCY), hoffset, voffset + line*vspacing); line++;
			drawString(e.simFPStimer.getName() + " " + e.units.toString(1/e.simFPStimer.getAverageTime(), Quantity.FREQUENCY), hoffset, voffset + line*vspacing); line++;
		}
		
		if ((FPStimer.getAverageTime() > 0.67 || e.simFPStimer.getAverageTime() > 0.67) && e.frame > 60) {
			Steam.setAchievement("LAG");
		}
		
		if (carrier_diffusion_warning_timer > 0) {
			Text text = drawString("Error: Metal cannot touch simulation boundary when carrier diffusion view is enabled.", hoffset, voffset + line*vspacing); line ++;
			text.bgcolor = Color.RED;
		}
		if (e.numerical_overflow) {
			Text text = drawString("Error: Numerical overflow detected. Please reset simulation.", hoffset, voffset + line*vspacing); line ++;
			text.bgcolor = Color.RED;
		}

		if (probetexttimer > 0) {
			drawString("Data saved to " + e.datafilename, hoffset, voffset + line*vspacing); line++;
			probetexttimer--;
		}
		
		line = 0;
		if (achievement_timer > 0) {
			achievement_timer--;
			Text text = drawString("Achievement unlocked: " + achievement_name, imgpanel.getWidth(), imgpanel.getHeight() - line*vspacing); line --;
			text.isBottomJustified = true;
			text.isRightJustified = true;
			text.bgcolor = Color.GREEN;
		}
		if (screenshot_timer > 0) {
			screenshot_timer--;
			Text text = drawString(screenshot_name, imgpanel.getWidth(), imgpanel.getHeight()- line*vspacing);
			text.isBottomJustified = true;
			text.isRightJustified = true;
		}

		if (g != null && e.opts.menu_axes.isSelected()) {
			int pad = 15;
			int length = 60;
			if (slice_x) {
				Text t1 = drawString("y", pad + length, imgpanel.getHeight() - pad);
				Text t2 = drawString("z", pad, imgpanel.getHeight() - pad - length);
				t1.isHorizontalCentered = true; t1.isVerticalCentered = true;
				t2.isHorizontalCentered = true; t2.isVerticalCentered = true;
			} else if (slice_y) {
				Text t1 = drawString("x", pad + length, imgpanel.getHeight() - pad);
				Text t2 = drawString("z", pad, imgpanel.getHeight() - pad - length);
				t1.isHorizontalCentered = true; t1.isVerticalCentered = true;
				t2.isHorizontalCentered = true; t2.isVerticalCentered = true;
			} else if (slice_z) {
				Text t1 = drawString("x", pad + length, imgpanel.getHeight() - pad);
				Text t2 = drawString("y", pad, imgpanel.getHeight() - pad - length);
				t1.isHorizontalCentered = true; t1.isVerticalCentered = true;
				t2.isHorizontalCentered = true; t2.isVerticalCentered = true;
			}
		}

		drawStrings(g);

		if (g != null && e.opts.menu_axes.isSelected()) {
			int pad = 15;
			int length = 40;
			int tip = 10;
			if (slice_x) {
				g.setColor(Color.GREEN);
				ArrowDrawer.drawArrow(g, pad, imgpanel.getHeight()-pad, 1, 0, length, tip);
				g.setColor(Color.BLUE);
				ArrowDrawer.drawArrow(g, pad, imgpanel.getHeight()-pad, 0, -1, length, tip);
			} else if (slice_y) {
				g.setColor(Color.RED);
				ArrowDrawer.drawArrow(g, pad, imgpanel.getHeight()-pad, 1, 0, length, tip);
				g.setColor(Color.BLUE);
				ArrowDrawer.drawArrow(g, pad, imgpanel.getHeight()-pad, 0, -1, length, tip);
			} else if (slice_z) {
				g.setColor(Color.RED);
				ArrowDrawer.drawArrow(g, pad, imgpanel.getHeight()-pad, 1, 0, length, tip);
				g.setColor(Color.GREEN);
				ArrowDrawer.drawArrow(g, pad, imgpanel.getHeight()-pad, 0, -1, length, tip);
			}
		}
	}
	
	class GraphicsThread extends Thread {
		
		int n_thread;
		int n_threads;

		double arrowlength;
		double vectorscalingconstant;
		int density_x;
		int density_y;
		double randomness = 0;
		
		double grid_offset;
		double dual_offset;

		double[][][] vfr_x;
		double[][][] vfr_y;
		double[][][] vfr_z;
		
		double[][][] vf_x;
		double[][][] vf_y;
		
		int npx;
		int npy;
		int slice;
		
		Random rand = new Random();

		public GraphicsThread(int n, int n_threads) {
			n_thread = n;
			this.n_threads = n_threads;
			System.out.println("Graphics thread " + n_thread + " initialized.");
		}

		int lower(int n_max) {
			return Math.min((n_thread*n_max)/n_threads, n_max);
		}

		int upper(int n_max) {
			return Math.min(((n_thread+1)*n_max)/n_threads, n_max);
		}

		@Override
		public void run() {
			try {
				while (true) {
					graphics_start_barrier.await();
					
					if (imgData != null) {
						stampPixelData();
					}

					if (synchronized_vector_display_mode != VectorMode.NONE && synchronized_vector_view != VectorView.NONE)
					{
						graphics_mid_barrier.await();
						
						arrowlength = 25.0/scalefactor;
						vectorscalingconstant = 10*Math.pow(10.0, e.opts.gui_brightness_vec.getValue()/10.0)/e.controls.vectorview.getOption().getScalingConstant(e);
						density_x = 10*scalefactor*rx/256;
						density_y = 10*scalefactor*ry/256;

						randomness = 0;
						if (synchronized_vector_display_mode == VectorMode.ARROWS) {
							randomness = 0.5;
						} else if (synchronized_vector_display_mode == VectorMode.LINES) {
							randomness = 0.75;
						}
						
						double[][][][] vf = {null, null, null};
						e.computeVectorField(vf, synchronized_vector_view);
						grid_offset = synchronized_vector_view.getGridOffset();
						dual_offset = synchronized_vector_view.getDualOffset();

						vfr_x = vf[0];
						vfr_y = vf[1];
						vfr_z = vf[2];
						
						vf_x = null;
						vf_y = null;
						npx = 0;
						npy = 0;

						if (slice_x) {
							vf_x = vf[1];
							vf_y = vf[2];
							npx = e.ny;
							npy = e.nz;
						} else if (slice_y) {
							vf_x = vf[0];
							vf_y = vf[2];
							npx = e.nx;
							npy = e.nz;
						} else if (slice_z) {
							vf_x = vf[0];
							vf_y = vf[1];
							npx = e.nx;
							npy = e.ny;
						}

						slice = e.renderer.getSlice();

						if (synchronized_vector_display_mode == VectorMode.LINES && synchronized_render) {
							drawLines();
						} else if (synchronized_vector_display_mode == VectorMode.ARROWS && synchronized_render) {
							drawArrows();
						} else if (synchronized_vector_display_mode == VectorMode.DOTS) {
							drawDots();
						}
					}
					
					if (synchronized_scalar_view != ScalarView.NONE && (synchronized_scalar_display_mode == ScalarMode.CONTOUR_COLORS || synchronized_scalar_display_mode == ScalarMode.CONTOUR) && synchronized_render) {
						graphics_mid_barrier.await();
						drawContours();
					}
					
					if (synchronized_show_carriers) {
						if (synchronized_update_carriers) {
							updateCarriers();
						}
						graphics_mid_barrier.await();
						drawCCdots();
					}
					graphics_end_barrier.await();
				}
			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
		

		public void stampPixelData() {
			if (n_thread == 0) e.t9.start();

			int lower = lower(imgwidth);
			int upper = upper(imgwidth);
			if (slice_z) {
				int k_slice = getSlice();
				for (int x = lower; x < upper; x++) {
					for (int y = 0; y < imgheight; y++) {
						int i = x/scalefactor;
						int j = e.ny-1-y/scalefactor;
						double scale = 1f/max(image_r[i][j][k_slice], image_g[i][j][k_slice], image_b[i][j][k_slice], 1f);
						int rgb = clamp((int)(256*image_r[i][j][k_slice]*scale), 0, 255) << 16
						| clamp((int)(256*image_g[i][j][k_slice]*scale), 0, 255) << 8
						| clamp((int)(256*image_b[i][j][k_slice]*scale), 0, 255);
						imgData[x + y*imgwidth] = rgb;
					}
				}
			}
			if (slice_x) {
				int i_slice = getSlice();
				for (int x = lower; x < upper; x++) {
					for (int y = 0; y < imgheight; y++) {
						int j = x/scalefactor;
						int k = e.nz-1-y/scalefactor;
						double scale = 1f/max(image_r[i_slice][j][k], image_g[i_slice][j][k], image_b[i_slice][j][k], 1f);
						int rgb = clamp((int)(256*image_r[i_slice][j][k]*scale), 0, 255) << 16
						| clamp((int)(256*image_g[i_slice][j][k]*scale), 0, 255) << 8
						| clamp((int)(256*image_b[i_slice][j][k]*scale), 0, 255);
						imgData[x + y*imgwidth] = rgb;
					}
				}
			}

			if (slice_y) {
				int j_slice = getSlice();
				for (int x = lower; x < upper; x++) {
					for (int y = 0; y < imgheight; y++) {
						int i = x/scalefactor;
						int k = e.nz-1-y/scalefactor;
						double scale = 1f/max(image_r[i][j_slice][k], image_g[i][j_slice][k], image_b[i][j_slice][k], 1f);
						int rgb = clamp((int)(256*image_r[i][j_slice][k]*scale), 0, 255) << 16
						| clamp((int)(256*image_g[i][j_slice][k]*scale), 0, 255) << 8
						| clamp((int)(256*image_b[i][j_slice][k]*scale), 0, 255);
						imgData[x + y*imgwidth] = rgb;
					}
				}
			}

			if (n_thread == 0) e.t9.stop();
		}
		
		private void drawLines() {
			rand.setSeed(n_thread);
			for (int i = lower(density_x); i < upper(density_x); i++) {
				for (int j = 0; j < density_y; j++) {

					//double x = (npx-1)*(i+0.5)/50;
					//double y = (npy-1)*(j+0.5)/50;
					double x = npx*(i+randomness*(rand.nextFloat()-0.5))/density_x;
					double y = npy*(j+randomness*(rand.nextFloat()-0.5))/density_y;
					for (int sign = -1; sign <= 1; sign += 2) {

						double prevx = x;
						double prevy = y;
						double dx = 0;
						double dy = 0;


						int steps = 30;
						for (int k = 0; k < steps; k++) {
							if (slice_x) {
								dx = Utils.bilinearinterp(vf_x, slice+dual_offset, prevx+grid_offset, prevy+dual_offset);
								dy = Utils.bilinearinterp(vf_y, slice+dual_offset, prevx+dual_offset, prevy+grid_offset);
							} else if (slice_y) {
								dx = Utils.bilinearinterp(vf_x, prevx+grid_offset, slice+dual_offset, prevy+dual_offset);
								dy = Utils.bilinearinterp(vf_y, prevx+dual_offset, slice+dual_offset, prevy+grid_offset);
							} else if (slice_z) {
								dx = Utils.bilinearinterp(vf_x, prevx+grid_offset, prevy+dual_offset, slice+dual_offset);
								dy = Utils.bilinearinterp(vf_y, prevx+dual_offset, prevy+grid_offset, slice+dual_offset);
							}

							double fieldmagnitude = Math.sqrt(dx*dx+dy*dy);
							double alphaFG = bump(0.5*(1.0-k/(double)(steps-1)), 0.5)*Math.min(1, vectorscalingconstant*fieldmagnitude);

							if (fieldmagnitude != 0) {
								dx /= fieldmagnitude;
								dy /= fieldmagnitude;
							}

							double nextx = prevx + dx*arrowlength*0.25*sign;
							double nexty = prevy + dy*arrowlength*0.25*sign;

							drawLine((int)((prevx+0.5)*scalefactor), (int)((prevy+0.5)*scalefactor), (int)((nextx+0.5)*scalefactor), (int)((nexty+0.5)*scalefactor), k == 0 && sign == 1,
							1f, 1f, 1f, (float) alphaFG, 1f);

							prevx = nextx;
							prevy = nexty;
						}
					}
				}
			}
		}
		
		private void drawArrows() {
			rand.setSeed(n_thread);
			
			Vector ctr = new Vector(0,0);
			Vector arrow = new Vector(0,0);
			Vector tip1 = new Vector(0,0);
			Vector tip2 = new Vector(0,0);
			Vector body1 = new Vector(0,0);
			Vector body2 = new Vector(0,0);
			
			for (int i = lower(density_x); i < upper(density_x); i++) {
				for (int j = 0; j < density_y; j++) {

					//double x = (npx-1)*(i+0.5)/50;
					//double y = (npy-1)*(j+0.5)/50;
					double x = npx*(i+randomness*(rand.nextFloat()-0.5))/density_x;
					double y = npy*(j+randomness*(rand.nextFloat()-0.5))/density_y;
					ctr.x = x+0.5;
					ctr.y = y+0.5;

					if (slice_x) {
						arrow.x = Utils.bilinearinterp(vf_x, slice+dual_offset, x+grid_offset, y+dual_offset);
						arrow.y = Utils.bilinearinterp(vf_y, slice+dual_offset, x+dual_offset, y+grid_offset);
					} else if (slice_y) {
						arrow.x = Utils.bilinearinterp(vf_x,x+grid_offset, slice+dual_offset, y+dual_offset);
						arrow.y = Utils.bilinearinterp(vf_y,x+dual_offset, slice+dual_offset, y+grid_offset);
					} else if (slice_z) {
						arrow.x = Utils.bilinearinterp(vf_x,x+grid_offset, y+dual_offset, slice+dual_offset);
						arrow.y = Utils.bilinearinterp(vf_y,x+dual_offset, y+grid_offset, slice+dual_offset);
					}

					double fieldmagnitude = Math.max(0.1, 10*vectorscalingconstant*Math.sqrt(arrow.dot(arrow)));
					arrow.normalize();
					tip1.copy(arrow);
					tip2.copy(arrow);
					tip1.rotate(Math.PI*5.0/6.0);
					tip2.rotate(Math.PI*7.0/6.0);

					body1.copy(ctr);
					body1.addmult(arrow, -0.5*arrowlength);
					body2.copy(ctr);
					body2.addmult(arrow, 0.5*arrowlength);
					tip1.scalarmult(0.35*arrowlength);
					tip1.add(body2);
					tip2.scalarmult(0.35*arrowlength);
					tip2.add(body2);
					double alphaFG = (0.1*Math.sqrt(fieldmagnitude));
					drawLine((int)(body1.x*scalefactor), (int)(body1.y*scalefactor), (int)(body2.x*scalefactor), (int)(body2.y*scalefactor), true,
					(float)fieldmagnitude, (float)fieldmagnitude, (float)fieldmagnitude, (float)alphaFG, 1f);
					drawLine((int)(body2.x*scalefactor), (int)(body2.y*scalefactor), (int)(tip1.x*scalefactor), (int)(tip1.y*scalefactor), false,
					(float)fieldmagnitude, (float)fieldmagnitude, (float)fieldmagnitude, (float)alphaFG, 1f);
					drawLine((int)(body2.x*scalefactor), (int)(body2.y*scalefactor), (int)(tip2.x*scalefactor), (int)(tip2.y*scalefactor), false,
					(float)fieldmagnitude, (float)fieldmagnitude, (float)fieldmagnitude, (float)alphaFG, 1f);
				}
			}
		}
		
		private void drawDots() {
			boolean paused = e.opts.gui_paused.isSelected();
			boolean conductors_only = synchronized_vector_view.isConductorOnly();

			int lower = lower(dots.size());
			int upper = upper(dots.size());
			for (int i = lower; i < upper; i++) {
				Dot d = dots.get(i);
				if (d.time <= 0 || d.x < 0 || d.y < 0 || d.z < 0 || d.x >= e.nx || d.y >= e.ny || d.z >= e.nz) {
					d.x = e.nx*rand.nextDouble();
					d.y = e.ny*rand.nextDouble();
					d.z = e.nz*rand.nextDouble();
					d.lifespan = 100*(1+rand.nextDouble());
					d.time = d.lifespan;
					if (conductors_only && Utils.bilinearinterp(e.conducting, d.x, d.y, d.z) == 0) {
						d.lifespan = 0;
						d.time = 0;
					}
				}
			}
			
			setColorFloat(1, 1, 1);
			int dot_offset = (int)(0.25*scalefactor-1)/2;
			for (int i = lower; i < upper; i++) {
				Dot d = dots.get(i);
				if (d.time > 0) {
					double dx = 0;
					double dy = 0;
					double dz = 0;
					int steps = 10;

					if (!paused) {
						for (int k = 0; k < steps; k++) {
							dx = Utils.bilinearinterp(vfr_x,d.x+grid_offset, d.y+dual_offset, d.z+dual_offset)*vectorscalingconstant/steps;
							dy = Utils.bilinearinterp(vfr_y,d.x+dual_offset, d.y+grid_offset, d.z+dual_offset)*vectorscalingconstant/steps;
							dz = Utils.bilinearinterp(vfr_z,d.x+dual_offset, d.y+dual_offset, d.z+grid_offset)*vectorscalingconstant/steps;
							double maxspeed = 0.5;
							double factor = Math.min(1, maxspeed/Math.sqrt(dx*dx+dy*dy+dz*dz));
							d.x += dx*factor;
							d.y += dy*factor;
							d.z += dz*factor;
						}

						double p = d.time/d.lifespan;
						d.brightness = Math.min(1, Math.max(0.1, 500*Math.sqrt(dx*dx+dy*dy+dz*dz)))*bump(p, 1/3.0);
						d.time -= 1;
					}

					double alphaFG = d.brightness;
					if (synchronized_render) {
						double x = project_x(d.x, d.y, d.z);
						double y = project_y(d.x, d.y, d.z);
						double z = project_z(d.x, d.y, d.z);
						
						if (Math.abs(z - slice) < 2)
							drawRectangle((int)((x+0.5)*scalefactor)-dot_offset, (int)((y+0.5)*scalefactor)-dot_offset, (int)(scalefactor*0.25), (int)(scalefactor*0.25), 1f, 1f, 1f, (float)alphaFG, 1f);
					}
				}
			}
		}
		
		private void drawContours() {
			double spacing = 0.2/scalingconstant;
			double contourwidth = e.ds;

			int slice = getSlice();

			for (int i = lower(imgwidth); i < upper(imgwidth); i++) {
				for (int j = 0; j < imgheight; j++) {
					double u = 0;
					double v = 0;
					
					if (slice_x) {
						u = Utils.bilinearinterp(scalarfield, slice - 0.5, (double)i/scalefactor - 0.5, (double)j/scalefactor - 0.5)/spacing;
						v = Utils.bilinearinterp(gradscalarfield, slice - 0.5, (double)i/scalefactor - 0.5, (double)j/scalefactor - 0.5)/spacing;
					} else if (slice_y) {
						u = Utils.bilinearinterp(scalarfield, (double)i/scalefactor - 0.5, slice - 0.5, (double)j/scalefactor - 0.5)/spacing;
						v = Utils.bilinearinterp(gradscalarfield, (double)i/scalefactor - 0.5, slice - 0.5, (double)j/scalefactor - 0.5)/spacing;
					} else if (slice_z) {
						u = Utils.bilinearinterp(scalarfield, (double)i/scalefactor - 0.5, (double)j/scalefactor - 0.5, slice - 0.5)/spacing;
						v = Utils.bilinearinterp(gradscalarfield, (double)i/scalefactor - 0.5, (double)j/scalefactor - 0.5, slice - 0.5)/spacing;
					}
					
					double f = ((((u%1)+1.5)%1)/Math.abs(v))/contourwidth;
					if (f < 1) {
						drawPixel(i, j, 1f, 1f, 1f, (float)(2*Math.min(f, 1-f)), 1f);
					}
				}
			}
		}
		
		private void updateCarriers() throws InterruptedException, BrokenBarrierException {
			if (n_thread == 0 && carrier_diffusion_warning_timer > 0) {
				carrier_diffusion_warning_timer--;
			}
			
			double C = cc_default_dot_density*Math.pow(10.0, e.opts.gui_carrier_density.getValue()/20.0);

			if (n_thread == 0) {
				rho_n_dist.prepare(e.rho_n);
				rho_p_dist.prepare(e.rho_p);
				rho_G_dist.prepare(e.G);
			}
			
			int i_low = lower(ccdots.size());
			int i_high = upper(ccdots.size());

			graphics_mid_barrier.await();

			double A = e.ds*e.ds;
			double N_G = rho_G_dist.getTotalAmount()*A*e.e_charge*C*delta_t;
			double N_n = rho_n_dist.getTotalAmount()*A*C*delta_t/tau;
			double N_p = rho_p_dist.getTotalAmount()*A*C*delta_t/tau;

			double N_n_excess = Math.max(C-C_prev, 0)*rho_n_dist.getTotalAmount()*A;
			double N_p_excess = Math.max(C-C_prev, 0)*rho_p_dist.getTotalAmount()*A;

			double P_deficit = Math.max(-(C-C_prev)/C_prev, 0);

			boolean show_gen_recomb = e.opts.menu_gen_recomb.isSelected();
			
			for (int i = i_low; i < i_high; i++) {
				ChargeCarrierDot d = ccdots.get(i);
				if (d == null) continue;
				
				d.time -= delta_t;
				if (d.time < 0) {
					ccdots.remove(i);
					i--;
				}
				else {
					double R_tmp = Utils.bilinearinterp(e.R, d.x, d.y, d.z)*e.e_charge;
					if (d.type == DotType.HOLE) {
						if (R_tmp > 0 && frand.next() < R_tmp/Utils.bilinearinterp(e.rho_p, d.x, d.y, d.z)*delta_t) {
							if (show_gen_recomb) {
								ccdots.replace(i, new ChargeCarrierDot(d.x, d.y, d.z, tau_events, tau_events*0.5, DotType.RECOMBINATION, 1));
							} else {
								ccdots.remove(i);
								i--;
							}
							continue;
						}
					} else if (d.type == DotType.ELECTRON) {
						if (R_tmp > 0 && frand.next() < -R_tmp/Utils.bilinearinterp(e.rho_n, d.x, d.y, d.z)*delta_t) {
							if (show_gen_recomb) {
								ccdots.replace(i, new ChargeCarrierDot(d.x, d.y, d.z, tau_events, tau_events*0.5, DotType.RECOMBINATION, 1));
							} else {
								ccdots.remove(i);
								i--;
							}
							continue;
						}
					} else {
						if (Utils.bilinearinterp(e.semiconducting, d.x, d.y, d.z) == 0) {
							d.time -= 5*delta_t;
						}
					}

					if (P_deficit > 0 && frand.next() < P_deficit) {
						ccdots.remove(i);
						i--;
						continue;
					}
				}
			}

			graphics_mid_barrier.await();

			rho_n_dist.generateSamples(N_n/n_threads, (c) -> { ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau, tau, DotType.ELECTRON, 0)); });
			rho_p_dist.generateSamples(N_p/n_threads, (c) -> { ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau, tau, DotType.HOLE, 0)); });
			rho_G_dist.generateSamples(N_G/n_threads, (c) -> {
				ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau, tau*frand.next(), DotType.ELECTRON, 0));
				if (show_gen_recomb) ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau_events, tau_events*0.5, DotType.GENERATION, 1));
			});
			rho_G_dist.generateSamples(N_G/n_threads, (c) -> {
				ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau, tau*frand.next(), DotType.HOLE, 0));
				if (show_gen_recomb) ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau_events, tau_events*0.5, DotType.GENERATION, 1));
			});
			rho_n_dist.generateSamples(N_n_excess/n_threads, (c) -> { ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau, tau*frand.next(), DotType.ELECTRON, 0)); });
			rho_p_dist.generateSamples(N_p_excess/n_threads, (c) -> { ccdots.add(new ChargeCarrierDot(c.x, c.y, c.z, tau, tau*frand.next(), DotType.HOLE, 0)); });

			C_prev = C;
		}
		
		public void drawCCdots() {
			boolean fast = e.opts.menu_hide_carriers_metal.isSelected();
			boolean show_diffusion = e.opts.menu_carrier_diffusion.isSelected();
			
			int lower = lower(ccdots.size());
			int upper = upper(ccdots.size());

			int steps = fast? 2 : 10;
			double dt_dot = delta_t/steps;

			int slice = getSlice();

			try {
				double factor = Math.sqrt(24*dt_dot);
				int dot_offset = (scalefactor-1)/2;
				
				for (int i = lower; i < upper; i++) {
					ChargeCarrierDot d = ccdots.get(i);

					if (d.time > 0) {

						boolean dorender = !fast || Utils.bilinearinterp(e.semiconducting, d.x, d.y, d.z) > 0;

						if (delta_t > 0) {
							if (!show_diffusion) {
								if (d.type == DotType.ELECTRON) {
									for (int k = 0; k < steps; k++) {
										double s = dt_dot/(e.ds*Utils.bilinearinterp(e.rho_n, d.x, d.y, d.z));
										d.x += s*Utils.bilinearinterp(e.Jx_n, d.x-0.5, d.y, d.z);
										d.y += s*Utils.bilinearinterp(e.Jy_n, d.x, d.y-0.5, d.z);
										d.z += s*Utils.bilinearinterp(e.Jz_n, d.x, d.y, d.z-0.5);
									}
								} else if (d.type == DotType.HOLE) {
									for (int k = 0; k < steps; k++) {
										double s = dt_dot/(e.ds*Utils.bilinearinterp(e.rho_p, d.x, d.y, d.z));
										d.x += s*Utils.bilinearinterp(e.Jx_p, d.x-0.5, d.y, d.z);
										d.y += s*Utils.bilinearinterp(e.Jy_p, d.x, d.y-0.5, d.z);
										d.z += s*Utils.bilinearinterp(e.Jz_p, d.x, d.y, d.z-0.5);
									}
								}
							} else {
								if (d.type == DotType.ELECTRON) {
									double sqrt_D_n = Utils.bilinearinterp(e.sqrt_D_eff_n, d.x, d.y, d.z);
									double s = dt_dot/e.ds;
									double t = factor*sqrt_D_n/e.ds; //Random walk PDF obeys diffusion equation. Variance of uniform dist is 12L^2 and variance of heat kernel is 2Dt
									for (int k = 0; k < steps; k++) {
										double dx_diff = t*(frand.next()-0.5) + s*Utils.bilinearinterp(e.vel_x_n, d.x-0.5, d.y, d.z);
										double dy_diff = t*(frand.next()-0.5) + s*Utils.bilinearinterp(e.vel_y_n, d.x, d.y-0.5, d.z);
										double dz_diff = t*(frand.next()-0.5) + s*Utils.bilinearinterp(e.vel_z_n, d.x, d.y, d.z-0.5);
										if (e.conducting[(int)(d.x+dx_diff+0.5)][(int)(d.y+dy_diff+0.5)][(int)(d.z+dz_diff+0.5)] == 1) {
											d.x += dx_diff;
											d.y += dy_diff;
											d.z += dz_diff;
										}
									}
								} else if (d.type == DotType.HOLE) {
									double sqrt_D_p = Utils.bilinearinterp(e.sqrt_D_eff_p, d.x, d.y, d.z);
									double s = dt_dot/e.ds;
									double t = factor*sqrt_D_p/e.ds;
									
									for (int k = 0; k < steps; k++) {
										double dx_diff = t*(frand.next()-0.5) + s*Utils.bilinearinterp(e.vel_x_p, d.x-0.5, d.y, d.z);
										double dy_diff = t*(frand.next()-0.5) + s*Utils.bilinearinterp(e.vel_y_p, d.x, d.y-0.5, d.z);
										double dz_diff = t*(frand.next()-0.5) + s*Utils.bilinearinterp(e.vel_z_p, d.x, d.y, d.z-0.5);
										if (e.conducting[(int)(d.x+dx_diff+0.5)][(int)(d.y+dy_diff+0.5)][(int)(d.z+dz_diff+0.5)] == 1) {
											d.x += dx_diff;
											d.y += dy_diff;
											d.z += dz_diff;
										}
									}
								}
							}

							double p = d.time/d.lifespan;
							d.brightness = 1.0*bump(p, 1/3.0);
						}

						double alphaFG = d.brightness;
						if (synchronized_render && dorender) {

							double x = project_x(d.x, d.y, d.z);
							double y = project_y(d.x, d.y, d.z);
							double z = project_z(d.x, d.y, d.z);

							if (Math.abs(z - slice) < 2){
								if (d.type == DotType.ELECTRON)
									drawRectangle((int)((x+0.5)*scalefactor)-dot_offset, (int)((y+0.5)*scalefactor)-dot_offset, scalefactor, scalefactor,
									0.25f, 0.25f, 1f, (float)alphaFG, 1-(float)alphaFG, d.random_id);
								else if (d.type == DotType.HOLE)
									drawRectangle((int)((x+0.5)*scalefactor)-dot_offset, (int)((y+0.5)*scalefactor)-dot_offset, scalefactor, scalefactor,
									1f, 0.25f, 0.25f, (float)alphaFG, 1-(float)alphaFG, d.random_id);
								else if (d.type == DotType.GENERATION) {
									drawRectangle((int)((x+0.5)*scalefactor)-dot_offset, (int)((y+0.5)*scalefactor)-dot_offset, scalefactor, scalefactor,
									0f, 0f, 0f, (float)alphaFG, 1-(float)alphaFG, d.random_id);
								} else if (d.type == DotType.RECOMBINATION) {
									drawRectangle((int)((x+0.5)*scalefactor)-dot_offset, (int)((y+0.5)*scalefactor)-dot_offset, scalefactor, scalefactor,
									1f, 1f, 1f, (float)alphaFG, 1-(float)alphaFG, d.random_id);
								}
							}
						}

					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				carrier_diffusion_warning_timer = 20;
			}
		}
		
	}

	public void clearStrings() {
		texts.clear();
	}

	public void drawStrings(Graphics g) {
		if (g == null)
			return;

		boolean dodraw = e.opts.menu_text_bg.isSelected() && e.opts.menu_interface.isSelected();

		int slice = getSlice();
		for (Text text : texts) {
			if (text.is3D) {

				double sf_x = (double)e.canvas.zoom_bound_x/(e.controls.zoom_i2-e.controls.zoom_i1+1);
				double sf_y = (double)e.canvas.zoom_bound_y/(e.controls.zoom_j2-e.controls.zoom_j1+1);

				int xcoord = 0;
				int ycoord = 0;
				int zcoord = 0;
				if (slice_x) {
					xcoord = text.y;
					ycoord = e.nz - 1 - text.z;
					zcoord = text.x - slice;
				} else if (slice_y) {
					xcoord = text.x;
					ycoord = e.nz - 1 - text.z;
					zcoord = text.y - slice;
				} else if (slice_z) {
					xcoord = text.x;
					ycoord = e.ny - 1 - text.y;
					zcoord = text.z - slice;
				}
				
				text.x = (int) ((xcoord-e.controls.zoom_i1+0.5)*sf_x+e.canvas.offset_x);
				text.y = (int) ((ycoord-e.controls.zoom_j1+0.5)*sf_y+e.canvas.offset_y);
				text.z = zcoord;
			}
		}
		
		for (Text text : texts) {
			text.computeDimensions(g);
			if (text.isRightJustified) text.x -= (text.width);
			if (text.isBottomJustified) text.y -= (text.height+5);
			if (text.isHorizontalCentered) text.x -= (text.width/2-3);
			if (text.isVerticalCentered) text.y -= (text.height/2+2);
		}
		
		for (Text text : texts) {
			if (Math.abs(text.z) > 4) continue;
			if (text.hasBackground && (dodraw || text.bgcolor != null)) {
				g.setFont(text.getFont());
				int x = text.x-3;
				int y = text.y+3;

				g.setColor(Color.GRAY);
				g.fillRect(x-2, y-2, text.width+4, text.height+4);
			}
		}

		for (Text text : texts) {
			if (Math.abs(text.z) > 4) continue;
			if (text.hasBackground && (dodraw || text.bgcolor != null)) {
				g.setFont(text.getFont());

				int x = text.x-3;
				int y = text.y+3;

				if (text.bgcolor != null)
					g.setColor(text.bgcolor);
				else
					g.setColor(Color.BLACK);
				
				g.fillRect(x, y, text.width, text.height);
			}
		}
		
		dodraw = e.opts.menu_interface.isSelected();

		for (Text text : texts) {
			if (Math.abs(text.z) > 4) continue;
			if (dodraw || text.bgcolor != null) {
				g.setFont(text.getFont());

				g.setColor(Color.DARK_GRAY);
				g.drawString(text.text, text.x+1, text.y+text.height-2);
				g.setColor(Color.WHITE);
				g.drawString(text.text, text.x, text.y+text.height-3);
			}
		}
	}

	public void startNewStringLayer() {
		texts.clear();
	}

	public Text drawString(String str1, int x, int y) {
		Text t = new Text(str1, x, y);
		texts.add(t);
		return t;
	}

	public void drawTwoColumnString(String str1, String str2, int x, int y) {
		drawString(String.format("%-10s", str1), x, y);
		Text text = drawString(str2, x+40, y);
		text.minwidth = 80;
	}

	public Text draw3dString(String str1, int x, int y, int z) {
		Text t = new Text(str1, x, y, z);
		t.is3D = true;
		texts.add(t);
		return t;
	}
	
	public static class Text {

		public static int fontsize = 12;
		public static Font bigfont = new Font(Font.SANS_SERIF, Font.PLAIN, (int)(1.3*fontsize));
		public static Font regularfont = new Font(Font.SANS_SERIF, Font.PLAIN, fontsize);
		public static Font monospacefont = getMonospacedFont();
		
		public static void setFontSize(int newfontsize) {
			fontsize = newfontsize;
			bigfont = new Font(Font.SANS_SERIF, Font.PLAIN, (int)(1.3*fontsize));
			regularfont = new Font(Font.SANS_SERIF, Font.PLAIN, fontsize);
			monospacefont = getMonospacedFont();
		}
		
		String text;
		int x;
		int y;
		int z;
		int minwidth;
		int width = 0;
		int height = 0;
		
		boolean isBig = false;
		boolean hasBackground = true;
		boolean isMonospaced = false;
		boolean isRightJustified = false;
		boolean isBottomJustified = false;
		boolean isHorizontalCentered = false;
		boolean isVerticalCentered = false;
		boolean is3D;
		Color bgcolor = null;

		public Text(String text, int x, int y) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.z = 0;
			minwidth = 0;
		}
		
		public Text(String text, int x, int y, int z) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.z = z;
			minwidth = 0;
		}
		
		public Font getFont() {
			if (isBig)
				return bigfont;
			else if (isMonospaced)
				return monospacefont;
			else
				return regularfont;
		}
		
		public void computeDimensions(Graphics g) {
			g.setFont(getFont());
			width = Math.max(minwidth, g.getFontMetrics().stringWidth(text)+8);
			height = g.getFontMetrics().getHeight()+4;
		}
		
		public static Font getMonospacedFont() {
			Font f = Font.decode("Consolas-PLAIN-" + fontsize);
			String specialstring = "\u03c1\u2099\u209A\u2080\u03d5";
			if (f.getFamily() == "Dialog" || f.canDisplayUpTo(specialstring) != -1)
				f = Font.decode("Andale Mono-PLAIN-" + fontsize);
			if (f.getFamily() == "Dialog" || f.canDisplayUpTo(specialstring) != -1)
				f = new Font(Font.MONOSPACED, Font.PLAIN, fontsize);
			System.out.println(f.getName());
			return f;
		}
	}

	public enum ScalarView {
		NONE("No scalar overlay",															"None",		Quantity.DIMENSIONLESS,				ColorScheme.OTHER			),
		E_FIELD("View: E field magnitude",													"E",		Quantity.ELECTRIC_FIELD,			ColorScheme.GREEN			),
		D_FIELD("View: D field magnitude",													"D",		Quantity.ELECTRIC_FLUX_DENSITY,		ColorScheme.GREEN			),
		B_FIELD("View: B field",															"B",		Quantity.MAGNETIC_FLUX_DENSITY,		ColorScheme.CYAN_YELLOW		),
		H_FIELD("View H field",																"H",		Quantity.MAGNETIC_FIELD_STRENGTH,	ColorScheme.CYAN_YELLOW		),
		POTENTIAL("View \u03d5: Electric scalar potential", 								"\u03d5",	Quantity.ELECTRIC_POTENTIAL,		ColorScheme.RED_BLUE		),
		ENERGY("View u: Electromagnetic energy density",									"u",		Quantity.ENERGY_DENSITY,			ColorScheme.GREEN			),
		CURRENT("View J: Total current magnitude",											"J",		Quantity.CURRENT_DENSITY,			ColorScheme.GREEN			),
		CHARGE("View \u03c1: Net charge density",											"\u03c1",	Quantity.CHARGE_DENSITY,			ColorScheme.OTHER			),
		ELECTRON_CHARGE("View \u03c1\u2099: Electron charge density",						"\u03c1\u2099",	Quantity.CHARGE_DENSITY,		ColorScheme.RED_BLUE		),
		HOLE_CHARGE("View \u03c1\u209A: Hole charge density",								"\u03c1\u209A",	Quantity.CHARGE_DENSITY,		ColorScheme.RED_BLUE		),
		BACKGROUND_CHARGE("View \u03c1\u2080: Static charge density (doping)",				"\u03c1\u2080",	Quantity.CHARGE_DENSITY,		ColorScheme.RED_BLUE		),
		COMBINED_CHARGE("View: Combined electron+hole charge density",						"\u03c1\u2099 and \u03c1\u209A",	Quantity.DIMENSIONLESS,	ColorScheme.OTHER	),
		HEAT("View Q: Heat dissipation",													"q",		Quantity.POWER_DENSITY,				ColorScheme.RED_BLUE		),
		ENTROPY("View s: Entropy generation rate",											"s",		Quantity.ENTROPY_DENSITY_RATE,		ColorScheme.RED_BLUE		),
		ELECTRON_POTENTIAL("View F\u2099: Electron quasi Fermi level",						"µ\u2099",	Quantity.ELECTRIC_POTENTIAL,		ColorScheme.RED_BLUE 		),
		HOLE_POTENTIAL("View F\u209A: Hole quasi Fermi level",								"µ\u209A",	Quantity.ELECTRIC_POTENTIAL,		ColorScheme.RED_BLUE 		),
		AVERAGE_POTENTIAL("View V: Voltage",												"V",		Quantity.ELECTRIC_POTENTIAL,		ColorScheme.RED_BLUE		),
		GENERATION("View G: Carrier generation rate (net)",									"G",		Quantity.RATE_DENSITY,				ColorScheme.GREEN			),
		RECOMBINATION("View R: Carrier recombination rate (net)",							"R",		Quantity.RATE_DENSITY,				ColorScheme.GREEN			),
		LIGHT("View: Emitted light",														"Light",	Quantity.DIMENSIONLESS,				ColorScheme.OTHER			),
		ELECTRON_DENSITY("View n\u2099: Electron density",									"n\u2099",	Quantity.NUMBER_DENSITY,			ColorScheme.GREEN			),
		HOLE_DENSITY("View n\u209A: Hole density",											"n\u209A",	Quantity.NUMBER_DENSITY,			ColorScheme.GREEN			),
		ELECTRON_VOLTAGE("View V\u2099: Electron voltage",									"V\u2099",	Quantity.ELECTRIC_POTENTIAL,		ColorScheme.RED_BLUE		),
		HOLE_VOLTAGE("View V\u209A: Hole voltage",											"V\u209A",	Quantity.ELECTRIC_POTENTIAL,		ColorScheme.RED_BLUE		),
		ELECTRON_VEL("View v\u2099: Electron drift velocity",								"v\u2099",	Quantity.VELOCITY,					ColorScheme.GREEN			),
		HOLE_VEL("View v\u209A: Hole drift velocity",										"v\u209A",	Quantity.VELOCITY,					ColorScheme.GREEN			),
		RECOMB_RAD("View: Radiative recombination rate",									"R (rad)",	Quantity.RATE_DENSITY,				ColorScheme.GREEN			),
		RECOMB_SRH("View: SRH recombination rate",											"R (SRG)",	Quantity.RATE_DENSITY,				ColorScheme.GREEN			),
		RECOMB_AUGER("View: Auger recombination rate",										"R (aug)",	Quantity.RATE_DENSITY,				ColorScheme.GREEN			),
		DEBUG("Debug",																		"Debug",	Quantity.DIMENSIONLESS,				ColorScheme.RED_BLUE		);
	
		enum ColorScheme {
			RED_BLUE, CYAN_YELLOW, GREEN, WHITE, OTHER;
		}
	
		public String name;
		public Quantity unit;
		public String shorthand;
		public ColorScheme colorscheme;
	
		ScalarView(String name, String shorthand, Quantity unit, ColorScheme colorScheme)
		{
			this.name = name;
			this.shorthand = shorthand;
			this.unit = unit;
			this.colorscheme = colorScheme;
		}
	
		@Override
		public String toString() {
			return name;
		}

		public double getScalingConstant(Simulation e) {
			double voltage = e.Eg_semi/e.e_charge;
			double number_density = e.n_default_doping_concentration;
			double charge_density = e.n_default_doping_concentration*e.e_charge;
			double length = Math.sqrt(e.eps_r_semi*e.eps0*voltage/charge_density);
			double electric_field = voltage/length;
			double velocity = electric_field*e.mu_electron_semi;
			double conductivity = charge_density*e.mu_electron_semi;
			double current_density = conductivity*electric_field;
			double rate_density = number_density*(e.k_SRH_n_semi+e.k_SRH_p_semi)
					+ number_density*number_density*e.k_rad_semi
					+ number_density*number_density*number_density*(e.k_aug_n_semi + e.k_aug_p_semi);
			
			switch (this) {
			case NONE: return 1;
			case E_FIELD: return electric_field;
			case D_FIELD: return e.eps0*electric_field;
			case B_FIELD: return e.mu0*current_density*length;
			case H_FIELD: return current_density*length;
			case POTENTIAL: return voltage;
			case ENERGY: return e.eps0*electric_field*electric_field;
			case CURRENT: return current_density;
			case CHARGE: return charge_density;
			case ELECTRON_CHARGE: return charge_density;
			case HOLE_CHARGE: return charge_density;
			case BACKGROUND_CHARGE: return charge_density;
			case COMBINED_CHARGE: return 0.1*charge_density;
			case HEAT: return current_density*electric_field;
			case ENTROPY: return current_density*electric_field/e.T;
			case ELECTRON_POTENTIAL: return voltage;
			case HOLE_POTENTIAL:  return voltage;
			case AVERAGE_POTENTIAL: return voltage;
			case GENERATION: return rate_density;
			case RECOMBINATION: return rate_density;
			case LIGHT: return rate_density;
			case ELECTRON_DENSITY: return number_density;
			case HOLE_DENSITY: return number_density;
			case ELECTRON_VOLTAGE: return voltage;
			case HOLE_VOLTAGE: return voltage;
			case ELECTRON_VEL: return velocity;
			case HOLE_VEL: return velocity;
			case RECOMB_RAD: return rate_density;
			case RECOMB_SRH: return rate_density;
			case RECOMB_AUGER: return rate_density;
			case DEBUG: return 1;
			}
			return 1;
		}
	}

	public enum VectorView {
		NONE("No vector overlay",								"None",				Quantity.DIMENSIONLESS			),
		E_FIELD("View E: Electric field",						"E",				Quantity.ELECTRIC_FIELD			),
		D_FIELD("View D: Displacement field",					"D",				Quantity.ELECTRIC_FLUX_DENSITY	),
		B_FIELD("View B: Magnetic flux density",				"B",				Quantity.MAGNETIC_FLUX_DENSITY			),
		H_FIELD("View H: Magnetic field strength",				"H",				Quantity.MAGNETIC_FIELD_STRENGTH		),
		ELECTRON_CURRENT("View J\u2099: Electron current",		"J\u2099",			Quantity.CURRENT_DENSITY		),
		HOLE_CURRENT("View J\u209A: Hole current",				"J\u209A",			Quantity.CURRENT_DENSITY		),
		TOTAL_CURRENT("View J: Total current",					"J",				Quantity.CURRENT_DENSITY		),
		EMF("View \u2130: External electromotive force",		"\u2130",			Quantity.ELECTRIC_FIELD			),
		POYNTING("View S: Poynting vector",						"S",				Quantity.INTENSITY				),
		ELECTRON_DRIFT("View: Electron drift current",			"J\u2099 (drift)",		Quantity.CURRENT_DENSITY	),
		ELECTRON_DIFFUSION("View: Electron diffusion current",	"J\u2099 (diffusion)",	Quantity.CURRENT_DENSITY	),
		ELECTRON_VELOCITY("View: Electron drift velocity",		"v\u2099",				Quantity.VELOCITY			),
		HOLE_DRIFT("View: Hole drift current",					"J\u209A (drift)",		Quantity.CURRENT_DENSITY	),
		HOLE_DIFFUSION("View: Hole diffusion current",			"J\u209A (diffusion)",	Quantity.CURRENT_DENSITY	),
		HOLE_VELOCITY("View: Hole drift velocity",				"v\u209A",				Quantity.VELOCITY			);
	
		public String name;
		public Quantity unit;
		public String shorthand;

		VectorView(String name, String shorthand, Quantity unit)
		{
			this.name = name;
			this.shorthand = shorthand;
			this.unit = unit;
		}
	
		@Override
		public String toString() {
			return name;
		}
		
		public boolean isConductorOnly() {
			return (!(this == E_FIELD || this == D_FIELD || this == POYNTING));
		}
		

		public double getScalingConstant(Simulation e) {
			double voltage = e.Eg_semi/e.e_charge;
			double charge_density = e.n_default_doping_concentration*e.e_charge;
			double length = Math.sqrt(e.eps_r_semi*e.eps0*voltage/charge_density);
			double electric_field = voltage/length;
			double velocity = electric_field*e.mu_electron_semi;
			double conductivity = charge_density*e.mu_electron_semi;
			double current_density = conductivity*electric_field;
			
			switch (this) {
			case NONE: return 1;
			case E_FIELD: return electric_field;
			case D_FIELD: return e.eps0*electric_field;
			case B_FIELD: return e.mu0*current_density*length;
			case H_FIELD: return current_density*length;
			case ELECTRON_CURRENT: return current_density;
			case HOLE_CURRENT: return current_density;
			case TOTAL_CURRENT: return current_density;
			case EMF: return electric_field;
			case POYNTING: return electric_field*current_density*length;
			case ELECTRON_DRIFT: return current_density;
			case ELECTRON_DIFFUSION: return current_density;
			case ELECTRON_VELOCITY: return velocity;
			case HOLE_DRIFT: return current_density;
			case HOLE_DIFFUSION: return current_density;
			case HOLE_VELOCITY: return velocity;
			}
			return 1;
		}
		
		public double getGridOffset() {
			if (this == B_FIELD || this == H_FIELD) {
				return 0;
			}
			return -0.5;
		}
		
		public double getDualOffset() {
			if (this == B_FIELD) {
				return 0.5;
			}
			if (this == H_FIELD) {
				return -0.5;
			}
			return 0;
		}
	}

	public enum ScalarMode {
		NONE("Turn off scalar overlay", ""),
		COLORS("Show colors", "color"),
		CONTOUR("Show contours", "contours"),
		CONTOUR_COLORS("Show contours and colors", "color+contours");
	
		String name;
		public String shorthand;
		ScalarMode(String name, String shorthand)
		{
			this.name = name;
			this.shorthand = shorthand;
		}
	
		@Override
		public String toString() {
			return name;
		}
	}
	
	public enum VectorMode {
		NONE("Turn off vector overlay", ""),
		ARROWS("Show vectors", "vectors"),
		LINES("Show lines", "lines"),
		DOTS("Show moving dots", "dots");
	
		String name;
		public String shorthand;
		VectorMode(String name, String shorthand)
		{
			this.name = name;
			this.shorthand = shorthand;
		}
	
		@Override
		public String toString() {
			return name;
		}
	}
	
	public enum RenderMode {
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
	
	public class Dot {
		double x = 0;
		double y = 0;
		double z = 0;
		double lifespan = 0;
		double time = 0;
		double brightness = 0;
		
		public Dot(double x, double y, double z, double lifespan, double time) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.lifespan = lifespan;
			this.time = time;
		}
	}

	public class ChargeCarrierDot extends Dot {
		DotType type;
		int random_id;
		
		public ChargeCarrierDot(double x, double y, double z, double lifespan, double time, DotType type, int random_id) {
			super(x, y, z, lifespan, time);
			this.type = type;
			this.random_id = random_id;
		}
	}
	
	public enum DotType {
		ELECTRON, HOLE, GENERATION, RECOMBINATION;
	}
	
	public class RenderCanvas extends JPanel {

		private static final long serialVersionUID = 7369516276529576171L;
		public int zoom_bound_x = 0;
		public int zoom_bound_y = 0;
		public int offset_x = 0;
		public int offset_y = 0;
		Color bg;

		Simulation e;

		@Override
		public void paintComponent(Graphics real) {
			draw((Graphics2D)real, getWidth(), getHeight());
		}
		
		@Override
		public void updateUI() {
			super.updateUI();
			Color c = this.getBackground();
			bg = new Color(clamp((int)(0.95*c.getRed())-5, 0, 255), clamp((int)(0.95*c.getGreen())-5, 0, 255), clamp((int)(0.95*c.getBlue())-5, 0, 255));
		}

		public void draw(Graphics2D g, int width, int height) {
			e.rwLock.readLock().lock();
			try {
				g.setBackground(bg);

				if (g.getClipBounds() != null)
					g.clearRect(0, 0, width, height);

				int canvas_x = width;
				int canvas_y = height;

				int xw = e.controls.zoom_i2 - e.controls.zoom_i1 + 1;
				int yw = e.controls.zoom_j2 - e.controls.zoom_j1 + 1;

				if (xw/(double)yw >= canvas_x/(double) canvas_y) {
					int dim2 = (int) (canvas_x*yw/(double)xw);
					zoom_bound_x = canvas_x - 1;
					zoom_bound_y = dim2 - 1;
					offset_x = 0;
					offset_y = (canvas_y - dim2)/2;
				} else {
					int dim2 = (int) (canvas_y*xw/(double)yw);
					zoom_bound_x = dim2 - 1;
					zoom_bound_y = canvas_y - 1;
					offset_x = (canvas_x - dim2)/2;
					offset_y = 0;
				}

				double sf_x = (double)(e.canvas.zoom_bound_x+1)/(e.controls.zoom_i2-e.controls.zoom_i1+1);
				double sf_y = (double)(e.canvas.zoom_bound_y+1)/(e.controls.zoom_j2-e.controls.zoom_j1+1);

				g.drawImage(e.renderer.img_front, (int) ((-e.controls.zoom_i1)*sf_x+e.canvas.offset_x), (int) ((-e.controls.zoom_j1)*sf_y+e.canvas.offset_y), (int) ((e.nx+1-e.controls.zoom_i1)*sf_x+e.canvas.offset_x), (int) ((e.ny+1-e.controls.zoom_j1)*sf_y+e.canvas.offset_y), 
						0, 0, (e.nx+1)*e.renderer.scalefactor-1, (e.ny+1)*e.renderer.scalefactor-1, e.opts);

				e.renderer.drawText((Graphics2D)g);
			} finally {
	        	e.rwLock.readLock().unlock();
	        }
		}

		public RenderCanvas(Simulation w) {
			e = w;
			setPreferredSize(new Dimension(768, 768));
			updateUI();
		}
	}
}
