// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

public class MenuCheckList<T extends Enum<?>, Button extends JRadioButtonMenuItem> implements ActionListener {

	public HashMap<T, Button> buttonmap = new HashMap<T, Button>();
	public HashMap<T, IconButton> toolbarmap = new HashMap<T, IconButton>();
	public List<Button> buttonlist = new ArrayList<Button>();
	public List<T> optionlist = new ArrayList<T>();
	public ButtonGroup buttongroup = new ButtonGroup();
	public JMenu menu;
	public HashSet<T> separators;
	private T default_option;
	private ActionListener listener;
	
	public MenuCheckList(T[] values, T default_option) {
		for (T b : values) {
			optionlist.add(b);
		}
		this.default_option = default_option;
	}
	
	public void initialize(JMenu menu, ActionListener a, T[] sep, Supplier<Button> constructor) {
		this.menu = menu;
		this.separators = new HashSet<T>();
		if (sep != null)
			separators.addAll(Arrays.asList(sep));
		
		for (T b : optionlist) {
			if (separators.contains(b))
				menu.add(new JSeparator());
			
			Button button = constructor.get();
			button.setText(b.toString());
			buttonmap.put(b, button);
			buttonlist.add(button);
			button.setActionCommand(optionlist.get(0).getClass().getName());
			button.addActionListener(a);
			button.addActionListener((ev) -> {
				updateToolbar();
			});
			buttongroup.add(button);
			menu.add(button);
		}
		
		if (default_option != null)
			buttongroup.setSelected(buttonmap.get(default_option).getModel(), true);
		
		listener = a;
	}
	
	public void addToolbarButton(T t, IconButton b) {
		toolbarmap.put(t, b);
		b.setActionCommand(optionlist.get(0).getClass().getName());
		//b.addActionListener(listener);
	}
	
	public void removeOption(T t) {
		menu.remove(buttonmap.get(t));
	}

	public void addOption(T t) {
		menu.add(buttonmap.get(t));
	}
	
	public Button getButton(T t) {
		return buttonmap.get(t);
	}

	public T getOption() {
		if (buttonmap.isEmpty()) {
			return default_option;
		}
		
		for (T t : buttonmap.keySet()) {
			if (buttonmap.get(t).getModel() == buttongroup.getSelection())
				return t;
		}
		
		return null;
	}


	public void setOption(T t) {
		if (buttonmap.get(t) != null)
			buttongroup.setSelected(buttonmap.get(t).getModel(), true);
		
		for (T t2 : toolbarmap.keySet()) {
			toolbarmap.get(t2).setHighlighted(t2.equals(t));
		}
	}
	
	public void setOption(int i) {
		buttongroup.setSelected(buttonlist.get(i).getModel(), true);

		T t = getOption();
		
		for (T t2 : toolbarmap.keySet()) {
			toolbarmap.get(t2).setHighlighted(t2.equals(t));
		}
	}
	
	public void updateToolbar() {
		T t = getOption();
		
		for (T t2 : toolbarmap.keySet()) {
			toolbarmap.get(t2).setHighlighted(t2.equals(t));
		}
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		//for (T b : buttonmap.keySet())
		//	if (ev.getSource() == buttonmap.get(b))
		//		selected = b;
	}
	
	public T containsButton(JRadioButtonMenuItem src) {
		for (T b : buttonmap.keySet())
			if (src == buttonmap.get(b))
				return b;
		
		return null;
	}
}
