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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Vector;

import Jama.Matrix;

/**
 * <p>
 * This handles all the IO for the viewer. That just basically means reading in
 * the file, checking for the format, and returning an array of edge objects.
 * The dimension is set by the DIMENSION variable in Edge.java
 * </p>
 * <b>SESS - 2014.05.11:</b>
 * <ul>
 * <li>Cleanup code a little bit.</li>
 * <li>{@link #loadVertexCoords(File)} reimplemented. Now validates the
 * structure of each line.</li>
 * </ul>
 */
public class ViewerIO {

	private File file;
	private Edge[] edges;
	private Vertex[] vertices;
	// private final String DELIMETER = " \t";
	private FileInputHandler fileio;
	private VertexStats stats;
	private static final int DIMENSION = Vertex.DIMENSION;
	// private int index, lineNumber;
	private int tokenCounter;
	private HashMap vertexIdMap;
	private HashMap edgeIdMap;
	private HashMap edgeColorMap;
	private HashMap vertexColorMap;
	private Vector edgesV;
	private Vector verticesV;
	private HashMap labelMap;
	private double scalingLabel;
	private double customMinX;
	private double customMaxX;
	private double customMinY;
	private double customMaxY;

	public ViewerIO(File filename) throws FileNotFoundException, IOException {
		file = filename;
		edgeColorMap = new HashMap();
		vertexColorMap = new HashMap();
		labelMap = new HashMap();
		scalingLabel = 1;
		customMinX = 0;
		customMaxX = 0;
		customMinY = 0;
		customMaxY = 0;

	}

	public void setLabelScale(double scale)
	{
		this.scalingLabel = scale;
	}

	public void setMinMaxXY(double minx, double miny,double maxx, double maxy)
	{
		customMinX = minx;
		customMaxX = maxx;
		customMinY = miny;
		customMaxY = maxy;

	}

	public double getMinX() {
		return customMinX;
	}

	public double getMaxX() {
		return customMaxX;
	}

	public double getMinY() {
		return customMinY;
	}

	public double getMaxY() {
		return customMaxY;
	}


	public double getLabelScale() {
		return this.scalingLabel;
	}

	// TODO: SESS - should move to "utils" class?
	static boolean isDouble(String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	// TODO: SESS - should move to "utils" class?
	static double[] toDouble(String[] values, int startIndex, int endIndex) {
		double[] result = new double[endIndex - startIndex + 1];
		for (int i = startIndex; i <= endIndex; i++) {
			result[i - startIndex] = Double.parseDouble(values[i]);
		}
		return result;
	}

	public void loadVertexCoords(File f) throws IOException {
		InputStream in = new FileInputStream(f);
		if (stats == null) {
			stats = new VertexStats();
		} else {
			stats.clear();
		}
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			try {
				String line;
				int currLine = 0;
				while ((line = reader.readLine()) != null) {
					currLine++;
					line = line.trim();
					if (line.isEmpty() || line.startsWith("#")) {
						continue;
					}
					String[] parts = line.split("\\s+");
					int expected = DIMENSION + 1;
					if (parts.length != expected) {
						throw new IOException(
								MessageFormat
										.format("Error at line {0}: Found {1} values while {2} are expected.",
												currLine, parts.length,
												expected));
					}
					for (int i = 1; i <= DIMENSION; i++) {
						if (!isDouble(parts[i])) {
							throw new IOException(
									MessageFormat
											.format("Error at line {0}: Column {1} must be a double value.",
													currLine, i));
						}
					}
					String id = parts[0];
					Vertex vertex = (Vertex) vertexIdMap.get(id);
					if (vertex == null) {
						// TODO: We should at least log that no vertex found
						continue;
					}
					double[] values = toDouble(parts, 1, DIMENSION);
					Matrix location = vertex.location();
					for (int i = 0; i < values.length; i++) {
						location.set(i, 0, values[i]);
					}
					// TODO: SESS - I don't know why so lets keep this code...
					if (location.get(0, 0) < 10000) {
						stats.addStatsOfVertex(vertex);
					}
				}
			} finally {
				reader.close();
			}
		} finally {
			in.close();
		}
	}

	public void loadSHORTFile() throws IOException, FileNotFoundException {
		this.loadSHORTFile(file);
	}

	public void loadSHORTFile(File f) throws IOException, FileNotFoundException {
		edgeIdMap = new HashMap();
		vertexIdMap = new HashMap();
		edgesV = new Vector();
		verticesV = new Vector();
		fileio = new FileInputHandler(file.getAbsolutePath());
		String v1 = null;
		while (fileio.readNextLine()) {
			if (fileio.getTokenCount() == 0) {
				break;
			}
			String id1 = fileio.getToken(0);
			if (id1.equals("#")) {
				v1 = fileio.getToken(1);
				continue;
			}
			loadEdge(v1, fileio.getToken(0));
		}
		tidyNewVerticesAndEdges();
	}

