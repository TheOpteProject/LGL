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

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

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

		// create a new frame object
		EdgesFrame frame = new EdgesFrame("lglview", IMAGE_SIZE, IMAGE_SIZE);

		// Deal with possible command line args
		// The first entry is a possible edges file (.ls) and the next
		// file is a possible coord file
		if (args.length > 0) {
			System.out.println("Loading edges file " + args[0]);
			frame.loadSHORTFile(new File(args[0]));
			if (args.length > 1) {
				System.out.println("Loading coords file " + args[1]);
				frame.loadCoordsFile(new File(args[1]));
			}
			if (args.length > 2) {
				System.out.println("Loading color file " + args[2]);
				File colorfile = new File(args[2]);
				frame.loadEdgeColorFile(colorfile);
			}
		}
	}

}
