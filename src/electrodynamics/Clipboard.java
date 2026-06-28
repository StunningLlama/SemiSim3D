// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;

import java.util.ArrayList;
import java.util.List;

import electrodynamics.probe.Probe;
import electrodynamics.util.Utils;

public class Clipboard {

	ClipboardMaterial[][][] mat;
	List<Probe> probes;
	Simulation e;

	public Clipboard(Simulation e) {
		this.e = e;
		mat = new ClipboardMaterial[e.nx][e.ny][e.nz];
		probes = new ArrayList<Probe>();
	}

	public void clear() {
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					if (mat[i][j][k] == null)
						mat[i][j][k] = new ClipboardMaterial();
					else
						mat[i][j][k].erase();
				}
			}
		}

		probes.clear();
	}

	public void cut() {
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					if (mat[i][j][k] == null)
						mat[i][j][k] = new ClipboardMaterial();
					mat[i][j][k].erase();
					if (e.controls.selected[i][j][k]) {
						mat[i][j][k] = new ClipboardMaterial(e, i, j, k);
						e.controls.selected[i][j][k] = false;
						e.eraseMaterial(i, j, k);
					}
				}
			}
		}

		probes = Utils.cloneList(e.probes, Probe::clone, (p) -> p.selected);
		e.probes.removeIf((p) -> p.selected);
	}

	public void paste(int delta_mx, int delta_my, int delta_mz) {
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					int si = i-delta_mx;
					int sj = j-delta_my;
					int sk = k-delta_mz;
					if (si >= 0 && sj >= 0 && si < e.nx && sj < e.ny && mat[si][sj][sk].m.type != MaterialType.VACUUM) {
						e.eraseMaterial(i, j, k);
						mat[si][sj][sk].paste(e, i, j, k);
						e.controls.selected[i][j][k] = true;
					}
				}
			}
		}


		for (Probe p : probes) {
			p.translate(delta_mx, delta_my, delta_mz);
			p.selected = true;
			if (p.checkInBounds(e)) {
				e.probes.add(p.clone());
			}
		}
	}

	public void transfer(Clipboard target) {
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					e.controls.selected[i][j][k] = false;
				}
			}
		}

		for (Probe p : probes)
			p.selected = false;

		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					target.mat[i][j][k] = mat[i][j][k].clone();
				}
			}
		}

		target.probes = Utils.cloneList(probes, Probe::clone);
	}

	public boolean copy(boolean deleteselection) {
		int i_min = e.nx-1;
		int j_min = e.ny-1;
		int k_min = e.nz-1;
		boolean clipboardempty = true;
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					mat[i][j][k].erase();
					if (e.controls.selected[i][j][k] && e.materials[i][j][k].type != MaterialType.VACUUM) {
						if (i < i_min) i_min = i;
						if (j < j_min) j_min = j;
					}
				}
			}
		}
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					if (e.controls.selected[i][j][k] && e.materials[i][j][k].type != MaterialType.VACUUM) {

						mat[i-i_min][j-j_min][k-k_min] = new ClipboardMaterial(e, i, j, k);

						if (deleteselection) {
							e.eraseMaterial(i, j, k);
						}

						clipboardempty = false;
					}
					if (deleteselection) {
						e.controls.selected[i][j][k] = false;
					}
				}
			}
		}

		probes = Utils.cloneList(e.probes, Probe::clone, (p) -> p.selected);

		if (clipboardempty && !probes.isEmpty()) {
			i_min = e.controls.mx_start;
			j_min = e.controls.my_start;

			clipboardempty = false;
		}

		for (Probe p : probes)
			p.translate(-i_min, -j_min, -k_min);


		if (deleteselection) {
			e.probes.removeIf((p) -> p.selected);
		}

		return clipboardempty;
	}

	public void flip_h() {
		int i_max = 0;
		int j_max = 0;
		int k_max = 0;
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					if (mat[i][j][k].m.type != MaterialType.VACUUM) {
						if (i > i_max) i_max = i;
						if (j > j_max) j_max = j;
						if (k > k_max) k_max = k;
					}
				}
			}
		}

		ClipboardMaterial[][][] new_selection = new ClipboardMaterial[i_max+1][j_max+1][k_max+1];
		for (int i = 0; i <= i_max; i++)
		{
			for (int j = 0; j <= j_max; j++)
			{
				for (int k = 0; k <= k_max; k++)
				{
					new_selection[i_max-i][j][k] = mat[i][j][k].clone();
					new_selection[i_max-i][j][k].flip_h();
					mat[i][j][k].erase();
				}
			}
		}

		for (int i = 0; i <= i_max; i++)
		{
			for (int j = 0; j <= j_max; j++)
			{
				for (int k = 0; k <= k_max; k++)
				{
					mat[i][j][k] = new_selection[i][j][k];
				}
			}
		}

		for (Probe p : probes) p.flip_h(0, i_max);
	}

	public void flip_v() {
		int i_max = 0;
		int j_max = 0;
		int k_max = 0;
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					if (mat[i][j][k].m.type != MaterialType.VACUUM) {
						if (i > i_max) i_max = i;
						if (j > j_max) j_max = j;
					}
				}
			}
		}

		ClipboardMaterial[][][] new_selection = new ClipboardMaterial[i_max+1][j_max+1][k_max+1];
		for (int i = 0; i <= i_max; i++)
		{
			for (int j = 0; j <= j_max; j++)
			{
				for (int k = 0; k <= k_max; k++)
				{
					new_selection[i][j_max - j][k] = mat[i][j][k].clone();
					new_selection[i][j_max - j][k].flip_v();
					mat[i][j][k].erase();
				}
			}
		}

		for (int i = 0; i <= i_max; i++)
		{
			for (int j = 0; j <= j_max; j++)
			{
				for (int k = 0; k <= k_max; k++)
				{
					mat[i][j][k] = new_selection[i][j][k];
				}
			}
		}

		for (Probe p : probes) p.flip_v(0, j_max);
	}

	public void rotate90() {
		int i_max = 0;
		int j_max = 0;
		int k_max = 0;
		for (int i = 0; i < e.nx; i++)
		{
			for (int j = 0; j < e.ny; j++)
			{
				for (int k = 0; k < e.nz; k++)
				{
					if (mat[i][j][k].m.type != MaterialType.VACUUM) {
						if (i > i_max) i_max = i;
						if (j > j_max) j_max = j;
						if (k > k_max) k_max = k;
					}
				}
			}
		}

		ClipboardMaterial[][][] new_selection = new ClipboardMaterial[j_max+1][i_max+1][k_max+1];
		for (int i = 0; i <= i_max; i++)
		{
			for (int j = 0; j <= j_max; j++)
			{
				for (int k = 0; k <= k_max; k++)
				{
					new_selection[j_max-j][i][k] = mat[i][j][k].clone();
					new_selection[j_max-j][i][k].rotate90();
					mat[i][j][k].erase();
				}
			}
		}

		for (int i = 0; i <= j_max; i++)
		{
			for (int j = 0; j <= i_max; j++)
			{
				for (int k = 0; k <= k_max; k++)
				{
					mat[i][j][k] = new_selection[i][j][k];
				}
			}
		}

		for (Probe p : probes) p.rotate90(i_max, j_max);
	}
}

