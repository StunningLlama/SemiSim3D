// Copyright (c) Brandon Li 2025-2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.codedisaster.steamworks.SteamAPI;

import electrodynamics.Renderer.ScalarView;
import electrodynamics.Renderer.VectorView;
import electrodynamics.gui.AdvancedOptions;
import electrodynamics.gui.MainWindow;
import electrodynamics.gui.MaterialManager;
import electrodynamics.gui.MaterialViewer;
import electrodynamics.gui.Preferences;
import electrodynamics.plot.BandPlot;
import electrodynamics.plot.CarrierPlot;
import electrodynamics.plot.Plot;
import electrodynamics.plot.ProbePlot;
import electrodynamics.plot.ScalarPlot;
import electrodynamics.plot.XYPlot;
import electrodynamics.probe.AreaProbe;
import electrodynamics.probe.ChargeProbe;
import electrodynamics.probe.CurrentProbe;
import electrodynamics.probe.FluxProbe;
import electrodynamics.probe.Ground;
import electrodynamics.probe.LineProbe;
import electrodynamics.probe.PointProbe;
import electrodynamics.probe.Probe;
import electrodynamics.probe.Ruler;
import electrodynamics.probe.VoltageProbe;
import electrodynamics.probe.VolumeProbe;
import electrodynamics.units.Quantity;
import electrodynamics.units.Units;
import electrodynamics.util.FastExp;
import electrodynamics.util.PeriodicTask;
import electrodynamics.util.Timer;
import electrodynamics.util.Utils;

public class Simulation extends PeriodicTask {
	
	//Update manual
	//Add heat and entropy
	//Implement arrow length 
	//Graphics settings
	//Fix sim lag affecting camera
	//Reset sim should reset camera
	//Constrain to layer
	//Cancel paste
	//Hide absorber currents??
	
	/* Parts */
	
	public Renderer.RenderCanvas canvas;
	public MainWindow opts;
	public AdvancedOptions adv_opts;
	public Renderer renderer;
	public Controls controls;
	public SaveManager savemanager;
	public Preferences prefs;
	public MaterialManager materialmanager;
	public MaterialViewer materialviewer;
	
	public ArrayList<Plot> plots = new ArrayList<>();
	public BandPlot bandplot;
	public ScalarPlot scalarplot;
	public CarrierPlot carrierplot;
	public XYPlot xyplot;
	
	public Units units = Units.SI;
	public String description = "Description of simulation";
	
	/* Multithreading */
	
	public ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	public ReentrantLock poissonLock = new ReentrantLock(true);

	private CyclicBarrier start_barrier = new CyclicBarrier(SemiSim.n_threads + 1);
	private CyclicBarrier stop_barrier = new CyclicBarrier(SemiSim.n_threads + 1);
	private CyclicBarrier mid_barrier = new CyclicBarrier(SemiSim.n_threads);

	
	/* Domain parameters */

	public int default_resolution_x;
	public int default_resolution_y;
	public int default_resolution_z;
	
	public int nx;						// Number of grid points in x-dimension
	public int ny;						// Number of grid points in y-dimension
	public int nz;						// Number of grid points in z-dimension
	public double ds;					// Spatial discretization
	public double dt;					// Timestep
	public double depth;				// Extent of circuit in z-dimension, only used to get reasonable values for current probe
	
	public int absorber_width;
	public double absorbing_coeff;
	public double boundary_stretch_factor;
	public double Hz_dissipation;
	public double dt_maximum;
	public int parity = -1;			// Negative sign resulting from flipped y-axis in graphics coordinate system
	public int magnetic_absorption = 0;

	public double time;
	public double AC_phase;
	public long stepnumber;
	public long frame;
	public int lastsimspeed = 0;
	public int iteration_multiplier = 0;
	public double targetframerate = 60;
	public double frameduration = 1000/targetframerate;
	
	public void resetTime() {
		time = 0;
		AC_phase = 0;
		stepnumber = 0;
		frame = 0;
		numerical_overflow = false;
		sign_violation_timer = 0;
		instability_timer = 0;
	}

	public double error_detection_threshold = 1e-3;
	public double big_error_detection_threshold = 1;
	public int sign_violation_timer;
	public int instability_timer;
	public boolean numerical_overflow;
	public String CFL_text = "";
	
	public boolean advsettings_tweaked = false;
	
	public double AC_freq;
	public double AC_amplitude;
	public boolean AC_source_exists;

	
	/* Physical constants */

	public double eps0;			// Vaccuum permittivity
	public double mu0;			// Vaccum permeability
	public double c;			// Speed of light
	public double kB;			// Boltzmann constant
	public double e_charge;		// Elementary charge
	public double e_mass;		// Electron mass
	public double eVtoJ = 1.6e-19;		// eV to J conversion factor
	public double q_n;			// Charge of single electron
	public double q_p;			// Charge of single hole

	
	/* Material properties */

	public double T;					// System temperature
	public double beta;					// Inverse temperature

	public double gv_semi;				// Semiconductor effective valence band DOS
	public double gc_semi;				// Semiconductor effective conduction DOS
	public double Eg_semi;				// Semiconductor band gap
	public double chi_semi;				// Semiconductor electron affinity

	public double mu_electron_semi;			// Electron mobility
	public double mu_hole_semi;				// Hole mobility, slightly lower than electron
	public double D_electron_semi;			// Diffusion constant, determined by Einstein relation
	public double D_hole_semi;				// Hole diffusion constant
	public double v_sat_n_semi;				// Velocity at which carrier velocity saturates
	public double v_sat_p_semi;				// Velocity at which carrier velocity saturates
	
	public double d_crit_n;				// Dopant density at which mobility drops by 1/2
	public double d_crit_p;
	public double eps_r_semi;			// Semiconductor dielectric constant

	public double k_rad_semi;			// Radiative recombination rate constant
	public double k_aug_n_semi;			// Auger recombination rate
	public double k_aug_p_semi;
	public double k_SRH_n_semi;			// Shockley-Read-Hall recombination rate, inverse electron lifetime
	public double k_SRH_p_semi;			// Inverse hole lifetime

	public double g_metal;				// Metal density of states
	public double g_metal_high;
	public double g_metal_low;
	public double W_metal_default;		// Metal work function, same as semiconductor
	public double W_metal_high;
	public double W_metal_low;
	public double Eg_metal;			// Same as semiconductor
	

	public double mu_electron_metal;			// Electron mobility
	public double mu_hole_metal;				// Hole mobility, slightly lower than electron
	public double D_electron_metal;			// Diffusion constant, determined by Einstein relation
	public double D_hole_metal;				// Hole diffusion constant
	public double v_sat_n_metal;				// Velocity at which carrier velocity saturates
	public double v_sat_p_metal;				// Velocity at which carrier velocity saturates
	
	public double eps_r_metal;			// Semiconductor dielectric constant
	public double k_rad_metal;			// Radiative recombination rate constant

	public double g_currentsource;			// Current source carrier concentration
	public double currentsource_mobility;	// Current source mobility
	public double currentsource_sigma;	// Current source mobility
	
	public double n_default_doping_concentration;
	public double p_default_doping_concentration;
	public double n_light_doping_concentration;
	public double p_light_doping_concentration;
	public double n_heavy_doping_concentration;
	public double p_heavy_doping_concentration;

	public double dielectric_eps_r;
	public double ferromagnet_mu_r;
	public double staticcharge_density;
	public double switch_open_mobility;
	public double max_EMF;
	public double max_current;
	public double default_AC_freq;

	public double default_flashlight_strength;
	
	public int junction_size;			// Free energy smoothing distance
	public int dopant_smoothing_distance;
	
	public double global_voltage_offset;

	public HashMap<MaterialType, String> default_names;
	public HashMap<MaterialType, String> modified_names;
	
	public boolean lock_resolution = false;

	public void setDefaultParameters() {
		if (!lock_resolution) {
			default_resolution_x = 32;
			default_resolution_y = 32;
			default_resolution_z = 32;
			ds = 1e-7;
		}
		depth = 1e-3;

		eps0 = 8.85e-12;
		mu0 = 1.257e-6;
		kB = 1.381e-23;
		e_charge = 1.6e-19;
		e_mass = 9e-31;
		T = 297.61;
		
		
		mu_electron_semi = 0.1400*1500;
		mu_hole_semi = 0.5*mu_electron_semi;
		
		v_sat_n_semi = 1.05e8;
		v_sat_p_semi = 0.525e8;

		gc_semi = 2.9366e25;
		gv_semi = 2.9366e25;
		chi_semi = 4.14*eVtoJ;
		Eg_semi = 1.12*eVtoJ;

		k_rad_semi = 1e-9;
		k_aug_n_semi = 0;
		k_aug_p_semi = 0;
		k_SRH_n_semi = 0;
		k_SRH_p_semi = 0;
		

		g_metal = 1.4683e30;
		g_metal_high = 2.5*g_metal;
		g_metal_low = 0.25*g_metal;
		W_metal_default = 4.7*eVtoJ;
		W_metal_high = W_metal_default + 0.3*eVtoJ;
		W_metal_low = W_metal_default - 0.3*eVtoJ;
		Eg_metal = 1.12*eVtoJ;
		k_rad_metal = 10*k_rad_semi;

		mu_electron_metal = mu_electron_semi;
		mu_hole_metal = mu_hole_semi;
		v_sat_n_metal = v_sat_n_semi;
		v_sat_p_metal = v_sat_p_semi;
		eps_r_metal = 1;
		

		
		n_default_doping_concentration = 5e19;
		p_default_doping_concentration = 5e19;
		n_light_doping_concentration = 1e19;
		p_light_doping_concentration = 1e19;
		n_heavy_doping_concentration = 2.5e20;
		p_heavy_doping_concentration = 2.5e20;

		dielectric_eps_r = 25.0;
		ferromagnet_mu_r = 250.0;
		staticcharge_density = 10.0;
		currentsource_mobility = 0.0002;
		max_EMF = 5e5;
		max_current = 5e7;
		default_AC_freq = 1e13;
		switch_open_mobility = 0.000001;
		
		default_flashlight_strength = 1e31;

		junction_size = 0;
		dopant_smoothing_distance = 0;
		
		d_crit_n = 1e100;
		d_crit_p = 1e100;
		eps_r_semi = 1;
		
		calculateDependentConstants();
	}
	
	public void calculateDependentConstants() {
		c = 1/Math.sqrt(eps0*mu0);
		q_n = -e_charge;
		q_p = e_charge;
		
		beta = 1/(kB*T);
		D_electron_semi = mu_electron_semi/(beta*e_charge);
		D_hole_semi = mu_hole_semi/(beta*e_charge);

		D_electron_metal = mu_electron_metal/(beta*e_charge);
		D_hole_metal = mu_hole_metal/(beta*e_charge);

		g_currentsource = g_metal;
		
		global_voltage_offset = (chi_semi+0.5*Eg_semi)/eVtoJ;

		Material cur_source = new Material();
		initializeMaterial(cur_source, MaterialType.CURRENT);
		currentsource_sigma = cur_source.calcConductivity(e_charge, kB*T);
		
		for (MaterialType type : MaterialType.values()) {
			String name = modified_names.get(type);
			if (name != null) type.name = name;
		}
	}
	
	/* Dynamical simulation variables */
	
	public double[][][] Ex;			// x component of E field
	public double[][][] Ey;			// y component of E field
	public double[][][] Ez;			// y component of E field
	public double[][][] Hx;			// z component of B field
	public double[][][] Hy;			// z component of B field
	public double[][][] Hz;			// z component of B field
	public double[][][] Bx_laplacian;
	public double[][][] By_laplacian;
	public double[][][] Bz_laplacian;

	public double[][][] rho_n;		// Electrons
	public double[][][] rho_p;		// Holes
	public double[][][] rho_back;	// Background
	public double[][][] rho_abs;		// Absorber charge
	public double[][][] rho_free;	// Total free charges

	public double[][][] Jx_n;		// Electron current
	public double[][][] Jy_n;
	public double[][][] Jz_n;
	public double[][][] Jx_p;		// Hole current
	public double[][][] Jy_p;
	public double[][][] Jz_p;
	public double[][][] Jx_abs;		// Absorber current
	public double[][][] Jy_abs;
	public double[][][] Jz_abs;
	public double[][][] Jx_free;		// Total free current
	public double[][][] Jy_free;
	public double[][][] Jz_free;

	public double[][][] Fnx;			// Total force on electrons
	public double[][][] Fny;
	public double[][][] Fnz;
	public double[][][] Fpx;			// Total force on holes
	public double[][][] Fpy;
	public double[][][] Fpz;

	/* Miscellaneous fields used for display */

	public double[][][] Bx;
	public double[][][] By;
	public double[][][] Bz;
	
	public double[][][] Dx;
	public double[][][] Dy;
	public double[][][] Dz;
	
	public double[][][] phi;				// Electric scalar potential

	public double[][][] Sx;				// Electric displacement field
	public double[][][] Sy;
	public double[][][] Sz;

	public double[][][] G;				// Generation rate
	public double[][][] R;				// Recombination rate
	public double[][][] mu_n;				// Total chemical potential of electrons with electrostatic contribution, equal to quasi-fermi level
	public double[][][] mu_p;				// Total chemical potential of holes, equal to negative of quasi-fermi level
	//public double[][][] grad_x_n;			// Gradient
	//public double[][][] grad_y_n;
	//public double[][][] grad_x_p;
	//public double[][][] grad_y_p;
	public double[][][] V_avg;				// Average voltage, as measured by voltmeter

	public double[][][] heat;
	public double[][][] entropy;

	public double[][][] vel_x_n;
	public double[][][] vel_y_n;
	public double[][][] vel_z_n;
	public double[][][] vel_x_p;
	public double[][][] vel_y_p;
	public double[][][] vel_z_p;
	
	public double[][][] drift_x_n;
	public double[][][] drift_y_n;
	public double[][][] drift_z_n;
	public double[][][] drift_x_p;
	public double[][][] drift_y_p;
	public double[][][] drift_z_p;
	
	public double[][][] diff_x_n;
	public double[][][] diff_y_n;
	public double[][][] diff_z_n;
	public double[][][] diff_x_p;
	public double[][][] diff_y_p;
	public double[][][] diff_z_p;

	public double[][][] sqrt_D_eff_n;
	public double[][][] sqrt_D_eff_p;

	public boolean updateMiscFields = false;
	public boolean store_diff_drift = false;
	public boolean need_diff_drift = false;
	public boolean need_heat = false;
	public boolean need_entropy = false;
	public double[][][] debug;

	
	/* Material parameters */

	public Material[][][] materials;

	public double[][][] mu0_n;		// Chemical potential of a single electron
	public double[][][] mu0_p;		// Chemical potential of a single hole
	public double[][][] E0_n;			// Energy of a single electron
	public double[][][] E0_p;			// Energy of a single hole
	public double[][][] n_i;			// Square root of charge carrier equilibrium constant
	public double[][][] k_rad;			// Radiative recombination rate constant
	public double[][][] k_SRH_n;			// Shockley-Read-Hall recombination rate
	public double[][][] k_SRH_p;
	public double[][][] k_aug_n;			// Auger recombination rate
	public double[][][] k_aug_p;
	public double[][][] L;			// Carrier generation due to incoming light

