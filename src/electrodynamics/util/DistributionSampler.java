// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.util;

// Generates random samples from a spatial distribution

public class DistributionSampler {

	// Test function
	/*public static void main(String[] args) {
		SpatialDistribution test = new SpatialDistribution();
		test.init(5, 5);
		test.prepare(new double[][] {{0, 0, 0, 0, 0},{0, 0, 0, 0, 0}, {0, 1, 4, 1, 0}, {0,0,0,0,0},{1,1,0,0,0}});
		test.generateNsamples(5000, (c) -> {System.out.println(c.x + "\t" + c.y);});
	}*/


	private double[][][] rho;				// Physical density function, unnormalized
	private double rho_tot = 0;			// Total amount
	private double[][][] p_dual;			// Probability distribution on dual grid
	private double[][][] p_dual_cdf;		// Cumulative probability on dual grid

	private int nx;
	private int ny;
	private int nz;

	private int mx;
	private int my;
	private int mz;

	FastRandom rand = new FastRandom();

	public void init(int nx, int ny, int nz) {
		this.nx = nx;
		this.ny = ny;
		this.nz = nz;
		mx = nx-1;
		my = ny-1;
		mz = nz-1;

		rho = new double[nx][ny][nz];

		p_dual = new double[nx-1][ny-1][nz-1];
		p_dual_cdf = new double[nx-1][ny-1][nz-1];
	}

	public void prepare(double[][][] rho_in) {
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					rho[i][j][k] = (rho_in[i][j][k] < 0) ? -rho_in[i][j][k]: rho_in[i][j][k];
				}
			}
		}

		rho_tot = 0;
		for (int i = 0; i < mx; i++) {
			for (int j = 0; j < my; j++) {
				for (int k = 0; k < mz; k++) {
					p_dual[i][j][k] = 0.125*(rho[i][j][k] + rho[i+1][j][k] + rho[i][j+1][k] + rho[i+1][j+1][k] + rho[i][j][k+1] + rho[i+1][j][k+1] + rho[i][j+1][k+1] + rho[i+1][j+1][k+1]);
					rho_tot += p_dual[i][j][k];
				}
			}
		}

		for (int i = 0; i < mx; i++) {
			for (int j = 0; j < my; j++) {
				for (int k = 0; k < mz; k++) {
					p_dual_cdf[i][j][k] = lin_index(p_dual_cdf, to_linear(i, j, k)-1) + p_dual[i][j][k]/rho_tot;
				}
			}
		}
	}

	public double getTotalAmount() {
		return rho_tot;
	}

	private int binarySearch(double p) {
		int M = mx*my;
		int lower = 1;
		int upper = M-1;
		int mid = 0;

		while (lower != upper) {
			mid = (lower+upper)/2;
			if (p <= lin_index(p_dual_cdf, mid-1))
			{
				if (upper-lower == 1)
					mid = lower+1;
				upper = mid;
			} else if (p > lin_index(p_dual_cdf, mid)){
				lower = mid;
			} else {
				break;
			}
		}

		return mid;
	}
	
	public double lin_index(double[][][] arr, int index) {
		if (index < 0)
			return 0;
		
		return arr[index/(my*mz)][(index/mz)%my][index%mz];
	}
	
	public int to_linear(int x, int y, int z) {
		return x*my*mz + y*mz + z;
	}

	private Coord generateSample() {
		
		while (true) {
			int index = binarySearch(rand.next());
			int i = index/(my*mz);
			int j = (index/mz)%my;
			int k = index%mz;
			double rhomax = Math.max(Utils.max(rho[i][j][k], rho[i+1][j][k], rho[i][j+1][k], rho[i+1][j+1][k]), Utils.max(rho[i][j][k+1], rho[i+1][j][k+1], rho[i][j+1][k+1], rho[i+1][j+1][k+1]));
			double di = rand.next();
			double dj = rand.next();
			double dk = rand.next();
			if (rand.next() < Utils.bilinearinterp(rho, i+di, j+dj, k+dk)/rhomax) {
				return new Coord(i+di, j+dj, k+dk);
			}
		}
	}

	public void generateSamples(double N, GenFunc f) {
		if (!Double.isFinite(rho_tot) || rho_tot <= 0 || N > 100000)
			return;

		int N_floor = (int)Math.floor(N);
		double N_frac = N-N_floor;

		for (int n = 0; n < N_floor; n++) {
			f.generate(generateSample());
		}

		if (rand.next() < N_frac) {
			f.generate(generateSample());
		}
	}

	public class Coord {
		public double x;
		public double y;
		public double z;

		public Coord(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	public interface GenFunc {
		void generate(Coord c);
	}
}
