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

import java.util.HashMap;

import Jama.Matrix;

public class FormatVertex {
	private Vertex[] vertices;
	private VertexStats stats;
	private int[] windowSizes; // X = 0 , Y = 1 etc
	private int threadCount;
	private static int DIMENSION = Vertex.DIMENSION;
	private VertexFitter fitter;
	private HashMap<Vertex,Label> labels;
	private double scaleBy;
	private double scaleCorrectionLabels;
	private double scaleCorrectionLabelsOriginal;
	private boolean scaleLabelUsed;
	private double minX, maxX;
	private double minY, maxY;
	boolean aligncenter;

	// CONSTRUCTORS
	public FormatVertex(Vertex[] e, HashMap<Vertex,Label> labels,double scaleLabels,double minX, double minY,double maxX,double maxY,
	boolean aligncenter,
			VertexStats stats, int[] wSizes,
			int threads2use) {
		vertices = e;
		this.stats = stats;
		windowSizes = wSizes;
		threadCount = threads2use;
		this.labels =  labels;
		fitter = new VertexFitter();
		this.scaleCorrectionLabels =  scaleLabels;
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.aligncenter = aligncenter;
		scaleBy = 1;
		scaleLabelUsed = false;
	
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

	public void setVertices(Vertex[] e) {
		this.vertices = e;
	}

	public void setStats(VertexStats stats) {
		this.stats = stats;
	}

	public void setWindowSizes(int[] y) {
		windowSizes = y;
	}

	public void setWindowSizeX(int x) {
		windowSizes[0] = x;
	}

	public void setWindowSizeY(int y) {
		windowSizes[1] = y;
	}

	public void setLabelScale(double scale)
	{

		scaleCorrectionLabels = scale/(scaleLabelUsed? scaleCorrectionLabelsOriginal: scaleCorrectionLabels);
		scaleLabelUsed = false;
		scaleLabels(1);

	}


	public void threads(int t) {
		threadCount = t;
	}

	// ACCESSORS
	public Vertex[] getVertices() {
		return vertices;
	}

	public VertexStats getStats() {
		return stats;
	}

	public int[] windowSizes() {
		return windowSizes;
	}

	public int threads() {
		return threadCount;
	}
	
	public double getScale()
	{
		return scaleBy;
	}

	public VertexFitter getFitter() {
		return fitter;
	}

	public void print() {
		System.out.println("Formatter Info:");
		fitter.print();
	}

	// /////////////////////////////////////////////////////////////////////
	// PRIVATE METHOD CALLS
	// /////////////////////////////////////////////////////////////////////

	private void translationIssues() {
		double[] offsets = new double[DIMENSION];
		if (aligncenter)
		{
			for (int d = 0; d < DIMENSION; ++d) {
				offsets[d]  =windowSizes[d]/2/scaleBy- stats.avg(d) ; 
			}

		}
		else

			for (int d = 0; d < DIMENSION; ++d) {
				if (d==0 && minX!=0 && maxX!=0)
					offsets[d] -= minX;
				else if (d==1 && minY!=0 && maxY!=0)
					offsets[d] -= minY;
				else
					offsets[d] -= stats.min(d);
			}
		Transformer transformer = new Transformer();
		transformer.move(offsets);
		// Add this job to the list
		fitter.addManipulation(transformer);
	}

	private void scalingIssues() {
		double scale;
		if (minX!=0 && maxX!=0)
			scale = windowSizes[0] / ( maxX-minX);
		else
			scale = windowSizes[0] / stats.span(0);


		for (int d = 1; d < DIMENSION; ++d) {
			double newScale = windowSizes[d] / stats.span(d);
			if (newScale < scale) {
				scale = newScale;
			}
		}
		Transformer transformer = new Transformer();
		transformer.scale(.99 * scale);
	//	scaleBy = .99 * scale;
		fitter.addManipulation(transformer);
	}

	private double calculateScale()
	{
		Matrix m = fitter.getManipulationMatrix();
		double sum = 0;
		for (int i = 0; i < Vertex.DIMENSION;i++)
			sum += m.get(0, i)*m.get(0, i); 

		double scale = Math.sqrt(sum);

		return scale;

	}

	private void scaleLabels(double currentScale)
	{
		labels.forEach((k,v) -> {
			Label l = (Label)v;
			Vertex vertex = (Vertex) k;
			l.linelength =(l.linelength * currentScale * scaleCorrectionLabels);
			//l.linesize =(int) (l.linesize *  scaleBy * scaleCorrectionLabels);
			l.bottomtextsize =  (l.bottomtextsize *  currentScale * scaleCorrectionLabels);
			l.toptextsize =  (l.toptextsize *  currentScale * scaleCorrectionLabels);
			l.shapesize =  (l.shapesize *  currentScale * scaleCorrectionLabels);

			});
			if (!scaleLabelUsed)
			{
				scaleCorrectionLabelsOriginal = scaleCorrectionLabels;

			}
			scaleLabelUsed = true;
			scaleCorrectionLabels = 1;


	}

	public void applyTransformation() {
		double currentScale = calculateScale();
		scaleBy = scaleBy * currentScale;
		// Now to do the work to each vertex
		long start = System.nanoTime();
		ManipVertexArray manipa[] = new ManipVertexArray[threadCount];
	//	VertexStats statsa[] = new VertexStats[threadCount];

		for (int t = 0; t < threadCount; ++t) {
			ManipVertexArray manip = new ManipVertexArray(vertices,"vertexrecalc"+t);
			manipa[t] = manip;
		//	statsa[t] = new VertexStats();
			manip.setFitter(fitter);
			manip.setStride(threadCount);
			manip.setOffset(t);
			manip.setVertexStats(stats); // Fix this if threading (race cond.)
			manip.start();
		}
		for (int t = 0; t < threadCount;t++)
		{
			try {
				manipa[t].getThread().join();
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}

		}
		long end = System.nanoTime();
		System.out.println("Time to recalculate vertices "+(end-start)/1000000000.0+"s");
		scaleLabels(currentScale);
		
	}

}
