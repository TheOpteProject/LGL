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

public class FindVertexFrame extends JFrame
{

    // Text field provides space to input the vertex
    // names.
    JScrollPane vertexList;
    GridBagLayout layout;

    // Buttons for dealing with submission/clearing the 
    // text fields
    JButton submit;
    JButton clear;
    JButton help;

    Color color_edge;
    JCheckBox colorEdges; 
    boolean willColorEdges;

    Color color_vertex;
    JCheckBox colorVertices;
    boolean willColorVertices;

    JCheckBox label;
    boolean labelVertices;

    JCheckBox zoom2region;
    boolean zoom;

    JTextArea textList;

    EdgesPanel panel;
    String[] vertices;

    public FindVertexFrame( String title , EdgesPanel p )
    {
	super(title);
	setResizable( false );
	
	panel = p;

	color_edge = Color.red;
	color_vertex = Color.red;

	layout = new GridBagLayout();
	Container c = getContentPane();
	c.setLayout( layout );
	GridBagConstraints constraints = new GridBagConstraints();

	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = new Insets(1,1,1,1);

	// SUBMIT BUTTON
	submit = new JButton("Submit");
	submit.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    try {
			String text = textList.getText();
			readInIds( text );
			panel.incomingVertices( FindVertexFrame.this );
		    } catch ( NullPointerException ee ) {
			// textList.getText() is the culprit
			// Just do nothing, probably hit
			// the submit button by accident
		    }
		}
	    }
				  );
	
	layout.setConstraints( submit , constraints );
	c.add( submit );

	// HELP BUTTON
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	help = new JButton("Help");
	help.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    JOptionPane.showMessageDialog( null , 
						   generateHelpMessage() , 
						   "Help", 
						   JOptionPane.INFORMATION_MESSAGE 
						   );
		}
	    }
				);
	layout.setConstraints( help , constraints );
	c.add( help );

	// CLEAR BUTTON
	clear = new JButton("Clear");
	clear.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    textList.setText("");
		    zoom2region.setSelected( false );
		    colorEdges.setSelected( false );
		    colorVertices.setSelected( false );
		    label.setSelected( false );
		}
	    }
				 );
	layout.setConstraints( clear , constraints );
	c.add( clear );

	// THE LIST OF VERTICES
	textList = new JTextArea( 10 , 20 );
	textList.setFont( new Font( "Helvetica", Font.PLAIN, 13) );
	vertexList = new JScrollPane( textList );
	vertexList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	vertexList.setBorder( BorderFactory.createCompoundBorder( 
           BorderFactory.createCompoundBorder(
					      BorderFactory.createTitledBorder("Vertices to Find"),
					      BorderFactory.createEmptyBorder(5,5,5,5)),
	   textList.getBorder()));
 	layout.setConstraints( vertexList , constraints );
 	c.add( vertexList );

	// ZOOM TO THE REGION
	zoom = false; // Off by default
	zoom2region = new JCheckBox("Zoom to Region");
	zoom2region.setSelected( zoom );
	zoom2region.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( zoom2region.isSelected() ) {
			zoom = true;
		    } else {
			zoom = false;
		    }
		}
	    }
				       );

	layout.setConstraints( zoom2region , constraints );
	c.add( zoom2region );

	// CHECK TO SEE IF YOU WANT THE EDGES LABELED
	labelVertices = false; // Off by default
	label = new JCheckBox("Show IDs");
	label.setSelected( labelVertices );
	label.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( label.isSelected() ) {
			labelVertices = true;
		    } else {
			labelVertices = false;
			if ( vertices != null ) {
			    // Turn off previously labeled vertices
			    panel.setIncomingLabelsAndStats( vertices , null , false , false );
			}
		    }
		}
	    }
				 );
	
	layout.setConstraints( label , constraints );
	c.add( label );

	// CHECK IF YOU WANT THE EDGES HIGHLIGHTED
	colorEdges = new JCheckBox("Color Edges");
	willColorEdges = false;
	colorEdges.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( colorEdges.isSelected() ) {
			color_edge = JColorChooser.showDialog( FindVertexFrame.this,
							       "Edge Color", 
							       color_edge ); 
			willColorEdges = true;

		    } else {
			willColorEdges = false;
		    }
		}
	    } );
	layout.setConstraints( colorEdges , constraints );
	c.add( colorEdges );

	// CHECK IF YOU WANT THE VERTICES HIGHLIGHTED
	colorVertices = new JCheckBox("Color Vertices");
	willColorVertices = false;
	colorVertices.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    if ( colorVertices.isSelected() ) {
			color_vertex = JColorChooser.showDialog( FindVertexFrame.this,
								 "Vertex Color", 
								 color_vertex ); 
			willColorVertices = true;

		    } else {
			willColorVertices = false;
		    }
		}
	    } );
	layout.setConstraints( colorVertices , constraints );
	c.add( colorVertices );

	pack();
	show();
    }


    // ACCESSORS
    public Color getEdgeColor() { return color_edge; }
    public Color getVertexColor() { return color_vertex; }
    public boolean willZoom() { return zoom; }
    public boolean willColorEdges() { return willColorEdges; }
    public boolean willColorVertices() { return willColorVertices; }
    public String[] getIds() { return vertices; }
    public boolean willLabel() { return labelVertices; }

    // PRIVATE METHOD CALLS
    private void readInIds( String t )
    {
	// Now each id should be one word with \n
	// at the end
	Vector tokens = new Vector();
	StringTokenizer tokenizer = new StringTokenizer( t , " \n\t" );
	while ( tokenizer.hasMoreTokens() ) {
	    tokens.addElement( tokenizer.nextElement() );
	}
	// Now to copy the tokens to the string array
	vertices = new String[ tokens.size() ];
	for ( int ii=0; ii<tokens.size(); ++ii ) {
	    vertices[ii] = (String) tokens.elementAt(ii);
	    // System.out.println("-" + vertices[ii] + "-");
	}
    }

    private String generateHelpMessage()
    {
	return "Paste the list of vertex ids you wish to\n" +
	    "find into the text box, and press \'Submit\'.\n" +
	    "The ids can be separated by any whitespace\n" +
	    "character.\n\n" +
	    "\'Clear\' - Clears the text box.\n\n" +
	    "\'Zoom to region\' - Zooms to the average\n position of all" +
	    " given vertices\n\n\'Show IDs\' - Show the ids of "+
	    "the given\nvertices." +
	    "\n\n\'Color Edges\' - Color ANY and ALL edges\n" +
	    "any of the given vertices may share.\n" +
	    "A color given here overrides the previous color of\n" +
	    "the edge." +
	    "\n\n\'Color Edges\' - Color all provided vertices.\n";
    }

}
