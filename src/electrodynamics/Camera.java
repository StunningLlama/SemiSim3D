package electrodynamics;

import java.awt.event.KeyEvent;

import electrodynamics.Renderer.Perspective;

public class Camera {
	
	Perspective perspective;
	float pitch;
	float yaw;
    float zoom;
    float cam_x;
    float cam_y;
    float cam_z;
    float cx;
    float cy;
    float cz;
    float distance;
    
    public Camera(Perspective perspective) {
    	this.perspective = perspective;
    	resetCameraPos(1);
    }

	public void resetCameraPos(float distance) {
		if (perspective == Perspective.ORTHO)
			zoom = 1f;
		else
			zoom = 0.5f;
		
		pitch = -(float)Math.PI/6;
		yaw = (float)(Math.PI*5/4);
		this.distance = 3*distance;
	    cam_x = this.distance*(float)Math.sqrt(3/2.0)/2;
	    cam_y = this.distance*(float)Math.sqrt(3/2.0)/2;
	    cam_z = this.distance/2;
	}
	
	public void setPivotPoint(double cx, double cy, double cz) {
		this.cx = (float)cx;
		this.cy = (float)cy;
		this.cz = (float)cz;
	}
	
	public void normalizeDistance() {
		double x = cam_x - cx;
		double y = cam_y - cy;		
		double z = cam_z - cz;
		
		double mag = Math.sqrt(x*x+y*y+z*z);

		cam_x = (float)(cx + x/mag);
		cam_y = (float)(cy + y/mag);
		cam_z = (float)(cz + z/mag);
	}
	
	public void rotateView(double dphi, double dtheta) {
		pitch -= (float)dtheta;
		yaw -= (float)dphi;

		float max_pitch = (float)(Math.PI/2*0.999);
		if (pitch > max_pitch) pitch = max_pitch;
		if (pitch < -max_pitch) pitch = -max_pitch;

		double x = cam_x - cx;
		double y = cam_y - cy;		
		double z = cam_z - cz;
		
		double r = Math.sqrt(x*x+y*y+z*z);
		double theta = Math.asin(z/r);
		double phi = Math.atan2(y, x);

		theta += (float)dtheta;
		phi -= (float)dphi;

		if (theta > max_pitch) theta = max_pitch;
		if (theta < -max_pitch) theta = -max_pitch;

		double xp = r*Math.cos(phi)*Math.cos(theta);
		double yp = r*Math.sin(phi)*Math.cos(theta);
		double zp = r*Math.sin(theta);
		
		cam_x = (float)(cx + xp);
		cam_y = (float)(cy + yp);
		cam_z = (float)(cz + zp);
	}
	
	public void rotateCamera(double dphi, double dtheta) {
		pitch += (float)dtheta;
		yaw += (float)dphi;

		if (pitch > Math.PI/2) pitch = (float)Math.PI/2;
		if (pitch < -Math.PI/2) pitch = -(float)Math.PI/2;

	}
	
	public void processKey(double speed, double framerate) {
		double nx = Math.cos(yaw);
		double ny = Math.sin(yaw);
		double movefactor = speed/framerate;
		double lookfactor = 1/framerate;

		if (perspective == Perspective.ORTHO) {
			if (Keyboard.isKeyPressed(KeyEvent.VK_W)) {
				cam_x -= nx*movefactor*Math.sin(pitch);
				cam_y -= ny*movefactor*Math.sin(pitch);
				cam_z += movefactor*Math.cos(pitch);
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_S)) {
				cam_x += nx*movefactor*Math.sin(pitch);
				cam_y += ny*movefactor*Math.sin(pitch);
				cam_z -= movefactor*Math.cos(pitch);
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_A)) {
				cam_x -= ny*movefactor;
				cam_y += nx*movefactor;
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_D)) {
				cam_x += ny*movefactor;
				cam_y -= nx*movefactor;
			}
			//normalizeDistance();
		} else {
			if (Keyboard.isKeyPressed(KeyEvent.VK_W)) {
				cam_x += nx*movefactor;
				cam_y += ny*movefactor;
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_S)) {
				cam_x -= nx*movefactor;
				cam_y -= ny*movefactor;
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_A)) {
				cam_x -= ny*movefactor;
				cam_y += nx*movefactor;
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_D)) {
				cam_x += ny*movefactor;
				cam_y -= nx*movefactor;
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_Q)) {
				cam_z -= 1*movefactor;
			}
			if (Keyboard.isKeyPressed(KeyEvent.VK_E)) {
				cam_z += 1*movefactor;
			}
		}

		if (Keyboard.isKeyPressed(KeyEvent.VK_LEFT)) {
			rotateCamera(lookfactor, 0);
		}
		if (Keyboard.isKeyPressed(KeyEvent.VK_RIGHT)) {
			rotateCamera(-lookfactor, 0);
		}
		if (Keyboard.isKeyPressed(KeyEvent.VK_UP)) {
			rotateCamera(0, lookfactor);
		}
		if (Keyboard.isKeyPressed(KeyEvent.VK_DOWN)) {
			rotateCamera(0, -lookfactor);
		}
	}
}
