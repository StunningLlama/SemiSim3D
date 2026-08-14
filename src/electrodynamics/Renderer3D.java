// Copyright (c) Brandon Li 2025
// This file is part of Brandon's Electromagnetic Simulation which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Random;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.TextRenderer;

import electrodynamics.Controls.Brush;
import electrodynamics.Renderer.ChargeCarrierDot;
import electrodynamics.Renderer.Dot;
import electrodynamics.Renderer.DotType;
import electrodynamics.Renderer.RenderMode;
import electrodynamics.Renderer.Text;
import electrodynamics.Renderer.VectorMode;
import electrodynamics.Renderer.VectorView;
import electrodynamics.util.ArrowDrawer;
import electrodynamics.util.Device;
import electrodynamics.util.Utils;
import electrodynamics.util.Vector3;

public class Renderer3D implements GLEventListener {

	Simulation e;
	public GLCanvas canvas;
	IntBuffer selectBuf;
	GLU glu;
	IntBuffer viewport;

	Vector3 g = new Vector3(0, 0, 0);

	float aspect = 1;
	float eye_offset = 0;
	int width;
	int height;
	boolean isMainCanvas = false;
	boolean isOrtho = false;
	boolean pick = true;
	Random rand = new Random();

	TextRenderer smallFont;
	TextRenderer bigFont;
	FPSAnimator animator;

	ArrayList<Text> fg_texts = new ArrayList<>();
	ArrayList<Text> bg_texts = new ArrayList<>();
	ArrayList<Text> ui_texts = new ArrayList<>();

	public Renderer3D(Simulation e) {
		this.e = e;
		GLProfile profile = GLProfile.get(GLProfile.GL2);
		GLCapabilities capabilities = new GLCapabilities(profile);

		// Create canvas
		canvas = new GLCanvas(capabilities);
		canvas.addGLEventListener(this);
		canvas.setSize(768, 768);

		animator = new FPSAnimator(canvas, (int)e.renderer.targetframerate);
	}

