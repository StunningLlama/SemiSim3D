// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.script;

import electrodynamics.GeneralMaterialType;
import electrodynamics.MaterialType;
import electrodynamics.Simulation;
import electrodynamics.script.Function.NaryFunction;
import electrodynamics.Controls.BrushAction;
import electrodynamics.Controls.BrushShape;
import electrodynamics.Controls.FloodFillFunc;

public class SimulationInterface {
	Simulation e;
	public State state;
	
	public boolean replace;
	public boolean overwrite;
	public double emf_x;
	public double emf_y;
	public double emf_z;
	public double brush_size;
	public BrushShape brush_shape;
	public GeneralMaterialType material;
	
	public SimulationInterface(Simulation e) {
		this.e = e;
		reset();
	}
	
	public void reset() {
		replace = false;
		overwrite = false;
		emf_x = 0;
		emf_y = 0;
		emf_z = 0;
		brush_size = 0.5;
		brush_shape = BrushShape.SQUARE;
		material = new GeneralMaterialType(MaterialType.METAL);
	}
	
	public void register(Evaluator evaluator) {
		evaluator.registerFunction("set_material", set_material);
		evaluator.registerFunction("write", write);
		evaluator.registerFunction("read", read);
		evaluator.registerFunction("list", list);
		evaluator.registerFunction("set_material", set_material);
		evaluator.registerFunction("set_brush_shape", set_brush_shape);
		evaluator.registerFunction("set_brush_size", set_brush_size);
		evaluator.registerFunction("set_replace", set_replace);
		evaluator.registerFunction("set_overwrite", set_overwrite);
		evaluator.registerFunction("set_emf_direction", set_emf_direction);
		evaluator.registerFunction("set_option", set_option);
		evaluator.registerFunction("wait", wait);
		evaluator.registerFunction("print", print);
		evaluator.registerFunction("clear", clear);
		evaluator.registerFunction("reset", reset);
		evaluator.registerFunction("rectangle", rectangle);
		evaluator.registerFunction("line", line);
		evaluator.registerFunction("point", point);
		evaluator.registerFunction("fill", fill);
		evaluator.registerFunction("set_vi", set_vi);
	}
	
	public BrushAction getAction() {
		GeneralMaterialType final_mat = material;
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


	//get field value
	//make probe
	//record probe
	//make plot
	//Wait iterations

	NaryFunction write = new NaryFunction() {
		@Override
		public String getHelpText() {
			return "write({address} [number], value [any type]): Writes {value} to memory location given by {address}";
		}
		
		@Override
		public int get_n_args() { return 2; }

		@Override
		public Object operate(Object[] args) {
			int address = (int) Math.round((Double) args[0]);
			Object value = args[1];
			
			if (address >= 0 && address < state.tape.length) {
				state.tape[address] = value;
				return 0;
			} else {
				return -1;
			}
		}
	};
	
	NaryFunction read = new NaryFunction() {
		@Override
		public String getHelpText() {
			return "read({address} [number]): Returns the value at memory location given by {address}.";
		}
		
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			int address = (int) Math.round((Double) args[0]);
			
			if (address >= 0 && address < state.tape.length) {
				return state.tape[address];
			} else {
				return Double.NaN;
			}
		}
	};
	
