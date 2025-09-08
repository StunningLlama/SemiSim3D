package electrodynamics;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

import electrodynamics.util.Utils;
import electrodynamics.util.Vector3;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class Renderer3D implements GLEventListener {

	Electrodynamics e;
	GLCanvas canvas;
	IntBuffer selectBuf;
	GLU glu;
	IntBuffer viewport;
	
	Vector3 g = new Vector3(0, 0, 0);

    float aspect = 1;
    float eye_offset = 0;
    int width;
    int height;
    boolean isMainCanvas = false;
    
    TextRenderer smallFont;
    TextRenderer bigFont;
    FPSAnimator animator;

	ArrayList<Text> texts = new ArrayList<>();
	
	public Renderer3D(Electrodynamics e) {
		this.e = e;
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);

        // Create canvas
        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this);
        canvas.setSize(768, 768);

        animator = new FPSAnimator(canvas, e.renderer.targetframerate);
	}

    @Override
    public void init(GLAutoDrawable drawable) {
    	glu = GLU.createGLU();
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0f, 0f, 0f, 1f);  // Black background
        gl.glEnable(GL2.GL_DEPTH_TEST);
        selectBuf = Buffers.newDirectIntBuffer(256);
        viewport = Buffers.newDirectIntBuffer(4);
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport);
        smallFont = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 6));
        bigFont = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 8));
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Called when the canvas is destroyed
    }

    @Override
    public void display(GLAutoDrawable drawable) {
    	if (!e.renderer.threeD_mode)
    		return;

		if (isMainCanvas) e.renderer.t5.start();
		
    	e.rwLock.readLock().lock();
    	try {
    		e.renderer.generatePixelData();

    		if (isMainCanvas && e.opts.gui_rotate.isSelected()) {
    			e.renderer.yaw += 1f/e.renderer.targetframerate;
    		}

    		GL2 gl = drawable.getGL().getGL2();

    		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    		// Draw scene

    		gl.glMatrixMode(GL2.GL_PROJECTION);
    		gl.glLoadIdentity();
    		setupProjectionMat(gl);

    		gl.glMatrixMode(GL2.GL_MODELVIEW);
    		gl.glLoadIdentity();
    		setupModelMat(gl);

    		drawThings(gl);

    		// Draw text

    		gl.glMatrixMode(GL2.GL_PROJECTION);
    		gl.glLoadIdentity();

    		gl.glMatrixMode(GL2.GL_MODELVIEW);
    		gl.glLoadIdentity();

    		texts.clear();
    		e.renderer.generateText(null, texts);

    		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    		gl.glDisable(GL2.GL_BLEND);

    		if (e.opts.gui_text_bg.isSelected())
    		{
    			gl.glBegin(GL2.GL_TRIANGLES);
    			for (int pass = 1; pass <= 2; pass++) {
    				for (Text text : texts) {
    					if (text.hasBackground && !text.is3D) {
    						Rectangle2D bounds;
    						if (text.big) 
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

    		for (Text text : texts) {
    			if (text.big) {
    				if (!text.is3D) {
    					bigFont.setColor(Color.WHITE);
    					bigFont.draw(text.text, text.x, canvas.getHeight()-(text.y));
    				}
    			}
    		}

    		bigFont.endRendering();

    		smallFont.beginRendering(canvas.getWidth(), canvas.getHeight());

    		for (Text text : texts) {
    			if (!text.big) {
    				if (!text.is3D) {
    					smallFont.setColor(Color.WHITE);
    					smallFont.draw(text.text, text.x, canvas.getHeight()-(text.y));
    				}
    			}
    		}

    		smallFont.endRendering();


    		gl.glMatrixMode(GL2.GL_PROJECTION);
    		gl.glLoadIdentity();
    		setupProjectionMat(gl);

    		gl.glMatrixMode(GL2.GL_MODELVIEW);
    		gl.glLoadIdentity();
    		setupModelMat(gl);

    		bigFont.begin3DRendering();

    		for (Text text : texts) {
    			if (text.big) {
    				if (text.is3D) {
    					bigFont.setColor(Color.BLACK);
    					bigFont.draw3D(text.text, text.x+0.5f-0.15f, text.y+0.5f-0.15f, text.z+0.5f-0.01f, 0.08f);
    					bigFont.setColor(Color.WHITE);
    					bigFont.draw3D(text.text, text.x+0.5f, text.y+0.5f, text.z+0.5f, 0.08f);
    				}
    			}
    		}

    		bigFont.end3DRendering();

    		smallFont.begin3DRendering();

    		for (Text text : texts) {
    			if (!text.big) {
    				if (text.is3D) {
    					smallFont.setColor(Color.BLACK);
    					smallFont.draw3D(text.text, text.x+0.5f-0.15f, text.y+0.5f-0.15f, text.z+0.5f-0.01f, 0.08f);
    					smallFont.setColor(Color.WHITE);
    					smallFont.draw3D(text.text, text.x+0.5f, text.y+0.5f, text.z+0.5f, 0.08f);
    				}
    			}
    		}

    		smallFont.end3DRendering();

    		// Pick

    		gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport);
    		gl.glMatrixMode(GL2.GL_PROJECTION);
    		gl.glLoadIdentity();
    		glu.gluPickMatrix(((float)e.controls.mx_3d)*width/canvas.getWidth(), (canvas.getHeight()-(float)e.controls.my_3d)*height/canvas.getHeight(), 1, 1, viewport);
    		setupProjectionMat(gl);

    		gl.glMatrixMode(GL2.GL_MODELVIEW);
    		gl.glLoadIdentity();
    		setupModelMat(gl);

    		gl.glSelectBuffer(selectBuf.capacity()*Integer.SIZE, selectBuf);
    		gl.glRenderMode(GL2.GL_SELECT);
    		gl.glInitNames();
    		gl.glPushName(-1);
    		drawHitboxes(gl);
    		int hits = gl.glRenderMode(GL2.GL_RENDER);
    		if (hits > 0 && e.controls.update3dCursor) {
    			//System.out.println(hits + " hits");

    			long maxdepth = Long.MAX_VALUE;
    			int m_nearest = 0;
    			for (int m = 0; m < hits; m++) {
    				long depth = Integer.toUnsignedLong(selectBuf.get(4*m+2));
    				//System.out.println(depth + " depth");
    				if (depth < maxdepth) {
    					maxdepth = depth;
    					m_nearest = m;
    				}
    			}



    			int index = selectBuf.get(4*m_nearest+3);
    			this.unpackCoords(index);
    			e.controls.update3dCursor = false;
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
        RenderMode mode = (RenderMode) e.opts.gui_3d_view.getSelectedItem();
        
        if (RenderMode.isOrthographic(mode))
        	gl.glOrtho(-e.renderer.scale * aspect, e.renderer.scale * aspect, -e.renderer.scale, e.renderer.scale, 0, 128);
        else if (RenderMode.isPerspective(mode))
        	gl.glFrustum(-0.01*e.renderer.scale * aspect, 0.01*e.renderer.scale * aspect, -0.01*e.renderer.scale, 0.01*e.renderer.scale, 0.01*64, 4*64);
        
        gl.glRotatef(90f, 0.0f, 0.0f, 1.0f);
        gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);
    }
    
    public void setupModelMat(GL2 gl) {

    	g.x = Math.cos(e.renderer.yaw)*Math.cos(e.renderer.pitch);
    	g.y = Math.sin(e.renderer.yaw)*Math.cos(e.renderer.pitch);
    	g.z = -Math.sin(e.renderer.pitch);
    	
        gl.glTranslatef(48, 0, 0);
        gl.glRotatef(-eye_offset, 0.0f, 0.0f, 1.0f);
        gl.glRotatef(-e.renderer.pitch*(float)(180/Math.PI), 0.0f, 1.0f, 0.0f);
        gl.glRotatef(-e.renderer.yaw*(float)(180/Math.PI), 0.0f, 0.0f, 1.0f);
        gl.glTranslatef(-e.nx/2, -e.ny/2, -e.nz/2);
    }
    
    public void drawThings(GL2 gl) {
        gl.glEnable(GL2.GL_BLEND);
        gl.glDepthMask(true);
        gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ONE);
        
        // Draw a triangle

        //System.out.println("Here3");
        
        RenderMode mode = (RenderMode) e.opts.gui_3d_view.getSelectedItem();

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
        				if (e.materials[i][j][k].type == MaterialType.ABSORBER  && di*g.x + dj*g.y + dk*g.z > 0)
        					continue;

        				if (e.renderer.solid[i][j][k] && !e.renderer.solid[i+di][j+dj][k+dk]) {
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


        	Brush brush = (Brush) e.opts.gui_brush.getSelectedItem();
        	boolean highlight = (e.opts.gui_brush_highlight.isSelected() && Brush.isBrushShapeImportant(brush));
        	if (highlight) {
        		for (int i = 0; i < e.nx; i++) for (int j = 0; j < e.ny; j++) for (int k = 0; k < e.nz; k++) {
        			for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) for (int dk = -1; dk <= 1; dk++) {
        				if (Math.abs(di)+Math.abs(dj)+Math.abs(dk) == 1
        				&& i+di >= 0 && i + di < e.nx
        				&& j+dj >= 0 && j + dj < e.ny
        				&& k+dk >= 0 && k + dk < e.nz) {
        					if (di*g.x + dj*g.y + dk*g.z > 0)
        						continue;

        					if (e.controls.under_brush[i][j][k] && !e.controls.under_brush[i+di][j+dj][k+dk]) {
            					float light = (float)(0.8+0.2*di+0.1*dj+0.05*dk);
            					
        						gl.glColor4f(light, light, light, 0.4f);
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
        	}
        	
            gl.glEnd();
        }
        

        gl.glDepthMask(false);
        gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ZERO, GL2.GL_ONE);
        gl.glLineWidth(3f);
        gl.glBegin(GL2.GL_LINES);
        

        if ((VectorView) e.opts.gui_view_vec.getSelectedItem() != VectorView.NONE) {

        	double arrowlength = 30.0/e.renderer.scalefactor;

        	VectorMode vector_display_mode = (VectorMode)e.opts.gui_view_vec_mode.getSelectedItem();
        	
        	double vectorscalingconstant = Math.pow(10.0, e.opts.gui_brightness_vec.getValue()/5.0)/((VectorView) e.opts.gui_view_vec.getSelectedItem()).scale;


        	e.renderer.rand.setSeed(4);

        	int density = 10;

        	double randomness = 0;

        	if (vector_display_mode == VectorMode.ARROWS) {
        		randomness = 0.75;
        	} else if (vector_display_mode == VectorMode.LINES) {
        		randomness = 0.75;
        	}

        	double[][][] vf_x = null;
        	double[][][] vf_y = null;
        	double[][][] vf_z = null;
        	
        	double grid_offset = -0.5;
        	double dual_offset = 0;
        	
        	switch ((VectorView) e.opts.gui_view_vec.getSelectedItem()) {
        	case NONE:
        		break;
        	case B_FIELD:
        		vf_x = e.Bx;
        		vf_y = e.By;
        		vf_z = e.Bz;
        		grid_offset = 0;
        		dual_offset = 0.5;
        		break;
        	case H_FIELD:
        		vf_x = e.Hx;
        		vf_y = e.Hy;
        		vf_z = e.Hz;
        		grid_offset = 0;
        		dual_offset = -0.5;
        		break;
        	case E_FIELD:
        		vf_x = e.Ex;
        		vf_y = e.Ey;
        		vf_z = e.Ez;
        		break;
        	case D_FIELD:
        		vf_x = e.Dx;
        		vf_y = e.Dy;
        		vf_z = e.Dz;
        		break;
        	case ELECTRON_CURRENT:
        		vf_x = e.Jx_n;
        		vf_y = e.Jy_n;
        		vf_z = e.Jz_n;
        		break;
        	case HOLE_CURRENT:
        		vf_x = e.Jx_p;
        		vf_y = e.Jy_p;
        		vf_z = e.Jz_p;
        		break;
        	case TOTAL_CURRENT:
        		vf_x = e.Jx_free;
        		vf_y = e.Jy_free;
        		vf_z = e.Jz_free;
        		break;
        	case POYNTING:
        		vf_x = e.Sx;
        		vf_y = e.Sy;
        		vf_z = e.Sz;
        		break;
        	case EMF:
        		vf_x = e.emfx;
        		vf_y = e.emfy;
        		vf_z = e.emfz;
        		break;
        	}


        	Vector3 ctr = new Vector3(0,0,0);
        	Vector3 arrow = new Vector3(0,0,0);
        	Vector3 tip1 = new Vector3(0,0,0);
        	Vector3 tip2 = new Vector3(0,0,0);
        	Vector3 body1 = new Vector3(0,0,0);
        	Vector3 body2 = new Vector3(0,0,0);
        	Vector3 o = new Vector3(-e.nx/2.0,-e.ny/2.0,-e.nz/2.0);

        	for (int i = 0; i <= density; i++) {
        		for (int j = 0; j <= density; j++) {
        			for (int k = 0; k <= density; k++) {

        				//double x = (nx-1)*(i+0.5)/50;
        				//double y = (ny-1)*(j+0.5)/50;
        				double x = (e.nx-1)*(i+randomness*(e.renderer.rand.nextFloat()-0.5))/density;
        				double y = (e.ny-1)*(j+randomness*(e.renderer.rand.nextFloat()-0.5))/density;
        				double z = (e.nz-1)*(k+randomness*(e.renderer.rand.nextFloat()-0.5))/density;
        				

    					
    					if (vector_display_mode == VectorMode.LINES && vf_x != null) {
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
    					} else if (vector_display_mode == VectorMode.ARROWS && vf_x != null) {
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
        	}
		}
        
        gl.glEnd();
        

        gl.glDepthMask(true);
    }
    
    public int packCoords(int i, int j, int k, int di, int dj, int dk) {
    	int di_tmp = (di+1);
    	int dj_tmp = (dj+1);
    	int dk_tmp = (dk+1);
    	
    	return ((((i * e.ny + j) * e.nz + k) * 3 + di_tmp) * 3 + dj_tmp) * 3 + dk_tmp;
    }
    

    public void unpackCoords(int c) {
    	int dk_tmp = c%3; c /= 3;
    	int dj_tmp = c%3; c /= 3;
    	int di_tmp = c%3; c /= 3;
    	int k = c%e.nz; c /= e.nz;
    	int j = c%e.ny; c /= e.ny;
    	int i = c%e.nx; c /= e.nx;
    	
    	e.controls.mx = e.controls.indexToCoord(i);
    	e.controls.my = e.controls.indexToCoord(j);
    	e.controls.mz = e.controls.indexToCoord(k);

    	e.controls.mx_normal = di_tmp-1;
    	e.controls.my_normal = dj_tmp-1;
    	e.controls.mz_normal = dk_tmp-1;
    }
    
    public void drawHitboxes(GL2 gl) {
        for (int i = 0; i < e.nx; i++) for (int j = 0; j < e.ny; j++) for (int k = 0; k < e.nz; k++) {
        	for (int di = -1; di <= 1; di++) for (int dj = -1; dj <= 1; dj++) for (int dk = -1; dk <= 1; dk++) {
        		if (Math.abs(di)+Math.abs(dj)+Math.abs(dk) == 1
        			&& i+di >= 0 && i + di < e.nx
        			&& j+dj >= 0 && j + dj < e.ny
        			&& k+dk >= 0 && k + dk < e.nz) {
    				if (di*g.x + dj*g.y + dk*g.z > 0)
    					continue;
    				
        			if (e.renderer.solid[i][j][k] && !e.renderer.solid[i+di][j+dj][k+dk]) {
    					gl.glLoadName(packCoords(i, j, k, di, dj, dk));
    			        gl.glBegin(GL2.GL_TRIANGLES);
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
        		        gl.glEnd();
        			}
        		}
        	}
        }
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