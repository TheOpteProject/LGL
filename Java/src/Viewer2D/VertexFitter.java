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
//import Jama.Matrix.*;

// This class is used to fit vertexes to a window

public class VertexFitter {

    private Matrix manipulation;
    //private static final int DIMENSION = Vertex.DIMENSION;

    // CONSTRUCTORS
    VertexFitter( Matrix m ) { manipulation = m; }

    VertexFitter() { manipulation = null; }

    VertexFitter( Transformer t ) {
	manipulation = t.getTransformationMatrix();
    }

    // ACCESSORS

    public void print() {
	System.out.println("VertexFitter Manip. Matrix:");
	manipulation.print(5,3);
    }

    public Matrix getManipulationMatrix() { return manipulation; }

    // MUTATOR

    // This method returns a new vertex fitted given the transformations
    public void fitVertex( Vertex original )
    {
	original.location( manipulation.times( original.location() ) );
    }

  

    // This is to add a manipulation to the 'to do' list.
    // This permits composite manipulations
    public void addManipulation( Transformer manip )
    {
	if ( manipulation == null ) {
	    manipulation = manip.getTransformationMatrix();
	} else {
	    manipulation = 
		manipulation.times( manip.getTransformationMatrix() );
	}
    }

    public void clear() { manipulation = null; }

    public void setManipulationMatrix( Matrix m ) { manipulation = m; }

}
