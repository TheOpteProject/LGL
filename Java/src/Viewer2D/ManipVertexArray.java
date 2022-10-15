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

public class ManipVertexArray implements Runnable{

    private int off;     // Array offset to start
    private int stride;  // Steps to skip each time
    private VertexFitter fitter;
    private Vertex[] vertices;
    private VertexStats stats;
    private Thread t;
    private String threadName;
    static final Object object = new Object();

    // CONSTRUCTORS

    public ManipVertexArray( Vertex[] verticesO ,String threadName)
    {
	// This is setup for 1 thread by default
	off = 0;
	stride = 1;
	vertices = verticesO;
    this.threadName = threadName;
    }
    Thread getThread()
    {
        return t;
    }

    public void run()
    {

	for ( int e = off; e < vertices.length; e += stride ) {
	    fitter.fitVertex( vertices[e] );
       // synchronized(object)
        {
	        stats.addStatsOfVertex( vertices[e] );
        }
	}
    }

    public void start () {
        System.out.println("Starting " +  threadName );
        if (t == null) {
           t = new Thread (this, threadName);
           t.start ();
        }
     }

    // ACCESSORS
    
    public void print() {
	System.out.println("Manip Vertex Arrray: ");
	System.out.println("Stride: " + stride + " Offset: " + off );
	System.out.println("Vertex Fitter: ");
	fitter.print();
    }

    public int getStride() { return stride; }
    public int getOffset() { return off; }
    public VertexFitter getFitter() { return fitter; }
    public Vertex[] getVertices() { return vertices; }
    public VertexStats getVertexStats() { return stats; }

    // MUTATORS

    public void setStride( int s ) { stride = s; }
    public void setOffset( int o ) { off = o; }
    public void setFitter( VertexFitter e ) { fitter = e; }
    public void setVertices( Vertex[] e ) { vertices = e; }
    public void setVertexStats( VertexStats s ) { stats = s; }

}
