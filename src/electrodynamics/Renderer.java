package electrodynamics;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

import electrodynamics.Electrodynamics.Brush;
import electrodynamics.Electrodynamics.RenderingMode;
import electrodynamics.Electrodynamics.VectorMode;
import electrodynamics.Electrodynamics.VectorView;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.swing.*;

public class Renderer implements GLEventListener {

	Electrodynamics e;
	GLCanvas canvas;
	IntBuffer selectBuf;
	GLU glu;
	IntBuffer viewport;
	
	Vector g = new Vector(0, 0, 0);
	float pitch = (float)Math.PI/6;
	float yaw = (float)Math.PI/4;
	float pitch_start = 0;
	float yaw_start = 0;

    float aspect = 1;
    float scale = 24f;
    int width;
    int height;
    
    TextRenderer renderer;
    FPSAnimator animator;
	
	public Renderer(Electrodynamics e) {
		this.e = e;
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);

        // Create canvas
        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(this);
        canvas.setSize(e.imgwidth, e.imgheight);

        animator = new FPSAnimator(canvas, e.targetframerate);
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
        renderer = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 6));
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Called when the canvas is destroyed
    }

    @Override
    public void display(GLAutoDrawable drawable) {
    	if (!e.threeD_mode)
    		return;
    	
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        setupProjectionMat(gl);
        
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        setupModelMat(gl);
        
        drawThings(gl);


        renderer.beginRendering(canvas.getWidth(), canvas.getHeight());
        
        e.renderText(null, renderer);
        
        renderer.endRendering();
        
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPickMatrix(((float)e.mx_3d/canvas.getWidth())*e.imgwidth*width/canvas.getWidth(), (1f-(float)e.my_3d/canvas.getHeight())*e.imgheight*height/canvas.getHeight(), 1, 1, viewport);
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
        if (hits > 0 && e.update3dCursor) {
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
        	e.update3dCursor = false;
        }
        
        gl.glFlush();
    }

    public void setupProjectionMat(GL2 gl) {
        RenderingMode mode = (RenderingMode) e.opts.gui_3d_view.getSelectedItem();
        
        if (RenderingMode.isOrthographic(mode))
        	gl.glOrtho(-scale * aspect, scale * aspect, -scale, scale, 0, 128);
        else if (RenderingMode.isPerspective(mode))
        	gl.glFrustum(-0.01*scale * aspect, 0.01*scale * aspect, -0.01*scale, 0.01*scale, 0.01*64, 4*64);
        
        gl.glRotatef(90f, 0.0f, 0.0f, 1.0f);
        gl.glRotatef(90f, 0.0f, 1.0f, 0.0f);
    }
    
    public void setupModelMat(GL2 gl) {

    	g.x = Math.cos(yaw)*Math.cos(pitch);
    	g.y = Math.sin(yaw)*Math.cos(pitch);
    	g.z = -Math.sin(pitch);
    	
        gl.glTranslatef(48, 0, 0);
        gl.glRotatef(-pitch*(float)(180/Math.PI), 0.0f, 1.0f, 0.0f);
        gl.glRotatef(-yaw*(float)(180/Math.PI), 0.0f, 0.0f, 1.0f);
        gl.glTranslatef(-16, -16, -16);
    }
    
    public void drawThings(GL2 gl) {
        gl.glEnable(GL2.GL_BLEND);
        gl.glDepthMask(true);
        gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ONE);
        
        // Draw a triangle

        //System.out.println("Here3");
        
        RenderingMode mode = (RenderingMode) e.opts.gui_3d_view.getSelectedItem();

        if (mode != RenderingMode.THREED_FIELDS_ONLY && mode != RenderingMode.THREED_PERSPECTIVE_FIELDS_ONLY) {
            float alpha = 1f;
            
        	if (mode == RenderingMode.THREED_TRANSLUCENT || mode == RenderingMode.THREED_PERSPECTIVE_TRANSLUCENT) {
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

        				if (e.materials[i][j][k].type != MaterialType.VACUUM && e.materials[i+di][j+dj][k+dk].type == MaterialType.VACUUM) {
        					//float r = e.materials[i][j][k].type.color_r/255f;
        					//float g = e.materials[i][j][k].type.color_g/255f;
        					//float b = e.materials[i][j][k].type.color_b/255f;
        					float r = e.image_r[i][j][k];
        					float g = e.image_g[i][j][k];
        					float b = e.image_b[i][j][k];
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

        					if (e.under_brush[i][j][k] && !e.under_brush[i+di][j+dj][k+dk]) {
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

        	double arrowlength = 30.0/e.scalefactor;

        	double vectorscalingconstant = 0.01*Math.pow(10.0, e.opts.gui_brightness_vec.getValue()/5.0);

        	VectorMode vector_display_mode = (VectorMode)e.opts.gui_view_vec_mode.getSelectedItem();

        	e.rand.setSeed(4);

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


        	Vector ctr = new Vector(0,0,0);
        	Vector arrow = new Vector(0,0,0);
        	Vector tip1 = new Vector(0,0,0);
        	Vector tip2 = new Vector(0,0,0);
        	Vector body1 = new Vector(0,0,0);
        	Vector body2 = new Vector(0,0,0);
        	Vector o = new Vector(-e.nx/2.0,-e.ny/2.0,-e.nz/2.0);

        	for (int i = 0; i <= density; i++) {
        		for (int j = 0; j <= density; j++) {
        			for (int k = 0; k <= density; k++) {

        				//double x = (nx-1)*(i+0.5)/50;
        				//double y = (ny-1)*(j+0.5)/50;
        				double x = (e.nx-1)*(i+randomness*(e.rand.nextFloat()-0.5))/density;
        				double y = (e.ny-1)*(j+randomness*(e.rand.nextFloat()-0.5))/density;
        				double z = (e.nz-1)*(k+randomness*(e.rand.nextFloat()-0.5))/density;
        				

    					
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
    								dx = e.bilinearinterp(vf_x, prevx+grid_offset, prevy+dual_offset, prevz+dual_offset);
    								dy = e.bilinearinterp(vf_y, prevx+dual_offset, prevy+grid_offset, prevz+dual_offset);
    								dz = e.bilinearinterp(vf_z, prevx+dual_offset, prevy+dual_offset, prevz+grid_offset);

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

        					arrow.x = e.bilinearinterp(vf_x, x+grid_offset, y+dual_offset, z+dual_offset);
        					arrow.y = e.bilinearinterp(vf_y, x+dual_offset, y+grid_offset, z+dual_offset);
        					arrow.z = e.bilinearinterp(vf_z, x+dual_offset, y+dual_offset, z+grid_offset);

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
    	
    	e.mx = e.indexToCoord(i);
    	e.my = e.indexToCoord(j);
    	e.mz = e.indexToCoord(k);

    	e.mx_normal = di_tmp-1;
    	e.my_normal = dj_tmp-1;
    	e.mz_normal = dk_tmp-1;
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
    				
        			if (e.materials[i][j][k].type != MaterialType.VACUUM && e.materials[i+di][j+dj][k+dk].type == MaterialType.VACUUM) {
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