	IntBuffer pick_fbo = GLBuffers.newDirectIntBuffer(1);
	IntBuffer pick_dbo = GLBuffers.newDirectIntBuffer(1);
	IntBuffer pick_texture = GLBuffers.newDirectIntBuffer(1);
	@Override
	public void init(GLAutoDrawable drawable) {
		glu = GLU.createGLU();
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(0f, 0f, 0f, 1f);  // Black background
		gl.glEnable(GL2.GL_DEPTH_TEST);
		selectBuf = Buffers.newDirectIntBuffer(256);
		viewport = Buffers.newDirectIntBuffer(4);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport);
		int font_scale = Device.isOnRetinaDisplay(canvas) ? 2:1;
		smallFont = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12/font_scale), true, true);
		bigFont = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 16/font_scale), true, true);
		
		setupPick(gl);
	}
	
	public void setupPick(GL2 gl) {
		gl.glGenFramebuffers(1, pick_fbo);
		gl.glGenTextures(1, pick_texture);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, pick_texture.get(0));

		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, 1, 1, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, ByteBuffer.allocate(1 * 1 * 4));

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_BASE_LEVEL, 0);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, 0); 

		gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, pick_fbo.get(0));
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, pick_fbo.get(0));
		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_2D, pick_texture.get(0), 0);
		gl.glDrawBuffer(GL2.GL_COLOR_ATTACHMENT0);


		gl.glGenRenderbuffers(1, pick_dbo);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, pick_dbo.get(0));
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, 1, 1);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, pick_dbo.get(0));

		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

		if(gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER) == GL2.GL_FRAMEBUFFER_COMPLETE)
			System.out.println("FB creation complete!");
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// Called when the canvas is destroyed
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		if (!SemiSim.ready)
			return;

		if (!e.renderer.threeD_mode)
			return;
		
		if (isMainCanvas) e.renderer.t5.start();

		boolean locked = e.rwLock.readLock().tryLock();
		if (!locked) {
			if (isMainCanvas) e.renderer.t5.stop();
			return;
		}
		try {
			e.renderer.drawPixels();
			e.renderer.drawOverlay(false);

			if (isMainCanvas && e.opts.gui_rotate.isSelected()) {
				e.controls.rotateView(1f/e.renderer.targetframerate, 0);
			}

			GL2 gl = drawable.getGL().getGL2();

			gl.glEnable(GL2.GL_DEPTH_TEST);
			gl.glClearColor(e.canvas.bg.getRed()/255f, e.canvas.bg.getGreen()/255f, e.canvas.bg.getBlue()/255f, 1.0f);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

			// Draw scene

			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			setupProjectionMat(gl);

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			setupModelMat(gl);

			drawScene(gl);

			// Draw text

			drawTexts(gl);

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			setupModelMat(gl);

			// Pick

			if (pick) {
				gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, pick_fbo.get(0));

				gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport);

				gl.glEnable(GL2.GL_DEPTH_TEST);
				gl.glEnable(GL2.GL_BLEND);
				gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
				gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);


				gl.glMatrixMode(GL2.GL_PROJECTION);
				gl.glLoadIdentity();
				glu.gluPickMatrix(((float)e.controls.mx_screen)*width/canvas.getWidth(), (canvas.getHeight()-(float)e.controls.my_screen)*height/canvas.getHeight(), 1, 1, viewport);
				setupProjectionMat(gl);

				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glLoadIdentity();
				setupModelMat(gl);

				drawHitboxes(gl);

				gl.glReadBuffer(GL2.GL_COLOR_ATTACHMENT0);
				IntBuffer output = Buffers.newDirectIntBuffer(4);
				gl.glReadPixels(0, 0, 1, 1, GL2.GL_RGBA, GL2.GL_INT, output);

				int index = ((int)Math.round(255*((double)output.get(0)/(double)2147483647)) << 16)
						+ ((int)Math.round(255*((double)output.get(1)/(double)2147483647)) << 8)
						+ ((int)Math.round(255*((double)output.get(2)/(double)2147483647)))
						+ ((int)Math.round(255*((double)output.get(3)/(double)2147483647)) << 24);
				
				//System.out.println(index);
				
				if (index > 0 && e.controls.update3dCursor) {
					this.unpackCoords(index);
					e.controls.update3dCursor = false;
				}
				
				e.controls.mouse_in_bounds = index > 0;

				gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			}

			gl.glFlush();
		}

		finally {
			e.rwLock.readLock().unlock();
		}

		if (isMainCanvas) e.renderer.t5.stop();

		if (isMainCanvas) e.renderer.FPStimer.stop();
		if (isMainCanvas) e.renderer.FPStimer.start();
	}


	public void addRectangle(GL2 gl, float x, float y, float w, float h, float z) {
		float x1 = 2*(x/canvas.getWidth())-1;
		float x2 = 2*((x+w)/canvas.getWidth())-1;
		float y1 = 1-2*((y-2)/canvas.getHeight());
		float y2 = 1-2*((y+h-2)/canvas.getHeight());
		gl.glVertex3f(x1, y1, z);
		gl.glVertex3f(x1, y2, z);
		gl.glVertex3f(x2, y1, z);
		gl.glVertex3f(x1, y2, z);
		gl.glVertex3f(x2, y1, z);
		gl.glVertex3f(x2, y2, z);
	}

	public void setupProjectionMat(GL2 gl) {
		RenderMode mode = e.controls.rendermode.getOption();
		isOrtho = RenderMode.isOrthographic(mode);

		if (isOrtho)
			gl.glOrtho(-e.renderer.scale * aspect, e.renderer.scale * aspect, -e.renderer.scale, e.renderer.scale, 0, 128);
		else if (RenderMode.isPerspective(mode))
			gl.glFrustum(-0.01*e.renderer.scale * aspect, 0.01*e.renderer.scale * aspect, -0.01*e.renderer.scale, 0.01*e.renderer.scale, 0.01*64, 4*64);

		gl.glRotatef(90f, 0.0f, 0.0f, 1.0f);
		gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);
	}

	public void setupModelMat(GL2 gl) {

		g.x = Math.cos(e.renderer.yaw)*Math.cos(e.renderer.pitch);
		g.y = Math.sin(e.renderer.yaw)*Math.cos(e.renderer.pitch);
		g.z = Math.sin(e.renderer.pitch);

		//gl.glTranslatef(48, 0, 0);
		gl.glRotatef(-eye_offset, 0.0f, 0.0f, 1.0f);
		gl.glRotatef(e.renderer.pitch*(float)(180/Math.PI), 0.0f, 1.0f, 0.0f);
		gl.glRotatef(-e.renderer.yaw*(float)(180/Math.PI), 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(-e.renderer.cam_x, -e.renderer.cam_y, -e.renderer.cam_z);
	}

	public void setupText(GL2 gl, float x, float y, float z) {
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		//gl.glTranslatef(48, 0, 0);
		gl.glRotatef(-eye_offset, 0.0f, 0.0f, 1.0f);
		gl.glRotatef(e.renderer.pitch*(float)(180/Math.PI), 0.0f, 1.0f, 0.0f);
		gl.glRotatef(-e.renderer.yaw*(float)(180/Math.PI), 0.0f, 0.0f, 1.0f);
		gl.glTranslatef(-e.renderer.cam_x, -e.renderer.cam_y, -e.renderer.cam_z);

		gl.glTranslatef(x, y, z);
		gl.glRotatef(e.renderer.yaw*(float)(180/Math.PI), 0.0f, 0.0f, 1.0f);
		gl.glRotatef(-e.renderer.pitch*(float)(180/Math.PI), 0.0f, 1.0f, 0.0f);
		gl.glRotatef(eye_offset, 0.0f, 0.0f, 1.0f);
		gl.glRotatef((float)(90), 1.0f, 0.0f, 0.0f);
		gl.glRotatef((float)(-90), 0.0f, 1.0f, 0.0f);
		gl.glTranslatef(-x, -y, -z);
	}

	public void drawScene(GL2 gl) {
		gl.glEnable(GL2.GL_BLEND);
		gl.glDepthMask(true);
		gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ONE);

		// Draw a triangle

		//System.out.println("Here3");

		RenderMode mode = e.controls.rendermode.getOption();

		if (mode != RenderMode.THREED_FIELDS_ONLY && mode != RenderMode.THREED_PERSPECTIVE_FIELDS_ONLY) {
			float alpha = 1f;

			if (mode == RenderMode.THREED_TRANSLUCENT || mode == RenderMode.THREED_PERSPECTIVE_TRANSLUCENT) {
				alpha = 0.3f;
				gl.glDepthMask(false);
				//gl.glBlendEquationSeparate(GL2.GL_ADD, GL2.GL_ADD);
				gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE);
				//gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			}

			gl.glBegin(GL2.GL_TRIANGLES);

			for (int i = 0; i < e.nx; i++) for (int j = 0; j < e.ny; j++) for (int k = 0; k < e.nz; k++) {
				for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) for (int dk = -1; dk <= 1; dk++) {
					if (Math.abs(di)+Math.abs(dj)+Math.abs(dk) == 1
							&& i+di >= 0 && i + di < e.nx
							&& j+dj >= 0 && j + dj < e.ny
							&& k+dk >= 0 && k + dk < e.nz) {
						if (e.materials[i][j][k].type == MaterialType.ABSORBER && !surfVisible(i, j, k, di, dj, dk))
							continue;

						if (e.renderer.opaque[i][j][k] && !e.renderer.opaque[i+di][j+dj][k+dk]) {
							//float r = e.materials[i][j][k].type.color_r/255f;
							//float g = e.materials[i][j][k].type.color_g/255f;
							//float b = e.materials[i][j][k].type.color_b/255f;
							float r = e.renderer.image_r[i][j][k];
							float g = e.renderer.image_g[i][j][k];
							float b = e.renderer.image_b[i][j][k];
							float light = (float)(0.8+0.2*di+0.1*dj+0.05*dk);
							gl.glColor4f(r*light, g*light, b*light, alpha);
							if (di == -1) {
								gl.glVertex3f(i, j, k);
								gl.glVertex3f(i, j+1, k);
								gl.glVertex3f(i, j+1, k+1);
								gl.glVertex3f(i, j, k);
								gl.glVertex3f(i, j, k+1);
								gl.glVertex3f(i, j+1, k+1);
							} else if (di == 1) {
								gl.glVertex3f(i+1, j, k);
								gl.glVertex3f(i+1, j+1, k);
								gl.glVertex3f(i+1, j+1, k+1);
								gl.glVertex3f(i+1, j, k);
								gl.glVertex3f(i+1, j, k+1);
								gl.glVertex3f(i+1, j+1, k+1);
							} else if (dj == -1) {
								gl.glVertex3f(i, j, k);
								gl.glVertex3f(i+1, j, k);
								gl.glVertex3f(i+1, j, k+1);
								gl.glVertex3f(i, j, k);
								gl.glVertex3f(i, j, k+1);
								gl.glVertex3f(i+1, j, k+1);
							} else if (dj == 1) {
								gl.glVertex3f(i, j+1, k);
								gl.glVertex3f(i+1, j+1, k);
								gl.glVertex3f(i+1, j+1, k+1);
								gl.glVertex3f(i, j+1, k);
								gl.glVertex3f(i, j+1, k+1);
								gl.glVertex3f(i+1, j+1, k+1);
							} else if (dk == -1) {
								gl.glVertex3f(i, j, k);
								gl.glVertex3f(i, j+1, k);
								gl.glVertex3f(i+1, j+1, k);
								gl.glVertex3f(i, j, k);
								gl.glVertex3f(i+1, j, k);
								gl.glVertex3f(i+1, j+1, k);
							} else if (dk == 1) {
								gl.glVertex3f(i, j, k+1);
								gl.glVertex3f(i, j+1, k+1);
								gl.glVertex3f(i+1, j+1, k+1);
								gl.glVertex3f(i, j, k+1);
								gl.glVertex3f(i+1, j, k+1);
								gl.glVertex3f(i+1, j+1, k+1);
							}
						}
					}
				}
			}


			for (int i = 0; i < e.nx; i++) for (int j = 0; j < e.ny; j++) for (int k = 0; k < e.nz; k++) {
				for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) for (int dk = -1; dk <= 1; dk++) {
					if (Math.abs(di)+Math.abs(dj)+Math.abs(dk) == 1
							&& i+di >= 0 && i + di < e.nx
							&& j+dj >= 0 && j + dj < e.ny
							&& k+dk >= 0 && k + dk < e.nz) {
						if (!surfVisible(i, j, k, di, dj, dk))
							continue;

						if (e.renderer.translucent[i][j][k] && !e.renderer.translucent[i+di][j+dj][k+dk]) {
							float r = e.renderer.image_r[i][j][k];
							float g = e.renderer.image_g[i][j][k];
							float b = e.renderer.image_b[i][j][k];
							float light = (float)(0.8+0.2*di+0.1*dj+0.05*dk);

							gl.glColor4f(r*light, g*light, b*light, 0.4f);
							float eps = 0.01f;
							if (di == -1) {
								gl.glVertex3f(i-eps, j-eps, k-eps);
								gl.glVertex3f(i-eps, j+1+eps, k-eps);
								gl.glVertex3f(i-eps, j+1+eps, k+1+eps);
								gl.glVertex3f(i-eps, j-eps, k-eps);
								gl.glVertex3f(i-eps, j-eps, k+1+eps);
								gl.glVertex3f(i-eps, j+1+eps, k+1+eps);
							} else if (di == 1) {
								gl.glVertex3f(i+1+eps, j-eps, k-eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k-eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k+1+eps);
								gl.glVertex3f(i+1+eps, j-eps, k-eps);
								gl.glVertex3f(i+1+eps, j-eps, k+1+eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k+1+eps);
							} else if (dj == -1) {
								gl.glVertex3f(i-eps, j-eps, k-eps);
								gl.glVertex3f(i+1+eps, j-eps, k-eps);
								gl.glVertex3f(i+1+eps, j-eps, k+1+eps);
								gl.glVertex3f(i-eps, j-eps, k-eps);
								gl.glVertex3f(i-eps, j-eps, k+1+eps);
								gl.glVertex3f(i+1+eps, j-eps, k+1+eps);
							} else if (dj == 1) {
								gl.glVertex3f(i-eps, j+1+eps, k-eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k-eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k+1+eps);
								gl.glVertex3f(i-eps, j+1+eps, k-eps);
								gl.glVertex3f(i-eps, j+1+eps, k+1+eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k+1+eps);
							} else if (dk == -1) {
								gl.glVertex3f(i-eps, j-eps, k-eps);
								gl.glVertex3f(i-eps, j+1+eps, k-eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k-eps);
								gl.glVertex3f(i-eps, j-eps, k-eps);
								gl.glVertex3f(i+1+eps, j-eps, k-eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k-eps);
							} else if (dk == 1) {
								gl.glVertex3f(i-eps, j-eps, k+1+eps);
								gl.glVertex3f(i-eps, j+1+eps, k+1+eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k+1+eps);
								gl.glVertex3f(i-eps, j-eps, k+1+eps);
								gl.glVertex3f(i+1+eps, j-eps, k+1+eps);
								gl.glVertex3f(i+1+eps, j+1+eps, k+1+eps);
							}
						}
					}
				}
			}

			gl.glEnd();
		}


		if (e.controls.vectorview.getOption() != VectorView.NONE) {

			double arrowlength = 2;

			VectorMode vector_display_mode = e.controls.vectormode.getOption();

			double vectorscalingconstant = Math.pow(10.0, e.opts.gui_brightness_vec.getValue()/5.0)/(e.controls.vectorview.getOption()).getScalingConstant(e);


			rand.setSeed(4);

			int density = 10;

			double randomness = 0;

			if (vector_display_mode == VectorMode.ARROWS) {
				randomness = 0.75;
			} else if (vector_display_mode == VectorMode.LINES) {
				randomness = 0.75;
			}


			VectorView synchronized_vector_view = e.controls.vectorview.getOption();
			double[][][][] vf = {null, null, null};
			e.computeVectorField(vf, synchronized_vector_view);
			double grid_offset = synchronized_vector_view.getGridOffset();
			double dual_offset = synchronized_vector_view.getDualOffset();

			double[][][] vf_x = vf[0];
			double[][][] vf_y = vf[1];
			double[][][] vf_z = vf[2];

			if (vector_display_mode == VectorMode.LINES && vf_x != null) {

				gl.glDepthMask(false);
				gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE);
				gl.glLineWidth(3f);
				gl.glBegin(GL2.GL_LINES);

				for (int i = 0; i <= density; i++) {
					for (int j = 0; j <= density; j++) {
						for (int k = 0; k <= density; k++) {
							double x = (e.nx-1)*(i+randomness*(rand.nextFloat()-0.5))/density;
							double y = (e.ny-1)*(j+randomness*(rand.nextFloat()-0.5))/density;
							double z = (e.nz-1)*(k+randomness*(rand.nextFloat()-0.5))/density;
							for (int sign = -1; sign <= 1; sign += 2) {

								double prevx = x;
								double prevy = y;
								double prevz = z;
								double dx = 0;
								double dy = 0;
								double dz = 0;

								int steps = 20;

								for (int m = 0; m < steps; m++) {
									dx = Utils.bilinearinterp(vf_x, prevx+grid_offset, prevy+dual_offset, prevz+dual_offset);
									dy = Utils.bilinearinterp(vf_y, prevx+dual_offset, prevy+grid_offset, prevz+dual_offset);
									dz = Utils.bilinearinterp(vf_z, prevx+dual_offset, prevy+dual_offset, prevz+grid_offset);

									double fieldmagnitude = Math.sqrt(dx*dx+dy*dy+dz*dz);
									double w = Math.min(vectorscalingconstant*fieldmagnitude, 1);
									float alpha = (float)((1.0/steps)*(steps-m)*w);

									if (fieldmagnitude != 0) {
										dx /= fieldmagnitude;
										dy /= fieldmagnitude;
										dz /= fieldmagnitude;
									}


									double nextx = prevx + dx*arrowlength*0.25*sign;
									double nexty = prevy + dy*arrowlength*0.25*sign;
									double nextz = prevz + dz*arrowlength*0.25*sign;

									if (nextx < 1 || nexty < 1 || nextz < 1 || nextx > e.nx-2 || nexty > e.ny-2 || nextz > e.nz-2)
										break;

									gl.glColor4f(1f, 1f, 1f, alpha);
									gl.glVertex3f((float)prevx + 0.5f, (float)prevy + 0.5f, (float)prevz + 0.5f);
									gl.glVertex3f((float)nextx + 0.5f, (float)nexty + 0.5f, (float)nextz + 0.5f);

									prevx = nextx;
									prevy = nexty;
									prevz = nextz;
								}
							}
						}
					}
				}

				gl.glEnd();
			} else if (vector_display_mode == VectorMode.ARROWS && vf_x != null) {

				Vector3 ctr = new Vector3(0,0,0);
				Vector3 arrow = new Vector3(0,0,0);
				Vector3 tip1 = new Vector3(0,0,0);
				Vector3 tip2 = new Vector3(0,0,0);
				Vector3 body1 = new Vector3(0,0,0);
				Vector3 body2 = new Vector3(0,0,0);
				Vector3 o = new Vector3(-e.nx/2.0,-e.ny/2.0,-e.nz/2.0);

				gl.glDepthMask(false);
				gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE);
				gl.glLineWidth(3f);
				gl.glBegin(GL2.GL_LINES);

				for (int i = 0; i <= density; i++) {
					for (int j = 0; j <= density; j++) {
						for (int k = 0; k <= density; k++) {
							double x = (e.nx-1)*(i+randomness*(rand.nextFloat()-0.5))/density;
							double y = (e.ny-1)*(j+randomness*(rand.nextFloat()-0.5))/density;
							double z = (e.nz-1)*(k+randomness*(rand.nextFloat()-0.5))/density;
							ctr.x = x+0.5;
							ctr.y = y+0.5;
							ctr.z = z+0.5;

							arrow.x = Utils.bilinearinterp(vf_x, x+grid_offset, y+dual_offset, z+dual_offset);
							arrow.y = Utils.bilinearinterp(vf_y, x+dual_offset, y+grid_offset, z+dual_offset);
							arrow.z = Utils.bilinearinterp(vf_z, x+dual_offset, y+dual_offset, z+grid_offset);

							double fieldmagnitude = Math.max(0.1, vectorscalingconstant*Math.sqrt(arrow.dot(arrow)));
							arrow.normalize();

							tip1.copy(arrow);
							tip1.cross(g);
							tip1.normalize();
							tip2.copy(tip1);
							tip2.scalarmult(-1);

							tip1.addmult(arrow, -2);
							tip2.addmult(arrow, -2);

							body1.copy(ctr);
							body1.addmult(arrow, -0.5*arrowlength);
							body2.copy(ctr);
							body2.addmult(arrow, 0.5*arrowlength);

							tip1.scalarmult(0.1*arrowlength);
							tip1.add(body2);
							tip2.scalarmult(0.1*arrowlength);
							tip2.add(body2);

							ctr.add(o);
							double depth = ctr.dot(g);
							float gray = 0.5f*(float)Math.exp(-depth/16.0);
							float alpha = (float)(0.1*Math.sqrt(fieldmagnitude));

							gl.glColor4f(gray, gray, gray, alpha);

							gl.glVertex3f((float)body1.x, (float)body1.y, (float)body1.z);
							gl.glVertex3f((float)body2.x, (float)body2.y, (float)body2.z);

							gl.glVertex3f((float)body2.x, (float)body2.y, (float)body2.z);
							gl.glVertex3f((float)tip1.x, (float)tip1.y, (float)tip1.z);

							gl.glVertex3f((float)body2.x, (float)body2.y, (float)body2.z);
							gl.glVertex3f((float)tip2.x, (float)tip2.y, (float)tip2.z);
						}
					}
				}

				gl.glEnd();
			} else if (vector_display_mode == VectorMode.DOTS) {
				gl.glEnable(GL2.GL_BLEND);
				gl.glDepthMask(true);
				gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ONE);
				gl.glBegin(GL2.GL_TRIANGLES);

				gl.glColor4f(1, 1, 1, 1);

				for (Dot d : e.renderer.dots) {
					float radius = 0.15f;
					float alpha = (float)d.brightness;
					drawDot(gl, (float)d.x-radius+0.5f, (float)d.y-radius+0.5f, (float)d.z-radius+0.5f, (float)d.x+radius+0.5f, (float)d.y+radius+0.5f, (float)d.z+radius+0.5f, 0.85f, 0.85f, 0.85f, alpha);
				}

				gl.glEnd();
			}

			if (e.opts.gui_carriers.isSelected()) {
				gl.glEnable(GL2.GL_BLEND);
				gl.glDepthMask(true);
				gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ONE);
				gl.glBegin(GL2.GL_TRIANGLES);
				for (int i = 0; i < e.renderer.ccdots.size(); i++) {
					ChargeCarrierDot d = e.renderer.ccdots.get(i);
					float radius = 0.15f;
					float alpha = (float)d.brightness;
					if (d.type == DotType.ELECTRON)
						drawDot(gl, (float)d.x-radius+0.5f, (float)d.y-radius+0.5f, (float)d.z-radius+0.5f, (float)d.x+radius+0.5f, (float)d.y+radius+0.5f, (float)d.z+radius+0.5f, 0.25f, 0.25f, 1f, alpha);
					else if (d.type == DotType.HOLE)
						drawDot(gl, (float)d.x-radius+0.5f, (float)d.y-radius+0.5f, (float)d.z-radius+0.5f, (float)d.x+radius+0.5f, (float)d.y+radius+0.5f, (float)d.z+radius+0.5f, 1f, 0.25f, 0.25f, alpha);
					else if (d.type == DotType.GENERATION) {
						drawDot(gl, (float)d.x-radius+0.5f, (float)d.y-radius+0.5f, (float)d.z-radius+0.5f, (float)d.x+radius+0.5f, (float)d.y+radius+0.5f, (float)d.z+radius+0.5f, 0f, 0f, 0f, alpha);
					} else if (d.type == DotType.RECOMBINATION) {
						drawDot(gl, (float)d.x-radius+0.5f, (float)d.y-radius+0.5f, (float)d.z-radius+0.5f, (float)d.x+radius+0.5f, (float)d.y+radius+0.5f, (float)d.z+radius+0.5f, 1f, 1f, 1f, alpha);
					}
				}

				gl.glEnd();
			}
		}

		gl.glDepthMask(false);
		gl.glBlendFuncSeparate(GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_ZERO);
		gl.glLineWidth(3f);
		gl.glBegin(GL2.GL_LINES);

		if (e.opts.menu_axes.isSelected()) {
			gl.glColor4f(1, 0, 0, 1);
			ArrowDrawer.drawArrow(gl, 0, 0, 0, 1, 0, 0, g, 4, 0.1f*4f);
			gl.glColor4f(0, 1, 0, 1);
			ArrowDrawer.drawArrow(gl, 0, 0, 0, 0, 1, 0, g, 4, 0.1f*4f);
			gl.glColor4f(0, 0, 1, 1);
			ArrowDrawer.drawArrow(gl, 0, 0, 0, 0, 0, 1, g, 4, 0.1f*4f);
		}

		gl.glEnd();

		gl.glDepthMask(true);
	}

	public void drawTexts(GL2 gl) {
		bg_texts.clear();
		fg_texts.clear();
		ui_texts.clear();
		e.renderer.drawText(null);

		for (Text t : e.renderer.texts) {
			if (t.is3D)
				fg_texts.add(t);
			else
				ui_texts.add(t);
		}

		if (e.opts.menu_axes.isSelected()) {
			bg_texts.add(new Text("x", 3, 0, 0));
			bg_texts.add(new Text("y", 0, 3, 0));
			bg_texts.add(new Text("z", 0, 0, 3));
		}

		for (Text t : bg_texts)
			t.is3D = true;

		for (Text text : bg_texts) {
			if (text.isBig) {
				if (text.is3D) {
					setupText(gl, text.x+0.5f, text.y+0.5f, text.z+0.5f);
					bigFont.begin3DRendering();
					bigFont.setColor(Color.WHITE);
					bigFont.draw3D(text.text, text.x+0.5f, text.y+0.5f, text.z+0.5f, 0.08f);
					bigFont.end3DRendering();
				}
			}
		}

		for (Text text : bg_texts) {
			if (!text.isBig) {
				if (text.is3D) {
					setupText(gl, text.x+0.5f, text.y+0.5f, text.z+0.5f);
					smallFont.begin3DRendering();
					smallFont.setColor(Color.WHITE);
					smallFont.draw3D(text.text, text.x+0.5f, text.y+0.5f, text.z+0.5f, 0.08f);
					smallFont.end3DRendering();
				}
			}
		}

		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

		for (Text text : fg_texts) {
			if (text.isBig) {
				if (text.is3D) {
					setupText(gl, text.x+0.5f, text.y+0.5f, text.z+0.5f);
					bigFont.begin3DRendering();
					bigFont.setColor(Color.WHITE);
					bigFont.draw3D(text.text, text.x+0.5f, text.y+0.5f, text.z+0.5f, 0.08f);
					bigFont.end3DRendering();
				}
			}
		}

		for (Text text : fg_texts) {
			if (!text.isBig) {
				if (text.is3D) {
					setupText(gl, text.x+0.5f, text.y+0.5f, text.z+0.5f);
					smallFont.begin3DRendering();
					smallFont.setColor(Color.WHITE);
					smallFont.draw3D(text.text, text.x+0.5f, text.y+0.5f, text.z+0.5f, 0.08f);
					smallFont.end3DRendering();
				}
			}
		}

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		gl.glDisable(GL2.GL_BLEND);

		for (Text text : ui_texts) {
			Rectangle2D bounds;
			if (text.isBig) 
				bounds = bigFont.getBounds(text.text);
			else
				bounds = smallFont.getBounds(text.text);

			text.width = (int)bounds.getWidth();
			text.height = (int)bounds.getHeight();

			text.y += text.height + 2;

			if (text.isRightJustified) text.x -= (text.width+3);
			if (text.isBottomJustified) text.y += (text.height+5);
			if (text.isHorizontalCentered) text.x -= (text.width/2-3);
			if (text.isVerticalCentered) text.y += (text.height/2+2);
		}

		if (e.opts.menu_text_bg.isSelected())
		{
			gl.glBegin(GL2.GL_TRIANGLES);
			for (int pass = 1; pass <= 2; pass++) {
				for (Text text : ui_texts) {
					if (text.hasBackground) {
						Rectangle2D bounds;
						if (text.isBig) 
							bounds = bigFont.getBounds(text.text);
						else
							bounds = smallFont.getBounds(text.text);
						int width = (int)Math.max(text.minwidth, bounds.getWidth()+8);
						int height = (int)bounds.getHeight()+4;
						int x = text.x-3;
						int y = text.y-height+6;

						if (pass == 1) {
							gl.glColor3f(0.5f, 0.5f, 0.5f);
							addRectangle(gl, x-2, y-2, width+4, height+4, 0);
						} else if (pass == 2) {
							gl.glColor3f(0, 0, 0);
							addRectangle(gl, x, y, width, height, -1);
						}
					}
				}
			}
			gl.glEnd();
		}

		bigFont.beginRendering(canvas.getWidth(), canvas.getHeight());

		for (Text text : ui_texts) {
			if (text.isBig && !text.is3D) {
				bigFont.setColor(Color.WHITE);
				bigFont.draw(text.text, text.x, canvas.getHeight()-(text.y));
			}
		}

		bigFont.endRendering();

		smallFont.beginRendering(canvas.getWidth(), canvas.getHeight());

		for (Text text : ui_texts) {
			if (!text.isBig && !text.is3D) {
				smallFont.setColor(Color.WHITE);
				smallFont.draw(text.text, text.x, canvas.getHeight()-(text.y));
			}
		}

		smallFont.endRendering();
	}

	public void drawDot(GL2 gl, float i, float j, float k, float i2, float j2, float k2, float r, float g, float b, float alpha) {
		float light = 0.8f-0.2f;
		gl.glColor4f(r*light, g*light, b*light, alpha);
		gl.glVertex3f(i, j, k);
		gl.glVertex3f(i, j2, k);
		gl.glVertex3f(i, j2, k2);
		gl.glVertex3f(i, j, k);
		gl.glVertex3f(i, j, k2);
		gl.glVertex3f(i, j2, k2);

		light = 0.8f+0.2f;
		gl.glColor4f(r*light, g*light, b*light, alpha);
		gl.glVertex3f(i2, j, k);
		gl.glVertex3f(i2, j2, k);
		gl.glVertex3f(i2, j2, k2);
		gl.glVertex3f(i2, j, k);
		gl.glVertex3f(i2, j, k2);
		gl.glVertex3f(i2, j2, k2);

		light = 0.8f-0.1f;
		gl.glColor4f(r*light, g*light, b*light, alpha);
		gl.glVertex3f(i, j, k);
		gl.glVertex3f(i2, j, k);
		gl.glVertex3f(i2, j, k2);
		gl.glVertex3f(i, j, k);
		gl.glVertex3f(i, j, k2);
		gl.glVertex3f(i2, j, k2);

		light = 0.8f+0.1f;
		gl.glColor4f(r*light, g*light, b*light, alpha);
		gl.glVertex3f(i, j2, k);
		gl.glVertex3f(i2, j2, k);
		gl.glVertex3f(i2, j2, k2);
		gl.glVertex3f(i, j2, k);
		gl.glVertex3f(i, j2, k2);
		gl.glVertex3f(i2, j2, k2);

		light = 0.8f-0.05f;
		gl.glColor4f(r*light, g*light, b*light, alpha);
		gl.glVertex3f(i, j, k);
		gl.glVertex3f(i, j2, k);
		gl.glVertex3f(i2, j2, k);
		gl.glVertex3f(i, j, k);
		gl.glVertex3f(i2, j, k);
		gl.glVertex3f(i2, j2, k);

		light = 0.8f+0.05f;
		gl.glColor4f(r*light, g*light, b*light, alpha);
		gl.glVertex3f(i, j, k2);
		gl.glVertex3f(i, j2, k2);
		gl.glVertex3f(i2, j2, k2);
		gl.glVertex3f(i, j, k2);
		gl.glVertex3f(i2, j, k2);
		gl.glVertex3f(i2, j2, k2);
	}

	public boolean surfVisible(float x, float y, float z, float nx, float ny, float nz) {
		if (isOrtho)
			return nx*g.x + ny*g.y + nz*g.z <= 0;
		else
			return nx*(x - e.renderer.cam_x) + ny*(y - e.renderer.cam_y) + nz*(z - e.renderer.cam_z) <= 0;
	}

	public int packCoords(int i, int j, int k, int di, int dj, int dk) {
		int di_tmp = (di+1);
		int dj_tmp = (dj+1);
		int dk_tmp = (dk+1);

		return ((((i * e.ny + j) * e.nz + k) * 3 + di_tmp) * 3 + dj_tmp) * 3 + dk_tmp;
	}

	public void storeInt(GL2 gl, int i) {
		float a = ((i & 0xFF000000) >> 24)/255f;
		float r = ((i & 0x00FF0000) >> 16)/255f;
		float g = ((i & 0x0000FF00) >> 8)/255f;
		float b = (i & 0x000000FF)/255f;
		gl.glColor4f(r, g, b, a);
	}

	public void unpackCoords(int c) {
		int dk_tmp = c%3; c /= 3;
		int dj_tmp = c%3; c /= 3;
		int di_tmp = c%3; c /= 3;
		int k = c%e.nz; c /= e.nz;
		int j = c%e.ny; c /= e.ny;
		int i = c%e.nx; c /= e.nx;

		e.controls.mx_3d = i;
		e.controls.my_3d = j;
		e.controls.mz_3d = k;

		int mx_normal = di_tmp-1;
		int my_normal = dj_tmp-1;
		int mz_normal = dk_tmp-1;

		if (Brush.sticksOut(e.controls.brushes.getOption())) {
			e.controls.mx_3d += mx_normal;
			e.controls.my_3d += my_normal;
			e.controls.mz_3d += mz_normal;
		}
	}

	public void drawHitboxes(GL2 gl) {
		gl.glBlendFuncSeparate(GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE, GL2.GL_ZERO);
		gl.glDepthMask(true);

		gl.glBegin(GL2.GL_TRIANGLES);
		for (int i = 0; i < e.nx; i++) for (int j = 0; j < e.ny; j++) for (int k = 0; k < e.nz; k++) {
			for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) for (int dk = -1; dk <= 1; dk++) {
				if (Math.abs(di)+Math.abs(dj)+Math.abs(dk) == 1
						&& i+di >= 0 && i + di < e.nx
						&& j+dj >= 0 && j + dj < e.ny
						&& k+dk >= 0 && k + dk < e.nz) {
					if (!surfVisible(i, j, k, di, dj, dk))
						continue;

					if (e.renderer.solid[i][j][k] && !e.renderer.solid[i+di][j+dj][k+dk]) {
						storeInt(gl, packCoords(i, j, k, di, dj, dk));
						
						if (di == -1) {
							gl.glVertex3f(i, j, k);
							gl.glVertex3f(i, j+1, k);
							gl.glVertex3f(i, j+1, k+1);
							gl.glVertex3f(i, j, k);
							gl.glVertex3f(i, j, k+1);
							gl.glVertex3f(i, j+1, k+1);
						} else if (di == 1) {
							gl.glVertex3f(i+1, j, k);
							gl.glVertex3f(i+1, j+1, k);
							gl.glVertex3f(i+1, j+1, k+1);
							gl.glVertex3f(i+1, j, k);
							gl.glVertex3f(i+1, j, k+1);
							gl.glVertex3f(i+1, j+1, k+1);
						} else if (dj == -1) {
							gl.glVertex3f(i, j, k);
							gl.glVertex3f(i+1, j, k);
							gl.glVertex3f(i+1, j, k+1);
							gl.glVertex3f(i, j, k);
							gl.glVertex3f(i, j, k+1);
							gl.glVertex3f(i+1, j, k+1);
						} else if (dj == 1) {
							gl.glVertex3f(i, j+1, k);
							gl.glVertex3f(i+1, j+1, k);
							gl.glVertex3f(i+1, j+1, k+1);
							gl.glVertex3f(i, j+1, k);
							gl.glVertex3f(i, j+1, k+1);
							gl.glVertex3f(i+1, j+1, k+1);
						} else if (dk == -1) {
							gl.glVertex3f(i, j, k);
							gl.glVertex3f(i, j+1, k);
							gl.glVertex3f(i+1, j+1, k);
							gl.glVertex3f(i, j, k);
							gl.glVertex3f(i+1, j, k);
							gl.glVertex3f(i+1, j+1, k);
						} else if (dk == 1) {
							gl.glVertex3f(i, j, k+1);
							gl.glVertex3f(i, j+1, k+1);
							gl.glVertex3f(i+1, j+1, k+1);
							gl.glVertex3f(i, j, k+1);
							gl.glVertex3f(i+1, j, k+1);
							gl.glVertex3f(i+1, j+1, k+1);
						}
					}
				}
			}
		}
		gl.glEnd();
		gl.glFlush();
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		this.width = width;
		this.height = height;
		if (height <= 0) height = 1;

		aspect = (float) width / height;
		//System.out.println(this.width + " " + this.height + " " + canvas.getWidth() + " " + canvas.getHeight() + " " + e.imgwidth + " " + e.imgheight) ;
	}
}