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

package ImageMaker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import Viewer2D.EdgesPanel;
import Viewer2D.FormatVertex;
import Viewer2D.ViewerIO;
import de.erichseifert.vectorgraphics2d.Document;
import de.erichseifert.vectorgraphics2d.Processor;
import de.erichseifert.vectorgraphics2d.Processors;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.util.PageSize;

/**
 * <b>SESS - 2014.05.10:</b>
 * <ul>
 * <li>Cleanup code a little bit.</li>
 * <li>Allow user to include edge color file ("-c &lt;file&gt;" at the end of
 * the command line).</li>
 * </ul>
 * 
 */
public class GenerateImages {
	public static Color EDGE_COLOR = Color.white;
	public static String EDGE_COLOR_FILE = "color_file";

	public static void main(String[] args) {
		boolean loadedEdgeColors = false;
		if (args.length == 0) {
			message();
		}

		int[] windowSizes = new int[2];
		windowSizes[0] = new Integer(args[0]).intValue();
		windowSizes[1] = new Integer(args[1]).intValue();
		System.out.println("Loading flindeberg mod");
		System.out.println("Image size is " + windowSizes[0] + " x "
				+ windowSizes[1]);

		// Check params
		String edgeFile = args[2];
		String edgeColorFile = EDGE_COLOR_FILE;
		List<String> coordFiles = new ArrayList<String>();
		boolean colorFileSwitch = false;
		for (int i = 3; i < args.length; i++) {
			String arg = args[i];
			if ("-c".equals(arg)) {
				colorFileSwitch = true;
				continue;
			}
			if (colorFileSwitch) {
				edgeColorFile = arg;
				break;
			}
			coordFiles.add(arg);
		}
		if (coordFiles.isEmpty()) {
			message();
		}

		System.out.println("Loading edge file: " + edgeFile + "...");
		ViewerIO verterIO = null;
		try {
			verterIO = new ViewerIO(new File(edgeFile));
			verterIO.loadSHORTFile();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		System.out.println("Trying to load edge color file: " + edgeColorFile
				+ "...");
		try {
			verterIO.loadEdgeColorFile(new File(edgeColorFile));
			loadedEdgeColors = true;
		} catch (java.io.FileNotFoundException e) {
			loadedEdgeColors = false;
			System.out.println(e.getMessage());
		} catch (java.io.IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		System.out.println("Edges loading complete.");

		System.out.println("Going for dark");
		// Lets process coords files
		for (String coordFile : coordFiles) {
			try {
				System.out.println("Loading " + coordFile + "...");
				verterIO.loadVertexCoords(new File(coordFile));

				String pngFile = MessageFormat.format(
						"{0}_{1,number,0}x{2,number,0}_dark.png", coordFile,
						windowSizes[0], windowSizes[1]);
				System.out.println("Preparing " + pngFile + "...");
				FormatVertex formatter = new FormatVertex(
						verterIO.getVertices(), verterIO.getStats(),
						windowSizes, 1);

				EdgesPanel panel = new EdgesPanel(verterIO.getEdges(),
						verterIO.getVertices(), windowSizes[0], windowSizes[1]);

				if (loadedEdgeColors)
					panel.addEdgeColors(verterIO.getEdgeColorMap());

				panel.showVertices(true);
				panel.setVisibilityTest(true);
				panel.setFormatter(formatter);
				panel.setEdgeColor(EDGE_COLOR);
				panel.setVertexColor(Color.white);
				panel.setBackgroundColor(Color.BLACK);

				BufferedImage bufferedImage = new BufferedImage(windowSizes[0],
						windowSizes[1], BufferedImage.TYPE_INT_ARGB);
				
				// Now the image has to be fitted to the given region
				panel.fitData();
				panel.writeImage(pngFile, bufferedImage);
				System.out.println("Done.");
			} catch (IOException e) {
				System.out.println(MessageFormat.format(
						"Error processing {0}:\n{1}", e.getMessage()));
			}
		}

		System.out.println("Going for light");
		// Lets process coords files
		for (String coordFile : coordFiles) {
			try {
				System.out.println("Loading " + coordFile + "...");
				verterIO.loadVertexCoords(new File(coordFile));

				String pngFile = MessageFormat.format(
						"{0}_{1,number,0}x{2,number,0}_light.png", coordFile,
						windowSizes[0], windowSizes[1]);
				System.out.println("Preparing " + pngFile + "...");
				FormatVertex formatter = new FormatVertex(
						verterIO.getVertices(), verterIO.getStats(),
						windowSizes, 1);

				EdgesPanel panel = new EdgesPanel(verterIO.getEdges(),
						verterIO.getVertices(), windowSizes[0], windowSizes[1]);

				if (loadedEdgeColors)
					panel.addEdgeColors(verterIO.getEdgeColorMap());

				panel.showVertices(true);
				panel.setVisibilityTest(true);
				panel.setFormatter(formatter);
				panel.setEdgeColor(EDGE_COLOR);
				panel.setVertexColor(Color.white);
				panel.setBackgroundColor(Color.white);

				BufferedImage bufferedImage = new BufferedImage(windowSizes[0],
						windowSizes[1], BufferedImage.TYPE_INT_ARGB);
				
				// Now the image has to be fitted to the given region
				panel.fitData();
				panel.writeImage(pngFile, bufferedImage);
				System.out.println("Done.");
			} catch (IOException e) {
				System.out.println(MessageFormat.format(
						"Error processing {0}:\n{1}", e.getMessage()));
			}
		}

		System.out.println("Going for transparent");
		// Lets process coords files
		for (String coordFile : coordFiles) {
			try {
				System.out.println("Loading " + coordFile + "...");
				verterIO.loadVertexCoords(new File(coordFile));

				String pngFile = MessageFormat.format(
						"{0}_{1,number,0}x{2,number,0}_transparent.png", coordFile,
						windowSizes[0], windowSizes[1]);
				System.out.println("Preparing " + pngFile + "...");
				FormatVertex formatter = new FormatVertex(
						verterIO.getVertices(), verterIO.getStats(),
						windowSizes, 1);

				EdgesPanel panel = new EdgesPanel(verterIO.getEdges(),
						verterIO.getVertices(), windowSizes[0], windowSizes[1]);

				if (loadedEdgeColors)
					panel.addEdgeColors(verterIO.getEdgeColorMap());

				panel.showVertices(true);
				panel.setVisibilityTest(true);
				panel.setFormatter(formatter);
				panel.setEdgeColor(EDGE_COLOR);
				panel.setVertexColor(Color.white);
				panel.setBackgroundColor(new Color(0f,0f,0f,0f));

				BufferedImage bufferedImage = new BufferedImage(windowSizes[0],
						windowSizes[1], BufferedImage.TYPE_INT_ARGB);
				
				// Now the image has to be fitted to the given region
				panel.fitData();
				panel.writeImage(pngFile, bufferedImage);
				System.out.println("Done.");
			} catch (IOException e) {
				System.out.println(MessageFormat.format(
						"Error processing {0}:\n{1}", e.getMessage()));
			}
		}
		

		System.out.println("Going for EPS (under construction)");
		// Lets process coords files
		for (String coordFile : coordFiles) {
			try {
				System.out.println("Loading " + coordFile + "...");
				verterIO.loadVertexCoords(new File(coordFile));

				String pngFile = MessageFormat.format(
						"{0}_{1,number,0}x{2,number,0}_transparent.pdf", coordFile,
						windowSizes[0], windowSizes[1]);
				System.out.println("Preparing " + pngFile + "...");
				FormatVertex formatter = new FormatVertex(
						verterIO.getVertices(), verterIO.getStats(),
						windowSizes, 1);

				EdgesPanel panel = new EdgesPanel(verterIO.getEdges(),
						verterIO.getVertices(), windowSizes[0], windowSizes[1]);

				if (loadedEdgeColors)
					panel.addEdgeColors(verterIO.getEdgeColorMap());

				panel.showVertices(true);
				panel.setVisibilityTest(true);
				panel.setFormatter(formatter);
				panel.setEdgeColor(EDGE_COLOR);
				panel.setVertexColor(Color.white);
				panel.setBackgroundColor(new Color(0f,0f,0f,0f));

				// Use vector instead
				VectorGraphics2D g2 = new VectorGraphics2D();
								
				// Now the image has to be fitted to the given region
				panel.fitData();
				panel.writeVectorImage(g2);

				CommandSequence commands = ((VectorGraphics2D) g2).getCommands();
				Processor pdfProcessor = Processors.get("pdf");
				Document doc = pdfProcessor.getDocument(commands, PageSize.A4);

				try {
					doc.writeTo(new FileOutputStream(pngFile));
				} catch (Exception e) {
					System.out.println("Could not write vector graphics");
				}

				System.out.println("Done.");
			} catch (IOException e) {
				System.out.println(MessageFormat.format(
						"Error processing {0}:\n{1}", e.getMessage()));
			}
		}
	}

	public static void message() {
		System.out
				.println("Arguments:\n\n"
						+ "\t<width> <height> <edges file> <coords file1> <coords file2>... [-c <colors file> ]\n\n"
						+ "If no colors file specified program will try to load file named \""
						+ EDGE_COLOR_FILE + "\".\n"
						+ "By default edges are white. flindeberg mod");
		System.exit(1);
	}

}
