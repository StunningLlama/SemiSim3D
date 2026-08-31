// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

import electrodynamics.Renderer.Text;
import electrodynamics.SemiSim;
import electrodynamics.Simulation;
import electrodynamics.units.Units;
import javax.swing.SpinnerNumberModel;

public class Preferences extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JPanel contentPane2;
	public JPanel panel;
	private JButton btn_apply;
	private JButton btn_reset;
	private JSpinner spinner_imgx;
	private JCheckBox chkbox_potential;
	public JComboBox<Units> gui_units;
	public JTabbedPane tabbedPane;
	
	Simulation e;
	public int current_pref_saveversion = 1;
	public File preferences_file = null;
	private JSpinner spinner_imgy;
	private JLabel lblUndoHistorySize_2;
	private JSpinner spinner_undosize;
	private JSpinner spinner_fps;
	private JLabel lblFontSize;
	private JSpinner spinner_font;
	private JButton btn_cancel;
	private JCheckBox chkbox_matname;
	private JComboBox<Theme> gui_lookfeel;
	private JCheckBox chkbox_voltage;
	private JSpinner spinner_fps_sim;
	private JSpinner spinner_linewidth;
	private JSpinner spinner_arrowdensity;
	private JCheckBox chkbox_antialias;

	public Preferences(Simulation e) {
		setResizable(false);
		this.e = e;
		preferences_file = SemiSim.getUserFile("preferences.json");
		
		setTitle("Preferences");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 581, 307);
		

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(0, 0, 699, 210);

		panel = new JPanel();
		panel.setPreferredSize(new Dimension(581, 240));
		panel.setLayout(null);
		
		contentPane = new JPanel();
		//contentPane.setPreferredSize(new Dimension(581, 250));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		contentPane2 = new JPanel();
		//contentPane2.setPreferredSize(new Dimension(581, 250));
		contentPane2.setBorder(new EmptyBorder(5, 5, 5, 5));

		tabbedPane.addTab("Interface", null, contentPane, null);
		contentPane.setLayout(null);
		
		tabbedPane.addTab("Graphics", null, contentPane2, null);
		contentPane2.setLayout(null);

		panel.add(tabbedPane);
		setContentPane(panel);
		
		JLabel lblNewLabel = new JLabel("Display width [px]");
		lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		lblNewLabel.setBounds(17, 18, 138, 16);
		contentPane2.add(lblNewLabel);
		
		btn_apply = new JButton("Apply");
		btn_apply.setBounds(317, 215, 128, 23);
		panel.add(btn_apply);
		
		btn_reset = new JButton("Reset to defaults");
		btn_reset.setBounds(10, 215, 136, 23);
		panel.add(btn_reset);
		
		spinner_imgx = new JSpinner();
		spinner_imgx.setModel(new SpinnerNumberModel(768, 1, 10000, 1));
		spinner_imgx.setBounds(167, 15, 109, 23);
		contentPane2.add(spinner_imgx);
		
		chkbox_potential = new JCheckBox("Display potential relative to ground");
		chkbox_potential.setHorizontalAlignment(SwingConstants.TRAILING);
		chkbox_potential.setSelected(true);
		chkbox_potential.setHorizontalTextPosition(SwingConstants.LEADING);
		chkbox_potential.setBounds(307, 14, 255, 23);
		contentPane.add(chkbox_potential);
		
		JLabel lblUnitSystem = new JLabel("Unit system");
		lblUnitSystem.setHorizontalAlignment(SwingConstants.TRAILING);
		lblUnitSystem.setBounds(287, 106, 116, 16);
		contentPane.add(lblUnitSystem);
		
		gui_units = new JComboBox<>();
		gui_units.setModel(new DefaultComboBoxModel<>(Units.values()));
		gui_units.setBounds(415, 104, 146, 23);
		contentPane.add(gui_units);
		
		JLabel lblDisplayHeightpx = new JLabel("Display height [px]");
		lblDisplayHeightpx.setHorizontalAlignment(SwingConstants.TRAILING);
		lblDisplayHeightpx.setBounds(17, 46, 138, 16);
		contentPane2.add(lblDisplayHeightpx);
		
		spinner_imgy = new JSpinner();
		spinner_imgy.setModel(new SpinnerNumberModel(768, 1, 10000, 1));
		spinner_imgy.setBounds(167, 43, 109, 23);
		contentPane2.add(spinner_imgy);
		
		JLabel lblUndoHistorySize = new JLabel("Target graphics FPS");
		lblUndoHistorySize.setHorizontalAlignment(SwingConstants.TRAILING);
		lblUndoHistorySize.setBounds(17, 104, 138, 16);
		contentPane2.add(lblUndoHistorySize);
		
		spinner_fps = new JSpinner();
		spinner_fps.setModel(new SpinnerNumberModel(60, 1, 1000, 1));
		spinner_fps.setBounds(167, 101, 109, 23);
		contentPane2.add(spinner_fps);
		
		lblUndoHistorySize_2 = new JLabel("Undo history size");
		lblUndoHistorySize_2.setToolTipText("Warning: uses memory");
		lblUndoHistorySize_2.setHorizontalAlignment(SwingConstants.TRAILING);
		lblUndoHistorySize_2.setBounds(18, 50, 138, 16);
		contentPane.add(lblUndoHistorySize_2);
		
		spinner_undosize = new JSpinner();
		spinner_undosize.setModel(new SpinnerNumberModel(4, 2, 100, 1));
		spinner_undosize.setBounds(168, 47, 109, 23);
		contentPane.add(spinner_undosize);
		
		lblFontSize = new JLabel("Font size [px]");
		lblFontSize.setHorizontalAlignment(SwingConstants.TRAILING);
		lblFontSize.setBounds(17, 75, 138, 16);
		contentPane2.add(lblFontSize);
		
		spinner_font = new JSpinner();
		spinner_font.setModel(new SpinnerNumberModel(12, 1, 100, 1));
		spinner_font.setBounds(167, 72, 109, 23);
		contentPane2.add(spinner_font);
		
		chkbox_antialias = new JCheckBox("Antialiasing");
		chkbox_antialias.setSelected(true);
		chkbox_antialias.setHorizontalTextPosition(SwingConstants.LEADING);
		chkbox_antialias.setHorizontalAlignment(SwingConstants.TRAILING);
		chkbox_antialias.setBounds(316, 75, 255, 23);
		contentPane2.add(chkbox_antialias);
		
		JLabel lblLineWidth = new JLabel("Arrow/Line width");
		lblLineWidth.setHorizontalAlignment(SwingConstants.TRAILING);
		lblLineWidth.setBounds(311, 19, 138, 16);
		contentPane2.add(lblLineWidth);
		
		spinner_linewidth = new JSpinner();
		spinner_linewidth.setModel(new SpinnerNumberModel(3, 0, 100, 1));
		spinner_linewidth.setBounds(461, 16, 109, 23);
		contentPane2.add(spinner_linewidth);
		
		JLabel lblUndoHistorySize_1_1 = new JLabel("Arrow/Line density");
		lblUndoHistorySize_1_1.setHorizontalAlignment(SwingConstants.TRAILING);
		lblUndoHistorySize_1_1.setBounds(311, 48, 138, 16);
		contentPane2.add(lblUndoHistorySize_1_1);
		
		spinner_arrowdensity = new JSpinner();
		spinner_arrowdensity.setModel(new SpinnerNumberModel(10, 0, 100, 1));
		spinner_arrowdensity.setBounds(461, 45, 109, 23);
		contentPane2.add(spinner_arrowdensity);
		
		btn_cancel = new JButton("Cancel");
		btn_cancel.setBounds(445, 215, 128, 23);
		panel.add(btn_cancel);
		
		chkbox_matname = new JCheckBox("Show material name next to cursor");
		chkbox_matname.setHorizontalTextPosition(SwingConstants.LEADING);
		chkbox_matname.setHorizontalAlignment(SwingConstants.TRAILING);
		chkbox_matname.setBounds(307, 43, 255, 23);
		contentPane.add(chkbox_matname);
		
		gui_lookfeel = new JComboBox<>();
		gui_lookfeel.setBounds(415, 133, 146, 23);
		contentPane.add(gui_lookfeel);
		
		JLabel lblUiTheme = new JLabel("UI theme");
		lblUiTheme.setHorizontalAlignment(SwingConstants.TRAILING);
		lblUiTheme.setBounds(287, 136, 116, 16);
		contentPane.add(lblUiTheme);

		gui_lookfeel.setModel(new DefaultComboBoxModel<Theme>(Theme.values));
		
		chkbox_voltage = new JCheckBox("Set voltage instead of EMF");
		chkbox_voltage.setSelected(true);
		chkbox_voltage.setActionCommand("");
		chkbox_voltage.setHorizontalTextPosition(SwingConstants.LEADING);
		chkbox_voltage.setHorizontalAlignment(SwingConstants.TRAILING);
		chkbox_voltage.setBounds(307, 71, 255, 23);
		contentPane.add(chkbox_voltage);
		
		JLabel lblTargetSimulationFps = new JLabel("Target simulation FPS");
		lblTargetSimulationFps.setHorizontalAlignment(SwingConstants.TRAILING);
		lblTargetSimulationFps.setBounds(18, 21, 138, 16);
		contentPane.add(lblTargetSimulationFps);
		
		spinner_fps_sim = new JSpinner();
		spinner_fps_sim.setModel(new SpinnerNumberModel(60, 1, 1000, 1));
		spinner_fps_sim.setBounds(168, 18, 109, 23);
		contentPane.add(spinner_fps_sim);
		
		pack();
		
		resetPrefs();
	}
	
	public void loadPrefs() {
		readfile(preferences_file);
		
		if (!preferences_file.exists()) {
			writeFile(preferences_file);
		}
	}
	
	public void initialize() {
		btn_apply.addActionListener(this);
		btn_reset.addActionListener(this);
		btn_cancel.addActionListener(this);
		
		setLocationRelativeTo(null);
		setVisible(false);
		
		applyWindowPrefs();
	}
	
	public void getPrefs() {
		spinner_imgx.setValue(e.renderer.imgpanel.getWidth());
		spinner_imgy.setValue(e.renderer.imgpanel.getHeight());
		spinner_undosize.setValue(e.controls.undoredo.history_size);
		spinner_fps.setValue((int) e.renderer.targetframerate);
		spinner_fps_sim.setValue((int) e.targetframerate);
		chkbox_potential.setSelected(e.renderer.display_relative_voltage);
		spinner_font.setValue(Text.fontsize);
		gui_units.setSelectedItem(e.units);
		chkbox_matname.setSelected(e.renderer.disp_mat_name);
		chkbox_voltage.setSelected(e.controls.setvoltage);
		gui_lookfeel.setSelectedItem(new Theme(UIManager.getLookAndFeel().getClass().getName()));
		this.chkbox_antialias.setSelected(e.renderer.antialias_3d);
		this.spinner_linewidth.setValue((int)e.renderer.arrow_thickness_3d);
		this.spinner_arrowdensity.setValue((int)Math.round(e.renderer.arrow_density_3d*10));
	}

	public void applyPrefs() {
		e.controls.undoredo.setHistorySize((int) spinner_undosize.getValue());
		
		e.renderer.targetframerate = (double)((int) spinner_fps.getValue());
		e.renderer.frameduration = 1000/e.renderer.targetframerate;
		e.controls.update3dmode = true;

		e.targetframerate = (double)((int) spinner_fps_sim.getValue());
		e.frameduration = 1000/e.targetframerate;
		
		e.renderer.display_relative_voltage = chkbox_potential.isSelected();
		Text.setFontSize((int) spinner_font.getValue());

		e.units = (Units) gui_units.getSelectedItem();
		e.renderer.disp_mat_name = chkbox_matname.isSelected();
		
		e.controls.setvoltage = chkbox_voltage.isSelected();
		
		e.renderer.antialias_3d = this.chkbox_antialias.isSelected();
		e.renderer.arrow_thickness_3d = (int) this.spinner_linewidth.getValue();
		e.renderer.arrow_density_3d = ((int) this.spinner_arrowdensity.getValue())/10f;
	}
	

	public void applyWindowPrefs() {
		SemiSim.changeLookAndFeel(e, ((Theme)gui_lookfeel.getSelectedItem()).info);
		int x = (int)(spinner_imgx.getValue());
		int y = (int)(spinner_imgy.getValue());
		e.renderer.imgpanel.setPreferredSize(new Dimension(x, y));
		e.opts.pack();
	}
	
	public void resetPrefs() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int opt_height = 256*(int)Math.floor(0.8*screenSize.getHeight()/256);
		spinner_imgx.setValue(opt_height);
		spinner_imgy.setValue(opt_height);
		chkbox_potential.setSelected(true);
		gui_units.setSelectedItem(Units.SI);
		spinner_undosize.setValue(5);
		spinner_fps.setValue(60);
		spinner_fps_sim.setValue(60);
		spinner_font.setValue(12);
		gui_lookfeel.setSelectedItem(Theme.default_theme);
		this.chkbox_antialias.setSelected(true);
		this.spinner_linewidth.setValue(3);
		this.spinner_arrowdensity.setValue(10);
	}

	public void readfile(File infile) {
		e.rwLock.writeLock().lock();
		try {
			if (infile == null || !infile.exists()) return;
			
			try {
				JsonReader fstr = new JsonReader(new InputStreamReader(new FileInputStream(infile)));
				Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

				fstr.beginObject();

				assertNextObject(fstr, "version");
				int version = fstr.nextInt();

				if (version > current_pref_saveversion) {
					fstr.close();
					return;
				}

				if (version == current_pref_saveversion) {
					assertNextObject(fstr, "preferences");
					fstr.beginObject();
					while (fstr.hasNext()) {
						String name = fstr.nextName();
						switch (name){
						case "imgsize":
							int size = fstr.nextInt();
							spinner_imgx.setValue(size);
							spinner_imgy.setValue(size);
							break;
						case "imgsize_x": spinner_imgx.setValue(fstr.nextInt()); break;
						case "imgsize_y": spinner_imgy.setValue(fstr.nextInt()); break;
						case "potential": chkbox_potential.setSelected(fstr.nextBoolean()); break;
						case "units": gui_units.setSelectedItem(gson.fromJson(fstr, Units.class)); break;
						case "fps": this.spinner_fps.setValue(fstr.nextInt()); break;
						case "fps_sim": this.spinner_fps_sim.setValue(fstr.nextInt()); break;
						case "fontsize": this.spinner_font.setValue(fstr.nextInt()); break;
						case "undosize": this.spinner_undosize.setValue(fstr.nextInt()); break;
						case "matname": this.chkbox_matname.setSelected(fstr.nextBoolean()); break;
						case "voltage": this.chkbox_voltage.setSelected(fstr.nextBoolean()); break;
						case "theme": gui_lookfeel.setSelectedItem(new Theme(fstr.nextString())); break;
						case "windowstate": e.opts.setExtendedState(fstr.nextInt()); break;
						case "antialias": this.chkbox_antialias.setSelected(fstr.nextBoolean()); break;
						case "linewidth": this.spinner_linewidth.setValue(fstr.nextInt()); break;
						case "arrowdensity": this.spinner_arrowdensity.setValue(fstr.nextInt()); break;
						default: fstr.skipValue();
						}
					}
					fstr.endObject();
					fstr.close();
				}
			} catch (FileNotFoundException ex) {
				return;
			} catch (IOException | IllegalArgumentException ex) {
				return;
			}
			return;
		} finally {
			e.rwLock.writeLock().unlock();
		}
	}

	public void writeFile(File outfile)
	{
		e.rwLock.writeLock().lock();
		try {
			try {
				PrintWriter fstr = new PrintWriter(new FileOutputStream(outfile));

				Gson gson = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

				JsonObject header = new JsonObject();
				header.addProperty("imgsize_x", (int)spinner_imgx.getValue());
				header.addProperty("imgsize_y", (int)spinner_imgy.getValue());
				header.addProperty("potential", chkbox_potential.isSelected());
				header.add("units", gson.toJsonTree((Units) gui_units.getSelectedItem()));
				header.addProperty("fps", (int)spinner_fps.getValue());
				header.addProperty("fps_sim", (int)spinner_fps_sim.getValue());
				header.addProperty("fontsize", (int)spinner_font.getValue());
				header.addProperty("undosize", (int)spinner_undosize.getValue());
				header.addProperty("matname", chkbox_matname.isSelected());
				header.addProperty("voltage", chkbox_voltage.isSelected());
				header.addProperty("theme", ((Theme) gui_lookfeel.getSelectedItem()).info.getClassName());
				header.addProperty("windowstate", e.opts.getExtendedState());
				header.addProperty("antialias", chkbox_antialias.isSelected());
				header.addProperty("linewidth", (int)spinner_linewidth.getValue());
				header.addProperty("arrowdensity", (int)spinner_arrowdensity.getValue());

				// Version should always be first
				JsonObject save = new JsonObject();
				save.addProperty("version", current_pref_saveversion);
				save.add("preferences", header);

				String json = gson.toJson(save);

				fstr.print(json);
				fstr.flush();
				fstr.close();
			} catch (FileNotFoundException e) {
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

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource() == btn_apply) {
			applyPrefs();
			applyWindowPrefs();
			writeFile(preferences_file);
		} else if (ev.getSource() == btn_reset) {
			resetPrefs();
		} else if (ev.getSource() == btn_cancel) {
			this.setVisible(false);
		}
	}
	
	public static class Theme {
		public static Theme[] values;
		public static Theme default_theme;
		public static void initThemes() {
			ArrayList<Theme> themes = new ArrayList<Theme>(UIManager.getInstalledLookAndFeels().length);
			default_theme = null;
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				themes.add(new Theme(info));
				if (info.getClassName() == FlatSolarizedLightIJTheme.class.getName()) default_theme = new Theme(info);
			}
			values = themes.toArray(new Theme[0]);
		}
		
		LookAndFeelInfo info;
		
		public Theme(LookAndFeelInfo info) {
			this.info = info;
		}
		
		public Theme(String classname) {
			for (Theme theme : values) {
				if (theme.info.getClassName().equals(classname)) {
					info = theme.info;
				}
			}
		}
		
		public String toString() {
			return info.getName();
		}
		
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Theme)) return false;
			return info.getClassName().equals(((Theme) other).info.getClassName());
		}
	}
}