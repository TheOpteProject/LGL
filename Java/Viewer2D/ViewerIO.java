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

import java.io.*;
import Jama.Matrix.*;
import Jama.*;
import java.util.*;
import java.awt.Color;

// This handles all the IO for the viewer.
// That just basically means reading in the
// file, checking for the format, and returning
// an array of edge objects. The dimension is set
// by the DIMENSION variable in Edge.java

public class ViewerIO {

    private File file;
    private Edge[] edges;
    private Vertex[] vertices;
    private final String DELIMETER = " \t";
    private FileInputHandler fileio;
    private VertexStats stats;
    private static final int DIMENSION = Vertex.DIMENSION;
    private int index , lineNumber;
    private int tokenCounter;
    private HashMap vertexIdMap;
    private HashMap edgeIdMap;
    private HashMap edgeColorMap;
    private HashMap vertexColorMap;
    private Vector edgesV;
    private Vector verticesV;

    public ViewerIO ( File filename ) throws FileNotFoundException , IOException {
	file = filename;
	index = 0; lineNumber = 0;
	edgeColorMap = new HashMap();
	vertexColorMap = new HashMap();
    }

    public void loadVertexCoords( File f ) throws IOException, FileNotFoundException
    {
	if ( vertexIdMap == null ) { return; } // Vertices/edges have to be loaded first
	FileInputHandler fi = new FileInputHandler( f.getAbsolutePath() );
	if ( stats != null ) { stats.clear(); }
	else { stats  = new VertexStats(); }
	while( fi.readNextLine() )
	    {
		String id = fi.getToken( 0 );
		//System.out.println(id);
		Vertex v1 = (Vertex) vertexIdMap.get( id );
		if ( v1 != null )
		    {
			Matrix m = v1.location();
			for ( int ii=1; ii<=DIMENSION; ++ii) {
			    m.set( ii-1 , 0 , fi.getTokenAsDouble(ii) );
			}
			if ( m.get(0,0) < 10000 )
			    stats.addStatsOfVertex( v1 );
		    }
	    } 
    }

    public void loadSHORTFile() throws IOException, FileNotFoundException
    {
	this.loadSHORTFile(file);
    }

    public void loadSHORTFile( File f ) throws IOException, FileNotFoundException
    {
	edgeIdMap = new HashMap();
	vertexIdMap = new HashMap();
	edgesV = new Vector();
	verticesV = new Vector();
	fileio = new FileInputHandler( file.getAbsolutePath() );	
	String v1 = null;
	while( fileio.readNextLine() )
	    {		
		if ( fileio.getTokenCount() == 0 ) { break; }
		String id1 = fileio.getToken( 0 );
		if ( id1.equals("#") ) { v1 = fileio.getToken(1); continue; }
		loadEdge( v1 , fileio.getToken(0) );
	    }
	tidyNewVerticesAndEdges();
    }



    // This just generates the edge relationships and not any
    // coordinates.
    public void loadLSFile() throws IOException, FileNotFoundException
    {
	this.loadLSFile(file);
    } 

    public void loadLSFile( File f ) throws IOException, FileNotFoundException
    {
	edgeIdMap = new HashMap();
	vertexIdMap = new HashMap();
	fileio = new FileInputHandler( file.getAbsolutePath() );
	fileio.readNextLine();
	index = 0;
	int vertexCount = fileio.getTokenAsInt(0);
	while(true)
	    {
		fileio.readNextLine();
		if ( fileio.getTokenAsInt(0) == -1 ||
		     fileio.getTokenCount() == 0 ) { break; }
		Vertex v = loadVertex( fileio.getToken(1) );
		int size = fileio.getTokenAsInt(0);
		loadGroup( fileio , v , size );
	    }
	tidyNewVerticesAndEdges();
    }

    public void loadEdgeColorFile( File f ) throws IOException, FileNotFoundException
    {
	if ( edges == null ) { return; }
	fileio = new FileInputHandler( f.getAbsolutePath() );
	while( fileio.readNextLine() )
	    {
		if ( fileio.getTokenCount() != 5 ){ throw new IOException(); }
		String id1 = fileio.getToken( 0 );
		String id2 = fileio.getToken( 1 );
		String edgeName = Edge.idEdge( id1 , id2 );
		Object o = edgeIdMap.get( edgeName );
		if ( o == null ) {
		    System.out.println("Undefined Edge: " + id1 + " " + id2); 
		    continue;
		}		
		edgeColorMap.put( (Edge)o , readColorRGB( 2 ) );
	    }
    } 

