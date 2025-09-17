// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Electromagnetic Simulation which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.InputMap;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import electrodynamics.util.PeriodicTask;
import electrodynamics.util.Timer;
import electrodynamics.util.Utils;

public class Electrodynamics extends PeriodicTask {
	//TODO:
	// Make colors more distinguishable
	// Add more instructions
	// Draw strings in 3d
	// Fix probes
	// Lines

	// Probes
	// Adjust vf brightness
	// Fix perspective projection
	// Update text rendering
	// Add data output

	// Demos:
	// Inductor
	// Capacitor
	// Transformer
	
	// Fix view saving


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

	double[][][] Dx;				// Electric displacement field
	double[][][] Dy;
	double[][][] Dz;
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

	double[][][] ac_x;
	double[][][] ac_y;
	double[][][] ac_z;

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

	double AC_phase;
	double AC_freq;
	double AC_amplitude;
	boolean AC_source_exists;


	/* Multithreading */

	//int n_threads = 5;
	
	ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	ReentrantLock poissonLock = new ReentrantLock(true);
	
	CyclicBarrier start_barrier = new CyclicBarrier(SemiSim3D.n_threads + 1);
	CyclicBarrier stop_barrier = new CyclicBarrier(SemiSim3D.n_threads + 1);
	CyclicBarrier mid_barrier = new CyclicBarrier(SemiSim3D.n_threads);




	boolean disable_semiconductors = true;

	/* Performance profiling */

	Timer t4 = new Timer("Poisson constraint solver", 20, true);
	Timer t7 = new Timer("Poisson potential solver", 20, true);
	Timer t6 = new Timer("Iterate simulation", 20, true);
	Timer t8 = new Timer("Calc misc fields", 20, true);
	Timer t9 = new Timer("Debug", 20, false);
	Timer simFPStimer = new Timer("Simulation FPS", 10, true);


	String sim_name = "Brandon's Electromagnetic Simulation 3D";
	Renderer renderer;
	Controls controls;
	MainWindow opts;
	BufferedImage screen;
	SaveManager savemanager;

