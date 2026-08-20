// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class IconButton extends JButton {

	private static final long serialVersionUID = 7046425063110958439L;

	BufferedImage icon;
	ImageIcon normal;
	ImageIcon highlighted;
	Color fc;
	Color bc;
	boolean grayscale = true;
	boolean is_highlighted = false;

	public IconButton(BufferedImage icon, int size) {
		super();

		this.icon = icon;
		this.setIcon(new ImageIcon(icon));
		setPreferredSize(new Dimension(size+8, size+8));
		setMaximumSize(new Dimension(size+8, size+8));
		
		updateUI();
	}

	@Override
	public void updateUI() {
		super.updateUI();

		fc = this.getForeground();
		bc = this.getBackground();

		if (icon != null) {
			makeNormalIcon();
			makeHighlightedIcon();
			
			setHighlighted(is_highlighted);
		}
	}

	public void setHighlighted(boolean value) {
		is_highlighted = value;
		if (is_highlighted) {
			setIcon(highlighted);
			this.setBackground(fc);
		}
		else {
			setIcon(normal);
			this.setBackground(bc);
		}
	}
	
	private void makeNormalIcon() {
		int fr = fc.getRed();
		int fg = fc.getGreen();
		int fb = fc.getBlue();

		BufferedImage image = new BufferedImage(icon.getWidth(), icon.getHeight(), icon.getType());
		Graphics gr = image.getGraphics();
		gr.drawImage(icon, 0, 0, null);
		for(int y = 0; y < icon.getHeight(); y++) {
			for(int x = 0; x < icon.getWidth(); x++)
			{
				int argb = icon.getRGB(x, y);

				int a = ((argb>>24)&255);
				int ir = ((argb>>16)&255);
				int ig = ((argb>>8)&255);
				int ib = ((argb>>0)&255);
				
				int r, g, b;

				if (grayscale || (ir+ig+ib < 30)) {
					r = fr;
					g = fg;
					b = fb;
				} else {
					r = ir;
					g = ig;
					b = ib;
				}
				if (r > 255)
					r = 255;
				if (g > 255)
					g = 255;
				if (b > 255)
					b = 255;
				
				image.setRGB(x, y, a<<24 | r << 16 | g << 8 | b);
			}
		}
		
		normal = new ImageIcon(image);
	}
	
	private void makeHighlightedIcon() {
		int fr = bc.getRed();
		int fg = bc.getGreen();
		int fb = bc.getBlue();
		
		int br = fc.getRed();
		int bg = fc.getGreen();
		int bb = fc.getBlue();

		BufferedImage image = new BufferedImage(icon.getWidth(), icon.getHeight(), icon.getType());
		Graphics gr = image.getGraphics();
		gr.drawImage(icon, 0, 0, null);
		for(int y = 0; y < icon.getHeight(); y++) {
			for(int x = 0; x < icon.getWidth(); x++)
			{
				int argb = icon.getRGB(x, y);

				int a = ((argb>>24)&255);
				double af = a/255.0;

				int ir = ((argb>>16)&255);
				int ig = ((argb>>8)&255);
				int ib = ((argb>>0)&255);
				
				int r, g, b;

				if (grayscale || (ir+ig+ib < 30)) {
					r = (int)(fr*af+br*(1-af));
					g = (int)(fg*af+bg*(1-af));
					b = (int)(fb*af+bb*(1-af));
				} else {
					r = (int)(ir*af+br*(1-af));
					g = (int)(ig*af+bg*(1-af));
					b = (int)(ib*af+bb*(1-af));
				}
				
				if (r > 255)
					r = 255;
				if (g > 255)
					g = 255;
				if (b > 255)
					b = 255;
				
				a = 255;
				
				image.setRGB(x, y, a<<24 | r << 16 | g << 8 | b);
			}
		}
		
		highlighted = new ImageIcon(image);
	}
}