    public void loadVertexColorFile( File f ) throws IOException, FileNotFoundException
    {
	if ( edges == null || vertices == null ) { return; }
	fileio = new FileInputHandler( f.getAbsolutePath() );
	while( fileio.readNextLine() )
	    {
		if ( fileio.getTokenCount() != 4 ){ throw new IOException(); }
		String id = fileio.getToken( 0 );
		Object o = vertexIdMap.get( id );
		if ( o == null ) {
		    System.out.println("Undefined Vertex: " + id);
		    continue;
		}
		vertexColorMap.put( (Vertex)o , readColorRGB(1) );
	    }
    }

    ////////////////////////////////////////////////////////////
    // ACCESSORS
    ////////////////////////////////////////////////////////////

    public Edge[] getEdges() { return edges; }
    public VertexStats getStats() { return stats; }
    public Vertex[] getVertices() { return vertices; }    
    public HashMap getEdgeColorMap() { return edgeColorMap; }
    public HashMap getVertexColorMap() { return vertexColorMap; }
    public HashMap getVertexIdMap() { return vertexIdMap; }
    public HashMap getEdgeIdMap() { return edgeIdMap; }


    ////////////////////////////////////////////////////////////
    // MUTATORS
    ////////////////////////////////////////////////////////////

    public void clearAllEdgeColors() { edgeColorMap.clear(); }
    public void clearAllVertexColors() { vertexColorMap.clear(); }

    ////////////////////////////////////////////////////////////
    // PRIVATE METHOD CALLS
    ////////////////////////////////////////////////////////////

    private Vertex loadVertex( String id )
    {
	Object o = vertexIdMap.get( id );
	if ( o != null ) { return (Vertex)o; }
	Vertex v = new Vertex();
	v.id( id );
	vertexIdMap.put( id , v );
	verticesV.add( v );
	return v;
    }
    
    private Edge loadEdge( Vertex v1 , Vertex v2 )
    {
	String edgeName = Edge.idEdge(v1,v2);
	Object o = edgeIdMap.get( edgeName );
	if ( o != null ) { return (Edge)o; }
	Edge e = new Edge( v1 , v2 );
	edgeIdMap.put( edgeName , e );
	edgesV.add( e );
	return e;
    }

    private Edge loadEdge( String id1 , String id2 )
    {
	return loadEdge( loadVertex(id1) , loadVertex(id2) );
    }
    
    private void loadGroup( FileInputHandler f , Vertex v1 , int size ) throws IOException
    {
	for ( int ii=0; ii<size; ++ii )
	    {
		f.readNextLine();
		Vertex v2 = loadVertex( fileio.getToken(0) );
		loadEdge( v1 , v2 );
	    }
    }
    
    private void readLocationIntoVertex( Vertex v )
    {
	Matrix m = new Matrix( DIMENSION+1 , 1 );
	for ( int ii=0; ii<DIMENSION; ++ii) {
	    m.set( ii , 0 , fileio.getTokenAsDouble(ii+tokenCounter) );
	}
	tokenCounter += DIMENSION;
	v.location( m );
    }

    private void readColor( int index )
    {
	readColorRGB( index );
    }

    private Color readColorRGB( int index )
    {
	double r = fileio.getTokenAsDouble( index++ );
	double g = fileio.getTokenAsDouble( index++ );
	double b = fileio.getTokenAsDouble( index++ );
	return new Color( (float) r , (float) g , (float) b );
    }

    private void tidyNewVerticesAndEdges()
    {
	// This just loads the edges into an array and out of the
	// vector
	edges = new Edge[ edgesV.size() ];
	for ( int ii=0; ii<edgesV.size(); ++ii ) { 
	    edges[ii] = (Edge)edgesV.elementAt(ii);
	    //edges[ii].print();
	}
	edgesV.clear();

	vertices = new Vertex[ verticesV.size() ];
	for ( int ii=0; ii<verticesV.size(); ++ii ) {
	    vertices[ii] = (Vertex)verticesV.elementAt(ii);
	    vertices[ii].index( ii );
	    //vertices[ii].print();
	}
	verticesV.clear();
    }
}
