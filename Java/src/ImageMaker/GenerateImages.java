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
import Viewer2D.VertexStats;
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

	static void loadLabels(String labelFile,ViewerIO verterIO)
	{
		if (!labelFile.isEmpty())
		{
			System.out.println("Loading label file: " + labelFile + "...");
			try {
				verterIO.loadLabelFile(new File(labelFile));
				
			} catch (java.io.FileNotFoundException e) {
				System.out.println(e.getMessage());
			} catch (java.io.IOException e) {
				System.out.println(e.getMessage());
				System.exit(1);
			}
			System.out.println("Labels loading complete.");

		}


	}

	static public class ParseArguments 
	{
		
		public String edgeFile;
		public String edgeColorFile;
		public String labelFile;
		public double scaling;
		public double minX;
		public double maxX;
		public double minY;
		public double maxY;
		int[] windowSizes;
		boolean alignmentCenter;
		public List<String> coordFiles;
		private boolean viewer2d;

		public ParseArguments (boolean viewer2d)
		{
			this.viewer2d = viewer2d;


		}

		void usage()
		{
			if (viewer2d)
				message2();
			else
				message();


		}

		public void parse(String[] args)
		{
		int minArguments = 3;	

		if (viewer2d)
		{
			minArguments = 1;
			if (args.length==0)
				return;
		}	
			
		if (args.length < minArguments) {
			usage();
		}
		int argno = 0;
		if (!viewer2d)
		{
			windowSizes = new int[2];
			windowSizes[0] = new Integer(args[0]).intValue();
			windowSizes[1] = new Integer(args[1]).intValue();
			argno+=2;
			System.out.println("Loading flindeberg mod 2");
			System.out.println("Image size is " + windowSizes[0] + " x "
					+ windowSizes[1]);
		}
	

		// Check params
		edgeFile = args[argno++];
		edgeColorFile = "";
		coordFiles = new ArrayList<String>();
		labelFile = "";
		boolean colorFileSwitch = false;
		boolean labelFileSwitch = false;
		boolean scaleSwitch = false;
		scaling = 1;
	
	
		boolean wasMin = false;
		boolean wasMax = false;
		boolean minSwitch = false;
		boolean maxSwitch = false;
		minX = 0;
		maxX = 0;
		minY = 0;
		maxY = 0;

		alignmentCenter = false;
		boolean alignSwitch = false;
		for (int i = argno; i < args.length; i++) {
			String arg = args[i];
			if ("-c".equals(arg)) {
				colorFileSwitch = true;
				continue;
			}
			if ("-l".equals(arg)) {
				labelFileSwitch = true;
				continue;
			}
			if ("-s".equals(arg)) {
				scaleSwitch = true;
				continue;
			}
			if ("-m".equals(arg)  && !viewer2d) {
				minSwitch = true;
				continue;
			}
			if ("-a".equals(arg) && !viewer2d) {
				alignSwitch  = true;
				continue;
			}
			if ("-M".equals(arg)  && !viewer2d) {
				maxSwitch = true;
				continue;
			}
			if (scaleSwitch) {
				scaleSwitch = false;
				scaling = Double.parseDouble(arg);
				continue;
			}
			if (minSwitch) {
				minSwitch = false;
				wasMin = true;
				String [] a = arg.split(",");
				if (a.length!=2)
				{
					System.out.println("Error:-m requires exactly 2 coordinates");
					System.exit(1);

				}
				minX = Double.parseDouble(a[0]);
				minY = Double.parseDouble(a[1]);
				continue;
			}
			if (maxSwitch) {
				maxSwitch = false;
				wasMax = true;
				String [] a = arg.split(",");
				if (a.length!=2)
				{
					System.out.println("Error:-M requires exactly 2 coordinates");
					System.exit(1);
				}
				maxX = Double.parseDouble(a[0]);
				maxY = Double.parseDouble(a[1]);
				continue;
			}
			if (alignSwitch) {
				alignSwitch = false;
				if (arg.equals("center"))
					alignmentCenter = true;
				continue;
			}
			if (labelFileSwitch) {
				labelFile = arg;
				labelFileSwitch = false;
			
				continue;
			}
			if (colorFileSwitch) {
				edgeColorFile = arg;
				colorFileSwitch = false;
				continue;
			}
			coordFiles.add(arg);
		}
		if (coordFiles.isEmpty()) {
			usage();
		}

		if (wasMax ^ wasMin)
		{
			System.out.println("Error:Both -m and -M need to be used at the same time, one of them is missing");
			System.exit(1);
		}


		}


	}

	public static void main(String[] args) {
		boolean loadedEdgeColors = false;
		ParseArguments pa = new ParseArguments(false);
		pa.parse(args);
		System.out.println("Loading edge file: " + pa.edgeFile + "...");
		ViewerIO verterIO = null;
		try {
			verterIO = new ViewerIO(new File(pa.edgeFile));
			verterIO.loadSHORTFile();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		verterIO.setLabelScale(pa.scaling);
		
		verterIO.setMinMaxXY(pa.minX,pa.minY,pa.maxX,pa.maxY);
		System.out.println("Trying to load edge color file: " + pa.edgeColorFile
				+ "...");
		try {
			verterIO.loadEdgeColorFile(new File(pa.edgeColorFile));
			loadedEdgeColors = true;
		} catch (java.io.FileNotFoundException e) {
			loadedEdgeColors = false;
			System.out.println(e.getMessage());
		} catch (java.io.IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}
		System.out.println("Edges loading complete.");

		generate("dark with labels","dark_withlabels",true,Color.BLACK,loadedEdgeColors, pa.windowSizes, pa.coordFiles, pa.labelFile, pa.alignmentCenter,verterIO); 


		generate("dark without labels","dark_nolabels",false,Color.BLACK,loadedEdgeColors, pa.windowSizes, pa.coordFiles, "", pa.alignmentCenter,verterIO); 

		generate("light","light",false,Color.white,loadedEdgeColors, pa.windowSizes, pa.coordFiles, pa.labelFile, pa.alignmentCenter,verterIO);
		
		generate("transparent","transparent",false,new Color(0f,0f,0f,0f),loadedEdgeColors, pa.windowSizes, pa.coordFiles, pa.labelFile, pa.alignmentCenter,verterIO);


		//generateDark(loadedEdgeColors, windowSizes, coordFiles, labelFile, alignmentCenter,verterIO);

		//generateLight(loadedEdgeColors, windowSizes, coordFiles, labelFile,alignmentCenter,verterIO);

		//generateTransparent(loadedEdgeColors, windowSizes, coordFiles, labelFile,alignmentCenter,verterIO);
		
		// System.out.println("Going for vector graphics (currently broken)");
		// // Lets process coords files
		// for (String coordFile : coordFiles) {
		// 	try {
		// 		System.out.println("Loading " + coordFile + "...");
		// 		verterIO.loadVertexCoords(new File(coordFile));

				
		// 		FormatVertex formatter = new FormatVertex(
		// 				verterIO.getVertices(), verterIO.getStats(),
		// 				windowSizes, 1);

		// 		EdgesPanel panel = new EdgesPanel(verterIO.getEdges(),
		// 				verterIO.getVertices(), windowSizes[0], windowSizes[1]);

		// 		if (loadedEdgeColors)
		// 			panel.addEdgeColors(verterIO.getEdgeColorMap());

		// 		panel.showVertices(true);
		// 		panel.setVisibilityTest(true);
		// 		panel.setFormatter(formatter);
		// 		panel.setEdgeColor(EDGE_COLOR);
		// 		panel.setVertexColor(Color.white);
		// 		panel.setBackgroundColor(new Color(0f,0f,0f,0f));

		// 		// Use vector instead
		// 		VectorGraphics2D g2 = new VectorGraphics2D();
								
		// 		// Now the image has to be fitted to the given region
		// 		panel.fitData();
		// 		panel.writeVectorImage(g2);

		// 		CommandSequence commands = ((VectorGraphics2D) g2).getCommands();
				
		// 		String[] elements = {"pdf", "eps", "svg"};
		// 		for (String ext : elements) {
					
		// 			Processor pdfProcessor = Processors.get(ext);
		// 			Document doc = pdfProcessor.getDocument(commands, PageSize.A4);
					
		// 			String pngFile = MessageFormat.format(
		// 					"{0}_{1,number,0}x{2,number,0}_transparent.{3}", coordFile,
		// 					windowSizes[0], windowSizes[1], ext);

		// 			System.out.println("Preparing " + pngFile + "...");
		// 			try {
		// 				doc.writeTo(new FileOutputStream(pngFile));
		// 			} catch (Exception e) {
		// 				System.out.println("Could not write vector graphics");
		// 			}

		// 			System.out.println("Done.");
		// 		}

				
		// 	} catch (IOException e) {
		// 		System.out.println(MessageFormat.format(
		// 				"Error processing {0}:\n{1}", e.getMessage()));
		// 	}
		// }
	}




	private static void generate(String displayname,String name,boolean showStat,Color background,boolean loadedEdgeColors, int[] windowSizes, List<String> coordFiles,String labelFile,boolean alignmentCenter,
			ViewerIO verterIO) {
		if (!labelFile.isEmpty())
			loadLabels(labelFile,verterIO);
		else
			verterIO.clearLabels();
		
			
		System.out.println("Going for "+displayname);
		// Lets process coords files
		for (String coordFile : coordFiles) {
			try {
				System.out.println("Loading " + coordFile + "...");
				verterIO.loadVertexCoords(new File(coordFile));

				VertexStats stat = verterIO.getStats();
				if (showStat)
					stat.print();
				//System.out.println("min x:" +stat.min(0)+" max x:"+stat.max(0));

				String pngFile = MessageFormat.format(
						"{0}_{1,number,0}x{2,number,0}_"+name+".png", coordFile,
						windowSizes[0], windowSizes[1]);
				System.out.println("Preparing " + pngFile + "...");
				FormatVertex formatter = new FormatVertex(
						verterIO.getVertices(),  verterIO.getLabels(),verterIO.getLabelScale(), verterIO.getMinX(),verterIO.getMinY(),verterIO.getMaxX(),verterIO.getMaxY(), alignmentCenter,verterIO.getStats(),
						windowSizes, 1);

				EdgesPanel panel = new EdgesPanel(verterIO.getEdges(),
						verterIO.getVertices(), verterIO.getLabels(), windowSizes[0], windowSizes[1]);

				if (loadedEdgeColors)
					panel.addEdgeColors(verterIO.getEdgeColorMap());

				panel.showVertices(true);
				panel.setVisibilityTest(true);
				panel.setFormatter(formatter);
				panel.setEdgeColor(EDGE_COLOR);
				panel.setVertexColor(Color.white);
				panel.setBackgroundColor(background);

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
	}

	

	public static void message() {
		System.out
				.println("Arguments:\n\n"
						+ "\t<width> <height> <edges file> <coords file1> <coords file2>... [-c <colors file> ] [-l <labels file>] [-m minx,miny -M -maxx,maxy] [-a center]\n\n"
						+ "If no colors file specified program will try to load file named \""
						+ EDGE_COLOR_FILE + "\".\n"
						+ "By default edges are white. flindeberg mod");
		System.exit(1);
	}
	public static void message2() {
		System.out
				.println("Arguments:\n\n"
						+ "\t<edges file> <coords file1> [-c <colors file> ] [-l <labels file>]\n\n"
						+ "If no colors file specified program will try to load file named \""
						+ EDGE_COLOR_FILE + "\".\n"
						);
		System.exit(1);
	}

}
