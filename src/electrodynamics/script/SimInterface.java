package electrodynamics.script;

import electrodynamics.GeneralMaterialType;
import electrodynamics.MaterialType;
import electrodynamics.Simulation;
import electrodynamics.Controls.BrushAction;
import electrodynamics.Controls.BrushShape;
import electrodynamics.Controls.FloodFillFunc;

public class SimInterface {
	Simulation e;
	public State state;
	
	public boolean replace = false;
	public boolean overwrite = false;
	public double emf_x = 0;
	public double emf_y = 0;
	public double emf_z = 0;
	
	public SimInterface(Simulation e) {
		this.e = e;
	}
	
	public void reset() {
		replace = false;
		overwrite = false;
		emf_x = 0;
		emf_y = 0;
		emf_z = 0;
	}
	
	public void register(Evaluator evaluator) {
		evaluator.registerFunction("list", list);
		evaluator.registerFunction("set_material", set_material);
		evaluator.registerFunction("set_brush_shape", set_brush_shape);
		evaluator.registerFunction("set_brush_size", set_brush_size);
		evaluator.registerFunction("set_replace", set_replace);
		evaluator.registerFunction("set_overwrite", set_overwrite);
		evaluator.registerFunction("set_emf_direction", set_emf_direction);
		evaluator.registerFunction("print", print);
		evaluator.registerFunction("reset", reset);
		evaluator.registerFunction("rectangle", rectangle);
		evaluator.registerFunction("line", line);
		evaluator.registerFunction("point", point);
		evaluator.registerFunction("fill", fill);
	}
	
	public BrushAction getAction() {
		GeneralMaterialType final_mat = (GeneralMaterialType) e.opts.gui_material.getSelectedItem();
		return (i, j, k, in_bounds) -> {
			if (in_bounds) {
				if (final_mat.type == MaterialType.VACUUM) {
					e.eraseMaterial(i, j, k);
				} else if (e.materials[i][j][k].type == MaterialType.VACUUM ^ replace || overwrite) {
					e.eraseMaterial(i, j, k);
					e.initializeMaterial(i, j, k, final_mat);
					if (final_mat.type.hasEMF()) {
						e.materials[i][j][k].emf_x = emf_x;
						e.materials[i][j][k].emf_y = emf_y;
						e.materials[i][j][k].emf_z = emf_z;
					}
				}
			}
		};
	}
	
	//set replace mode
	//get field value
	//make probe
	//record probe
	//make plot
	
	//pause/unpause
	//set sim variables
	//set timestep

	NaryFunction list = new NaryFunction() {
		@Override
		public int get_n_args() { return 0; }

		@Override
		public Object operate(Object[] args) {
			state.println("Materials:");
			for (MaterialType v : MaterialType.values())
				state.println(v.name());

			state.println("\nBrush shapes:");
			for (BrushShape v : BrushShape.values())
				state.println(v.name());
			return 0;
		}
	};
	
