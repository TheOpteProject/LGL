//  
//  Copyright (c) 2003 Alex Adai, All Rights Reserved.
//  
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License as
//  published by the Free Software Foundation; either version 2 of
//  the License, or (at your option) any later version.
//  
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
//  MA 02111-1307 USA
//  

package Viewer2D;

import java.io.File;

import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import ImageMaker.GenerateImages;

import java.awt.Insets;

/**
 * <b>SESS - 2014.05.11:</b>
 * <ul>
 * <li>Cleanup code a little bit.</li>
 * <li>L&F property(-dlaf=&lt;L&F name&gt;). If not L&F property set system's
 * default. Metal is to 90s...</li>
 * </ul>
 * 
 */
public class Viewer2D {

	// Aprox memory consumption: 1000 x 1000 x 4b = 4M
	// Aprox memory consumption: 2000 x 2000 x 4b = 16M
	// TODO: SESS - I will keep this size until I understand zoom and move...
	static int IMAGE_SIZE = 1000;

	public static void main(String[] args) {
		String laf = System.getProperty("laf");
		if (laf == null) {
			try {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			String OS = System.getProperty("os.name").toLowerCase();
			//if (OS.contains("win"))
			//	UIManager.put("MenuItem.margin", new Insets(2, -15, 2, 2));
		} else {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if (laf.equals(info.getName())) {
					try {
						UIManager.setLookAndFeel(info.getClassName());
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}

		InputMap im = (InputMap)UIManager.get("Button.focusInputMap");
		im.put(KeyStroke.getKeyStroke("pressed SPACE"), "none");
		im.put(KeyStroke.getKeyStroke("released SPACE"), "none");

		// create a new frame object
		EdgesFrame frame = new EdgesFrame("lglview", IMAGE_SIZE, IMAGE_SIZE);

		GenerateImages.ParseArguments pa = new GenerateImages.ParseArguments(true);
		pa.parse(args);

		// Deal with possible command line args
		// The first entry is a possible edges file (.ls) and the next
		// file is a possible coord file
		if (args.length > 0) 
		{
			System.out.println("Loading edges file " + pa.edgeFile);
			frame.loadSHORTFile(new File(pa.edgeFile));
			//if (args.length > 1) 
			{
				System.out.println("Loading coords file " + pa.coordFiles.get(0));
				frame.loadCoordsFile(new File(args[1]));
			}
			if (!pa.edgeColorFile.isEmpty()) {
				System.out.println("Loading color file " + pa.edgeColorFile);
				File colorfile = new File(pa.edgeColorFile);
				frame.loadEdgeColorFile(colorfile);
			}
		}
	}

}
