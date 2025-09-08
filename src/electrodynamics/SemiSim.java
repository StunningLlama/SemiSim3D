package electrodynamics;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import electrodynamics.Electrodynamics.SimulationThread;

public class SemiSim {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(
			UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		Electrodynamics w = new Electrodynamics();
		
		java.util.Timer t = new java.util.Timer();
		//javax.swing.Timer t = new javax.swing.Timer(w.renderer.frameduration, w.controls);
		for (int i = 0; i < w.n_threads; i++) {
			w.sim_threads.add(w.new SimulationThread(i, w.n_threads, w.nx));
		}

		for (int i = 0; i < w.n_threads; i++) {
			w.sim_threads.get(i).start();
		}

		t.schedule(w, 0, w.renderer.frameduration);
		w.renderer.set3Dmode();
		//t.start();

	}
	
	public static void detect64Bit(MainWindow opts) {
		if (!System.getProperty("sun.arch.data.model").equals("64"))
		{
			int result = JOptionPane.showConfirmDialog(opts, "Running this application on a 32-bit platform may cause some issues. Do you still wish to proceed?", "Warning", JOptionPane.YES_NO_OPTION);
			if (result != JOptionPane.OK_OPTION)
			{
				System.exit(0);
			}
		}
	}
}
