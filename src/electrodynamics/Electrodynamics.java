// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.jogamp.opengl.util.awt.TextRenderer;

public class Electrodynamics extends TimerTask implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, ActionListener, AdjustmentListener {
	//TODO:
	// Make colors more distinguishable
	// Add more instructions
	// Fix brush location
	// Draw strings in 3d
	// Fix probes
	
	// Probes
	// Adjust vf brightness
	// Fix perspective projection
	
	// Demos:
	// Inductor
	// Capacitor
	// Transformer
	// 
	

	/* Dynamical simulation variables */

	double[][][] Ex;			// x component of E field
	double[][][] Ey;			// y component of E field
	double[][][] Ez;			// z component of E field
	double[][][] Hx;			// x component of B field
	double[][][] Hy;			// y component of B field
	double[][][] Hz;			// z component of B field
	double[][][] Bx;			// x component of B field
	double[][][] By;			// y component of B field
	double[][][] Bz;			// z component of B field
	double[][][] Bx_laplacian;
	double[][][] By_laplacian;
	double[][][] Bz_laplacian;

	double[][][] rho_n;		// Electrons
	double[][][] rho_p;		// Holes
	double[][][] rho_back;	// Background
	double[][][] rho_abs;		// Absorber charge
	double[][][] rho_free;	// Total free charges
	double[][][] mobility_factor;

	double[][][] Jx_n;		// Electron current
	double[][][] Jy_n;
	double[][][] Jz_n;
	double[][][] Jx_p;		// Hole current
	double[][][] Jy_p;
	double[][][] Jz_p;
	double[][][] Jx_abs;		// Absorber current
	double[][][] Jy_abs;
	double[][][] Jz_abs;
	double[][][] Jx_free;		// Total free current
	double[][][] Jy_free;
	double[][][] Jz_free;

	/* Miscellaneous fields used for display */

	//double[][][] Dx;				// Electric displacement field
	//double[][][] Dy;
	//double[][][] Dz;
	//double[][][] Bz;				// B field
	double[][][] Sx;				// Poynting vector
	double[][][] Sy;
	double[][][] Sz;
	double[][][] u;				// EM energy density
	double[][][] phi;				// Electric scalar potential

	double[][][] G;				// Generation rate
	double[][][] F_n;				// Total chemical potential of electrons (quasi-Fermi level minus the electrostatic potential)
	double[][][] F_p;				// Total chemical potential of holes
	double[][][] grad_E0x_n;		// Gradient of chemical energy
	double[][][] grad_E0y_n;
	double[][][] grad_E0z_n;
	double[][][] grad_E0x_p;
	double[][][] grad_E0y_p;
	double[][][] grad_E0z_p;
	double[][][] grad_Fx_n;		// Gradient of total chemical potential
	double[][][] grad_Fy_n;
	double[][][] grad_Fz_n;
	double[][][] grad_Fx_p;
	double[][][] grad_Fy_p;
	double[][][] grad_Fz_p;
	double[][][] Q;				// Heat dissipation rate
	double[][][] S;				// Free energy dissipation rate
	double[][][] F;				// Average total electrochemical potential

	boolean updateMiscFields = false;
	double[][][] debug;
	double[][][] debug2;

	/* Material parameters */

	Material[][][] materials;
	Material[][][] selection;
	Material[][][] clipboard;

	double[][][] F0_n;		// Standard chemical potential of electrons
	double[][][] F0_p;		// Standard chemical potential of holes
	double[][][] E0_n;		// Standard chemical energy of electrons
	double[][][] E0_p;		// Standard chemical energy of holes
	double[][][] K;			// Charge carrier equilibrium constant
	double[][][] E_a;			// Activation energy of recombination
	double[][][] R;			// Recombination rate constant

	double[][][] cmfx_n;		// Chemical-motive force for electrons
	double[][][] cmfy_n;
	double[][][] cmfz_n;

	double[][][] cmfx_p;		// Chemical-motive force for holes
	double[][][] cmfy_p;
	double[][][] cmfz_p;

	double[][][] emfx;		// External electromotive force
	double[][][] emfy;
	double[][][] emfz;
	double[][][] epsx;		// Dielectric constant
	double[][][] epsy;
	double[][][] epsz;
	double[][][] mu_x;		// Relative permeability
	double[][][] mu_y;		// Relative permeability
	double[][][] mu_z;		// Relative permeability

	int[][][] conducting;		// Does the material have partially filled bands
	int[][][] conducting_x;
	int[][][] conducting_y;
	int[][][] conducting_z;

	//double[][][] H_absorptivity_x;
	//double[][][] H_absorptivity_y;
	//double[][][] H_absorptivity_z;
	double[][][] absorptivity_x;
	double[][][] absorptivity_y;
	double[][][] absorptivity_z;

	/* Multigrid Poisson eq solver */

	//int log2_resolution;
	int MG_levels;
	double[][][] MG_rho0;
	double[][][][] MG_rho;
	double[][][][] MG_epsx;
	double[][][][] MG_epsy;
	double[][][][] MG_epsz;
	double[][][] MG_eps_avg;
	double[][][] MG_phi1;
	double[][][] MG_phi2;

	/* Pathfinding */

	int[][][] distance;
	boolean[][][] visited;


	/* Physical constants */

	double eps0 = 8.85e-12;
	double mu0 = 1.257e-6;
	double c = 1/Math.sqrt(eps0*mu0);
	double c_squared = 1/(eps0*mu0);
	double kT = 4.11e-21;
	double beta = 1/kT;
	double e_charge = 1.6e-19;
	double m_electron = 9e-31;


	/* Material properties */

	double mu_electron = 0.1400*1500;					// Electron mobility
	double D_electron = mu_electron/(beta*e_charge);	// Diffusion constant, determined by Einstein relation

	double mu_hole = 0.5*mu_electron;					// Hole mobility, slightly lower than electron
	double D_hole = mu_hole/(beta*e_charge);

	double eVtoJ = e_charge;				// eV to J conversion factor

	double ni_semi = 1e16;					// Semiconductor equilibrium concentration
	double W_semi = 4.7*eVtoJ;				// Semiconductor work function
	double E_b_semi = 1.12*eVtoJ;			// Semiconductor band gap

	double ni_metal = 5e20;				// Metal charge carrier concentration
	double W_metal_default = W_semi;		// Metal work function
	double W_metal_high = W_semi + 0.3*eVtoJ;
	double W_metal_low = W_semi - 0.3*eVtoJ;
	double E_b_metal = E_b_semi;

	double recombination_rate_semi = 1e7/ni_semi;
	double recombination_rate_metal = 1e8/ni_semi;
	double K_semi = ni_semi*ni_semi;

	double recombination_cross_section = 1e-10;
	double arrhenius_prefactor = recombination_cross_section*Math.sqrt(8*kT/(Math.PI*m_electron/2));
	double E_a_semi = -Math.log(recombination_rate_semi/arrhenius_prefactor);
	double E_a_metal = -Math.log(recombination_rate_metal/arrhenius_prefactor);

	double q_n = -e_charge;				// Charge of single electron
	double q_p = e_charge;				// Charge of single hole

	double n_default_doping_concentration = 5e19;
	double p_default_doping_concentration = 5e19;
	double n_light_doping_concentration = 1e19;
	double p_light_doping_concentration = 1e19;
	double n_heavy_doping_concentration = 2.5e20;
	double p_heavy_doping_concentration = 2.5e20;

	double dielectric_eps_r = 25.0;
	double ferromagnet_mu_r = 1000.0;
	double staticcharge_density = 10.0;

	double E_sat = 5e5;					// Maximum electric field before velocity saturates

	int junction_size = 3;


	/* Probes */

	List<VoltageProbe> voltageprobes;
	List<CurrentProbe> currentprobes;
	VoltageProbe ground = null;


	/* Domain parameters */

	double min_width;				// Width, in SI
	double ds;					// Spatial discretization
	double dt;					// Timestep
	double depth = 1e-3;		// Extent of circuit in z-dimension, only used to get reasonable values for current probe
	//int default_resolution = 32;
	int nx;						// Number of grid points in x-dimension
	int ny;						// Number of grid points in y-dimension
	int nz;
	int absorber_width;
	double absorbing_coeff;
	double H_dissipation;
	double dt_maximum;

	double time = 0.0;
	long stepnumber = 0;
	long frame = 0;
	int lastsimspeed = 0;

	double error_detection_threshold = 1e-5;
	boolean sign_violation = false;


	/* Multithreading */

	int n_threads = Runtime.getRuntime().availableProcessors();
	//int n_threads = 5;
	CyclicBarrier start_barrier = new CyclicBarrier(n_threads + 1);
	CyclicBarrier stop_barrier = new CyclicBarrier(n_threads + 1);
	CyclicBarrier mid_barrier = new CyclicBarrier(n_threads);
	ArrayList<SimulationThread> sim_threads = new ArrayList<SimulationThread>();


	/* Graphics */

	String sim_name = "Brandon's 3D circuit simulator";
	RenderCanvas r;
	MainWindow opts;
	HelpDialog help;
	BufferedImage screen;
	float[][][] image_r;
	float[][][] image_g;
	float[][][] image_b;
	float col_r = 0;
	float col_g = 0;
	float col_b = 0;
	double alphaBG = 0;
	double alphaFG = 0;
	boolean slice_x = false;
	boolean slice_y = false;
	boolean slice_z = true;
	Random rand = new Random();

	ArrayList<Text> texts = new ArrayList<Text>();
	Font bigfont = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
	Font regularfont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	int scalefactor;
	int imgwidth = 0;
	int imgheight = 0;
	int targetframerate = 60;
	int frameduration = 1000/targetframerate;

	/* 3D graphics */
	Renderer renderer;
	boolean threeD_mode = true;

	/* Performance profiling */

	Timer t4 = new Timer("Poisson constraint solver", true);
	Timer t7 = new Timer("Poisson potential solver", true);
	Timer t5 = new Timer("Graphics", true);
	Timer t6 = new Timer("Iterate simulation", true);
	Timer t8 = new Timer("Calc misc fields", true);
	Timer t9 = new Timer("Debug", false);


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


	/* Saving and loading */

	public static File infile;
	public static File outfile;
	int saveversion = 1;
	String fileextension = ".semisim";
	String startingpath = ".";


	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(
			UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		Electrodynamics w = new Electrodynamics();
		//java.util.Timer t = new java.util.Timer();
		javax.swing.Timer t = new javax.swing.Timer(w.frameduration, w);
		for (int i = 0; i < w.n_threads; i++) {
			w.sim_threads.add(w.new SimulationThread(i, w.n_threads, w.nx));
		}

		for (int i = 0; i < w.n_threads; i++) {
			w.sim_threads.get(i).start();
		}

		//t.schedule(w, 0, w.frameduration);
		w.renderer.animator.start();
		t.start();

	}

	public Electrodynamics() {

		opts = new MainWindow();
		r = new RenderCanvas(this);
		r.setFocusable(true);

		detect64Bit();

		initializeGrid(0.1e-6, 32, 32, 32);
		renderer = new Renderer(this);
		//opts.add(r, BorderLayout.CENTER);
		opts.add(renderer.canvas, BorderLayout.CENTER);
		r.addMouseListener(this);
		r.addMouseMotionListener(this);
		r.addMouseWheelListener(this);
		r.addKeyListener(this);

		renderer.canvas.addMouseListener(this);
		renderer.canvas.addMouseMotionListener(this);
		renderer.canvas.addMouseWheelListener(this);
		renderer.canvas.addKeyListener(this);

		opts.gui_reset.addActionListener(this);
		opts.gui_resetall.addActionListener(this);
		opts.gui_save.addActionListener(this);
		opts.gui_open.addActionListener(this);
		opts.gui_help.addActionListener(this);
		opts.gui_editdesc.addActionListener(this);
		opts.gui_view.addActionListener(this);
		opts.gui_view_vec.addActionListener(this);
		opts.gui_brush.addActionListener(this);
		opts.gui_material.addActionListener(this);
		opts.gui_slice.addAdjustmentListener(this);
		opts.gui_3d_view.addActionListener(this);
		
		opts.gui_slice.setVisible(false);
		opts.gui_slicelabel.setVisible(false);

		opts.gui_material.removeItem(MaterialType.ABSORBER);
		//opts.gui_material.removeItem(MaterialType.SWITCH);

		//opts.gui_view.removeItem(ScalarView.DEBUG);

		opts.pack();

		addKeyBinds(r);
		addKeyBinds(this.opts.contentPane);

		InputMap im = (InputMap)UIManager.get("Button.focusInputMap");
		im.put(KeyStroke.getKeyStroke("pressed SPACE"), "none");
		im.put(KeyStroke.getKeyStroke("released SPACE"), "none");

		//help = new HelpDialog();
		//help.setVisible(false);
	}

	public void detect64Bit() {
		if (!System.getProperty("sun.arch.data.model").equals("64"))
		{
			int result = JOptionPane.showConfirmDialog(opts, "Running this application on a 32-bit platform may cause some issues. Do you still wish to proceed?", "Warning", JOptionPane.YES_NO_OPTION);
			if (result != JOptionPane.OK_OPTION)
			{
				System.exit(0);
			}
		}
	}

	public void run() {
		try {
			int iterationmultiplier = opts.gui_simspeed_2.getValue();

			if (clear) {
				resetFields(false);
				multigridSolve(true, false);
				time = 0.0;
				clear = false;
			}

			if (reset) {
				int result = JOptionPane.showConfirmDialog(opts, "Do you wish to reset the entire simulation?", "Reset", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.OK_OPTION)
				{
					resetFields(true);
					time = 0.0;
				}
				reset = false;
			}

			if (opts.gui_simspeed.getValue() != lastsimspeed) {
				lastsimspeed = opts.gui_simspeed.getValue();
				dt = dt_maximum*(lastsimspeed/20.0);
			}

			handleMouseInput();

			if (!opts.gui_paused.isSelected() || advanceframe) {
				for (int i = 0; i < iterationmultiplier ; i++) {
					start_barrier.await();
					stop_barrier.await();
				}

				if (frame % 2 == 0)
					calcMiscFields(true);
				else
					calcMiscFields(false);

				frame++;
			} else if (updateMiscFields) {
				calcMiscFields(true);
			}


			if (save) {
				writeFile();
				save = false;
			}

			if (load) {
				readFile();
				load = false;
			}

			if (!threeD_mode)
				r.repaint();
			else
				render();
			
		} catch (Exception e) {
			JOptionPane.showConfirmDialog(opts, e.getMessage(), "Error", JOptionPane.OK_OPTION);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void initializeGrid(double ds, int x_resolution, int y_resolution, int z_resolution) {
		this.ds = ds;
		nx = x_resolution;
		ny = y_resolution;
		nz = z_resolution;

		int min_res = Math.min(Math.min(nx, ny), nz);
		min_width = min_res*ds;
		MG_levels = (int)Math.round(Math.log(min_res/4)/Math.log(2))+1;

		dt_maximum = 0.9*ds/(Math.sqrt(3)*c);

		H_dissipation = 0.001*ds*ds/dt_maximum;

		Ex = new double[nx][ny][nz];
		Ey = new double[nx][ny][nz];
		Ez = new double[nx][ny][nz];
		Hx = new double[nx][ny][nz];
		Hy = new double[nx][ny][nz];
		Hz = new double[nx][ny][nz];
		Bx = new double[nx][ny+1][nz+1];
		By = new double[nx+1][ny][nz+1];
		Bz = new double[nx+1][ny+1][nz];
		Bx_laplacian = new double[nx][ny][nz];
		By_laplacian = new double[nx][ny][nz];
		Bz_laplacian = new double[nx][ny][nz];
		rho_abs = new double[nx][ny][nz];
		rho_n = new double[nx][ny][nz];
		rho_p = new double[nx][ny][nz];
		rho_back = new double[nx][ny][nz];
		rho_free = new double[nx][ny][nz];
		mobility_factor = new double[nx][ny][nz];

		Jx_abs = new double[nx][ny][nz];
		Jy_abs = new double[nx][ny][nz];
		Jz_abs = new double[nx][ny][nz];
		Jx_n = new double[nx][ny][nz];
		Jy_n = new double[nx][ny][nz];
		Jz_n = new double[nx][ny][nz];
		Jx_p = new double[nx][ny][nz];
		Jy_p = new double[nx][ny][nz];
		Jz_p = new double[nx][ny][nz];
		Jx_free = new double[nx][ny][nz];
		Jy_free = new double[nx][ny][nz];
		Jz_free = new double[nx][ny][nz];

		materials = new Material[nx][ny][nz];
		selection = new Material[nx][ny][nz];
		clipboard = new Material[nx][ny][nz];
		K = new double[nx][ny][nz];
		F0_n = new double[nx][ny][nz];
		F0_p = new double[nx][ny][nz];
		F_n = new double[nx][ny][nz];
		F_p = new double[nx][ny][nz];
		E0_n = new double[nx][ny][nz];
		E0_p = new double[nx][ny][nz];
		E_a = new double[nx][ny][nz];
		R = new double[nx][ny][nz];
		cmfx_n = new double[nx][ny][nz];
		cmfy_n = new double[nx][ny][nz];
		cmfz_n = new double[nx][ny][nz];
		cmfx_p = new double[nx][ny][nz];
		cmfy_p = new double[nx][ny][nz];
		cmfz_p = new double[nx][ny][nz];
		conducting = new int[nx][ny][nz];
		conducting_x = new int[nx][ny][nz];
		conducting_y = new int[nx][ny][nz];
		conducting_z = new int[nx][ny][nz];
		//H_absorptivity_x = new double[nx][ny][nz];
		//H_absorptivity_y = new double[nx][ny][nz];
		//H_absorptivity_z = new double[nx][ny][nz];
		absorptivity_x = new double[nx][ny][nz];
		absorptivity_y = new double[nx][ny][nz];
		absorptivity_z = new double[nx][ny][nz];
		emfx = new double[nx][ny][nz];
		emfy = new double[nx][ny][nz];
		emfz = new double[nx][ny][nz];
		epsx = new double[nx][ny][nz];
		epsy = new double[nx][ny][nz];
		epsz = new double[nx][ny][nz];
		mu_x = new double[nx][ny][nz];
		mu_y = new double[nx][ny][nz];
		mu_z = new double[nx][ny][nz];

		Sx = new double[nx][ny][nz];
		Sy = new double[nx][ny][nz];
		Sz = new double[nx][ny][nz];
		u = new double[nx][ny][nz];
		phi = new double[nx][ny][nz];

		G = new double[nx][ny][nz];
		F_n = new double[nx][ny][nz];
		F_p = new double[nx][ny][nz];
		grad_E0x_n = new double[nx][ny][nz];
		grad_E0y_n = new double[nx][ny][nz];
		grad_E0z_n = new double[nx][ny][nz];
		grad_E0x_p = new double[nx][ny][nz];
		grad_E0y_p = new double[nx][ny][nz];
		grad_E0z_p = new double[nx][ny][nz];
		grad_Fx_n = new double[nx][ny][nz];
		grad_Fy_n = new double[nx][ny][nz];
		grad_Fz_n = new double[nx][ny][nz];
		grad_Fx_p = new double[nx][ny][nz];
		grad_Fy_p = new double[nx][ny][nz];
		grad_Fz_p = new double[nx][ny][nz];
		Q = new double[nx][ny][nz];
		S = new double[nx][ny][nz];
		F = new double[nx][ny][nz];
		debug = new double[nx][ny][nz];
		debug2 = new double[nx][ny][nz];


		MG_rho0 = new double[nx][ny][nz];
		MG_rho = new double[MG_levels][nx][ny][nz];
		MG_epsx = new double[MG_levels][nx][ny][nz];
		MG_epsy = new double[MG_levels][nx][ny][nz];
		MG_epsz = new double[MG_levels][nx][ny][nz];
		MG_eps_avg = new double[nx][ny][nz];
		MG_phi1 = new double[nx][ny][nz];
		MG_phi2 = new double[nx][ny][nz];

		distance = new int[nx][ny][nz];
		visited = new boolean[nx][ny][nz];

		under_brush = new boolean[nx][ny][nz];
		selected = new boolean[nx][ny][nz];
		selected_EMF = new boolean[nx][ny][nz];

		voltageprobes = new CopyOnWriteArrayList<VoltageProbe>();		
		currentprobes = new CopyOnWriteArrayList<CurrentProbe>();

		resetFields(true);
		multigridSolve(true, false);
		calcMiscFields(true);

		text_x = 0;
		text_y = 0;
		texting = false;

		image_r = new float[nx][ny][nz];
		image_g = new float[nx][ny][nz];
		image_b = new float[nx][ny][nz];
		scalefactor = (int)(768.0/Math.max(Math.max(nx, ny), nz));

		opts.setVisible(true);

		generateCanvas();
	}

	public void generateCanvas() {
		if (slice_x) {
			imgwidth = (int)Math.ceil(scalefactor*ny);
			imgheight = (int)Math.ceil(scalefactor*nz);
			opts.gui_slice.setMaximum(opts.gui_slice.getVisibleAmount() + nx - 1);
		} else if (slice_y) {
			imgwidth = (int)Math.ceil(scalefactor*nx);
			imgheight = (int)Math.ceil(scalefactor*nz);
			opts.gui_slice.setMaximum(opts.gui_slice.getVisibleAmount() + ny - 1);
		} else if (slice_z) {
			imgwidth = (int)Math.ceil(scalefactor*nx);
			imgheight = (int)Math.ceil(scalefactor*ny);
			opts.gui_slice.setMaximum(opts.gui_slice.getVisibleAmount() + nz - 1);
		}
		
		r.setPreferredSize(new Dimension(imgwidth, imgheight));
		screen = (BufferedImage) opts.createImage(imgwidth, imgheight);
	}

	public void resetFields(boolean resetall) {

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{

					if (resetall) {
						if (materials[i][j][k] == null)
							materials[i][j][k] = new Material();
						else
							materials[i][j][k].erase();

						if (selection[i][j][k] == null)
							selection[i][j][k] = new Material();
						else
							selection[i][j][k].erase();

						if (clipboard[i][j][k] == null)
							clipboard[i][j][k] = new Material();

						F0_n[i][j][k] = 0;
						F0_p[i][j][k] = 0;
						F_n[i][j][k] = 0;
						F_p[i][j][k] = 0;
						E0_n[i][j][k] = 0;
						E0_p[i][j][k] = 0;
						K[i][j][k] = 0;
						E_a[i][j][k] = 0;
						R[i][j][k] = 0;

						cmfx_n[i][j][k] = 0;
						cmfy_n[i][j][k] = 0;
						cmfz_n[i][j][k] = 0;
						cmfx_p[i][j][k] = 0;
						cmfy_p[i][j][k] = 0;
						cmfz_p[i][j][k] = 0;

						emfx[i][j][k] = 0.0;
						emfy[i][j][k] = 0.0;
						emfz[i][j][k] = 0.0;

						epsx[i][j][k] = eps0;
						epsy[i][j][k] = eps0;
						mu_z[i][j][k] = mu0;

						conducting[i][j][k] = 0;
						conducting_x[i][j][k] = 0;
						conducting_y[i][j][k] = 0;
						conducting_z[i][j][k] = 0;

						//H_absorptivity_x[i][j][k] = 0;
						//H_absorptivity_y[i][j][k] = 0;
						//H_absorptivity_z[i][j][k] = 0;
						absorptivity_x[i][j][k] = 0;
						absorptivity_y[i][j][k] = 0;
						absorptivity_z[i][j][k] = 0;
					}

					Ex [i][j][k] = 0.0;
					Ey [i][j][k] = 0.0;
					Ez [i][j][k] = 0.0;
					Hx [i][j][k] = 0.0;
					Hy [i][j][k] = 0.0;
					Hz [i][j][k] = 0.0;
					Bx [i][j][k] = 0.0;
					Bx [i][j+1][k] = 0.0;
					Bx [i][j][k+1] = 0.0;
					Bx [i][j+1][k+1] = 0.0;
					By [i][j][k] = 0.0;
					By [i+1][j][k] = 0.0;
					By [i][j][k+1] = 0.0;
					By [i+1][j][k+1] = 0.0;
					Bz [i][j][k] = 0.0;
					Bz [i+1][j][k] = 0.0;
					Bz [i][j+1][k] = 0.0;
					Bz [i+1][j+1][k] = 0.0;
					Bx_laplacian [i][j][k] = 0.0;
					By_laplacian [i][j][k] = 0.0;
					Bz_laplacian [i][j][k] = 0.0;

					rho_abs[i][j][k] = 0.0;
					rho_n[i][j][k] = 0.0;
					rho_p[i][j][k] = 0.0;
					rho_back[i][j][k] = 0.0;
					rho_free[i][j][k] = 0.0;
					mobility_factor[i][j][k] = 0.0;

					Jx_abs[i][j][k] = 0.0;
					Jy_abs[i][j][k] = 0.0;
					Jz_abs[i][j][k] = 0.0;
					Jx_n[i][j][k] = 0.0;
					Jy_n[i][j][k] = 0.0;
					Jz_n[i][j][k] = 0.0;
					Jx_p[i][j][k] = 0.0;
					Jy_p[i][j][k] = 0.0;
					Jz_p[i][j][k] = 0.0;
					Jx_free[i][j][k] = 0.0;
					Jy_free[i][j][k] = 0.0;
					Jz_free[i][j][k] = 0.0;

					MG_phi1 [i][j][k] = 0.0;
					MG_phi2 [i][j][k] = 0.0;

					distance[i][j][k] = Integer.MAX_VALUE;
					visited[i][j][k] = false;

					Sx[i][j][k] = 0.0;
					Sy[i][j][k] = 0.0;
					Sz[i][j][k] = 0.0;
					u[i][j][k] = 0.0;
					phi[i][j][k] = 0.0;

					G[i][j][k] = 0.0;
					F_n[i][j][k] = 0.0;
					F_p[i][j][k] = 0.0;
					grad_E0x_n[i][j][k] = 0.0;
					grad_E0y_n[i][j][k] = 0.0;
					grad_E0z_n[i][j][k] = 0.0;
					grad_E0x_p[i][j][k] = 0.0;
					grad_E0y_p[i][j][k] = 0.0;
					grad_E0z_p[i][j][k] = 0.0;
					grad_Fx_n[i][j][k] = 0.0;
					grad_Fy_n[i][j][k] = 0.0;
					grad_Fz_n[i][j][k] = 0.0;
					grad_Fx_p[i][j][k] = 0.0;
					grad_Fy_p[i][j][k] = 0.0;
					grad_Fz_p[i][j][k] = 0.0;
					Q[i][j][k] = 0.0;
					S[i][j][k] = 0.0;
					F[i][j][k] = 0.0;
					debug[i][j][k] = 0.0;
					debug2[i][j][k] = 0.0;

					selected[i][j][k] = false;
					selected_EMF[i][j][k] = false;
				}
			}
		}

		if (resetall) {
			voltageprobes.clear();
			currentprobes.clear();
			ground = null;

			opts.setTitle(sim_name);
		}


		constructBoundary();

		initializeAllMaterials();
		updateAllMaterials();
		checkCFL();
	}

