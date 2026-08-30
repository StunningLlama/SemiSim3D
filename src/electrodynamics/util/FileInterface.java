package electrodynamics.util;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

public abstract class FileInterface {

	/* Saving and loading */

	public File infile;
	public File outfile;
	public File currentfile;
	public int saveversion = 0;
	public boolean multifile = false;
	public String fileextension = "*";
	public String read_title = "Open file";
	public String write_title = "Save file";
	public Path startingpath = Paths.get(".");
	public Component window = null;
	
	public FileInterface() {}
	
	public void readFile()
	{
		SwingUtilities.invokeLater(() -> {
			File testfile = startingpath.toFile();
			if (!testfile.canRead()) {
				JOptionPane.showMessageDialog(window,
				"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
			}

			JFileChooser fd = new JFileChooser(startingpath.toFile());
			fd.setDialogTitle(read_title);
			fd.setMultiSelectionEnabled(multifile);
			fd.setFileFilter(new FileFilter(){
				@Override
				public boolean accept(File f) {
					if (f.isDirectory() || f.getName().endsWith(fileextension)) return true;
					return false;
				}
				@Override
				public String getDescription() {
					return fileextension;
				}
			});
			fd.setVisible(true);
			int result = fd.showOpenDialog(window);
			startingpath = fd.getCurrentDirectory().toPath();

			if (result == JFileChooser.APPROVE_OPTION) {
				if (multifile) {
					File[] files = fd.getSelectedFiles();
					for (File infile : files)
						readfile(infile);
				} else {
					infile = fd.getSelectedFile();
					readfile(infile);
				}
			} else {
				infile = null;
			}
		});
	}

	public void readfile(File infile) {
		readfile(infile, null);
	}
	
	public abstract void readfile(File file, Runnable callback);

	public void writeFile(boolean saveas)
	{
		SwingUtilities.invokeLater(() -> {
			if (!saveas && currentfile != null && currentfile.exists()) {
				writeFile(currentfile);
				return;
			}
			
			File testfile = startingpath.toFile();
			if (!testfile.canWrite()) {
				JOptionPane.showMessageDialog(window,
				"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
			}

			JFileChooser fd = new JFileChooser(startingpath.toFile());
			fd.setDialogTitle(write_title);
			fd.setFileFilter(new FileFilter(){
				@Override
				public boolean accept(File f) {
					if (f.isDirectory() || f.getName().endsWith(fileextension)) return true;
					return false;
				}
				@Override
				public String getDescription() {
					return fileextension;
				}
			});
			int result = fd.showSaveDialog(window);
			startingpath = fd.getCurrentDirectory().toPath();

			if (result == JFileChooser.APPROVE_OPTION)
				outfile = fd.getSelectedFile();
			else
				outfile = null;

			if (outfile == null) return;
			if (!outfile.getName().endsWith(fileextension))
				outfile = new File(outfile.getAbsolutePath() + fileextension);

			if (outfile.exists()) {
				String[] options = {"Yes", "No"};

				result = JOptionPane.showOptionDialog(window, "A file named " + outfile.getName() + " already exists. Do you wish to overwrite it?", "Message", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (result != JOptionPane.OK_OPTION)
					return;
			}
			
			writeFile(outfile);
			currentfile = outfile;
		});
	}

	public void writeFile(File outfile) {
		writeFile(outfile, null);
	}
	
	public abstract void writeFile(File file, Runnable callback);
}