	// This just generates the edge relationships and not any
	// coordinates.
	public void loadLSFile() throws IOException, FileNotFoundException {
		this.loadLSFile(file);
	}

	public void loadLSFile(File f) throws IOException, FileNotFoundException {
		edgeIdMap = new HashMap();
		vertexIdMap = new HashMap();
		fileio = new FileInputHandler(file.getAbsolutePath());
		fileio.readNextLine();
		int vertexCount = fileio.getTokenAsInt(0);
		while (true) {
			fileio.readNextLine();
			if (fileio.getTokenAsInt(0) == -1 || fileio.getTokenCount() == 0) {
				break;
			}
			Vertex v = loadVertex(fileio.getToken(1));
			int size = fileio.getTokenAsInt(0);
			loadGroup(fileio, v, size);
		}
		tidyNewVerticesAndEdges();
	}

	public void loadEdgeColorFile(File f) throws IOException,
			FileNotFoundException {
		if (edges == null) {
			return;
		}
		fileio = new FileInputHandler(f.getAbsolutePath());
		while (fileio.readNextLine()) {
			if (fileio.getTokenCount() != 5) {
				throw new IOException();
			}
			String id1 = fileio.getToken(0);
			String id2 = fileio.getToken(1);
			String edgeName = Edge.idEdge(id1, id2);
			Object o = edgeIdMap.get(edgeName);
			if (o == null) {
				System.out.println("Undefined Edge: " + id1 + " " + id2);
				continue;
			}
			edgeColorMap.put((Edge) o, readColorRGB(2));
		}
	}

	public void loadVertexColorFile(File f) throws IOException,
			FileNotFoundException {
		if (edges == null || vertices == null) {
			return;
		}
		fileio = new FileInputHandler(f.getAbsolutePath());
		while (fileio.readNextLine()) {
			if (fileio.getTokenCount() != 4) {
				throw new IOException();
			}
			String id = fileio.getToken(0);
			Object o = vertexIdMap.get(id);
			if (o == null) {
				System.out.println("Undefined Vertex: " + id);
				continue;
			}
			vertexColorMap.put((Vertex) o, readColorRGB(1));
		}
	}

	public void loadLabelFile(File f) throws IOException,FileNotFoundException {
		if (edges == null || vertices == null) {
			return;
		}
		labelMap.clear();
		fileio = new FileInputHandler(f.getAbsolutePath());
		fileio.setDelimeters(",");
		while (fileio.readNextLine()) {
			int s = fileio.getTokenCount();
			if (s != 21) {
				throw new IOException("number of fields are not correct in the file");
			}
			
			int index = 0;
			String id = fileio.getToken(index++);
			String shape = fileio.getToken(index++);
			Integer shapesize = fileio.getTokenAsInt(index++);
			Integer shapeboarderwidth = fileio.getTokenAsInt(index++);

			
			  
			Color shapebordercolor = readColorRGBHex(index++);
		
			Color shapefillcolor = readColorRGBHexAlpha(index++);
			index++;
			double shapefillopacity = 0;//fileio.getTokenAsDouble(index++);
			if (shapesize == null || shapeboarderwidth == null || shapebordercolor == null || shapefillcolor == null)
			{
				shapesize = 0;
				shapeboarderwidth = 0;
				shape = "";
			}


			Integer linesize = fileio.getTokenAsInt(index++);
			Integer linelength = fileio.getTokenAsInt(index++);
			Double lineangle =  fileio.getTokenAsDouble(index++);
			Color linecolor = readColorRGBHex(index++);

			

			
			String toptextttf = fileio.getToken(index++);
			Integer toptextsize = fileio.getTokenAsInt(index++);
			Color toptextcolor = readColorRGBHex(index++);
			Color topbgfillcolor =readColorRGBHex(index++);

			String bottomtextttf = fileio.getToken(index++);
			Integer bottomtextsize = fileio.getTokenAsInt(index++);
			Color bottomtextcolor = readColorRGBHex(index++);
			Color bottombgfillcolor =readColorRGBHex(index++);

			String toptext = fileio.getToken(index++);
			String bottomtext = fileio.getToken(index++);

			if (toptextttf.isEmpty() || toptextsize== null || toptextcolor == null || topbgfillcolor==null)
			{
				toptext = "";
				toptextsize = 0;
			}	

			if (bottomtextttf.isEmpty()  || bottomtextsize== null || bottomtextcolor == null || bottombgfillcolor==null)
			{
				bottomtext = "";
				bottomtextsize = 0;
			}	

			

			if (linesize == null)
			{
				linecolor = null;
				linesize = 0;
			}	
			if (lineangle ==null || linelength== null )
			{
			  //toptext = "";
			  //bottomtext = "";
			  lineangle = 0.0;
			  linelength = 0;	
			}

			

			Object o = vertexIdMap.get(id);
			if (o == null) {
				System.out.println("Undefined Vertex: " + id);
				continue;
			}
			Label l = new Label(shape,
								shapesize ,
								shapeboarderwidth ,
								shapebordercolor,
								shapefillcolor ,
								shapefillopacity ,
								linesize ,
								linelength ,
								lineangle ,
								linecolor,
								toptextttf,
								toptextsize ,
								toptextcolor ,
								topbgfillcolor ,
								bottomtextttf ,
								bottomtextsize ,
								bottomtextcolor ,
								bottombgfillcolor ,
								toptext,
								bottomtext);
			labelMap.put((Vertex) o,l);

		}

	}
	  