	public void constructBoundary() {
		absorber_width = 2;
		absorbing_coeff = 50*c/this.min_width;
		double max_stretch = 10;
		double coeff = Math.log(max_stretch);

		if ((BoundaryCondition)opts.gui_bc.getSelectedItem() == BoundaryCondition.DISSIPATIVE) {
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						double dx = (double)Math.max(0, absorber_width-Math.min(i, nx-1-i))/absorber_width;
						double dy = (double)Math.max(0, absorber_width-Math.min(j, ny-1-j))/absorber_width;
						double dz = (double)Math.max(0, absorber_width-Math.min(k, nz-1-k))/absorber_width;
						double depth = Math.sqrt(dx*dx+dy*dy+dz*dz);
						if (depth > 0) {
							double stretchfactor = Math.exp(coeff*depth);

							materials[i][j][k].erase();
							materials[i][j][k].type = MaterialType.ABSORBER;
							materials[i][j][k].eps_r = stretchfactor;
							materials[i][j][k].mu_r = stretchfactor;
							materials[i][j][k].absorptivity = 1;
						}
					}
				}
			}
		} else {
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						if (materials[i][j][k].type == MaterialType.ABSORBER) {
							materials[i][j][k].erase();
						}
					}
				}
			}
		}
	}

	class SimulationThread extends Thread {

		int i_min;
		int i_max;
		int n_thread;

		public SimulationThread(int n, int n_threads, int nx) {
			i_min = (n*nx)/n_threads;
			i_max = (n+1)*nx/n_threads-1;
			n_thread = n;
			System.out.println("Thread " + n_thread + ": " + i_min + " < i <= " + i_max);
		}

		@Override
		public void run() {
			try {
				while (true) {
					start_barrier.await();

					if (n_thread == 0) {
						t6.start();

						stepnumber++;
					}
					
					for (int i = 0; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 0; j < ny-1; j++)
							{
								for (int k = 1; k < nz-1; k++)
								{
									Bz_laplacian[i][j][k] = (Bz[i+2][j+1][k]+Bz[i+1][j+2][k]+Bz[i+1][j+1][k+1]+Bz[i][j+1][k]+Bz[i+1][j][k]+Bz[i+1][j+1][k-1]-6*Bz[i+1][j+1][k])/(ds*ds);
								}
							}
						}
					}

					for (int i = 1; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 0; j < ny-1; j++)
							{
								for (int k = 0; k < nz-1; k++)
								{
									Bx_laplacian[i][j][k] = (Bx[i+1][j+1][k+1]+Bx[i][j+2][k+1]+Bx[i][j+1][k+2]+Bx[i-1][j+1][k+1]+Bx[i][j][k+1]+Bx[i][j+1][k]-6*Bx[i][j+1][k+1])/(ds*ds);
								}
							}
						}
					}

					for (int i = 0; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 1; j < ny-1; j++)
							{
								for (int k = 0; k < nz-1; k++)
								{
									By_laplacian[i][j][k] = (By[i+2][j][k+1]+By[i+1][j+1][k+1]+By[i+1][j][k+2]+By[i][j][k+1]+By[i+1][j-1][k+1]+By[i+1][j][k]-6*By[i+1][j][k+1])/(ds*ds);
								}
							}
						}
					}

					mid_barrier.await();

					for (int i = 1; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 0; j < ny-1; j++)
							{
								for (int k = 0; k < nz-1; k++)
								{
									/*double sigma = H_absorptivity_x[i][j][k]*mu_x[i][j][k]*absorbing_coeff;

									Hx[i][j][k] = (Hx[i][j][k]*(1-0.5*dt*sigma/mu_x[i][j][k])
									+ (-(Ez[i][j+1][k] - Ez[i][j][k]) + (Ey[i][j][k+1] - Ey[i][j][k]))*dt/(ds*mu_x[i][j][k])
									+ H_dissipation*dt*Hx_laplacian[i][j][k]/mu_x[i][j][k])
									/(1+0.5*dt*sigma/mu_x[i][j][k]);*/

									Hx[i][j][k] = Hx[i][j][k] + (-(Ez[i][j+1][k] - Ez[i][j][k]) + (Ey[i][j][k+1] - Ey[i][j][k]))*dt/(ds*mu_x[i][j][k])
									+ H_dissipation*dt*Bx_laplacian[i][j][k]/mu_x[i][j][k];
									
									Bx[i][j+1][k+1] = Hx[i][j][k]*mu_x[i][j][k];
								}
							}
						}
					}

					for (int i = 0; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 1; j < ny-1; j++)
							{
								for (int k = 0; k < nz-1; k++)
								{
									/*double sigma = H_absorptivity_y[i][j][k]*mu_y[i][j][k]*absorbing_coeff;

									Hy[i][j][k] = (Hy[i][j][k]*(1-0.5*dt*sigma/mu_y[i][j][k])
									+ (-(Ex[i][j][k+1] - Ex[i][j][k]) + (Ez[i+1][j][k] - Ez[i][j][k]))*dt/(ds*mu_y[i][j][k])
									+ H_dissipation*dt*Hy_laplacian[i][j][k]/mu_y[i][j][k])
									/(1+0.5*dt*sigma/mu_y[i][j][k]);*/

									Hy[i][j][k] = Hy[i][j][k] + (-(Ex[i][j][k+1] - Ex[i][j][k]) + (Ez[i+1][j][k] - Ez[i][j][k]))*dt/(ds*mu_y[i][j][k])
									+ H_dissipation*dt*By_laplacian[i][j][k]/mu_y[i][j][k];

									By[i+1][j][k+1] = Hy[i][j][k]*mu_y[i][j][k];
								}
							}
						}
					}

					for (int i = 0; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 0; j < ny-1; j++)
							{
								for (int k = 1; k < nz-1; k++)
								{
									/*double sigma = H_absorptivity_z[i][j][k]*mu_z[i][j][k]*absorbing_coeff;

									Hz[i][j][k] = (Hz[i][j][k]*(1-0.5*dt*sigma/mu_z[i][j][k])
									+ (-(Ey[i+1][j][k] - Ey[i][j][k]) + (Ex[i][j+1][k] - Ex[i][j][k]))*dt/(ds*mu_z[i][j][k])
									+ H_dissipation*dt*Hz_laplacian[i][j][k]/mu_z[i][j][k])
									/(1+0.5*dt*sigma/mu_z[i][j][k]);*/

									Hz[i][j][k] = Hz[i][j][k] + (-(Ey[i+1][j][k] - Ey[i][j][k]) + (Ex[i][j+1][k] - Ex[i][j][k]))*dt/(ds*mu_z[i][j][k])
									+ H_dissipation*dt*Bz_laplacian[i][j][k]/mu_z[i][j][k];

									Bz[i+1][j+1][k] = Hz[i][j][k]*mu_z[i][j][k];
								}
							}
						}
					}


					for (int i = 1; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 1; j < ny-1; j++)
							{	
								for (int k = 1; k < nz - 1; k++)
								{
									double generation_rate = conducting[i][j][k]*R[i][j][k]*(K[i][j][k] - rho_n[i][j][k]*rho_p[i][j][k]/(q_n*q_p));

									rho_n[i][j][k] = rho_n[i][j][k] - (Jx_n[i][j][k]-Jx_n[i-1][j][k] + Jy_n[i][j][k]-Jy_n[i][j-1][k] + Jz_n[i][j][k] - Jz_n[i][j][k-1])*dt/ds + dt*q_n*generation_rate;
									rho_p[i][j][k] = rho_p[i][j][k] - (Jx_p[i][j][k]-Jx_p[i-1][j][k] + Jy_p[i][j][k]-Jy_p[i][j-1][k] + Jz_p[i][j][k] - Jz_p[i][j][k-1])*dt/ds + dt*q_p*generation_rate;
									rho_abs[i][j][k] = rho_abs[i][j][k] - (Jx_abs[i][j][k]-Jx_abs[i-1][j][k] + Jy_abs[i][j][k]-Jy_abs[i][j-1][k] + Jz_abs[i][j][k]-Jz_abs[i][j][k-1])*dt/ds;

									rho_free[i][j][k] = rho_abs[i][j][k]+rho_n[i][j][k]+rho_p[i][j][k]+rho_back[i][j][k];

									double E_avg = Math.sqrt(0.5*(Ex[i][j][k]*Ex[i][j][k] + Ex[i-1][j][k]*Ex[i-1][j][k] + Ey[i][j][k]*Ey[i][j][k] + Ey[i][j-1][k]*Ey[i][j-1][k] + Ez[i][j][k]*Ez[i][j][k] + Ez[i][j][k-1]*Ez[i][j][k-1]));
									mobility_factor[i][j][k] = Math.min(1, E_sat/E_avg);
								}
							}
						}
					}

					mid_barrier.await();

					for (int i = 0; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 1; j < ny-1; j++)
							{

								for (int k = 1; k < nz-1; k++)
								{
									double ex_prev = Ex[i][j][k];

									double mf = Math.min(mobility_factor[i+1][j][k], mobility_factor[i][j][k]);
									//double mobility_factor = Math.min(1, E_sat/Math.abs(Ex[i][j][k]));

									double sigma_n = conducting_x[i][j][k]*mf*mu_electron*logmean(-rho_n[i+1][j][k],-rho_n[i][j][k]);
									double sigma_p = conducting_x[i][j][k]*mf*mu_hole*logmean(rho_p[i+1][j][k], rho_p[i][j][k]);

									Jx_abs[i][j][k] = 0;

									Jx_n[i][j][k] = conducting_x[i][j][k]*(-mf*D_electron*(rho_n[i+1][j][k] - rho_n[i][j][k])/ds
									+ sigma_n*(emfx[i][j][k] + cmfx_n[i][j][k]/q_n));

									Jx_p[i][j][k] = conducting_x[i][j][k]*(-mf*D_hole*(rho_p[i+1][j][k] - rho_p[i][j][k])/ds
									+ sigma_p*(emfx[i][j][k] + cmfx_p[i][j][k]/q_p));

									double sigma = sigma_n + sigma_p + absorptivity_x[i][j][k]*epsx[i][j][k]*absorbing_coeff;
									double jx = Jx_abs[i][j][k] + Jx_n[i][j][k] + Jx_p[i][j][k];

									Ex[i][j][k] = (Ex[i][j][k]*(1-0.5*dt*sigma/epsx[i][j][k]) + (((Hz[i][j][k] - Hz[i][j-1][k]) - (Hy[i][j][k] - Hy[i][j][k-1]))/ds - jx)*dt/epsx[i][j][k])
									/(1+0.5*dt*sigma/epsx[i][j][k]);

									Jx_abs[i][j][k] += 0.5*(absorptivity_x[i][j][k]*epsx[i][j][k]*absorbing_coeff)*(ex_prev + Ex[i][j][k]);
									Jx_n[i][j][k] += 0.5*sigma_n*(ex_prev + Ex[i][j][k]);
									Jx_p[i][j][k] += 0.5*sigma_p*(ex_prev + Ex[i][j][k]);
								}
							}
						}
					}

					for (int i = 1; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 0; j < ny-1; j++)
							{
								for (int k = 1; k < nz-1; k++)
								{
									double ey_prev = Ey[i][j][k];

									double mf = Math.min(mobility_factor[i][j+1][k], mobility_factor[i][j][k]);
									//double mobility_factor = Math.min(1, E_sat/Math.abs(Ey[i][j][k]));

									double sigma_n = conducting_y[i][j][k]*mf*mu_electron*logmean(-rho_n[i][j+1][k],-rho_n[i][j][k]);
									double sigma_p = conducting_y[i][j][k]*mf*mu_hole*logmean(rho_p[i][j+1][k], rho_p[i][j][k]);

									Jy_abs[i][j][k] = 0;

									Jy_n[i][j][k] = conducting_y[i][j][k]*(-mf*D_electron*(rho_n[i][j+1][k] - rho_n[i][j][k])/ds
									+ sigma_n*(emfy[i][j][k] + cmfy_n[i][j][k]/q_n));

									Jy_p[i][j][k] = conducting_y[i][j][k]*(-mf*D_hole*(rho_p[i][j+1][k] - rho_p[i][j][k])/ds
									+ sigma_p*(emfy[i][j][k] + cmfy_p[i][j][k]/q_p));

									double sigma = sigma_n + sigma_p + absorptivity_y[i][j][k]*epsy[i][j][k]*absorbing_coeff;
									double jy = Jy_abs[i][j][k] + Jy_n[i][j][k] + Jy_p[i][j][k];

									Ey[i][j][k] = (Ey[i][j][k]*(1-0.5*dt*sigma/epsy[i][j][k]) + (((Hx[i][j][k] - Hx[i][j][k-1]) - (Hz[i][j][k] - Hz[i-1][j][k]))/ds - jy)*dt/epsy[i][j][k])
									/(1+0.5*dt*sigma/epsy[i][j][k]);

									Jy_abs[i][j][k] += 0.5*(absorptivity_y[i][j][k]*epsy[i][j][k]*absorbing_coeff)*(ey_prev + Ey[i][j][k]);
									Jy_n[i][j][k] += 0.5*sigma_n*(ey_prev + Ey[i][j][k]);
									Jy_p[i][j][k] += 0.5*sigma_p*(ey_prev + Ey[i][j][k]);
								}
							}
						}
					}

					for (int i = 1; i < nx-1; i++)
					{
						if (i >= i_min && i <= i_max) {
							for (int j = 1; j < ny-1; j++)
							{
								for (int k = 0; k < nz-1; k++)
								{
									double ez_prev = Ez[i][j][k];

									double mf = Math.min(mobility_factor[i][j][k+1], mobility_factor[i][j][k]);
									//double mobility_factor = Math.min(1, E_sat/Math.abs(Ey[i][j][k]));

									double sigma_n = conducting_z[i][j][k]*mf*mu_electron*logmean(-rho_n[i][j][k+1],-rho_n[i][j][k]);
									double sigma_p = conducting_z[i][j][k]*mf*mu_hole*logmean(rho_p[i][j][k+1], rho_p[i][j][k]);

									Jz_abs[i][j][k] = 0;

									Jz_n[i][j][k] = conducting_z[i][j][k]*(-mf*D_electron*(rho_n[i][j][k+1] - rho_n[i][j][k])/ds
									+ sigma_n*(emfz[i][j][k] + cmfz_n[i][j][k]/q_n));

									Jz_p[i][j][k] = conducting_z[i][j][k]*(-mf*D_hole*(rho_p[i][j][k+1] - rho_p[i][j][k])/ds
									+ sigma_p*(emfz[i][j][k] + cmfz_p[i][j][k]/q_p));


									double sigma = sigma_n + sigma_p + absorptivity_z[i][j][k]*epsz[i][j][k]*absorbing_coeff;
									double jz = Jz_abs[i][j][k] + Jz_n[i][j][k] + Jz_p[i][j][k];

									Ez[i][j][k] = (Ez[i][j][k]*(1-0.5*dt*sigma/epsz[i][j][k]) + (((Hy[i][j][k] - Hy[i-1][j][k]) - (Hx[i][j][k] - Hx[i][j-1][k]))/ds - jz)*dt/epsz[i][j][k])
									/(1+0.5*dt*sigma/epsz[i][j][k]);

									Jz_abs[i][j][k] += 0.5*(absorptivity_z[i][j][k]*epsz[i][j][k]*absorbing_coeff)*(ez_prev + Ez[i][j][k]);
									Jz_n[i][j][k] += 0.5*sigma_n*(ez_prev + Ez[i][j][k]);
									Jz_p[i][j][k] += 0.5*sigma_p*(ez_prev + Ez[i][j][k]);
								}
							}
						}
					}

					if (n_thread == 0) {


						for (int i = 1; i < nx-1; i++)
						{
							for (int j = 0; j < ny-1; j++)
							{
								Bx[i][j+1][0] = Bx[i][j+1][1];
								Bx[i][j+1][nz] = Bx[i][j+1][nz-1];
							}
							for (int k = 0; k < nz-1; k++)
							{
								Bx[i][0][k+1] = Bx[i][1][k+1];
								Bx[i][ny][k+1] = Bx[i][ny-1][k+1];
							}
						}
						
						for (int j = 1; j < ny-1; j++)
						{
							for (int i = 0; i < nx-1; i++)
							{
								By[i+1][j][0] = By[i+1][j][1];
								By[i+1][j][nz] = By[i+1][j][nz-1];
							}
							for (int k = 0; k < nz-1; k++)
							{
								By[0][j][k+1] = By[1][j][k+1];
								By[nx][j][k+1] = By[nx-1][j][k+1];
							}
						}
						
						for (int k = 1; k < nz-1; k++)
						{
							for (int j = 0; j < ny-1; j++)
							{
								Bz[0][j+1][k] = Bz[1][j+1][k];
								Bz[nx][j+1][k] = Bz[nx-1][j+1][k];
							}
							for (int i = 0; i < nx-1; i++)
							{
								Bz[i+1][0][k] = Bz[i+1][1][k];
								Bz[i+1][ny][k] = Bz[i+1][ny-1][k];
							}
						}

						t6.stop();

						if (stepnumber%500 == 0) {
							multigridSolve(true, false);
						}

						time += dt;

						advanceframe = false;
					}

					stop_barrier.await();
				}
			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
	}

	public double logmean(double x, double y)
	{
		if (x <= 0 || y <= 0)
			return 0;

		//My approximation
		if (Math.abs((x-y)/(x+y)) <  1e-3)
			return (2/3.0)*Math.sqrt(x*y) + (1/6.0)*(x+y);

		//return (x-y)/lut.log(x/y);
		return (x-y)/Math.log(x/y);
	}

	LogLUT lut = new LogLUT();

	// Quick and dirty way to calculate log precisely (relative error < 10^-9)
	class LogLUT {
		//int min_exponent = -1022;
		//int orders = 2045;
		int min_exponent = -100;
		int orders = 200;
		//int divisions = 4;
		int length;
		long A = (long)0b1111111111 << 52;

		double[] x;
		double[] log_x;

		public LogLUT() {
			length = orders*4;
			x = new double[length];
			log_x = new double[length];

			for (int i = 0; i < length; i++) {
				x[i] = Math.pow(2, min_exponent+i/4)*(1+(i%4)/4.0);
				log_x[i] = Math.log(x[i]);
			}
		}

		public double log(double y) {
			int exp = Math.getExponent(y);
			double mantissa = Double.longBitsToDouble(A|Double.doubleToRawLongBits(y)&(~0x7ff0000000000000l));
			int i = (int)(((exp-min_exponent)<<2) + 4*mantissa - 3.5);
			double w = y/x[i];
			return (w-1)/(0.66666666666666667*Math.sqrt(w) + 0.16666666666666667*(w+1)) + log_x[i];
		}
	}

	public void updateAllMaterials() {		
		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					mu_x[i][j][k] = 0.25*mu0*(materials[i][j][k].mu_r+materials[i][j][k+1].mu_r+materials[i][j+1][k].mu_r+materials[i][j+1][k+1].mu_r);
					//H_absorptivity_z[i][j][k] = 0*0.25*(materials[i][j][k].absorptivity+materials[i][j][k+1].absorptivity+materials[i][j+1][k].absorptivity+materials[i][j+1][k+1].absorptivity);
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					mu_y[i][j][k] = 0.25*mu0*(materials[i][j][k].mu_r+materials[i+1][j][k].mu_r+materials[i][j][k+1].mu_r+materials[i+1][j][k+1].mu_r);
					//H_absorptivity_y[i][j][k] = 0*0.25*(materials[i][j][k].absorptivity+materials[i+1][j][k].absorptivity+materials[i][j][k+1].absorptivity+materials[i+1][j][k+1].absorptivity);
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					mu_z[i][j][k] = 0.25*mu0*(materials[i][j][k].mu_r+materials[i+1][j][k].mu_r+materials[i][j+1][k].mu_r+materials[i+1][j+1][k].mu_r);
					//H_absorptivity_z[i][j][k] = 0*0.25*(materials[i][j][k].absorptivity+materials[i+1][j][k].absorptivity+materials[i][j+1][k].absorptivity+materials[i+1][j+1][k].absorptivity);
				}
			}
		}

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					rho_back[i][j][k] = materials[i][j][k].rho_back;
					conducting[i][j][k] = materials[i][j][k].conducting*materials[i][j][k].activated;
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					conducting_x[i][j][k] = Math.min(materials[i+1][j][k].conducting*materials[i+1][j][k].activated, materials[i][j][k].conducting*materials[i][j][k].activated);
					emfx[i][j][k] = 0.5*(materials[i+1][j][k].emf*Math.cos(materials[i+1][j][k].emf_direction)*materials[i+1][j][k].activated
					+ materials[i][j][k].emf*Math.cos(materials[i][j][k].emf_direction)*materials[i][j][k].activated);
					epsx[i][j][k] = eps0*0.5*(materials[i+1][j][k].eps_r + materials[i][j][k].eps_r);
					absorptivity_x[i][j][k] = Math.min(materials[i+1][j][k].absorptivity, materials[i][j][k].absorptivity);
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					conducting_y[i][j][k] = Math.min(materials[i][j+1][k].conducting*materials[i][j+1][k].activated, materials[i][j][k].conducting*materials[i][j][k].activated);
					emfy[i][j][k] = 0.5*(materials[i][j+1][k].emf*Math.sin(materials[i][j+1][k].emf_direction)*materials[i][j+1][k].activated
					+ materials[i][j][k].emf*Math.sin(materials[i][j][k].emf_direction)*materials[i][j][k].activated);
					epsy[i][j][k] = eps0*0.5*(materials[i][j+1][k].eps_r + materials[i][j][k].eps_r);
					absorptivity_y[i][j][k] = Math.min(materials[i][j+1][k].absorptivity, materials[i][j][k].absorptivity);
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					conducting_z[i][j][k] = Math.min(materials[i][j][k+1].conducting*materials[i][j][k+1].activated, materials[i][j][k].conducting*materials[i][j][k].activated);
					emfz[i][j][k] = 0;
					epsz[i][j][k] = eps0*0.5*(materials[i][j][k+1].eps_r + materials[i][j][k].eps_r);
					absorptivity_z[i][j][k] = Math.min(materials[i][j][k+1].absorptivity, materials[i][j][k].absorptivity);
				}
			}
		}

		computeChemicalForces();

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (K[i][j][k] > 0 || materials[i][j][k].type == MaterialType.SWITCH) {
						if (rho_n[i][j][k] == 0 && rho_p[i][j][k] == 0) {
							rho_n[i][j][k] = calcEquilibriumElectronCharge(rho_back[i][j][k], K[i][j][k]);
							rho_p[i][j][k] = calcEquilibriumHoleCharge(rho_back[i][j][k], K[i][j][k]);
						}
					} else {
						rho_n[i][j][k] = 0;
						rho_p[i][j][k] = 0;
					}

					if (materials[i][j][k].absorptivity == 0) {
						rho_abs[i][j][k] = 0;
					}

					rho_free[i][j][k] = rho_abs[i][j][k]+rho_n[i][j][k]+rho_p[i][j][k]+rho_back[i][j][k];
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					Jx_n[i][j][k] *= conducting_x[i][j][k];
					Jx_p[i][j][k] *= conducting_x[i][j][k];
					Jx_abs[i][j][k] *= (absorptivity_x[i][j][k] > 0)? 1:0;
				}
			}
		}

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					Jy_n[i][j][k] *= conducting_y[i][j][k];
					Jy_p[i][j][k] *= conducting_y[i][j][k];
					Jy_abs[i][j][k] *= (absorptivity_y[i][j][k] > 0)? 1:0;
				}
			}
		}


		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					Jz_n[i][j][k] *= conducting_z[i][j][k];
					Jz_p[i][j][k] *= conducting_z[i][j][k];
					Jz_abs[i][j][k] *= (absorptivity_z[i][j][k] > 0)? 1:0;
				}
			}
		}

		prescaleDielectric();
	}


	public void updateJustEMFs() {
		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					emfx[i][j][k] = 0.5*(materials[i+1][j][k].emf*Math.cos(materials[i+1][j][k].emf_direction)*materials[i+1][j][k].activated
					+ materials[i][j][k].emf*Math.cos(materials[i][j][k].emf_direction)*materials[i][j][k].activated);
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					emfy[i][j][k] = 0.5*(materials[i][j+1][k].emf*Math.sin(materials[i][j+1][k].emf_direction)*materials[i][j+1][k].activated
					+ materials[i][j][k].emf*Math.sin(materials[i][j][k].emf_direction)*materials[i][j][k].activated);
				}
			}
		}
	}

	public void computeFreeEnergy(int i, int j, int k, double ni, double W, double B)
	{
		double K = ni*ni;

		E0_n[i][j][k] = -W + 0.5*B;
		E0_p[i][j][k] = W + 0.5*B;

		double TS_n = (Math.log(K)/beta + E0_n[i][j][k] + E0_p[i][j][k])/2.0;
		double TS_p = TS_n;

		F0_n[i][j][k] = E0_n[i][j][k] - TS_n;
		F0_p[i][j][k] = E0_p[i][j][k] - TS_p;
	}

	public double calcEquilibriumElectronCharge(double rho_back, double K) {
		double B = rho_back/e_charge;
		return -e_charge*0.5*(B+Math.sqrt(B*B+4*K));
	}

	public double calcEquilibriumHoleCharge(double rho_back, double K) {
		double B = -rho_back/e_charge;
		return e_charge*0.5*(B+Math.sqrt(B*B+4*K));
	}

	// Mark all points on grid that are connected to given point and within a specified distance using Dijkstra's algorithm
	public void markNeighborhood(int i1, int j1, int k1, int max_distance) {

		for (int i = i1 - max_distance; i <= i1+max_distance; i++) {
			for (int j = j1 - max_distance; j <= j1+max_distance; j++) {
				for (int k = k1 - max_distance; k <= k1+max_distance; k++) {
					if (i >= 0 && j >= 0 && k >= 0 && i < nx && j < ny && k < nz) {
						distance[i][j][k] = Integer.MAX_VALUE;
						visited[i][j][k] = false;
					}
				}
			}
		}
		distance[i1][j1][k1] = 0;

		while (true) {
			int smallest_length = Integer.MAX_VALUE;
			int smallest_i = 0;
			int smallest_j = 0;
			int smallest_k = 0;
			for (int i = i1 - max_distance; i <= i1+max_distance; i++) {
				for (int j = j1 - max_distance; j <= j1+max_distance; j++) {
					for (int k = k1 - max_distance; k <= k1+max_distance; k++) {
						if (i >= 0 && j >= 0 && k >= 0 && i < nx && j < ny && k < nz) {
							if (distance[i][j][k] < smallest_length && visited[i][j][k] == false && conducting[i][j][k] == 1) {
								smallest_length = distance[i][j][k];
								smallest_i = i;
								smallest_j = j;
								smallest_k = k;
							}
						}
					}
				}
			}

			if (smallest_length == Integer.MAX_VALUE)
				break;

			for (int di = -1; di <= 1; di ++) {
				for (int dj = -1; dj <= 1; dj ++) {
					for (int dk = -1; dk <= 1; dk ++) {
						if (Math.abs(di)+Math.abs(dj)+Math.abs(dk) > 0 /* == 1 for vN neighborhood */
						&& smallest_i+di >= 0 && smallest_j+dj >= 0 && smallest_k+dk >= 0 && smallest_i+di < nx && smallest_j+dj < ny && smallest_k+dk < nz
						&& distance[smallest_i][smallest_j][smallest_k] < max_distance
						&& conducting[smallest_i+di][smallest_j+dj][smallest_k+dk] == 1) {
							distance[smallest_i+di][smallest_j+dj][smallest_k+dk] = Math.min(distance[smallest_i+di][smallest_j+dj][smallest_k+dk], distance[smallest_i][smallest_j][smallest_k]+1);
						}
					}
				}
			}

			visited[smallest_i][smallest_j][smallest_k] = true;
		}
	}

	public void computeChemicalForces()
	{

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (conducting[i][j][k] == 1) {
						computeFreeEnergy(i, j, k, materials[i][j][k].ni, materials[i][j][k].W, materials[i][j][k].Eb);
					} else {
						F0_n[i][j][k] = 0;
						F0_p[i][j][k] = 0;
						E0_n[i][j][k] = 0;
						E0_p[i][j][k] = 0;
					}

					E_a[i][j][k] = materials[i][j][k].Ea;
				}
			}
		}

		// Smoothing out free energy in space makes simulation more stable
		double[][][] F_n_tmp = copyArray(F0_n);
		double[][][] F_p_tmp = copyArray(F0_p);
		double[][][] E_n_tmp = copyArray(E0_n);
		double[][][] E_p_tmp = copyArray(E0_p);
		double[][][] Ea_tmp = copyArray(E_a);

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (conducting[i][j][k] == 1) {
						double F_n_sum = 0;
						double F_p_sum = 0;
						double E_n_sum = 0;
						double E_p_sum = 0;
						double Ea_sum = 0;
						double neighbors = 0;

						markNeighborhood(i, j, k, junction_size);

						for (int di = -junction_size; di <= junction_size; di++) {
							for (int dj = -junction_size; dj <= junction_size; dj++) {
								for (int dk = -junction_size; dk <= junction_size; dk++) {
									if (i+di >= 0 && j+dj >= 0 && k+dk >= 0 && i+di < nx && j+dj < ny && k+dk < nz && conducting[i+di][j+dj][k+dk] == 1 && visited[i+di][j+dj][k+dk]) {
										F_p_sum += F_p_tmp[i+di][j+dj][k+dk];
										F_n_sum += F_n_tmp[i+di][j+dj][k+dk];
										E_p_sum += E_p_tmp[i+di][j+dj][k+dk];
										E_n_sum += E_n_tmp[i+di][j+dj][k+dk];
										Ea_sum += Ea_tmp[i+di][j+dj][k+dk];
										neighbors += 1;
										distance[i+di][j+dj][k+dk] = Integer.MAX_VALUE;
										visited[i+di][j+dj][k+dk] = false;
									}
								}
							}
						}
						F0_n[i][j][k] = F_n_sum/neighbors;
						F0_p[i][j][k] = F_p_sum/neighbors;
						E0_n[i][j][k] = E_n_sum/neighbors;
						E0_p[i][j][k] = E_p_sum/neighbors;
						E_a[i][j][k] = Ea_sum/neighbors;
						K[i][j][k] = Math.exp(-beta*(F0_n[i][j][k]+F0_p[i][j][k]));
						R[i][j][k] = arrhenius_prefactor*Math.exp(-E_a[i][j][k]);
					} else {
						E_a[i][j][k] = 0;
						K[i][j][k] = 0;
						R[i][j][k] = 0;
					}
				}
			}
		}


		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					cmfx_n[i][j][k] = -conducting_x[i][j][k]*(F0_n[i+1][j][k] - F0_n[i][j][k])/ds;
					cmfx_p[i][j][k] = -conducting_x[i][j][k]*(F0_p[i+1][j][k] - F0_p[i][j][k])/ds;
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					cmfy_n[i][j][k] = -conducting_y[i][j][k]*(F0_n[i][j+1][k] - F0_n[i][j][k])/ds;
					cmfy_p[i][j][k] = -conducting_y[i][j][k]*(F0_p[i][j+1][k] - F0_p[i][j][k])/ds;
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					cmfz_n[i][j][k] = -conducting_z[i][j][k]*(F0_n[i][j][k+1] - F0_n[i][j][k])/ds;
					cmfz_p[i][j][k] = -conducting_z[i][j][k]*(F0_p[i][j][k+1] - F0_p[i][j][k])/ds;
				}
			}
		}
	}

	public double[][][] copyArray(double[][][] array) {
		double [][][] newarray = new double[array.length][][];
		for(int i = 0; i < array.length; i++) {
			newarray[i] = new double[array[i].length][];
			for(int j = 0; j < array[i].length; j++) {
				newarray[i][j] = array[i][j].clone();
			}
		}
		return newarray;
	}

	public void initializeAllMaterials() {
		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					initializeMaterial(i, j, k);
				}
			}
		}
	}

	public void initializeMaterial(int i, int j, int k) {
		initializeMaterial(i, j, k, materials[i][j][k].type);
	}

	public void initializeMaterial(int i, int j, int k, MaterialType material) {
		if (i < 0 || j < 0 || k < 0 || i >= nx || j >= ny || k >= nz || materials[i][j][k].modified)
			return;

		materials[i][j][k].type = material;

		if (material == MaterialType.DIELECTRIC) materials[i][j][k].eps_r = dielectric_eps_r;
		else if (material == MaterialType.FERROMAGNET) materials[i][j][k].mu_r = ferromagnet_mu_r;
		else if (material == MaterialType.POS_CHARGE) materials[i][j][k].rho_back = staticcharge_density;
		else if (material == MaterialType.NEG_CHARGE) materials[i][j][k].rho_back = -staticcharge_density;

		if (MaterialType.isConducting(material))
		{
			materials[i][j][k].conducting = 1;
			materials[i][j][k].ni = ni_metal;
			materials[i][j][k].W = W_metal_default;
			materials[i][j][k].Eb = E_b_metal;
			materials[i][j][k].Ea = E_a_metal;

			if (material == MaterialType.METAL_HIGH_W) materials[i][j][k].W = W_metal_high;
			else if (material == MaterialType.METAL_LOW_W) materials[i][j][k].W = W_metal_low;
			else if (material == MaterialType.METAL_HIGH_C) materials[i][j][k].ni = 2.5*ni_metal;
			else if (material == MaterialType.METAL_LOW_C) materials[i][j][k].ni = 0.25*ni_metal;
		}

		if (MaterialType.isSemiconducting(material))
		{
			materials[i][j][k].conducting = 1;
			materials[i][j][k].semiconducting = 1;
			materials[i][j][k].ni = ni_semi;
			materials[i][j][k].W = W_semi;
			materials[i][j][k].Eb = E_b_semi;
			materials[i][j][k].Ea = E_a_semi;

			if (material == MaterialType.SEMI_P_TYPE) materials[i][j][k].rho_back = -p_default_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_N_TYPE) materials[i][j][k].rho_back = n_default_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_HEAVY_P_TYPE) materials[i][j][k].rho_back = -p_heavy_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_HEAVY_N_TYPE) materials[i][j][k].rho_back = n_heavy_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_LIGHT_P_TYPE) materials[i][j][k].rho_back = -p_light_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_LIGHT_N_TYPE) materials[i][j][k].rho_back = n_light_doping_concentration*e_charge;
		}
	}

	public void handleMouseInput() {
		Brush brush = (Brush) opts.gui_brush.getSelectedItem();
		BrushShape brushshape = (BrushShape) opts.gui_brush_1.getSelectedItem();

		boolean pressing = false;
		boolean releasing = false;

		if (mouse_pressed) {
			if (!mouse_pressed_prev) {
				pressing = true;
				r.requestFocus();
				if ((Brush.isMaterialModifyingBrush(brush) && !shift_down) || brush == Brush.SELECT)
					opts.gui_paused.setSelected(true);
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
						opts.gui_brush.setSelectedItem(Brush.FILL);
						brush = (Brush) opts.gui_brush.getSelectedItem();
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
					opts.gui_brush.setSelectedItem(Brush.DRAW);
					brush = (Brush) opts.gui_brush.getSelectedItem();
					//r.requestFocus();
				}
			}
		}


		if (!threeD_mode) {
			mx_normal = 0;
			my_normal = 0;
			mz_normal = 0;
		}
		
		mx_realspace = Math.round((mx-1)/(double)scalefactor - 0.5 + mx_normal)*ds;
		my_realspace = Math.round((my-1)/(double)scalefactor - 0.5 + my_normal)*ds;
		mz_realspace = Math.round((mz-1)/(double)scalefactor - 0.5 + mz_normal)*ds;

		mx_start_realspace =  Math.round((mx_start-1)/(double)scalefactor - 0.5 + mx_normal)*ds;
		my_start_realspace = Math.round((my_start-1)/(double)scalefactor - 0.5 + my_normal)*ds;
		mz_start_realspace = Math.round((mz_start-1)/(double)scalefactor - 0.5 + mz_normal)*ds;

		mx_index = (int)Math.round((mx-1)/(double)scalefactor - 0.5);
		my_index = (int)Math.round((my-1)/(double)scalefactor - 0.5);
		mz_index = (int)Math.round((mz-1)/(double)scalefactor - 0.5);

		mx_start_index = (int)Math.round((mx_start-1)/(double)scalefactor - 0.5);
		my_start_index = (int)Math.round((my_start-1)/(double)scalefactor - 0.5);
		mz_start_index = (int)Math.round((mz_start-1)/(double)scalefactor - 0.5);

		if (mx_index < 0) mx_index = 0;
		if (my_index < 0) my_index = 0;
		if (mz_index < 0) mz_index = 0;
		if (mx_index >= nx) mx_index = nx-1;
		if (my_index >= ny) my_index = ny-1;
		if (mz_index >= nz) mz_index = nz-1;

		if (mx_start_index < 0) mx_start_index = 0;
		if (my_start_index < 0) my_start_index = 0;
		if (mz_start_index < 0) mz_start_index = 0;
		if (mx_start_index >= nx) mx_start_index = nx-1;
		if (my_start_index >= ny) my_start_index = ny-1;
		if (mz_start_index >= nz) mz_start_index = nz-1;

		opts.gui_stepsizelbl.setText("Step size: " + getSI(dt, "s"));
		opts.gui_stepslbl.setText("Steps/frame: " + opts.gui_simspeed_2.getValue());

		brushsize = (min_width/500)*(Math.pow(10.0, opts.gui_brushsize.getValue()/500.0) + opts.gui_brushsize.getValue()/100.0);
		opts.lblBrushSize.setText("Brush size: " + (int)Math.ceil(brushsize/ds));

		if (!Brush.isMaterialModifyingBrush(brush))
		{
			opts.gui_parameter2.setVisible(false);
			opts.gui_parameter2_text.setVisible(false);
			opts.gui_parameter2_text.setText("");
		}


		if (Brush.isMaterialModifyingBrush(brush) && brush != Brush.FILL) {
			opts.gui_brush_1.setVisible(true);
			opts.gui_brush_highlight.setVisible(true);
			opts.gui_brushsize.setVisible(true);
			opts.lblBrushSize.setVisible(true);
		} else {
			opts.gui_brush_1.setVisible(false);
			opts.gui_brush_highlight.setVisible(false);
			opts.gui_brushsize.setVisible(false);
			opts.lblBrushSize.setVisible(false);
		}


		if (Brush.isMaterialModifyingBrush(brush) && brush != Brush.ERASE) {
			opts.gui_material.setVisible(true);
		} else {
			opts.gui_material.setVisible(false);
		}

		if (brush_changed) {
			if (!(brush == Brush.SELECT || brush == Brush.FLOODSELECT)) {
				for (int i = 0; i < nx; i++)
				{
					for (int j = 0; j < ny; j++)
					{
						for (int k = 0; k < nz; k++)
						{
							selected[i][j][k] = false;
						}
					}
				}
			}

			if (brush != Brush.INTERACT) {
				for (int i = 0; i < nx; i++)
				{
					for (int j = 0; j < ny; j++)
					{
						for (int k = 0; k < nz; k++)
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
			int i_min = nx-1;
			int j_min = ny-1;
			int k_min = nz-1;
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						clipboard[i][j][k].erase();
						if (selected[i][j][k] && materials[i][j][k].type != MaterialType.VACUUM) {
							if (i < i_min) i_min = i;
							if (j < j_min) j_min = j;
							if (k < k_min) k_min = k;
						}
					}
				}
			}
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						if (selected[i][j][k] && materials[i][j][k].type != MaterialType.VACUUM) {
							clipboard[i-i_min][j-j_min][k-k_min] = materials[i][j][k].clone();
							if (cut) {
								materials[i][j][k].erase();
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
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						selected[i][j][k] = false;
					}
				}
			}

			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
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
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						if (selected[i][j][k] && materials[i][j][k].type != MaterialType.VACUUM) {
							materials[i][j][k].erase();
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
				renderer.pitch_start =  renderer.pitch;
				renderer.yaw_start = renderer.yaw;
				
				break;
			}  else if (mouse_pressed && shift_down) {
				renderer.pitch = renderer.pitch_start + 2*(float)(my_3d - my_3d_start)/imgheight;
				renderer.yaw = renderer.yaw_start - 2*(float)(mx_3d - mx_3d_start)/imgwidth;

				if (renderer.pitch > Math.PI/2) renderer.pitch = (float)Math.PI/2;
				if (renderer.pitch < -Math.PI/2) renderer.pitch = -(float)Math.PI/2;

				break;
			}

			if ((mousebutton == MouseEvent.BUTTON2 || alt_down) && pressing) {
				opts.gui_material.setSelectedItem(materials[mx_index][my_index][mz_index].type);
			}

			double angle = 0;
			MaterialType mat = (MaterialType) opts.gui_material.getSelectedItem();

			if (mat == MaterialType.EMF) {
				opts.gui_parameter2.setVisible(true);
				opts.gui_parameter2_text.setVisible(true);

				int directionval = (int)(opts.gui_parameter2.getValue()/6);
				if (directionval == 0) {
					opts.gui_parameter2_text.setText("EMF direction: Up");
					angle = -Math.PI/2;
				}
				if (directionval == 1) {
					opts.gui_parameter2_text.setText("EMF direction: Right");
					angle = 0;
				}
				if (directionval == 2) {
					opts.gui_parameter2_text.setText("EMF direction: Down");
					angle = Math.PI/2;
				}
				if (directionval == 3) {
					opts.gui_parameter2_text.setText("EMF direction: Left");
					angle = Math.PI;
				}
				//opts.gui_parameter2_text.setText("Brush orientation: " + directionval*(360/24) + " deg");
				//angle = Math.PI * directionval/12.0;
			} else {
				opts.gui_parameter2.setVisible(false);
				opts.gui_parameter2_text.setVisible(false);
				opts.gui_parameter2_text.setText("");
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
						floodFillSet(mx_index, my_index, mz_index, materials[mx_index][my_index][mz_index].type, mat, angle);
					}
				} else if (!threeD_mode && mouse_pressed || threeD_mode && pressing) {
					drawMaterialLine(mxp_realspace, myp_realspace, mzp_realspace, mx_realspace, my_realspace, mz_realspace, brush, brushshape, mat, brushsize, angle);
				}
			}

			if (Brush.isBrushShapeImportant(brush)) {
				for (int i = 0; i < nx; i++)
				{
					for (int j = 0; j < ny; j++)
					{
						for (int k = 0; k < nz; k++)
						{
							if (opts.gui_brush_highlight.isSelected()) {
								double cx = 0;
								double cy = 0;
								double cz = 0;
								cx = i*ds;
								cy = j*ds;
								cz = k*ds;

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
			if (materials[mx_index][my_index][mz_index].type == MaterialType.EMF || materials[mx_index][my_index][mz_index].type == MaterialType.SWITCH)
				updateCursor(HAND_CURSOR);
			else
				updateCursor(DEFAULT_CURSOR);

			if (pressing) {
				boolean turn_on_EMF = !selected_EMF[mx_index][my_index][mz_index];

				for (int i = 0; i < nx; i++)
				{
					for (int j = 0; j < ny; j++)
					{
						for (int k = 0; k < nz; k++)
						{
							selected_EMF[i][j][k] = false;
						}
					}
				}
				EMF_selected = false;

				if (materials[mx_index][my_index][mz_index].type == MaterialType.EMF && turn_on_EMF) {
					floodFillSelectEMF(mx_index, my_index, mz_index, true);
					EMF_selected = true;
					int setting = (int)(Math.round(50*materials[mx_index][my_index][mz_index].emf/max_EMF));
					opts.gui_parameter3.setValue(setting);
					prev_EMF_setting = setting;
				}

				if (materials[mx_index][my_index][mz_index].type == MaterialType.SWITCH) {
					this.floodFillToggleSwitch(mx_index, my_index, mz_index, 1-materials[mx_index][my_index][mz_index].activated);
					update = true;
				}

				renderer.pitch_start =  renderer.pitch;
				renderer.yaw_start = renderer.yaw;
			}  else if (mouse_pressed) {
				renderer.pitch = renderer.pitch_start + 2*(float)(my_3d - my_3d_start)/imgheight;
				renderer.yaw = renderer.yaw_start - 2*(float)(mx_3d - mx_3d_start)/imgwidth;

				if (renderer.pitch > Math.PI/2) renderer.pitch = (float)Math.PI/2;
				if (renderer.pitch < -Math.PI/2) renderer.pitch = -(float)Math.PI/2;
			}
			break;
		case FLOODSELECT:
		case SELECT:
			if (pressing) {
				if (brush == Brush.FLOODSELECT && !moving_selection) {
					floodFillSelect(mx_index, my_index, mz_index, materials[mx_index][my_index][mz_index].type, !selected[mx_index][my_index][mz_index]);
				}
				else if (moving_selection && !dragging_selection) {
					for (int i = 0; i < nx; i++)
					{
						for (int j = 0; j < ny; j++)
						{
							for (int k = 0; k < nz; k++)
							{
								int si = i-delta_mx_index;
								int sj = j-delta_my_index;
								int sk = k-delta_mz_index;
								if (si >= 0 && sj >= 0 && sk >= 0 && si < nx && sj < ny && sk < nz && selection[si][sj][sk].type != MaterialType.VACUUM) {
									materials[i][j][k].erase();
									materials[i][j][k] = selection[si][sj][sk].clone();
									selected[i][j][k] = true;
								}
							}
						}
					}
					moving_selection = false;
					dragging_selection = false;
				} else if (!moving_selection && selected[mx_index][my_index][mz_index]) {
					for (int i = 0; i < nx; i++)
					{
						for (int j = 0; j < ny; j++)
						{
							for (int k = 0; k < nz; k++)
							{
								selection[i][j][k].erase();
								if (selected[i][j][k]) {
									selection[i][j][k] = materials[i][j][k].clone();
									selected[i][j][k] = false;
									materials[i][j][k].erase();
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

						for (int i = 0; i < nx; i++)
						{
							for (int j = 0; j < ny; j++)
							{
								for (int k = 0; k < nz; k++)
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
						for (int i = 0; i < nx; i++)
						{
							for (int j = 0; j < ny; j++)
							{
								for (int k = 0; k < nz; k++)
								{
									int si = i-delta_mx_index;
									int sj = j-delta_my_index;
									int sk = k-delta_mz_index;
									if (si >= 0 && sj >= 0 && sk >= 0 && si < nx && sj < ny && sk < nz && selection[si][sj][sk].type != MaterialType.VACUUM) {
										materials[i][j][k].erase();
										materials[i][j][k] = selection[si][sj][sk].clone();
										selected[i][j][k] = true;
									}
								}
							}
						}
						moving_selection = false;
						dragging_selection = false;
					} else {
						if (delta_mx_index == 0 && delta_my_index == 0 && delta_mz_index == 0) {
							for (int i = 0; i < nx; i++)
							{
								for (int j = 0; j < ny; j++)
								{
									for (int k = 0; k < nz; k++)
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
			if (pressing) {
				CurrentProbe p = new CurrentProbe();
				p.x1 = mx_start_index;
				p.y1 = my_start_index;
				p.z1 = mz_start_index;
				p.x2 = mx_index;
				p.y2 = my_index;
				p.z2 = mz_index;
				currentprobes.add(p);
			} else if (mouse_pressed) {
				currentprobes.get(currentprobes.size()-1).x2 = mx_index;
				currentprobes.get(currentprobes.size()-1).y2 = my_index;
				currentprobes.get(currentprobes.size()-1).z2 = mz_index;
			}
			break;
		case VOLTAGE:
			if (pressing) {
				VoltageProbe p = new VoltageProbe();
				p.x = mx_start_index;
				p.y = my_start_index;
				p.z = mz_start_index;
				voltageprobes.add(p);
			} else if (mouse_pressed) {
				voltageprobes.get(voltageprobes.size()-1).x = mx_index;
				voltageprobes.get(voltageprobes.size()-1).y = my_index;
				voltageprobes.get(voltageprobes.size()-1).z = mz_index;
			}
			break;
		case DELETEPROBE:
			updateCursor(DEFAULT_CURSOR);
			int i = 0;
			while(i < voltageprobes.size()) {
				VoltageProbe p = voltageprobes.get(i);
				if (length(p.x-mx_index, p.y-my_index, p.z-mz_index) < 3) {
					updateCursor(HAND_CURSOR);
					if (pressing) {
						voltageprobes.remove(i);
						i--;
					}
				}
				i++;
			}

			i = 0;
			while(i < currentprobes.size()) {
				CurrentProbe p = currentprobes.get(i);
				if (length(p.x1-mx_index, p.y1-my_index, p.z1-mz_index) < 3 || length(p.x2-mx_index, p.y2-my_index, p.z2-mz_index) < 3) {
					updateCursor(HAND_CURSOR);
					if (pressing) {
						currentprobes.remove(i);
						i--;
					}
				}
				i++;
			}

			if (ground != null && length(ground.x-mx_index, ground.y-my_index, ground.z-mz_index) < 3) {
				updateCursor(HAND_CURSOR);
				if (pressing) {
					ground = null;
				}
			}

			break;
		case TEXT:
			updateCursor(HAND_CURSOR);
			if (mouse_pressed) {
				texting = true;
				text_x = mx_index;
				text_y = my_index;
			}
			//TODO
			break;
		case GROUND:
			if (pressing) {
				if (ground == null)
					ground = new VoltageProbe();
				ground.x = mx_start_index;
				ground.y = my_start_index;
				ground.z = mz_start_index;
			} else if (mouse_pressed) {
				ground.x = mx_index;
				ground.y = my_index;
				ground.z = mz_index;
			}
			break;
		}

		if (brush != Brush.TEXT)
		{
			texting = false;
		}

		setEMFs();

		if (releasing || (BoundaryCondition)opts.gui_bc.getSelectedItem() != prev_boundary || update) {

			constructBoundary();
			updateAllMaterials();
			multigridSolve(true, false);
		}

		prev_boundary = (BoundaryCondition)opts.gui_bc.getSelectedItem();

		mxp_realspace = mx_realspace;
		myp_realspace = my_realspace;
		mzp_realspace = mz_realspace;
	}

	public void drawMaterialLine(double x1, double y1, double z1, double x2, double y2, double z2, Brush brush, BrushShape brushshape, MaterialType mat, double brushsize, double EMF_angle) {
		Vector a = new Vector(0, 0, 0);
		Vector b = new Vector(0, 0, 0);
		Vector p = new Vector(0, 0, 0);
		Vector ab = new Vector(0, 0, 0);
		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					double cx = 0;
					double cy = 0;
					double cz = 0;
					cx = i*ds;
					cy = j*ds;
					cz = k*ds;

					a.initialize(x1, y1, z1);
					b.initialize(x2, y2, z2);
					p.initialize(cx, cy, cz);
					p.addmult(a, -1);
					ab.copy(b);
					ab.addmult(a, -1);

					double l2 = ab.dot(ab);
					if (l2 == 0)
						l2 = 1;
					double t = clamp(p.dot(ab)/l2, 0, 1);
					ab.scalarmult(t);
					p.addmult(ab, -1);
					double r = 0;
					if (brushshape == BrushShape.CIRCLE)
						r = Math.sqrt(p.dot(p));
					else if (brushshape == BrushShape.SQUARE)
						r = Math.max(Math.max(Math.abs(p.x), Math.abs(p.y)), Math.abs(p.z));
					if (r <= brushsize) {
						if (mat == MaterialType.VACUUM) {
							materials[i][j][k].erase();
						} else if (materials[i][j][k].type == MaterialType.VACUUM || brush == Brush.REPLACE) {
							materials[i][j][k].erase();
							initializeMaterial(i, j, k, mat);
							if (mat == MaterialType.EMF) materials[i][j][k].emf_direction = EMF_angle;
						}
					}
				}
			}
		}
	}

	public void updateCursor(Cursor c) {
		r.setCursor(c);
		renderer.canvas.setCursor(c);
	}

	public void setEMFs() {
		int EMF_setting = opts.gui_parameter3.getValue();
		double new_EMF = max_EMF*EMF_setting/50.0;

		if (opts.gui_brush.getSelectedItem() == Brush.INTERACT && EMF_selected) {
			opts.gui_parameter3.setVisible(true);
			opts.gui_parameter3_text.setVisible(true);
			opts.gui_parameter3_text.setText("EMF: " + getSI(new_EMF, "V/m"));
		} else {
			opts.gui_parameter3.setVisible(false);
			opts.gui_parameter3_text.setVisible(false);
			opts.gui_parameter3_text.setText("");
		}

		if (EMF_setting != prev_EMF_setting && EMF_selected) {
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						if (materials[i][j][k].type == MaterialType.EMF && selected_EMF[i][j][k]) {
							materials[i][j][k].emf = new_EMF;
						}
					}
				}
			}

			updateJustEMFs();
		}

		prev_EMF_setting = EMF_setting;
	}


	public void checkCFL() {
		System.out.println("Wave equation CFL ratio = " + (Math.sqrt(3)*c)/(ds/dt_maximum));
		System.out.println("Electron diffusion CFL ratio = " + dt_maximum/(ds*ds/(4*D_electron)));
		System.out.println("Hole diffusion CFL ratio = " + dt_maximum/(ds*ds/(4*D_hole)));
	}

	public void calcMiscFields(boolean updatePhi) {
		ScalarView view_scalar = (ScalarView) opts.gui_view.getSelectedItem();

		//Always find potentials
		if (updatePhi)
			multigridSolve(false, true);

		t8.start();



		sign_violation = false;
		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					// Prevent errors when taking log of 0
					if (conducting[i][j][k] == 0) {
						F_n[i][j][k] = Double.NaN;
						F_p[i][j][k] = Double.NaN;
						F[i][j][k] = Double.NaN;
					} else {
						F_n[i][j][k] = F0_n[i][j][k] + kT*Math.log(rho_n[i][j][k]/q_n);
						F_p[i][j][k] = F0_p[i][j][k] + kT*Math.log(rho_p[i][j][k]/q_p);
						// Think about why we should take average


						double probe_n = -e_charge*ni_metal;
						double probe_p = e_charge*ni_metal;
						//double sigma_n = mu_electron*logmean(-rho_n[i][j][k], -probe_n);
						//double sigma_p = mu_hole*logmean(rho_p[i][j][k], probe_p);
						double sigma_n = mu_electron*(-rho_n[i][j][k]);
						double sigma_p = mu_hole*rho_p[i][j][k];

						F[i][j][k] = (sigma_n*(F_n[i][j][k]/q_n+phi[i][j][k]) + sigma_p*(F_p[i][j][k]/q_p+phi[i][j][k]))
						/(sigma_n + sigma_p) - W_semi/eVtoJ;
					}

					G[i][j][k] = conducting[i][j][k]*R[i][j][k]*(K[i][j][k] - rho_n[i][j][k]*rho_p[i][j][k]/(q_n*q_p));

					if (rho_n[i][j][k] > error_detection_threshold || rho_p[i][j][k] < -error_detection_threshold)
						sign_violation = true;
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					Jx_free[i][j][k] = Jx_abs[i][j][k] + Jx_n[i][j][k] + Jx_p[i][j][k];
					Sx[i][j][k] = 0.125*((Hz[i][j][k]*(Ey[i+1][j][k] + Ey[i][j][k]) + Hz[i][j-1][k]*(Ey[i+1][j-1][k] - Ey[i][j-1][k]))
					- (Hy[i][j][k]*(Ez[i+1][j][k] + Ez[i][j][k]) + Hy[i][j][k-1]*(Ez[i+1][j][k-1] + Ez[i][j][k-1])));
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					Jy_free[i][j][k] = Jy_abs[i][j][k] + Jy_n[i][j][k] + Jy_p[i][j][k];
					Sy[i][j][k] = 0.125*((Hx[i][j][k]*(Ez[i][j+1][k] + Ez[i][j][k]) + Hx[i][j][k-1]*(Ez[i][j+1][k-1] - Ez[i][j][k-1]))
					- (Hz[i][j][k]*(Ex[i][j+1][k] + Ex[i][j][k]) + Hz[i-1][j][k]*(Ex[i-1][j+1][k] + Ex[i-1][j][k])));
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					Jz_free[i][j][k] = Jz_abs[i][j][k] + Jz_n[i][j][k] + Jz_p[i][j][k];
					Sz[i][j][k] = 0.125*((Hy[i][j][k]*(Ex[i][j][k+1] + Ex[i][j][k]) + Hy[i-1][j][k]*(Ex[i-1][j][k+1] - Ex[i-1][j][k]))
					- (Hx[i][j][k]*(Ey[i][j][k+1] + Ey[i][j][k]) + Hx[i][j-1][k]*(Ey[i][j-1][k+1] + Ey[i][j-1][k])));
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					debug[i][j][k] = (Bx[i+1][j+1][k+1]-Bx[i][j+1][k+1]+By[i+1][j+1][k+1]-By[i+1][j][k+1]+Bz[i+1][j+1][k+1]-Bz[i+1][j+1][k])/ds;
					debug2[i][j][k] = (Hx[i+1][j][k]-Hx[i][j][k]+Hy[i][j+1][k]-Hy[i][j][k]+Hz[i][j][k+1]-Hz[i][j][k])/ds;
				}
			}
		}

			
		/*if (view_scalar == ScalarView.HEAT) {
			for (int i = 0; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					grad_E0x_n[i][j][k] = -conducting_x[i][j][k]*(E0_n[i+1][j] - E0_n[i][j][k])/ds;
					grad_E0x_p[i][j][k] = -conducting_x[i][j][k]*(E0_p[i+1][j] - E0_p[i][j][k])/ds;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 0; j < ny-1; j++)
				{
					grad_E0y_n[i][j][k] = -conducting_y[i][j][k]*(E0_n[i][j+1] - E0_n[i][j][k])/ds;
					grad_E0y_p[i][j][k] = -conducting_y[i][j][k]*(E0_p[i][j+1] - E0_p[i][j][k])/ds;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					double n_contrib = -G[i][j][k]*E0_n[i][j][k];
					double jn_contrib = 0.5*(grad_E0x_n[i-1][j]*Jx_n[i-1][j]+grad_E0x_n[i][j][k]*Jx_n[i][j][k]
							+grad_E0y_n[i][j-1]*Jy_n[i][j-1]+grad_E0y_n[i][j][k]*Jy_n[i][j][k])/q_n;

					double p_contrib = -G[i][j][k]*E0_p[i][j][k];
					double jp_contrib = 0.5*(grad_E0x_p[i-1][j]*Jx_p[i-1][j]+grad_E0x_p[i][j][k]*Jx_p[i][j][k]
							+grad_E0y_p[i][j-1]*Jy_p[i][j-1]+grad_E0y_p[i][j][k]*Jy_p[i][j][k])/q_p;

					double ohm_contrib = 0.5*(Ex[i-1][j]*Jx_free[i-1][j]+Ex[i][j][k]*Jx_free[i][j][k]+Ey[i][j-1]*Jy_free[i][j-1]+Ey[i][j][k]*Jy_free[i][j][k]);

					Q[i][j][k] = n_contrib + jn_contrib + p_contrib + jp_contrib + ohm_contrib;
				}
			}
		}

		if (view_scalar == ScalarView.ENTROPY) {			
			for (int i = 0; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					grad_Fx_n[i][j][k] = conducting_x[i][j][k] == 1? -(F_n[i+1][j] - F_n[i][j][k])/ds : 0;
					grad_Fx_p[i][j][k] = conducting_x[i][j][k] == 1? -(F_p[i+1][j] - F_p[i][j][k])/ds : 0;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 0; j < ny-1; j++)
				{
					grad_Fy_n[i][j][k] = conducting_y[i][j][k] == 1? -(F_n[i][j+1] - F_n[i][j][k])/ds : 0;
					grad_Fy_p[i][j][k] = conducting_y[i][j][k] == 1? -(F_p[i][j+1] - F_p[i][j][k])/ds : 0;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					double n_contrib = -G[i][j][k]*F_n[i][j][k];
					double jn_contrib = 0.5*(grad_Fx_n[i-1][j]*Jx_n[i-1][j]+grad_Fx_n[i][j][k]*Jx_n[i][j][k]
							+grad_Fy_n[i][j-1]*Jy_n[i][j-1]+grad_Fy_n[i][j][k]*Jy_n[i][j][k])/q_n;

					double p_contrib = -G[i][j][k]*F_p[i][j][k];
					double jp_contrib = 0.5*(grad_Fx_p[i-1][j]*Jx_p[i-1][j]+grad_Fx_p[i][j][k]*Jx_p[i][j][k]
							+grad_Fy_p[i][j-1]*Jy_p[i][j-1]+grad_Fy_p[i][j][k]*Jy_p[i][j][k])/q_p;

					double ohm_contrib = 0.5*(Ex[i-1][j]*Jx_free[i-1][j]+Ex[i][j][k]*Jx_free[i][j][k]+Ey[i][j-1]*Jy_free[i][j-1]+Ey[i][j][k]*Jy_free[i][j][k]);

					S[i][j][k] = n_contrib + jn_contrib + p_contrib + jp_contrib + ohm_contrib;
				}
			}
		}*/

		if (ground != null)
			ground.potential = F[ground.x][ground.y][ground.z];

		for (VoltageProbe p: voltageprobes) {
			p.potential = F[p.x][p.y][p.z];
		}

		for (CurrentProbe p: currentprobes) {
			p.current = calcCurrent(p.x1, p.y1, p.x2, p.y2);
			//if (updatePhi) {
			System.out.println(1e15*time + "\t" + 1000*p.current);
			//}
		}

		updateMiscFields = false;

		t8.stop();
	}

	public double calcCurrent(int x0, int y0, int x1, int y1) {
		return 0;
		/*int dy = y1 - y0;
		int dx = x1 - x0;
		float t = (float) 0.5;
		float J = 0;

		if (Math.abs(dx) > Math.abs(dy)) {
			float m = (float) dy / (float) dx;
			t += y0;
			dx = (dx < 0) ? -1 : 1;
			m *= dx;
			while (x0 != x1) {
				int x0_prev = x0;
				float t_prev = t;

				x0 += dx;
				t += m;

				J += accumCurrent(x0_prev, (int)t_prev, x0, (int)t, Jx_free, Jy_free);

			}
		} else {
			float m = (float) dx / (float) dy;
			t += x0;
			dy = (dy < 0) ? -1 : 1;
			m *= dy;
			while (y0 != y1) {
				int y0_prev = y0;
				float t_prev = t;

				y0 += dy;
				t += m;

				J += accumCurrent((int)t_prev, y0_prev, (int)t, y0, Jx_free, Jy_free);
			}
		}

		return J;*/
	}

	public double accumCurrent(int x0, int y0, int x1, int y1, double[][] Jx, double[][] Jy) {
		int dx = x1-x0;
		int dy = y1-y0;
		if (dx == 1 && dy == 0) {
			return Math.abs(Jy[x0+1][y0])*ds;
		} else if (dx == -1 && dy == 0) {
			return Math.abs(Jy[x0][y0])*ds;
		} else if (dx == 0 && dy == 1) {
			return Math.abs(Jx[x0][y0+1])*ds;
		} else if (dx == 0 && dy == -1) {
			return Math.abs(Jx[x0][y0])*ds;
		} else if (dx == 1 && dy == 1) {
			return (Math.abs(Jx[x0][y0+1]) + Math.abs(Jy[x0+1][y0+1]))*ds;
		} else if (dx == 1 && dy == -1) {
			return (Math.abs(Jx[x0][y0])  + Math.abs(Jy[x0+1][y0-1]))*ds;
		} else if (dx == -1 && dy == 1) {
			return (Math.abs(Jx[x0][y0+1]) + Math.abs(Jy[x0][y0+1]))*ds;
		} else if (dx == -1 && dy == -1) {
			return (Math.abs(Jx[x0][y0]) + Math.abs(Jy[x0][y0-1]))*ds;
		}
		return 0;
	}

	public void prescaleDielectric() {
		downscale_x_vector(epsx, MG_epsx);
		downscale_y_vector(epsy, MG_epsy);
		downscale_z_vector(epsz, MG_epsz);
	}

	public void multigridSolve(boolean correctEfield, boolean computePhi) {
		assert(!(correctEfield && computePhi));

		//if (computePhi)
		//	return;

		if (correctEfield)
			t4.start();
		if (computePhi)
			t7.start();

		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					MG_phi1[i][j][k] = 0;
					MG_phi2[i][j][k] = 0;
					MG_rho0[i][j][k] = 0;
					for (int m = 0; m < MG_levels; m++) {
						MG_rho[m][i][j][k] = 0;
					}
				}
			}
		}

		if (correctEfield)
		{
			for (int i = 1; i < nx-1; i++) {
				for (int j = 1; j < ny-1; j++) {
					for (int k = 1; k < nz-1; k++) {
						MG_rho0[i][j][k] = ((Ex[i][j][k]*epsx[i][j][k]-Ex[i-1][j][k]*epsx[i-1][j][k]
						+ Ey[i][j][k]*epsy[i][j][k]-Ey[i][j-1][k]*epsy[i][j-1][k]
						+ Ez[i][j][k]*epsz[i][j][k]-Ez[i][j][k-1]*epsz[i][j][k-1]
						)/ds) - rho_free[i][j][k];
					}
				}
			}

			double num = 0;
			double denom = 0;
			for (int i = 1; i < nx-1; i++) {
				for (int j = 1; j < ny-1; j++) {
					for (int k = 1; k < nz-1; k++) {
						num += MG_rho0[i][j][k]*MG_rho0[i][j][k];
						denom += rho_free[i][j][k]*rho_free[i][j][k];
					}
				}
			}
			System.out.println("Starting poisson residual: " + Math.sqrt(num/denom));
		}
		if (computePhi) {
			for (int i = 1; i < nx-1; i++) {
				for (int j = 1; j < ny-1; j++) {
					for (int k = 1; k < nz-1; k++) {
						MG_rho0[i][j][k] = (Ex[i][j][k]-Ex[i-1][j][k]
						+ Ey[i][j][k]-Ey[i][j-1][k]
						+ Ez[i][j][k]-Ez[i][j][k-1])/(ds)
						+ (phi[i+1][j][k]+phi[i][j+1][k]+phi[i][j][k+1]
						+ phi[i-1][j][k]+phi[i][j-1][k]+phi[i][j][k-1]-6*phi[i][j][k])/(ds*ds);
					}
				}
			}
		}

		//int[] stepsarray = {0, 0, 200, 200, 200, 200, 200, 50, 20};
		int[] stepsarray = {0, 0, 100, 100, 50, 25, 25, 25, 20};
		//int[] stepsarray = {0, 0, 1, 0, 0, 0, 0, 0, 0};


		downscale(MG_rho0, MG_rho);

		for (int fineness = 0; fineness < MG_levels; fineness++) {
			int scalefactor = 1 << (MG_levels-fineness-1);
			int nx_tmp = nx / scalefactor;
			int ny_tmp = ny / scalefactor;
			int nz_tmp = nz / scalefactor;

			int poissonsteps = stepsarray[(fineness > 8)? 8 : fineness];
			double alpha = ds*ds*scalefactor*scalefactor;

			JacobiIteration(poissonsteps, nx_tmp, ny_tmp, nz_tmp, alpha, fineness, computePhi);

			if (fineness == MG_levels - 1)
				break;

			for (int i = 0; i < nx_tmp; i++) {
				for (int j = 0; j < ny_tmp; j++) {
					for (int k = 0; k < nz_tmp; k++) {
						MG_phi2[i][j][k] = MG_phi1[i][j][k];
					}
				}
			}

			for (int i = 0; i < nx_tmp; i++)
			{
				for (int j = 0; j < ny_tmp; j++) {
					for (int k = 0; k < nz_tmp; k++) {
						MG_phi1[2*i][2*j][2*k] = MG_phi2[i][j][k];
						MG_phi1[2*i+1][2*j][2*k] = MG_phi2[i][j][k];
						MG_phi1[2*i][2*j+1][2*k] = MG_phi2[i][j][k];
						MG_phi1[2*i+1][2*j+1][2*k] = MG_phi2[i][j][k];
						MG_phi1[2*i][2*j][2*k+1] = MG_phi2[i][j][k];
						MG_phi1[2*i+1][2*j][2*k+1] = MG_phi2[i][j][k];
						MG_phi1[2*i][2*j+1][2*k+1] = MG_phi2[i][j][k];
						MG_phi1[2*i+1][2*j+1][2*k+1] = MG_phi2[i][j][k];
					}
				}
			}

			/*for (int i = 1; i < 2*nx_tmp-1; i++)
			{
				for (int j = 1; j < 2*nx_tmp-1; j++) {
					MG_phi1[i][j][k] = this.bilinearinterp(MG_phi2, (i-0.5)/2.0, (j-0.5)/2.0);
				}
			}*/

			for (int i = 0; i < 2*nx_tmp; i++) {
				for (int j = 0; j < 2*ny_tmp; j++) {
					for (int k = 0; k < 2*nz_tmp; k++) {
						MG_phi2[i][j][k] = MG_phi1[i][j][k];
					}
				}
			}
		}

		if (correctEfield) {
			for (int i = 0; i < nx-1; i++)
			{
				for (int j = 0; j < ny-1; j++)
				{
					for (int k = 0; k < nz-1; k++)
					{
						Ex[i][j][k] = Ex[i][j][k] + (MG_phi1[i+1][j][k]-MG_phi1[i][j][k])/ds;
						Ey[i][j][k] = Ey[i][j][k] + (MG_phi1[i][j+1][k]-MG_phi1[i][j][k])/ds;
						Ez[i][j][k] = Ez[i][j][k] + (MG_phi1[i][j][k+1]-MG_phi1[i][j][k])/ds;
					}
				}
			}
		}

		if (computePhi)
		{
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						phi[i][j][k] = phi[i][j][k] + MG_phi1[i][j][k];
					}
				}
			}
		}

		if (correctEfield) {

			double num = 0;
			double denom = 0;
			for (int i = 1; i < nx-1; i++) {
				for (int j = 1; j < ny-1; j++) {
					for (int k = 1; k < nz-1; k++) {
						double drho = ((Ex[i][j][k]*epsx[i][j][k]-Ex[i-1][j][k]*epsx[i-1][j][k]
						+ Ey[i][j][k]*epsy[i][j][k]-Ey[i][j-1][k]*epsy[i][j-1][k]
						+ Ez[i][j][k]*epsz[i][j][k]-Ez[i][j][k-1]*epsz[i][j][k-1])/ds) - rho_free[i][j][k];
						num += drho*drho;
						denom += rho_free[i][j][k]*rho_free[i][j][k];
					}
				}
			}

			System.out.println("Poisson residual: " + Math.sqrt(num/denom));
		}

		if (correctEfield)
			t4.stop();
		if (computePhi)
			t7.stop();
	}


	public void JacobiIteration(int steps, int xmax, int ymax, int zmax, double alpha, int fineness, boolean calcPhi) {

		if (calcPhi) {
			for (int poissonit = 0; poissonit < steps; poissonit++) {
				for (int i = 1; i < xmax-1; i++) {
					for (int j = 1; j < ymax-1; j++) {
						for (int k = 1; k < zmax-1; k++) {
							MG_phi2[i][j][k] = (0.1*MG_phi1[i][j][k] + ((MG_phi1[i-1][j][k] + MG_phi1[i+1][j][k]
							+ MG_phi1[i][j-1][k] + MG_phi1[i][j+1][k]
							+ MG_phi1[i][j][k-1] + MG_phi1[i][j][k+1]) + MG_rho[fineness][i][j][k]*alpha)/6.0)/1.1;
						}
					}
				}
				for (int i = 1; i < xmax-1; i++) {
					for (int j = 1; j < ymax-1; j++) {
						for (int k = 1; k < zmax-1; k++) {
							MG_phi1[i][j][k] = (0.1*MG_phi2[i][j][k] + ((MG_phi2[i-1][j][k] + MG_phi2[i+1][j][k]
							+ MG_phi2[i][j-1][k] + MG_phi2[i][j+1][k]
							+ MG_phi2[i][j][k-1] + MG_phi2[i][j][k+1]) + MG_rho[fineness][i][j][k]*alpha)/6.0)/1.1;
						}
					}
				}
			}
		} else {
			for (int i = 1; i < xmax-1; i++) {
				for (int j = 1; j < ymax-1; j++) {
					for (int k = 1; k < zmax-1; k++) {
						MG_eps_avg[i][j][k] = (MG_epsx[fineness][i-1][j][k]+MG_epsx[fineness][i][j][k]+MG_epsy[fineness][i][j-1][k]+MG_epsy[fineness][i][j][k]+MG_epsz[fineness][i][j][k-1]+MG_epsz[fineness][i][j][k]);
					}
				}
			}

			for (int poissonit = 0; poissonit < steps; poissonit++) {
				for (int i = 1; i < xmax-1; i++) {
					for (int j = 1; j < ymax-1; j++) {
						for (int k = 1; k < zmax-1; k++) {
							MG_phi2[i][j][k] = (0.1*MG_phi1[i][j][k] + ((MG_phi1[i-1][j][k]*MG_epsx[fineness][i-1][j][k]
							+ MG_phi1[i+1][j][k]*MG_epsx[fineness][i][j][k]
							+ MG_phi1[i][j-1][k]*MG_epsy[fineness][i][j-1][k]
							+ MG_phi1[i][j+1][k]*MG_epsy[fineness][i][j][k]
							+ MG_phi1[i][j][k-1]*MG_epsz[fineness][i][j][k-1]
							+ MG_phi1[i][j][k+1]*MG_epsz[fineness][i][j][k])
							+ MG_rho[fineness][i][j][k]*alpha)/MG_eps_avg[i][j][k])/1.1;
						}
					}
				}
				for (int i = 1; i < xmax-1; i++) {
					for (int j = 1; j < ymax-1; j++) {
						for (int k = 1; k < zmax-1; k++) {
							MG_phi1[i][j][k] = (0.1*MG_phi2[i][j][k] + ((MG_phi2[i-1][j][k]*MG_epsx[fineness][i-1][j][k]
							+ MG_phi2[i+1][j][k]*MG_epsx[fineness][i][j][k]
							+ MG_phi2[i][j-1][k]*MG_epsy[fineness][i][j-1][k]
							+ MG_phi2[i][j+1][k]*MG_epsy[fineness][i][j][k]
							+ MG_phi2[i][j][k-1]*MG_epsz[fineness][i][j][k-1]
							+ MG_phi2[i][j][k+1]*MG_epsz[fineness][i][j][k])
							+ MG_rho[fineness][i][j][k]*alpha)/MG_eps_avg[i][j][k])/1.1;
						}
					}
				}
			}
		}
	}

	public void downscale(double[][][] source, double[][][][] dest) {
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					dest[MG_levels-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = MG_levels-2; m >= 0; m--) {
			nx_d = nx_d/2;
			ny_d = ny_d/2;
			nz_d = nz_d/2;

			for (int i = 0; i < nx_d; i++) {
				for (int j = 0; j < ny_d; j++) {
					for (int k = 0; k < nz_d; k++) {
						dest[m][i][j][k] = 0.125*
						(dest[m+1][2*i][2*j][2*k]+dest[m+1][2*i+1][2*j][2*k]
						+dest[m+1][2*i][2*j+1][2*k]+dest[m+1][2*i+1][2*j+1][2*k]
						+dest[m+1][2*i][2*j][2*k+1]+dest[m+1][2*i+1][2*j][2*k+1]
						+dest[m+1][2*i][2*j+1][2*k+1]+dest[m+1][2*i+1][2*j+1][2*k+1]);
					}
				}
			}
		}
	}

	public void downscale_x_vector(double[][][] source, double[][][][] dest) {
		for (int i = 0; i < nx-1; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					dest[MG_levels-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = MG_levels-2; m >= 0; m--) {
			nx_d = nx_d/2;
			ny_d = ny_d/2;
			nz_d = nz_d/2;

			for (int i = 0; i < nx_d-1; i++) {
				for (int j = 0; j < ny_d; j++) {
					for (int k = 0; k < nz_d; k++) {
						dest[m][i][j][k] = 0.5*0.125*
						(dest[m+1][2*i][2*j][2*k]+dest[m+1][2*i][2*j+1][2*k]
						+dest[m+1][2*i][2*j][2*k+1]+dest[m+1][2*i][2*j+1][2*k+1]
						+2*dest[m+1][2*i+1][2*j][2*k]+2*dest[m+1][2*i+1][2*j+1][2*k]
						+2*dest[m+1][2*i+1][2*j][2*k+1]+2*dest[m+1][2*i+1][2*j+1][2*k+1]
						+dest[m+1][2*i+2][2*j][2*k]+dest[m+1][2*i+2][2*j+1][2*k]
						+dest[m+1][2*i+2][2*j][2*k+1]+dest[m+1][2*i+2][2*j+1][2*k+1]);
					}
				}
			}
		}
	}

	public void downscale_y_vector(double[][][] source, double[][][][] dest) {
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny-1; j++) {
				for (int k = 0; k < nz; k++) {
					dest[MG_levels-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = MG_levels-2; m >= 0; m--) {
			nx_d = nx_d/2;
			ny_d = ny_d/2;
			nz_d = nz_d/2;

			for (int i = 0; i < nx_d; i++) {
				for (int j = 0; j < ny_d-1; j++) {
					for (int k = 0; k < nz_d; k++) {
						dest[m][i][j][k] = 0.5*0.125*
						(dest[m+1][2*i][2*j][2*k]+dest[m+1][2*i+1][2*j][2*k]
						+dest[m+1][2*i][2*j][2*k+1]+dest[m+1][2*i+1][2*j][2*k+1]
						+2*dest[m+1][2*i][2*j+1][2*k]+2*dest[m+1][2*i+1][2*j+1][2*k]
						+2*dest[m+1][2*i][2*j+1][2*k+1]+2*dest[m+1][2*i+1][2*j+1][2*k+1]
						+dest[m+1][2*i][2*j+2][2*k]+dest[m+1][2*i+1][2*j+2][2*k]
						+dest[m+1][2*i][2*j+2][2*k+1]+dest[m+1][2*i+1][2*j+2][2*k+1]);
					}
				}
			}
		}
	}

	public void downscale_z_vector(double[][][] source, double[][][][] dest) {
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz-1; k++) {
					dest[MG_levels-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = MG_levels-2; m >= 0; m--) {
			nx_d = nx_d/2;
			ny_d = ny_d/2;
			nz_d = nz_d/2;

			for (int i = 0; i < nx_d; i++) {
				for (int j = 0; j < ny_d; j++) {
					for (int k = 0; k < nz_d-1; k++) {
						dest[m][i][j][k] = 0.5*0.125*
						(dest[m+1][2*i][2*j][2*k]+dest[m+1][2*i+1][2*j][2*k]
						+dest[m+1][2*i][2*j+1][2*k]+dest[m+1][2*i+1][2*j+1][2*k]
						+2*dest[m+1][2*i][2*j][2*k+1]+2*dest[m+1][2*i][2*j+1][2*k+1]
						+2*dest[m+1][2*i][2*j+1][2*k+1]+2*dest[m+1][2*i+1][2*j+1][2*k+1]
						+dest[m+1][2*i][2*j][2*k+2]+dest[m+1][2*i+1][2*j][2*k+2]
						+dest[m+1][2*i][2*j+1][2*k+2]+dest[m+1][2*i+1][2*j+1][2*k+2]);
					}
				}
			}
		}
	}

	public double bilinearinterp(double[][][] array, double x, double y, double z) {
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

		Queue<FloodFillCoordinate> queue = new LinkedList<FloodFillCoordinate>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < nx && coord.j >= 0 && coord.j < ny && coord.k >= 0 && coord.k < nz && materials[coord.i][coord.j][coord.k].type == old_mat && materials[coord.i][coord.j][coord.k].type != new_mat) {
				materials[coord.i][coord.j][coord.k].erase();
				initializeMaterial(coord.i, coord.j, coord.k, new_mat);
				if (new_mat == MaterialType.EMF) materials[coord.i][coord.j][coord.k].emf_direction = EMF_angle;
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
		Queue<FloodFillCoordinate> queue = new LinkedList<FloodFillCoordinate>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < nx && coord.j >= 0 && coord.j < ny && coord.k >= 0 && coord.k < nz && materials[coord.i][coord.j][coord.k].type == mat && selected[coord.i][coord.j][coord.k] != select) {
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
		Queue<FloodFillCoordinate> queue = new LinkedList<FloodFillCoordinate>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < nx && coord.j >= 0 && coord.j < ny && coord.k >= 0 && coord.k < nz && materials[coord.i][coord.j][coord.k].type == MaterialType.EMF && selected_EMF[coord.i][coord.j][coord.k] != select) {
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
		Queue<FloodFillCoordinate> queue = new LinkedList<FloodFillCoordinate>();
		queue.add(new FloodFillCoordinate(i, j, k));

		while (queue.size() > 0) {
			FloodFillCoordinate coord = queue.remove();
			if (coord.i >= 0 && coord.i < nx && coord.j >= 0 && coord.j < ny && coord.k >= 0 && coord.k < nz && materials[coord.i][coord.j][coord.k].type == MaterialType.SWITCH && materials[coord.i][coord.j][coord.k].activated != active) {
				materials[coord.i][coord.j][coord.k].activated = active;
				queue.add(new FloodFillCoordinate(coord.i-1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i+1, coord.j, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j-1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j+1, coord.k));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k-1));
				queue.add(new FloodFillCoordinate(coord.i, coord.j, coord.k+1));
			}
		}
	}

	/*public void drawPixelRectangle(int x, int y, int w, int h) {
		for (int i = x; i < x+w; i++) {
			for (int j = y; j < y+h; j++) {
				setPixel(i, j, k);
			}
		}
	}*/

	public void setPixel(int i, int j, int k) {
		if (i < 0 || j < 0  || k < 0 || i >= nx || j >= ny || k > nz)
			return;

		image_r[i][j][k] = (float)(image_r[i][j][k]*alphaBG + col_r*alphaFG);
		image_g[i][j][k] = (float)(image_g[i][j][k]*alphaBG + col_g*alphaFG);
		image_b[i][j][k] = (float)(image_b[i][j][k]*alphaBG + col_b*alphaFG);
	}

	public void stampPixelData() {
		t9.start();
		int[] imgData = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
		
		if (slice_z) {
			int scansize = nx*scalefactor;
			int k_slice = opts.gui_slice.getValue();
			for (int x = 0; x < nx*scalefactor; x++) {
				for (int y = 0; y < ny*scalefactor; y++) {
					int i = x/scalefactor;
					int j = ny-1-y/scalefactor;
					double scale = 1f/max(image_r[i][j][k_slice], image_g[i][j][k_slice], image_b[i][j][k_slice], 1f);
					int rgb = clamp((int)(256*image_r[i][j][k_slice]*scale), 0, 255) << 16
					| clamp((int)(256*image_g[i][j][k_slice]*scale), 0, 255) << 8
					| clamp((int)(256*image_b[i][j][k_slice]*scale), 0, 255);
					imgData[x + y*scansize] = rgb;
				}
			}
		}
		if (slice_x) {
			int scansize = ny*scalefactor;
			int i_slice = opts.gui_slice.getValue();
			for (int x = 0; x < ny*scalefactor; x++) {
				for (int y = 0; y < nz*scalefactor; y++) {
					int j = x/scalefactor;
					int k = nz-1-y/scalefactor;
					double scale = 1f/max(image_r[i_slice][j][k], image_g[i_slice][j][k], image_b[i_slice][j][k], 1f);
					int rgb = clamp((int)(256*image_r[i_slice][j][k]*scale), 0, 255) << 16
					| clamp((int)(256*image_g[i_slice][j][k]*scale), 0, 255) << 8
					| clamp((int)(256*image_b[i_slice][j][k]*scale), 0, 255);
					imgData[x + y*scansize] = rgb;
				}
			}
		}

		if (slice_y) {
			int scansize = nx*scalefactor;
			int j_slice = opts.gui_slice.getValue();
			for (int x = 0; x < nx*scalefactor; x++) {
				for (int y = 0; y < nz*scalefactor; y++) {
					int i = x/scalefactor;
					int k = nz-1-y/scalefactor;
					double scale = 1f/max(image_r[i][j_slice][k], image_g[i][j_slice][k], image_b[i][j_slice][k], 1f);
					int rgb = clamp((int)(256*image_r[i][j_slice][k]*scale), 0, 255) << 16
					| clamp((int)(256*image_g[i][j_slice][k]*scale), 0, 255) << 8
					| clamp((int)(256*image_b[i][j_slice][k]*scale), 0, 255);
					imgData[x + y*scansize] = rgb;
				}
			}
		}

		t9.stop();
	}

	public void drawLine(int x0, int y0, int x1, int y1, boolean draw_starting_point) {
		y0 = imgheight - 1 - y0;
		y1 = imgheight - 1 - y1;
		try {
			int[] imgData = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
			int scansize = scalefactor*nx;

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

	/*public void drawPixelLine(int x0, int y0, int x1, int y1) {
		int dy = y1 - y0;
		int dx = x1 - x0;
		float t = (float) 0.5;

		setPixel(x0, y0);

		if (Math.abs(dx) > Math.abs(dy)) {
			float m = (float) dy / (float) dx;
			t += y0;
			dx = (dx < 0) ? -1 : 1;
			m *= dx;
			while (x0 != x1) {
				x0 += dx;
				t += m;

				setPixel(x0, (int)t);
			}
		} else {
			float m = (float) dx / (float) dy;
			t += x0;
			dy = (dy < 0) ? -1 : 1;
			m *= dy;
			while (y0 != y1) {
				y0 += dy;
				t += m;

				setPixel((int)t, y0);
			}
		}
	}*/

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
		if (val != val) return min;
		if (val < min) return min;
		if (val > max) return max;
		return val;
	}

	public void setalphaBG(double alpha) {
		alphaBG = alpha;
	}

	public void setalphaFG(double alpha) {
		alphaFG = alpha;
	}

	public void render() {

		t5.start();
		Graphics2D g = (Graphics2D) screen.getGraphics();
		double scalingconstant = 10.0*Math.pow(10.0, opts.gui_brightness.getValue()/10.0);

		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					image_r[i][j][k] = 0;
					image_g[i][j][k] = 0;
					image_b[i][j][k] = 0;
				}
			}
		}

		clearStrings();

		/* Draw pixels */

		setalphaBG(0);
		setalphaFG(1);

		Brush brush = (Brush) opts.gui_brush.getSelectedItem();

		//int k_slice = opts.gui_zslice.getValue();

		if (opts.gui_elem_colors.isSelected()) {
			for (int i = 0; i < nx; i++) {
				for (int j = 0; j < ny; j++) {
					for (int k = 0; k < nz; k++) {
						setColor(materials[i][j][k].type.color_r, materials[i][j][k].type.color_g, materials[i][j][k].type.color_b);

						setPixel(i, j, k);
					}
				}
			}
		} else {
			for (int i = 0; i < nx; i++) {
				for (int j = 0; j < ny; j++) {
					for (int k = 0; k < nz; k++) {
						setColor(materials[i][j][k].type.color_grayscale, materials[i][j][k].type.color_grayscale, materials[i][j][k].type.color_grayscale);
						setPixel(i, j, k);
					}
				}
			}
		}

		if (moving_selection) {
			if (opts.gui_elem_colors.isSelected()) {
				for (int i = 0; i < nx; i++) {
					for (int j = 0; j < ny; j++) {
						for (int k = 0; k < nz; k++) {
							int si = i-delta_mx_index;
							int sj = j-delta_my_index;
							if (si >= 0 && sj >= 0 && si < nx && sj < ny && selection[si][sj][k].type != MaterialType.VACUUM) {
								setColor(selection[si][sj][k].type.color_r, selection[si][sj][k].type.color_g, selection[si][sj][k].type.color_b);
								setPixel(i, j, k);
							}
						}
					}
				}
			}else {
				for (int i = 0; i < nx; i++) {
					for (int j = 0; j < ny; j++) {
						for (int k = 0; k < nz; k++) {
							int si = i-delta_mx_index;
							int sj = j-delta_my_index;
							if (si >= 0 && sj >= 0 && si < nx && sj < ny && selection[si][sj][k].type != MaterialType.VACUUM) {
								setColor(selection[si][sj][k].type.color_grayscale, selection[si][sj][k].type.color_grayscale, selection[si][sj][k].type.color_grayscale);
								setPixel(i, j, k);
							}
						}
					}
				}
			}
		}

		if ((ScalarView) opts.gui_view.getSelectedItem() != ScalarView.NONE) {
			setalphaBG(1.0);
			setalphaFG(1.0);
			for (int i = 1; i < nx-1; i++) {
				for (int j = 1; j < ny-1; j++) {
					for (int k = 1; k < nz-1; k++) {
						float v = 0;
						switch ((ScalarView) opts.gui_view.getSelectedItem()) {
						case NONE:
							setColorFloat(0, 0, 0);
							break;
						case E_FIELD:
							double vx = 0.5*(Ex[i][j][k]+Ex[i-1][j][k]);
							double vy = 0.5*(Ey[i][j][k]+Ey[i][j-1][k]);
							double vz = 0.5*(Ez[i][j][k]+Ez[i][j][k-1]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant/1e5), 0);
							break;
						case H_FIELD:
							vx = 0.25*(Hx[i][j][k]+Hx[i][j-1][k]+Hx[i][j][k-1]+Hx[i][j-1][k-1]);
							vy = 0.25*(Hy[i][j][k]+Hy[i-1][j][k]+Hy[i][j][k-1]+Hy[i-1][j][k-1]);
							vz = 0.25*(Hz[i][j][k]+Hz[i-1][j][k]+Hz[i][j-1][k]+Hz[i-1][j-1][k]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant/1e2), 0);
							break;
						case CURRENT:
							vx = 0.5*(Jx_free[i][j][k]+Jx_free[i-1][j][k]);
							vy = 0.5*(Jy_free[i][j][k]+Jy_free[i][j-1][k]);
							vz = 0.5*(Jz_free[i][j][k]+Jz_free[i][j][k-1]);
							setColorFloat(0, (float)(length(vx, vy, vz)*scalingconstant/1e7), 0);
							break;
						case POTENTIAL:
							v = (float)(phi[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case CHARGE:
							v = (float)(rho_free[i][j][k]*scalingconstant);
							float rc = Math.min(Math.max(v, 0), 1);
							float bc = Math.min(Math.max(-v, 0), 1);
							float gc = Math.min(rc, bc);

							setalphaBG(1-0.5*gc);
							//setalphaFG(gc);
							setColorFloat(rc, gc, bc);
							break;
						case BACKGROUND_CHARGE:
							v = (float)(rho_back[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case ELECTRON_CHARGE:
							v = (float)(rho_n[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case HOLE_CHARGE:
							v = (float)(rho_p[i][j][k]*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case COMBINED_CHARGE:
							rc = (float)(clamp(0.2*Math.log(rho_p[i][j][k]*scalingconstant), 0, 1));
							bc = (float)(clamp(0.2*Math.log(-rho_n[i][j][k]*scalingconstant), 0, 1));
							gc = Math.min(rc, bc);
							double it = Math.max(rc, bc);
							setalphaBG(1-0.5*gc);
							setalphaFG(0.6*it);
							setColorFloat(rc, gc, bc);
							break;
						case ENERGY:
							v = (float)(u[i][j][k]*scalingconstant);
							setColorFloat(0, v, 0);
							break;
						case ENTROPY:
							v = (float)(S[i][j][k]*scalingconstant/1e12);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case HEAT:
							v = (float)(Q[i][j][k]*scalingconstant/1e12);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case ELECTRON_POTENTIAL:
							v = (float)(conducting[i][j][k]*(F_n[i][j][k]/q_n+phi[i][j][k]-W_semi/eVtoJ)*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case HOLE_POTENTIAL:
							v = (float)(conducting[i][j][k]*(F_p[i][j][k]/q_p+phi[i][j][k]-W_semi/eVtoJ)*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case DEBUG:
							v = (float)(debug[i][j][k]/1e5*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case DEBUG2:
							v = (float)(debug2[i][j][k]*mu0/1e5*scalingconstant);
							setColorFloat(v, 0, -v);
							break;
						case RECOMBINATION:
							v = (float)(-G[i][j][k]/1e30*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case AVERAGE_POTENTIAL:
							v = (float)(F[i][j][k]*scalingconstant);
							setColorFloat(v, Math.abs(v), -v);
							break;
						case LIGHT:
							v = (float)(-materials[i][j][k].semiconducting*this.G[i][j][k]/1e30*scalingconstant);
							setColorFloat(v, v, v);
							break;
						}
						setPixel(i, j, k);
					}
				}
			}
		}

		boolean highlight = (opts.gui_brush_highlight.isSelected() && Brush.isBrushShapeImportant(brush));
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					if (materials[i][j][k].type == MaterialType.EMF && i > 0 && j > 0 && i < nx-1 && j < ny-1) {
						setalphaBG(0.25);
						setalphaFG(0.75);

						int offset = 0;
						if (selected_EMF[i][j][k])
							offset = 60*(2*((i+j+k)%2)-1);

						if (!threeD_mode && (materials[i+1][j][k].type != MaterialType.EMF
						|| materials[i-1][j][k].type != MaterialType.EMF
						|| materials[i][j+1][k].type != MaterialType.EMF
						|| materials[i][j-1][k].type != MaterialType.EMF))
						{
							offset = -30;
						}

						int delta_r = MaterialType.EMF.color_r+offset;
						int delta_g = MaterialType.EMF.color_g+offset;
						int delta_b = MaterialType.EMF.color_b+offset;
						setColor(delta_r, delta_g, delta_b);

						setPixel(i, j, k);
					} else if (!(materials[i][j][k].activated == 1)) {
						setalphaBG(0.25);
						setalphaFG(0.75);
						setColor(0, 0, 0);
						setPixel(i, j, k);
					}

					if (selected[i][j][k] || (highlight && under_brush[i][j][k]))
					{
						setalphaBG(0.75);
						setalphaFG(0.25);

						int s = selected[i][j][k]? 1:0;
						int h = (highlight && under_brush[i][j][k])? 1:0;
						int delta_r = 256*s + 256*h;
						int delta_g = 100*s + 256*h;
						int delta_b = 256*s + 256*h;

						setColor(delta_r, delta_g, delta_b);

						setPixel(i, j, k);
					}
				}

			}
		}


		/*setalphaBG(1);
		setalphaFG(1);
		setColorFloat(0.7f, 0.7f, 0.7f);

		if (brush == Brush.LINE && mouse_pressed) {
			drawPixelLine(mx_start_index, my_start_index, mx_index, my_index);
		}


		setalphaFG(1.0);
		setColorFloat(0.5f, 1.0f, 1.0f);

		for (CurrentProbe p: currentprobes) {
			drawPixelRectangle(p.x1-1, p.y1-1, 3, 3);
			drawPixelRectangle(p.x2-1, p.y2-1, 3, 3);
		}

		for (VoltageProbe p: voltageprobes) {
			drawPixelRectangle(p.x-1, p.y-1, 3, 3);
		}

		if (ground != null) {
			drawPixelRectangle(ground.x-1, ground.y-1, 3, 3);
		}

		setalphaFG(0.3);
		setColorFloat(0.5f, 1.0f, 1.0f);

		for (CurrentProbe p: currentprobes) {
			drawPixelLine(p.x1, p.y1, p.x2, p.y2);
		}

		setalphaFG(0.8);
		setColorFloat(1.0f, 1.0f, 1.0f);

		if (texting) {
			drawPixelLine(text_x, text_y, text_x, text_y+7);
		}*/

		if (threeD_mode)
			return;

		stampPixelData();
		
		//int k_slice = opts.gui_zslice.getValue();

		/* Draw vectors */

		if ((VectorView) opts.gui_view_vec.getSelectedItem() != VectorView.NONE) {
			setalphaBG(1.0);

			double arrowlength = 25.0/scalefactor;

			double vectorscalingconstant = 0.01*Math.pow(10.0, opts.gui_brightness_vec.getValue()/5.0);

			VectorMode vector_display_mode = (VectorMode)opts.gui_view_vec_mode.getSelectedItem();

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

			switch ((VectorView) opts.gui_view_vec.getSelectedItem()) {
			case NONE:
				break;
        	case B_FIELD:
        		vf_x = Bx;
        		vf_y = By;
        		vf_z = Bz;
        		grid_offset = 0;
        		dual_offset = 0.5;
        		break;
			case H_FIELD:
				vf_x = Hx;
				vf_y = Hy;
				vf_z = Hz;
        		grid_offset = 0;
        		dual_offset = -0.5;
				break;
			case E_FIELD:
				vf_x = Ex;
				vf_y = Ey;
				vf_z = Ez;
				break;
			case ELECTRON_CURRENT:
				vf_x = Jx_n;
				vf_y = Jy_n;
				vf_z = Jz_n;
				break;
			case HOLE_CURRENT:
				vf_x = Jx_p;
				vf_y = Jy_p;
				vf_z = Jz_p;
				break;
			case TOTAL_CURRENT:
				vf_x = Jx_free;
				vf_y = Jy_free;
				vf_z = Jz_free;
				break;
			case POYNTING:
				vf_x = Sx;
				vf_y = Sy;
				vf_z = Sz;
				break;
			case EMF:
				vf_x = emfx;
				vf_y = emfy;
				vf_z = emfz;
				break;
			}
			
			int npx = 0;
			int npy = 0;
			
			if (slice_x) {
				vf_px = vf_y;
				vf_py = vf_z;
				npx = ny;
				npy = nz;
			} else if (slice_y) {
				vf_px = vf_x;
				vf_py = vf_z;
				npx = nx;
				npy = nz;
			} else if (slice_z) {
				vf_px = vf_x;
				vf_py = vf_y;
				npx = nx;
				npy = ny;
			}
			
			int slice = opts.gui_slice.getValue();


			Vector ctr = new Vector(0,0,0);
			Vector arrow = new Vector(0,0,0);
			Vector tip1 = new Vector(0,0,0);
			Vector tip2 = new Vector(0,0,0);
			Vector body1 = new Vector(0,0,0);
			Vector body2 = new Vector(0,0,0);

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
									dpx = bilinearinterp(vf_px, slice+dual_offset, prevpx+grid_offset, prevpy+dual_offset);
									dpy = bilinearinterp(vf_py, slice+dual_offset, prevpx+dual_offset, prevpy+grid_offset);
								} else if (slice_y) {
									dpx = bilinearinterp(vf_px, prevpx+grid_offset, slice+dual_offset, prevpy+dual_offset);
									dpy = bilinearinterp(vf_py, prevpx+dual_offset, slice+dual_offset, prevpy+grid_offset);
								} else if (slice_z) {
									dpx = bilinearinterp(vf_px, prevpx+grid_offset, prevpy+dual_offset, slice+dual_offset);
									dpy = bilinearinterp(vf_py, prevpx+dual_offset, prevpy+grid_offset, slice+dual_offset);
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
							arrow.x = bilinearinterp(vf_px, slice+dual_offset, px+grid_offset, py+dual_offset);
							arrow.y = bilinearinterp(vf_py, slice+dual_offset, px+dual_offset, py+grid_offset);
						} else if (slice_y) {
							arrow.x = bilinearinterp(vf_px,px+grid_offset, slice+dual_offset, py+dual_offset);
							arrow.y = bilinearinterp(vf_py,px+dual_offset, slice+dual_offset, py+grid_offset);
						} else if (slice_z) {
							arrow.x = bilinearinterp(vf_px,px+grid_offset, py+dual_offset, slice+dual_offset);
							arrow.y = bilinearinterp(vf_py,px+dual_offset, py+grid_offset, slice+dual_offset);
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

		((Graphics2D)g).setRenderingHint(
		RenderingHints.KEY_TEXT_ANTIALIASING,
		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		renderText(g, null);

		/*if (!opts.gui_brush_highlight.isSelected()) {
			int r = (int)(scalefactor*brushsize/ds);
			int brushshape = opts.gui_brush_1.getSelectedIndex();
			if (Brush.isBrushShapeImportant(brush))
				if (brushshape == 0) {
					g.setColor(new Color(50, 50, 50));
					g.drawOval(mx - r, my - r, 2*r, 2*r);
					g.setColor(new Color(200, 200, 200));
					g.drawOval(mx - r-1, my - r-1, 2*r, 2*r);
				} else {
					g.setColor(new Color(50, 50, 50));
					g.drawRect(mx - r, my - r, 2*r-2, 2*r-2);
					g.setColor(new Color(200, 200, 200));
					g.drawRect(mx - r-1, my - r-1, 2*r, 2*r);
				}
		}*/

		t5.stop();
	}
	
	
	public void renderText(Graphics g, TextRenderer t) {
		/* Draw text */

		for (VoltageProbe p: voltageprobes) {
			if (ground != null)
				drawStringWithBackgroundAndBorder("V = " + getSI(p.potential - ground.potential, "V"), p.x*scalefactor-5, p.y*scalefactor - 12);
			else
				drawStringWithBackgroundAndBorder("V = " + getSI(p.potential, "V"), p.x*scalefactor-5, p.y*scalefactor - 12);
		}

		if (ground != null)
			drawStringWithBackgroundAndBorder("Ground = " + getSI(ground.potential - ground.potential, "V"), ground.x*scalefactor-5, ground.y*scalefactor - 12);

		for (CurrentProbe p: currentprobes) {
			double xa = 0.5*(p.x1+p.x2)*scalefactor;
			double ya = 0.5*(p.y1+p.y2)*scalefactor;

			double dx = p.x2 - p.x1;
			double dy = p.y2 - p.y1;
			double len = length(dx, dy);
			dx = dx/len;
			dy = dy/len;
			if (Math.abs(dx) > Math.abs(dy))
			{
				dx = -Math.abs(dx);
			} else {
				dy = -2*Math.abs(dy);
			}

			drawStringWithBackgroundAndBorder("I = " + getSI(p.current*depth, "A"), (int)(xa-8*dy)-5, (int)(ya+12*dx)+5);
		}

		drawStringBackgrounds(g, t);
		drawStrings(g, t);
		startNewStringLayer();

		{
			double mx_t = mx_index;
			double my_t = my_index;
			double mz_t = mz_index;
			//double mx_t = (mouseX/(double)scalefactor);
			//double my_t = (mouseY/(double)scalefactor);
			int mi = mx_index;
			int mj = my_index;
			int mk = mz_index;

			if (mi < 0)
				mi = 0;
			if (mi >= nx-1)
				mi = nx-2;
			if (mj < 0)
				mj = 0;
			if (mj >= ny-1)
				mj = ny-2;
			if (mk < 0)
				mk = 0;
			if (mk >= nz-1)
				mk = nz-2;

			Material mat = materials[mi][mj][mk];

			int vspacing = 12;
			int voffset = 1 + my_3d;
			int hoffset = 5 + mx_3d+15;
			
			if (opts.gui_tooltip.isSelected()) {
				if (voffset + 22*vspacing > imgheight) {
					voffset = voffset - ((voffset + 22*vspacing) - imgheight);
				}
				if (hoffset + 120 > imgwidth) {
					hoffset = hoffset - ((hoffset + 120) - imgwidth);
				}
			}

			String name = "Material: " + mat.type.name + (mat.modified? " (Modified)" : "");

			this.drawBigStringWithBackground(name, hoffset, voffset + 1*vspacing);
			if (opts.gui_tooltip.isSelected()) {
				voffset = voffset+3;
				int line = 2;
				drawTwoColumnString("E" , 							getSI(getFieldMagnitude(Ex, Ey, Ez, mx_t, my_t, mz_t), "V/m"), hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("H" , 							getSI(getDualFieldMagnitude(Hx, Hy, Hz, mx_t, my_t, mz_t), "T"),	hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03d5" , 						getSI(bilinearinterp(phi,mx_t, my_t, mz_t), "V"),					hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u2130" , 						getSI(mat.emf, "V/m"),										hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03b5/\u03b5\u2080" , 		getSI(mat.eps_r, ""),										hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03bc/\u03bc\u2080" , 		getSI(mat.mu_r, ""),											hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1\u2099" , 				getSI(bilinearinterp(rho_n,mx_t, my_t, mz_t), "C/m^3"),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1\u209A" , 				getSI(bilinearinterp(rho_p,mx_t, my_t, mz_t), "C/m^3"),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1\u2080",					getSI(bilinearinterp(rho_back,mx_t, my_t, mz_t), "C/m^3"),		hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("\u03c1" , 						getSI(bilinearinterp(rho_free,mx_t, my_t, mz_t), "C/m^3"),		hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("J\u2099" ,						getSI(getFieldMagnitude(Jx_n, Jy_n, Jz_n, mx_t, my_t, mz_t), "A/m^2"),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("J\u209A" , 					getSI(getFieldMagnitude(Jx_p, Jy_p, Jz_p, mx_t, my_t, mz_t), "A/m^2"),			hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("J" , 							getSI(getFieldMagnitude(Jx_free, Jy_free, Jz_free, mx_t, my_t, mz_t), "A/m^2"),	hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("F\u2099" , 					getSI(bilinearinterp(F_n,mx_t, my_t, mz_t)/q_n+bilinearinterp(phi,mx_t, my_t, mz_t)-W_semi/eVtoJ, "V"),	hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("F\u209a" , 					getSI(bilinearinterp(F_p,mx_t, my_t, mz_t)/q_p+bilinearinterp(phi,mx_t, my_t, mz_t)-W_semi/eVtoJ, "V"),	hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("x" , 							getSI(mx_t*ds, "m"),								hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("y" , 							getSI(my_t*ds, "m"),								hoffset, voffset + line*vspacing); line++;
				drawTwoColumnString("z" , 							getSI(mz_t*ds, "m"),								hoffset, voffset + line*vspacing); line++;
			}
		}

		drawStringBackgrounds(g, t);
		drawStrings(g, t);
		startNewStringLayer();

		int vspacing = 13;
		int voffset = 3;
		int hoffset = 5;
		int line = 1;
		drawStringWithBackground("Time: " + getSI(time, "s"), hoffset, voffset + line*vspacing); line++;
		if (opts.gui_paused.isSelected())
		{
			drawStringWithBackground("Paused", hoffset, voffset + line*vspacing); line++;
		}
		if (sign_violation) {
			drawStringWithBackground("Warning: Numerical instability detected. Please decrease timestep.", hoffset, voffset + line*vspacing); line++;
		}
		if (debugging) {
			long total = Runtime.getRuntime().totalMemory();
			long used  = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			drawStringWithBackground("Used memory " + getSI(used, "B"), hoffset, voffset + line*vspacing); line++;
			drawStringWithBackground("Total memory " + getSI(total, "B"), hoffset, voffset + line*vspacing); line++;
			drawStringWithBackground(t4.name + " " + getSI(t4.time, "s"), hoffset, voffset + line*vspacing); line++;
			drawStringWithBackground(t5.name + " " + getSI(t5.avgtime, "s"), hoffset, voffset + line*vspacing); line++;
			drawStringWithBackground(t6.name + " " + getSI(t6.avgtime*opts.gui_simspeed_2.getValue(), "s"), hoffset, voffset + line*vspacing); line++;
			drawStringWithBackground(t7.name + " " + getSI(t7.avgtime, "s"), hoffset, voffset + line*vspacing); line++;
			drawStringWithBackground(t8.name + " " + getSI(t8.avgtime, "s"), hoffset, voffset + line*vspacing); line++;
		}

		drawStringBackgrounds(g, t);
		drawStrings(g, t);

		clearStrings();
	}
	
	double getFieldMagnitude(double[][][] vx, double[][][] vy, double[][][] vz, double x, double y, double z) {
		return length(bilinearinterp(vx, x-0.5 , y, z), bilinearinterp(vy, x, y-0.5, z), bilinearinterp(vz, x, y, z-0.5));
	}
	
	double getDualFieldMagnitude(double[][][] vx, double[][][] vy, double[][][] vz, double x, double y, double z) {
		return length(bilinearinterp(vx, x, y-0.5, z-0.5), bilinearinterp(vy, x-0.5, y, z-0.5), bilinearinterp(vz, x-0.5, y-0.5, z));
	}

	class Text {
		String text;
		int x;
		int y;
		int minwidth;
		boolean big;
		boolean hasBackground;
		boolean hasBorder;

		public Text(String text, int x, int y, boolean big, boolean hasBackground, boolean hasBorder) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.big = big;
			this.hasBackground = hasBackground;
			this.hasBorder = hasBorder;
			minwidth = 0;
		}
	}

	public void clearStrings() {
		texts.clear();
	}

	public void drawStringBackgrounds(Graphics g, TextRenderer t) {
		if (!opts.gui_text_bg.isSelected())
			return;

		if (g != null) {
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
	}

	public void drawStrings(Graphics g, TextRenderer t) {
		if (g != null) {
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
		
		if (t != null) {
			for (Text text : texts) {
				//if (text.big)
				//	g.setFont(bigfont);
				//else
				//	g.setFont(regularfont);

				//t.setColor(Color.DARK_GRAY);
				//t.draw(text.text, text.x+1, imgheight-(text.y+1));
				t.setColor(Color.WHITE);
				t.draw(text.text, text.x, imgheight-(text.y));
			}
		}
	}

	public void startNewStringLayer() {
		texts.clear();
	}

	public void drawString(String str1, int x, int y) {
		texts.add(new Text(str1, x, y, false, false, false));
	}

	public void drawStringWithBackground(String str1, int x, int y) {
		texts.add(new Text(str1, x, y, false, true, false));
	}

	public void drawStringWithBackgroundAndBorder(String str1, int x, int y) {
		texts.add(new Text(str1, x, y, false, true, true));
	}

	public void drawBigString(String str1, int x, int y) {
		texts.add(new Text(str1, x, y, true, false, false));
	}

	public void drawBigStringWithBackground(String str1, int x, int y) {
		texts.add(new Text(str1, x, y, true, true, false));
	}

	public void drawTwoColumnString(String str1, String str2, int x, int y) {
		texts.add(new Text(String.format("%-10s", str1), x, y, false, true, true));
		texts.add(new Text(str2, x+40, y, false, true, true));
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

	public boolean readFile()
	{

		File testfile = new File(startingpath);
		if (!testfile.canRead()) {
			JOptionPane.showMessageDialog(opts,
			"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
		}

		JFileChooser fd = new JFileChooser(startingpath);
		fd.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				if (f.getName().endsWith(fileextension)) return true;
				return false;
			}
			@Override
			public String getDescription() {
				return fileextension;
			}
		});
		fd.setVisible(true);
		int result = fd.showOpenDialog(opts);
		startingpath = fd.getCurrentDirectory().getPath();

		if (result == JFileChooser.APPROVE_OPTION)
			infile = fd.getSelectedFile();
		else
			infile = null;
		
		if (infile == null || !infile.exists()) return false;
		try {
			JsonReader fstr = new JsonReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(infile))));
			Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

			fstr.beginObject();
			if (!fstr.nextName().equals("version")) {
				fstr.close();
				throw new IllegalArgumentException();
			}

			int version = fstr.nextInt();
			if (version != 1) {
				fstr.close();
				throw new IllegalArgumentException();
			}

			JOptionPane optionPane = new JOptionPane("Loading file, please wait.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
			JDialog dialog = optionPane.createDialog("Loading");

			dialog.setModal(false);
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.setVisible(true);


			initializeGrid(0.1e-6, 32, 32, 32);

			while (fstr.hasNext()) {
				String name = fstr.nextName();
				switch (name){
				case "time": time = fstr.nextDouble(); break;
				case "gui_paused": opts.gui_paused.setSelected(fstr.nextBoolean()); break;
				case "gui_tooltip": opts.gui_tooltip.setSelected(fstr.nextBoolean()); break;
				case "gui_text_bg": opts.gui_text_bg.setSelected(fstr.nextBoolean()); break;
				case "gui_view": opts.gui_view.setSelectedIndex(fstr.nextInt()); break;
				case "gui_view_vec": opts.gui_view_vec.setSelectedIndex(fstr.nextInt()); break;
				case "gui_view_vec_mode": opts.gui_view_vec_mode.setSelectedIndex(fstr.nextInt()); break;
				case "gui_simspeed": opts.gui_simspeed.setValue(fstr.nextInt()); break;
				case "gui_simspeed_2": opts.gui_simspeed_2.setValue(fstr.nextInt()); break;
				case "gui_brightness": opts.gui_brightness.setValue(fstr.nextInt()); break;
				case "gui_brightness_vec": opts.gui_brightness_vec.setValue(fstr.nextInt()); break;
				case "gui_elem_colors": opts.gui_elem_colors.setSelected(fstr.nextBoolean()); break;
				case "gui_bc": opts.gui_bc.setSelectedIndex(fstr.nextInt()); break;
				case "description": opts.textPane.setText(fstr.nextString()); break;
				case "gui_3d_view": opts.gui_3d_view.setSelectedIndex(fstr.nextInt()); break;
				case "gui_zslice": opts.gui_slice.setValue(fstr.nextInt()); break;
				case "pitch": renderer.pitch = (float)(fstr.nextDouble()); break;
				case "yaw": renderer.yaw = (float)(fstr.nextDouble()); break;
				case "zoom": renderer.scale = (float)(fstr.nextDouble()); break;

				case "ex": Ex = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "ey": Ey = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "ez": Ez = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "hx": Hx = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "hy": Hy = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "hz": Hz = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "bx": Bx = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "by": By = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "bz": Bz = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;

				case "rho_c": rho_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "rho_n": rho_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "rho_p": rho_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "rho_back": rho_back = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "rho_free": rho_free = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;

				case "jx_c": Jx_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jy_c": Jy_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jz_c": Jz_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jx_n": Jx_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jy_n": Jy_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jz_n": Jz_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jx_p": Jx_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jy_p": Jy_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
				case "jz_p": Jz_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;

				case "materials": materials = validateArraySize((Material[][][]) gson.fromJson(fstr, Material[][][].class)); break;

				case "voltageprobes": voltageprobes = new CopyOnWriteArrayList<>(Arrays.asList(
				(VoltageProbe[]) gson.fromJson(fstr, VoltageProbe[].class))); break;
				case "currentprobes": currentprobes = new CopyOnWriteArrayList<>(Arrays.asList(
				(CurrentProbe[]) gson.fromJson(fstr, CurrentProbe[].class))); break;

				case "ground": ground = (VoltageProbe) gson.fromJson(fstr, VoltageProbe.class); break;

				default: fstr.skipValue(); break; // skip others
				}
			}
			fstr.endObject();
			fstr.close();

			opts.textPane.setEditable(false);
			opts.textPane.setCaretPosition(0);
			constructBoundary();
			this.initializeAllMaterials();
			updateAllMaterials();
			calcMiscFields(true);

			dialog.dispose();
			opts.setTitle(sim_name + " - " + infile.getName());
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException | IllegalArgumentException e) {
			JOptionPane.showMessageDialog(opts,
			"Error: Unable to load file.");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Material[][][] validateArraySize(Material[][][] array) throws RuntimeException {
		return array;
		
		/*Material[][][] field = new Material[nx][ny][nz];

		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				for (int k = 0; k < array[i][j].length; k++) {
					if (i < nx && j < ny && k < nz) {
						field[i][j][k] = array[i][j][k];
					}
				}
			}
		}

		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					if (field[i][j][k] == null || field[i][j][k].type == MaterialType.ABSORBER)
						field[i][j][k] = new Material();
				}
			}
		}

		return field;*/
	}

	public double[][][] validateArraySize(double[][][] array) throws RuntimeException {
		return array;
		
		/*double[][][] field = new double[nx][ny][nz];

		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				for (int k = 0; k < array[i][j].length; k++) {
					if (i < nx && j < ny && k < nz) {
						field[i][j][k] = array[i][j][k];
					}
				}
			}
		}

		return field;*/
	}

	public boolean writeFile()
	{
		File testfile = new File(startingpath);
		if (!testfile.canWrite()) {
			JOptionPane.showMessageDialog(opts,
			"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
		}

		JFileChooser fd = new JFileChooser(startingpath);
		fd.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				if (f.getName().endsWith(fileextension)) return true;
				return false;
			}
			@Override
			public String getDescription() {
				return fileextension;
			}
		});
		int result = fd.showSaveDialog(opts);
		startingpath = fd.getCurrentDirectory().getPath();

		if (result == JFileChooser.APPROVE_OPTION)
			outfile = fd.getSelectedFile();
		else
			outfile = null;

		if (outfile == null) return false;
		if (!outfile.getName().endsWith(fileextension))
			outfile = new File(outfile.getAbsolutePath() + fileextension);

		if (outfile.exists()) {
			result = JOptionPane.showConfirmDialog(opts, "A file with that name already exists. Do you wish to overwrite it?", "Save file", JOptionPane.YES_NO_OPTION);
			if (result != JOptionPane.OK_OPTION)
				return false;
		}

		try {
			PrintWriter fstr = new PrintWriter(new GZIPOutputStream(new FileOutputStream(outfile)));

			Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

			JsonObject obj = new JsonObject();



			JOptionPane optionPane = new JOptionPane("Saving file, please wait.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
			JDialog dialog = optionPane.createDialog("Saving");

			dialog.setModal(false);
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.setVisible(true);

			// Version should always be first
			obj.addProperty("version", saveversion);
			obj.addProperty("time", time);

			obj.addProperty("gui_paused", opts.gui_paused.isSelected());
			obj.addProperty("gui_tooltip", opts.gui_tooltip.isSelected());
			obj.addProperty("gui_text_bg", opts.gui_text_bg.isSelected());
			obj.addProperty("gui_view", opts.gui_view.getSelectedIndex());
			obj.addProperty("gui_view_vec", opts.gui_view_vec.getSelectedIndex());
			obj.addProperty("gui_view_vec_mode", opts.gui_view_vec_mode.getSelectedIndex());
			obj.addProperty("gui_simspeed", opts.gui_simspeed.getValue());
			obj.addProperty("gui_simspeed_2", opts.gui_simspeed_2.getValue());
			obj.addProperty("gui_brightness", opts.gui_brightness.getValue());
			obj.addProperty("gui_brightness_vec", opts.gui_brightness_vec.getValue());
			obj.addProperty("gui_elem_colors", opts.gui_elem_colors.isSelected());
			obj.addProperty("gui_bc", opts.gui_bc.getSelectedIndex());
			obj.addProperty("description", opts.textPane.getText());
			obj.addProperty("gui_3d_view", opts.gui_3d_view.getSelectedIndex());
			obj.addProperty("gui_zslice", opts.gui_slice.getValue());
			obj.addProperty("pitch", renderer.pitch);
			obj.addProperty("yaw", renderer.yaw);
			obj.addProperty("zoom", renderer.scale);

			obj.add("ex", gson.toJsonTree(Ex));
			obj.add("ey", gson.toJsonTree(Ey));
			obj.add("ez", gson.toJsonTree(Ez));
			obj.add("hx", gson.toJsonTree(Hx));
			obj.add("hy", gson.toJsonTree(Hy));
			obj.add("hz", gson.toJsonTree(Hz));
			obj.add("bx", gson.toJsonTree(Bx));
			obj.add("by", gson.toJsonTree(By));
			obj.add("bz", gson.toJsonTree(Bz));

			obj.add("rho_c", gson.toJsonTree(rho_abs));
			obj.add("rho_n", gson.toJsonTree(rho_n));
			obj.add("rho_p", gson.toJsonTree(rho_p));
			obj.add("rho_back", gson.toJsonTree(rho_back));
			obj.add("rho_free", gson.toJsonTree(rho_free));

			obj.add("jx_c", gson.toJsonTree(Jx_abs));
			obj.add("jy_c", gson.toJsonTree(Jy_abs));
			obj.add("jz_c", gson.toJsonTree(Jz_abs));
			obj.add("jx_n", gson.toJsonTree(Jx_n));
			obj.add("jy_n", gson.toJsonTree(Jy_n));
			obj.add("jz_n", gson.toJsonTree(Jz_n));
			obj.add("jx_p", gson.toJsonTree(Jx_p));
			obj.add("jy_p", gson.toJsonTree(Jy_p));
			obj.add("jz_p", gson.toJsonTree(Jz_p));

			obj.add("materials", gson.toJsonTree(materials));

			obj.add("voltageprobes", gson.toJsonTree((VoltageProbe[]) voltageprobes.toArray(new VoltageProbe[voltageprobes.size()])));
			obj.add("currentprobes", gson.toJsonTree((CurrentProbe[]) currentprobes.toArray(new CurrentProbe[currentprobes.size()])));
			obj.add("ground", gson.toJsonTree(ground));

			String json = gson.toJson(obj);
			fstr.print(json);
			fstr.flush();
			fstr.close();

			dialog.dispose();
			opts.setTitle(sim_name + " - " + outfile.getName());
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
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
		if (!threeD_mode) {
			if (slice_x) {
				my = eventX;
				mz = imgheight - 1 - eventY;
				mx = indexToCoord(opts.gui_slice.getValue());
			} else if (slice_y) {
				mx = eventX;
				mz = imgheight - 1 - eventY;
				my = indexToCoord(opts.gui_slice.getValue());
			} else if (slice_z) {
				mx = eventX;
				my = imgheight - 1 - eventY;
				mz = indexToCoord(opts.gui_slice.getValue());
			}
		}
	}
	
	boolean update3dCursor = false;

	public int indexToCoord(int n) {
		return (int)(scalefactor*(n + 0.5) + 1);
	}

	@SuppressWarnings("serial")
	private Action key_pause = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			opts.gui_paused.setSelected(!opts.gui_paused.isSelected());
		}
	};

	@SuppressWarnings("serial")
	private Action key_frame = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			advanceframe = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_dbg = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			debugging = !debugging;
			Timer.allEnabled = debugging;
		}
	};

	@SuppressWarnings("serial")
	private Action key_changebrush = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			opts.gui_brush_1.setSelectedIndex((opts.gui_brush_1.getSelectedIndex()+1)%2);
		}
	};

	@SuppressWarnings("serial")
	private Action key_shift = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			shift_down = true;
		}
	};
	@SuppressWarnings("serial")
	private Action key_shift_up = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			shift_down = false;
		}
	};
	@SuppressWarnings("serial")
	private Action key_ctrl = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			ctrl_down = true;
		}
	};
	@SuppressWarnings("serial")
	private Action key_ctrl_up = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			ctrl_down = false;
		}
	};

	@SuppressWarnings("serial")
	private Action key_cut = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			cut = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_copy = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			copy = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_paste = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			paste = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_delete = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			delete = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_color = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			opts.gui_elem_colors.setSelected(!opts.gui_elem_colors.isSelected());
		}
	};

	ScalarView prev_scalar_view = ScalarView.NONE;
	VectorView prev_vector_view = VectorView.NONE;

	@SuppressWarnings("serial")
	private Action key_scalar_view = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			if (opts.gui_view.getSelectedItem() == ScalarView.NONE)
				opts.gui_view.setSelectedItem(prev_scalar_view);
			else
			{
				prev_scalar_view = (ScalarView) opts.gui_view.getSelectedItem();
				opts.gui_view.setSelectedItem(ScalarView.NONE);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action key_vector_view = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			if (opts.gui_view_vec.getSelectedItem() == VectorView.NONE)
				opts.gui_view_vec.setSelectedItem(prev_vector_view);
			else
			{
				prev_vector_view = (VectorView) opts.gui_view_vec.getSelectedItem();
				opts.gui_view_vec.setSelectedItem(VectorView.NONE);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action key_tooltip = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			opts.gui_tooltip.setSelected(!opts.gui_tooltip.isSelected());
		}
	};

	@SuppressWarnings("serial")
	private Action key_textbg = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			opts.gui_text_bg.setSelected(!opts.gui_text_bg.isSelected());
		}
	};

	@SuppressWarnings("serial")
	private Action key_alt = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			alt_down = true;
		}
	};

	@SuppressWarnings("serial")
	private Action key_alt_up = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			alt_down = false;
		}
	};

	@SuppressWarnings("serial")
	private Action key_3d = new AbstractAction(null) {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (texting) return;
			//set3Dmode(threeD_mode);
		}
	};
	
	public void set3Dmode(boolean threeD_mode) {
		this.threeD_mode = threeD_mode;
		
		if (threeD_mode) {
			opts.remove(r);
			opts.add(renderer.canvas, BorderLayout.CENTER);
			opts.pack();
		} else {
			opts.remove(renderer.canvas);
			opts.add(r, BorderLayout.CENTER);
			opts.pack();
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


		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), key_cut);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), key_cut);
		contentPane.getActionMap().put(key_cut, key_cut);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), key_copy);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), key_copy);
		contentPane.getActionMap().put(key_copy, key_copy);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), key_paste);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), key_paste);
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
		
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, KeyEvent.SHIFT_DOWN_MASK), key_shift);
		contentPane.getActionMap().put(key_shift, key_shift);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true), key_shift_up);
		contentPane.getActionMap().put(key_shift_up, key_shift_up);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, KeyEvent.CTRL_DOWN_MASK), key_ctrl);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_META, KeyEvent.META_DOWN_MASK), key_ctrl);
		contentPane.getActionMap().put(key_ctrl, key_ctrl);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL, 0, true), key_ctrl_up);
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_META, 0, true), key_ctrl_up);
		contentPane.getActionMap().put(key_ctrl_up, key_ctrl_up);
		
		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, KeyEvent.ALT_DOWN_MASK), key_alt);
		contentPane.getActionMap().put(key_alt, key_alt);

		map.put(KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0, true), key_alt_up);
		contentPane.getActionMap().put(key_alt_up, key_alt_up);


	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (shift_down)
			opts.gui_brushsize.setValue(opts.gui_brushsize.getValue() - (int)(10*e.getPreciseWheelRotation()));
		else
			renderer.scale *= Math.exp(-(int)(10*e.getPreciseWheelRotation())/100.0);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof javax.swing.Timer)
			this.run();
		else if (e.getSource() == opts.gui_reset)
			clear = true;
		else if (e.getSource() == opts.gui_resetall)
			reset = true;
		else if (e.getSource() == opts.gui_save)
			save = true;
		else if (e.getSource() == opts.gui_open)
			load = true;
		else if (e.getSource() == opts.gui_help)
			help.setVisible(true);
		else if (e.getSource() == opts.gui_editdesc) {
			opts.textPane.setEditable(!opts.textPane.isEditable());
		} else if (e.getSource() == opts.gui_view) {
			//updateMiscFields = true;
		} else if (e.getSource() == opts.gui_view_vec) {
			//updateMiscFields = true;
		} else if (e.getSource() == opts.gui_brush) {
			brush_changed = true;
		} else if (e.getSource() == opts.gui_3d_view) {
			RenderingMode mode = (RenderingMode) opts.gui_3d_view.getSelectedItem();
			set3Dmode(RenderingMode.is3d(mode));
			opts.gui_slice.setVisible(!RenderingMode.is3d(mode));
			opts.gui_slicelabel.setVisible(!RenderingMode.is3d(mode));
			slice_x = (mode == RenderingMode.SLICE_X);
			slice_y = (mode == RenderingMode.SLICE_Y);
			slice_z = (mode == RenderingMode.SLICE_Z);
			generateCanvas();
			opts.pack();
			int tmp = opts.gui_slice.getValue();
			opts.gui_slice.setValue(0);
			opts.gui_slice.setValue(1);
			opts.gui_slice.setValue(tmp);
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
		FLOODSELECT("Select region"),
		TEXT("Add text");

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
	};

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
	};

	enum ScalarView {
		NONE("No scalar overlay"),
		E_FIELD("View E field magnitude"),
		H_FIELD("View H field magnitude"),
		CHARGE("View \u03c1: Net charge density"),
		CURRENT("View J: Total current magnitude"),
		POTENTIAL("View \u03d5: Electric scalar potential"),
		ENERGY("View u: Electromagnetic energy density"),
		ELECTRON_CHARGE("View \u03c1\u2099: Electron charge density"),
		HOLE_CHARGE("View \u03c1\u209A: Hole charge density"),
		COMBINED_CHARGE("View: Combined electron+hole charge density"),
		BACKGROUND_CHARGE("View \u03c1\u2080: Background charge density"),
		HEAT("View Q: Heat dissipation"),
		ENTROPY("View s: Entropy generation (Free energy dissipation)"),
		ELECTRON_POTENTIAL("View F\u2099: Electron chemical potential (quasi Fermi level)"),
		HOLE_POTENTIAL("View F\u209A: Hole chemical potential (quasi Fermi level)"),
		AVERAGE_POTENTIAL("View F: Average electrochemical potential"),
		RECOMBINATION("View R: Recombination rate"),
		LIGHT("View: Emitted light"),
		DEBUG("Debug"),
		DEBUG2("Debug 2");

		String name;
		ScalarView(String name)
		{
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	enum VectorView {
		NONE("No vector overlay"),
		E_FIELD("View E field"),
		B_FIELD("View B field"),
		H_FIELD("View H field"),
		ELECTRON_CURRENT("View J\u2099: Electron current"),
		HOLE_CURRENT("View J\u209A: Hole current"),
		TOTAL_CURRENT("View J: Total current"),
		EMF("View \u2130: External electromotive force"),
		POYNTING("View S: Poynting vector");

		String name;
		VectorView(String name)
		{
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

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
	};

	enum BoundaryCondition {
		DISSIPATIVE("Absorbing boundary"),
		CONDUCTING("Conducting boundary");

		String name;
		BoundaryCondition(String name)
		{
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
	
	enum RenderingMode {
		SLICE_X("2D x cross-section"),
		SLICE_Y("2D y cross-section"),
		SLICE_Z("2D z cross-section"),
		THREED("3D orthographic"),
		THREED_FIELDS_ONLY("3D orthographic (fields only)"),
		THREED_TRANSLUCENT("3D orthographic (transparent)"),
		THREED_PERSPECTIVE("3D perspective"),
		THREED_PERSPECTIVE_FIELDS_ONLY("3D perspective (fields only)"),
		THREED_PERSPECTIVE_TRANSLUCENT("3D perspective (transparent)");

		String name;
		RenderingMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		public static boolean is3d(RenderingMode mode) {
			return (mode == THREED || mode == THREED_FIELDS_ONLY || mode == THREED_TRANSLUCENT
			|| mode == THREED_PERSPECTIVE || mode == THREED_PERSPECTIVE_FIELDS_ONLY || mode == THREED_PERSPECTIVE_TRANSLUCENT);
		}

		public static boolean isOrthographic(RenderingMode mode) {
			return (mode == THREED || mode == THREED_FIELDS_ONLY || mode == THREED_TRANSLUCENT);
		}

		public static boolean isPerspective(RenderingMode mode) {
			return (mode == THREED_PERSPECTIVE || mode == THREED_PERSPECTIVE_FIELDS_ONLY || mode == THREED_PERSPECTIVE_TRANSLUCENT);
		}
	}

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
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getSource() == opts.gui_slice) {
			if (slice_x) {
				opts.gui_slicelabel.setText("Slice x = " + getSI(opts.gui_slice.getValue()*ds, "m"));
			} else if (slice_y) {
				opts.gui_slicelabel.setText("Slice y = " + getSI(opts.gui_slice.getValue()*ds, "m"));
			} else if (slice_z) {
				opts.gui_slicelabel.setText("Slice z = " + getSI(opts.gui_slice.getValue()*ds, "m"));
			}
		}
	}
}

class CurrentProbe {
	int x1;
	int y1;
	int z1;
	int x2;
	int y2;
	int z2;

	double current = 0;
}

class VoltageProbe {
	int x;
	int y;
	int z;

	double potential = 0;
}

class RenderCanvas extends JPanel {

	private static final long serialVersionUID = 7369516276529576171L;

	Electrodynamics parent;
	@Override
	public void paintComponent(Graphics real) {
		parent.render();
		real.drawImage(parent.screen, 0, 0, parent.opts);
	}

	public RenderCanvas(Electrodynamics w) {
		parent = w;
	}
}

class Timer {
	long tstart = 0;
	String name;
	boolean enabled = true;
	boolean outputavg = false;
	double avgtime = 0;
	double time = 0;
	static boolean allEnabled = false;

	public Timer(String name, boolean enabled) {
		this.name = name;
		this.enabled = enabled;
	}

	void start() {
		if (enabled) {
			tstart = System.nanoTime();
		}
	}

	void disableOutput() {
		enabled = false;
	}
	void enableOutput() {
		enabled = true;
	}

	void stop() {
		if (allEnabled && enabled) {
			long tend = System.nanoTime();
			long diff = tend - tstart;
			time = diff/1e9;
			avgtime = avgtime*0.99+time*0.01;
		}
	}

	void stop(String msg) {
		if (allEnabled && enabled) {
			long tend = System.nanoTime();
			long diff = tend - tstart;
			time = diff/1e9;
			avgtime = avgtime*0.95+time*0.05;
		}
	}
}

class Vector {
	double x;
	double y;
	double z;

	public Vector(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector copy() {
		return new Vector(x, y, z);
	}

	public void copy(Vector b) {
		this.x = b.x;
		this.y = b.y;
		this.z = b.z;
	}

	public void initialize(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void add(Vector b) {
		x += b.x;
		y += b.y;
		z += b.z;
	}

	public void scalarmult(double c) {
		x *= c;
		y *= c;
		z *= c;
	}

	public void addmult(Vector b, double c) {
		x += b.x * c;
		y += b.y * c;
		z += b.z * c;
	}

	public void rotate_z(double theta) {
		double xf = x*Math.cos(theta) + y*Math.sin(theta);
		double yf = -x*Math.sin(theta) + y*Math.cos(theta);
		x = xf;
		y = yf;
	}

	public void normalize() {
		double magnitude = Math.sqrt(x*x+y*y+z*z);
		if (magnitude != 0) {
			x /= magnitude;
			y /= magnitude;
			z /= magnitude;
		}
	}

	public double dot(Vector b) {
		return this.x * b.x + this.y * b.y + this.z * b.z;
	}

	public void cross(Vector b) {
		double ax = x;
		double ay = y;
		double az = z;

		x = ay*b.z - az*b.y;
		y = az*b.x - ax*b.z;
		z = ax*b.y - ay*b.x;
	}
}

enum MaterialType
{

	EMF					("Voltage source (Adjustable)",			230, 216, 46, 230),
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

class Material implements Cloneable {
	MaterialType type = MaterialType.VACUUM;

	boolean modified = false;
	int activated = 1;
	int conducting = 0;
	int semiconducting = 0;
	double emf = 0.0;			// EMF strength
	double emf_direction = 0.0;	// EMF direction
	double eps_r = 1.0;			// Permittivity
	double mu_r = 1.0;			// Permeability
	double rho_back = 0.0;		// Background charge density
	double ni = 0;				// Equilibrium carrier density
	double W = 0;				// Work function
	double Eb = 0;				// Bandgap
	double Ea = 0;				// Recombination activation energy
	double absorptivity = 0.0;

	public void erase() {
		type = MaterialType.VACUUM;
		modified = false;
		activated = 1;
		conducting = 0;
		semiconducting = 0;
		emf = 0.0;
		emf_direction = 0.0;
		eps_r = 1.0;
		mu_r = 1.0;
		rho_back = 0.0;
		ni = 0;
		W = 0;
		Eb = 0;
		Ea = 0;
		absorptivity = 0;
	}

	@Override
	public Material clone() {
		try {
			return (Material) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}