	public Electrodynamics() {
		controls = new Controls(this);
		renderer = new Renderer(this);
		savemanager = new SaveManager(this);
		opts = new MainWindow();

		SemiSim3D.detect64Bit();
		
		initializeGrid(0.1e-6, 32, 32, 32);

		renderer.create3dCanvas();

		renderer.imgpanel = new JPanel();
		renderer.imgpanel.setLayout(new GridLayout(1,2));
		renderer.imgpanel.setPreferredSize(new Dimension(768, 768));
		opts.add(renderer.imgpanel, BorderLayout.CENTER);

		renderer.set3Dmode();

		renderer.canvas.addMouseListener(controls);
		renderer.canvas.addMouseMotionListener(controls);
		renderer.canvas.addMouseWheelListener(controls);
		renderer.canvas.addKeyListener(controls);

		renderer.renderer_left_eye.canvas.addMouseListener(controls);
		renderer.renderer_left_eye.canvas.addMouseMotionListener(controls);
		renderer.renderer_left_eye.canvas.addMouseWheelListener(controls);
		renderer.renderer_left_eye.canvas.addKeyListener(controls);

		renderer.renderer_right_eye.canvas.addMouseListener(controls);
		renderer.renderer_right_eye.canvas.addMouseMotionListener(controls);
		renderer.renderer_right_eye.canvas.addMouseWheelListener(controls);
		renderer.renderer_right_eye.canvas.addKeyListener(controls);

		opts.gui_reset.addActionListener(controls);
		opts.gui_resetall.addActionListener(controls);
		opts.gui_save.addActionListener(controls);
		opts.gui_open.addActionListener(controls);
		opts.gui_help.addActionListener(controls);
		opts.gui_editdesc.addActionListener(controls);
		opts.gui_view.addActionListener(controls);
		opts.gui_view_vec.addActionListener(controls);
		opts.gui_brush.addActionListener(controls);
		opts.gui_material.addActionListener(controls);
		opts.gui_slice.addAdjustmentListener(controls);
		opts.gui_3d_view.addActionListener(controls);
		opts.gui_parallax.addAdjustmentListener(controls);

		opts.gui_slice.setVisible(false);
		opts.gui_slicelabel.setVisible(false);

		opts.gui_material.removeItem(MaterialType.ABSORBER);
		opts.gui_view.removeItem(ScalarView.DEBUG);
		opts.gui_view.removeItem(ScalarView.DEBUG2);

		if (disable_semiconductors) {
			opts.gui_material.removeItem(MaterialType.SEMI);
			opts.gui_material.removeItem(MaterialType.SEMI_N_TYPE);
			opts.gui_material.removeItem(MaterialType.SEMI_P_TYPE);
			opts.gui_material.removeItem(MaterialType.SEMI_LIGHT_N_TYPE);
			opts.gui_material.removeItem(MaterialType.SEMI_LIGHT_P_TYPE);
			opts.gui_material.removeItem(MaterialType.SEMI_HEAVY_N_TYPE);
			opts.gui_material.removeItem(MaterialType.SEMI_HEAVY_P_TYPE);
			opts.gui_material.removeItem(MaterialType.METAL_HIGH_W);
			opts.gui_material.removeItem(MaterialType.METAL_LOW_W);
			
			opts.gui_view.removeItem(ScalarView.BACKGROUND_CHARGE);
			opts.gui_view.removeItem(ScalarView.COMBINED_CHARGE);
			opts.gui_view.removeItem(ScalarView.ELECTRON_CHARGE);
			opts.gui_view.removeItem(ScalarView.ELECTRON_POTENTIAL);
			opts.gui_view.removeItem(ScalarView.HOLE_CHARGE);
			opts.gui_view.removeItem(ScalarView.HOLE_POTENTIAL);
			opts.gui_view.removeItem(ScalarView.RECOMBINATION);
			opts.gui_view.removeItem(ScalarView.LIGHT);
			opts.gui_view.removeItem(ScalarView.AVERAGE_POTENTIAL);
			
			opts.gui_view_vec.removeItem(VectorView.ELECTRON_CURRENT);
			opts.gui_view_vec.removeItem(VectorView.HOLE_CURRENT);
		}

		opts.pack();

		controls.addKeyBinds(renderer.canvas);
		controls.addKeyBinds(opts.contentPane);
		controls.addKeyBinds(renderer.imgpanel);

		InputMap im = (InputMap)UIManager.get("Button.focusInputMap");
		im.put(KeyStroke.getKeyStroke("pressed SPACE"), "none");
		im.put(KeyStroke.getKeyStroke("released SPACE"), "none");
		

		//help = new HelpDialog();
		//help.setVisible(false);
	}

	@Override
	public void run() {
		rwLock.readLock().lock();
		try {
			try {
				int iterationmultiplier = opts.gui_simspeed_2.getValue();

				if (controls.clear) {
    				SwingUtilities.invokeLater(() -> {
    					resetFields(false);
    					multigridSolve(true, false);
    				});
					time = 0.0;
					controls.clear = false;
				}

    			if (controls.reset) {
    				SwingUtilities.invokeLater(() -> {
        				int result = JOptionPane.showConfirmDialog(opts, "Do you wish to reset the entire simulation?", "Message", JOptionPane.YES_NO_OPTION);
        				if (result == JOptionPane.OK_OPTION)
        				{
        					resetFields(true);
        					time = 0.0;
        				}
    				});
    				controls.reset = false;
    			}

				if (opts.gui_simspeed.getValue() != lastsimspeed) {
					lastsimspeed = opts.gui_simspeed.getValue();
					dt = dt_maximum*(lastsimspeed/20.0);
				}

				controls.handleMouseInput();

				if (!opts.gui_paused.isSelected() || controls.advanceframe) {
					for (int i = 0; i < iterationmultiplier ; i++) {
						start_barrier.await();
						stop_barrier.await();
					}

					calcMiscFields(false);

					frame++;
				} else if (updateMiscFields) {
					calcMiscFields(false);
				}


				if (controls.save) {
					savemanager.writeFile();
					controls.save = false;
				}

				if (controls.load) {
					savemanager.readFile();
					controls.load = false;
				}


				simFPStimer.stop();
				simFPStimer.start();

			} catch (Exception e) {
				SemiSim3D.displayErrorMessage(e);
			}
		} finally {
			rwLock.readLock().unlock();
		}

        SemiSim3D.instance.threadPool.schedule(this, nextDelay(renderer.frameduration), TimeUnit.MILLISECONDS);
	}
	

