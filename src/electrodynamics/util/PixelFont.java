// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class PixelFont {
	public int width;
	public int height;
	
	public HashMap<Character, boolean[][]> map = new HashMap<Character, boolean[][]>();
	
	public void load(File file, int width, int height) {
		this.width = width;
		this.height = height;
		//int ppi = Toolkit.getDefaultToolkit().getScreenResolution();
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(height * (96f / 72f));

			BufferedImage screenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = screenshot.createGraphics();

			g.setFont(font);

			for (char c = ' '; c <= '~'; c++) {
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, width, height);

				g.setColor(Color.WHITE);
				g.drawString(String.valueOf(c), 0, height);

				boolean[][] data = new boolean[width][height];
				
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						data[i][j] = (screenshot.getRGB(i, j) & 0xFF) > 128;
					}
				}
				
				map.put(c, data);
			}

			g.dispose();
		} catch (FontFormatException | IOException e) {
			throw new RuntimeException("Pixel font creation failed");
		}
	}
	
	public boolean getPixel(char c, int i, int j) {
		return map.get(c)[i][j];
	}
	
	public boolean containsChar(char c) {
		return map.containsKey(c);
	}
}
