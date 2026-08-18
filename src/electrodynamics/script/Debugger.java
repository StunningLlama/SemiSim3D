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