	public double[][][] cmfx_n;		// Chemical-motive force for electrons
	public double[][][] cmfy_n;
	public double[][][] cmfz_n;

	public double[][][] cmfx_p;		// Chemical-motive force for holes
	public double[][][] cmfy_p;
	public double[][][] cmfz_p;

	public double[][][] emfx;					// External electromotive force
	public double[][][] emfy;
	public double[][][] emfz;
	public double[][][] epsx;					// Dielectric constant
	public double[][][] epsy;
	public double[][][] epsz;
	public double[][][] mu_x;					// Relative permeability
	public double[][][] mu_y;					// Relative permeability
	public double[][][] mu_z;					// Relative permeability

	public double[][][] D_n;					// Electron diffusion constant
	public double[][][] D_p;					// Hole diffusion constant

	public double[][][] v_sat_n;				// Saturation velocity
	public double[][][] v_sat_p;				// Saturation velocity
	public int[][][] conducting;		// Does the material have partially filled bands
	public int[][][] conducting_x;
	public int[][][] conducting_y;
	public int[][][] conducting_z;

	public int[][][] semiconducting;

	public int[][][] ac_x;
	public int[][][] ac_y;
	public int[][][] ac_z;

	public double[][][] absorptivity;
	public double[][][] absorptivity_x;
	public double[][][] absorptivity_y;
	public double[][][] absorptivity_z;

	public double[][][] absorptivity_x_dual;
	public double[][][] absorptivity_y_dual;
	public double[][][] absorptivity_z_dual;

	
	/* Multigrid Poisson eq solver */

	public int log2_resolution;
	public double[][][] MG_rho0;
	public double[][][][] MG_rho;
	public double[][][][] MG_epsx;
	public double[][][][] MG_epsy;
	public double[][][][] MG_epsz;
	public double[][][] MG_eps_avg;
	public double[][][] MG_phi1;
	public double[][][] MG_phi2;

	
	/* Pathfinding */

	public int[][][] abs_depth;
	public int[][][] distance;
	public boolean[][][] visited;
	public boolean[][][] needs_smoothing;
	public double[][][] smooth_arr;
	
	
	/* Probes */
	public List<Probe> probes = new CopyOnWriteArrayList<>();
	public String datafilename = "probedata.txt";
	public File datafile;
	public PrintWriter datastream;


	/* Performance profiling */
	
	Timer t4 = new Timer("Poisson constraint solver", 1, true);
	Timer t7 = new Timer("Poisson potential solver", 10, true);
	Timer t6 = new Timer("Iterate simulation", 40, true);
	Timer t8 = new Timer("Calc misc fields", 20, true);
	Timer t9 = new Timer("Stamp pixels", 20, true);
	Timer simFPStimer = new Timer("Simulation FPS", 10, true);

	public Simulation() {
		default_names = new HashMap<MaterialType, String>();
		for (MaterialType type : MaterialType.values()) {
			default_names.put(type, type.name);
		}
		
		controls = new Controls(this);
		prefs = new Preferences(this);
		renderer = new Renderer(this);
		savemanager = new SaveManager(this);
		canvas = renderer.new RenderCanvas(this);
		opts = new MainWindow(this);
		canvas.setFocusable(true);
		adv_opts = new AdvancedOptions(this);
		materialmanager = new MaterialManager(this);
		materialviewer = new MaterialViewer(this);
		datafile = SemiSim.getUserFile(datafilename);

		bandplot = new BandPlot(); plots.add(bandplot);
		scalarplot = new ScalarPlot(); plots.add(scalarplot);
		carrierplot = new CarrierPlot(); plots.add(carrierplot);
		xyplot = new XYPlot(); plots.add(xyplot);
		plots.add(new ProbePlot("Voltage probe plot", "Voltage", "V", 1e-3, Quantity.ELECTRIC_POTENTIAL, (p) -> (p instanceof VoltageProbe && !(p instanceof Ground)), 0));
		plots.add(new ProbePlot("Current probe plot", "Current", "I", 1e-3, Quantity.ELECTRIC_CURRENT, (p) -> p instanceof CurrentProbe, 200));
		plots.add(new ProbePlot("Charge probe plot", "Charge", "Q", 1e-15, Quantity.CHARGE, (p) -> p instanceof ChargeProbe, 400));
		plots.add(new ProbePlot("Flux probe plot", "Magnetic flux", "\u03A6", 1e-15, Quantity.MAGNETIC_FLUX, (p) -> p instanceof FluxProbe, 600));
		
		for (Plot p : plots)
			p.initialize();
		
		renderer.create3dCanvas();

		SemiSim.detect64Bit();
		
		reset(true, Preset.DEFAULT);

		opts.initialize();
		adv_opts.initialize();
		prefs.initialize();
		materialmanager.initialize();
		materialviewer.initialize();
		Steam.e = this;

		try {
			datastream = new PrintWriter(new FileOutputStream(datafile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		rwLock.readLock().lock();
        try {
    		try {
    			iteration_multiplier = opts.gui_simspeed_2.getValue();

    			if (controls.clear) {
    				new Thread(() -> {
    					reset(false, null);
    				}).start();
    				controls.clear = false;
    			}
    			
    			if (controls.reset) {
    				SwingUtilities.invokeLater(() -> {
        				int result = JOptionPane.showConfirmDialog(opts, "Do you wish to reset the entire simulation?", "Message", JOptionPane.YES_NO_OPTION);
        				if (result == JOptionPane.OK_OPTION)
        				{
        					reset(true, null);
            				opts.setDefaults(this);
            				description = "Description of simulation";
            				SaveManager.currentfile = null;
        				}
    				});
    				controls.reset = false;
    			}

    			if (controls.updateimagesize) {
    				SwingUtilities.invokeLater(() -> {
    					renderer.setCanvasSize();
    				});
    				controls.updateimagesize = false;
    			}

    			lastsimspeed = opts.gui_simspeed.getValue();
    			dt = dt_maximum*(lastsimspeed/20.0);

    			if (controls.undo) {
    				controls.undoredo.undo(this);
    				controls.undo = false;
    			}

    			if (controls.redo) {
    				controls.undoredo.redo(this);
    				controls.redo = false;
    			}

    			controls.handleMouseInput();

    			if (!opts.gui_paused.isSelected() || controls.advanceframe) {
    				for (int i = 0; i < iteration_multiplier ; i++) {
    					store_diff_drift = (i == iteration_multiplier-1 && need_diff_drift);
    					start_barrier.await();
    					stop_barrier.await();
    				}
    				calcMiscFields(false);
    				frame++;
    			} else if (updateMiscFields) {
    				calcMiscFields(false);
    			}
    			performProbeMeasurements();

    			setDebugInfo();
    			
    			if (numerical_overflow)
    				opts.gui_paused.setSelected(true);

    			if (controls.save) {
    				savemanager.writeFile(false);
    				controls.save = false;
    			}
    			
    			if (controls.saveas) {
    				savemanager.writeFile(true);
    				controls.saveas = false;
    			}

    			if (controls.load) {
    				savemanager.readFile();
    				controls.load = false;
    			}

    			if (controls.exit) {
    				SwingUtilities.invokeLater(() -> {
    					controls.windowClosing(null);
    				});
    				controls.exit = false;
    			}

    			simFPStimer.stop();
    			if (!opts.gui_paused.isSelected())
    				simFPStimer.start();

    		} catch (Exception e) {
    			SemiSim.displayErrorMessage(e);
    		}
        } finally {
            rwLock.readLock().unlock();
        }

		if (SteamAPI.isSteamRunning()) {
			SteamAPI.runCallbacks();
		}
		
        SemiSim.instance.threadPool.schedule(this, nextDelay(frameduration), TimeUnit.MILLISECONDS);
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
	        	SemiSim.displayErrorMessage(e);
	        } finally {
	            rwLock.readLock().unlock();
	        }
	        SemiSim.instance.threadPool.schedule(this, nextDelay(frameduration), TimeUnit.MILLISECONDS);
		}
	};
	
	public void calculateMaxTimestep() {
		GeneralMaterialType[] types = materialmanager.makelist();
		
		double D_electron_max = 0;
		double D_hole_max = 0;
		double sigma_epsr_max = 0;
		
		String D_electron_name = "";
		String D_hole_name = "";
		String sigma_epsr_name = "";
		
		for (GeneralMaterialType type : types) {
			Material mat = new Material();
			initializeMaterial(mat, type);
			
			if (mat.D_n > D_electron_max) {
				D_electron_max = mat.D_n;
				D_electron_name = mat.getDisplayName();
			}
			
			if (mat.D_p > D_hole_max) {
				D_hole_max = mat.D_p;
				D_hole_name = mat.getDisplayName();
			}
			

			double rho_n = mat.calcEquilibriumElectronCharge(e_charge, kB*T);
			double rho_p = mat.calcEquilibriumHoleCharge(e_charge, kB*T);
			double sigma_epsr = e_charge*(-mat.D_n*beta*rho_n + mat.D_p*beta*rho_p)/mat.eps_r;
			if (sigma_epsr > sigma_epsr_max) {
				sigma_epsr_max = sigma_epsr;
				sigma_epsr_name = mat.getDisplayName();
			}
		}

		dt_maximum = 0.9*Math.min(Math.min(ds/(Math.sqrt(3)*c), 4*eps0/sigma_epsr_max), Math.min(ds*ds/(4*D_electron_max), ds*ds/(4*D_hole_max)));
		
		Hz_dissipation = 0.01*ds*ds/dt_maximum;
		absorber_width = (int)Math.ceil(0.045*Math.min(nx, ny));
		absorbing_coeff = 50*c/(ds*Math.min(nx, ny));
		boundary_stretch_factor = 10;
		renderer.tau = 5000*dt_maximum;
		renderer.tau_events = renderer.tau*0.2;
		renderer.cc_default_dot_density = 1/(25*e_charge*n_default_doping_concentration*ds*ds);
		
		CFL_text = "";
		CFL_text += "Wave equation stability ratio = " + units.toString((Math.sqrt(3)*c)/(ds/dt_maximum), Quantity.DIMENSIONLESS) + "\n";
		CFL_text += "Electron diffusion stability ratio (" + D_electron_name + ") = " + units.toString(dt_maximum/(ds*ds/(4*D_electron_max)), Quantity.DIMENSIONLESS) + "\n";
		CFL_text += "Hole diffusion stability ratio (" + D_hole_name + ") = " + units.toString(dt_maximum/(ds*ds/(4*D_hole_max)), Quantity.DIMENSIONLESS) + "\n";
		CFL_text += "Conduction stability ratio (" + sigma_epsr_name + ") = " + units.toString(dt_maximum*sigma_epsr_max/(4*eps0), Quantity.DIMENSIONLESS) + "\n";
		System.out.print(CFL_text);
	}

	public boolean reset(boolean resetall, Preset preset) {
		
		if (resetall || preset != null) {
			modified_names = default_names;
			if (preset == null)
				Preset.DEFAULT.applyPreset(this);
			else
				preset.applyPreset(this);
			
			materialmanager.resetMaterialList();
		}

		boolean size_changed = (default_resolution_x != nx || default_resolution_y != ny || default_resolution_z != nz);
		
		// Simulation and graphics threads should not be active when variables are initialized
		rwLock.writeLock().lock();
		try {
			if (size_changed) {
				nx = default_resolution_x;
				ny = default_resolution_y;
				nz = default_resolution_z;
				
				if(nx < 4) {
					nx = 4;
				}
				if(ny < 4) {
					ny = 4;
				}
				if(nz < 4) {
					nz = 4;
				}

				int log2_nx = (int) Math.round(Math.log(nx)/Math.log(2));
				if(1 << log2_nx != nx) {
					nx = 1 << log2_nx;
				}

				int log2_ny = (int) Math.round(Math.log(ny)/Math.log(2));
				if(1 << log2_ny != ny) {
					ny = 1 << log2_ny;
				}

				int log2_nz = (int) Math.round(Math.log(nz)/Math.log(2));
				if(1 << log2_nz != nz) {
					nz = 1 << log2_nz;
				}
				
				Utils.nx = nx;
				Utils.ny = ny;
				Utils.nz = nz;


				System.out.println("Initializing simulation grid with resolution " + default_resolution_x + "*" + default_resolution_y + "*" + default_resolution_z);

				int min_res = Math.min(Math.min(nx, ny), nz);
				log2_resolution = (int) Math.round(Math.log(min_res)/Math.log(2));
			}
			
			calculateMaxTimestep();

			if (size_changed) {
				Ex = new double[nx][ny][nz];			Ey = new double[nx][ny][nz];			Ez = new double[nx][ny][nz];
				Bx = new double[nx][ny+1][nz+1];		By = new double[nx+1][ny][nz+1];		Bz = new double[nx+1][ny+1][nz];
				Hx = new double[nx][ny][nz];			Hy = new double[nx][ny][nz];			Hz = new double[nx][ny][nz];
				Bx_laplacian = new double[nx][ny][nz];	By_laplacian = new double[nx][ny][nz];	Bz_laplacian = new double[nx][ny][nz];
				rho_abs = new double[nx][ny][nz];
				rho_n = new double[nx][ny][nz];			rho_p = new double[nx][ny][nz];
				rho_back = new double[nx][ny][nz];
				rho_free = new double[nx][ny][nz];

				Jx_abs = new double[nx][ny][nz];		Jy_abs = new double[nx][ny][nz];		Jz_abs = new double[nx][ny][nz];
				Jx_n = new double[nx][ny][nz];			Jy_n = new double[nx][ny][nz];			Jz_n = new double[nx][ny][nz];
				Jx_p = new double[nx][ny][nz];			Jy_p = new double[nx][ny][nz];			Jz_p = new double[nx][ny][nz];
				Jx_free = new double[nx][ny][nz];		Jy_free = new double[nx][ny][nz];		Jz_free = new double[nx][ny][nz];

				Fnx = new double[nx][ny][nz];			Fny = new double[nx][ny][nz];			Fnz = new double[nx][ny][nz];
				Fpx = new double[nx][ny][nz];			Fpy = new double[nx][ny][nz];			Fpz = new double[nx][ny][nz];

				materials = new Material[nx][ny][nz];
				n_i = new double[nx][ny][nz];
				mu0_n = new double[nx][ny][nz];			mu0_p = new double[nx][ny][nz];
				mu_n = new double[nx][ny][nz];			mu_p = new double[nx][ny][nz];
				E0_n = new double[nx][ny][nz];			E0_p = new double[nx][ny][nz];
				k_rad = new double[nx][ny][nz];
				k_SRH_n = new double[nx][ny][nz];		k_SRH_p = new double[nx][ny][nz];
				k_aug_n = new double[nx][ny][nz];		k_aug_p = new double[nx][ny][nz];
				L = new double[nx][ny][nz];
				cmfx_n = new double[nx][ny][nz];		cmfy_n = new double[nx][ny][nz];		cmfz_n = new double[nx][ny][nz];
				cmfx_p = new double[nx][ny][nz];		cmfy_p = new double[nx][ny][nz];		cmfz_p = new double[nx][ny][nz];
				D_n = new double[nx][ny][nz];			D_p = new double[nx][ny][nz];
				v_sat_n = new double[nx][ny][nz];		v_sat_p = new double[nx][ny][nz];
				conducting = new int[nx][ny][nz];
				semiconducting = new int[nx][ny][nz];
				conducting_x = new int[nx][ny][nz];		conducting_y = new int[nx][ny][nz];		conducting_z = new int[nx][ny][nz];
				ac_x = new int[nx][ny][nz];				ac_y = new int[nx][ny][nz];				ac_z = new int[nx][ny][nz];
				absorptivity = new double[nx][ny][nz];
				absorptivity_x = new double[nx][ny][nz]; absorptivity_y = new double[nx][ny][nz]; absorptivity_z = new double[nx][ny][nz];
				absorptivity_x_dual = new double[nx][ny][nz]; absorptivity_y_dual = new double[nx][ny][nz]; absorptivity_z_dual = new double[nx][ny][nz];
				emfx = new double[nx][ny][nz];			emfy = new double[nx][ny][nz];			emfz = new double[nx][ny][nz];
				epsx = new double[nx][ny][nz];			epsy = new double[nx][ny][nz];			epsz = new double[nx][ny][nz];
				mu_x = new double[nx][ny][nz];			mu_y = new double[nx][ny][nz];			mu_z = new double[nx][ny][nz];

				vel_x_n = new double[nx][ny][nz];		vel_y_n = new double[nx][ny][nz];		vel_z_n = new double[nx][ny][nz];
				vel_x_p = new double[nx][ny][nz];		vel_y_p = new double[nx][ny][nz];		vel_z_p = new double[nx][ny][nz];
				
				drift_x_n = new double[nx][ny][nz];		drift_y_n = new double[nx][ny][nz];		drift_z_n = new double[nx][ny][nz];
				drift_x_p = new double[nx][ny][nz];		drift_y_p = new double[nx][ny][nz];		drift_z_p = new double[nx][ny][nz];
				
				diff_x_n = new double[nx][ny][nz];		diff_y_n = new double[nx][ny][nz];		diff_z_n = new double[nx][ny][nz];
				diff_x_p = new double[nx][ny][nz];		diff_y_p = new double[nx][ny][nz];		diff_z_p = new double[nx][ny][nz];

				sqrt_D_eff_n = new double[nx][ny][nz];	sqrt_D_eff_p = new double[nx][ny][nz];
				
				heat = new double[nx][ny][nz];
				entropy = new double[nx][ny][nz];

				Dx = new double[nx][ny][nz];			Dy = new double[nx][ny][nz];			Dz = new double[nx][ny][nz];
				phi = new double[nx][ny][nz];

				Sx = new double[nx][ny][nz];			Sy = new double[nx][ny][nz];			Sz = new double[nx][ny][nz];

				G = new double[nx][ny][nz];
				R = new double[nx][ny][nz];
				mu_n = new double[nx][ny][nz];			mu_p = new double[nx][ny][nz];
				//grad_x_n = new double[nx][ny][nz];	grad_y_n = new double[nx][ny][nz];
				//grad_x_p = new double[nx][ny][nz];	grad_y_p = new double[nx][ny][nz];
				V_avg = new double[nx][ny][nz];
				debug = new double[nx][ny][nz];

				MG_rho0 = new double[nx][ny][nz];
				MG_rho = new double[log2_resolution+1][nx][ny][nz];
				MG_epsx = new double[log2_resolution+1][nx][ny][nz];
				MG_epsy = new double[log2_resolution+1][nx][ny][nz];
				MG_epsz = new double[log2_resolution+1][nx][ny][nz];
				MG_eps_avg = new double[nx][ny][nz];
				MG_phi1 = new double[nx][ny][nz];
				MG_phi2 = new double[nx][ny][nz];

				distance = new int[nx][ny][nz];
				visited = new boolean[nx][ny][nz];
				needs_smoothing = new boolean[nx][ny][nz];
				abs_depth = new int[nx][ny][nz];
				smooth_arr = new double[nx][ny][nz];

				opts.setVisible(true);

				controls.setResolution();
				renderer.setResolution();
			}

			resetTime();
			
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						if (resetall || size_changed) {
							if (materials[i][j][k] == null)
								materials[i][j][k] = new Material();
							else
								materials[i][j][k].initialize();

							mu0_n[i][j][k] = 0;		mu0_p[i][j][k] = 0;
							mu_n[i][j][k] = 0;		mu_p[i][j][k] = 0;
							E0_n[i][j][k] = 0;		E0_p[i][j][k] = 0;
							n_i[i][j][k] = 0;
							k_rad[i][j][k] = 0;
							k_SRH_n[i][j][k] = 0;	k_SRH_p[i][j][k] = 0;
							k_aug_n[i][j][k] = 0;	k_aug_p[i][j][k] = 0;
							L[i][j][k] = 0;

							cmfx_n[i][j][k] = 0;	cmfy_n[i][j][k] = 0;	cmfz_n[i][j][k] = 0;
							cmfx_p[i][j][k] = 0;	cmfy_p[i][j][k] = 0;	cmfz_p[i][j][k] = 0;

							emfx[i][j][k] = 0.0;	emfy[i][j][k] = 0.0;	emfz[i][j][k] = 0.0;

							epsx[i][j][k] = eps0;	epsy[i][j][k] = eps0;	epsz[i][j][k] = eps0;
							mu_x[i][j][k] = mu0;	mu_y[i][j][k] = mu0;	mu_z[i][j][k] = mu0;

							D_n[i][j][k] = 0.0;		D_p[i][j][k] = 0.0;
							v_sat_n[i][j][k] = 0.0;	v_sat_p[i][j][k] = 0.0;

							conducting[i][j][k] = 0;
							conducting_x[i][j][k] = 0;	conducting_y[i][j][k] = 0;	conducting_z[i][j][k] = 0;

							semiconducting[i][j][k] = 0;

							ac_x[i][j][k] = 0;		ac_y[i][j][k] = 0;		ac_z[i][j][k] = 0;

							absorptivity[i][j][k] = 0;
							absorptivity_x[i][j][k] = 0; absorptivity_y[i][j][k] = 0; absorptivity_z[i][j][k] = 0;
							absorptivity_x_dual[i][j][k] = 0; absorptivity_y_dual[i][j][k] = 0; absorptivity_z_dual[i][j][k] = 0;

							controls.selected[i][j][k] = false;
							controls.selected_EMF[i][j][k] = false;
						}

						Ex [i][j][k] = 0.0;			Ey [i][j][k] = 0.0;			Ez [i][j][k] = 0.0;
						Hx[i][j][k] = 0.0;			Hy[i][j][k] = 0.0;			Hz[i][j][k] = 0.0;
						Bx [i][j][k] = 0.0;			By [i][j][k] = 0.0;			Bz [i][j][k] = 0.0;
						Bx_laplacian [i][j][k] = 0.0;	By_laplacian [i][j][k] = 0.0;	Bz_laplacian [i][j][k] = 0.0;

						rho_abs[i][j][k] = 0.0;
						rho_n[i][j][k] = 0.0;		rho_p[i][j][k] = 0.0;
						rho_back[i][j][k] = 0.0;
						rho_free[i][j][k] = 0.0;

						Jx_abs[i][j][k] = 0.0;		Jy_abs[i][j][k] = 0.0;		Jz_abs[i][j][k] = 0.0;
						Jx_n[i][j][k] = 0.0;		Jy_n[i][j][k] = 0.0;		Jz_n[i][j][k] = 0.0;
						Jx_p[i][j][k] = 0.0;		Jy_p[i][j][k] = 0.0;		Jz_p[i][j][k] = 0.0;
						Jx_free[i][j][k] = 0.0;		Jy_free[i][j][k] = 0.0;		Jz_free[i][j][k] = 0.0;

						Fnx[i][j][k] = 0.0;			Fny[i][j][k] = 0.0;			Fnz[i][j][k] = 0.0;
						Fpx[i][j][k] = 0.0;			Fpy[i][j][k] = 0.0;			Fpz[i][j][k] = 0.0;

						MG_rho0 [i][j][k] = 0.0;
						MG_eps_avg [i][j][k] = 0.0;
						MG_phi1 [i][j][k] = 0.0;
						MG_phi2 [i][j][k] = 0.0;
						for (int n = 0; n < log2_resolution+1; n++) {
							MG_rho[n][i][j][k] = 0;
							MG_epsx[n][i][j][k] = 0;
							MG_epsy[n][i][j][k] = 0;
							MG_epsz[n][i][j][k] = 0;
						}

						distance[i][j][k] = Integer.MAX_VALUE;
						visited[i][j][k] = false;
						needs_smoothing[i][j][k] = false;

						drift_x_n[i][j][k] = 0;		drift_y_n[i][j][k] = 0;		drift_z_n[i][j][k] = 0;
						drift_x_p[i][j][k] = 0;		drift_y_p[i][j][k] = 0;		drift_z_p[i][j][k] = 0;

						diff_x_n[i][j][k] = 0;		diff_y_n[i][j][k] = 0;		diff_z_n[i][j][k] = 0;
						diff_x_p[i][j][k] = 0;		diff_y_p[i][j][k] = 0;		diff_z_p[i][j][k] = 0;

						heat[i][j][k] = 0.0;
						entropy[i][j][k] = 0.0;

						Dx[i][j][k] = 0.0; 			Dy[i][j][k] = 0.0; 			Dz[i][j][k] = 0.0;
						
						phi[i][j][k] = 0.0;

						Sx[i][j][k] = 0.0;	 		Sy[i][j][k] = 0.0;			Sz[i][j][k] = 0.0;

						G[i][j][k] = 0.0;
						R[i][j][k] = 0.0;
						mu_n[i][j][k] = 0.0;
						mu_p[i][j][k] = 0.0;
						//grad_x_n[i][j][k] = 0.0;	grad_y_n[i][j][k] = 0.0;
						//grad_x_p[i][j][k] = 0.0;	grad_y_p[i][j][k] = 0.0;
						//grad_x_n[i][j][k] = 0.0;	grad_y_n[i][j][k] = 0.0;
						//grad_x_p[i][j][k] = 0.0;	grad_y_p[i][j][k] = 0.0;
						V_avg[i][j][k] = 0.0;

						abs_depth[i][j][k] = 0;

						debug[i][j][k] = 0.0;
					}
				}
			}

