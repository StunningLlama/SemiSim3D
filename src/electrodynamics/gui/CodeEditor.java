// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import electrodynamics.Renderer.Text;
import electrodynamics.SemiSim;
import electrodynamics.Simulation;
import electrodynamics.script.Function;
import electrodynamics.script.Interpreter;
import electrodynamics.script.Interpreter.Instruction;
import electrodynamics.script.SimulationInterface;
import electrodynamics.script.State;
import electrodynamics.util.FileInterface;

public class CodeEditor extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	Simulation e;
	private JPanel contentPane;
	public JTextField width;
	public JTextField resolution;
	public JTextField depth;
	public JTextField ni_metal;
	public JTextField W_metal;
	public JTextField E_b_metal;
	public JTextField W_metal_high;
	public JTextField W_metal_low;
	public JTextField recomb_rate_metal;
	public JTextField T;
	
	public CustTextArea list;
	private JButton btn_cancel;
	private JButton btn_run;
	private CustTextArea  textPane;
	private JPanel panel;
	private JPanel panel_1;
	private JPanel panel_2;
	private JPanel panel_3;
	private JScrollPane scrollPane_1;

	public JMenuBar menuBar;
	public JMenu menu_file;
	public JMenu menu_help2;
	public JMenuItem menu_new;
	public JMenuItem menu_open;
	public JMenuItem menu_save;
	public JMenuItem menu_saveas;
	public JMenuItem menu_help;

	public CodeEditor(Simulation e) {
		this.e = e;
		setTitle("Code editor");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 705, 595);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		menu_file = new JMenu("File");
		menuBar.add(menu_file);

		menu_new = new JMenuItem("New script...");
		//menu_new.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		menu_file.add(menu_new);

		menu_open = new JMenuItem("Open file...");
		//menu_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		menu_file.add(menu_open);

		menu_save = new JMenuItem("Save");
		//menu_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		menu_file.add(menu_save);

		menu_saveas = new JMenuItem("Save as...");
		//menu_saveas.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		menu_file.add(menu_saveas);
		

		menu_help2 = new JMenu("Help");
		menuBar.add(menu_help2);

		menu_help = new JMenuItem("Functions");
		menu_help2.add(menu_help);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		//contentPane.add(panel, BorderLayout.EAST);
		panel.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		panel.add(scrollPane);
		
		list = new CustTextArea();
		scrollPane.setViewportView(list);
		list.setColumns(35);
		list.setEditable(false);
		
		panel_1 = new JPanel();
		panel_1.setBorder(new EmptyBorder(5, 5, 5, 5));
		//contentPane.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		
		textPane = new CustTextArea();
		textPane.setColumns(50);
		
		scrollPane_1 = new JScrollPane(textPane);
		panel_1.add(scrollPane_1);
		
		TextLineNumber tln = new TextLineNumber(textPane);
		scrollPane_1.setRowHeaderView( tln );
		//scrollPane_1.setViewportView(textPane);
		
		panel_3 = new JPanel();
		contentPane.add(panel_3, BorderLayout.SOUTH);
		panel_3.setLayout(new BorderLayout(0, 0));
		
		panel_2 = new JPanel();
		panel_3.add(panel_2, BorderLayout.EAST);
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		
		btn_run = new JButton("Run script");
		panel_2.add(btn_run);
		
		btn_cancel = new JButton("Close");
		panel_2.add(btn_cancel);
		
		splitPane = new JSplitPane();
		contentPane.add(splitPane, BorderLayout.CENTER);
		splitPane.add(panel_1, JSplitPane.LEFT);
		splitPane.add(panel, JSplitPane.RIGHT);
		
		splitPane.setPreferredSize(new Dimension(800, 500));
		
		pack();
		splitPane.setDividerLocation(0.66);

		UndoManager manager = new UndoManager();
		textPane.getDocument().addUndoableEditListener(manager);

	    KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK);
	    KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK);
	    
		textPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(undoKeyStroke, "undo");
		textPane.getActionMap().put("undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					manager.undo();
				} catch (CannotUndoException ex) {}
			}
		});
		
		textPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(redoKeyStroke, "redo");
		textPane.getActionMap().put("redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					manager.redo();
				} catch (CannotRedoException ex) {}
			}
		});
	}

	Interpreter interpreter = new Interpreter();
	SimulationInterface simint;
	State state = new State();
	private JSplitPane splitPane;
	
	public void initialize() {
		this.btn_cancel.addActionListener(this);
		this.btn_run.addActionListener(this);
		setLocationRelativeTo(null);

		simint = new SimulationInterface(e);
		simint.state = state;
		simint.register(interpreter.evaluator);
		
		makeHelpText();
		
		io.fileextension = ".ssscript";
		io.saveversion = 0;
		io.window = this;
		io.startingpath = SemiSim.userdir;

		menu_new.addActionListener((ev) -> {
			textPane.setText("");
		});
		menu_open.addActionListener((ev) -> {
			io.readFile();
		});
		menu_save.addActionListener((ev) -> {
			io.writeFile(false);
		});
		menu_saveas.addActionListener((ev) -> {
			io.writeFile(true);
		});
		menu_help.addActionListener((ev) -> {
			HtmlBox box = new HtmlBox();
			box.textPane.setText(helptext);
			box.setVisible(true);
		});
	}
	
	public void makeHelpText() {
		helptext = "";
		for (Function f : interpreter.evaluator.functions.values()) {
			if (!f.getHelpText().isEmpty()) {
				helptext += f.getHelpText().replace("{", "<b>").replace("}", "</b>").replace("[", "[<i>").replace("]", "</i>]") + "<br><br>";
			}
		}
	}
	
	String helptext = "";
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btn_cancel) {
			this.setVisible(false);
		} else if (e.getSource() == btn_run) {
			if (running) {
				halt();
			} else {
				thread = new Thread(() -> { run(); });
				thread.start();
			}
		}
	}
	
	Thread thread;
	boolean running = false;
	
	public void halt() {
		if (!running) return;
		running = false;
		btn_run.setText("Run script");
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
		}
	}
	
	public void run() {
		if (running) return;
		running = true;
		btn_run.setText("Stop script");
		
		simint.reset();
		state.reset();
		state.println_force("Running...");
		String code = textPane.getText();
		try {
			List<Instruction> program = interpreter.process(code);
			e.rwLock.readLock().lock();
			interpreter.execute(program, state);
		} catch (Exception ex) {
			state.println_force(ex.getMessage());
		} finally {
			e.rwLock.readLock().unlock();
		}
			
		state.println_force("Script finished.");
		flush();
		halt();
	}
	
	public void flush() {
		list.setText(state.get_output());
	}
	
	class CustTextArea extends JTextArea {
		private static final long serialVersionUID = 151978487570095242L;
		
		@Override
		public void updateUI() {
			super.updateUI();
			this.setFont(Text.getMonospacedFont());
		}
	}
	

	FileInterface io = new FileInterface() {

		public void readfile(File infile, Runnable callback) {
			SwingUtilities.invokeLater(() -> {
				if (infile == null || !infile.exists()) return;

				try {
					textPane.setText(new String(Files.readAllBytes(infile.toPath())));
				} catch (FileNotFoundException ex) {
					return;
				} catch (IOException | IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(CodeEditor.this,
							"Unable to load file.\n" + ex.getMessage());
					ex.printStackTrace();
					return;
				}
				return;
			});
		}

		public void writeFile(File outfile, Runnable callback)
		{
			SwingUtilities.invokeLater(() -> {
				try {
					PrintWriter fstr = new PrintWriter(new FileOutputStream(outfile));

					fstr.write(textPane.getText());

					fstr.flush();
					fstr.close();

					JOptionPane.showMessageDialog(CodeEditor.this, "Script saved.");
				} catch (FileNotFoundException e) {
					return;
				}
				return;
			});
		}
	};
	
}
