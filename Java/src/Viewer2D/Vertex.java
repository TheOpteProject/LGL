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

import Jama.Matrix;

public class Vertex implements Comparable {
	private Matrix locus;
	private int ndx;
	private String name;
	// private String descrip;
	private boolean showid;
	private boolean showDescrip;
	private boolean showV;
	static public final int DIMENSION = 2;

	// CONSTRUCTORS
	public Vertex() {
		ndx = 0;
		locus = new Matrix(DIMENSION + 1, 1);
		name = null;
		matrixPrep();
		varPrep();
	}

	public Vertex(Matrix m, int ndx, String n) {
		this.ndx = ndx;
		locus = m;
		name = n;
		varPrep();
	}

	// MUTATORS
	public void index(int i) {
		ndx = i;
	}

	public void location(Matrix l) {
		locus = l;
		matrixPrep();
	}

	public void id(String s) {
		name = s;
	}

	// public void description( String s ) { descrip = s; }
	public void showID(boolean b) {
		showid = b;
	}

	public void showDescription(boolean b) {
		showDescrip = b;
	}

	public void showVertex(boolean b) {
		showV = b;
	}

	// ACCESSORS
	public void print() {
		System.out.println("Vertex " + name + " with index " + ndx);
		// System.out.println("Descrip: " + descrip );
		System.out
				.println("ShowID: " + showid + " ShowDescrip: " + showDescrip);
		locus.print(5, 3);
	}

	public int index() {
		return ndx;
	}

	public Matrix location() {
		return locus;
	}

	public String id() {
		return name;
	}

	// public String description() { return descrip; }
	public boolean doesShowID() {
		return showid;
	}

	public boolean doesShowDescription() {
		return showDescrip;
	}

	public boolean doesShowVertex() {
		return showV;
	}

	// ---------------------------------------------------------
	// COMPARABLE IMPLEMENTATION
	// ---------------------------------------------------------

	public boolean equals(Object o) {
		if (!(o instanceof Vertex))
			return false;
		Vertex v = (Vertex) o;
		return name.equals(v.id());
	}

	public int compareTo(Object o) {
		Vertex v = (Vertex) o;
		return name.compareTo(v.id());
	}

	// ---------------------------------------------------------

	// PRIVATE METHOD CALLS
	private void matrixPrep() {
		// This just makes sure the matrices
		// are transformation ready
		locus.set(DIMENSION, 0, 1);
	}

	private void varPrep() {
		showid = false;
		showDescrip = false;
		showV = false;
	}

}
