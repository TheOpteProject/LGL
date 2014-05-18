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

public class FindEdgesFrame extends JFrame
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

    HashMap edgeColors;
    Color color;
    JCheckBox colorEdges; 

    JCheckBox label;   
    boolean labelVertices;

    JCheckBox zoom2region;
    boolean zoom;

    boolean willColor;
   
    JTextArea textList;

    EdgesPanel panel;
    String[] vertices;

    public FindEdgesFrame( String title , EdgesPanel p )
    {
	super(title);
	setResizable( false );
	
	panel = p;

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
			panel.incomingEdges( FindEdgesFrame.this );
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

	// CLEAR BUTTON
	clear = new JButton("Clear");
	clear.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    textList.setText("");
		}
	    }
				 );
	layout.setConstraints( clear , constraints );
	c.add( clear );

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

	// THE LIST OF EDGES
	textList = new JTextArea( 10 , 20 );
	textList.setFont( new Font( "Helvetica", Font.PLAIN, 13) );
	vertexList = new JScrollPane( textList );
	vertexList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	vertexList.setBorder( BorderFactory.createCompoundBorder( 
           BorderFactory.createCompoundBorder(
					      BorderFactory.createTitledBorder("Edges to Find"),
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
	edgeColors = new HashMap();
	willColor = false;
	colorEdges = new JCheckBox("Color Edges");
	colorEdges.addActionListener( new ActionListener()
	    {
		public void actionPerformed( ActionEvent e )
		{
		    color = Color.red;
		    if ( colorEdges.isSelected() ) {
			color = JColorChooser.showDialog( FindEdgesFrame.this,
							  "Edge Color", 
							  color );
			willColor = true;
		    } else {
			willColor = false;
			if ( edgeColors != null ) {
			    panel.removeEdgeColors( panel.setEdgeColorsFromFindEdgesFrame( getIds() ,
											   color ) );
			}
			
		    }
		}
	    }
				       );

	layout.setConstraints( help , constraints );
	c.add( colorEdges );

	pack();
	show();
    }


    // ACCESSORS

    public HashMap getEdgeColors() { return edgeColors; }
    public Color getEdgeColor() { return color; }
    public boolean willZoom() { return zoom; }
    public String[] getIds() { return vertices; }
    public boolean willLabel() { return labelVertices; }
    public boolean willColor() { return willColor; }

    public void setEdgeColors( Color c )
    {
	if ( vertices != null )
	    {
		for ( int ii=0; ii<vertices.length; ++ii )
		    {
			//edgeColors.put( 
		    }
	    }
    }

    // PRIVATE METHOD CALLS
    private void readInIds( String t ) throws NoSuchElementException
    {
	// Now each id should be one word with \n
	// at the end
	Vector v1 = new Vector();
	Vector v2 = new Vector();
	StringTokenizer tokenizer = new StringTokenizer( t , " \t\n" );
	int i=0;
	while ( tokenizer.hasMoreTokens() ) {
	    v1.addElement( tokenizer.nextElement() );
	    //System.out.println( (String) v1.elementAt(i) ); System.out.flush();
	    v2.addElement( tokenizer.nextElement() );
	    //System.out.println( (String) v2.elementAt(i++) ); System.out.flush();
	}
	// Now to copy the tokens to the string array
	// This will load the ids in alternating order.
	vertices = new String[ v1.size() * 2 ];
	i=0;
	for ( int ii=0; ii<v1.size(); ++ii ) {
	    vertices[i++] = (String) v1.elementAt(ii);
	    //System.out.println( vertices[i-1] );
	    vertices[i++] = (String) v2.elementAt(ii);
	    //System.out.println( vertices[i-1] );
	}
    }

    private String generateHelpMessage()
    {
	return "Paste the list of vertex ids you wish to\n" +
	    "find into the text box, and press \'Submit\'.\n" +
	    "The edges must be one per line (2 vertices per\n" +
	    "line) or in some sort of alternating format " +
	    "whitespace delimited.\n\n" +
	    "\'Clear\' - Clears the text box.\n\n" +
	    "\'Zoom to region\' - Zooms to the region of all\n" +
	    " given vertices.\n\n\'Show IDs\' - Show the ids of "+
	    "the given\nvertices." +
	    "\n\n\'Color Edges\' - Color the selected edges.\n" +
	    "A color given here overrides the previous color of\n" +
	    "the edge. When you undo the selection, the default\n" +
	    "color will then be given to the edge and not the\nprevious " +
	    "color. Thus, the original color is lost.\n";
    }

}
