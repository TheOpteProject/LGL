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

public class FormatVertex
{
    private Vertex[] vertices;
    private VertexStats stats;
    private int[] windowSizes; // X = 0 , Y = 1 etc
    private int threadCount;
    private static int DIMENSION = Vertex.DIMENSION;
    private VertexFitter fitter;

    // CONSTRUCTORS
    public FormatVertex ( Vertex[] e , VertexStats stats ,
			int[] wSizes , int threads2use )
    {
	vertices = e;
	this.stats = stats;
	windowSizes = wSizes;
	threadCount = threads2use;
	fitter = new VertexFitter();
    }

    // MUTATORS

    public void fitDataToWindow() {
	fitter.clear();
	// Here the appropriate translation/scaling matrices
	// must be generated and applied to the originalVertices.	
	// Stretch the vertices to fit the window
	scalingIssues();

	// Recenter the vertices based on the avg of max and min values
	translationIssues();

	applyTransformation();
    }

    public void setVertices( Vertex[] e ) { this.vertices = e; }
    public void setStats( VertexStats stats ) { this.stats = stats; }
    public void setWindowSizes( int[] y ) { windowSizes=y; }
    public void setWindowSizeX( int x ) { windowSizes[0]=x; }
    public void setWindowSizeY( int y ) { windowSizes[1]=y; }
    public void threads( int t ) { threadCount = t; }

    // ACCESSORS
    public Vertex[] getVertices() { return vertices; }
    public VertexStats getStats() { return stats; }
    public int[] windowSizes() { return windowSizes; }
    public int threads() { return threadCount; }
    public VertexFitter getFitter() { return fitter; }

    public void print()
    {
	System.out.println("Formatter Info:");
	fitter.print();
    }

    ///////////////////////////////////////////////////////////////////////
    // PRIVATE METHOD CALLS
    ///////////////////////////////////////////////////////////////////////

    private void translationIssues()
    {
	double[] offsets = new double[DIMENSION];
	for ( int d=0; d<DIMENSION; ++d ) {
	    offsets[d] -= stats.min(d);
	}
	Transformer transformer = new Transformer();
	transformer.move( offsets );
	// Add this job to the list
	fitter.addManipulation( transformer );
    }

    private void scalingIssues()
    {
	double scale = windowSizes[0]/stats.span(0);
	for ( int d=1; d<DIMENSION; ++d ) {
	    double newScale = windowSizes[d]/stats.span(d);
	    if ( newScale < scale ) {
		scale = newScale;
	    }
	}
	Transformer transformer = new Transformer();
	transformer.scale(.99*scale );
	fitter.addManipulation( transformer );
    }

    private void applyTransformation()
    {
	// Now to do the work to each vertex
	for ( int t=0; t<threadCount; ++t ) {
	    ManipVertexArray manip = 
		new ManipVertexArray( vertices );
	    manip.setFitter( fitter );
	    manip.setStride( threadCount );
	    manip.setOffset( t );
	    manip.setVertexStats( stats ); // Fix this if threading (race cond.)
	    manip.run();
	}
    }

}
