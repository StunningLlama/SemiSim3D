// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.probe;

import electrodynamics.Renderer.VectorView;
import electrodynamics.units.Quantity;

public class FluxProbe extends AreaProbe {
	public FluxProbe(int mx, int my, int mz) {
		super(mx, my, mz);
		quantity = Quantity.MAGNETIC_FLUX;
		shorthand = "Φ";
		vectorname = VectorView.B_FIELD;
	}
}