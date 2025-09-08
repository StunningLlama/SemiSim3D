package electrodynamics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

public class SaveManager {
	Electrodynamics e;
	
	/* Saving and loading */

	public static File infile;
	public static File outfile;
	int saveversion = 1;
	String fileextension = ".semisim";
	String startingpath = ".";
	
	public SaveManager(Electrodynamics e) {
		this.e = e;
	}
	
	public void readFile()
	{
		SwingUtilities.invokeLater(() -> {
			e.rwLock.writeLock().lock();
			try {
				File testfile = new File(startingpath);
				if (!testfile.canRead()) {
					JOptionPane.showMessageDialog(e.opts,
					"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
				}

				JFileChooser fd = new JFileChooser(startingpath);
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
				int result = fd.showOpenDialog(e.opts);
				startingpath = fd.getCurrentDirectory().getPath();

				if (result == JFileChooser.APPROVE_OPTION)
					infile = fd.getSelectedFile();
				else
					infile = null;

				if (infile == null || !infile.exists()) return;
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
					dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					dialog.setVisible(true);


					e.initializeGrid(0.1e-6, 32, 32, 32);

					while (fstr.hasNext()) {
						String name = fstr.nextName();
						switch (name){
						case "time": e.time = fstr.nextDouble(); break;
						case "gui_paused": e.opts.gui_paused.setSelected(fstr.nextBoolean()); break;
						case "gui_tooltip": e.opts.gui_tooltip.setSelected(fstr.nextBoolean()); break;
						case "gui_text_bg": e.opts.gui_text_bg.setSelected(fstr.nextBoolean()); break;
						case "gui_view": e.opts.gui_view.setSelectedIndex(fstr.nextInt()); break;
						case "gui_view_vec": e.opts.gui_view_vec.setSelectedIndex(fstr.nextInt()); break;
						case "gui_view_vec_mode": e.opts.gui_view_vec_mode.setSelectedIndex(fstr.nextInt()); break;
						case "gui_simspeed": e.opts.gui_simspeed.setValue(fstr.nextInt()); break;
						case "gui_simspeed_2": e.opts.gui_simspeed_2.setValue(fstr.nextInt()); break;
						case "gui_brightness": e.opts.gui_brightness.setValue(fstr.nextInt()); break;
						case "gui_brightness_vec": e.opts.gui_brightness_vec.setValue(fstr.nextInt()); break;
						case "gui_elem_colors": e.opts.gui_elem_colors.setSelected(fstr.nextBoolean()); break;
						case "gui_bc": e.opts.gui_bc.setSelectedIndex(fstr.nextInt()); break;
						case "description": e.opts.textPane.setText(fstr.nextString()); break;
						case "gui_3d_view": e.opts.gui_3d_view.setSelectedIndex(fstr.nextInt()); break;
						case "gui_zslice": e.opts.gui_slice.setValue(fstr.nextInt()); break;
						case "pitch": e.renderer.pitch = (float)(fstr.nextDouble()); break;
						case "yaw": e.renderer.yaw = (float)(fstr.nextDouble()); break;
						case "zoom": e.renderer.scale = (float)(fstr.nextDouble()); break;

						case "ex": e.Ex = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "ey": e.Ey = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "ez": e.Ez = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "hx": e.Hx = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "hy": e.Hy = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "hz": e.Hz = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "bx": e.Bx = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "by": e.By = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "bz": e.Bz = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;

						case "rho_c": e.rho_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "rho_n": e.rho_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "rho_p": e.rho_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "rho_back": e.rho_back = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "rho_free": e.rho_free = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;

						case "jx_c": e.Jx_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jy_c": e.Jy_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jz_c": e.Jz_abs = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jx_n": e.Jx_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jy_n": e.Jy_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jz_n": e.Jz_n = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jx_p": e.Jx_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jy_p": e.Jy_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "jz_p": e.Jz_p = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;

						case "materials": e.materials = validateArraySize((Material[][][]) gson.fromJson(fstr, Material[][][].class)); break;

						case "voltageprobes": e.voltageprobes = new CopyOnWriteArrayList<>(Arrays.asList(
						(VoltageProbe[]) gson.fromJson(fstr, VoltageProbe[].class))); break;
						case "currentprobes": e.currentprobes = new CopyOnWriteArrayList<>(Arrays.asList(
						(CurrentProbe[]) gson.fromJson(fstr, CurrentProbe[].class))); break;

						case "ground": e.ground = (VoltageProbe) gson.fromJson(fstr, VoltageProbe.class); break;

						default: fstr.skipValue(); break; // skip others
						}
					}
					fstr.endObject();
					fstr.close();

					e.opts.textPane.setEditable(false);
					e.opts.textPane.setCaretPosition(0);
					e.constructBoundary();
					e.initializeAllMaterials();
					e.updateAllMaterials();
					e.calcMiscFields(true);

					dialog.dispose();
					e.opts.setTitle(e.sim_name + " - " + infile.getName());
				} catch (FileNotFoundException e) {
					return;
				} catch (IOException | IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(e.opts,
					"Error: Unable to load file.");
					ex.printStackTrace();
					return;
				}
				return;
			} finally {
				e.rwLock.writeLock().unlock();
			}
		});
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

	public void writeFile()
	{
		SwingUtilities.invokeLater(() -> {
			e.rwLock.writeLock().lock();
			try {
				File testfile = new File(startingpath);
				if (!testfile.canWrite()) {
					JOptionPane.showMessageDialog(e.opts,
					"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
				}

				JFileChooser fd = new JFileChooser(startingpath);
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
				int result = fd.showSaveDialog(e.opts);
				startingpath = fd.getCurrentDirectory().getPath();

				if (result == JFileChooser.APPROVE_OPTION)
					outfile = fd.getSelectedFile();
				else
					outfile = null;

				if (outfile == null) return;
				if (!outfile.getName().endsWith(fileextension))
					outfile = new File(outfile.getAbsolutePath() + fileextension);

				if (outfile.exists()) {
					result = JOptionPane.showConfirmDialog(e.opts, "A file with that name already exists. Do you wish to overwrite it?", "Save file", JOptionPane.YES_NO_OPTION);
					if (result != JOptionPane.OK_OPTION)
						return;
				}

				try {
					PrintWriter fstr = new PrintWriter(new GZIPOutputStream(new FileOutputStream(outfile)));

					Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

					JsonObject obj = new JsonObject();



					JOptionPane optionPane = new JOptionPane("Saving file, please wait.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
					JDialog dialog = optionPane.createDialog("Saving");

					dialog.setModal(false);
					dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					dialog.setVisible(true);

					// Version should always be first
					obj.addProperty("version", saveversion);
					obj.addProperty("time", e.time);

					obj.addProperty("gui_paused", e.opts.gui_paused.isSelected());
					obj.addProperty("gui_tooltip", e.opts.gui_tooltip.isSelected());
					obj.addProperty("gui_text_bg", e.opts.gui_text_bg.isSelected());
					obj.addProperty("gui_view", e.opts.gui_view.getSelectedIndex());
					obj.addProperty("gui_view_vec", e.opts.gui_view_vec.getSelectedIndex());
					obj.addProperty("gui_view_vec_mode", e.opts.gui_view_vec_mode.getSelectedIndex());
					obj.addProperty("gui_simspeed", e.opts.gui_simspeed.getValue());
					obj.addProperty("gui_simspeed_2", e.opts.gui_simspeed_2.getValue());
					obj.addProperty("gui_brightness", e.opts.gui_brightness.getValue());
					obj.addProperty("gui_brightness_vec", e.opts.gui_brightness_vec.getValue());
					obj.addProperty("gui_elem_colors", e.opts.gui_elem_colors.isSelected());
					obj.addProperty("gui_bc", e.opts.gui_bc.getSelectedIndex());
					obj.addProperty("description", e.opts.textPane.getText());
					obj.addProperty("gui_3d_view", e.opts.gui_3d_view.getSelectedIndex());
					obj.addProperty("gui_zslice", e.opts.gui_slice.getValue());
					obj.addProperty("pitch", e.renderer.pitch);
					obj.addProperty("yaw", e.renderer.yaw);
					obj.addProperty("zoom", e.renderer.scale);

					obj.add("ex", gson.toJsonTree(e.Ex));
					obj.add("ey", gson.toJsonTree(e.Ey));
					obj.add("ez", gson.toJsonTree(e.Ez));
					obj.add("hx", gson.toJsonTree(e.Hx));
					obj.add("hy", gson.toJsonTree(e.Hy));
					obj.add("hz", gson.toJsonTree(e.Hz));
					obj.add("bx", gson.toJsonTree(e.Bx));
					obj.add("by", gson.toJsonTree(e.By));
					obj.add("bz", gson.toJsonTree(e.Bz));

					obj.add("rho_c", gson.toJsonTree(e.rho_abs));
					obj.add("rho_n", gson.toJsonTree(e.rho_n));
					obj.add("rho_p", gson.toJsonTree(e.rho_p));
					obj.add("rho_back", gson.toJsonTree(e.rho_back));
					obj.add("rho_free", gson.toJsonTree(e.rho_free));

					obj.add("jx_c", gson.toJsonTree(e.Jx_abs));
					obj.add("jy_c", gson.toJsonTree(e.Jy_abs));
					obj.add("jz_c", gson.toJsonTree(e.Jz_abs));
					obj.add("jx_n", gson.toJsonTree(e.Jx_n));
					obj.add("jy_n", gson.toJsonTree(e.Jy_n));
					obj.add("jz_n", gson.toJsonTree(e.Jz_n));
					obj.add("jx_p", gson.toJsonTree(e.Jx_p));
					obj.add("jy_p", gson.toJsonTree(e.Jy_p));
					obj.add("jz_p", gson.toJsonTree(e.Jz_p));

					obj.add("materials", gson.toJsonTree(e.materials));

					obj.add("voltageprobes", gson.toJsonTree(e.voltageprobes.toArray(new VoltageProbe[e.voltageprobes.size()])));
					obj.add("currentprobes", gson.toJsonTree(e.currentprobes.toArray(new CurrentProbe[e.currentprobes.size()])));
					obj.add("ground", gson.toJsonTree(e.ground));

					String json = gson.toJson(obj);
					fstr.print(json);
					fstr.flush();
					fstr.close();

					dialog.dispose();
					e.opts.setTitle(e.sim_name + " - " + outfile.getName());
				} catch (FileNotFoundException e) {
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				return;
			} finally {
				e.rwLock.writeLock().unlock();
			}
		});
	}
}