	// //////////////////////////////////////////////////////////
	// ACCESSORS
	// //////////////////////////////////////////////////////////

	public Edge[] getEdges() {
		return edges;
	}

	public VertexStats getStats() {
		return stats;
	}

	public Vertex[] getVertices() {
		return vertices;
	}

	public HashMap getLabels() {
		return labelMap;
	}


	public HashMap getEdgeColorMap() {
		return edgeColorMap;
	}

	public HashMap getVertexColorMap() {
		return vertexColorMap;
	}

	public HashMap getVertexIdMap() {
		return vertexIdMap;
	}

	public HashMap getEdgeIdMap() {
		return edgeIdMap;
	}

	// //////////////////////////////////////////////////////////
	// MUTATORS
	// //////////////////////////////////////////////////////////

	public void clearAllEdgeColors() {
		edgeColorMap.clear();
	}

	public void clearAllVertexColors() {
		vertexColorMap.clear();
	}

	// //////////////////////////////////////////////////////////
	// PRIVATE METHOD CALLS
	// //////////////////////////////////////////////////////////

	private Vertex loadVertex(String id) {
		Object o = vertexIdMap.get(id);
		if (o != null) {
			return (Vertex) o;
		}
		Vertex v = new Vertex();
		v.id(id);
		vertexIdMap.put(id, v);
		verticesV.add(v);
		return v;
	}

	private Edge loadEdge(Vertex v1, Vertex v2) {
		String edgeName = Edge.idEdge(v1, v2);
		Object o = edgeIdMap.get(edgeName);
		if (o != null) {
			return (Edge) o;
		}
		Edge e = new Edge(v1, v2);
		edgeIdMap.put(edgeName, e);
		edgesV.add(e);
		return e;
	}

	private Edge loadEdge(String id1, String id2) {
		return loadEdge(loadVertex(id1), loadVertex(id2));
	}

	private void loadGroup(FileInputHandler f, Vertex v1, int size)
			throws IOException {
		for (int ii = 0; ii < size; ++ii) {
			f.readNextLine();
			Vertex v2 = loadVertex(fileio.getToken(0));
			loadEdge(v1, v2);
		}
	}

	private void readLocationIntoVertex(Vertex v) {
		Matrix m = new Matrix(DIMENSION + 1, 1);
		for (int ii = 0; ii < DIMENSION; ++ii) {
			m.set(ii, 0, fileio.getTokenAsDouble(ii + tokenCounter));
		}
		tokenCounter += DIMENSION;
		v.location(m);
	}

	private void readColor(int index) {
		readColorRGB(index);
	}

	private Color readColorRGB(int index) {
		double r = fileio.getTokenAsDouble(index++);
		double g = fileio.getTokenAsDouble(index++);
		double b = fileio.getTokenAsDouble(index++);
		return new Color((float) r, (float) g, (float) b);
	}

	private Color readColorRGBHex(int index) {
		String hex = fileio.getToken(index);
		try
		{
			int code = Integer.parseInt(hex,16);  
			
			return new Color((float) (code >> 16)/255, (float) ((code >> 8) & 255)/255, (float) (code & 255)/255);
		}
		catch (NumberFormatException nfe) {
			return null;
		}
	}

	private Color readColorRGBHexAlpha(int index) {
		String hex = fileio.getToken(index++);
		try
		{
			int code = Integer.parseInt(hex,16);  
			Double d = fileio.getTokenAsDouble(index);
			if (d==null)
				return null;
			float alpha = (float) d.floatValue();
			return new Color((float) (code >> 16)/255, (float) ((code >> 8) & 255)/255, (float) (code & 255)/255,alpha/100);
		}
		catch (NumberFormatException nfe) {
			return null;
		}

	}


	private void tidyNewVerticesAndEdges() {
		// This just loads the edges into an array and out of the
		// vector
		edges = new Edge[edgesV.size()];
		for (int ii = 0; ii < edgesV.size(); ++ii) {
			edges[ii] = (Edge) edgesV.elementAt(ii);
			// edges[ii].print();
		}
		edgesV.clear();

		vertices = new Vertex[verticesV.size()];
		for (int ii = 0; ii < verticesV.size(); ++ii) {
			vertices[ii] = (Vertex) verticesV.elementAt(ii);
			vertices[ii].index(ii);
			// vertices[ii].print();
		}
		verticesV.clear();
	}

	
}
