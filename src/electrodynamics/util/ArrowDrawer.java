package electrodynamics.util;

import java.awt.Graphics2D;

import com.jogamp.opengl.GL2;

public class ArrowDrawer {
	private static Vector3 ctr3 = new Vector3(0,0,0);
	private static Vector3 arrow3 = new Vector3(0,0,0);
	private static Vector3 tip1_3 = new Vector3(0,0,0);
	private static Vector3 tip2_3 = new Vector3(0,0,0);
	private static Vector3 body1_3 = new Vector3(0,0,0);
	private static Vector3 body2_3 = new Vector3(0,0,0);
	
	public static void drawArrow(GL2 gl, float x, float y, float z, float dx, float dy, float dz, Vector3 perspective, float arrowlength, float tipsize) {
		ctr3.x = x;
		ctr3.y = y;
		ctr3.z = z;

		arrow3.x = dx;
		arrow3.y = dy;
		arrow3.z = dz;

		arrow3.normalize();

		tip1_3.copy(arrow3);
		tip1_3.cross(perspective);
		tip1_3.normalize();
		tip2_3.copy(tip1_3);
		tip2_3.scalarmult(-1);
		
		tip1_3.addmult(arrow3, -2);
		tip2_3.addmult(arrow3, -2);
		
		body1_3.copy(ctr3);
		body2_3.copy(ctr3);
		body2_3.addmult(arrow3, arrowlength);
		
		tip1_3.scalarmult(tipsize);
		tip1_3.add(body2_3);
		tip2_3.scalarmult(tipsize);
		tip2_3.add(body2_3);

		gl.glVertex3f((float)body1_3.x, (float)body1_3.y, (float)body1_3.z);
		gl.glVertex3f((float)body2_3.x, (float)body2_3.y, (float)body2_3.z);

		gl.glVertex3f((float)body2_3.x, (float)body2_3.y, (float)body2_3.z);
		gl.glVertex3f((float)tip1_3.x, (float)tip1_3.y, (float)tip1_3.z);
		
		gl.glVertex3f((float)body2_3.x, (float)body2_3.y, (float)body2_3.z);
		gl.glVertex3f((float)tip2_3.x, (float)tip2_3.y, (float)tip2_3.z);
	}
	
	public static void drawCenteredArrow(GL2 gl, float x, float y, float z, float dx, float dy, float dz, Vector3 perspective, float arrowlength, float tipsize) {
		ctr3.x = x;
		ctr3.y = y;
		ctr3.z = z;

		arrow3.x = dx;
		arrow3.y = dy;
		arrow3.z = dz;

		arrow3.normalize();

		tip1_3.copy(arrow3);
		tip1_3.cross(perspective);
		tip1_3.normalize();
		tip2_3.copy(tip1_3);
		tip2_3.scalarmult(-1);
		
		tip1_3.addmult(arrow3, -2);
		tip2_3.addmult(arrow3, -2);
		
		body1_3.copy(ctr3);
		body1_3.addmult(arrow3, -0.5*arrowlength);
		body2_3.copy(ctr3);
		body2_3.addmult(arrow3, 0.5*arrowlength);
		
		tip1_3.scalarmult(tipsize);
		tip1_3.add(body2_3);
		tip2_3.scalarmult(tipsize);
		tip2_3.add(body2_3);

		gl.glVertex3f((float)body1_3.x, (float)body1_3.y, (float)body1_3.z);
		gl.glVertex3f((float)body2_3.x, (float)body2_3.y, (float)body2_3.z);

		gl.glVertex3f((float)body2_3.x, (float)body2_3.y, (float)body2_3.z);
		gl.glVertex3f((float)tip1_3.x, (float)tip1_3.y, (float)tip1_3.z);
		
		gl.glVertex3f((float)body2_3.x, (float)body2_3.y, (float)body2_3.z);
		gl.glVertex3f((float)tip2_3.x, (float)tip2_3.y, (float)tip2_3.z);
	}
	

	private static Vector ctr = new Vector(0,0);
	private static Vector arrow = new Vector(0,0);
	private static Vector tip1 = new Vector(0,0);
	private static Vector tip2 = new Vector(0,0);
	private static Vector body1 = new Vector(0,0);
	private static Vector body2 = new Vector(0,0);
	
	public static void drawArrow(Graphics2D g, float x, float y, float dx, float dy, float arrowlength, float tipsize) {
		ctr.x = x;
		ctr.y = y;

		arrow.x = dx;
		arrow.y = dy;

		arrow.normalize();
		tip1.copy(arrow);
		tip2.copy(arrow);
		tip1.rotate(Math.PI*5.0/6.0);
		tip2.rotate(Math.PI*7.0/6.0);

		body1.copy(ctr);
		body2.copy(ctr);
		body2.addmult(arrow, arrowlength);
		tip1.scalarmult(tipsize);
		tip1.add(body2);
		tip2.scalarmult(tipsize);
		tip2.add(body2);
		g.drawLine((int)body1.x, (int)body1.y, (int)body2.x, (int)body2.y);
		g.drawLine((int)body2.x, (int)body2.y, (int)tip1.x, (int)tip1.y);
		g.drawLine((int)body2.x, (int)body2.y, (int)tip2.x, (int)tip2.y);
	}
	

	public static void drawCenteredArrow(Graphics2D g, float x, float y, float dx, float dy, float arrowlength, float tipsize) {
		ctr.x = x;
		ctr.y = y;

		arrow.x = dx;
		arrow.y = dy;

		arrow.normalize();
		tip1.copy(arrow);
		tip2.copy(arrow);
		tip1.rotate(Math.PI*5.0/6.0);
		tip2.rotate(Math.PI*7.0/6.0);

		body1.copy(ctr);
		body1.addmult(arrow, -0.5*arrowlength);
		body2.copy(ctr);
		body2.addmult(arrow, 0.5*arrowlength);
		tip1.scalarmult(tipsize);
		tip1.add(body2);
		tip2.scalarmult(tipsize);
		tip2.add(body2);
		g.drawLine((int)body1.x, (int)body1.y, (int)body2.x, (int)body2.y);
		g.drawLine((int)body2.x, (int)body2.y, (int)tip1.x, (int)tip1.y);
		g.drawLine((int)body2.x, (int)body2.y, (int)tip2.x, (int)tip2.y);
	}
}
