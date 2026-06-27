package electrodynamics;

public class Material implements Cloneable {
	MaterialType type = MaterialType.VACUUM;

	boolean modified = false;
	int activated = 1;
	int conducting = 0;
	int semiconducting = 0;
	double emf;
	int emf_x = 0;			// EMF strength
	int emf_y = 0;			// EMF strength
	int emf_z = 0;			// EMF strength
	double eps_r = 1.0;			// Permittivity
	double mu_r = 1.0;			// Permeability
	double rho_back = 0.0;		// Background charge density
	double ni = 0;				// Equilibrium carrier density
	double W = 0;				// Work function
	double Eb = 0;				// Bandgap
	double Ea = 0;				// Recombination activation energy
	double absorptivity = 0.0;

	public void erase() {
		type = MaterialType.VACUUM;
		modified = false;
		activated = 1;
		conducting = 0;
		semiconducting = 0;
		emf = 0;
		emf_x = 0;
		emf_y = 0;
		emf_z = 0;
		eps_r = 1.0;
		mu_r = 1.0;
		rho_back = 0.0;
		ni = 0;
		W = 0;
		Eb = 0;
		Ea = 0;
		absorptivity = 0;
	}

	@Override
	public Material clone() {
		try {
			return (Material) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}