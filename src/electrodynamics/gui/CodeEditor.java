package electrodynamics.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import electrodynamics.Simulation;
import electrodynamics.script.Interpreter;
import electrodynamics.script.Interpreter.Instruction;
import electrodynamics.script.SimInterface;
import electrodynamics.script.State;

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
	
	public JTextArea list;
	private JButton btn_cancel;
	private JButton btn_run;
	private JTextArea  textPane;
	private JPanel panel;
	private JPanel panel_1;
	private JPanel panel_2;
	private JPanel panel_3;
	private JScrollPane scrollPane_1;

	public CodeEditor(Simulation e) {
		this.e = e;
		setTitle("Code editor");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 705, 595);
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
		
		list = new JTextArea();
		scrollPane.setViewportView(list);
		list.setColumns(35);
		list.setEditable(false);
		
		panel_1 = new JPanel();
		panel_1.setBorder(new EmptyBorder(5, 5, 5, 5));
		//contentPane.add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		
		textPane = new JTextArea ();
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
		
		btn_run = new JButton("Run commands");
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
	SimInterface simint;
	State state = new State();
	private JSplitPane splitPane;
	
	public void initialize() {
		this.btn_cancel.addActionListener(this);
		this.btn_run.addActionListener(this);
		setLocationRelativeTo(null);

		simint = new SimInterface(e);
		simint.state = state;
		simint.register(interpreter.evaluator);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btn_cancel) {
			this.setVisible(false);
		} else if (e.getSource() == btn_run) {
			simint.reset();
			state.reset();
			state.println_force("Running...");
			String code = textPane.getText();
			try {
				List<Instruction> program = interpreter.process(code);
				interpreter.execute(program, state);
			} catch (Exception ex) {
				state.println_force(ex.getMessage());
			}
				
			state.println_force("Script finished.");
			
			list.setText(state.get_output());
		}
	}
}
