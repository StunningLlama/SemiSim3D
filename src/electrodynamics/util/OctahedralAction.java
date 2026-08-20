// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.util;

// Group representation of the octahedral group (and translations)
public interface OctahedralAction {
	public void flip_x();
	public void flip_y();
	public void flip_z();
	public void rot_x();
	public void rot_y();
	public void rot_z();
	public void translate(int dx, int dy, int dz);
	
	public enum OctahedralGenerator {
		FLIP_X("Flip along x-axis"),
		FLIP_Y("Flip along y-axis"),
		FLIP_Z("Flip along z-axis"),
		ROT_X("Rotate around x-axis"),
		ROT_Y("Rotate around y-axis"),
		ROT_Z("Rotate around z-axis");
		
		private String name;
		OctahedralGenerator(String name)
		{
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
		
		public void apply(OctahedralAction action) {
			switch(this) {
			case FLIP_X:
				action.flip_x();
				break;
			case FLIP_Y:
				action.flip_y();
				break;
			case FLIP_Z:
				action.flip_z();
				break;
			case ROT_X:
				action.rot_x();
				break;
			case ROT_Y:
				action.rot_y();
				break;
			case ROT_Z:
				action.rot_z();
				break;
			default:
				break;
				
			}
		}
	};
}
