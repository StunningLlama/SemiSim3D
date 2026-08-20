// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.script;

import java.util.HashMap;

public class State {
	public HashMap<String, Object> variables;
	public boolean suppress_output = false;
	public StringBuilder stdout = new StringBuilder();
	public static int stdout_max_length = 10000;
	
	public State() {
		variables = new HashMap<String, Object>();
	}
	
	public void reset() {
		variables.clear();
		stdout.delete(0, stdout.length());
	}
	
	public void println(String str) {
		if (suppress_output) return;
		print_force(str + "\n");
	}

	public void println_force(String str) {
		print_force(str + "\n");
	}
	
	public void print(String str) {
		if (suppress_output) return;
		print_force(str);
	}
	
	public void print_force(String str) {
		stdout.append(str);
		if (stdout.length() > stdout_max_length + 1000) {
			stdout.delete(0, stdout.length() - stdout_max_length);
		}
	}
	
	public String get_output() {
		return stdout.toString();
	}
}