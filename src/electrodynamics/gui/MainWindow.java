// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Electromagnetic Simulation which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.gui;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import electrodynamics.BuildFlags;
import electrodynamics.Controls;
import electrodynamics.Controls.Brush;
import electrodynamics.Controls.BrushShape;
import electrodynamics.Controls.CustProbeType;
import electrodynamics.MaterialType;
import electrodynamics.Renderer.Perspective;
import electrodynamics.Renderer.RenderMode;
import electrodynamics.Renderer.ScalarMode;
import electrodynamics.Renderer.ScalarView;
import electrodynamics.Renderer.Stereo;
import electrodynamics.Renderer.VectorMode;
import electrodynamics.Renderer.VectorView;
import electrodynamics.SemiSim;
import electrodynamics.Simulation;
import electrodynamics.Simulation.BoundaryCondition;
import electrodynamics.util.OctahedralAction.OctahedralGenerator;

public class MainWindow extends JFrame implements ComponentListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5756219569007074449L;
	
	Simulation e;
	public HashMap<String, AbstractButton> boolean_names = new HashMap<String, AbstractButton>();
	public HashMap<String, Adjustable> integer_names = new HashMap<String, Adjustable>();
	public List<JMenuItem> clipboardbuttons = new ArrayList<JMenuItem>();
	
	public JPanel contentPane;
	public JButton gui_reset;
	public JScrollBar gui_simspeed;
	public JScrollBar gui_brightness;
	public JScrollBar gui_brushsize;
	public JScrollBar gui_slice;
	public JCheckBox gui_paused;
	public JPanel panel;
	public JLabel gui_parameter2_text;
	public JScrollBar gui_parameter2;
	public JLabel lblVectorBrightness;
	public JScrollBar gui_brightness_vec;
	public JScrollBar gui_simspeed_2;
	public JComboBox gui_brush_1;
	public JScrollBar gui_parameter3;
	public JTextArea textPane;
	public JScrollPane scrollPane;
	public JLabel gui_parameter1_text;
	public JLabel gui_parameter3_text;
	public JLabel gui_stepslbl;
	public JLabel gui_stepsizelbl;
	public JCheckBox gui_brush_highlight;
	public JComboBox gui_material;
	public JComboBox gui_brush;
	public JComboBox gui_bc;
	public JLabel lblBrushSize;
	public JLabel gui_slicelabel;
	public JCheckBox gui_rotate;
	public JScrollBar gui_parallax;
	public JLabel gui_parallaxlabel;
	public JScrollBar gui_parameter1;
	

	public JMenuBar menuBar;
	public JMenu menu_examples;
	public JMenu menu_tools;
	public JMenu menu_view;
	public JMenu menu_graphics;
	public JMenu menu_help2;
	public JMenu menu_edit;
	public JMenu menu_file;
	public JMenuItem menu_open;
	public JMenuItem menu_saveas;
	public JMenuItem menu_about;
	public JMenuItem menu_help3;
	public JMenuItem menu_help;
	public JMenuItem menu_cut;
	public JMenuItem menu_copy;
	public JMenuItem menu_paste;
	public JMenuItem menu_undo;
	public JMenuItem menu_redo;
	public JMenuItem menu_editdesc;
	public JMenuItem menu_new;
	public JMenuItem menu_pref;
	public JMenuItem menu_selectall;
	public JMenuItem menu_deselectall;
	public JMenuItem menu_save;
	public JMenuItem menu_exit;
	public JMenuItem menu_github;
	public JMenuItem menu_report;
	public JMenuItem menu_advancedsettings;
	public JMenuItem menu_cust_material;
	public JMenuItem menu_view_materials;
	public JMenuItem menu_browse;
	public JMenuItem menu_probearrows;
	private JMenuItem menu_workshop;
	private JMenuItem menu_load_workshop;
	public JSeparator separator_3;
	private JSeparator separator_1;
	public JSeparator separator_2;
	private JSeparator separator_6;
	public CustJCheckBoxMenuItem menu_hide_carriers_metal;
	public CustJCheckBoxMenuItem menu_carrier_diffusion;
	public CustJCheckBoxMenuItem menu_gen_recomb;
	public CustJCheckBoxMenuItem menu_materialname;
	public CustJCheckBoxMenuItem menu_interface;
	public CustJCheckBoxMenuItem menu_tooltip;
	public CustJCheckBoxMenuItem menu_text_bg;
	public CustJCheckBoxMenuItem menu_elem_colors;
	public CustJCheckBoxMenuItem menu_borders;
	public CustJCheckBoxMenuItem menu_carriers;
	public CustJCheckBoxMenuItem menu_probes;
	public CustJCheckBoxMenuItem menu_time;
	public CustJCheckBoxMenuItem menu_colormap;
	public CustJCheckBoxMenuItem menu_axes;
	public CustJCheckBoxMenuItem menu_debug;

	public JScrollBar gui_plotinterval;
	public JLabel gui_plotinterval_text;
	public JLabel gui_carrierlbl;
	public JScrollBar gui_carrier_density;
	public JCheckBox gui_carriers;
	public JLabel gui_light_text;
	public JScrollBar gui_light;
	public JComboBox<CustProbeType> gui_probetype;
	public HashMap<Brush, IconButton> toolbuttons = new HashMap<Brush, IconButton>();
	public JLabel gui_slicelabel_l;
	public JScrollBar gui_slice_l;
	public JScrollBar gui_slice_h;

	JPanel panel_3;
	JPanel panel_4;
	public JLabel gui_slicelabel_h;

	/**
	 * Create the frame.
	 */
	public MainWindow(Simulation e) {
		this.e = e;
		setTitle("Brandon's semiconductor simulator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 589, 830);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		menu_file = new JMenu("File");
		menuBar.add(menu_file);

		menu_new = new JMenuItem("New simulation...");
		menu_new.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		menu_file.add(menu_new);

		menu_open = new JMenuItem("Open file...");
		menu_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		menu_file.add(menu_open);

		menu_save = new JMenuItem("Save");
		menu_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		menu_file.add(menu_save);

		menu_saveas = new JMenuItem("Save as...");
		menu_saveas.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		menu_file.add(menu_saveas);

		menu_editdesc = new JMenuItem("Edit description...");
		menu_file.add(menu_editdesc);

		menu_pref = new JMenuItem("Preferences");
		menu_file.add(menu_pref);

		separator_1 = new JSeparator();
		menu_file.add(separator_1);

		menu_load_workshop = new JMenuItem("Load workshop item...");
		menu_file.add(menu_load_workshop);

		menu_workshop = new JMenuItem("Upload to workshop...");
		menu_file.add(menu_workshop);

		separator_6 = new JSeparator();
		menu_file.add(separator_6);

		menu_exit = new JMenuItem("Exit");
		menu_file.add(menu_exit);

		menu_edit = new JMenu("Edit");
		menuBar.add(menu_edit);

		menu_undo = new JMenuItem("Undo");
		menu_undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		menu_edit.add(menu_undo);

		menu_redo = new JMenuItem("Redo");
		menu_redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		menu_edit.add(menu_redo);

		JSeparator separator = new JSeparator();
		menu_edit.add(separator);

		menu_cut = new JMenuItem("Cut");
		menu_cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
		menu_edit.add(menu_cut);

		menu_copy = new JMenuItem("Copy");
		menu_copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		menu_edit.add(menu_copy);

		menu_paste = new JMenuItem("Paste");
		menu_paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		menu_edit.add(menu_paste);
		
		for (OctahedralGenerator action : OctahedralGenerator.values()) {
			JMenuItem oct = new JMenuItem(action.toString());
			oct.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent ev) {
					e.controls.clipboard_action = action;
				}
			});
			menu_edit.add(oct);
			clipboardbuttons.add(oct);
		}

		separator_3 = new JSeparator();
		menu_edit.add(separator_3);

		menu_selectall = new JMenuItem("Select all");
		menu_selectall.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
		menu_edit.add(menu_selectall);

		menu_deselectall = new JMenuItem("Deselect all");
		menu_deselectall.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK));
		menu_edit.add(menu_deselectall);

		separator_2 = new JSeparator();
		menu_edit.add(separator_2);

		menu_advancedsettings = new JMenuItem("Simulation settings");
		menu_edit.add(menu_advancedsettings);

		menu_cust_material = new JMenuItem("Material editor");
		menu_edit.add(menu_cust_material);

		menu_view_materials = new JMenuItem("Material property viewer");
		menu_edit.add(menu_view_materials);

		menu_tools = new JMenu("Tools");
		menuBar.add(menu_tools);

		menu_view = new JMenu("View");
		menuBar.add(menu_view);

		menu_graphics = new JMenu("Graphics");
		menuBar.add(menu_graphics);

		menu_interface = new CustJCheckBoxMenuItem("Display interface");
		menu_interface.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0));
		menu_interface.setSelected(true);
		menu_graphics.add(menu_interface);

		menu_time = new CustJCheckBoxMenuItem("Show time");
		menu_time.setSelected(true);
		menu_graphics.add(menu_time);

		menu_materialname = new CustJCheckBoxMenuItem("Show material name");
		menu_materialname.setSelected(true);
		menu_graphics.add(menu_materialname);

		menu_tooltip = new CustJCheckBoxMenuItem("Show simulation variables");
		menu_tooltip.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
		menu_graphics.add(menu_tooltip);

		menu_probes = new CustJCheckBoxMenuItem("Show probes");
		menu_probes.setSelected(true);
		menu_graphics.add(menu_probes);

		menu_elem_colors = new CustJCheckBoxMenuItem("Show material colors");
		menu_elem_colors.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));
		menu_elem_colors.setSelected(true);
		menu_graphics.add(menu_elem_colors);

		menu_borders = new CustJCheckBoxMenuItem("Show material borders");
		menu_borders.setSelected(true);
		menu_graphics.add(menu_borders);

		menu_text_bg = new CustJCheckBoxMenuItem("Show text background");
		menu_text_bg.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, 0));
		menu_text_bg.setSelected(true);
		menu_graphics.add(menu_text_bg);

		menu_colormap = new CustJCheckBoxMenuItem("Show color scale");
		menu_colormap.setSelected(true);
		menu_graphics.add(menu_colormap);

		menu_axes = new CustJCheckBoxMenuItem("Show axes");
		menu_axes.setSelected(true);
		menu_graphics.add(menu_axes);
		
		menu_probearrows = new CustJCheckBoxMenuItem("Show current probe arrows");
		menu_probearrows.setSelected(true);
		menu_graphics.add(menu_probearrows);

		JSeparator separator_4 = new JSeparator();
		menu_graphics.add(separator_4);

		menu_carriers = new CustJCheckBoxMenuItem("Show charge carriers");
		menu_graphics.add(menu_carriers);

		menu_hide_carriers_metal = new CustJCheckBoxMenuItem("Hide carriers in metal");
		menu_hide_carriers_metal.setSelected(true);
		menu_graphics.add(menu_hide_carriers_metal);

		menu_gen_recomb = new CustJCheckBoxMenuItem("Show generation and recombination");
		menu_gen_recomb.setSelected(true);
		menu_graphics.add(menu_gen_recomb);

		menu_carrier_diffusion = new CustJCheckBoxMenuItem("Show carrier diffusion");
		menu_graphics.add(menu_carrier_diffusion);

		JSeparator separator_5 = new JSeparator();
		menu_graphics.add(separator_5);

		menu_debug = new CustJCheckBoxMenuItem("Debug mode");
		menu_debug.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));
		//menu_graphics.add(menu_debug);

		menu_examples = new JMenu("Examples");
		menuBar.add(menu_examples);
		
		menu_browse = new JMenuItem("Browse all examples");
		menu_examples.add(menu_browse);

		menu_examples.add(new JSeparator());

		menu_help2 = new JMenu("Help");
		menuBar.add(menu_help2);

		menu_help = new JMenuItem("Open manual");
		menu_help2.add(menu_help);

		menu_github = new JMenuItem("Github");
		menu_help2.add(menu_github);

		menu_report = new JMenuItem("Report a bug...");
		menu_help2.add(menu_report);

		menu_about = new JMenuItem("About...");
		menu_help2.add(menu_about);

		menu_help3 = new JMenuItem("Quick reference...");
		menu_help3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		menu_help2.add(menu_help3);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		panel = new JPanel();
		panel.setPreferredSize(new Dimension(380, 200));
		panel.setMinimumSize(new Dimension(200, 200));
		contentPane.add(panel, BorderLayout.EAST);
		panel.setLayout(null);
		
		gui_reset = new JButton("Reset fields");
		gui_reset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		gui_reset.setBounds(10, 6, 171, 23);
		panel.add(gui_reset);
		
		gui_paused = new JCheckBox("Paused");
		gui_paused.setSelected(true);
		gui_paused.setBounds(10, 37, 82, 23);
		panel.add(gui_paused);
		
		gui_brushsize = new JScrollBar();
		gui_brushsize.setMaximum(1010);
		gui_brushsize.setValue(500);
		gui_brushsize.setOrientation(JScrollBar.HORIZONTAL);
		gui_brushsize.setBounds(201, 199, 171, 17);
		panel.add(gui_brushsize);
		
		gui_simspeed = new JScrollBar();
		gui_simspeed.setValue(20);
		gui_simspeed.setBlockIncrement(1);
		gui_simspeed.setMaximum(30);
		gui_simspeed.setOrientation(JScrollBar.HORIZONTAL);
		gui_simspeed.setBounds(11, 95, 171, 17);
		panel.add(gui_simspeed);
		
		gui_brightness = new JScrollBar();
		gui_brightness.setValue(-20);
		gui_brightness.setBlockIncrement(1);
		gui_brightness.setMinimum(-45);
		gui_brightness.setMaximum(45);
		gui_brightness.setOrientation(JScrollBar.HORIZONTAL);
		gui_brightness.setBounds(10, 199, 171, 17);
		panel.add(gui_brightness);
		
		gui_slice = new JScrollBar();
		gui_slice.setMaximum(42);
		gui_slice.setOrientation(JScrollBar.HORIZONTAL);
		gui_slice.setBounds(203, 403, 171, 17);
		panel.add(gui_slice);
		
		gui_brush = new JComboBox();
		gui_brush.setMaximumRowCount(16);
		gui_brush.setModel(new DefaultComboBoxModel(Brush.values()));
		gui_brush.setSelectedIndex(0);
		gui_brush.setBounds(201, 39, 171, 22);
		panel.add(gui_brush);
		
		gui_stepsizelbl = new JLabel("Step size");
		gui_stepsizelbl.setBounds(20, 73, 161, 14);
		panel.add(gui_stepsizelbl);
		
		JLabel label5 = new JLabel("Scalar Brightness");
		label5.setBounds(18, 177, 150, 14);
		panel.add(label5);
		
		lblBrushSize = new JLabel("Brush size");
		lblBrushSize.setBounds(211, 174, 138, 14);
		panel.add(lblBrushSize);
		
		gui_parameter1_text = new JLabel("Meow");
		gui_parameter1_text.setBounds(211, 283, 150, 14);
		panel.add(gui_parameter1_text);
		
		gui_parameter2_text = new JLabel("Direction");
		gui_parameter2_text.setBounds(211, 233, 161, 14);
		panel.add(gui_parameter2_text);
		
		gui_parameter2 = new JScrollBar();
		gui_parameter2.setOrientation(JScrollBar.HORIZONTAL);
		gui_parameter2.setMaximum(34);
		gui_parameter2.setBounds(201, 254, 171, 17);
		panel.add(gui_parameter2);
		
		lblVectorBrightness = new JLabel("Vector field brightness");
		lblVectorBrightness.setBounds(19, 234, 150, 14);
		panel.add(lblVectorBrightness);
		
		gui_brightness_vec = new JScrollBar();
		gui_brightness_vec.setValue(-10);
		gui_brightness_vec.setOrientation(JScrollBar.HORIZONTAL);
		gui_brightness_vec.setMinimum(-45);
		gui_brightness_vec.setMaximum(45);
		gui_brightness_vec.setBlockIncrement(1);
		gui_brightness_vec.setBounds(10, 254, 171, 17);
		panel.add(gui_brightness_vec);
		
		gui_brush_1 = new JComboBox();
		gui_brush_1.setMaximumRowCount(16);
		gui_brush_1.setModel(new DefaultComboBoxModel(BrushShape.values()));
		gui_brush_1.setSelectedIndex(1);
		gui_brush_1.setBounds(201, 106, 171, 22);
		panel.add(gui_brush_1);
		
		gui_stepslbl = new JLabel("Steps/Frame");
		gui_stepslbl.setBounds(20, 124, 144, 14);
		panel.add(gui_stepslbl);
		
		gui_simspeed_2 = new JScrollBar();
		gui_simspeed_2.setMinimum(1);
		gui_simspeed_2.setValue(25);
		gui_simspeed_2.setOrientation(JScrollBar.HORIZONTAL);
		gui_simspeed_2.setMaximum(110);
		gui_simspeed_2.setBlockIncrement(1);
		gui_simspeed_2.setBounds(10, 146, 171, 17);
		panel.add(gui_simspeed_2);
		
		gui_parameter3 = new JScrollBar();
		gui_parameter3.setMinimum(-50);
		gui_parameter3.setOrientation(JScrollBar.HORIZONTAL);
		gui_parameter3.setMaximum(60);
		gui_parameter3.setBlockIncrement(1);
		gui_parameter3.setBounds(202, 253, 171, 17);
		panel.add(gui_parameter3);
		
		gui_parameter3_text = new JLabel("EMF");
		gui_parameter3_text.setBounds(210, 234, 150, 14);
		panel.add(gui_parameter3_text);
		
		scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(20, 434, 341, 319);
		panel.add(scrollPane);
		
		textPane = new JTextArea();
		textPane.setColumns(16);
		textPane.setWrapStyleWord(true);
		textPane.setRows(16);
		textPane.setText("Description of simulation");
		textPane.setFont(new Font("SansSerif", Font.PLAIN, 13));
		textPane.setMargin(new Insets(4, 4, 4, 4));
		textPane.setLineWrap(true);
		textPane.setEditable(false);
		scrollPane.setColumnHeaderView(textPane);
		
		gui_brush_highlight = new JCheckBox("Brush highlight");
		gui_brush_highlight.setSelected(true);
		gui_brush_highlight.setBounds(201, 142, 160, 23);
		panel.add(gui_brush_highlight);
		
		gui_material = new JComboBox();
		gui_material.setMaximumRowCount(16);
		gui_material.setModel(new DefaultComboBoxModel(MaterialType.values()));
		gui_material.setSelectedIndex(0);
		gui_material.setBounds(201, 73, 171, 22);
		panel.add(gui_material);
		
		gui_bc = new JComboBox();
		gui_bc.setModel(new DefaultComboBoxModel(BoundaryCondition.values()));
		gui_bc.setSelectedIndex(0);
		gui_bc.setBounds(201, 6, 171, 22);
		panel.add(gui_bc);
		

		gui_carrierlbl = new JLabel("Charge carrier density");
		gui_carrierlbl.setBounds(20, 328, 150, 14);
		panel.add(gui_carrierlbl);

		gui_carrier_density = new JScrollBar();
		gui_carrier_density.setValue(-45);
		gui_carrier_density.setOrientation(JScrollBar.HORIZONTAL);
		gui_carrier_density.setMinimum(-45);
		gui_carrier_density.setMaximum(45);
		gui_carrier_density.setBlockIncrement(1);
		gui_carrier_density.setBounds(10, 349, 171, 17);
		panel.add(gui_carrier_density);

		gui_carriers = new JCheckBox("Show charge carriers");
		gui_carriers.setSelected(false);
		gui_carriers.setBounds(10, 290, 171, 23);
		panel.add(gui_carriers);
		
		gui_slicelabel = new JLabel("Slice");
		gui_slicelabel.setBounds(213, 380, 150, 14);
		panel.add(gui_slicelabel);
		
		gui_rotate = new JCheckBox("Rotate view");
		gui_rotate.setBounds(90, 36, 112, 23);
		panel.add(gui_rotate);
		
		gui_parallax = new JScrollBar();
		gui_parallax.setMaximum(55);
		gui_parallax.setValue(7);
		gui_parallax.setOrientation(JScrollBar.HORIZONTAL);
		gui_parallax.setBounds(202, 351, 171, 17);
		panel.add(gui_parallax);
		
		gui_parallaxlabel = new JLabel("Parallax");
		gui_parallaxlabel.setBounds(212, 328, 150, 14);
		panel.add(gui_parallaxlabel);
		
		gui_parameter1 = new JScrollBar();
		gui_parameter1.setValue(-10);
		gui_parameter1.setOrientation(JScrollBar.HORIZONTAL);
		gui_parameter1.setMinimum(-45);
		gui_parameter1.setMaximum(45);
		gui_parameter1.setBlockIncrement(1);
		gui_parameter1.setBounds(201, 304, 171, 17);
		panel.add(gui_parameter1);
		

		gui_plotinterval_text = new JLabel("Probe plot interval");
		gui_plotinterval_text.setBounds(207, 233, 167, 14);
		panel.add(gui_plotinterval_text);

		gui_plotinterval = new JScrollBar();
		gui_plotinterval.setValue(10);
		gui_plotinterval.setMinimum(1);
		gui_plotinterval.setOrientation(JScrollBar.HORIZONTAL);
		gui_plotinterval.setMaximum(60);
		gui_plotinterval.setBounds(203, 254, 171, 17);
		panel.add(gui_plotinterval);

		gui_light_text = new JLabel("Light");
		gui_light_text.setBounds(203, 233, 172, 14);
		panel.add(gui_light_text);

		gui_light = new JScrollBar();
		gui_light.setMinimum(-15);
		gui_light.setOrientation(JScrollBar.HORIZONTAL);
		gui_light.setMaximum(25);
		gui_light.setBounds(203, 252, 171, 17);
		panel.add(gui_light);
		
		gui_probetype = new JComboBox<CustProbeType>();
		gui_probetype.setModel(new DefaultComboBoxModel<>(CustProbeType.values()));
		gui_probetype.setMaximumRowCount(16);
		gui_probetype.setBounds(201, 106, 171, 22);
		panel.add(gui_probetype);
		
		gui_slicelabel_l = new JLabel("Lower cut");
		gui_slicelabel_l.setBounds(20, 380, 150, 14);
		panel.add(gui_slicelabel_l);
		
		gui_slice_l = new JScrollBar();
		gui_slice_l.setOrientation(JScrollBar.HORIZONTAL);
		gui_slice_l.setMaximum(42);
		gui_slice_l.setBounds(10, 403, 171, 17);
		panel.add(gui_slice_l);
		
		gui_slicelabel_h = new JLabel("Upper cut");
		gui_slicelabel_h.setBounds(213, 380, 150, 14);
		panel.add(gui_slicelabel_h);
		
		gui_slice_h = new JScrollBar();
		gui_slice_h.setOrientation(JScrollBar.HORIZONTAL);
		gui_slice_h.setMaximum(42);
		gui_slice_h.setValue(32);
		gui_slice_h.setBounds(203, 403, 171, 17);
		panel.add(gui_slice_h);
		panel_3 = new JPanel();
		//panel_3.setBorder(new EmptyBorder(0, 0, 0, 0));
		contentPane.add(panel_3, BorderLayout.CENTER);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		panel_4 = new JPanel();
		//panel_4.setBorder(new EmptyBorder(0, 0, 0, 0));
		panel_3.add(panel_4, BorderLayout.NORTH);
		panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.X_AXIS));
		panel_4.setBorder(new EmptyBorder(2, 0, 2, 0));
	}
	
	public void listSettings() {
		boolean_names.put("gui_paused", gui_paused);
		boolean_names.put("gui_tooltip", menu_tooltip);
		boolean_names.put("gui_text_bg", menu_text_bg);
		boolean_names.put("gui_elem_colors", menu_elem_colors);
		boolean_names.put("gui_interface", menu_interface);
		boolean_names.put("gui_borders", menu_borders);
		boolean_names.put("show_material", menu_materialname);
		boolean_names.put("show_probes", menu_probes);
		boolean_names.put("show_time", menu_time);
		boolean_names.put("show_colormap", menu_colormap);
		boolean_names.put("show_axes", menu_axes);
		boolean_names.put("gui_carriers", gui_carriers);
		boolean_names.put("gui_hide_carriers_metal", menu_hide_carriers_metal);
		boolean_names.put("show_carrier_diffusion", menu_carrier_diffusion);
		boolean_names.put("show_gen_recomb", menu_gen_recomb);

		integer_names.put("gui_simspeed", gui_simspeed);
		integer_names.put("gui_simspeed_2", gui_simspeed_2);
		integer_names.put("gui_brightness", gui_brightness);
		integer_names.put("gui_brightness_vec", gui_brightness_vec);
		integer_names.put("gui_carrier_density", gui_carrier_density);
		integer_names.put("gui_parameter1", gui_parameter1);
	}
	
	public void setDefaults(Simulation e) {
		setTitle(SemiSim.name);

		menu_tooltip.setSelected(false);
		menu_text_bg.setSelected(true);
		menu_elem_colors.setSelected(true);
		menu_interface.setSelected(true);
		menu_borders.setSelected(true);
		menu_materialname.setSelected(true);
		menu_probes.setSelected(true);
		menu_time.setSelected(true);
		menu_colormap.setSelected(false);
		menu_axes.setSelected(true);
		gui_carriers.setSelected(false);
		menu_hide_carriers_metal.setSelected(true);
		menu_carrier_diffusion.setSelected(false);
		menu_gen_recomb.setSelected(true);

		gui_simspeed.setValue(20);
		gui_simspeed_2.setValue(25);
		gui_brightness.setValue(0);
		gui_brightness_vec.setValue(0);
		gui_carrier_density.setValue(10);
		gui_parameter1.setValue(0);
		
		gui_paused.setSelected(false);
		gui_brush.setSelectedItem(Brush.INTERACT);
		gui_brushsize.setValue(10);
		gui_brush_1.setSelectedIndex(1);
		//gui_material.setSelectedIndex(0);

		e.controls.brushes.setOption(Controls.Brush.INTERACT);
		e.controls.scalarview.setOption(ScalarView.CHARGE);
		e.controls.scalarmode.setOption(ScalarMode.COLORS);
		e.controls.vectorview.setOption(VectorView.E_FIELD);
		e.controls.vectormode.setOption(VectorMode.ARROWS);
		e.controls.perspective.setOption(Perspective.ORTHO);
		e.controls.stereo.setOption(Stereo.DISABLED);
		e.controls.rendermode.setOption(RenderMode.NORMAL);
		
		setRedundantOptions();
	}
	
	public void setRedundantOptions() {
		menu_carriers.setSelected(gui_carriers.isSelected());
	}
	
	public void initialize() {
		//TODO
		listSettings();
		
		e.canvas.addMouseListener(e.controls);
		e.canvas.addMouseMotionListener(e.controls);
		e.canvas.addMouseWheelListener(e.controls);
		e.canvas.addKeyListener(e.controls);
		this.getRootPane().addComponentListener(this);

		gui_reset.addActionListener(e.controls);
		gui_brush.addActionListener(e.controls);
		gui_material.addActionListener(e.controls);
		gui_carriers.addActionListener(e.controls);
		menu_advancedsettings.addActionListener(e.controls);
		menu_open.addActionListener(e.controls);
		menu_saveas.addActionListener(e.controls);
		menu_save.addActionListener(e.controls);
		menu_about.addActionListener(e.controls);
		menu_help3.addActionListener(e.controls);
		menu_help.addActionListener(e.controls);
		menu_undo.addActionListener(e.controls);
		menu_redo.addActionListener(e.controls);
		menu_saveas.addActionListener(e.controls);
		menu_cut.addActionListener(e.controls);
		menu_copy.addActionListener(e.controls);
		menu_paste.addActionListener(e.controls);
		menu_editdesc.addActionListener(e.controls);
		menu_new.addActionListener(e.controls);
		menu_selectall.addActionListener(e.controls);
		menu_deselectall.addActionListener(e.controls);
		menu_pref.addActionListener(e.controls);
		menu_carriers.addActionListener(e.controls);
		menu_debug.addActionListener(e.controls);
		menu_exit.addActionListener(e.controls);
		menu_github.addActionListener(e.controls);
		menu_report.addActionListener(e.controls);
		menu_cust_material.addActionListener(e.controls);
		menu_view_materials.addActionListener(e.controls);
		menu_workshop.addActionListener(e.controls);
		menu_load_workshop.addActionListener(e.controls);
		gui_slice.addAdjustmentListener(e.controls);
		gui_slice_l.addAdjustmentListener(e.controls);
		gui_slice_h.addAdjustmentListener(e.controls);
		gui_parallax.addAdjustmentListener(e.controls);
		menu_browse.addActionListener(e.controls);
		
		gui_reset				.setActionCommand("gui_reset");
		gui_brush				.setActionCommand("gui_brush");
		gui_material			.setActionCommand("gui_material");
		gui_carriers			.setActionCommand("gui_carriers");
		menu_advancedsettings	.setActionCommand("menu_advancedsettings");
		menu_open				.setActionCommand("menu_open");
		menu_saveas				.setActionCommand("menu_saveas");
		menu_save				.setActionCommand("menu_save");
		menu_about				.setActionCommand("menu_about");
		menu_help3				.setActionCommand("menu_help3");
		menu_help				.setActionCommand("menu_help");
		menu_undo				.setActionCommand("menu_undo");
		menu_redo				.setActionCommand("menu_redo");
		menu_saveas				.setActionCommand("menu_saveas");
		menu_cut				.setActionCommand("menu_cut");
		menu_copy				.setActionCommand("menu_copy");
		menu_paste				.setActionCommand("menu_paste");
		menu_editdesc			.setActionCommand("menu_editdesc");
		menu_new				.setActionCommand("menu_new");
		menu_selectall			.setActionCommand("menu_selectall");
		menu_deselectall		.setActionCommand("menu_deselectall");
		menu_pref				.setActionCommand("menu_pref");
		menu_carriers			.setActionCommand("menu_carriers");
		menu_debug				.setActionCommand("menu_debug");
		menu_exit				.setActionCommand("menu_exit");
		menu_github				.setActionCommand("menu_github");
		menu_report				.setActionCommand("menu_report");
		menu_cust_material		.setActionCommand("menu_cust_material");
		menu_view_materials		.setActionCommand("menu_view_materials");
		menu_workshop			.setActionCommand("menu_workshop");
		menu_load_workshop			.setActionCommand("menu_load_workshop");
		menu_browse				.setActionCommand("menu_browse");

		removeKeyListeners(gui_brush);
		removeKeyListeners(gui_bc);
		removeKeyListeners(gui_material);
		removeKeyListeners(gui_brush_1);
		
		gui_brush.addItemListener(e.controls);
		
		if (!BuildFlags.steam_enabled) {
			menu_file.remove(menu_load_workshop);
			menu_file.remove(menu_workshop);
			menu_file.remove(separator_6);
		}

		e.controls.perspective.initialize(menu_graphics, e.controls, null, () -> new CustJRadioButtonMenuItem());
		menu_graphics.add(new JSeparator());
		e.controls.rendermode.initialize(menu_graphics, e.controls, null, () -> new CustJRadioButtonMenuItem());
		menu_graphics.add(new JSeparator());
		e.controls.slice.initialize(menu_graphics, e.controls, null, () -> new CustJRadioButtonMenuItem());
		menu_graphics.add(new JSeparator());
		e.controls.stereo.initialize(menu_graphics, e.controls, null, () -> new CustJRadioButtonMenuItem());
		
		e.controls.brushes.initialize(menu_tools, e.controls, new Controls.Brush[] {Controls.Brush.DRAW, Controls.Brush.VOLTAGE, Controls.Brush.BANDS}, () -> new JRadioButtonMenuItem());

		e.controls.scalarview.initialize(menu_view, e.controls, null, () -> new CustJRadioButtonMenuItem());
		
		
		
		MenuCheckList<ScalarView, ?> c = e.controls.scalarview;

        JMenu thermo = new JMenu("Thermodynamics");
        c.menu.add(thermo);
        JMenu carrier = new JMenu("Carrier dynamics");
        c.menu.add(carrier);
        JMenu recomb = new JMenu("Recombination");
        c.menu.add(recomb);
		c.removeOption(ScalarView.ELECTRON_POTENTIAL); thermo.add(c.getButton(ScalarView.ELECTRON_POTENTIAL));
		c.removeOption(ScalarView.ELECTRON_VOLTAGE); thermo.add(c.getButton(ScalarView.ELECTRON_VOLTAGE));
		c.removeOption(ScalarView.HOLE_POTENTIAL); thermo.add(c.getButton(ScalarView.HOLE_POTENTIAL));
		c.removeOption(ScalarView.HOLE_VOLTAGE); thermo.add(c.getButton(ScalarView.HOLE_VOLTAGE));
		c.removeOption(ScalarView.HEAT); thermo.add(c.getButton(ScalarView.HEAT));
		c.removeOption(ScalarView.ENTROPY); thermo.add(c.getButton(ScalarView.ENTROPY));

		c.removeOption(ScalarView.ELECTRON_DENSITY); carrier.add(c.getButton(ScalarView.ELECTRON_DENSITY));
		c.removeOption(ScalarView.ELECTRON_VEL); carrier.add(c.getButton(ScalarView.ELECTRON_VEL));
		c.removeOption(ScalarView.HOLE_DENSITY); carrier.add(c.getButton(ScalarView.HOLE_DENSITY));
		c.removeOption(ScalarView.HOLE_VEL); carrier.add(c.getButton(ScalarView.HOLE_VEL));

		c.removeOption(ScalarView.RECOMB_RAD); recomb.add(c.getButton(ScalarView.RECOMB_RAD));
		c.removeOption(ScalarView.RECOMB_SRH); recomb.add(c.getButton(ScalarView.RECOMB_SRH));
		c.removeOption(ScalarView.RECOMB_AUGER); recomb.add(c.getButton(ScalarView.RECOMB_AUGER));
		c.removeOption(ScalarView.LIGHT); recomb.add(c.getButton(ScalarView.LIGHT));
		//menu_view.remove();
		
		menu_view.add(new JSeparator());
		e.controls.scalarmode.initialize(menu_view, e.controls, null, () -> new CustJRadioButtonMenuItem());
		menu_view.add(new JSeparator());
		e.controls.vectorview.initialize(menu_view, e.controls, null, () -> new CustJRadioButtonMenuItem());
		

		MenuCheckList<VectorView, ?> v = e.controls.vectorview;
        JMenu dd = new JMenu("Diffusion and drift");
        v.menu.add(dd);

		v.removeOption(VectorView.ELECTRON_DIFFUSION); dd.add(v.getButton(VectorView.ELECTRON_DIFFUSION));
		v.removeOption(VectorView.ELECTRON_DRIFT); dd.add(v.getButton(VectorView.ELECTRON_DRIFT));
		v.removeOption(VectorView.ELECTRON_VELOCITY); dd.add(v.getButton(VectorView.ELECTRON_VELOCITY));
		v.removeOption(VectorView.HOLE_DIFFUSION); dd.add(v.getButton(VectorView.HOLE_DIFFUSION));
		v.removeOption(VectorView.HOLE_DRIFT); dd.add(v.getButton(VectorView.HOLE_DRIFT));
		v.removeOption(VectorView.HOLE_VELOCITY); dd.add(v.getButton(VectorView.HOLE_VELOCITY));
		
		menu_view.add(new JSeparator());
		e.controls.vectormode.initialize(menu_view, e.controls, null, () -> new CustJRadioButtonMenuItem());

		e.controls.brushes.buttonmap.get(Controls.Brush.INTERACT).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0));
		e.controls.brushes.buttonmap.get(Controls.Brush.DRAW).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0));
		e.controls.brushes.buttonmap.get(Controls.Brush.LINE).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0));
		e.controls.brushes.buttonmap.get(Controls.Brush.FILL).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0));
		e.controls.brushes.buttonmap.get(Controls.Brush.SELECT).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, 0));
		e.controls.brushes.buttonmap.get(Controls.Brush.CAMERA).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0));

		e.controls.scalarmode.buttonmap.get(ScalarMode.NONE).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
		e.controls.vectormode.buttonmap.get(VectorMode.NONE).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0));
		
		MenuBuilder.addDirectoryToMenu(menu_examples, SemiSim.getRootFile("examples"), e.savemanager.fileextension, (File f) -> e.savemanager.readfile(f));

		//gui_material.removeItem(MaterialType.ABSORBER);
		if (!BuildFlags.debugging)
			e.controls.scalarview.removeOption(ScalarView.DEBUG);
		
		e.controls.scalarview.removeOption(ScalarView.NONE);
		e.controls.vectorview.removeOption(VectorView.NONE);
		

		menu_graphics.add(new JSeparator());
		menu_graphics.add(menu_debug);

		gui_parameter1.setEnabled(true);
		gui_parameter1.setVisible(true);
		gui_parameter1_text.setEnabled(true);
		gui_parameter1_text.setVisible(true);

		e.controls.addKeyBinds(e.canvas);
		e.controls.addKeyBinds(panel);
		e.controls.addKeyBinds(e.renderer.imgpanel);
		
		InputMap im = (InputMap)UIManager.get("Button.focusInputMap");
		im.put(KeyStroke.getKeyStroke("pressed SPACE"), "none");
		im.put(KeyStroke.getKeyStroke("released SPACE"), "none");
		
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(e.controls);
		//setLocationRelativeTo(null);
		

		panel_3.add(e.renderer.imgpanel, BorderLayout.CENTER);

		e.renderer.set3Dmode();
		
		e.renderer.renderer_left_eye.canvas.addMouseListener(e.controls);
		e.renderer.renderer_left_eye.canvas.addMouseMotionListener(e.controls);
		e.renderer.renderer_left_eye.canvas.addMouseWheelListener(e.controls);
		e.renderer.renderer_left_eye.canvas.addKeyListener(e.controls);

		e.renderer.renderer_right_eye.canvas.addMouseListener(e.controls);
		e.renderer.renderer_right_eye.canvas.addMouseMotionListener(e.controls);
		e.renderer.renderer_right_eye.canvas.addMouseWheelListener(e.controls);
		e.renderer.renderer_right_eye.canvas.addKeyListener(e.controls);
		
		e.renderer.imgpanel.addKeyListener(e.controls);
		
		addToolButton(Brush.INTERACT, 25);
		addToolButton(Brush.DRAW, 25);
		addToolButton(Brush.ERASE, 25);
		addToolButton(Brush.LINE, 25);
		addToolButton(Brush.FILL, 25);
		addToolButton(Brush.RECTANGLE, 25);
		addToolButton(Brush.TEXT, 25);
		addToolButton(Brush.SELECT, 25);
		addToolButton(Brush.CAMERA, 25);
		addToolButton(Brush.ZOOM, 25);
		addToolButton(Brush.PAN, 25);
		
		if (BuildFlags.steam_enabled)
			menu_github.setText("Steam");

		try {
			BufferedImage icon = ImageIO.read(SemiSim.getRootFile("images/icon.png"));
			if (icon != null) {
				setIconImage(icon);
			}
		} catch (IOException e1) {}
		
		setDefaults(e);
	}
	
	public void addToolButton(Brush brush, int size) {
		try {
			Image icon = ImageIO.read(SemiSim.getRootFile("images/icons/" + brush.name() + ".png")).getScaledInstance(size, size, Image.SCALE_SMOOTH);
			if (icon != null) {
				int width = icon.getWidth(null);
				int height = icon.getHeight(null);

				// width and height are of the toolkit image
				BufferedImage bufferedicon = new BufferedImage(width, height, 
				      BufferedImage.TYPE_INT_ARGB);
				Graphics g = bufferedicon.getGraphics();
				g.drawImage(icon, 0, 0, null);
				g.dispose();
				
				
				IconButton button = new IconButton(bufferedicon, size);
				
				button.setToolTipText(brush.toString());
				button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						e.controls.brushes.setOption(brush);
						gui_brush.setSelectedItem(brush);
					}
				});
				
				panel_4.add(button);
				toolbuttons.put(brush, button);
			}
		} catch (IOException e1) {}
	}
	
	public void removeKeyListeners(Component c) {
		KeyListener[] list = c.getKeyListeners();
		for (int i = 0; i < list.length; i++) {
			c.removeKeyListener(list[i]);
		}
	}
	
	public class CustJCheckBoxMenuItem extends JCheckBoxMenuItem {

		private static final long serialVersionUID = 760172179099467782L;

		public CustJCheckBoxMenuItem(String text) {
			super(text);
		}

		@Override
		protected void processMouseEvent(MouseEvent evt) {
			if (evt.getID() == MouseEvent.MOUSE_RELEASED  && contains(evt.getPoint())) {
				doClick();
				setArmed(true);
			} else
				super.processMouseEvent(evt);
		}

	}
	

	@Override
	public void componentResized(ComponentEvent ev) {
		e.controls.updateimagesize = true;
	}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentHidden(ComponentEvent e) {}
}