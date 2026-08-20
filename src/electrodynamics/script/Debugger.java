// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.script;

import java.util.List;

public class Debugger {
	static void print(List<Unit> units) {
		for (Unit u : units) {
			System.out.print(u.toString() + " ");
		}
		System.out.println();
	}
}
