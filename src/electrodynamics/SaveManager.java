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
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import electrodynamics.Renderer.Perspective;
import electrodynamics.Renderer.RenderMode;
import electrodynamics.Renderer.ScalarMode;
import electrodynamics.Renderer.ScalarView;
import electrodynamics.Renderer.Stereo;
import electrodynamics.Renderer.VectorMode;
import electrodynamics.Renderer.VectorView;
import electrodynamics.Simulation.BoundaryCondition;
import electrodynamics.probe.ChargeProbe;
import electrodynamics.probe.CurrentProbe;
import electrodynamics.probe.FluxProbe;
import electrodynamics.probe.Ground;
import electrodynamics.probe.Probe;
import electrodynamics.probe.VoltageProbe;

public class SaveManager {
	Simulation e;
	
	/* Saving and loading */

	public static File infile;
	public static File outfile;
	public static File currentfile;
	int saveversion = 3;
	public String fileextension = ".semisim3d";
	Path startingpath;
	JDialog dialog;
	
	public SaveManager(Simulation e) {
		this.e = e;
		startingpath = SemiSim.getUserFile("simulations").toPath();
	}
	

	public void readFile()
	{
		SwingUtilities.invokeLater(() -> {
			File testfile = startingpath.toFile();
			if (!testfile.canRead()) {
				JOptionPane.showMessageDialog(e.opts,
				"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
			}

			JFileChooser fd = new JFileChooser(startingpath.toFile());
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
			startingpath = fd.getCurrentDirectory().toPath();

			if (result == JFileChooser.APPROVE_OPTION)
				infile = fd.getSelectedFile();
			else
				infile = null;
			
			readfile(infile);
		});
	}

	public void readfile(File infile) {
		readfile(infile, null);
	}

	@SuppressWarnings("unchecked")
	public void readfile(File infile, Runnable callback) {
		JOptionPane optionPane = new JOptionPane("Loading file, please wait.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
		dialog = optionPane.createDialog("Loading");
		dialog.setModal(false);
		dialog.setVisible(true);

		SwingUtilities.invokeLater(() -> {
			e.rwLock.writeLock().lock();
			try {
				if (infile == null || !infile.exists()) return;

				System.out.println("Attempting to load " + infile.getAbsolutePath());

				if (e.controls.changesmade) {
					String[] options = {"Yes", "No"};
					int result = JOptionPane.showOptionDialog(e.opts, "There are unsaved changes. Do you still wish to open this file?", "Message", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
					if (result != JOptionPane.OK_OPTION)
						return;
				}
				try {
					JsonReader fstr = new JsonReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(infile))));
					fstr.setStrictness(Strictness.LENIENT);
					Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
					Gson gson_probe = new GsonBuilder().serializeSpecialFloatingPointValues().registerTypeHierarchyAdapter(Probe.class, new ProbeAdapter()).create();

					fstr.beginObject();

					assertNextObject(fstr, "version");
					int version = fstr.nextInt();

					if (version > saveversion) {
						fstr.close();
						throw new IllegalArgumentException("The file was created in a newer version of SemiSim.");
					}

					if (version == saveversion) {

						setDefaults();
						assertNextObject(fstr, "header");
						fstr.beginObject();
						while (fstr.hasNext()) {
							String name = fstr.nextName();
							switch (name){
							case "nx": e.default_resolution_x = fstr.nextInt(); break;
							case "ny": e.default_resolution_y = fstr.nextInt(); break;
							case "nz": e.default_resolution_z = fstr.nextInt(); break;
							case "ds": e.ds = fstr.nextDouble(); break;
							case "time": e.time = fstr.nextDouble(); break;
							case "phase": e.AC_phase = fstr.nextDouble(); break;
							case "description": e.description = fstr.nextString(); break;

							case "gui_zslice": e.opts.gui_slice.setValue(fstr.nextInt()); break;
							case "pitch": e.renderer.pitch = (float)(fstr.nextDouble()); break;
							case "yaw": e.renderer.yaw = (float)(fstr.nextDouble()); break;
							case "zoom": e.renderer.scale = (float)(fstr.nextDouble()); break;
							case "cam_x": e.renderer.cam_x = (float)(fstr.nextDouble()); break;
							case "cam_y": e.renderer.cam_y = (float)(fstr.nextDouble()); break;
							case "cam_z": e.renderer.cam_z = (float)(fstr.nextDouble()); break;
							case "parallax": e.opts.gui_parallax.setValue(fstr.nextInt()); break;
							case "rotate": e.opts.gui_rotate.setSelected(fstr.nextBoolean()); break;

							case "scalarview": e.controls.scalarview.setOption(gson.fromJson(fstr, ScalarView.class)); break;
							case "vectorview": e.controls.vectorview.setOption(gson.fromJson(fstr, VectorView.class)); break;
							case "scalarmode": e.controls.scalarmode.setOption(gson.fromJson(fstr, ScalarMode.class)); break;
							case "vectormode": e.controls.vectormode.setOption(gson.fromJson(fstr, VectorMode.class)); break;
							case "perspective": e.controls.perspective.setOption(gson.fromJson(fstr, Perspective.class)); break;
							case "rendermode": e.controls.rendermode.setOption(gson.fromJson(fstr, RenderMode.class)); break;
							case "stereo": e.controls.stereo.setOption(gson.fromJson(fstr, Stereo.class)); break;
							case "gui_bc": e.opts.gui_bc.setSelectedItem(gson.fromJson(fstr, BoundaryCondition.class)); break;

							default:
								if (e.opts.boolean_names.containsKey(name)) e.opts.boolean_names.get(name).setSelected(fstr.nextBoolean());
								else if (e.opts.integer_names.containsKey(name)) e.opts.integer_names.get(name).setValue(fstr.nextInt());
								break;
							}
						}
						fstr.endObject();
						e.opts.setRedundantOptions();
						e.lock_resolution = true;
						e.reset(true, null);
						e.lock_resolution = false;

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
							case "materialmap": e.materialmanager.mat_map = (HashMap<Integer, Material>) gson.fromJson(fstr, new TypeToken<HashMap<Integer, Material>>(){}.getType()); break;
							case "last_material_id": e.materialmanager.id_counter = fstr.nextInt(); break;

							case "all_probes": e.probes.addAll((ArrayList<Probe>) gson_probe.fromJson(fstr, new TypeToken<ArrayList<Probe>>() {}.getType())); break;

							case "voltageprobes": e.probes.addAll(Arrays.asList(
									(VoltageProbe[]) gson.fromJson(fstr, VoltageProbe[].class))); break;
							case "currentprobes": e.probes.addAll(Arrays.asList(
									(CurrentProbe[]) gson.fromJson(fstr, CurrentProbe[].class))); break;
							case "chargeprobes": e.probes.addAll(Arrays.asList(
									(ChargeProbe[]) gson.fromJson(fstr, ChargeProbe[].class))); break;
							case "fluxprobes": e.probes.addAll(Arrays.asList(
									(FluxProbe[]) gson.fromJson(fstr, FluxProbe[].class))); break;
							case "ground": e.probes.add((Ground) gson.fromJson(fstr, Ground.class)); break;

							default: fstr.skipValue(); break; // skip others
							}
						}
						fstr.endObject();

						for (Probe p : e.probes) {
							p.eliminateNulls();
							p.data.fixWeirdIssue();
						}

						e.relabelProbes();

						if (testNextObject(fstr, "advsettings")) {
							fstr.beginObject();
							e.lock_resolution = true;
							e.adv_opts.readAdvancedSettings(gson, fstr, Preset.DEFAULT);
							e.calculateDependentConstants();
							e.calculateMaxTimestep();
							e.lock_resolution = false;
							fstr.endObject();
							e.advsettings_tweaked = true;
						} else {
							e.advsettings_tweaked = false;
						}

						fstr.close();

						e.opts.textPane.setText(e.description);
						e.opts.textPane.setEditable(false);
						e.opts.textPane.setCaretPosition(0);
						e.materialmanager.updateUI();
						if (version < 4) {
							e.initializeAllMaterials();
						}
						e.updateAllMaterials(false);
						e.calcMiscFields(true);
						updateLabels();
						e.controls.undoredo.captureState(e);
					} else {
						throw new IllegalArgumentException("The file was created in an older version of SemiSim.");
					}

					dialog.dispose();
					e.opts.setTitle(SemiSim.name + " - " + infile.getName());
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
				dialog.dispose();
				if (callback != null)
					callback.run();
			}
		});
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
	

