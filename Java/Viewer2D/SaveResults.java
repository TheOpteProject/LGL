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
import javax.swing.JOptionPane.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;
import java.util.*;
import java.io.*;

public class SaveResults extends JFrame
{

    private JCheckBox saveColors;
    private JCheckBox saveVisibleIds;

    private EdgesPanel panel;

    private JButton write;
    private JButton comment;
    private JButton help;

    private JComboBox combo; // Stdout or 2 file
    private int format;
    private String[] comboOptions = { "Write to STDOUT" ,
				      "Write to File" };

    public static final int FILE_OUTPUT = 0;
    public static final int SCREEN_OUTPUT = 1;

    SaveResults( EdgesPanel p )
    {
	super("Save Results");
	setResizable( false );
	panel = p;




    }
    

    // Save ids that are currently labeled 
    public String getLabeledIds()
    {
	String s = new String();
	Vertex[] vertices = panel.getVertices();
	for ( int ii=0; ii<vertices.length; ++ii )
	    {
		if ( vertices[ii].doesShowID() )
		    {
			s += vertices[ii].id() + "\n";
		    }
	    }
	return s;
    }

    // Save all the edges that have non-default
    // colors
    public String getColoredEdges()
    {
	HashMap ce = panel.getEdgeColors();
	Iterator i = ce.entrySet().iterator();
	String s = new String();
	while ( i.hasNext() )   
	    {
		Map.Entry entry =  (Map.Entry) i.next();
		Edge e = (Edge) entry.getKey();
		s += e.vertex1().id() + " " + e.vertex2().id() + " ";
		Color c = (Color) entry.getValue();
		s += (float)c.getRed() / 255.0 + " ";
		s += (float)c.getGreen() / 255.0 + " ";
		s += (float)c.getBlue() / 255.0 + "\n";
	    }
	return s;
    }


    /////////////////////////////////////////////////////////
    // PRIVATE METHOD CALLS
    /////////////////////////////////////////////////////////

    private String getComment()
    {


	return "";
    }

    private String writeHeader( String comments )
    {



	return "";
    }

    private void getOutputFile()
    {




    }

}