	NaryFunction list = new NaryFunction() {
		@Override
		public String getHelpText() {
			return "list(): Lists all materials and brush shapes.";
		}
		
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
		public String getHelpText() {
			return "set_material({name} [string]): Sets the brush material to that with name given by {name}. See list() for a list of material names.";
		}
		
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			String name = (String) args[0];
			material = new GeneralMaterialType(MaterialType.valueOf(name));
			state.println("Material changed to " + material.toString());
			return 0;
		}
	};
	
	NaryFunction set_brush_shape = new NaryFunction() {
		@Override
		public String getHelpText() {
			return "set_brush_shape({shape} [string]): Sets the brush shape to that given by {shape}. See list() for a list of shapes.";
		}
		
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			String name = (String) args[0];
			brush_shape = BrushShape.valueOf(name);
			state.println("Brush shape changed to " + brush_shape.toString());
			return 0;
		}
	};

	NaryFunction set_brush_size = new NaryFunction() {
		@Override
		public String getHelpText() {
			return "set_brush_size({size} [number]): Sets the brush radius (in pixels) to {size}. {size} may be a real number.";
		}
		
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			brush_size = (double) args[0];
			state.println("Brush size changed to " + brush_size);
			return 0;
		}
	};

	NaryFunction set_replace = new NaryFunction() {
		public String getHelpText() {
			return "set_replace({replace} [boolean]): Sets replace mode.";
		}
		
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
		public String getHelpText() {
			return "set_overwrite({overwrite} [boolean]): Sets overwrite mode.";
		}
		
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
		public String getHelpText() {
			return "set_emf_direction({x} [number], {y} [number], {z} [number]): Sets EMF direction to vector ({x}, {y}, {z}). The vector should be normalized.";
		}
		
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
	

	NaryFunction set_option = new NaryFunction() {
		public String getHelpText() {
			return "set_option({name} [string], {value} [number]): Changes the simulation setting given by {name} to {value}.";
		}
		
		@Override
		public int get_n_args() { return 2; }

		@Override
		public Object operate(Object[] args) {
			String name = (String) args[0];
			double value = (double) args[1];
			
			if (e.opts.boolean_names.containsKey(name)) {
				e.opts.boolean_names.get(name).setSelected(value > 0.5);
			} else if (e.opts.integer_names.containsKey(name)) {
				e.opts.integer_names.get(name).setValue((int)Math.round(value));
			} else {
				return -1;
			}
			
			return 0;
		}
	};
	

	NaryFunction wait = new NaryFunction() {
		public String getHelpText() {
			return "wait({time} [number]): Waits for amount of time {time} in seconds.";
		}
		
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			double value = (double) args[0];
			
			e.codeeditor.flush();

			e.rwLock.readLock().unlock();
			
			long nanos = (long)(value*1e9);
			
			try {
				Thread.sleep(nanos/1000000l, (int)(nanos%1000000l));
			} catch (InterruptedException e1) {
				throw new RuntimeException("Script was interrupted.");
			} finally {
				e.rwLock.readLock().lock();
			}
			
			return 0;
		}
	};
	
	NaryFunction print = new NaryFunction() {
		public String getHelpText() {
			return "print({value} [any type]): Prints {value}.";
		}
		
		@Override
		public int get_n_args() { return 1; }

		@Override
		public Object operate(Object[] args) {
			state.println(args[0].toString());
			return 0;
		}
	};
	

	NaryFunction clear = new NaryFunction() {
		public String getHelpText() {
			return "clear(): Clears all materials in the simulation.";
		}
		
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
	
	NaryFunction reset = new NaryFunction() {
		public String getHelpText() {
			return "reset(): Resets all fields.";
		}
		
		@Override
		public int get_n_args() { return 0; }

		@Override
		public Object operate(Object[] args) {
			e.controls.clear = true;
			return 0;
		}
	};
	
	NaryFunction rectangle = new NaryFunction() {
		public String getHelpText() {
			return "rectangle({x1} [number], {y1} [number], {z1} [number], {x2} [number], {y2} [number], {z2} [number]): Draws rectangle from ({x1}, {y1}, {z1}) to ({x2}, {y2}, {z2}), in pixels.";
		}
		
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
			e.controls.updatematerials = true;
			
			state.println("Rectangle drawn from " + i1 + " " + j1 + " " + k1 + " to " + i2 + " " + j2 + " " + k2);
			return 0;
		}
	};
	
	NaryFunction line = new NaryFunction() {
		public String getHelpText() {
			return "line({x1} [number], {y1} [number], {z1} [number], {x2} [number], {y2} [number], {z2} [number]): Draws line from ({x1}, {y1}, {z1}) to ({x2}, {y2}, {z2}), in pixels.";
		}
		
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

			e.controls.applyBrush(i1, j1, k1, i2, j2, k2, brush_shape, brush_size, action);
			e.controls.updatematerials = true;
			
			state.println("Line drawn from " + i1 + " " + j1 + " " + k1 + " to " + i2 + " " + j2 + " " + k2);
			return 0;
		}
	};
	
	NaryFunction point = new NaryFunction() {
		public String getHelpText() {
			return "point({x} [number], {y} [number], {z} [number]): Draws pixel at ({x}, {y}, {z}).";
		}
		
		@Override
		public int get_n_args() { return 3; }

		@Override
		public Object operate(Object[] args) {
			int i = (int)Math.round((double) args[0]);
			int j = (int)Math.round((double) args[1]);
			int k = (int)Math.round((double) args[2]);

			BrushAction action = getAction();

			action.perform(i, j, k, true);
			e.controls.updatematerials = true;

			//state.println("Pixel set " + i + " " + j + " " + k);
			return 0;
		}
	};
	
	NaryFunction fill = new NaryFunction() {
		public String getHelpText() {
			return "fill({x} [number], {y} [number], {z} [number]): Fills region with a material, starting at ({x}, {y}, {z}).";
		}
		
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
				e.controls.updatematerials = true;
			}
			
			return 0;
		}
	};
	
	NaryFunction set_vi = new NaryFunction() {
		public String getHelpText() {
			return "set_vi({x} [number], {y} [number], {z} [number], {value} [number]): Sets the voltage (V) or current (I) of a source at ({x}, {y}, {z}) to {value}";
		}
		
		@Override
		public int get_n_args() { return 4; }

		@Override
		public Object operate(Object[] args) {
			int mx = (int)Math.round((double) args[0]);
			int my = (int)Math.round((double) args[1]);
			int mz = (int)Math.round((double) args[2]);
			int value = (int)Math.round((double) args[3]);
			
			boolean iscurrentselected = e.materials[mx][my][mz].type == MaterialType.CURRENT;

			double emf = 0;
			if (!iscurrentselected) {
				 emf = value/e.controls.calcVoltageLength(e.materials[mx][my][mz].emf_x, e.materials[mx][my][mz].emf_y, e.materials[mx][my][mz].emf_z);
			} else {
				emf = value/(e.currentsource_sigma*e.controls.calcCurrentArea((int)Math.round(e.materials[mx][my][mz].emf_x), (int)Math.round(e.materials[mx][my][mz].emf_y), (int)Math.round(e.materials[mx][my][mz].emf_z)));
			}
			
			double emf_final = emf;
			
			e.controls.floodFill(mx, my, mz, new FloodFillFunc() {
				@Override
				public boolean isValid(int i, int j, int k) {
					return e.materials[i][j][k].type == e.materials[mx][my][mz].type;
				}

				@Override
				public void fill(int i, int j, int k) {
					e.materials[i][j][k].emf = emf_final;
				}
			});
			
			return 0;
		}
	};
}