	public void writeFile(boolean saveas)
	{
		SwingUtilities.invokeLater(() -> {
			if (!saveas && currentfile != null && currentfile.exists()) {
				writeFile(currentfile);
				e.opts.setTitle(SemiSim.name + " - " + outfile.getName());
				e.controls.changesmade = false;
				return;
			}
			
			File testfile = startingpath.toFile();
			if (!testfile.canWrite()) {
				JOptionPane.showMessageDialog(e.opts,
				"Error: Java does not have access to this folder. Please see instructions to fix this issue.");
			}

			JFileChooser fd = new JFileChooser(startingpath.toFile());
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

				result = JOptionPane.showOptionDialog(e.opts, "A file named " + outfile.getName() + " already exists. Do you wish to overwrite it?", "Message", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (result != JOptionPane.OK_OPTION)
					return;
			}
			
			writeFile(outfile);

			e.opts.setTitle(SemiSim.name + " - " + outfile.getName());
			e.controls.changesmade = false;
			currentfile = outfile;
		});
	}

	public void writeFile(File outfile) {
		writeFile(outfile, null);
	}

	public void writeFile(File outfile, Runnable callback)
	{
		JOptionPane optionPane = new JOptionPane("Saving file, please wait.", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
		dialog = optionPane.createDialog("Saving");

		dialog.setModal(false);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setVisible(true);

		SwingUtilities.invokeLater(() -> {
			e.rwLock.writeLock().lock();
			try {
				for (Probe p : e.probes) {
					p.prepareForSave();
				}
				
				try {
					PrintWriter fstr = new PrintWriter(new GZIPOutputStream(new FileOutputStream(outfile)));

					Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().registerTypeHierarchyAdapter(Probe.class, new ProbeAdapter()).create();

					JsonObject header = new JsonObject();
					header.addProperty("ds", e.ds);
					header.addProperty("nx", e.default_resolution_x);
					header.addProperty("ny", e.default_resolution_y);
					header.addProperty("nz", e.default_resolution_z);
					header.addProperty("time", e.time);
					header.addProperty("phase", e.AC_phase);

					for (String s : e.opts.boolean_names.keySet()) header.addProperty(s, e.opts.boolean_names.get(s).isSelected());
					for (String s : e.opts.integer_names.keySet()) header.addProperty(s, e.opts.integer_names.get(s).getValue());

					header.addProperty("description", e.description);
					header.add("scalarview", gson.toJsonTree(e.controls.scalarview.getOption()));
					header.add("vectorview", gson.toJsonTree(e.controls.vectorview.getOption()));
					header.add("scalarmode", gson.toJsonTree(e.controls.scalarmode.getOption()));
					header.add("vectormode", gson.toJsonTree(e.controls.vectormode.getOption()));
					header.add("perspective", gson.toJsonTree(e.controls.perspective.getOption()));
					header.add("rendermode", gson.toJsonTree(e.controls.rendermode.getOption()));
					header.add("stereo", gson.toJsonTree(e.controls.stereo.getOption()));
					header.add("gui_bc", gson.toJsonTree(e.opts.gui_bc.getSelectedItem()));

					header.addProperty("gui_zslice", e.opts.gui_slice.getValue());
					header.addProperty("pitch", e.renderer.pitch);
					header.addProperty("yaw", e.renderer.yaw);
					header.addProperty("zoom", e.renderer.scale);
					header.addProperty("cam_x", e.renderer.cam_x);
					header.addProperty("cam_y", e.renderer.cam_y);
					header.addProperty("cam_z", e.renderer.cam_z);
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
					data.add("all_probes", gson.toJsonTree(e.probes));
					data.add("materialmap", gson.toJsonTree(e.materialmanager.mat_map));
					data.addProperty("last_material_id", e.materialmanager.id_counter);

					JsonObject advsettings = new JsonObject();
					e.adv_opts.writeAdvancedSettings(gson, advsettings);

					// Version should always be first
					JsonObject save = new JsonObject();
					save.addProperty("version", saveversion);
					save.add("header", header);
					save.add("data", data);
					save.add("advsettings", advsettings);

					String json = gson.toJson(save);

					fstr.print(json);
					fstr.flush();
					fstr.close();

					dialog.dispose();
				} catch (FileNotFoundException e) {
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				return;
			} finally {
				e.rwLock.writeLock().unlock();
				dialog.dispose();
				if (callback != null)
					callback.run();
			}
		});
	}

	public void updateLabels() {
		for (Probe p : e.probes)
			if (p.labelcoord.x == -1) p.calculateDefaultLabelCoords();
	}
	
	public ArrayList<Probe> filterByType(List<Probe> list, Predicate<? super Probe> filter) {
		ArrayList<Probe> listcopy = new ArrayList<Probe>(list);
		listcopy.removeIf(filter);
		return listcopy;
	}
	
	public void setDefaults() {
		e.setDefaultParameters();
		e.opts.setDefaults(e);
	}
}

class ProbeAdapter implements JsonSerializer<Probe>, JsonDeserializer<Probe>{
	
    @Override
    public JsonElement serialize(Probe src, Type typeOfSrc,
            JsonSerializationContext context) {

    	JsonElement elem = new GsonBuilder().serializeSpecialFloatingPointValues().create().toJsonTree(src);
        elem.getAsJsonObject().addProperty("CLASS_NAME", src.getClass().getName());
        return elem;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Probe deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException  {
    	JsonObject jsonObject = json.getAsJsonObject();
        String typeName = jsonObject.get("CLASS_NAME").getAsString();

        try {
            Class<? extends Probe> cls = (Class<? extends Probe>) Class.forName(typeName);
            return new GsonBuilder().serializeSpecialFloatingPointValues().create().fromJson(json, cls);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }
}