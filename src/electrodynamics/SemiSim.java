// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Electromagnetic Simulation which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import electrodynamics.Simulation.SimulationThread;

public class SemiSim {
	
	public static SemiSim instance;
	public static int n_threads = Runtime.getRuntime().availableProcessors();

	ArrayList<SimulationThread> sim_threads = new ArrayList<>();
	Timer master_timer = new Timer();
	Timer graphics_timer = new Timer();
	Timer misc_timer = new Timer();
	ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(3, new LoggingRejectionHandler());
	Simulation sim;
	
	public SemiSim() {
		sim = new Simulation();
		
		//master_timer.schedule(sim, 0, sim.renderer.frameduration);
		//graphics_timer.schedule(sim.renderer, 0, sim.renderer.frameduration);
		//misc_timer.schedule(sim.potentialSolver, 0, sim.renderer.frameduration);
	}
	
	public void startThreads() {
		for (int i = 0; i < n_threads; i++) {
			sim_threads.add(sim.new SimulationThread(i, n_threads, sim.nx));
		}
		
		for (int i = 0; i < n_threads; i++) {
			sim_threads.get(i).start();
		}

		threadPool.schedule(sim, 0, TimeUnit.MILLISECONDS);
		threadPool.schedule(sim.renderer, 0, TimeUnit.MILLISECONDS);
		threadPool.schedule(sim.potentialSolver, 0, TimeUnit.MILLISECONDS);
	}
	
	
    static class LoggingRejectionHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("[ERROR] Task rejected: " + r + 
                               " | Pool size=" + executor.getPoolSize() + 
                               " | Active count=" + executor.getActiveCount() + 
                               " | Queue size=" + executor.getQueue().size());
        }
    }
	
	
	public static void displayErrorMessage(Exception e) {
		if (instance == null) return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				JOptionPane.showMessageDialog(instance.sim.opts, e.toString(), "Error", JOptionPane.OK_OPTION);
				e.printStackTrace();
				try {
					PrintWriter pw = new PrintWriter(new FileOutputStream("error_log.txt"));
				    e.printStackTrace(pw);
				    pw.flush();
				    pw.close();
				} catch (FileNotFoundException e1) {
					System.exit(-1);
				}     
				System.exit(-1);
			});
		} catch (InvocationTargetException | InterruptedException e1) {
			System.exit(-1);
		}
	}

	public static void detect64Bit() {
		if (instance == null) return;
		if (!System.getProperty("sun.arch.data.model").equals("64"))
		{
			int result = JOptionPane.showConfirmDialog(instance.sim.opts, "Running this application on a 32-bit platform may cause some issues. Do you still wish to proceed?", "Message", JOptionPane.YES_NO_OPTION);
			if (result != JOptionPane.OK_OPTION)
			{
				System.exit(0);
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		try {
			SwingUtilities.invokeAndWait(() -> {
				instance = new SemiSim();
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		instance.startThreads();
	}
}