	TimerTask potentialSolver = new PeriodicTask() {
		@Override
		public void run() {
	        rwLock.readLock().lock();
	        try {
				if (!opts.gui_paused.isSelected() || controls.advanceframe || updateMiscFields) {
					multigridSolve(false, true);
					//calcMiscFields(true);
				}
	        } catch( Exception e) {
	        	SemiSim3D.displayErrorMessage(e);
	        } finally {
	            rwLock.readLock().unlock();
	        }
	        SemiSim3D.instance.threadPool.schedule(this, nextDelay(renderer.frameduration), TimeUnit.MILLISECONDS);
		}
	};

	public void initializeGrid(double ds, int x_resolution, int y_resolution, int z_resolution) {
		this.ds = ds;
		nx = x_resolution;
		ny = y_resolution;
		nz = z_resolution;

		Utils.nx = nx;
		Utils.ny = ny;
		Utils.nz = nz;

		int min_res = Math.min(Math.min(nx, ny), nz);
		min_width = min_res*ds;
		MG_levels = (int)Math.round(Math.log(min_res/4)/Math.log(2))+1;

		dt_maximum = 0.9*ds/(Math.sqrt(3)*c);

		H_dissipation = 0.001*ds*ds/dt_maximum;

		rwLock.writeLock().lock();
		try {
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
			ac_x = new double[nx][ny][nz];
			ac_y = new double[nx][ny][nz];
			ac_z = new double[nx][ny][nz];
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

			Dx = new double[nx][ny][nz];
			Dy = new double[nx][ny][nz];
			Dz = new double[nx][ny][nz];
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


			voltageprobes = new CopyOnWriteArrayList<>();
			currentprobes = new CopyOnWriteArrayList<>();

			renderer.initializeGrid(ds, x_resolution, y_resolution, z_resolution);
			controls.initializeGrid(ds, x_resolution, y_resolution, z_resolution);

			resetFields(true);
			multigridSolve(true, false);
			calcMiscFields(true);

			opts.setVisible(true);
		} finally {
			rwLock.writeLock().unlock();
		}
		
	}