			controls.selection.clear();
			controls.clipboard.clear();
			
			//TODO
			if (resetall || size_changed) {
				controls.EMF_selected = false;
				controls.changesmade = false;
				opts.gui_bc.setSelectedItem(BoundaryCondition.DISSIPATIVE);
				controls.prev_boundary = BoundaryCondition.DISSIPATIVE;
				
				probes.clear();
				probe_index = 0;

				for (Plot p: plots) {
					p.frame.setVisible(false);
				}

				controls.undoredo.resetUndoHistory(this);
				controls.resetZoom();
			}
			
			for (Probe p : probes) {
				p.reset();
				p.data.resetData();
			}
			
			renderer.resetChargeDots();

			initializeAllMaterials();
			updateAllMaterials(true);
			controls.undoredo.captureState(this);
			multigridSolve(true, false);
		} finally {
			rwLock.writeLock().unlock();
		}

		return true;
	}

	public void constructBoundary() {
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
						if ((BoundaryCondition)opts.gui_bc.getSelectedItem() == BoundaryCondition.DISSIPATIVE) {
							if (materials[i][j][k].type == MaterialType.VACUUM) {
								initializeMaterial(materials[i][j][k], MaterialType.ABSORBER);
								materials[i][j][k].auto_placed = true;
							}
						} else if (materials[i][j][k].type == MaterialType.ABSORBER) {
							if (materials[i][j][k].auto_placed) {
								eraseMaterial(i, j, k);
							}
						}
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
					abs_depth[i][j][k] = 0;
				}
			}
		}

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (materials[i][j][k].type != MaterialType.ABSORBER) {
						set(i+1, j, k, 1);
						set(i-1, j, k, 1);
						set(i, j+1, k, 1);
						set(i, j-1, k, 1);
						set(i, j, k+1, 1);
						set(i, j, k-1, 1);
					}
				}
			}
		}

		int dmax = Math.max(Math.max(nx, ny), nz);
		for (int d = 1; d < dmax; d++) {
			for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					for (int k = 0; k < nz; k++)
					{
						if (abs_depth[i][j][k] == d) {
							set(i+1, j, k, d+1);
							set(i-1, j, k, d+1);
							set(i, j+1, k, d+1);
							set(i, j-1, k, d+1);
							set(i, j, k+1, d+1);
							set(i, j, k-1, d+1);
						}
					}
				}
			}
		}

		double coeff = Math.log(boundary_stretch_factor);
		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (materials[i][j][k].type == MaterialType.ABSORBER) {
						double depth = (double)abs_depth[i][j][k]/absorber_width;
						double stretchfactor = Math.exp(coeff*depth);
						materials[i][j][k].eps_r = stretchfactor;
						materials[i][j][k].mu_r = stretchfactor;
						materials[i][j][k].absorptivity = 1;
					}
				}
			}
		}
	}
	
	public void set(int i, int j, int k, int d) {
		if (i < 0 || j < 0 || k < 0 || i >= nx || j >= ny || k >= nz) return;
		if (materials[i][j][k].type == MaterialType.ABSORBER && abs_depth[i][j][k] == 0)
			abs_depth[i][j][k] = d;
	}

	class SimulationThread extends Thread {

		int i_min;
		int i_max;
		int j_min;
		int j_max;
		int k_min;
		int k_max;
		int n_thread;

		public SimulationThread(int n, int n_threads) {
			n_thread = n;
			System.out.println("Simulation thread " + n_thread + ": " + i_min + " < i <= " + i_max + " initialized.");
		}

		@Override
		public void run() {
			try {
				while (true) {
					start_barrier.await();

					i_min = (n_thread*nx)/SemiSim.n_threads;
					i_max = (n_thread+1)*nx/SemiSim.n_threads-1;
					j_min = (n_thread*ny)/SemiSim.n_threads;
					j_max = (n_thread+1)*ny/SemiSim.n_threads-1;
					k_min = (n_thread*nz)/SemiSim.n_threads;
					k_max = (n_thread+1)*nz/SemiSim.n_threads-1;

					if (n_thread == 0) {
						t6.start();

						stepnumber++;
					}
					
					calcF();
					
					mid_barrier.await();
					
					updateE();
					if (store_diff_drift) {
						storeDD();
						storeDeff();
					}
					updateBlap();
					
					mid_barrier.await();
					
					updateH();
					updateRho();

					mid_barrier.await();
					
					if (n_thread == 0) {
						t6.stop();
						
						/* Enforce Gauss law constraint */
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
		
		private void calcF() {
			for (int i = 0; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
					for (int j = 1; j < ny-1; j++)
					{
						for (int k = 1; k < nz-1; k++)
						{
							if (conducting_x[i][j][k] == 1) {
								double emf_phase = ac_x[i][j][k]*AC_amplitude+(1-ac_x[i][j][k]);
								Fnx[i][j][k] = emf_phase*emfx[i][j][k] + cmfx_n[i][j][k]/q_n + Ex[i][j][k];
								Fpx[i][j][k] = emf_phase*emfx[i][j][k] + cmfx_p[i][j][k]/q_p + Ex[i][j][k];
							} else {
								Fnx[i][j][k] = Ex[i][j][k];
								Fpx[i][j][k] = Ex[i][j][k];
							}
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
							if (conducting_y[i][j][k] == 1) {
								double emf_phase = ac_y[i][j][k]*AC_amplitude+(1-ac_y[i][j][k]);
								Fny[i][j][k] = emf_phase*emfy[i][j][k] + cmfy_n[i][j][k]/q_n + Ey[i][j][k];
								Fpy[i][j][k] = emf_phase*emfy[i][j][k] + cmfy_p[i][j][k]/q_p + Ey[i][j][k];
							} else {
								Fny[i][j][k] = Ey[i][j][k];
								Fpy[i][j][k] = Ey[i][j][k];
							}
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
							if (conducting_z[i][j][k] == 1) {
								double emf_phase = ac_z[i][j][k]*AC_amplitude+(1-ac_z[i][j][k]);
								Fnz[i][j][k] = emf_phase*emfz[i][j][k] + cmfz_n[i][j][k]/q_n + Ez[i][j][k];
								Fpz[i][j][k] = emf_phase*emfz[i][j][k] + cmfz_p[i][j][k]/q_p + Ez[i][j][k];
							} else {
								Fnz[i][j][k] = Ey[i][j][k];
								Fpz[i][j][k] = Ey[i][j][k];
							}
						}
					}
				}
			}

			setB_Boundary();
		}
		
		public void updateE() {
			for (int i = 0; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
					for (int j = 1; j < ny-1; j++)
					{
						for (int k = 1; k < nz-1; k++)
						{
							double ex_prev = Ex[i][j][k];

							double sigma_n = 0;
							double sigma_p = 0;

							if (conducting_x[i][j][k] == 1) {

								double d_n = 0.5*(D_n[i+1][j][k] + D_n[i][j][k]);
								double d_p = 0.5*(D_p[i+1][j][k] + D_p[i][j][k]);

								double rho_n_avg = 0.5*(rho_n[i+1][j][k] + rho_n[i][j][k]);
								double rho_p_avg = 0.5*(rho_p[i+1][j][k] + rho_p[i][j][k]);

								double Esat_n = 0.5*(v_sat_n[i+1][j][k] + v_sat_n[i][j][k])/(d_n*e_charge*beta);
								double Esat_p = 0.5*(v_sat_p[i+1][j][k] + v_sat_p[i][j][k])/(d_p*e_charge*beta);

								double Fny_avg = 0.25*(Fny[i][j][k] + Fny[i][j-1][k] + Fny[i+1][j][k] + Fny[i+1][j-1][k]);
								double Fpy_avg = 0.25*(Fpy[i][j][k] + Fpy[i][j-1][k] + Fpy[i+1][j][k] + Fpy[i+1][j-1][k]);

								double Fnz_avg = 0.25*(Fnz[i][j][k] + Fnz[i][j][k-1] + Fnz[i+1][j][k] + Fnz[i+1][j][k-1]);
								double Fpz_avg = 0.25*(Fpz[i][j][k] + Fpz[i][j][k-1] + Fpz[i+1][j][k] + Fpz[i+1][j][k-1]);

								double Fn_ratio = (Fny_avg*Fny_avg+Fnz_avg*Fnz_avg)/(Esat_n*Esat_n);
								double Fp_ratio = (Fpy_avg*Fpy_avg+Fpz_avg*Fpz_avg)/(Esat_n*Esat_n);

								double w_n = Fnx[i][j][k]*q_n*beta*ds/2;
								double w_p = Fpx[i][j][k]*q_p*beta*ds/2;

								double mr_n = 1/Math.sqrt((Fnx[i][j][k]*Fnx[i][j][k])/(Esat_n*Esat_n) + Fn_ratio + 1);
								double mr_p = 1/Math.sqrt((Fpx[i][j][k]*Fpx[i][j][k])/(Esat_p*Esat_p) + Fp_ratio + 1);

								// whether to use taylor series approximation around x=0
								boolean approx_n = Math.abs(w_n) < 0.2;
								boolean approx_p = Math.abs(w_p) < 0.2;

								double exp_2wn = approx_n? 1 : FastExp.exp(2*w_n);
								double exp_2wp = approx_p? 1 : FastExp.exp(2*w_p);

								sigma_n = -d_n*rho_n_avg*e_charge*beta*(1 + Fn_ratio)*(mr_n*mr_n*mr_n);
								sigma_p = d_p*rho_p_avg*e_charge*beta*(1 + Fp_ratio)*(mr_p*mr_p*mr_p);

								Jx_n[i][j][k] = mr_n*d_n/ds*(-(rho_n[i+1][j][k] - rho_n[i][j][k])*Utils.xtanhxm1(w_n, exp_2wn, approx_n)
								+ 2*rho_n_avg*w_n) - sigma_n*ex_prev;
								Jx_p[i][j][k] = mr_p*d_p/ds*(-(rho_p[i+1][j][k] - rho_p[i][j][k])*Utils.xtanhxm1(w_p, exp_2wp, approx_p)
								+ 2*rho_p_avg*w_p) - sigma_p*ex_prev;
							} else {
								Jx_n[i][j][k] = 0;
								Jx_p[i][j][k] = 0;
							}

							//if (n_thread == 0) t6.start();
							Jx_abs[i][j][k] = 0;

							double sigma = sigma_n + sigma_p + absorptivity_x[i][j][k]*epsx[i][j][k]*absorbing_coeff;

							double jx = Jx_abs[i][j][k] + Jx_n[i][j][k] + Jx_p[i][j][k];

							Ex[i][j][k] = (Ex[i][j][k]*(1-0.5*dt*sigma/epsx[i][j][k]) + (((Hz[i][j][k] - Hz[i][j-1][k]) - (Hy[i][j][k] - Hy[i][j][k-1]))/ds - jx)*dt/epsx[i][j][k])
								/(1+0.5*dt*sigma/epsx[i][j][k]);

							Jx_abs[i][j][k] += 0.5*(absorptivity_x[i][j][k]*epsx[i][j][k]*absorbing_coeff)*(ex_prev + Ex[i][j][k]);
							Jx_n[i][j][k] += 0.5*sigma_n*(ex_prev + Ex[i][j][k]);
							Jx_p[i][j][k] += 0.5*sigma_p*(ex_prev + Ex[i][j][k]);
							//if (n_thread == 0) t6.stop();
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

							double sigma_n = 0;
							double sigma_p = 0;

							if (conducting_y[i][j][k] == 1) {

								double d_n = 0.5*(D_n[i][j+1][k] + D_n[i][j][k]);
								double d_p = 0.5*(D_p[i][j+1][k] + D_p[i][j][k]);

								double rho_n_avg = 0.5*(rho_n[i][j+1][k] + rho_n[i][j][k]);
								double rho_p_avg = 0.5*(rho_p[i][j+1][k] + rho_p[i][j][k]);

								double Esat_n = 0.5*(v_sat_n[i][j+1][k] + v_sat_n[i][j][k])/(d_n*e_charge*beta);
								double Esat_p = 0.5*(v_sat_p[i][j+1][k] + v_sat_p[i][j][k])/(d_p*e_charge*beta);

								double Fnx_avg = 0.25*(Fnx[i][j][k] + Fnx[i-1][j][k] + Fnx[i][j+1][k] + Fnx[i-1][j+1][k]);
								double Fpx_avg = 0.25*(Fpx[i][j][k] + Fpx[i-1][j][k] + Fpx[i][j+1][k] + Fpx[i-1][j+1][k]);

								double Fnz_avg = 0.25*(Fnx[i][j][k] + Fnx[i][j][k-1] + Fnx[i][j+1][k] + Fnx[i][j+1][k-1]);
								double Fpz_avg = 0.25*(Fpx[i][j][k] + Fpx[i][j][k-1] + Fpx[i][j+1][k] + Fpx[i][j+1][k-1]);
								
								double Fn_ratio = (Fnx_avg*Fnx_avg+Fnz_avg*Fnz_avg)/(Esat_n*Esat_n);
								double Fp_ratio = (Fpx_avg*Fpx_avg+Fpz_avg*Fpz_avg)/(Esat_n*Esat_n);

								double w_n = Fny[i][j][k]*q_n*beta*ds/2;
								double w_p = Fpy[i][j][k]*q_p*beta*ds/2;

								double mr_n = 1/Math.sqrt((Fny[i][j][k]*Fny[i][j][k])/(Esat_n*Esat_n)+Fn_ratio+1);
								double mr_p = 1/Math.sqrt((Fpy[i][j][k]*Fpy[i][j][k])/(Esat_p*Esat_p)+Fp_ratio+1);

								boolean approx_n = Math.abs(w_n) < 0.2;
								boolean approx_p = Math.abs(w_p) < 0.2;

								double exp_2wn = approx_n? 1 : FastExp.exp(2*w_n);
								double exp_2wp = approx_p? 1 : FastExp.exp(2*w_p);

								sigma_n = -d_n*rho_n_avg*e_charge*beta*(1 + Fn_ratio)*(mr_n*mr_n*mr_n);
								sigma_p = d_p*rho_p_avg*e_charge*beta*(1 + Fp_ratio)*(mr_p*mr_p*mr_p);

								Jy_n[i][j][k] = mr_n*d_n/ds*(-(rho_n[i][j+1][k] - rho_n[i][j][k])*Utils.xtanhxm1(w_n, exp_2wn, approx_n)
								+ 2*rho_n_avg*w_n) - sigma_n*ey_prev;
								Jy_p[i][j][k] = mr_p*d_p/ds*(-(rho_p[i][j+1][k] - rho_p[i][j][k])*Utils.xtanhxm1(w_p, exp_2wp, approx_p)
								+ 2*rho_p_avg*w_p) - sigma_p*ey_prev;

							} else {
								Jy_n[i][j][k] = 0;
								Jy_p[i][j][k] = 0;
							}

							Jy_abs[i][j][k] = 0;

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

							double sigma_n = 0;
							double sigma_p = 0;

							if (conducting_z[i][j][k] == 1) {

								double d_n = 0.5*(D_n[i][j][k+1] + D_n[i][j][k]);
								double d_p = 0.5*(D_p[i][j][k+1] + D_p[i][j][k]);

								double rho_n_avg = 0.5*(rho_n[i][j][k+1] + rho_n[i][j][k]);
								double rho_p_avg = 0.5*(rho_p[i][j][k+1] + rho_p[i][j][k]);

								double Esat_n = 0.5*(v_sat_n[i][j][k+1] + v_sat_n[i][j][k])/(d_n*e_charge*beta);
								double Esat_p = 0.5*(v_sat_p[i][j][k+1] + v_sat_p[i][j][k])/(d_p*e_charge*beta);

								double Fnx_avg = 0.25*(Fnx[i][j][k] + Fnx[i-1][j][k] + Fnx[i][j][k+1] + Fnx[i-1][j][k+1]);
								double Fpx_avg = 0.25*(Fpx[i][j][k] + Fpx[i-1][j][k] + Fpx[i][j][k+1] + Fpx[i-1][j][k+1]);

								double Fny_avg = 0.25*(Fny[i][j][k] + Fny[i][j-1][k] + Fny[i][j][k+1] + Fny[i][j-1][k+1]);
								double Fpy_avg = 0.25*(Fpy[i][j][k] + Fpy[i][j-1][k] + Fpy[i][j][k+1] + Fpy[i][j-1][k+1]);
								
								double Fn_ratio = (Fnx_avg*Fnx_avg+Fny_avg*Fny_avg)/(Esat_n*Esat_n);
								double Fp_ratio = (Fpx_avg*Fpx_avg+Fpy_avg*Fpy_avg)/(Esat_n*Esat_n);

								double w_n = Fnz[i][j][k]*q_n*beta*ds/2;
								double w_p = Fpz[i][j][k]*q_p*beta*ds/2;

								double mr_n = 1/Math.sqrt((Fnz[i][j][k]*Fnz[i][j][k])/(Esat_n*Esat_n)+Fn_ratio+1);
								double mr_p = 1/Math.sqrt((Fpz[i][j][k]*Fpz[i][j][k])/(Esat_p*Esat_p)+Fp_ratio+1);

								boolean approx_n = Math.abs(w_n) < 0.2;
								boolean approx_p = Math.abs(w_p) < 0.2;

								double exp_2wn = approx_n? 1 : FastExp.exp(2*w_n);
								double exp_2wp = approx_p? 1 : FastExp.exp(2*w_p);

								sigma_n = -d_n*rho_n_avg*e_charge*beta*(1 + Fn_ratio)*(mr_n*mr_n*mr_n);
								sigma_p = d_p*rho_p_avg*e_charge*beta*(1 + Fp_ratio)*(mr_p*mr_p*mr_p);

								Jz_n[i][j][k] = mr_n*d_n/ds*(-(rho_n[i][j][k+1] - rho_n[i][j][k])*Utils.xtanhxm1(w_n, exp_2wn, approx_n)
								+ 2*rho_n_avg*w_n) - sigma_n*ez_prev;
								Jz_p[i][j][k] = mr_p*d_p/ds*(-(rho_p[i][j][k+1] - rho_p[i][j][k])*Utils.xtanhxm1(w_p, exp_2wp, approx_p)
								+ 2*rho_p_avg*w_p) - sigma_p*ez_prev;

							} else {
								Jz_n[i][j][k] = 0;
								Jz_p[i][j][k] = 0;
							}

							Jz_abs[i][j][k] = 0;

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
		}
		
		public void storeDD() {
			for (int i = 0; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
					for (int j = 1; j < ny-1; j++)
					{
						for (int k = 1; k < nz-1; k++)
						{
							if (conducting_x[i][j][k] == 1) {

								double d_n = 0.5*(D_n[i+1][j][k] + D_n[i][j][k]);
								double d_p = 0.5*(D_p[i+1][j][k] + D_p[i][j][k]);

								double rho_n_avg = 0.5*(rho_n[i+1][j][k] + rho_n[i][j][k]);
								double rho_p_avg = 0.5*(rho_p[i+1][j][k] + rho_p[i][j][k]);

								double Esat_n = 0.5*(v_sat_n[i+1][j][k] + v_sat_n[i][j][k])/(d_n*e_charge*beta);
								double Esat_p = 0.5*(v_sat_p[i+1][j][k] + v_sat_p[i][j][k])/(d_p*e_charge*beta);

								double Fny_avg = 0.25*(Fny[i][j][k] + Fny[i][j-1][k] + Fny[i+1][j][k] + Fny[i+1][j-1][k]);
								double Fpy_avg = 0.25*(Fpy[i][j][k] + Fpy[i][j-1][k] + Fpy[i+1][j][k] + Fpy[i+1][j-1][k]);

								double Fnz_avg = 0.25*(Fnz[i][j][k] + Fnz[i][j][k-1] + Fnz[i+1][j][k] + Fnz[i+1][j][k-1]);
								double Fpz_avg = 0.25*(Fpz[i][j][k] + Fpz[i][j][k-1] + Fpz[i+1][j][k] + Fpz[i+1][j][k-1]);

								double Fn_ratio = (Fny_avg*Fny_avg+Fnz_avg*Fnz_avg)/(Esat_n*Esat_n);
								double Fp_ratio = (Fpy_avg*Fpy_avg+Fpz_avg*Fpz_avg)/(Esat_n*Esat_n);

								double w_n = Fnx[i][j][k]*q_n*beta*ds/2;
								double w_p = Fpx[i][j][k]*q_p*beta*ds/2;

								double mr_n = 1/Math.sqrt((Fnx[i][j][k]*Fnx[i][j][k])/(Esat_n*Esat_n) + Fn_ratio + 1);
								double mr_p = 1/Math.sqrt((Fpx[i][j][k]*Fpx[i][j][k])/(Esat_p*Esat_p) + Fp_ratio + 1);

								// whether to use taylor series approximation around x=0
								boolean approx_n = Math.abs(w_n) < 0.2;
								boolean approx_p = Math.abs(w_p) < 0.2;

								double exp_2wn = approx_n? 1 : FastExp.exp(2*w_n);
								double exp_2wp = approx_p? 1 : FastExp.exp(2*w_p);

								diff_x_n[i][j][k] = mr_n*d_n/ds*(-(rho_n[i+1][j][k] - rho_n[i][j][k])*Utils.xtanhxm1(w_n, exp_2wn, approx_n));
								diff_x_p[i][j][k] = mr_p*d_p/ds*(-(rho_p[i+1][j][k] - rho_p[i][j][k])*Utils.xtanhxm1(w_p, exp_2wp, approx_p));

								drift_x_n[i][j][k] = 2*rho_n_avg*w_n*mr_n*d_n/ds;
								drift_x_p[i][j][k] = 2*rho_p_avg*w_p*mr_p*d_p/ds;

								vel_x_n[i][j][k] = 2*w_n*mr_n*d_n/ds;
								vel_x_p[i][j][k] = 2*w_p*mr_p*d_p/ds;
							} else {
								diff_x_n[i][j][k] = 0;
								diff_x_p[i][j][k] = 0;

								drift_x_n[i][j][k] = 0;
								drift_x_p[i][j][k] = 0;

								vel_x_n[i][j][k] = 0;
								vel_x_p[i][j][k] = 0;
							}
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
							if (conducting_y[i][j][k] == 1) {

								double d_n = 0.5*(D_n[i][j+1][k] + D_n[i][j][k]);
								double d_p = 0.5*(D_p[i][j+1][k] + D_p[i][j][k]);

								double rho_n_avg = 0.5*(rho_n[i][j+1][k] + rho_n[i][j][k]);
								double rho_p_avg = 0.5*(rho_p[i][j+1][k] + rho_p[i][j][k]);

								double Esat_n = 0.5*(v_sat_n[i][j+1][k] + v_sat_n[i][j][k])/(d_n*e_charge*beta);
								double Esat_p = 0.5*(v_sat_p[i][j+1][k] + v_sat_p[i][j][k])/(d_p*e_charge*beta);

								double Fnx_avg = 0.25*(Fnx[i][j][k] + Fnx[i-1][j][k] + Fnx[i][j+1][k] + Fnx[i-1][j+1][k]);
								double Fpx_avg = 0.25*(Fpx[i][j][k] + Fpx[i-1][j][k] + Fpx[i][j+1][k] + Fpx[i-1][j+1][k]);

								double Fnz_avg = 0.25*(Fnx[i][j][k] + Fnx[i][j][k-1] + Fnx[i][j+1][k] + Fnx[i][j+1][k-1]);
								double Fpz_avg = 0.25*(Fpx[i][j][k] + Fpx[i][j][k-1] + Fpx[i][j+1][k] + Fpx[i][j+1][k-1]);

								double Fn_ratio = (Fnx_avg*Fnx_avg+Fnz_avg*Fnz_avg)/(Esat_n*Esat_n);
								double Fp_ratio = (Fpx_avg*Fpx_avg+Fpz_avg*Fpz_avg)/(Esat_n*Esat_n);

								double w_n = Fny[i][j][k]*q_n*beta*ds/2;
								double w_p = Fpy[i][j][k]*q_p*beta*ds/2;

								double mr_n = 1/Math.sqrt((Fny[i][j][k]*Fny[i][j][k])/(Esat_n*Esat_n)+Fn_ratio+1);
								double mr_p = 1/Math.sqrt((Fpy[i][j][k]*Fpy[i][j][k])/(Esat_p*Esat_p)+Fp_ratio+1);

								boolean approx_n = Math.abs(w_n) < 0.2;
								boolean approx_p = Math.abs(w_p) < 0.2;

								double exp_2wn = approx_n? 1 : FastExp.exp(2*w_n);
								double exp_2wp = approx_p? 1 : FastExp.exp(2*w_p);

								diff_y_n[i][j][k] = mr_n*d_n/ds*(-(rho_n[i][j+1][k] - rho_n[i][j][k])*Utils.xtanhxm1(w_n, exp_2wn, approx_n));
								diff_y_p[i][j][k] = mr_p*d_p/ds*(-(rho_p[i][j+1][k] - rho_p[i][j][k])*Utils.xtanhxm1(w_p, exp_2wp, approx_p));

								drift_y_n[i][j][k] = 2*rho_n_avg*w_n*mr_n*d_n/ds;
								drift_y_p[i][j][k] = 2*rho_p_avg*w_p*mr_p*d_p/ds;

								vel_y_n[i][j][k] = 2*w_n*mr_n*d_n/ds;
								vel_y_p[i][j][k] = 2*w_p*mr_p*d_p/ds;

							} else {
								diff_y_n[i][j][k] = 0;
								diff_y_p[i][j][k] = 0;

								drift_y_n[i][j][k] = 0;
								drift_y_p[i][j][k] = 0;

								vel_y_n[i][j][k] = 0;
								vel_y_p[i][j][k] = 0;
							}
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
							if (conducting_z[i][j][k] == 1) {

								double d_n = 0.5*(D_n[i][j][k+1] + D_n[i][j][k]);
								double d_p = 0.5*(D_p[i][j][k+1] + D_p[i][j][k]);

								double rho_n_avg = 0.5*(rho_n[i][j][k+1] + rho_n[i][j][k]);
								double rho_p_avg = 0.5*(rho_p[i][j][k+1] + rho_p[i][j][k]);

								double Esat_n = 0.5*(v_sat_n[i][j][k+1] + v_sat_n[i][j][k])/(d_n*e_charge*beta);
								double Esat_p = 0.5*(v_sat_p[i][j][k+1] + v_sat_p[i][j][k])/(d_p*e_charge*beta);

								double Fnx_avg = 0.25*(Fnx[i][j][k] + Fnx[i-1][j][k] + Fnx[i][j][k+1] + Fnx[i-1][j][k+1]);
								double Fpx_avg = 0.25*(Fpx[i][j][k] + Fpx[i-1][j][k] + Fpx[i][j][k+1] + Fpx[i-1][j][k+1]);

								double Fny_avg = 0.25*(Fny[i][j][k] + Fny[i][j-1][k] + Fny[i][j][k+1] + Fny[i][j-1][k+1]);
								double Fpy_avg = 0.25*(Fpy[i][j][k] + Fpy[i][j-1][k] + Fpy[i][j][k+1] + Fpy[i][j-1][k+1]);

								double Fn_ratio = (Fnx_avg*Fnx_avg+Fny_avg*Fny_avg)/(Esat_n*Esat_n);
								double Fp_ratio = (Fpx_avg*Fpx_avg+Fpy_avg*Fpy_avg)/(Esat_n*Esat_n);

								double w_n = Fnz[i][j][k]*q_n*beta*ds/2;
								double w_p = Fpz[i][j][k]*q_p*beta*ds/2;

								double mr_n = 1/Math.sqrt((Fnz[i][j][k]*Fnz[i][j][k])/(Esat_n*Esat_n)+Fn_ratio+1);
								double mr_p = 1/Math.sqrt((Fpz[i][j][k]*Fpz[i][j][k])/(Esat_p*Esat_p)+Fp_ratio+1);

								boolean approx_n = Math.abs(w_n) < 0.2;
								boolean approx_p = Math.abs(w_p) < 0.2;

								double exp_2wn = approx_n? 1 : FastExp.exp(2*w_n);
								double exp_2wp = approx_p? 1 : FastExp.exp(2*w_p);

								diff_z_n[i][j][k] = mr_n*d_n/ds*(-(rho_n[i][j][k+1] - rho_n[i][j][k])*Utils.xtanhxm1(w_n, exp_2wn, approx_n));
								diff_z_p[i][j][k] = mr_p*d_p/ds*(-(rho_p[i][j][k+1] - rho_p[i][j][k])*Utils.xtanhxm1(w_p, exp_2wp, approx_p));

								drift_z_n[i][j][k] = 2*rho_n_avg*w_n*mr_n*d_n/ds;
								drift_z_p[i][j][k] = 2*rho_p_avg*w_p*mr_p*d_p/ds;

								vel_z_n[i][j][k] = 2*w_n*mr_n*d_n/ds;
								vel_z_p[i][j][k] = 2*w_p*mr_p*d_p/ds;

							} else {
								diff_z_n[i][j][k] = 0;
								diff_z_p[i][j][k] = 0;

								drift_z_n[i][j][k] = 0;
								drift_z_p[i][j][k] = 0;

								vel_z_n[i][j][k] = 0;
								vel_z_p[i][j][k] = 0;
							}
						}
					}
				}
			}
		}

		private void storeDeff()
		{	
			for (int i = 1; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
					for (int j = 1; j < ny-1; j++)
					{
						for (int k = 1; k < nz-1; k++)
						{
							if (conducting[i][j][k] == 1) {
								double Fxn_avg = 0.5*(Fnx[i][j][k]+Fnx[i-1][j][k]);
								double Fyn_avg = 0.5*(Fny[i][j][k]+Fny[i][j-1][k]);
								double Fzn_avg = 0.5*(Fnz[i][j][k]+Fnz[i][j][k-1]);

								double Fxp_avg = 0.5*(Fpx[i][j][k]+Fpx[i-1][j][k]);
								double Fyp_avg = 0.5*(Fpx[i][j][k]+Fpx[i][j-1][k]);
								double Fzp_avg = 0.5*(Fpz[i][j][k]+Fpz[i][j][k-1]);

								double Esat_n = v_sat_n[i][j][k]/(D_n[i][j][k]*e_charge*beta);
								double Esat_p = v_sat_p[i][j][k]/(D_p[i][j][k]*e_charge*beta);

								sqrt_D_eff_n[i][j][k] = Math.sqrt(D_n[i][j][k]/Math.sqrt((Fxn_avg*Fxn_avg+Fyn_avg*Fyn_avg+Fzn_avg*Fzn_avg)/(Esat_n*Esat_n) + 1));
								sqrt_D_eff_p[i][j][k] = Math.sqrt(D_p[i][j][k]/Math.sqrt((Fxp_avg*Fxp_avg+Fyp_avg*Fyp_avg+Fzp_avg*Fzp_avg)/(Esat_p*Esat_p) + 1));
							} else {
								sqrt_D_eff_n[i][j][k] = 0;
								sqrt_D_eff_p[i][j][k] = 0;
							}
						}
					}
				}
			}
		}

		private void updateBlap() {
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


			for (int i = 0; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
					for (int j = 0; j < ny-1; j++)
					{
						for (int k = 0; k < nz-1; k++)
						{
							//debug[i][j][k] = (Hx[i+1][j][k]-Hx[i][j][k] + Hy[i][j+1][k]-Hy[i][j][k] + Hz[i][j][k+1]-Hz[i][j][k])/ds;
							debug[i][j][k] = (Bx[i+1][j+1][k+1]-Bx[i][j+1][k+1] + By[i+1][j+1][k+1]-By[i+1][j][k+1] + Bz[i+1][j+1][k+1]-Bz[i+1][j+1][k])/ds;
						}
					}
				}
			}
		}

		private void updateH() {
			for (int i = 1; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
					for (int j = 0; j < ny-1; j++)
					{
						for (int k = 0; k < nz-1; k++)
						{
							double sigma = absorptivity_x_dual[i][j][k]*mu_x[i][j][k]*absorbing_coeff;
							Hx[i][j][k] = (Hx[i][j][k]*(1-0.5*dt*sigma/mu_x[i][j][k]) + (-(Ez[i][j+1][k] - Ez[i][j][k]) + (Ey[i][j][k+1] - Ey[i][j][k]))*dt/(ds*mu_x[i][j][k])
									+ Hz_dissipation*dt*Bx_laplacian[i][j][k]/mu_x[i][j][k])/(1+0.5*dt*sigma/mu_x[i][j][k]);

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
							double sigma = absorptivity_y_dual[i][j][k]*mu_y[i][j][k]*absorbing_coeff;
							Hy[i][j][k] = (Hy[i][j][k]*(1-0.5*dt*sigma/mu_y[i][j][k]) + (-(Ex[i][j][k+1] - Ex[i][j][k]) + (Ez[i+1][j][k] - Ez[i][j][k]))*dt/(ds*mu_y[i][j][k])
									+ Hz_dissipation*dt*By_laplacian[i][j][k]/mu_y[i][j][k])/(1+0.5*dt*sigma/mu_y[i][j][k]);

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
							double sigma = absorptivity_z_dual[i][j][k]*mu_z[i][j][k]*absorbing_coeff;
							Hz[i][j][k] = (Hz[i][j][k]*(1-0.5*dt*sigma/mu_z[i][j][k]) + (-(Ey[i+1][j][k] - Ey[i][j][k]) + (Ex[i][j+1][k] - Ex[i][j][k]))*dt/(ds*mu_z[i][j][k])
									+ Hz_dissipation*dt*Bz_laplacian[i][j][k]/mu_z[i][j][k])/(1+0.5*dt*sigma/mu_z[i][j][k]);

							Bz[i+1][j+1][k] = Hz[i][j][k]*mu_z[i][j][k];
						}
					}
				}
			}
		}

		private void updateRho() {
			/* Update charge carriers */
			for (int i = 1; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
					for (int j = 1; j < ny-1; j++)
					{
						for (int k = 1; k < nz-1; k++)
						{
							if (conducting[i][j][k] == 1) {
								double n = rho_n[i][j][k]/q_n;
								double p = rho_p[i][j][k]/q_p;
								double ni = n_i[i][j][k];
								double rate_const = (k_aug_n[i][j][k]*n+k_aug_p[i][j][k]*p)
										+ (k_SRH_n[i][j][k]*k_SRH_p[i][j][k])/(k_SRH_n[i][j][k]*(n+ni) + k_SRH_p[i][j][k]*(p+ni) + Double.MIN_VALUE) // Avoid divide by zero
										+ k_rad[i][j][k];
								double recomb_rate = rate_const*(n*p - ni*ni) - L[i][j][k];

								rho_n[i][j][k] = rho_n[i][j][k] - (Jx_n[i][j][k]-Jx_n[i-1][j][k] + Jy_n[i][j][k]-Jy_n[i][j-1][k] + Jz_n[i][j][k]-Jz_n[i][j][k-1])*dt/ds - dt*q_n*recomb_rate;
								rho_p[i][j][k] = rho_p[i][j][k] - (Jx_p[i][j][k]-Jx_p[i-1][j][k] + Jy_p[i][j][k]-Jy_p[i][j-1][k] + Jz_p[i][j][k]-Jz_p[i][j][k-1])*dt/ds - dt*q_p*recomb_rate;
							}

							rho_abs[i][j][k] = rho_abs[i][j][k] - (Jx_abs[i][j][k]-Jx_abs[i-1][j][k] + Jy_abs[i][j][k]-Jy_abs[i][j-1][k] + Jz_abs[i][j][k]-Jz_abs[i][j][k-1])*dt/ds;
							rho_free[i][j][k] = rho_abs[i][j][k]+rho_n[i][j][k]+rho_p[i][j][k]+rho_back[i][j][k];
						}
					}
				}
			}
		}

		public void setB_Boundary() {
			for (int i = 1; i < nx-1; i++)
			{
				if (i >= i_min && i <= i_max) {
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
			}

			for (int j = 1; j < ny-1; j++)
			{
				if (j >= j_min && j <= j_max) {
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
			}

			for (int k = 1; k < nz-1; k++)
			{
				if (k >= k_min && k <= k_max) {
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
			}
		}
	}
	
	public void calcMiscFields(boolean updatePhi) {
		
		if (updatePhi)
			multigridSolve(false, true);

		deduceRequiredComputations();
		
		t8.start();

		//sign_violation = 0;
		double rho_n_max_tmp = 0;
		double rho_p_max_tmp = 0;
		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					// Prevent errors when taking log of 0
					if (conducting[i][j][k] == 0) {
						mu_n[i][j][k] = Double.NaN;
						mu_p[i][j][k] = Double.NaN;
						V_avg[i][j][k] = Double.NaN;
					} else {
						mu_n[i][j][k] = mu0_n[i][j][k] + kB*T*Math.log(Math.abs(rho_n[i][j][k]/q_n)) + phi[i][j][k]*q_n;
						mu_p[i][j][k] = mu0_p[i][j][k] + kB*T*Math.log(Math.abs(rho_p[i][j][k]/q_p)) + phi[i][j][k]*q_p;
						// Think about why we should take average


						//double probe_n = -e_charge*ni_metal;
						//double probe_p = e_charge*ni_metal;
						//double sigma_n = mu_electron*logmean(-rho_n[i][j][k], -probe_n);
						//double sigma_p = mu_hole*logmean(rho_p[i][j][k], probe_p);
						double sigma_n = D_n[i][j][k]*beta*e_charge*(-rho_n[i][j][k]);
						double sigma_p = D_p[i][j][k]*beta*e_charge*rho_p[i][j][k];

						V_avg[i][j][k] = (sigma_n*mu_n[i][j][k]/q_n + sigma_p*mu_p[i][j][k]/q_p)
						/(sigma_n + sigma_p) - global_voltage_offset;
					}

					if (conducting[i][j][k] == 1) {
						double n = rho_n[i][j][k]/q_n;
						double p = rho_p[i][j][k]/q_p;
						double ni = n_i[i][j][k];
						double rate_const = (k_aug_n[i][j][k]*n+k_aug_p[i][j][k]*p)
						+ (k_SRH_n[i][j][k]*k_SRH_p[i][j][k])/(k_SRH_n[i][j][k]*(n+ni) + k_SRH_p[i][j][k]*(p+ni) + Double.MIN_VALUE) // Avoid divide by zero
						+ k_rad[i][j][k];

						G[i][j][k] = rate_const*ni*ni + L[i][j][k];
						R[i][j][k] = rate_const*n*p;
					} else {
						G[i][j][k] = 0;
						R[i][j][k] = 0;
					}

					if (rho_n[i][j][k] > error_detection_threshold || rho_p[i][j][k] < -error_detection_threshold) {
						sign_violation_timer = 20;
					}

					if (rho_n[i][j][k] > big_error_detection_threshold || rho_p[i][j][k] < -big_error_detection_threshold) {
						instability_timer = 20;
						Steam.setAchievement("CRASH");
					}

					/*if (rho_n[i][j][k] > 0 || rho_p[i][j][k] < -0) {
						debug[i][j][k] = 1;
					} else {
						debug[i][j][k] = 0;
					}*/

					if (-rho_n[i][j][k] > rho_n_max_tmp)
						rho_n_max_tmp = -rho_n[i][j][k];

					if (rho_p[i][j][k] > rho_p_max_tmp)
						rho_p_max_tmp = rho_p[i][j][k];
				}
			}
		}
		renderer.rho_n_max = rho_n_max_tmp;
		renderer.rho_p_max = rho_p_max_tmp;

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

		for (int i = 0; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					if (!Double.isFinite(Hz[i][j][k]))
						numerical_overflow = true;
				}
			}
		}

		/*if (need_heat) {
			for (int i = 0; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					grad_x_n[i][j][k] = conducting_x[i][j][k] == 1? -conducting_x[i][j][k]*(E0_n[i+1][j] - E0_n[i][j][k])/ds : 0;
					grad_x_p[i][j][k] = conducting_x[i][j][k] == 1? -conducting_x[i][j][k]*(E0_p[i+1][j] - E0_p[i][j][k])/ds : 0;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 0; j < ny-1; j++)
				{
					grad_y_n[i][j][k] = conducting_x[i][j][k] == 1? -conducting_y[i][j][k]*(E0_n[i][j+1] - E0_n[i][j][k])/ds : 0;
					grad_y_p[i][j][k] = conducting_x[i][j][k] == 1? -conducting_y[i][j][k]*(E0_p[i][j+1] - E0_p[i][j][k])/ds : 0;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					double n_contrib = -(G[i][j][k]-R[i][j][k])*E0_n[i][j][k];
					double jn_contrib = 0.5*(grad_x_n[i-1][j]*Jx_n[i-1][j]+grad_x_n[i][j][k]*Jx_n[i][j][k]
							+grad_y_n[i][j-1]*Jy_n[i][j-1]+grad_y_n[i][j][k]*Jy_n[i][j][k])/q_n;

					double p_contrib = -(G[i][j][k]-R[i][j][k])*E0_p[i][j][k];
					double jp_contrib = 0.5*(grad_x_p[i-1][j]*Jx_p[i-1][j]+grad_x_p[i][j][k]*Jx_p[i][j][k]
							+grad_y_p[i][j-1]*Jy_p[i][j-1]+grad_y_p[i][j][k]*Jy_p[i][j][k])/q_p;

					double ohm_contrib = 0.5*(Ex[i-1][j]*Jx_free[i-1][j]+Ex[i][j][k]*Jx_free[i][j][k]+Ey[i][j-1]*Jy_free[i][j-1]+Ey[i][j][k]*Jy_free[i][j][k]);

					heat[i][j][k] = n_contrib + jn_contrib + p_contrib + jp_contrib + ohm_contrib;
				}
			}
		}

		if (need_entropy) {
			for (int i = 0; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					grad_x_n[i][j][k] = conducting_x[i][j][k] == 1? -(mu_n[i+1][j] - mu_n[i][j][k] - phi[i+1][j]*q_n + phi[i][j][k]*q_n)/ds : 0;
					grad_x_p[i][j][k] = conducting_x[i][j][k] == 1? -(mu_p[i+1][j] - mu_p[i][j][k] - phi[i+1][j]*q_p + phi[i][j][k]*q_p)/ds : 0;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 0; j < ny-1; j++)
				{
					grad_y_n[i][j][k] = conducting_y[i][j][k] == 1? -(mu_n[i][j+1] - mu_n[i][j][k] - phi[i][j+1]*q_n + phi[i][j][k]*q_n)/ds : 0;
					grad_y_p[i][j][k] = conducting_y[i][j][k] == 1? -(mu_p[i][j+1] - mu_p[i][j][k] - phi[i][j+1]*q_p + phi[i][j][k]*q_p)/ds : 0;
				}
			}

			for (int i = 1; i < nx-1; i++)
			{
				for (int j = 1; j < ny-1; j++)
				{
					double n_contrib = -(G[i][j][k]-R[i][j][k])*mu_n[i][j][k];
					double jn_contrib = 0.5*(grad_x_n[i-1][j]*Jx_n[i-1][j]+grad_x_n[i][j][k]*Jx_n[i][j][k]
							+grad_y_n[i][j-1]*Jy_n[i][j-1]+grad_y_n[i][j][k]*Jy_n[i][j][k])/q_n;

					double p_contrib = -(G[i][j][k]-R[i][j][k])*mu_p[i][j][k];
					double jp_contrib = 0.5*(grad_x_p[i-1][j]*Jx_p[i-1][j]+grad_x_p[i][j][k]*Jx_p[i][j][k]
							+grad_y_p[i][j-1]*Jy_p[i][j-1]+grad_y_p[i][j][k]*Jy_p[i][j][k])/q_p;

					double ohm_contrib = 0.5*(Ex[i-1][j]*Jx_free[i-1][j]+Ex[i][j][k]*Jx_free[i][j][k]+Ey[i][j-1]*Jy_free[i][j-1]+Ey[i][j][k]*Jy_free[i][j][k]);

					entropy[i][j][k] = (n_contrib + jn_contrib + p_contrib + jp_contrib + ohm_contrib)/T;
				}
			}
		}*/
		
		updateMiscFields = false;

		t8.stop();
	}
	
	public void deduceRequiredComputations() {
		ScalarView view_scalar = controls.scalarview.getOption();
		VectorView view_vector = controls.vectorview.getOption();
		
		need_diff_drift = false;
		need_diff_drift |= (view_scalar == ScalarView.ELECTRON_VEL || view_scalar == ScalarView.HOLE_VEL);
		need_diff_drift |= (view_vector == VectorView.ELECTRON_DIFFUSION || view_vector == VectorView.ELECTRON_DRIFT || view_vector == VectorView.ELECTRON_VELOCITY
			|| view_vector == VectorView.HOLE_DIFFUSION || view_vector == VectorView.HOLE_DRIFT || view_vector == VectorView.HOLE_VELOCITY);
		need_diff_drift |= (opts.gui_carriers.isSelected() && opts.menu_carrier_diffusion.isSelected());
		
		need_heat = false;
		need_heat |= (view_scalar == ScalarView.HEAT);

		need_entropy = false;
		need_entropy |= (view_scalar == ScalarView.ENTROPY);
		
		for (Probe p : probes) {
			ScalarView probe_scalar = null;
			VectorView probe_vector = null;
			if (p instanceof PointProbe) {
				probe_scalar = ((PointProbe)p).scalarname;
			} else if (p instanceof LineProbe) {
				probe_vector = ((LineProbe)p).vectorname;
			} else if (p instanceof AreaProbe) {
				probe_vector = ((AreaProbe)p).vectorname;
			} else if (p instanceof VolumeProbe) {
				probe_scalar = ((VolumeProbe)p).scalarname;
			} 
			
			need_diff_drift |= (probe_scalar == ScalarView.ELECTRON_VEL || probe_scalar == ScalarView.HOLE_VEL);
			need_diff_drift |= (probe_vector == VectorView.ELECTRON_DIFFUSION || probe_vector == VectorView.ELECTRON_DRIFT || probe_vector == VectorView.ELECTRON_VELOCITY
				|| probe_vector == VectorView.HOLE_DIFFUSION || probe_vector == VectorView.HOLE_DRIFT || probe_vector == VectorView.HOLE_VELOCITY);
			need_diff_drift |= (opts.gui_carriers.isSelected() && opts.menu_carrier_diffusion.isSelected());
			need_heat |= (probe_scalar == ScalarView.HEAT);
			need_entropy |= (probe_scalar == ScalarView.ENTROPY);
		}
	}
	
	public void performProbeMeasurements() {
		for (Probe p: probes) {
			p.measure(this, frame%controls.plotinterval == 0);
		}

		if (controls.logdata) {
			logProbeData();
			controls.logdata = false;
		}
	}
	
	public void computeScalarField(double[][][] scalarfield, int i1, int j1, int k1, ScalarView scalarview, int n_thread, int n_threads) {
		int sx = scalarfield.length;
		int sy = scalarfield[0].length;
		int sz = scalarfield[0][0].length;

		int lower = 0;
		int upper = sx;
		
		if (n_threads > 0) {
			lower = Math.min((n_thread*sx)/n_threads, sx);
			upper = Math.min(((n_thread+1)*sx)/n_threads, sx);
		}

		switch (scalarview) {
		case NONE:
			break;
		case B_FIELD:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1)
							scalarfield[i][j][k] = Utils.getFieldMagnitude(Bx, By, Bz, i+i1, j+j1, k+k1, 0, 0.5);
					}
				}
			}
			break;
		case E_FIELD:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1)
							scalarfield[i][j][k] = Utils.getFieldMagnitude(Ex, Ey, Ez, i+i1, j+j1, k+k1);
					}
				}
			}
			break;
		case D_FIELD:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1)
							scalarfield[i][j][k] = Utils.getFieldMagnitude(Dx, Dy, Dz, i+i1, j+j1, k+k1);
					}
				}
			}
			break;
		case H_FIELD:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1)
							scalarfield[i][j][k] = Utils.getDualFieldMagnitude(Hx, Hy, Hz, i+i1, j+j1, k+k1);
					}
				}
			}
			break;
		case CURRENT:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1)
							scalarfield[i][j][k] = Utils.getFieldMagnitude(Jx_free, Jy_free, Jz_free, i+i1, j+j1, k+k1);
					}
				}
			}
			break;
		case POTENTIAL:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = phi[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case CHARGE:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = rho_free[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case BACKGROUND_CHARGE:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = rho_back[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case ELECTRON_CHARGE:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = rho_n[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case HOLE_CHARGE:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = rho_p[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case COMBINED_CHARGE:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = Double.NaN;
					}
				}
			}
			break;
		case HEAT:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = heat[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case ELECTRON_POTENTIAL:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = mu_n[i+i1][j+j1][k+k1]/eVtoJ;
					}
				}
			}
			break;
		case HOLE_POTENTIAL:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = -mu_p[i+i1][j+j1][k+k1]/eVtoJ;
					}
				}
			}
			break;
		case DEBUG:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = debug[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case GENERATION:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = G[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case RECOMBINATION:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = R[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case AVERAGE_POTENTIAL:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = V_avg[i+i1][j+j1][k+k1]; // -offset
					}
				}
			}
			break;
		case ELECTRON_DENSITY:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = rho_n[i+i1][j+j1][k+k1]/q_n;
					}
				}
			}
			break;
		case ELECTRON_VEL:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1)
							scalarfield[i][j][k] = Utils.getFieldMagnitude(vel_x_n, vel_y_n, vel_z_n, i+i1, j+j1, k+k1);
					}
				}
			}
			break;
		case ELECTRON_VOLTAGE:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = -mu_n[i+i1][j+j1][k+k1]/eVtoJ;
					}
				}
			}
			break;
		case ENERGY:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1) {
							double E_energy = 0.25*(Ex[i+i1][j+j1][k+k1]*Ex[i+i1][j+j1][k+k1]*epsx[i+i1][j+j1][k+k1] + Ex[i+i1-1][j+j1][k+k1]*Ex[i+i1-1][j+j1][k+k1]*epsx[i+i1-1][j+j1][k+k1])
							+ 0.25*(Ey[i+i1][j+j1][k+k1]*Ey[i+i1][j+j1][k+k1]*epsy[i+i1][j+j1][k+k1] + Ey[i+i1][j+j1-1][k+k1]*Ey[i+i1][j+j1-1][k+k1]*epsy[i+i1][j+j1-1][k+k1])
							+ 0.25*(Ez[i+i1][j+j1][k+k1]*Ez[i+i1][j+j1][k+k1]*epsz[i+i1][j+j1][k+k1] + Ez[i+i1][j+j1][k+k1-1]*Ez[i+i1][j+j1][k+k1-1]*epsz[i+i1][j+j1][k+k1-1]);
							double B_energy = 0.125*(Hx[i+i1][j+j1][k+k1]*Hx[i+i1][j+j1][k+k1]*mu_x[i+i1][j+j1][k+k1]+Hx[i+i1][j+j1][k+k1-1]*Hx[i+i1][j+j1][k+k1-1]*mu_x[i+i1][j+j1][k+k1-1]+Hx[i+i1][j+j1-1][k+k1]*Hx[i+i1][j+j1-1][k+k1]*mu_x[i+i1][j+j1-1][k+k1]+Hx[i+i1][j+j1-1][k+k1-1]*Hx[i+i1][j+j1-1][k+k1-1]*mu_x[i+i1][j+j1-1][k+k1-1])
							+ 0.125*(Hy[i+i1][j+j1][k+k1]*Hy[i+i1][j+j1][k+k1]*mu_y[i+i1][j+j1][k+k1]+Hy[i+i1-1][j+j1][k+k1]*Hy[i+i1-1][j+j1][k+k1]*mu_y[i+i1-1][j+j1][k+k1]+Hy[i+i1][j+j1][k+k1-1]*Hy[i+i1][j+j1][k+k1-1]*mu_y[i+i1][j+j1][k+k1-1]+Hy[i+i1-1][j+j1][k+k1-1]*Hy[i+i1-1][j+j1][k+k1-1]*mu_y[i+i1-1][j+j1][k+k1-1])
							+ 0.125*(Hz[i+i1][j+j1][k+k1]*Hz[i+i1][j+j1][k+k1]*mu_z[i+i1][j+j1][k+k1]+Hz[i+i1-1][j+j1][k+k1]*Hz[i+i1-1][j+j1][k+k1]*mu_z[i+i1-1][j+j1][k+k1]+Hz[i+i1][j+j1-1][k+k1]*Hz[i+i1][j+j1-1][k+k1]*mu_z[i+i1][j+j1-1][k+k1]+Hz[i+i1-1][j+j1-1][k+k1]*Hz[i+i1-1][j+j1-1][k+k1]*mu_z[i+i1-1][j+j1-1][k+k1]);
							scalarfield[i][j][k] = E_energy + B_energy;
						}
					}
				}
			}
			break;
		case ENTROPY:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int k = 0; k < sz; k++) {
					for (int j = 0; j < sy; j++) {
						scalarfield[i][j][k] = entropy[i+i1][j+j1][k+k1];
					}
				}
			}
			break;
		case HOLE_DENSITY:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = rho_p[i+i1][j+j1][k+k1]/q_p;
					}
				}
			}
			break;
		case HOLE_VEL:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						if (i + i1 > 0 && j + j1 > 0 && k + k1 > 0 && i + i1 < nx-1 && j + j1 < ny-1 && k + k1 < nz-1)
							scalarfield[i][j][k] = Utils.getFieldMagnitude(vel_x_p, vel_y_p, vel_z_p, i+i1, j+j1, k+k1);
					}
				}
			}
			break;
		case HOLE_VOLTAGE:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						scalarfield[i][j][k] = mu_p[i+i1][j+j1][k+k1]/eVtoJ;
					}
				}
			}
			break;
		case LIGHT:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						double n = rho_n[i+i1][j+j1][k+k1]/q_n;
						double p = rho_p[i+i1][j+j1][k+k1]/q_p;
						scalarfield[i][j][k] = semiconducting[i+i1][j+j1][k+k1]*k_rad[i+i1][j+j1][k+k1]*n*p;
						if (!(scalarfield[i][j][k] > 0))
							scalarfield[i][j][k] = 0;
					}
				}
			}
			break;
		case RECOMB_AUGER:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						double n = rho_n[i+i1][j+j1][k+k1]/q_n;
						double p = rho_p[i+i1][j+j1][k+k1]/q_p;
						scalarfield[i][j][k] = (k_aug_n[i+i1][j+j1][k+k1]*n+k_aug_p[i+i1][j+j1][k+k1]*p)*n*p;
					}
				}
			}
			break;
		case RECOMB_RAD:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						double n = rho_n[i+i1][j+j1][k+k1]/q_n;
						double p = rho_p[i+i1][j+j1][k+k1]/q_p;
						scalarfield[i][j][k] = k_rad[i+i1][j+j1][k+k1]*n*p;
					}
				}
			}
			break;
		case RECOMB_SRH:
			for (int i = 0; i < sx; i++) if (i >= lower && i < upper) {
				for (int j = 0; j < sy; j++) {
					for (int k = 0; k < sz; k++) {
						double n = rho_n[i+i1][j+j1][k+k1]/q_n;
						double p = rho_p[i+i1][j+j1][k+k1]/q_p;
						double ni = n_i[i+i1][j+j1][k+k1];
						double rate_const = (k_SRH_n[i+i1][j+j1][k+k1]*k_SRH_p[i+i1][j+j1][k+k1])/(k_SRH_n[i+i1][j+j1][k+k1]*(n+ni) + k_SRH_p[i+i1][j+j1][k+k1]*(p+ni) + Double.MIN_VALUE);
						scalarfield[i][j][k] = rate_const*n*p;
					}
				}
			}
			break;
		}
	}

	public void computeVectorField(double[][][][] vectorfield, VectorView vector_view) {
		switch (vector_view) {
		case NONE:
			break;
		case D_FIELD:
			vectorfield[0] = Dx;
			vectorfield[1] = Dy;
			vectorfield[2] = Dz;
			break;
		case E_FIELD:
			vectorfield[0] = Ex;
			vectorfield[1] = Ey;
			vectorfield[2] = Ez;
			break;
		case B_FIELD:
			vectorfield[0] = Bx;
			vectorfield[1] = By;
			vectorfield[2] = Bz;
			break;
		case H_FIELD:
			vectorfield[0] = Hx;
			vectorfield[1] = Hy;
			vectorfield[2] = Hz;
			break;
		case ELECTRON_CURRENT:
			vectorfield[0] = Jx_n;
			vectorfield[1] = Jy_n;
			vectorfield[2] = Jz_n;
			break;
		case HOLE_CURRENT:
			vectorfield[0] = Jx_p;
			vectorfield[1] = Jy_p;
			vectorfield[2] = Jz_p;
			break;
		case TOTAL_CURRENT:
			vectorfield[0] = Jx_free;
			vectorfield[1] = Jy_free;
			vectorfield[2] = Jz_free;
			break;
		case EMF:
			vectorfield[0] = emfx;
			vectorfield[1] = emfy;
			vectorfield[2] = emfz;
			break;
		case ELECTRON_DIFFUSION:
			vectorfield[0] = diff_x_n;
			vectorfield[1] = diff_y_n;
			vectorfield[2] = diff_z_n;
			break;
		case ELECTRON_DRIFT:
			vectorfield[0] = drift_x_n;
			vectorfield[1] = drift_y_n;
			vectorfield[2] = drift_z_n;
			break;
		case HOLE_DIFFUSION:
			vectorfield[0] = diff_x_p;
			vectorfield[1] = diff_y_p;
			vectorfield[2] = diff_z_p;
			break;
		case HOLE_DRIFT:
			vectorfield[0] = drift_x_p;
			vectorfield[1] = drift_y_p;
			vectorfield[2] = drift_z_p;
			break;
		case POYNTING:
			vectorfield[0] = Sx;
			vectorfield[1] = Sy;
			vectorfield[2] = Sz;
			break;
		case ELECTRON_VELOCITY:
			vectorfield[0] = vel_x_n;
			vectorfield[1] = vel_y_n;
			vectorfield[2] = vel_z_n;
			break;
		case HOLE_VELOCITY:
			vectorfield[0] = vel_x_p;
			vectorfield[1] = vel_y_p;
			vectorfield[2] = vel_z_p;
			break;
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
						E0_n[i][j][k] = materials[i][j][k].Ec;
						E0_p[i][j][k] = -materials[i][j][k].Ev;
						mu0_n[i][j][k] = materials[i][j][k].Ec - kB*T*Math.log(materials[i][j][k].gc);
						mu0_p[i][j][k] = -materials[i][j][k].Ev - kB*T*Math.log(materials[i][j][k].gv);
					} else {
						E0_n[i][j][k] = 0;
						E0_p[i][j][k] = 0;
						mu0_n[i][j][k] = 0;
						mu0_p[i][j][k] = 0;
					}
				}
			}
		}

		// Smoothing out free energy in space makes simulation more stable (No longer required in v2.0)
		if (junction_size > 0) {
			markJunctions(junction_size);
			smoothArray(mu0_n, junction_size, false);
			smoothArray(mu0_p, junction_size, false);
			smoothArray(E0_n, junction_size, false);
			smoothArray(E0_p, junction_size, false);
			smoothArray(k_rad, junction_size, true);
			smoothArray(k_SRH_n, junction_size, true);
			smoothArray(k_SRH_p, junction_size, true);
			smoothArray(k_aug_n, junction_size, true);
			smoothArray(k_aug_p, junction_size, true);
			
			/*for (int i = 0; i < nx; i++)
			{
				for (int j = 0; j < ny; j++)
				{
					if (needs_smoothing[i][j][k]) {
						this.v_sat_n[i][j][k] *= 0.1;
						this.v_sat_p[i][j][k] *= 0.1;
					}
				}
			}*/
			
			/*for (int i = 0; i < junction_size; i++) {
				smoothArray(mu0_n, 1, false);
				smoothArray(mu0_p, 1, false);
				smoothArray(E0_n, 1, false);
				smoothArray(E0_p, 1, false);
				smoothArray(k_rad, 1, true);
				smoothArray(k_SRH_n, 1, true);
				smoothArray(k_SRH_p, 1, true);
				smoothArray(k_aug_n, 1, true);
				smoothArray(k_aug_p, 1, true);
			}*/
		}
		
		if (dopant_smoothing_distance > 0) {
			markJunctions(dopant_smoothing_distance);
			smoothArray(rho_back, dopant_smoothing_distance, false);
		}

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (conducting[i][j][k] == 1) {
						n_i[i][j][k] = Math.exp(-0.5*beta*(mu0_n[i][j][k]+mu0_p[i][j][k]));
					} else {
						n_i[i][j][k] = 0;
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
					cmfx_n[i][j][k] = -conducting_x[i][j][k]*(mu0_n[i+1][j][k] - mu0_n[i][j][k])/ds;
					cmfx_p[i][j][k] = -conducting_x[i][j][k]*(mu0_p[i+1][j][k] - mu0_p[i][j][k])/ds;
				}
			}
		}

		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 1; k < nz-1; k++)
				{
					cmfy_n[i][j][k] = -conducting_y[i][j][k]*(mu0_n[i][j+1][k] - mu0_n[i][j][k])/ds;
					cmfy_p[i][j][k] = -conducting_y[i][j][k]*(mu0_p[i][j+1][k] - mu0_p[i][j][k])/ds;
				}
			}
		}
		
		for (int i = 1; i < nx-1; i++)
		{
			for (int j = 1; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					cmfz_n[i][j][k] = -conducting_z[i][j][k]*(mu0_n[i][j][k+1] - mu0_n[i][j][k])/ds;
					cmfz_p[i][j][k] = -conducting_z[i][j][k]*(mu0_p[i][j][k+1] - mu0_p[i][j][k])/ds;
				}
			}
		}

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (conducting[i][j][k] == 0) {
						mu0_n[i][j][k] = Double.NaN;
						mu0_p[i][j][k] = Double.NaN;
						E0_n[i][j][k] = Double.NaN;
						E0_p[i][j][k] = Double.NaN;
					}
				}
			}
		}
	}
	
	public void smoothArray(double[][][] arr, int smoothing_radius, boolean logarithmic) {

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					smooth_arr[i][j][k] = logarithmic? Math.log(arr[i][j][k]) : arr[i][j][k];
				}
			}
		}

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					if (needs_smoothing[i][j][k]) {
						double sum = 0;
						double neighbors = 0;

						markNeighborhood(i, j, k, smoothing_radius);

						for (int di = -smoothing_radius; di <= smoothing_radius; di++) {
							for (int dj = -smoothing_radius; dj <= smoothing_radius; dj++) {
								for (int dk = -smoothing_radius; dk <= smoothing_radius; dk++) {
									if (i+di >= 0 && j+dj >= 0 && i+di < nx && j+dj < ny && k+dk < nz && conducting[i+di][j+dj][k+dk] == 1 && visited[i+di][j+dj][k+dk]) {
										sum += smooth_arr[i+di][j+dj][k+dk];
										neighbors += 1;
									}
									distance[i+di][j+dj][k+dk] = Integer.MAX_VALUE;
									visited[i+di][j+dj][k+dk] = false;
								}
							}
						}

						if (logarithmic) {
							arr[i][j][k] = (sum == Double.NaN)? 0 : Math.exp(sum/neighbors);
						} else {
							arr[i][j][k] = sum/neighbors;
						}
					}
				}
			}
		}
	}

	public void updateAllMaterials(boolean updateRho) {
		constructBoundary();
		
		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny-1; j++)
			{
				for (int k = 0; k < nz-1; k++)
				{
					mu_x[i][j][k] = 0.25*mu0*(materials[i][j][k].mu_r+materials[i][j][k+1].mu_r+materials[i][j+1][k].mu_r+materials[i][j+1][k+1].mu_r);
					absorptivity_z_dual[i][j][k] = magnetic_absorption*0.25*(materials[i][j][k].absorptivity+materials[i][j][k+1].absorptivity+materials[i][j+1][k].absorptivity+materials[i][j+1][k+1].absorptivity);
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
					absorptivity_y_dual[i][j][k] = magnetic_absorption*0.25*(materials[i][j][k].absorptivity+materials[i+1][j][k].absorptivity+materials[i][j][k+1].absorptivity+materials[i+1][j][k+1].absorptivity);
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
					absorptivity_z_dual[i][j][k] = magnetic_absorption*0.25*(materials[i][j][k].absorptivity+materials[i+1][j][k].absorptivity+materials[i][j+1][k].absorptivity+materials[i+1][j+1][k].absorptivity);
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
					conducting[i][j][k] = materials[i][j][k].conducting;
					semiconducting[i][j][k] = materials[i][j][k].semiconducting;
					k_rad[i][j][k] = materials[i][j][k].k_rad;
					k_SRH_n[i][j][k] = materials[i][j][k].k_SRH_n;
					k_SRH_p[i][j][k] = materials[i][j][k].k_SRH_p;
					k_aug_n[i][j][k] = materials[i][j][k].k_aug_n;
					k_aug_p[i][j][k] = materials[i][j][k].k_aug_p;
					D_n[i][j][k] = materials[i][j][k].D_n;
					D_p[i][j][k] = materials[i][j][k].D_p;
					v_sat_n[i][j][k] = materials[i][j][k].v_sat_n;
					v_sat_p[i][j][k] = materials[i][j][k].v_sat_p;

					if (materials[i][j][k].type == MaterialType.SWITCH) {
						if (materials[i][j][k].activated == 0) {
							D_n[i][j][k] *= switch_open_mobility;
							D_p[i][j][k] *= switch_open_mobility;
						}
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
					if (n_i[i][j][k] > 0) {
						if (updateRho || (rho_n[i][j][k] == 0 && rho_p[i][j][k] == 0)) {
							rho_n[i][j][k] = calcEquilibriumElectronCharge(rho_back[i][j][k], n_i[i][j][k]*n_i[i][j][k]);
							rho_p[i][j][k] = calcEquilibriumHoleCharge(rho_back[i][j][k], n_i[i][j][k]*n_i[i][j][k]);
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

	public double calcEquilibriumElectronCharge(double rho_back, double K) {
		double B = rho_back/e_charge;
		return -e_charge*0.5*(B+Math.sqrt(B*B+4*K));
	}

	public double calcEquilibriumHoleCharge(double rho_back, double K) {
		double B = -rho_back/e_charge;
		return e_charge*0.5*(B+Math.sqrt(B*B+4*K));
	}

	public void markJunctions(int smoothing_radius) {

		for (int i = 0; i < nx; i++)
		{
			for (int j = 0; j < ny; j++)
			{
				for (int k = 0; k < nz; k++)
				{
					needs_smoothing[i][j][k] = false;
					Material mat = materials[i][j][k];
					if (conducting[i][j][k] == 1) {

						markNeighborhood(i, j, k, smoothing_radius);

						for (int di = -smoothing_radius; di <= smoothing_radius; di++) {
							for (int dj = -smoothing_radius; dj <= smoothing_radius; dj++) {
								for (int dk = -smoothing_radius; dk <= smoothing_radius; dk++) {
									if (i+di >= 0 && j+dj >= 0 && k+dk >= 0 && i+di < nx && j+dj < ny && k+dk < nz) {
										if (conducting[i+di][j+dj][k+dk] == 1) {
											if (!visited[i+di][j+dj][k+dk]) {
												needs_smoothing[i][j][k] = true;
											}

											if (mat.type != materials[i+di][j+dj][k+dk].type || mat.cust_id != materials[i+di][j+dj][k+dk].cust_id) {
												needs_smoothing[i][j][k] = true;
											}
										}

										distance[i+di][j+dj][k+dk] = Integer.MAX_VALUE;
										visited[i+di][j+dj][k+dk] = false;
									}
								}
							}
						}
					}
				}
			}
		}
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
	
	public void eraseMaterial(int i, int j, int k) {
		materials[i][j][k].initialize();
		rho_p[i][j][k] = 0;
		rho_n[i][j][k] = 0;
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
		if (i < 0 || j < 0 || k < 0 || i >= nx || j >= ny || k >= nz)
			return;
		
		if (materials[i][j][k].cust_id == -1) {
			initializeMaterial(materials[i][j][k], materials[i][j][k].type);
		} else {
			if (materialmanager.mat_map.containsKey(materials[i][j][k].cust_id))
				materials[i][j][k].copyFrom(materialmanager.mat_map.get(materials[i][j][k].cust_id));
			else
				materials[i][j][k].initialize();
		}
	}

	public void initializeMaterial(int i, int j, int k, GeneralMaterialType material) {
		if (i < 0 || j < 0 || k < 0 || i >= nx || j >= ny || k >= nz)
			return;

		if (material.cust_id == -1) {
			initializeMaterial(materials[i][j][k], material.type);
		} else {
			if (materialmanager.mat_map.containsKey(material.cust_id))
				materials[i][j][k].copyFrom(materialmanager.mat_map.get(material.cust_id));
			else
				materials[i][j][k].initialize();
		}
	}


	public void initializeMaterial(Material mat, GeneralMaterialType material) {
		if (material.cust_id == -1) {
			initializeMaterial(mat, material.type);
		} else {
			if (materialmanager.mat_map.containsKey(material.cust_id))
				mat.copyFrom(materialmanager.mat_map.get(material.cust_id));
			else
				mat.initialize();
		}
	}

	public void initializeMaterial(Material mat, MaterialType material) {
		if (mat.modified)
			return;

		mat.type = material;

		if (material == MaterialType.DIELECTRIC) mat.eps_r = dielectric_eps_r;
		else if (material == MaterialType.FERROMAGNET) mat.mu_r = ferromagnet_mu_r;
		else if (material == MaterialType.POS_CHARGE) mat.rho_back = staticcharge_density;
		else if (material == MaterialType.NEG_CHARGE) mat.rho_back = -staticcharge_density;

		if (material.isConducting())
		{
			mat.conducting = 1;
			double Phi = W_metal_default;
			double g = g_metal;
			mat.k_rad = k_rad_metal;
			mat.k_SRH_n = 0;
			mat.k_SRH_p = 0;
			mat.k_aug_n = 0;
			mat.k_aug_p = 0;
			mat.D_n = D_electron_metal;
			mat.D_p = D_hole_metal;
			mat.v_sat_n = v_sat_n_metal;
			mat.v_sat_p = v_sat_p_metal;
			mat.eps_r = eps_r_metal;
			//mat.mu_r = 1/mat.eps_r;

			if (material == MaterialType.METAL_HIGH_W) Phi = W_metal_high;
			else if (material == MaterialType.METAL_LOW_W) Phi = W_metal_low;
			else if (material == MaterialType.METAL_HIGH_C) g = g_metal_high;
			else if (material == MaterialType.METAL_LOW_C) g = g_metal_low;
			else if (material == MaterialType.CURRENT) {
				g = g_currentsource;
				mat.D_n *= currentsource_mobility;
				mat.D_p *= currentsource_mobility;
			}
			
			mat.computeBandstructurePhiG(Phi, Eg_metal, g, g, kB*T);
		}

		if (material.isSemiconducting())
		{
			mat.conducting = 1;
			mat.semiconducting = 1;
			mat.k_rad = k_rad_semi;
			mat.k_SRH_n = k_SRH_n_semi;
			mat.k_SRH_p = k_SRH_p_semi;
			mat.k_aug_n = k_aug_n_semi;
			mat.k_aug_p = k_aug_p_semi;
			mat.D_n = D_electron_semi;
			mat.D_p = D_hole_semi;
			mat.v_sat_n = v_sat_n_semi;
			mat.v_sat_p = v_sat_p_semi;
			mat.eps_r = eps_r_semi;
			//mat.mu_r = 1/mat.eps_r;

			if (material == MaterialType.SEMI_P_TYPE) mat.rho_back = -p_default_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_N_TYPE) mat.rho_back = n_default_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_HEAVY_P_TYPE) mat.rho_back = -p_heavy_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_HEAVY_N_TYPE) mat.rho_back = n_heavy_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_LIGHT_P_TYPE) mat.rho_back = -p_light_doping_concentration*e_charge;
			else if (material == MaterialType.SEMI_LIGHT_N_TYPE) mat.rho_back = n_light_doping_concentration*e_charge;

			mat.computeBandstructureChiG(chi_semi, Eg_semi, gc_semi, gv_semi, kB*T);
			
			mat.D_n *= calculateMobilityFactor(d_crit_n, mat.rho_back);
			mat.D_p *= calculateMobilityFactor(d_crit_p, mat.rho_back);
		}

		mat.auto_placed = false;
	}
	
	public double calculateMobilityFactor(double d_crit, double donorDensity) {
		double density = Math.abs(donorDensity/e_charge);
		return 1/(Math.sqrt(density/d_crit)+1);
		
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
						for (int m = 0; m < log2_resolution; m++) {
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
				
				if (controls.debugging)
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

			for (int fineness = 0; fineness < log2_resolution; fineness++) {
				int scalefactor = 1 << (log2_resolution-fineness-1);
				int nx_tmp = nx / scalefactor;
				int ny_tmp = ny / scalefactor;
				int nz_tmp = nz / scalefactor;

				int poissonsteps = stepsarray[(fineness > 8)? 8 : fineness];
				double alpha = ds*ds*scalefactor*scalefactor;

				JacobiIteration(poissonsteps, nx_tmp, ny_tmp, nz_tmp, alpha, fineness, computePhi);

				if (fineness == log2_resolution - 1)
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

				if (controls.debugging)
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
					dest[log2_resolution-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = log2_resolution-2; m >= 0; m--) {
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
					dest[log2_resolution-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = log2_resolution-2; m >= 0; m--) {
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
					dest[log2_resolution-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = log2_resolution-2; m >= 0; m--) {
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
					dest[log2_resolution-1][i][j][k] = source[i][j][k];
				}
			}
		}
		int nx_d = nx;
		int ny_d = ny;
		int nz_d = nz;
		for (int m = log2_resolution-2; m >= 0; m--) {
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
	
	public void logProbeData() {
		String data = "";
		data += ("t = " + units.toString(time, Quantity.TIME));

		for (Probe p: probes) {
			if (!(p instanceof Ground || p instanceof Ruler)) {
				data += ", ";
				data += p.getText(units);
			}
		}

		data += "\n";

		datastream.print(data);
		datastream.flush();

		renderer.probetexttimer = 30;
	}
	
	public void setDebugInfo() {
		if (!controls.debugging) {
			return;
		}
		
		int mx = controls.mx;
		int my = controls.my;
		int mz = controls.mz;
		
		String str = "";
		str += "Mouse\n";
		str += ("x\t"  						+	units.toString(mx*ds, Quantity.LENGTH) + "\n");
		str += ("y\t"  						+	units.toString(my*ds, Quantity.LENGTH) + "\n");
		str += ("z\t"  						+	units.toString(mz*ds, Quantity.LENGTH) + "\n");
		str += "\nFields\n";
		str += ("E\t"  						+	units.toString(Utils.bilinearinterp_length(Ex, Ey, Ez, mx, my, mz), Quantity.ELECTRIC_FIELD) + "\n");
		str += ("D\t"  						+	units.toString(Utils.bilinearinterp_length(Dx, Dy, Dz, mx, my, mz), Quantity.ELECTRIC_FLUX_DENSITY) + "\n");
		str += ("B\t"  						+	units.toString(parity*Utils.bilinearinterp_length(Bx, By, Bz, mx, my, mz), Quantity.MAGNETIC_FLUX_DENSITY) + "\n");
		str += ("H\t"  						+	units.toString(parity*Utils.bilinearinterp_length(Hx, Hy, Hz, mx, my, mz), Quantity.MAGNETIC_FIELD_STRENGTH) + "\n");
		str += "\nCharge/current\n";
		str += ("\u03c1\u2099\t"  			+	units.toString(rho_n[mx][my][mz], Quantity.CHARGE_DENSITY) + "\n");
		str += ("\u03c1\u209A\t"  			+	units.toString(rho_p[mx][my][mz], Quantity.CHARGE_DENSITY) + "\n");
		str += ("\u03c1\u2080\t"			+	units.toString(rho_back[mx][my][mz], Quantity.CHARGE_DENSITY) + "\n");
		str += ("\u03c1 abs\t"				+	units.toString(rho_abs[mx][my][mz], Quantity.CHARGE_DENSITY) + "\n");
		str += ("\u03c1\t"  				+	units.toString(rho_free[mx][my][mz], Quantity.CHARGE_DENSITY) + "\n");
		str += ("J\u2099\t" 				+	units.toString(Utils.bilinearinterp_length(Jx_n, Jy_n, Jz_n, mx, my, mz), Quantity.CURRENT_DENSITY) + "\n");
		str += ("J\u209A\t"  				+	units.toString(Utils.bilinearinterp_length(Jx_p, Jy_p, Jz_p, mx, my, mz), Quantity.CURRENT_DENSITY) + "\n");
		str += ("J abs\t"  					+	units.toString(Utils.bilinearinterp_length(Jx_abs, Jy_abs, Jz_abs, mx, my, mz), Quantity.CURRENT_DENSITY) + "\n");
		str += ("J\t"  						+	units.toString(Utils.bilinearinterp_length(Jx_free, Jy_free, Jz_free, mx, my, mz), Quantity.CURRENT_DENSITY) + "\n");
		str += "\nThermodynamic quantities\n";
		str += ("\u03d5\t"  				+	units.toString(phi[mx][my][mz], Quantity.ELECTRIC_POTENTIAL) + "\n");
		str += ("F\u2099\t"  				+	units.toString(mu_n[mx][my][mz]/eVtoJ, Quantity.ELECTRIC_POTENTIAL) + "\n");
		str += ("F\u209a\t"  				+	units.toString(-mu_p[mx][my][mz]/eVtoJ, Quantity.ELECTRIC_POTENTIAL) + "\n");
		str += ("V\t"  						+	units.toString(V_avg[mx][my][mz], Quantity.ELECTRIC_POTENTIAL) + "\n");
		str += ("G\t"  						+	units.toString(G[mx][my][mz], Quantity.RATE_DENSITY) + "\n");
		str += ("R\t"  						+	units.toString(R[mx][my][mz], Quantity.RATE_DENSITY) + "\n");
		str += ("Electron CMF\t"  			+	units.toString(Utils.bilinearinterp_length(cmfx_n, cmfy_n, cmfz_n, mx, my, mz), Quantity.FORCE) + "\n");
		str += ("Hole CMF\t"  				+	units.toString(Utils.bilinearinterp_length(cmfx_p, cmfy_p, cmfz_p, mx, my, mz), Quantity.FORCE) + "\n");
		str += ("EMF\t"  					+	units.toString(Utils.bilinearinterp_length(emfx, emfy, emfz, mx, my, mz), Quantity.ELECTRIC_FIELD) + "\n");
		str += ("Electron vel.\t"  			+	units.toString(Utils.bilinearinterp_length(Jx_n, Jy_n, Jz_n, mx, my, mz)/rho_n[mx][my][mz], Quantity.VELOCITY) + "\n");
		str += ("Hole vel.\t"  				+	units.toString(Utils.bilinearinterp_length(Jx_p, Jy_p, Jz_p, mx, my, mz)/rho_p[mx][my][mz], Quantity.VELOCITY) + "\n");
		str += "\nNumerical stability ratios\n";
		str += CFL_text;
		
		opts.textPane.setText(str);
	}

	public boolean hasGround() {
		for (Probe p : probes) {
			if (p instanceof Ground)
				return true;
		}
		
		return false;
	}
	
	public Ground getGround() {
		for (Probe p : probes) {
			if (p instanceof Ground)
				return (Ground)p;
		}
		
		return null;
	}
	
	public boolean hasRuler() {
		for (Probe p : probes) {
			if (p instanceof Ruler)
				return true;
		}
		
		return false;
	}
	
	public Ruler getRuler() {
		for (Probe p : probes) {
			if (p instanceof Ruler)
				return (Ruler)p;
		}
		
		return null;
	}
	
	public void relabelProbes() {
		probe_index = 0;
		for (Probe p : probes) {
			p.name = getProbeName();
			probe_index++;
		}
	}
	
	public void addProbe(Probe p) {
		probes.add(p);
		relabelProbes();
	}
	
	public void removeProbe(Probe p) {
		probes.remove(p);
		relabelProbes();
	}
	
	int probe_index = 0;
	public String getProbeName() {
		if (probe_index < 26) return String.valueOf((char)('a'+probe_index));
		return String.valueOf(probe_index - 26);
	}
	
	public enum BoundaryCondition {
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
}