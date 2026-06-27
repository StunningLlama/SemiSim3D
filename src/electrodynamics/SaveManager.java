// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Electromagnetic Simulation which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

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

import electrodynamics.Renderer.RenderMode;
import electrodynamics.Renderer.ScalarView;
import electrodynamics.Renderer.VectorMode;
import electrodynamics.Renderer.VectorView;
import electrodynamics.Simulation.BoundaryCondition;

public class SaveManager {
	Simulation e;
	
	/* Saving and loading */

	public static File infile;
	public static File outfile;
	int saveversion = 2;
	String fileextension = ".semisim3d";
	String startingpath = ".";
	
	public SaveManager(Simulation e) {
		this.e = e;
	}
	

	public void readFile()
	{
		SwingUtilities.invokeLater(() -> {
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

			readFile(infile);
		});
	}
	
	public void readFile(File infile) {
		e.rwLock.writeLock().lock();
		try {
			if (infile == null || !infile.exists()) return;
			try {
				JsonReader fstr = new JsonReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(infile))));
				Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

				fstr.beginObject();

				assertNextObject(fstr, "version");
				int version = fstr.nextInt();

				if (version > saveversion) {
					fstr.close();
					throw new IllegalArgumentException("The file was created in a newer version of SemiSim.");
				}

				JOptionPane optionPane = new JOptionPane("Loading file, please wait.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
				JDialog dialog = optionPane.createDialog("Loading");

				dialog.setModal(false);
				dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				dialog.setVisible(true);

				if (version == saveversion) {

					double ds_default = 0.1e-6;
					int nx_default = 32;
					int ny_default = 32;
					int nz_default = 32;
					
					assertNextObject(fstr, "header");
					fstr.beginObject();
					while (fstr.hasNext()) {
						String name = fstr.nextName();
						switch (name){
						case "ds": ds_default = fstr.nextDouble(); break;
						case "nx": nx_default = validateResolution(fstr.nextInt()); break;
						case "ny": ny_default = validateResolution(fstr.nextInt()); break;
						case "nz": nz_default = validateResolution(fstr.nextInt()); break;
						case "time": e.time = fstr.nextDouble(); break;
						case "gui_paused": e.opts.gui_paused.setSelected(fstr.nextBoolean()); break;
						case "gui_tooltip": e.opts.gui_tooltip.setSelected(fstr.nextBoolean()); break;
						case "gui_text_bg": e.opts.gui_text_bg.setSelected(fstr.nextBoolean()); break;
						case "gui_view": e.opts.gui_view.setSelectedItem(gson.fromJson(fstr, ScalarView.class)); break;
						case "gui_view_vec": e.opts.gui_view_vec.setSelectedItem(gson.fromJson(fstr, VectorView.class)); break;
						case "gui_view_vec_mode": e.opts.gui_view_vec_mode.setSelectedItem(gson.fromJson(fstr, VectorMode.class)); break;
						case "gui_simspeed": e.opts.gui_simspeed.setValue(fstr.nextInt()); break;
						case "gui_simspeed_2": e.opts.gui_simspeed_2.setValue(fstr.nextInt()); break;
						case "gui_brightness": e.opts.gui_brightness.setValue(fstr.nextInt()); break;
						case "gui_brightness_vec": e.opts.gui_brightness_vec.setValue(fstr.nextInt()); break;
						case "gui_elem_colors": e.opts.gui_elem_colors.setSelected(fstr.nextBoolean()); break;
						case "gui_bc": e.opts.gui_bc.setSelectedItem(gson.fromJson(fstr, BoundaryCondition.class)); break;
						case "description": e.opts.textPane.setText(fstr.nextString()); break;
						case "gui_3d_view": e.opts.gui_3d_view.setSelectedItem(gson.fromJson(fstr, RenderMode.class)); break;
						case "gui_zslice": e.opts.gui_slice.setValue(fstr.nextInt()); break;
						case "pitch": e.renderer.pitch = (float)(fstr.nextDouble()); break;
						case "yaw": e.renderer.yaw = (float)(fstr.nextDouble()); break;
						case "zoom": e.renderer.scale = (float)(fstr.nextDouble()); break;
						case "parallax": e.opts.gui_parallax.setValue(fstr.nextInt()); break;
						case "rotate": e.opts.gui_rotate.setSelected(fstr.nextBoolean()); break;
						default: fstr.skipValue(); break; // skip others
						}
					}
					fstr.endObject();

					e.initializeGrid(ds_default, nx_default, ny_default, nz_default);
					
					
					e.resetFields(true);

					assertNextObject(fstr, "data");
					fstr.beginObject();
					while (fstr.hasNext()) {
						String name = fstr.nextName();
						switch (name){
						case "ex": e.Ex = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "ey": e.Ey = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "ez": e.Ez = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "hx": e.Hx = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "hy": e.Hy = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "hz": e.Hz = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class)); break;
						case "bx": e.Bx = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class), e.nx, e.ny+1, e.nz+1); break;
						case "by": e.By = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class), e.nx+1, e.ny, e.nz+1); break;
						case "bz": e.Bz = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class), e.nx+1, e.ny+1, e.nz); break;

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
					e.updateAllMaterials();
					e.calcMiscFields(true);
					e.controls.undoredo.captureState(e);
				} /*else if (version == 1) {

					e.initializeGrid(0.1e-6, 32, 32, 32);
					e.resetFields(true);

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
						case "bx": e.Bx = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class), e.nx, e.ny+1, e.nz+1); break;
						case "by": e.By = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class), e.nx+1, e.ny, e.nz+1); break;
						case "bz": e.Bz = validateArraySize((double[][][]) gson.fromJson(fstr, double[][][].class), e.nx+1, e.ny+1, e.nz); break;

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
					e.calcMiscFields(true);
					e.controls.undoredo.captureState(e);
				}*/

				dialog.dispose();
				e.opts.setTitle(e.sim_name + " - " + infile.getName());
			} catch (FileNotFoundException ex) {
				return;
			} catch (IOException | IllegalArgumentException ex) {
				System.out.println("Oh no");
				JOptionPane.showMessageDialog(e.opts,
				"Unable to load file.\n" + ex.getMessage());
				ex.printStackTrace();
				return;
			}
			return;
		} finally {
			e.rwLock.writeLock().unlock();
		}
	}
	
	public void assertNextObject(JsonReader fstr, String name) throws IOException {
		if (!fstr.nextName().equals(name)) {
			fstr.close();
			throw new IllegalArgumentException(name + " not found in file.");
		}
	}
	
	public boolean testNextObject(JsonReader fstr, String name) throws IOException {
		return fstr.nextName().equals(name);
	}

	public int validateResolution(int resolution) throws RuntimeException {
		if(resolution < 4) {
			throw new IllegalArgumentException("Resolution must be a power of 2");
		}
		
		int log2_resolution = (int) Math.round(Math.log(resolution)/Math.log(2));
		if(1 << log2_resolution != resolution) {
			throw new IllegalArgumentException("Resolution must be a power of 2");
		}
		
		return resolution;
	}
	

	public Material[][][] validateArraySize(Material[][][] array) {
		if (array.length != e.nx) throw new IllegalArgumentException("Array size mismatch");
		for (int i = 0; i < array.length; i++) {
			if (array[i].length != e.ny) throw new IllegalArgumentException("Array size mismatch");
			for (int j = 0; j < array[i].length; j++) {
				if (array[i][j].length != e.nz) throw new IllegalArgumentException("Array size mismatch");
			}
		}
		return array;
	}

	public double[][][] validateArraySize(double[][][] array) {
		return validateArraySize(array, e.nx, e.ny, e.nz);
	}
	
	public double[][][] validateArraySize(double[][][] array, int nx, int ny, int nz) throws RuntimeException {
		if (array.length != nx) throw new IllegalArgumentException("Array size mismatch");
		for (int i = 0; i < array.length; i++) {
			if (array[i].length != ny) throw new IllegalArgumentException("Array size mismatch");
			for (int j = 0; j < array[i].length; j++) {
				if (array[i][j].length != nz) throw new IllegalArgumentException("Array size mismatch");
			}
		}
		return array;
	}
	

	public void writeFile()
	{
		SwingUtilities.invokeLater(() -> {
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
				result = JOptionPane.showConfirmDialog(e.opts, "A file with that name already exists. Do you wish to overwrite it?", "Message", JOptionPane.YES_NO_OPTION);
				if (result != JOptionPane.OK_OPTION)
					return;
			}
			
			writeFile(outfile);
		});
	}
	
	public void writeFile(File outfile) {
		e.rwLock.writeLock().lock();
		try {
			try {
				PrintWriter fstr = new PrintWriter(new GZIPOutputStream(new FileOutputStream(outfile)));

				Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

				JOptionPane optionPane = new JOptionPane("Saving file, please wait.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
				JDialog dialog = optionPane.createDialog("Saving");

				dialog.setModal(false);
				dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				dialog.setVisible(true);

				JsonObject header = new JsonObject();
				header.addProperty("ds", e.ds);
				header.addProperty("nx", e.nx);
				header.addProperty("ny", e.ny);
				header.addProperty("nz", e.nz);
				header.addProperty("time", e.time);
				header.addProperty("gui_paused", e.opts.gui_paused.isSelected());
				header.addProperty("gui_tooltip", e.opts.gui_tooltip.isSelected());
				header.addProperty("gui_text_bg", e.opts.gui_text_bg.isSelected());
				header.add("gui_view", gson.toJsonTree(e.opts.gui_view.getSelectedItem()));
				header.add("gui_view_vec", gson.toJsonTree(e.opts.gui_view_vec.getSelectedItem()));
				header.add("gui_view_vec_mode", gson.toJsonTree(e.opts.gui_view_vec_mode.getSelectedItem()));
				header.addProperty("gui_simspeed", e.opts.gui_simspeed.getValue());
				header.addProperty("gui_simspeed_2", e.opts.gui_simspeed_2.getValue());
				header.addProperty("gui_brightness", e.opts.gui_brightness.getValue());
				header.addProperty("gui_brightness_vec", e.opts.gui_brightness_vec.getValue());
				header.addProperty("gui_elem_colors", e.opts.gui_elem_colors.isSelected());
				header.add("gui_bc", gson.toJsonTree(e.opts.gui_bc.getSelectedItem()));
				header.addProperty("description", e.opts.textPane.getText());
				header.add("gui_3d_view", gson.toJsonTree(e.opts.gui_3d_view.getSelectedItem()));
				header.addProperty("gui_zslice", e.opts.gui_slice.getValue());
				header.addProperty("pitch", e.renderer.pitch);
				header.addProperty("yaw", e.renderer.yaw);
				header.addProperty("zoom", e.renderer.scale);
				header.addProperty("parallax", e.opts.gui_parallax.getValue());
				header.addProperty("rotate", e.opts.gui_rotate.isSelected());

				JsonObject data = new JsonObject();
				data.add("ex", gson.toJsonTree(e.Ex));
				data.add("ey", gson.toJsonTree(e.Ey));
				data.add("ez", gson.toJsonTree(e.Ez));
				data.add("hx", gson.toJsonTree(e.Hx));
				data.add("hy", gson.toJsonTree(e.Hy));
				data.add("hz", gson.toJsonTree(e.Hz));
				data.add("bx", gson.toJsonTree(e.Bx));
				data.add("by", gson.toJsonTree(e.By));
				data.add("bz", gson.toJsonTree(e.Bz));

				data.add("rho_c", gson.toJsonTree(e.rho_abs));
				data.add("rho_n", gson.toJsonTree(e.rho_n));
				data.add("rho_p", gson.toJsonTree(e.rho_p));
				data.add("rho_back", gson.toJsonTree(e.rho_back));
				data.add("rho_free", gson.toJsonTree(e.rho_free));

				data.add("jx_c", gson.toJsonTree(e.Jx_abs));
				data.add("jy_c", gson.toJsonTree(e.Jy_abs));
				data.add("jz_c", gson.toJsonTree(e.Jz_abs));
				data.add("jx_n", gson.toJsonTree(e.Jx_n));
				data.add("jy_n", gson.toJsonTree(e.Jy_n));
				data.add("jz_n", gson.toJsonTree(e.Jz_n));
				data.add("jx_p", gson.toJsonTree(e.Jx_p));
				data.add("jy_p", gson.toJsonTree(e.Jy_p));
				data.add("jz_p", gson.toJsonTree(e.Jz_p));

				data.add("materials", gson.toJsonTree(e.materials));

				data.add("voltageprobes", gson.toJsonTree(e.voltageprobes.toArray(new VoltageProbe[e.voltageprobes.size()])));
				data.add("currentprobes", gson.toJsonTree(e.currentprobes.toArray(new CurrentProbe[e.currentprobes.size()])));
				data.add("ground", gson.toJsonTree(e.ground));


				// Version should always be first
				JsonObject save = new JsonObject();
				save.addProperty("version", saveversion);
				save.add("header", header);
				save.add("data", data);
				
				String json = gson.toJson(save);

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
	}
}