class ClipboardMaterial implements Cloneable {
	double rho_n = 0;
	double rho_p = 0;
	Material m = new Material();

	public ClipboardMaterial() {};

	public ClipboardMaterial(Material m) {
		this.m.copyFrom(m);
	}

	public ClipboardMaterial(Simulation e, int i, int j, int k) {
		this(e.materials[i][j][k]);
		rho_n = e.rho_n[i][j][k];
		rho_p = e.rho_p[i][j][k];
	}

	public void paste(Simulation e, int i, int j, int k) {
		e.materials[i][j][k].copyFrom(m);
		e.rho_n[i][j][k] = rho_n;
		e.rho_p[i][j][k] = rho_p;
	}

	public void erase() {
		m.initialize();
		rho_n = 0;
		rho_p = 0;
	}

	@Override
	public ClipboardMaterial clone() {
		ClipboardMaterial mat = new ClipboardMaterial(m);
		mat.rho_n = rho_n;
		mat.rho_p = rho_p;
		return mat;
	}

	public void rotate90() {
		//TODO
		//m.emf_direction += Math.PI/2.0;
	}

	public void flip_h() {
		//m.emf_direction = Math.PI - m.emf_direction;
	}

	public void flip_v() {
		//m.emf_direction = -m.emf_direction;
	}
}