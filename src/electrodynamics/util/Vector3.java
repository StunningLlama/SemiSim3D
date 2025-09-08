// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.util;

public class Vector3 {
	public double x;
	public double y;
	public double z;

	public Vector3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3 copy() {
		return new Vector3(x, y, z);
	}

	public void copy(Vector3 b) {
		x = b.x;
		y = b.y;
		z = b.z;
	}

	public void initialize(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void add(Vector3 b) {
		x += b.x;
		y += b.y;
		z += b.z;
	}

	public void scalarmult(double c) {
		x *= c;
		y *= c;
		z *= c;
	}

	public void addmult(Vector3 b, double c) {
		x += b.x * c;
		y += b.y * c;
		z += b.z * c;
	}

	public void rotate_z(double theta) {
		double xf = x*Math.cos(theta) + y*Math.sin(theta);
		double yf = -x*Math.sin(theta) + y*Math.cos(theta);
		x = xf;
		y = yf;
	}

	public void normalize() {
		double magnitude = Math.sqrt(x*x+y*y+z*z);
		if (magnitude != 0) {
			x /= magnitude;
			y /= magnitude;
			z /= magnitude;
		}
	}

	public double dot(Vector3 b) {
		return x * b.x + y * b.y + z * b.z;
	}

	public void cross(Vector3 b) {
		double ax = x;
		double ay = y;
		double az = z;

		x = ay*b.z - az*b.y;
		y = az*b.x - ax*b.z;
		z = ax*b.y - ay*b.x;
	}
}