	public void resetFields(boolean resetall) {

		rwLock.writeLock().lock();
		try {
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

							if (controls.selection[i][j][k] == null)
								controls.selection[i][j][k] = new Material();
							else
								controls.selection[i][j][k].erase();

							if (controls.clipboard[i][j][k] == null)
								controls.clipboard[i][j][k] = new Material();

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
							epsx[i][j][k] = eps0;
							mu_x[i][j][k] = mu0;
							mu_y[i][j][k] = mu0;
							mu_z[i][j][k] = mu0;

							conducting[i][j][k] = 0;
							conducting_x[i][j][k] = 0;
							conducting_y[i][j][k] = 0;
							conducting_z[i][j][k] = 0;

							ac_x[i][j][k] = 0.0;
							ac_y[i][j][k] = 0.0;
							ac_z[i][j][k] = 0.0;

							//H_absorptivity_x[i][j][k] = 0;
							//H_absorptivity_y[i][j][k] = 0;
							//H_absorptivity_z[i][j][k] = 0;
							absorptivity_x[i][j][k] = 0;
							absorptivity_y[i][j][k] = 0;
							absorptivity_z[i][j][k] = 0;
						}

						Ex[i][j][k] = 0.0;
						Ey[i][j][k] = 0.0;
						Ez[i][j][k] = 0.0;
						Hx[i][j][k] = 0.0;
						Hy[i][j][k] = 0.0;
						Hz[i][j][k] = 0.0;
						Bx[i][j][k] = 0.0;
						Bx[i][j+1][k] = 0.0;
						Bx[i][j][k+1] = 0.0;
						Bx[i][j+1][k+1] = 0.0;
						By[i][j][k] = 0.0;
						By[i+1][j][k] = 0.0;
						By[i][j][k+1] = 0.0;
						By[i+1][j][k+1] = 0.0;
						Bz[i][j][k] = 0.0;
						Bz[i+1][j][k] = 0.0;
						Bz[i][j+1][k] = 0.0;
						Bz[i+1][j+1][k] = 0.0;
						Bx_laplacian[i][j][k] = 0.0;
						By_laplacian[i][j][k] = 0.0;
						Bz_laplacian[i][j][k] = 0.0;

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

						MG_phi1[i][j][k] = 0.0;
						MG_phi2[i][j][k] = 0.0;

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

						controls.selected[i][j][k] = false;
						controls.selected_EMF[i][j][k] = false;
					}
				}
			}

			if (resetall) {
				voltageprobes.clear();
				currentprobes.clear();
				ground = null;

				opts.setTitle(sim_name);
			}

			initializeAllMaterials();
			updateAllMaterials();
			checkCFL();
		}
		finally {
			rwLock.writeLock().unlock();
		}
	}

	public void constructBoundary() {
		absorber_width = 2;
		absorbing_coeff = 100*c/min_width;
		double max_stretch = 4;
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

									double sigma_n = conducting_x[i][j][k]*mf*mu_electron*Utils.logmean(-rho_n[i+1][j][k],-rho_n[i][j][k]);
									double sigma_p = conducting_x[i][j][k]*mf*mu_hole*Utils.logmean(rho_p[i+1][j][k], rho_p[i][j][k]);

									double emf_phase = ac_x[i][j][k]*AC_amplitude+(1-ac_x[i][j][k]);
									
									Jx_abs[i][j][k] = 0;

									Jx_n[i][j][k] = conducting_x[i][j][k]*(-mf*D_electron*(rho_n[i+1][j][k] - rho_n[i][j][k])/ds
									+ sigma_n*(emf_phase*emfx[i][j][k] + cmfx_n[i][j][k]/q_n));

									Jx_p[i][j][k] = conducting_x[i][j][k]*(-mf*D_hole*(rho_p[i+1][j][k] - rho_p[i][j][k])/ds
									+ sigma_p*(emf_phase*emfx[i][j][k] + cmfx_p[i][j][k]/q_p));

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

									double sigma_n = conducting_y[i][j][k]*mf*mu_electron*Utils.logmean(-rho_n[i][j+1][k],-rho_n[i][j][k]);
									double sigma_p = conducting_y[i][j][k]*mf*mu_hole*Utils.logmean(rho_p[i][j+1][k], rho_p[i][j][k]);

									double emf_phase = ac_y[i][j][k]*AC_amplitude+(1-ac_y[i][j][k]);
									
									Jy_abs[i][j][k] = 0;

									Jy_n[i][j][k] = conducting_y[i][j][k]*(-mf*D_electron*(rho_n[i][j+1][k] - rho_n[i][j][k])/ds
									+ sigma_n*(emf_phase*emfy[i][j][k] + cmfy_n[i][j][k]/q_n));

									Jy_p[i][j][k] = conducting_y[i][j][k]*(-mf*D_hole*(rho_p[i][j+1][k] - rho_p[i][j][k])/ds
									+ sigma_p*(emf_phase*emfy[i][j][k] + cmfy_p[i][j][k]/q_p));

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

									double sigma_n = conducting_z[i][j][k]*mf*mu_electron*Utils.logmean(-rho_n[i][j][k+1],-rho_n[i][j][k]);
									double sigma_p = conducting_z[i][j][k]*mf*mu_hole*Utils.logmean(rho_p[i][j][k+1], rho_p[i][j][k]);

									double emf_phase = ac_z[i][j][k]*AC_amplitude+(1-ac_z[i][j][k]);
									
									Jz_abs[i][j][k] = 0;

									Jz_n[i][j][k] = conducting_z[i][j][k]*(-mf*D_electron*(rho_n[i][j][k+1] - rho_n[i][j][k])/ds
									+ sigma_n*(emf_phase*emfz[i][j][k] + cmfz_n[i][j][k]/q_n));

									Jz_p[i][j][k] = conducting_z[i][j][k]*(-mf*D_hole*(rho_p[i][j][k+1] - rho_p[i][j][k])/ds
									+ sigma_p*(emf_phase*emfz[i][j][k] + cmfz_p[i][j][k]/q_p));


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

					// Magnetic field boundary condition
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
						AC_phase += 2*Math.PI*AC_freq*dt;
						AC_amplitude = Math.cos(AC_phase);

						controls.advanceframe = false;
					}

					stop_barrier.await();
				}
			} catch (InterruptedException | BrokenBarrierException e) {
				e.printStackTrace();
			}
		}
	}

	public void updateAllMaterials() {
		constructBoundary();

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
					emfx[i][j][k] = Utils.minAbs(materials[i+1][j][k].emf_x*materials[i+1][j][k].emf, materials[i][j][k].emf_x*materials[i][j][k].emf);
					epsx[i][j][k] = eps0*0.5*(materials[i+1][j][k].eps_r + materials[i][j][k].eps_r);
					absorptivity_x[i][j][k] = Math.min(materials[i+1][j][k].absorptivity, materials[i][j][k].absorptivity);
					ac_x[i][j][k] = (materials[i][j][k].type == MaterialType.AC_EMF || materials[i+1][j][k].type == MaterialType.AC_EMF) ? 1:0;
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
					emfy[i][j][k] = Utils.minAbs(materials[i][j+1][k].emf_y*materials[i][j+1][k].emf, materials[i][j][k].emf_y*materials[i][j][k].emf);
					epsy[i][j][k] = eps0*0.5*(materials[i][j+1][k].eps_r + materials[i][j][k].eps_r);
					absorptivity_y[i][j][k] = Math.min(materials[i][j+1][k].absorptivity, materials[i][j][k].absorptivity);
					ac_y[i][j][k] = (materials[i][j][k].type == MaterialType.AC_EMF || materials[i][j+1][k].type == MaterialType.AC_EMF) ? 1:0;
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
					emfz[i][j][k] = Utils.minAbs(materials[i][j][k+1].emf_z*materials[i][j][k+1].emf, materials[i][j][k].emf_z*materials[i][j][k].emf);
					epsz[i][j][k] = eps0*0.5*(materials[i][j][k+1].eps_r + materials[i][j][k].eps_r);
					absorptivity_z[i][j][k] = Math.min(materials[i][j][k+1].absorptivity, materials[i][j][k].absorptivity);
					ac_z[i][j][k] = (materials[i][j][k].type == MaterialType.AC_EMF || materials[i][j][k+1].type == MaterialType.AC_EMF) ? 1:0;
				}
			}
		}

		computeChemicalForces();

		AC_source_exists = false;
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

					if (materials[i][j][k].type == MaterialType.AC_EMF)
						AC_source_exists = true;
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


		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					Hx[i][j][k] = Bx[i][j+1][k+1]/mu_x[i][j][k];
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					Hy[i][j][k] = By[i+1][j][k+1]/mu_y[i][j][k];
				}
			}
		}

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					Hz[i][j][k] = Bz[i+1][j+1][k]/mu_z[i][j][k];
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
					emfx[i][j][k] = Utils.minAbs(materials[i+1][j][k].emf_x*materials[i+1][j][k].emf, materials[i][j][k].emf_x*materials[i][j][k].emf);
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					emfy[i][j][k] = Utils.minAbs(materials[i][j+1][k].emf_y*materials[i][j+1][k].emf, materials[i][j][k].emf_y*materials[i][j][k].emf);
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					emfz[i][j][k] = Utils.minAbs(materials[i][j][k+1].emf_z*materials[i][j][k+1].emf, materials[i][j][k].emf_z*materials[i][j][k].emf);
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
							if (distance[i][j][k] < smallest_length && !visited[i][j][k] && conducting[i][j][k] == 1) {
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
					Dx[i][j][k] = epsx[i][j][k]*Ex[i][j][k];
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
					Dy[i][j][k] = epsy[i][j][k]*Ey[i][j][k];
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
					Dz[i][j][k] = epsz[i][j][k]*Ez[i][j][k];
					Jz_free[i][j][k] = Jz_abs[i][j][k] + Jz_n[i][j][k] + Jz_p[i][j][k];
					Sz[i][j][k] = 0.125*((Hy[i][j][k]*(Ex[i][j][k+1] + Ex[i][j][k]) + Hy[i-1][j][k]*(Ex[i-1][j][k+1] - Ex[i-1][j][k]))
					- (Hx[i][j][k]*(Ey[i][j][k+1] + Ey[i][j][k]) + Hx[i][j-1][k]*(Ey[i][j-1][k+1] + Ey[i][j-1][k])));
				}
			}
		}

		if (view_scalar == ScalarView.ENERGY) {

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					for (int k = 1; k < nz-1; k++)
					{

						u[i][j][k] = 0.25*(Ex[i][j][k]*Ex[i][j][k]*epsx[i][j][k] + Ex[i-1][j][k]*Ex[i-1][j][k]*epsx[i-1][j][k])
								+ 0.25*(Ey[i][j][k]*Ey[i][j][k]*epsy[i][j][k] + Ey[i][j-1][k]*Ey[i][j-1][k]*epsy[i][j-1][k])
								+ 0.25*(Ez[i][j][k]*Ez[i][j][k]*epsz[i][j][k] + Ez[i][j][k-1]*Ez[i][j][k-1]*epsz[i][j][k-1])
								+ 0.125*(Hx[i][j][k]*Hx[i][j][k]*mu_x[i][j][k]+Hx[i][j][k-1]*Hx[i][j][k-1]*mu_x[i][j][k-1]+Hx[i][j-1][k]*Hx[i][j-1][k]*mu_x[i][j-1][k]+Hx[i][j-1][k-1]*Hx[i][j-1][k-1]*mu_x[i][j-1][k-1])
								+ 0.125*(Hy[i][j][k]*Hy[i][j][k]*mu_y[i][j][k]+Hy[i-1][j][k]*Hy[i-1][j][k]*mu_y[i-1][j][k]+Hy[i][j][k-1]*Hy[i][j][k-1]*mu_y[i][j][k-1]+Hy[i-1][j][k-1]*Hy[i-1][j][k-1]*mu_y[i-1][j][k-1])
								+ 0.125*(Hz[i][j][k]*Hz[i][j][k]*mu_z[i][j][k]+Hz[i-1][j][k]*Hz[i-1][j][k]*mu_z[i-1][j][k]+Hz[i][j-1][k]*Hz[i][j-1][k]*mu_z[i][j-1][k]+Hz[i-1][j-1][k]*Hz[i-1][j-1][k]*mu_z[i-1][j-1][k]);
					}
				}
			}
		}

		/*for (int i = 0; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					debug[i][j][k] = (Bx[i+1][j+1][k+1]-Bx[i][j+1][k+1]+By[i+1][j+1][k+1]-By[i+1][j][k+1]+Bz[i+1][j+1][k+1]-Bz[i+1][j+1][k])/ds;
					debug2[i][j][k] = (Hx[i+1][j][k]-Hx[i][j][k]+Hy[i][j+1][k]-Hy[i][j][k]+Hz[i][j][k+1]-Hz[i][j][k])/ds;
				}
			}
		}*/


		if (view_scalar == ScalarView.HEAT) {
			for (int i = 0; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					for (int k = 1; k < nz-1; k++)
					{
						grad_E0x_n[i][j][k] = -conducting_x[i][j][k]*(E0_n[i+1][j][k] - E0_n[i][j][k])/ds;
						grad_E0x_p[i][j][k] = -conducting_x[i][j][k]*(E0_p[i+1][j][k] - E0_p[i][j][k])/ds;
					}
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 0; j < ny-1; j++)
				{
					for (int k = 1; k < nz-1; k++)
					{
						grad_E0y_n[i][j][k] = -conducting_y[i][j][k]*(E0_n[i][j+1][k] - E0_n[i][j][k])/ds;
						grad_E0y_p[i][j][k] = -conducting_y[i][j][k]*(E0_p[i][j+1][k] - E0_p[i][j][k])/ds;
					}
				}
			}


			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					for (int k = 0; k < nz-1; k++)
					{
						grad_E0z_n[i][j][k] = -conducting_y[i][j][k]*(E0_n[i][j][k+1] - E0_n[i][j][k])/ds;
						grad_E0z_p[i][j][k] = -conducting_y[i][j][k]*(E0_p[i][j][k+1] - E0_p[i][j][k])/ds;
					}
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					for (int k = 1; k < nz-1; k++)
					{
						double n_contrib = -G[i][j][k]*E0_n[i][j][k];
						double jn_contrib = 0.5*(grad_E0x_n[i-1][j][k]*Jx_n[i-1][j][k]+grad_E0x_n[i][j][k]*Jx_n[i][j][k]
						+grad_E0y_n[i][j-1][k]*Jy_n[i][j-1][k]+grad_E0y_n[i][j][k]*Jy_n[i][j][k]
						+grad_E0z_n[i][j][k-1]*Jz_n[i][j][k-1]+grad_E0z_n[i][j][k]*Jz_n[i][j][k])/q_n;

						double p_contrib = -G[i][j][k]*E0_p[i][j][k];
						double jp_contrib = 0.5*(grad_E0x_p[i-1][j][k]*Jx_p[i-1][j][k]+grad_E0x_p[i][j][k]*Jx_p[i][j][k]
						+grad_E0y_p[i][j-1][k]*Jy_p[i][j-1][k]+grad_E0y_p[i][j][k]*Jy_p[i][j][k]
						+grad_E0z_p[i][j][k-1]*Jz_p[i][j][k-1]+grad_E0z_p[i][j][k]*Jz_p[i][j][k])/q_p;

						double ohm_contrib = 0.5*(Ex[i-1][j][k]*Jx_free[i-1][j][k]+Ex[i][j][k]*Jx_free[i][j][k]
						+Ey[i][j-1][k]*Jy_free[i][j-1][k]+Ey[i][j][k]*Jy_free[i][j][k]
						+Ez[i][j][k-1]*Jz_free[i][j][k-1]+Ez[i][j][k]*Jz_free[i][j][k]);

						Q[i][j][k] = n_contrib + jn_contrib + p_contrib + jp_contrib + ohm_contrib;
					}
				}
			}
		}

		if (view_scalar == ScalarView.ENTROPY) {
			for (int i = 0; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					for (int k = 1; k < nz-1; k++)
					{
						grad_Fx_n[i][j][k] = conducting_x[i][j][k] == 1? -(F_n[i+1][j][k] - F_n[i][j][k])/ds : 0;
						grad_Fx_p[i][j][k] = conducting_x[i][j][k] == 1? -(F_p[i+1][j][k] - F_p[i][j][k])/ds : 0;
					}
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 0; j < ny-1; j++)
				{
					for (int k = 1; k < nz-1; k++)
					{
						grad_Fy_n[i][j][k] = conducting_y[i][j][k] == 1? -(F_n[i][j+1][k] - F_n[i][j][k])/ds : 0;
						grad_Fy_p[i][j][k] = conducting_y[i][j][k] == 1? -(F_p[i][j+1][k] - F_p[i][j][k])/ds : 0;
					}
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					for (int k = 0; k < nz-1; k++)
					{
						grad_Fz_n[i][j][k] = conducting_z[i][j][k] == 1? -(F_n[i][j][k+1] - F_n[i][j][k])/ds : 0;
						grad_Fz_p[i][j][k] = conducting_z[i][j][k] == 1? -(F_p[i][j][k+1] - F_p[i][j][k])/ds : 0;
					}
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					for (int k = 1; k < nz-1; k++)
					{
						double n_contrib = -G[i][j][k]*F_n[i][j][k];
						double jn_contrib = 0.5*(grad_Fx_n[i-1][j][k]*Jx_n[i-1][j][k]+grad_Fx_n[i][j][k]*Jx_n[i][j][k]
						+grad_Fy_n[i][j-1][k]*Jy_n[i][j-1][k]+grad_Fy_n[i][j][k]*Jy_n[i][j][k]
						+grad_Fz_n[i][j][k-1]*Jz_n[i][j][k-1]+grad_Fz_n[i][j][k]*Jz_n[i][j][k])/q_n;

						double p_contrib = -G[i][j][k]*F_p[i][j][k];
						double jp_contrib = 0.5*(grad_Fx_p[i-1][j][k]*Jx_p[i-1][j][k]+grad_Fx_p[i][j][k]*Jx_p[i][j][k]
						+grad_Fy_p[i][j-1][k]*Jy_p[i][j-1][k]+grad_Fy_p[i][j][k]*Jy_p[i][j][k]
						+grad_Fz_p[i][j][k-1]*Jz_p[i][j][k-1]+grad_Fz_p[i][j][k]*Jz_p[i][j][k])/q_p;

						double ohm_contrib = 0.5*(Ex[i-1][j][k]*Jx_free[i-1][j][k]+Ex[i][j][k]*Jx_free[i][j][k]
						+Ey[i][j-1][k]*Jy_free[i][j-1][k]+Ey[i][j][k]*Jy_free[i][j][k]
						+Ez[i][j][k-1]*Jz_free[i][j][k-1]+Ez[i][j][k]*Jz_free[i][j][k]);

						S[i][j][k] = n_contrib + jn_contrib + p_contrib + jp_contrib + ohm_contrib;
					}
				}
			}
		}

		if (ground != null)
			ground.potential = F[ground.x][ground.y][ground.z];

		for (VoltageProbe p: voltageprobes) {
			p.potential = F[p.x][p.y][p.z];
		}

		for (CurrentProbe p: currentprobes) {
			p.current = calcCurrent(p);
		}

		updateMiscFields = false;

		t8.stop();
	}

	public double calcCurrent(CurrentProbe p) {
		double J = 0;

		int n_min = 0;
		int n_max = 0;
		int m_min = 0;
		int m_max = 0;
		int l = 0;

		if (p.normal_x) {
			n_min = Math.min(p.y1, p.y2);
			n_max = Math.max(p.y1, p.y2);
			m_min = Math.min(p.z1, p.z2);
			m_max = Math.max(p.z1, p.z2);
			l = p.x1;
		} else if (p.normal_y) {
			n_min = Math.min(p.x1, p.x2);
			n_max = Math.max(p.x1, p.x2);
			m_min = Math.min(p.z1, p.z2);
			m_max = Math.max(p.z1, p.z2);
			l = p.y1;
		} else if (p.normal_z) {
			n_min = Math.min(p.x1, p.x2);
			n_max = Math.max(p.x1, p.x2);
			m_min = Math.min(p.z1, p.z2);
			m_max = Math.max(p.z1, p.z2);
			l = p.z1;
		} else {
			return 0;
		}

		for (int n = n_min; n <= n_max; n++) {
			for (int m = m_min; m <= m_max; m++) {
				if (p.normal_x) {
					J += 0.5*(Jx_free[l][n][m]+Jx_free[l+1][n][m])*(ds*ds);
				} else if (p.normal_y) {
					J += 0.5*(Jy_free[n][l][m]+Jx_free[n][l+1][m])*(ds*ds);
				} else if (p.normal_z) {
					J += 0.5*(Jz_free[n][m][l]+Jx_free[n][m][l+1])*(ds*ds);
				}
			}
		}

		return Math.abs(J);
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

		poissonLock.lock();
		try {

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
					MG_phi1[i][j][k] = bilinearinterp(MG_phi2, (i-0.5)/2.0, (j-0.5)/2.0);
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
		} finally {
			poissonLock.unlock();
		}
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
}

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

class CurrentProbe {
	int x1;
	int y1;
	int z1;

	int x2;
	int y2;
	int z2;

	boolean normal_x = false;
	boolean normal_y = false;
	boolean normal_z = false;

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
		if (!parent.renderer.threeD_mode)
			real.drawImage(parent.screen, 0, 0, parent.opts);
	}

	public RenderCanvas(Electrodynamics w) {
		parent = w;
	}
}


enum MaterialType
{

	EMF					("Voltage source (Adjustable)",			230, 216, 46, 230),
	AC_EMF				("AC voltage source (Adjustable)",		230, 150, 216, 230),
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
		|| material == MaterialType.AC_EMF
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
	double emf;
	int emf_x = 0;			// EMF strength
	int emf_y = 0;			// EMF strength
	int emf_z = 0;			// EMF strength
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
		emf = 0;
		emf_x = 0;
		emf_y = 0;
		emf_z = 0;
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