	NaryFunction set_material = new NaryFunction() {
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			String name = (String) args[0];
			e.opts.gui_material.setSelectedItem(new GeneralMaterialType(MaterialType.valueOf(name)));
			state.println("Material changed to " + e.opts.gui_material.getSelectedItem().toString());
			return 0;
		}
	};
	
	NaryFunction set_brush_shape = new NaryFunction() {
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			String name = (String) args[0];
			e.opts.gui_brush_1.setSelectedItem(BrushShape.valueOf(name));
			state.println("Brush shape changed to " + e.opts.gui_brush_1.getSelectedItem().toString());
			return 0;
		}
	};

	NaryFunction set_brush_size = new NaryFunction() {
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			double size = (double) args[0];
			e.controls.brushsize = size;
			state.println("Brush size changed to " + e.opts.gui_brushsize.getValue());
			return 0;
		}
	};

	NaryFunction set_replace = new NaryFunction() {
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			double value = (double) args[0];
			replace = value > 0.5;
			state.println("Replace set to " + replace);
			return 0;
		}
	};

	NaryFunction set_overwrite = new NaryFunction() {
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			double value = (double) args[0];
			overwrite = value > 0.5;
			state.println("Overwrite set to " + overwrite);
			return 0;
		}
	};
	
	NaryFunction set_emf_direction = new NaryFunction() {
		@Override
		public int get_n_args() { return 3; }

		@Override
		public Object operate(Object[] args) {
			emf_x = (double) args[0];
			emf_y = (double) args[1];
			emf_z = (double) args[2];
			return 0;
		}
	};
	
	NaryFunction print = new NaryFunction() {
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			state.println(args[0].toString());
			return 0;
		}
	};
	

	NaryFunction reset = new NaryFunction() {
		@Override
		public int get_n_args() { return 0; }

		@Override
		public Object operate(Object[] args) {
			for (int i = 0; i < e.nx; i++)
			{
				for (int j = 0; j < e.ny; j++)
				{
					for (int k = 0; k < e.nz; k++)
					{
						e.eraseMaterial(i, j, k);
					}
				}
			}
			
			state.println("Materials reset.");
			return 0;
		}
	};
	
	NaryFunction rectangle = new NaryFunction() {
		@Override
		public int get_n_args() { return 6; }

		@Override
		public Object operate(Object[] args) {
			int i1 = (int)Math.round((double) args[0]);
			int j1 = (int)Math.round((double) args[1]);
			int k1 = (int)Math.round((double) args[2]);
			int i2 = (int)Math.round((double) args[3]);
			int j2 = (int)Math.round((double) args[4]);
			int k2 = (int)Math.round((double) args[5]);
			
			BrushAction action = getAction();
			
			for (int i = i1; i <= i2; i++) {
				for (int j = j1; j <= j2; j++) {
					for (int k = k1; k <= k2; k++) {
						action.perform(i, j, k, true);
					}
				}
			}
			e.controls.flagChanges(true);
			
			state.println("Rectangle drawn from " + i1 + " " + j2 + " " + k1 + " to " + i2 + " " + j2 + " " + k2);
			return 0;
		}
	};
	
	NaryFunction line = new NaryFunction() {
		@Override
		public int get_n_args() { return 6; }

		@Override
		public Object operate(Object[] args) {
			int i1 = (int)Math.round((double) args[0]);
			int j1 = (int)Math.round((double) args[1]);
			int k1 = (int)Math.round((double) args[2]);
			int i2 = (int)Math.round((double) args[3]);
			int j2 = (int)Math.round((double) args[4]);
			int k2 = (int)Math.round((double) args[5]);
			
			BrushAction action = getAction();

			e.controls.applyBrush(i1, j1, k1, i2, j2, k2, (BrushShape) e.opts.gui_brush_1.getSelectedItem(), e.controls.brushsize, action);
			e.controls.flagChanges(true);
			
			state.println("Line drawn from " + i1 + " " + j2 + " " + k1 + " to " + i2 + " " + j2 + " " + k2);
			return 0;
		}
	};
	
	NaryFunction point = new NaryFunction() {
		@Override
		public int get_n_args() { return 3; }

		@Override
		public Object operate(Object[] args) {
			int i = (int)Math.round((double) args[0]);
			int j = (int)Math.round((double) args[1]);
			int k = (int)Math.round((double) args[2]);

			BrushAction action = getAction();

			action.perform(i, j, k, true);
			e.controls.flagChanges(true);

			//state.println("Pixel set " + i + " " + j + " " + k);
			return 0;
		}
	};
	
	NaryFunction fill = new NaryFunction() {
		@Override
		public int get_n_args() { return 3; }

		@Override
		public Object operate(Object[] args) {
			int i1 = (int)Math.round((double) args[0]);
			int j1 = (int)Math.round((double) args[1]);
			int k1 = (int)Math.round((double) args[2]);

			GeneralMaterialType old_mat = new GeneralMaterialType(e.materials[i1][j1][k1]);
			GeneralMaterialType new_mat = new GeneralMaterialType(MaterialType.SEMI_HEAVY_N_TYPE);
			if (!new_mat.equals(old_mat)) {
				e.controls.floodFill(i1, j1, k1, new FloodFillFunc() {
					@Override
					public boolean isValid(int i, int j, int k) {
						return e.materials[i][j][k].type == old_mat.type && e.materials[i][j][k].cust_id == old_mat.cust_id;
					}

					@Override
					public void fill(int i, int j, int k) {
						e.eraseMaterial(i, j, k);
						e.initializeMaterial(e.materials[i][j][k], new_mat);
						if (new_mat.type.hasEMF()) {
							e.materials[i][j][k].emf_x = emf_x;
							e.materials[i][j][k].emf_y = emf_y;
							e.materials[i][j][k].emf_z = emf_z;
						}
					}
				});
				e.controls.flagChanges(true);
			}
			
			return 0;
		}
	};
}
