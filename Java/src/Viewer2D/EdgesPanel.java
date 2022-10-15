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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import java.awt.event.MouseWheelEvent;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import Jama.Matrix;
import java.awt.BasicStroke;
import Viewer2D.Label;
import java.awt.AlphaComposite;
import java.awt.FontFormatException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Image;

/**
 * <b>SESS - 2014.05.13:</b>
 * <ul>
 * <li>Cleanup code a little bit.</li>
 * <li>Reimpleented "Save edge color" because was badly implemented (took
 * forever).</li>
 * </ul>
 */
public class EdgesPanel extends JPanel implements MouseListener,
		MouseMotionListener, MouseWheelListener {
	private static final long serialVersionUID = -765273216020721560L;

	private Edge[] edges;
	private Vertex[] vertices;

	private HashMap<Object,Object> labels;

	private int xWindowSize, yWindowSize;

	private VertexFitter fitter;

	private boolean doVisibilityTest;

	private String statusMessage;
	private JTextField statusBar;

	private Font font;

	private HashMap<Fontid,Font> fontMap;

	// These are hashes relating edge/vertex id
	// to specific info
	private HashMap edgeColorMap;
	private HashMap vertexIdMap;
	private HashMap edgeIdMap;
	private HashMap vertexColorMap;

	private int vertexRadius;

	// Default edge color
	private Color edgeColor;
	private Color vertexColor;
	private Color backgroundColor;

	private boolean zoomRegion;
	private boolean zoomPoint;
	private boolean idRegion;
	private boolean drawVertices;

	private double zoomStepSize;

	private FormatVertex formatter;
	private double moveStepSize;

	private boolean idsIncluded;
	// Default font color
	private Color fontColor;

	enum modes
	{
		nomode,
		handmode,
		magnifiermode
	

	}

	private modes mode;
	private boolean handtouch = false;

	private Cursor cursorMagnifier;

	private double x1, y1, x2, y2;
	private Matrix mins, maxs;
	private Matrix inverted;

	private BufferedImage bufferedImage;
	private boolean paintImage;

	private double defaultScale;

	ModeChanged modeChange;

	private JPopupMenu menu;

	protected BufferedImage getBufferedImage() {
		if (bufferedImage == null) {
			bufferedImage = new BufferedImage(xWindowSize, yWindowSize,
					BufferedImage.TYPE_INT_RGB);
		}
		return bufferedImage;
	}

	// TODO: SESS - shall we expose this?
	// public void setBufferedImage(BufferedImage bufferedImage) {
	// this.bufferedImage = bufferedImage;
	// }

	public interface ModeChanged {  
		//declare abstract method  
		void onChange(modes m);  
	}  
	 

	public boolean isPaintImage() {
		return paintImage;
	}

	public void setPaintImage(boolean paintImage) {
		this.paintImage = paintImage;
	}

	public void setPaintImage() {
		paintImage = true;
	}

	public EdgesPanel(Edge[] edges, Vertex[] vertices, HashMap labels, int xWindowSize,
			int yWindowSize) {
		super();
		
		System.out.println("EdgesPanel()"); // TODO: deleteme
		this.xWindowSize = xWindowSize;
		this.yWindowSize = yWindowSize;
		this.setPreferredSize(new Dimension(xWindowSize, yWindowSize));
		this.edges = edges;
		this.vertices = vertices;
		this.labels = labels;

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	

		// By default don't draw ids
		idsIncluded = false;
		doVisibilityTest = false;
		zoomRegion = false;
		idRegion = false;
		zoomPoint = false;
		drawVertices = false;

		statusMessage = "Waiting for HightLight Event";

		// These are the default colors
		edgeColorMap = new HashMap();
		vertexColorMap = new HashMap();
		fontMap = new HashMap();
		edgeColor = Color.black;
		fontColor = Color.blue;
		// Lets start with transparent
		//backgroundColor = Color.white;
		// transparent white
		backgroundColor = new Color(0f,0f,0f,0f);
		vertexColor = Color.red;

		// Matrices need for zooming moving etc
		mins = new Matrix(2, 1);
		maxs = new Matrix(2, 1);
		mode = modes.nomode;
		Image image = null;
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		String sep = System.getProperty("file.separator");
		String workingdir = System.getProperty("user.dir");
		//System.out.println("Working Directory = " + System.getProperty("user.dir"));
		
		java.net.URL imgURL = getClass().getResource("icons8-magnifier-67_32x32.png");
		if(null != imgURL) {
			System.out.println("Icon URL = " + imgURL.getFile());
			//Image image = toolkit.getImage(workingdir+sep+"Java/src/Viewer2D"+sep+"icons8-magnifier-67_32x32.png");
			image = toolkit.getImage(imgURL);
		}
		cursorMagnifier = toolkit.createCustomCursor(image , new Point(0,0), "magnifier");
		setPopup() ;
		defaultScale = 1;
	}

	

	private void setPopup() 
	{

		menu = new JPopupMenu();

		

		JMenuItem fos = new JMenuItem("Fit on Screen");  
		JMenuItem n100 = new JMenuItem("100%");  
		JMenuItem n200 = new JMenuItem("200%");  
		JMenuItem zoomIn = new JMenuItem("Zoom In"); 
		JMenuItem zoomOut = new JMenuItem("Zoom Out");  
		menu.add(fos);
		menu.add(n100);
		menu.add(n200);
		menu.addSeparator();
		menu.add(zoomIn);
		menu.add(zoomOut);

		fos.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e) {              
				Dimension r = getBounds().getSize();
				xWindowSize = r.width;
				yWindowSize  = r.height;
				//bufferedImage = null;
				//getBufferedImage();
				formatter.setWindowSizeX(xWindowSize);
				formatter.setWindowSizeY(yWindowSize);
				fitData();
			
				setPaintImage(); // TODO: move inside panel.xxxx()?
				repaint();
			}  
		   });  

	    n100.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e) {    
				double scale = formatter.getScale();       
				if (scale!=defaultScale)   
				{
					Point p = getMousePosition();
					int	x1 = (int)p.getX();
					int y1 = (int)p.getY();
					double targetScale;
				
					targetScale = defaultScale/scale;
					
					zoom2PointBy(x1, y1,targetScale);
					setPaintImage();
					repaint();

				}
				
			}  
		   });  

	    n200.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e) {     
				double scale = formatter.getScale();       
				if (scale!=defaultScale*2)   
				{
					Point p = getMousePosition();
					int	x1 = (int)p.getX();
					int y1 = (int)p.getY();
					double targetScale;
				
					targetScale = defaultScale*2/scale;
					
					zoom2PointBy(x1, y1,targetScale);
					setPaintImage();
					repaint();

				}         
				
			}  
		   });  

		zoomIn.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e) {   
				
				Point p = getMousePosition();
				int	x1 = (int)p.getX();
				int y1 = (int)p.getY();
				
		
				zoom2Point(x1, y1);
				setPaintImage();
				repaint();           
				
			}  
		   });  

		zoomOut.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e) {       
				Point p = getMousePosition();
				int	x1 = (int)p.getX();
				int y1 = (int)p.getY();
				
		
				zoomOutFromPoint(x1, y1);
				setPaintImage();
				repaint();         
				
			}  
		   });  


        //setComponentPopupMenu(menu);
		
		
		//panel.re
		
	}

	// -------------------------------------------------------
	// PAINTING ISSUES
	// -------------------------------------------------------

	public void paint(Graphics g) {
		System.out.println("paint()"); // TODO: deleteme
		super.paint(g);
		// TODO: deleteme! - super.paint() already call paintComponent()
		// paintComponent(g);
	}

	public void update(Graphics g) {
		System.out.println("update()"); // TODO: deleteme
		paintImage();
		paint(g);
	}

	@Override
	public void repaint() {
		//System.out.println("repaint()"); // TODO: deleteme
		paintImage();
		super.repaint();
	}
	
	// Paint a bitmap image
	public void paintImage(BufferedImage bufferedImage) {
		System.out.println("paintImage(" + bufferedImage + ")");
		Graphics2D g2 = (Graphics2D) bufferedImage.getGraphics();
		
		Rectangle2D.Double rect = new Rectangle2D.Double(0, 0,
				bufferedImage.getWidth(), bufferedImage.getHeight());
		// g2.setPaint(backgroundColor);
		System.out.println(" Getting background color '" + backgroundColor + "'");
		System.out.println(" Getting background color alpha '" + backgroundColor.getTransparency() + "'");

		g2.setColor(backgroundColor);
		g2.setPaint(backgroundColor);
		g2.fill(rect);
	
		paintImage(g2);
	}

	/** Used by vector path */
	private void paintImage(Graphics2D g2){
		setRenderingHints(g2);

		if (edges != null) {
			paintNonColoredEdges(g2);
			// Now to draw the colored edges. They must be drawn last
			// (so they are drawn on top);
			paintColoredEdges(g2);
			paintVertices(g2);
			paintIds(g2);
			paintLabels(g2);
		}
	}

	public void paintImage() {
		//System.out.println("generateImage() " + paintImage + " "
		//		+ bufferedImage);
		// SESS: Sanity check
		// Seems that inherited constructor calls repaint().
		// Until I understand why lets keep this check.
		if (bufferedImage == null) {
			return;
		}
		if (!paintImage) {
			return;
		}
		paintImage(getBufferedImage());
		paintImage = false;
	}

	public void paintNonColoredEdges(Graphics g) {
		//System.out.println("paintNonColoredEdges() " + g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(edgeColor);
		for (Edge edge : edges) {
			Matrix v1 = edge.vertex1().location();
			Matrix v2 = edge.vertex2().location();
			if (doVisibilityTest && !visible(v1, v2)) {
				continue;
			}
			if (edgeColorMap.get(edge) == null) {
				g2.draw(new Line2D.Double(v1.get(0, 0), v1.get(1, 0), v2.get(0,
						0), v2.get(1, 0)));
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void paintColoredEdges(Graphics g) {
		//System.out.println("paintColoredEdges() " + g);
		Graphics2D g2 = (Graphics2D) g;
		for (Map.Entry<Edge, Color> e : ((Map<Edge, Color>) edgeColorMap)
				.entrySet()) {
			Edge edge = e.getKey();
			Matrix v1 = edge.vertex1().location();
			Matrix v2 = edge.vertex2().location();
			if (doVisibilityTest && !visible(v1, v2)) {
				continue;
			}
			g2.setColor(e.getValue());
			g2.draw(new Line2D.Double(v1.get(0, 0), v1.get(1, 0), v2.get(0, 0),
					v2.get(1, 0)));
		}
	}

	public void paintNonColoredVertices(Graphics g) {
		//System.out.println("paintNonColoredVertices() " + g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(vertexColor);

		for (Vertex vertex : vertices) {
			if (!visible(vertex.location())) {
				continue;
			}
			if (vertexColorMap.get(vertex) == null) {
				continue;
			}
			Matrix m = vertex.location();
			g2.fill(new Rectangle((int) m.get(0, 0) - vertexRadius, (int) m
					.get(1, 0) - vertexRadius, 2 * vertexRadius,
					2 * vertexRadius));
		}
	}

	@SuppressWarnings("unchecked")
	public void paintColoredVertices(Graphics g) {
		//System.out.println("paintColoredVertices() " + g);
		Graphics2D g2 = (Graphics2D) g;
		for (Map.Entry<Vertex, Color> e : ((Map<Vertex, Color>) vertexColorMap)
				.entrySet()) {
			Vertex vertex = e.getKey();
			if (!visible(vertex.location())) {
				continue;
			}
			if (vertexColorMap.get(vertex) == null) {
				continue;
			}
			Matrix m = vertex.location();
			g2.setColor(e.getValue());
			g2.fill(new Rectangle((int) m.get(0, 0) - vertexRadius, (int) m
					.get(1, 0) - vertexRadius, 2 * vertexRadius,
					2 * vertexRadius));
		}
	}


	public void paintComponent(Graphics g) {
		System.out.println("paintComponent() " + g);
		super.paintComponent(g);
		g.drawImage(getBufferedImage(), 0, 0, null);

		// TODO: Work in progress... DELETEME AFTER
		// Graphics2D g2 = (Graphics2D) g;
		// setRenderingHints(g2);
		// setBackground(backgroundColor);
		// if (edges != null) {
		// paintNonColoredEdges(g);
		// // Now to draw the colored edges. They must be drawn last
		// // (so they are drawn on top);
		// paintColoredEdges(g);
		// paintVertices(g);
		// paintIds(g2);
		// }
	}

	public void applyFit(VertexFitter f) {
		/*for (int ii = 0; ii < vertices.length; ++ii) {
			f.fitVertex(vertices[ii]);
		
		}*/
		formatter.getFitter().setManipulationMatrix(f.getManipulationMatrix());
		formatter.applyTransformation();
		fitter = f;
		inverted = fitter.getManipulationMatrix().inverse();
		fitter.setManipulationMatrix(inverted);
	}

	public void fitData() {
		formatter.fitDataToWindow();
		defaultScale = formatter.getScale();
	}

	public void setEdges(Edge[] e) {
		edges = e;
	}

	// ACCESSING AND REMOVING EDGE COLORS
	public void removeEdgeColors(HashMap h) {
		Iterator i = h.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry entry = (Map.Entry) i.next();
			Edge e = (Edge) entry.getKey();
			edgeColorMap.remove(e);
		}
	}

	public void addEdgeColors(HashMap h) {
		edgeColorMap.putAll(h);
	}

	public void addVertexColors(HashMap h) {
		vertexColorMap.putAll(h);
	}

	public void addLabels(HashMap h) {
        labels.putAll(h);
	}

	public void clearAllEdgeColors() {
		edgeColorMap.clear();
	}

	public void clearAllVertexColors() {
		vertexColorMap.clear();
	}

	public void setEdgeIdMap(HashMap h) {
		edgeIdMap = h;
	}

	public void setVertexIdMap(HashMap h) {
		vertexIdMap = h;
	}

	public void setVertexRadius(int r) {
		vertexRadius = r;
	}

	public void setVertices(Vertex[] v) {
		vertices = v;
	}

	public void setFormatter(FormatVertex v) {
		formatter = v;
	}

	public void setFontColor(Color c) {
		fontColor = c;
	}

	public void setEdgeColor(Color c) {
		edgeColor = c;
	}

	public void setVertexColor(Color c) {
		vertexColor = c;
	}

	public void setFont(Font s) {
		font = s;
	}

	public void setMoveStepSize(double s) {
		moveStepSize = s;
	}

	public void setBackgroundColor(Color c) {
		backgroundColor = c;
	}

	public void setMins(Matrix m) {
		mins = m;
	}

	public void setMaxs(Matrix m) {
		maxs = m;
	}

	public void prepZoomRegion(boolean s) {
		zoomRegion = s;
	}

	public void prepZoomPoint(boolean s) {
		zoomPoint = s;
	}

	public void prepIdRegion(boolean s) {
		idRegion = s;
	}

	public void setZoomStepSize(double s) {
		zoomStepSize = s;
	}

	public void setVisibilityTest(boolean b) {
		doVisibilityTest = b;
	}

	public void setStatusBar(JTextField f) {
		statusBar = f;
		statusBar.setText(statusMessage);
	}

	// ACCESSORS
	public int windowSizeX() {
		return xWindowSize;
	}

	public int windowSizeY() {
		return yWindowSize;
	}

	public Edge[] getEdges() {
		return edges;
	}

	public Vertex[] getVertices() {
		return vertices;
	}

	public HashMap getEdgeColors() {
		return edgeColorMap;
	}

	public HashMap getVertexColors() {
		return vertexColorMap;
	}

	public double getZoomStepSize() {
		return zoomStepSize;
	}

	public void writeImage(String imageName, BufferedImage i) {
		File f = new File(imageName);
		if (statusBar != null) {
			statusBar.setText("Writing " + f.getAbsolutePath() + "...");
		}

		Graphics2D g = (Graphics2D) getGraphics();
		if (i == null) {
			i = g.getDeviceConfiguration().createCompatibleImage(xWindowSize,
					yWindowSize, BufferedImage.TYPE_INT_ARGB);
		}
		// TODO: SESS - check is OK
		paintImage(i);
		// paint(i.getGraphics());
		try {
			ImageIO.write(i, "png", f);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Image Write Failed", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		if (statusBar != null) {
			statusBar.setText("Done.");
		}
	}


	public void writeVectorImage(Graphics2D i)
	{
		// Just try paint it		
		paintImage(i);
	}

	// Produce an image of a given region
	public void imageRegion(Matrix mins, Matrix maxs, String imageName,
			BufferedImage i) {
		VertexFitter f = new VertexFitter();
		// Move to the average of the given positions
		move2Point((maxs.get(0, 0) + mins.get(0, 0)) * .5,
				(maxs.get(1, 0) + mins.get(1, 0)) * .5, f);
		writeImage(imageName, i);
	}

	public void saveVertexColorMap(File f) {
		if (f == null) {
			return;
		}
		String buffer = new String();
		Iterator i = vertexColorMap.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry entry = (Map.Entry) i.next();
			Vertex v = (Vertex) entry.getKey();
			Color c = (Color) entry.getValue();
			float[] rgb = c.getColorComponents(null);
			buffer += v.id() + " " + rgb[0] + " " + rgb[1] + " " + rgb[2]
					+ "\n";
		}
		try {
			FileWriter out = new FileWriter(f);
			out.write(buffer);
			out.flush();
			out.close();
		} catch (IOException ee) {
			JOptionPane.showMessageDialog(null, "Error On Write", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	@SuppressWarnings("unchecked")
	public void saveEdgeColorMap(File f) {
		if (f == null) {
			return;
		}
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			try {
				for (Map.Entry<Edge, Color> e : ((Map<Edge, Color>) edgeColorMap)
						.entrySet()) {
					Edge edge = e.getKey();
					float[] colors = e.getValue().getColorComponents(null);
					String line = MessageFormat
							.format("{0} {1} {2,number,0.0} {3,number,0.0} {4,number,0.0}",
									edge.vertex1().id(), edge.vertex2().id(),
									colors[0], colors[1], colors[2]);
					out.write(line);
					out.newLine();
				}
			} finally {
				out.close();
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null,
					"Error saving edge colors file:\n" + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		// TODO: SESS - DELETEME! - old code took for ever!
		// String buffer = new String();
		// Iterator i = edgeColorMap.entrySet().iterator();
		// while (i.hasNext()) {
		// Map.Entry entry = (Map.Entry) i.next();
		// Edge e = (Edge) entry.getKey();
		// String id1 = e.vertex1().id();
		// String id2 = e.vertex2().id();
		// Color c = (Color) entry.getValue();
		// float[] rgb = c.getColorComponents(null);
		// buffer += id1 + " " + id2 + " " + rgb[0] + " " + rgb[1] + " "
		// + rgb[2] + "\n";
		// }
		// try {
		// FileWriter out = new FileWriter(f);
		// out.write(buffer);
		// out.flush();
		// out.close();
		// } catch (IOException ee) {
		// JOptionPane.showMessageDialog(null, "Error On Write", "Error",
		// JOptionPane.ERROR_MESSAGE);
		// }
	}

	// PRIVATE METHOD CALLS

	private boolean highlightSanityCheck() {
		if (edges == null || vertices == null) {
			return false;
		}
		if ((maxs.get(0, 0) - mins.get(0, 0) < 5)
				|| (maxs.get(1, 0) - mins.get(1, 0) < 5)) {
			// Probably hit the buttons by accident
			return false;
		} else {
			return true;
		}
	}

	// -----------------------------------------------------

	// TODO: SESS - Mmmm... hard codded 10000...
	public boolean visible(Matrix m1, Matrix m2) {
		return (visible(m1) || visible(m2))
				&& ((m1.get(0, 0) < 10000) && (m1.get(1, 0) < 10000)
						&& (m2.get(0, 0) < 10000) && (m2.get(1, 0) < 10000));
	}

	// TODO: SESS - Mmmm... I added a scroller to the UI...
	public boolean visible(Matrix m) {
		return (0 <= m.get(0, 0) && m.get(0, 0) <= xWindowSize)
				&& (0 <= m.get(1, 0) && m.get(1, 0) <= yWindowSize);
	}

	// -----------------------------------------------------
	// DRAWING ID ISSUES
	// -----------------------------------------------------

	public void setVertexColorsFromFindVertexFrame(String[] id, Color c) {
		for (int ii = 0; ii < id.length; ++ii) {
			Vertex v = (Vertex) vertexIdMap.get(id[ii]);
			if (v == null) {
				continue;
			}
			vertexColorMap.put(v, c);
		}
	}

	private void paintIds(Graphics2D g) {
		if (vertices == null) {
			return;
		}
		g.setColor(fontColor);
		g.setFont(font);
		for (int ii = 0; ii < vertices.length; ++ii) {
			if (doVisibilityTest)
				if (!visible(vertices[ii].location()))
					continue;

			if (vertices[ii].doesShowID() || idsIncluded) {
				// System.out.println("Showing id of " + vertices[ii].id() );
				g.drawString(vertices[ii].id(), (int) vertices[ii].location()
						.get(0, 0), (int) vertices[ii].location().get(1, 0));
			}
		}
	}

	private class Fontid
	{
		String filename;
		float size;

		Fontid(String f, float s)
		{
			filename = f;
			size = s;

		}
        public boolean equals(Object o) {
			return ((Fontid)o).filename == this.filename &&  ((Fontid)o).size == this.size ;
	   }
	   public int hashCode() {
		return  filename.hashCode()+(int)(size*10);
	  }

	}
 
	private  Font getFont(String ttffile, float size) 
	{
		Font f = font;
		Fontid id = new Fontid(ttffile,size);
		if (fontMap.containsKey(id))
		{
			return fontMap.get(id);
		}

		try {
		  f = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(ttffile)).deriveFont(size);
		  fontMap.put(id,f);
		} catch (FontFormatException ffe) {
		System.out.println("Tried to load a bad font...");
		ffe.printStackTrace();
	    } 
		catch (FileNotFoundException e) {
			System.out.println("could not open file"+ ttffile);
			e.printStackTrace();
			} 
	   catch (IOException ioe) {
				System.out.println("I have no idea what went wrong");
				ioe.printStackTrace();
			   }

		return f;
	}

	private void drawStringWithBackground(Graphics2D g,String text, int x, int y, Color bgcolor, Color textcolor)
	{
		
		//g.setFont(new Font())
		//Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("font.ttf"));
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D rect = fm.getStringBounds(text, g);

		if (bgcolor!=null)
		{
			g.setColor(bgcolor);
			g.fillRect(x,
					y - fm.getAscent(),
					(int) rect.getWidth(),
					(int) rect.getHeight());
		}

		g.setColor(textcolor);
		g.drawString(text, x, y);


	}

	public void paintLabels(Graphics2D g)
	{
		if (vertices == null) {
			return;
		}
		if (labels == null) {
			return;
		}
        labels.forEach((k,v) -> { //System.out.println("Key = "
                //+ k + ", Value = " + v);
            //System.out.println("key: " + name);
			//Label l =  labels.get(name);
			Label l = (Label)v;
			Vertex vertex = (Vertex) k;
			if (l.linesize!=0 && l.linecolor != null)
			{
				g.setColor(l.linecolor);
				g.setStroke(new BasicStroke(l.linesize*1.0f));
			}
			double rad = (90-l.lineangle)/360*2*Math.PI;
			double xdiff = l.linelength * Math.cos(rad);
			double ydiff = -l.linelength * Math.sin(rad);
			double xstart = vertex.location().get(0, 0);
			double ystart =  vertex.location().get(1, 0);
			double xend = xstart+xdiff;
			double yend = ystart+ydiff;
			if (l.linesize!=0 && l.linecolor != null)
				g.draw(new Line2D.Double(xstart,ystart,xend, yend));

			if (l.shape.equals("circle"))
			{
				double radius = l.shapesize;
				//g.setColor(l.shapefillcolor);
				//g.fillOval(x, y, width, height);
                //Shape theCircle = new Ellipse2D.Double(xstart - radius, ystart - radius, 2.0 * radius, 2.0 * radius);

				//BasicStroke dashed =new BasicStroke(3.0f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10.0f);
				BasicStroke stroke1 =new BasicStroke(l.shapeborderwidth);
    			Ellipse2D.Double circle = new Ellipse2D.Double(xstart - radius, ystart - radius, 2.0 * radius, 2.0 * radius);
    			Ellipse2D.Double circleBorder = new Ellipse2D.Double(xstart - radius, ystart - radius, 2.0 * radius, 2.0 * radius);
    			g.setColor(l.shapefillcolor);
    			//g2.setRenderingHints(hints);
    			g.fill(circle);
    			//Composite oldComposite=g.getComposite();
    			//g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0));        
    			g.setColor(l.shapebordercolor);
    			g.setStroke(stroke1);
    			g.draw(circleBorder);
    			//g2.setComposite(oldComposite);
			}
			if (!l.toptext.isEmpty())
			{
				g.setFont(getFont(l.toptextttf,(float)l.toptextsize));
				drawStringWithBackground(g,l.toptext, (int)xend, (int)yend, l.topbgfillcolor, l.toptextcolor);
			}
			if (!l.bottomtext.isEmpty())
			{
				double xend2 = xend;
				double yend2 = yend;
				if (!l.toptext.isEmpty())
				{
					FontMetrics fm = g.getFontMetrics();
					Rectangle2D rect = fm.getStringBounds(l.bottomtext, g);
					yend2+=rect.getHeight();
				}
				else
				{
					
				}
				g.setFont(getFont(l.bottomtextttf,(float)l.bottomtextsize));
				drawStringWithBackground(g,l.bottomtext, (int)xend, (int)yend2, l.bottombgfillcolor, l.bottomtextcolor);
			}
		
		
		});

	}

	public void paintVertices(Graphics g) {
		if (vertices == null) {
			return;
		}
		if (drawVertices) {
			paintNonColoredVertices(g);
		}
		// Now to draw the colored vertices. They must be drawn last
		// (so they are drawn on top);
		paintColoredVertices(g);
	}

	public void showIds(boolean b) {
		idsIncluded = b;
		if (vertices == null) {
			return;
		}
		for (int ii = 0; ii < vertices.length; ++ii) {
			vertices[ii].showID(b);
		}
	}

	public void showVertices(boolean b) {
		drawVertices = b;
		if (vertices == null) {
			return;
		}
		for (int ii = 0; ii < vertices.length; ++ii) {
			vertices[ii].showVertex(b);
		}
	}

	private void runIdRegion() {
		if (highlightSanityCheck()) {
			// Check to see which vertices are in
			// this region and set the flags to show
			// the ids.
			for (int ii = 0; ii < vertices.length; ++ii) {
				Matrix x = vertices[ii].location();
				if (((mins.get(0, 0) < x.get(0, 0)) && (x.get(0, 0) < maxs.get(
						0, 0)))
						&& ((mins.get(1, 0) < x.get(1, 0)) && (x.get(1, 0) < maxs
								.get(1, 0)))) {
					vertices[ii].showID(true);
				} else {
					vertices[ii].showID(false);
				}
			}
		}
	}

	// -----------------------------------------------------
	// HANDLING INCOMING ORDERS FROM A FINDFRAME
	// -----------------------------------------------------

	public void setIncomingLabelsAndStats(String[] ids, VertexStats s,
			boolean willLabel, boolean willZoom) {
		statusBar.setText("Looking at IDs");
		for (int ii = 0; ii < ids.length; ++ii) {
			Object o = vertexIdMap.get(ids[ii]);
			if (o == null) {
				handleBadVertex(ids[ii]);
				continue;
			}
			Vertex v = (Vertex) o;
			v.showID(willLabel);
			// System.out.println("Labeling " + v.id());
			if (willZoom) {
				s.addStatsOfVertex(v);
				// System.out.println("Adding stats of " + v.id());
			}
		}
		statusBar.setText("Done.");
	}

	public void incomingEdges(FindEdgesFrame f) {
		String[] ids = f.getIds();

		// One issue could be to redraw the edges with
		// new colors?
		if (f.willColor()) {
			edgeColorMap.putAll(setEdgeColorsFromFindEdgesFrame(ids,
					f.getEdgeColor()));
		}

		VertexStats stats = new VertexStats();

		// Here the lables are done pairwise
		if (f.willLabel() || f.willZoom()) {
			statusBar.setText("Looking at labels...");
			setIncomingLabelsAndStats(ids, stats, f.willLabel(), f.willZoom());
			statusBar.setText("Done.");
		}

		if (f.willZoom()) {
			statusBar.setText("Starting to zoom");
			// Just zooming to the average point now
			if (ids.length == 1) {
				zoom2Point(stats.avg(0), stats.avg(1));
			} else {
				mins = stats.mins();
				maxs = stats.maxs();
				runZoomRegion();
			}
			statusBar.setText("Done Zooming.");
		}

		repaint();
	}

	public void incomingVertices(FindVertexFrame f) {
		String[] ids = f.getIds();
		// Check to see if we have to show all edges
		// the given vertices may share
		if (f.willColorEdges()) {
			setEdgeColorsFromFindVertexFrame(ids, f.getEdgeColor());
		}

		if (f.willColorVertices()) {
			setVertexColorsFromFindVertexFrame(ids, f.getVertexColor());
		}

		VertexStats stats = new VertexStats();

		// Is it necessary to write labels on all the
		// selected vertices or zoom to this particular
		// region
		if (f.willLabel() || f.willZoom()) {
			statusBar.setText("Looking at labels...");
			setIncomingLabelsAndStats(ids, stats, f.willLabel(), f.willZoom());
			statusBar.setText("Done.");
		}

		if (f.willZoom()) {
			statusBar.setText("Starting to zoom...");
			// Just zooming to the average point now
			if (ids.length == 1) {
				zoom2Point(stats.avg(0), stats.avg(1));
			} else {
				mins = stats.mins();
				maxs = stats.maxs();
				runZoomRegion();
			}
			statusBar.setText("Done Zooming.");
		}

		repaint();

	}

	public HashMap setEdgeColorsFromFindVertexFrame(String ids[], Color color) {
		// statusBar.setText("Checking all possible combinations for edges. This may take time...");
		HashMap h = new HashMap();
		for (int ii = 0; ii < ids.length; ++ii) {
			for (int jj = ii + 1; jj < ids.length; ++jj) {
				// Check and see if the possible edge name exists in the
				// hash. If it does , then paint it
				Object o = edgeIdMap.get(Edge.idEdge(ids[ii], ids[jj]));
				if (o != null) {
					h.put((Edge) o, color);
				}
				// System.out.println("COLOR: " + color);
				// edges[jj].print();
			}
		}
		// statusBar.setText("Done.");
		addEdgeColors(h);
		return h;
	}

	public HashMap setEdgeColorsFromFindEdgesFrame(String ids[], Color color) {
		statusBar.setText("Finding edges to color...");
		HashMap h = new HashMap();
		for (int ii = 0; ii < ids.length; ii += 2) {
			String id1 = ids[ii];
			String id2 = ids[ii + 1];
			String edgeName = Edge.idEdge(id1, id2);
			Object o = edgeIdMap.get(edgeName);
			if (o == null) {
				handleBadEdge(id1, id2);
				continue;
			}
			h.put((Edge) o, color);
			// System.out.println("COLOR: " + color);
			// edges[jj].print();
		}
		statusBar.setText("Done.");
		return h;
	}

	// -----------------------------------------------------
	// ERROR HANDLING
	// -----------------------------------------------------

	private void handleBadVertex(String id) {
		JOptionPane.showMessageDialog(null, id + " is a Bad Vertex", "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	private void handleBadEdge(String id1, String id2) {
		JOptionPane.showMessageDialog(null, id1 + " and " + id2
				+ " is an undefined Edge.", "Error", JOptionPane.ERROR_MESSAGE);
	}

	// -----------------------------------------------------
	// ZOOMING ISSUES
	// -----------------------------------------------------

	public void undo() {
		if (fitter != null) {
			fitter.setManipulationMatrix(inverted);
			applyFit(fitter);
		}
	}

	public void runZoomRegion() {
		if (highlightSanityCheck()) {
			VertexStats stats = new VertexStats();
			stats.setMin(mins);
			stats.setMax(maxs);
			formatter.setStats(stats);
			formatter.fitDataToWindow();
			fitter = formatter.getFitter();
			inverted = fitter.getManipulationMatrix().inverse();
		}
	}


	public void zoomIn(VertexFitter f) {
		if (edges == null) {
			return;
		}
		mins.set(0, 0, zoomStepSize * xWindowSize);
		mins.set(1, 0, zoomStepSize * yWindowSize);
		maxs.set(0, 0, xWindowSize - zoomStepSize * xWindowSize);
		maxs.set(1, 0, yWindowSize - zoomStepSize * yWindowSize);
		zoomPrep(f);
		setPaintImage();
	}

	public void zoomIn2(VertexFitter f) {
		zoom(f,1/zoomStepSize);
	}

	public void zoom(VertexFitter f, double scale)
	{
		if (edges == null) {
			return;
		}
		Transformer trans = new Transformer();
		trans.scale(scale);
		f.addManipulation(trans);
		setPaintImage();

	}

	public void zoomOut2(VertexFitter f) {
		zoom(f,zoomStepSize);
	}

	public void zoomOut(VertexFitter f) {
		if (edges == null) {
			return;
		}

		/*mins.set(0, 0, -zoomStepSize * xWindowSize);
		mins.set(1, 0, -zoomStepSize * yWindowSize);
		maxs.set(0, 0, xWindowSize + zoomStepSize * xWindowSize);
		maxs.set(1, 0, yWindowSize + zoomStepSize * yWindowSize);*/

		mins.set(0, 0, -zoomStepSize * xWindowSize);
		mins.set(1, 0, -zoomStepSize * yWindowSize);
		maxs.set(0, 0, xWindowSize + zoomStepSize * xWindowSize);
		maxs.set(1, 0, yWindowSize + zoomStepSize * yWindowSize);

		zoomPrep(f);
	}

	public void handMode()
	{
       mode = modes.handmode;
	   setCursor(new Cursor(Cursor.HAND_CURSOR));
	   setComponentPopupMenu(null);
	   //menu.setVisible(false);
	   
	}

	modes getMode()
	{
      return mode;

	}

	public void  magnifierMode()
	{
		mode = modes.magnifiermode;
	   setCursor(cursorMagnifier);
	   setComponentPopupMenu(menu);
	  

	}

	public void noMode()
	{
       mode = modes.nomode;
	   setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	   setComponentPopupMenu(null);
	   //menu.setVisible(false);
	}

	public void zoom2Point(double x, double y) {
		if (edges == null) {
			return;
		}
		VertexFitter f = new VertexFitter();
		move2Point(-x, -y, f);
		zoomIn2(f);
		move2Point(x, y, f);
		

		//move2Point(-x, -y, f);
		//move2Point(-2*x/zoomStepSize, -2*y/zoomStepSize, f);
		applyFit(f);
	}

	public void zoom2PointBy(double x, double y, double scale) {
		if (edges == null) {
			return;
		}
		VertexFitter f = new VertexFitter();
		move2Point(-x, -y, f);
		zoom(f,scale);
		move2Point(x, y, f);
		

		//move2Point(-x, -y, f);
		//move2Point(-2*x/zoomStepSize, -2*y/zoomStepSize, f);
		applyFit(f);
	}



	public void zoomOutFromPoint(double x, double y) {
		if (edges == null) {
			return;
		}
		VertexFitter f = new VertexFitter();
		//move2Point(x, y, f);
		//zoomOut(f);
		move2Point(-x, -y, f);
		zoomOut2(f);
		move2Point(x, y, f);


		applyFit(f);
	}

	private void zoomPrep(VertexFitter f) {
		double[] scales = new double[2];
		double spanx = maxs.get(0, 0) - mins.get(0, 0);
		double spany = maxs.get(1, 0) - mins.get(1, 0);
		// Scale to fit new range
		scales[0] = xWindowSize / spanx;
		scales[1] = xWindowSize / spanx;//yWindowSize / spany;
		Transformer trans = new Transformer();
		trans.scale(scales);
		f.addManipulation(trans);
		// Translate to center
		double[] moves = new double[2];
		moves[0] = zoomStepSize * xWindowSize;
		if (scales[0] > 1) {
			moves[0] *= -1;
		}
		moves[1] = zoomStepSize * yWindowSize;
		if (scales[1] > 1) {
			moves[1] *= -1;
		}
		Transformer trans2 = new Transformer();
		trans2.move(moves);
		f.addManipulation(trans2);
	}

	// -----------------------------------------------------
	// MOVING ISSUES
	// -----------------------------------------------------

	public void moveUp() {
		if (edges == null) {
			return;
		}

		double move = moveStepSize * yWindowSize;

		mins.set(0, 0, 0);
		mins.set(1, 0, move);
		maxs.set(0, 0, xWindowSize);
		maxs.set(1, 0, move + yWindowSize);

		VertexFitter f = new VertexFitter();
		movePrep(f);
		applyFit(f);
	}

	public void moveDown() {
		if (edges == null) {
			return;
		}

		double move = moveStepSize * yWindowSize;

		mins.set(0, 0, 0);
		mins.set(1, 0, -move);
		maxs.set(0, 0, xWindowSize);
		maxs.set(1, 0, -move + yWindowSize);

		VertexFitter f = new VertexFitter();
		movePrep(f);
		applyFit(f);
	}

	public void moveLeft() {
		if (edges == null) {
			return;
		}

		double move = moveStepSize * yWindowSize;

		mins.set(0, 0, move);
		mins.set(1, 0, 0);
		maxs.set(0, 0, xWindowSize + move);
		maxs.set(1, 0, yWindowSize);

		VertexFitter f = new VertexFitter();
		movePrep(f);
		applyFit(f);
	}

	public void moveRight() {
		if (edges == null) {
			return;
		}

		double move = moveStepSize * yWindowSize;

		mins.set(0, 0, -move);
		mins.set(1, 0, 0);
		maxs.set(0, 0, xWindowSize - move);
		maxs.set(1, 0, yWindowSize);

		VertexFitter f = new VertexFitter();
		movePrep(f);
		applyFit(f);
	}

	private void move2Point(double x, double y, VertexFitter f) {
		if (edges == null) {
			return;
		}
		double[] moves = new double[2];
		moves[0] = -x;// + xWindowSize * .5;
		moves[1] = -y; //+ yWindowSize * .5;
		Transformer trans = new Transformer();
		trans.move(moves);
		f.addManipulation(trans);
	}

	private void movePrep(VertexFitter f) {
		double[] moves = new double[2];
		moves[0] = (maxs.get(0, 0) + mins.get(0, 0)) * .5 - xWindowSize * .5;
		moves[1] = (maxs.get(1, 0) + mins.get(1, 0)) * .5 - yWindowSize * .5;
		Transformer trans = new Transformer();
		trans.move(moves);
		f.addManipulation(trans);
	}

	private void setRenderingHints(Graphics2D g2) {
		// g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION ,
		// RenderingHints.VALUE_INTERPOLATION_BILINEAR );
		// g2.setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION ,
		// RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY );
		// g2.setRenderingHint( RenderingHints.KEY_COLOR_RENDERING ,
		// RenderingHints.VALUE_COLOR_RENDER_QUALITY );
		// g2.setRenderingHint( RenderingHints.KEY_RENDERING ,
		// RenderingHints.VALUE_RENDER_QUALITY );
		// g2.setRenderingHint( RenderingHints.KEY_DITHERING ,
		// RenderingHints.VALUE_DITHER_ENABLE );
		// SESS - 2014.05.10
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
	}

	// -----------------------------------------------------
	// MOUSE EVENT ISSUES SUCH AS HIGHLIGHTING ETC.
	// -----------------------------------------------------

	public void mousePressed(MouseEvent e) {
		if (mode == modes.handmode)
		{
			x1 = e.getX();
			y1 = e.getY();
			handtouch  = true;
			System.out.println("click");
		}
		else
		if (zoomRegion || idRegion) {
			x1 = e.getX();
			y1 = e.getY();
			statusBar.setText(statusMessage + ": Pressed at (" + x1 + "," + y1
					+ ")");
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (mode == modes.handmode && handtouch)
		{
			handtouch = false;
		}
		if (zoomRegion || idRegion) {
			x2 = e.getX();
			y2 = e.getY();
			statusBar.setText("Region set to (" + x1 + "," + y1 + ") to ("
					+ e.getX() + "," + e.getY() + ") Working on redraw...");
			mins.set(0, 0, Math.min(x1, x2));
			mins.set(1, 0, Math.min(y1, y2));
			maxs.set(0, 0, Math.max(x1, x2));
			maxs.set(1, 0, Math.max(y1, y2));
			if (zoomRegion) {
				runZoomRegion();
			}
			if (idRegion) {
				runIdRegion();
			}
			setPaintImage();
			repaint();
			statusBar.setText("Done.");
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (mode == modes.handmode && handtouch)
		{
			if (edges == null) {
				return;
			}
			System.out.println("updating" +  e.getX());
			double x2 = e.getX();
			double y2 = e.getY();
		
		
				double ymove = y2-y1;

				double xmove = x2-x1;
		
				mins.set(0, 0, xmove);
				mins.set(1, 0, ymove);
				maxs.set(0, 0, xmove+ xWindowSize);
				maxs.set(1, 0, ymove + yWindowSize);
		
				VertexFitter f = new VertexFitter();
				movePrep(f);
				applyFit(f);
				setPaintImage(); // TODO: move inside panel.xxxx()?
				repaint();
			
		
			
		
				/*double move = moveStepSize * yWindowSize;
		
				mins.set(0, 0, move);
				mins.set(1, 0, 0);
				maxs.set(0, 0, xWindowSize + move);
				maxs.set(1, 0, yWindowSize);*/
		   x1 = x2;
		   y1 = y2;
			
		}
		if (zoomRegion || idRegion) {
			statusBar.setText("Highlighting region from (" + x1 + "," + y1
					+ ") to (" + e.getX() + "," + e.getY() + ")");
		}
	}

	public void mouseClicked(MouseEvent e) {

		if (mode==modes.magnifiermode)
		{
			x1 = e.getX();
			y1 = e.getY();
			
			zoom2Point(x1, y1);
			setPaintImage();
			repaint();
			
		}

		if (zoomPoint) {
			x1 = e.getX();
			y1 = e.getY();
			// For left clicks zoom in and for right clicks zoom out
			if (SwingUtilities.isLeftMouseButton(e)) {
				statusBar.setText(statusMessage + ": Zooming to (" + x1 + ","
						+ y1 + ")");
				zoom2Point(x1, y1);
				setPaintImage();
				repaint();
			} else if (SwingUtilities.isRightMouseButton(e)) {
				statusBar.setText(statusMessage + ": Zooming out from (" + x1
						+ "," + y1 + ")");
				zoomOutFromPoint(x1, y1);
				setPaintImage();
				repaint();
			} else {
				statusBar
						.setText("Left click zooms in. Right click zooms out.");
			}
		}
	}

	public void mouseMoved(MouseEvent e) {

		//System.out.println(e.getX()+" "+e.getY());
 

	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseWheelMoved(MouseWheelEvent e) {

		if (mode==modes.magnifiermode)
		{
			int notches = e.getWheelRotation();
			x1 = e.getX();
			y1 = e.getY();
			if (notches < 0) 
				{
					
					zoom2Point(x1, y1);
					setPaintImage();
					repaint();
					/*VertexFitter f = new VertexFitter();
					zoomIn(f);
					applyFit(f);
					setPaintImage(); // TODO: move inside panel.xxxx()?
					repaint();*/
				}
			else
			{
				zoomOutFromPoint(x1, y1);
				setPaintImage();
				repaint();
				/*VertexFitter f = new VertexFitter();
				zoomOut(f);
				applyFit(f);
				setPaintImage(); // TODO: move inside panel.xxxx()?
				repaint();*/

			}

		}
		String message;
		/*int notches = e.getWheelRotation();
		if (notches < 0) {
			message = "Mouse wheel moved UP "
						 + -notches + " notch(es)\n";
		} else {
			message = "Mouse wheel moved DOWN "
						 + notches + " notch(es)\n";
		}
		System.out.println(message);*/
		
	 }

}
