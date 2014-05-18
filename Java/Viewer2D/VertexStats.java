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

import Jama.*;
import Jama.Matrix.*;

public class VertexStats {

    private Matrix mins, maxs;
    private Matrix totals;  // Used to determine avg
    private int vertexCtr;
    private static final int DIMENSION = Vertex.DIMENSION;

    // CONSTRUCTORS
    public VertexStats() { 	
	mins = new Matrix(DIMENSION+1,1,Double.MAX_VALUE);
	maxs = new Matrix(DIMENSION+1,1,Double.MIN_VALUE);
	totals = new Matrix(DIMENSION+1,1,0.0);
	vertexCtr = 0;
	prepMatrices();
    }

    // MUTATORS
    public void addStatsOfVertex( Vertex v )
    {
	minMaxTest( v.location() ); 
	totals.plusEquals( v.location() );
	++vertexCtr;
    }

    // This tests both mins and maxs
    public void minMaxTest( Matrix m ) {
	// Only the firt DIMENSION elements need to be 
	// checked. The last element is just a place
	// holder
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    if ( m.get(ii,0) < mins.get(ii,0) ) {
		mins.set(ii,0,m.get(ii,0));
	    } else if ( m.get(ii,0) > maxs.get(ii,0) ) {
		maxs.set(ii,0, m.get(ii,0));
	    } 
	}
    }

    // This transfers stats from antother VertexStats object
    // to this one.
    // This makes it possible for mutliple vertexStats to work
    // different parts of the data set, and then integrate their
    // info into 1 object
    public void integrateVertexStats( VertexStats es ) {
	minMaxTest( es.mins ); minMaxTest( es.maxs );
	totals.plusEquals( es.totals );
	vertexCtr += es.vertexCtr;
    }

    // Reset to defaults, set ready for next read.
    public void clear() {
	vertexCtr = 0;
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    mins.set(ii,0,Double.MAX_VALUE);
	    maxs.set(ii,0,Double.MIN_VALUE);
	    totals.set(ii,0,0.0);
	}
    }

    public void setMin( Matrix m ) { mins = m; }
    public void setMax( Matrix m ) { maxs = m; }

    // ACCESSORS
    public double vertexCount() { return vertexCtr; }
    public double min( int d ) { return mins.get(d,0); }
    public double max( int d ) { return maxs.get(d,0); }
    public double span( int d ) { return maxs.get(d,0) - mins.get(d,0); }

    public boolean fitsSpan( Matrix m )
    {
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    if ( m.get(ii,0) < mins.get(ii,0) ||
		 maxs.get(ii,0) < m.get(ii,0) ) {
		return false;
	    }
	}
	return true;
    }

    public double avg( int d ) { return (totals.get(d,0)/(double)vertexCtr); }
    public Matrix mins() { return mins; }
    public Matrix maxs() { return maxs; }

    public void print() {
	System.out.println("\tS T A T S");
	System.out.println("Mins: "); mins.print(4,2);
	System.out.println("Maxs: "); maxs.print(4,2);
	System.out.println("Totals: "); totals.print(4,2);
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    System.out.println("Avg" + ii + ": " + avg(ii) );
	}
	System.out.println("Vertices Counted: " + vertexCtr);
    }

    // PRIVATE METHOD CALLS
    private void prepMatrices()
    {
	mins.set(DIMENSION,0,1);
	maxs.set(DIMENSION,0,1);
    }

}
