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

import javax.swing.*; 
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;   
import java.io.*;

public class Viewer2D
{
    private static int XWINDOWSIZE = 800;
    private static int YWINDOWSIZE = 800;
    private static int threads;
    private static int[] windowSizes;

    public static void main(String[] args) 
    {
	windowSizes = new int[2];
	windowSizes[0] = XWINDOWSIZE;
	windowSizes[1] = YWINDOWSIZE;

	// Presume at first we are only doing work on a UP
	// machine
	threads = 1;

	// create a new frame object
	EdgesFrame frame = new EdgesFrame("lglview", XWINDOWSIZE , 
					  YWINDOWSIZE );

	// Deal with possible command line args
	// The first entry is a possible edges file (.ls) and the next
	// file is a possible coord file
	if ( args.length > 0 )
	    {
		System.out.println( "Loading edges file " + args[0] );
		frame.loadSHORTFile( new File(args[0]) );
		if ( args.length > 1 ) {
		    System.out.println( "Loading coords file " + args[1] );
		    frame.loadCoordsFile( new File(args[1]) );
		}
	    }

    }

}
 
