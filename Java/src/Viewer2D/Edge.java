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

//import Jama.*;
//import Jama.Matrix.*;

public class Edge {

    // Each row of the matrix is a dimension with row 0
    // as x, row 1 as y etc...
    private Vertex left;  // Vertex 1 of edge
    private Vertex right; // Vertex 2 of edge
    public static final int DIMENSION = Vertex.DIMENSION;

    ////////////////////////////////////////////////////////////    
    // CONSTRUCTORS
    ////////////////////////////////////////////////////////////

    public Edge() { left = null; right = null; }

    public Edge( Vertex m1 , Vertex m2 ) 
    {
	left = m1; right = m2;
    }

    ////////////////////////////////////////////////////////////
    // MUTATORS
    ////////////////////////////////////////////////////////////

    public void vertex1( Vertex m ) { left=m; }
    public void vertex2( Vertex m ) { right=m; }

    ////////////////////////////////////////////////////////////
    // ACCESSORS
    ////////////////////////////////////////////////////////////

    public Vertex vertex1() { return left; }
    public Vertex vertex2() { return right; }

    public void print() {
	System.out.println("Edge: ");
	left.print(); 
	right.print();
    }

    ////////////////////////////////////////////////////////////
    // STATIC METHOD CALLS
    ////////////////////////////////////////////////////////////

    // These 2 methods provide a consistent naming scheme for any
    // edge.

    public static String idEdge( Edge e ) 
    {
	return idEdge( e.vertex1() , e.vertex2() );
    }

    public static String idEdge( Vertex v1 , Vertex v2 )
    {
	return idEdge( v1.id() , v2.id() );
    }

    public static String idEdge( String id1 , String id2 )
    {
	// This will return an id where the string
	// less than the other is returned first.
	if ( id1.compareTo( id2 ) >= 0 ) {
	    return id1 + " " + id2;
	} else {
	    return id2 + " " + id1;
	}
    }

}
