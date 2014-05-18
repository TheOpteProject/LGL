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

import Jama.*;
import Viewer2D.*;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GenerateImages
{    
    public static int IMAGE_WIDTH  = 256;
    public static int IMAGE_HEIGHT = 256;
    public static Color EDGE_COLOR = Color.white;
    public static String EDGE_COLOR_FILE = "color_file";

    public static void main( String[] args )
    {
	boolean loadedEdgeColors = false;
	if ( args.length == 0 ) { message(); }

	int[] windowSizes = new int[2];
	windowSizes[0] = new Integer( args[0] ).intValue(); //IMAGE_WIDTH;
	windowSizes[1] = new Integer( args[1] ).intValue(); //IMAGE_HEIGHT;	

	// Just used for edges, not the coords
	System.out.print("Loading edge file " + args[2] + "...");
	ViewerIO io = null;
	try {
	    io = new ViewerIO( new File( args[2] ) );
	    io.loadSHORTFile();
	} catch ( java.io.FileNotFoundException e ) {
	    System.out.println("File Not Found.");
	    System.exit(0);
	} catch ( java.io.IOException e ) {
	    System.out.println("Error on Edge File IO");		
	    System.exit(0);
	}

	try {
	    io.loadEdgeColorFile( new File( EDGE_COLOR_FILE ) );
	    loadedEdgeColors = true;
	} catch ( java.io.FileNotFoundException e ) {
	    // OK ... so no color file, moving on
	    loadedEdgeColors = false;
	    System.out.print("No color file Found");
	} catch ( java.io.IOException e ) {
	    System.out.println("\nError on Edge Color File IO");		
	    loadedEdgeColors = false;
	    System.exit(0);
	}

	System.out.println("Done.");
	
	// Remaining args are just the files to convert.
	for ( int ii=1+2; ii<args.length; ++ii )
	    {
		try {
		    System.out.print("Loading " + args[ii] + "..." );		    
		    io.loadVertexCoords( new File( args[ii] ) );
		    String outfilename =  args[ii] + ".png";
		    System.out.print("Done.\n\tFitting image and writing " + 
				     outfilename + "...");
		    FormatVertex formatter = new FormatVertex( io.getVertices() , 
							       io.getStats() ,
							       windowSizes , 1 );

		    EdgesPanel panel = new EdgesPanel( io.getEdges() , 
						       io.getVertices() ,
						       windowSizes[0] , windowSizes[1] );

		    if ( loadedEdgeColors )
			panel.addEdgeColors( io.getEdgeColorMap() );

		    panel.showVertices( true );
		    panel.setVisibilityTest( true );
		    panel.setFormatter( formatter );
		    panel.setEdgeColor( EDGE_COLOR );
		    panel.setVertexColor( Color.white );

		    BufferedImage i = new BufferedImage( windowSizes[0],
							 windowSizes[1],
							 BufferedImage.TYPE_INT_RGB );
		    // Now the image has to be fitted to the given region
		    panel.fitData();
		    panel.writeImage( outfilename , i );
		    System.out.println("Done.");
		    
		} catch ( java.io.FileNotFoundException e ) {
		    System.out.println("File Not Found. Skipping");
		} catch ( java.io.IOException e ) {
		    System.out.println("Error on IO. Skipping");		
		}
	    }		
    }

    public static void message()
    {
	String usage = "arguments: ImageWidthInteger ImageHeightInteger edges.lgl coords1 coords2....";
	usage += "\nIf you have a file named \"color_file\" in the current directory\n";
	usage += "It will read that in as an edge color file. By default edges are white.\n";
	System.out.println(usage);
	System.exit(0);
    }

}
