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
import java.lang.Math;

// This class is used to generate matrices that
// perform a given transformation/maninpulation.
// It is basically a wrapper for a matrix with a
// palatable interface

public class Transformer {

    private Matrix transformation;
    private static final int DIMENSION = Edge.DIMENSION;

    public Transformer()
    {
	transformation = Matrix.identity( DIMENSION+1 , DIMENSION+1 );
    }

    public Transformer( Transformer t )
    {
	this.transformation = t.transformation;
    }

    // MUTATORS

    // This will make the transformer one that will move
    // points by a given amount.
    // Symmetric move
    public void move( double distance )
    {
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    transformation.set(ii,DIMENSION,distance);
	}
    }

    // Move by a given array amount
    public void move( double[] distances )
    {
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    transformation.set(ii,DIMENSION,distances[ii]);
	}
    }

    // This will make the transformer one that will scale
    // points by a certain amount.
    // Symmetric scaling
    public void scale( double s )
    {
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    transformation.set(ii,ii,s);
	}
    }

    // Scale by a given array amount
    public void scale( double[] s )
    {
	for ( int ii=0; ii<DIMENSION; ++ii ) {
	    transformation.set(ii,ii,s[ii]);
	}
    }

    // This will make the transformer one that will rotate
    // points by a certain amount.
    public void rotate_cc_2D( double theta )
    {
	transformation.set(0,0,Math.cos(theta));
	transformation.set(0,1,-Math.sin(theta));
	transformation.set(1,0,Math.sin(theta));
	transformation.set(1,1,-Math.cos(theta));
    }

    // ACCESSORS

    // This will return the resulting transformation matrix
    public Matrix getTransformationMatrix() { return transformation; }

    public void print()
    {
	System.out.println("Transformation Matrix: ");
	transformation.print(5,3);
    }

}
