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
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.filechooser.*;
import java.io.*;

public class VertexDescripTable extends JFrame {

    private JTable table;
    private Vector rows, columnNames;
    
    private HashMap vertexDescriptions;
    
    private JButton idSort;
    private JButton descripSort;
    private JButton help;

    // Where the vertices/edges are drawn.
    private EdgesPanel panel;

    private JScrollPane scrollPane;
    private Dimension size;

    private JComboBox comboBox;
    private int format;
    private String[] comboOptions = { "Show All" ,
				      "Show Only Visible",
				      "Show Only Highlighted" };

    public static final int SHOW_ALL = 0;
    public static final int SHOW_ONLY_VISIBLE = 1;
    public static final int SHOW_ONLY_HIGHLIGHTED = 2;
    
    public static final int TABLE_X = 350;
    public static final int TABLE_Y = 500;

    private GridBagLayout layout;

    VertexDescripTable( EdgesPanel p , HashMap vertexDescrip )
    {
	super("Vertex Descriptions");
	panel = p;

	this.vertexDescriptions = vertexDescrip;

	size = new Dimension( TABLE_X , TABLE_Y );

	// INITIALIZE FRAME LAYOUT
	layout = new GridBagLayout();
	Container c = getContentPane();
	c.setLayout( layout );
	c.setFont(new Font("Helvetica", Font.PLAIN, 14));
	GridBagConstraints constraints = new GridBagConstraints();
	constraints.fill = GridBagConstraints.HORIZONTAL;
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = new Insets(1,1,1,1);
	constraints.weighty = 0.0;

	// REORDER ROWS BASED ON ID
	idSort = new JButton("Sort By ID");
	idSort.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    sortBasedOnID();
		}
	    } );
	constraints.weighty = 0.0;
	layout.setConstraints( idSort , constraints );
	c.add( idSort );

	// REORDER ROWS BASED ON DESCRIPTIONS
	descripSort = new JButton("Sort By Descrip");
	descripSort.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    sortBasedOnDescrip();
		}
	    } );
	constraints.weighty = 0.0;
	layout.setConstraints( descripSort , constraints );
	c.add( descripSort );	

	// UPDATE THE TABLE
	JButton update = new JButton("Update");
	update.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    updateTable();
		}
	    } );
	constraints.weighty = 0.0;
	layout.setConstraints( update , constraints );
	c.add( update );

	// WRITE CURRENT TABLE TO FILE
	JButton save = new JButton("Save");
	save.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    saveTable2File();
		}
	    } );
	constraints.weighty = 0.0;
	layout.setConstraints( save , constraints );
	c.add( save );

	// SHOW HELP MESSAGE
	help = new JButton("Help");
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	help.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    JOptionPane.showMessageDialog( null , 
						   generateHelpMessage() , 
						   "Help", 
						   JOptionPane.INFORMATION_MESSAGE );
		}
	    } );
	constraints.weighty = 0.0;
	layout.setConstraints( help , constraints );
	c.add( help );

	// INITIALIZE TABLE
	rows = new Vector();
	loadDataIntoRows( panel.getVertices() );
	columnNames = new Vector();
	columnNames.addElement("Vertex");
	columnNames.addElement("Description");
	table = new JTable( rows , columnNames );
	table.setPreferredScrollableViewportSize( new Dimension( 2*TABLE_X ,
								 2*TABLE_Y ) );
	table.setAutoResizeMode( javax.swing.JTable.AUTO_RESIZE_OFF );
	scrollPane = new JScrollPane( table );
	scrollPane.setPreferredSize(size);
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	constraints.weightx = 1.0;
	constraints.weighty = 1.0;
	layout.setConstraints( scrollPane , constraints );
	c.add( scrollPane );

	// FORMAT FOR SHOWING DESCRIPTIONS
	comboBox = new JComboBox( comboOptions );
	format = SHOW_ALL;
	comboBox.setSelectedIndex( format );
	comboBox.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    JComboBox cb = (JComboBox)e.getSource();
		    format = cb.getSelectedIndex();
		    updateTable();
		}
	    } );
	constraints.gridwidth = GridBagConstraints.REMAINDER;
	layout.setConstraints( comboBox , constraints );
	c.add( comboBox );

	addMouseListener( new VDTMouseListener() );
	setResizable( false );

	// DONE
	pack();
	show();
    }

    // Sort the rows of the table based on string compared vertex
    // ids.
    public void sortBasedOnID()
    {
	Vector ids = new Vector();
	HashMap ids2vertex = new HashMap();
	Vertex[] vertices = panel.getVertices();
	for ( int ii=0; ii<vertices.length; ++ii )
	    {
		if ( doesShow( vertices[ii] ) )
		    {
			ids.add( vertices[ii].id() );
			ids2vertex.put( vertices[ii].id() , vertices[ii] );
		    }
	    }
	Collections.sort( ids );
	Vector newRows = new Vector();
	for ( int ii=0; ii<ids.size(); ++ii )
	    {
		Vector rowData = new Vector();
		String id = ( String ) ids.elementAt(ii);
		rowData.addElement( id );
		rowData.addElement( vertexDescriptions.get( (Vertex) ids2vertex.get( id ) ) );
		newRows.add( rowData );
	    }
	clearTableAndInputData( newRows );
    }

    // Sort the rows of the table based on string compared vertex
    // descriptions
    public void sortBasedOnDescrip()
    {
	Vector descrip = new Vector();
	HashMap descrip2index = new HashMap();
	Vertex[] vertices = panel.getVertices();
	for ( int ii=0; ii<vertices.length; ++ii )
	    {
		if ( doesShow( vertices[ii] ) )
		    {
			// You have to add the id as well since the
			// descriptions may not be unique
			String d = new String( (String) vertexDescriptions.get( vertices[ii] ) + 
					       vertices[ii].id() );
			descrip.add( d );
			descrip2index.put(  d , vertices[ii] );
		    }
	    }
	Collections.sort( descrip );
	Vector newRows = new Vector();
	for ( int ii=0; ii<descrip.size(); ++ii )
	    {
		Vector rowData = new Vector();
		String d = ( String ) descrip.elementAt(ii);
		rowData.addElement( ( (Vertex)descrip2index.get( d )).id() );
		rowData.addElement( vertexDescriptions.get( (Vertex) descrip2index.get( d ) ) );
		newRows.add( rowData );
	    }
	clearTableAndInputData( newRows );
    }

    // Just redraw the table
    public void updateTable()
    {
	Vector newRows = new Vector();
	Vertex[] vertices = panel.getVertices();
	for ( int ii=0; ii<vertices.length; ++ii ) {
	    if ( doesShow( vertices[ii] ) ) {
		Vector rowData = new Vector();
		rowData.addElement( vertices[ii].id() );
		rowData.addElement( vertexDescriptions.get( vertices[ii] ) );
		newRows.add( rowData );
	    }
	}
	clearTableAndInputData( newRows );
    }

    // Determines whether the provided vertex will show its
    // description in the table
    public boolean doesShow( Vertex v )
    {
	if ( vertexDescriptions.get( v ) == null ) 
	    return false;
	// Now to look at the format to see which
	// vertices are shown
	if ( format == SHOW_ALL ) {
	    return true;
	} else if ( format == SHOW_ONLY_VISIBLE ) {
	    return panel.visible( v.location() );
	} else if ( format == SHOW_ONLY_HIGHLIGHTED ) {
	    return v.doesShowID();
	}
	return false; // Should never be here
    }

    //////////////////////////////////////////////////////////////
    // PRIVATE METHOD CALLS
    //////////////////////////////////////////////////////////////

    private void saveTable2File()
    {
	// Request a file name...
	JFileChooser chooser = new JFileChooser();
	int returnVal = chooser.showSaveDialog( VertexDescripTable.this );
	if ( returnVal == JFileChooser.APPROVE_OPTION )
	    {
		File tableFile = chooser.getSelectedFile();
		if ( tableFile == null || tableFile.getName().equals("") )
		    {
			JOptionPane.showMessageDialog( null , "Invalid File Name" ,
						       "Error" ,
						       JOptionPane.ERROR_MESSAGE );	
		    }
		else
		    {
			String buffer = new String();
			for ( int row=0; row<table.getRowCount(); ++row )
			    {
				String tmp = new String();
				for ( int col=0; col<table.getColumnCount(); ++col )
				    {
					tmp += (String) table.getValueAt( row, col );
				    }
				if ( tmp.equals("") ) { continue; }
				buffer += tmp + "\n";
			    }
			try {
			    FileWriter out = new FileWriter( tableFile );
			    out.write( buffer );
			    out.flush();
			    out.close();
			} catch ( IOException ee ) {
			    JOptionPane.showMessageDialog( null , "Error On Write" ,
							   "Error" ,
							   JOptionPane.ERROR_MESSAGE );
			}
		    }
	    }
    }

    private void loadDataIntoRows( Vertex[] v )
    {
	for ( int ii=0; ii<v.length; ++ii )
	    {
		if ( doesShow( v[ii] ) )
		    {
			Vector data = new Vector();
			data.addElement( v[ii].id() );
			data.addElement( vertexDescriptions.get( v[ii] ) );
			rows.addElement( data );
		    }
	    }
    }

    private void clearTableAndInputData( Vector newData )
    {
	table.setAutoCreateColumnsFromModel( true );
	for ( int ii=0; ii<newData.size(); ++ii ) {
	    Vector row = (Vector) newData.elementAt( ii );
	    for ( int jj=0; jj<row.size(); ++jj ) {
		table.setValueAt( row.elementAt(jj) , ii , jj );
	    }
	}
	// Zero out the rest of the entries
	for ( int ii=newData.size(); ii<table.getRowCount(); ++ii ) {
	    if ( ii >= rows.size() ) { break; }
	    Vector row = (Vector) rows.elementAt( ii );
	    for ( int jj=0; jj<row.size(); ++jj ) {
		table.setValueAt( "" , ii , jj );
	    }
	}
	rows = newData;
    }
    
    private String generateHelpMessage()
    {
	return "\nSort by ID - Sort the rows of the table\n" +
	    "\tbased on the Vertex ID (String based sort).\n\n" +
	    "Sort by Description - Sort the rows of the table\n" +
	    "\tbased on the Vertex Description (String based sort).\n\n" +
	    "\'Update\' - Update the description table.\n\n" +
	    "\'Save\' - This will save the current table (both columns)\n" +
	    "to a specified file.\n\n" +
	    "\'Option Box\' - This allows one to determine which\n" +
	    "\tdescriptions are shown in the table.\n\n" +
	    "ISSUES: The main window is not resizeable, but the\n" +
	    "columns are resizable even beyond the panel size.\n" +
	    "Although these text fields are editable, this is not\n" +
	    "intentional. Any changes made to these fields will not\n" +
	    "change the actual data.\n";
    }

    /////////////////////////////////////////////////////////////////////

    class VDTMouseListener extends MouseInputAdapter
    {
	public void mouseReleased( MouseEvent e )
	{
	    if ( sizeChanged(e) ) { makeChanges(); }
	}
	
	public void mouseEntered( MouseEvent e )
	{ 
	    //if ( sizeChanged(e) ) { makeChanges(); }
	}
	
	public void mouseExited( MouseEvent e )
	{
	    //if ( sizeChanged(e) ) { makeChanges(); }
	}

	public boolean sizeChanged( MouseEvent e )
	{
	    return ! size.equals( getSize() );
	}
	
	public void makeChanges()
	{
	    size = VertexDescripTable.this.getSize();
	    size.height -= 100;
	    scrollPane.setPreferredSize(size);
	    table.setPreferredSize(size);
	    table.revalidate();
	    scrollPane.revalidate();
	    pack();
	    repaint();
	}

	public void mousePressed( MouseEvent e ) 
	{
	    //if ( sizeChanged(e) ) { makeChanges(); }
	}


	public void mouseClicked( MouseEvent e ) { }
    }
    
}
