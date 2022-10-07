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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import Viewer2D.EdgesPanel.modes;

/**
 * <b>SESS - 2014.05.11:</b>
 * <ul>
 * <li>Initial frame size set to ({@link #FRAME_WIDTH}, {@link #FRAME_HEIGHT}).</li>
 * <li>frame is center on the screen.</p>
 * </ul>
 * <b>SESS - 2014.05.11:</b>
 * <ul>
 * <li>Cleanup code a little bit.</li>
 * <li>Resizable frame. Frame's layout is Border now, was GridBag</li>
 * <li>Use the same JFileChooser so last directory is remember.</li>
 * </ul>
 */
// TODO: SESS - Leverage UI thread. All operations are run on UI thread!
// TODO: SESS - Also show "working, please wait..."
// TODO: SESS - When edges panel should repaint is repainting this frame
// should be only the edges panel
public class EdgesFrame extends JFrame implements KeyListener {
	private static final long serialVersionUID = 1099866981814538683L;

	static final int FRAME_WIDTH = 1024;
	static final int FRAME_HEIGHT = 768;

	private Edge[] edges;
	private Vertex[] vertices;

	private HashMap labels;

	private double labelScale;

	private EdgesPanel panel; // Has the edges drawn
	private JTextField statusBar; // Shows highlighted/user info

	private String statusMessage;

	private ViewerIO edgesio;
	private File edgesFile;
	private File coordsFile;

	// These are hashes relating edge/vertex id
	// to specific info
	@SuppressWarnings("rawtypes")
	private HashMap vertexIdMap;
	@SuppressWarnings("rawtypes")
	private HashMap edgeIdMap;

	private int threads;

	private Container container;

	private BorderLayout layout;

	private FormatVertex formatter;
	private int[] windowSizes;

	private double moveStepSize;

	private double zoomStepSize;

	// Related to the showing ids
	private JRadioButtonMenuItem showFonts, showIdsHighlighted,
			zoomHighlighted, zoomRegion, showVertices;

	private Font font;

	private int vertexRadius;

	private JRadioButtonMenuItem[] idStyles;

	private JRadioButton blockers;

	private Color fontColor, vertexColor, edgeColor, backgroundColor;

	private int STATUSBAR_Y;
	

	JToggleButton hand;

	JToggleButton magnifier;

	public EdgesFrame(String title, int x, int y) {
		super(title);

		threads = 1;

		STATUSBAR_Y = 50;

		moveStepSize = .2;
		zoomStepSize = .8;

		windowSizes = new int[2];
		windowSizes[0] = x;
		windowSizes[1] = y;

		font = new Font("Helvetica", Font.BOLD, 13);

		vertexRadius = 1;
		vertexColor = Color.red;
		fontColor = Color.blue;
		edgeColor = Color.black;
		backgroundColor = Color.white;

		container = getContentPane();
		layout = new BorderLayout();
		container.setLayout(layout);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel = new EdgesPanel(edges, vertices, new HashMap(),windowSizes[0], windowSizes[1]);
		panel.setBackground(backgroundColor);

		setMenuBars();
		hand = new JToggleButton("Hand");
		magnifier = new JToggleButton("Magnifier");
		setButtons();
		addKeyListener(this);

		JScrollPane scrollPane = new JScrollPane(panel);
		container.add(scrollPane, BorderLayout.CENTER);

		statusBar = new JTextField(statusMessage, STATUSBAR_Y);
		statusBar.setEditable(false);
		container.add(statusBar, BorderLayout.SOUTH);
		panel.setStatusBar(statusBar);

		// Let's start with the screen size
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int width = Math.min(screen.width, FRAME_WIDTH);
		int height = Math.min(screen.height, FRAME_HEIGHT);
		setSize(width, height);
		setLocation((screen.width - width) / 2, (screen.height - height) / 2);
		setResizable(true);
		setVisible(true);
		labels = new HashMap<>();
		labelScale = 1;
		/*ActionListener okListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
				System.out.println("2");
            }
        };
		getRootPane().registerKeyboardAction(okListener,
		KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
		JComponent.WHEN_IN_FOCUSED_WINDOW);*/
		//KeyStroke k = .getKeyStroke(' ' );
		//k.getKeyStroke(keyChar, onKeyRelease)
	

		panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(' ' ), "SPACE");
	
		panel.getActionMap().put("SPACE", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				//do nothing
			 
				if (panel.getMode()!=modes.handmode)
				{
					setMode(modes.handmode);
				}
				else
					setMode(modes.nomode);
			}
		});
		
	
	}

	private JFileChooser fileChooser;

	/**
	 * <p>
	 * Use the same file chooser so last directory is remember.
	 * </p>
	 * 
	 * @param ext
	 *            Default extension for FileSuffixFilter.
	 * @return
	 */
	protected JFileChooser getFileChooser(String ext) {
		if (fileChooser == null) {
			fileChooser = new JFileChooser(System.getProperty("user.dir"));
		}
		fileChooser.resetChoosableFileFilters();
		fileChooser.setSelectedFile(null);
		if (ext != null) {
			fileChooser.setFileFilter(new FileSuffixFilter(ext));
		}
		return fileChooser;
	}

	protected JFileChooser getFileChooser() {
		return getFileChooser(null);
	}

	private void setMenuBars() {
		JMenuBar bar = new JMenuBar();
		setJMenuBar(bar);

		// -----------------------------------------------------
		// FILE ISSUES
		// -----------------------------------------------------

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		// LOAD THE EDGES FILE ( SOMETHING.lgl )
		JMenuItem openFile = new JMenuItem("Open .lgl file");
		openFile.setMnemonic('O');
		openFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = getFileChooser("lgl");
				int returnVal = chooser.showOpenDialog(EdgesFrame.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					edgesFile = chooser.getSelectedFile();
					loadSHORTFile(edgesFile);
				}
			}
		});
		fileMenu.add(openFile);

		// LOAD THE 2D COORDS
		JMenuItem cFile = new JMenuItem("Open 2D Coords file");
		cFile.setMnemonic('C');
		cFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (edgesCheck()) {
					JFileChooser chooser = getFileChooser();
					int returnVal = chooser.showOpenDialog(EdgesFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						coordsFile = chooser.getSelectedFile();
						loadCoordsFile(coordsFile);
					}
				}
			}
		});
		fileMenu.add(cFile);

		// LOAD VERTEX DESCRIPTIONS
		JMenuItem openVertexDescripFile = new JMenuItem(
				"Load Vertex Information");
		openVertexDescripFile.setMnemonic('D');
		openVertexDescripFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vertexCheck()) {
					JFileChooser chooser = getFileChooser();
					int returnVal = chooser.showOpenDialog(EdgesFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						loadVertexDescriptions(chooser.getSelectedFile());
					}
				}
			}
		});
		fileMenu.add(openVertexDescripFile);

		// OPEN EDGE COLOR FILE
		JMenuItem edgeColorMenu = new JMenuItem("Open Edge Color File");
		edgeColorMenu.setMnemonic('O');
		edgeColorMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vertexCheck()) {
					JFileChooser chooser = getFileChooser();
					int returnVal = chooser.showOpenDialog(EdgesFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						loadEdgeColorFile(chooser.getSelectedFile());
					}
				}
			}
		});
		fileMenu.add(edgeColorMenu);

		// SAVE THE CURRENT EDGE COLORS
		JMenuItem saveEdgeColorMenu = new JMenuItem("Save Edge Colors");
		saveEdgeColorMenu.setMnemonic('S');
		saveEdgeColorMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vertexCheck()) {
					JFileChooser chooser = getFileChooser();
					int returnVal = chooser.showSaveDialog(EdgesFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						panel.saveEdgeColorMap(chooser.getSelectedFile());
					}
				}
			}
		});
		fileMenu.add(saveEdgeColorMenu);

		// OPEN VERTEX COLOR FILE
		JMenuItem vertexColorMenu = new JMenuItem("Open Vertex Color File");
		vertexColorMenu.setMnemonic('O');
		vertexColorMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vertexCheck()) {
					JFileChooser chooser = getFileChooser();
					int returnVal = chooser.showOpenDialog(EdgesFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						loadVertexColorFile(chooser.getSelectedFile());
					}
				}
			}
		});
		fileMenu.add(vertexColorMenu);

		// SAVE THE CURRENT VERTEX COLORS
		JMenuItem saveVertexColorMenu = new JMenuItem("Save Vertex Colors");
		saveVertexColorMenu.setMnemonic('S');
		saveVertexColorMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vertexCheck()) {
					JFileChooser chooser = getFileChooser();
					int returnVal = chooser.showSaveDialog(EdgesFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						panel.saveVertexColorMap(chooser.getSelectedFile());
					}
				}
			}
		});
		fileMenu.add(saveVertexColorMenu);

		// RELOAD THE ORIGINAL FILE
		JMenuItem reload = new JMenuItem("Reload 2D Coords File");
		reload.setMnemonic('R');
		reload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (edgesCheck() && coordsFile != null) {
					loadCoordsFile(coordsFile);
				}
			}
		});
		fileMenu.add(reload);

		// load labels

		JMenuItem labelMenu = new JMenuItem("Load labels");
		labelMenu.setMnemonic('L');
		labelMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (vertexCheck()) {
					JFileChooser chooser = getFileChooser();
					int returnVal = chooser.showOpenDialog(EdgesFrame.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						loadLabelFile(chooser.getSelectedFile());
					}
				}
			}
		});
		fileMenu.add(labelMenu);





		// APPLICATION EXIT
		JMenuItem exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		fileMenu.add(exit);

		bar.add(fileMenu);

		// -----------------------------------------------------
		// EDITING PREFERENCES
		// -----------------------------------------------------

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic('E');

		// CHANGE THE STEP SIZE OF THE MOVE
		JMenuItem movesize = new JMenuItem("Change Move Step Size");
		movesize.setMnemonic('M');
		movesize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String newSize = JOptionPane.showInputDialog(EdgesFrame.this,
						"Enter the new Move step size (0,1)",
						Double.toString(moveStepSize));
				if (newSize != null) {
					double newStepSize = Double.parseDouble(newSize);
					if (newStepSize <= 0 || newStepSize >= 1) {
						JOptionPane.showMessageDialog(null, "Illegal Value",
								"Error", JOptionPane.ERROR_MESSAGE);
					} else {
						moveStepSize = newStepSize;
						panel.setMoveStepSize(newStepSize);
					}
				}
			}
		});
		edit.add(movesize);

		// CHANGE THE STEP SIZE OF ZOOMING
		JMenuItem zoomsize = new JMenuItem("Change Zoom Step Size");
		zoomsize.setMnemonic('Z');
		zoomsize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String newSize = JOptionPane.showInputDialog(EdgesFrame.this,
						"Enter the new Zoom step size (0,1)",
						Double.toString(zoomStepSize));
				if (newSize != null) {
					double newZoomStepSize = Double.parseDouble(newSize);
					if (newZoomStepSize <= 0 || newZoomStepSize >= 1) {
						JOptionPane.showMessageDialog(null, "Illegal Value",
								"Error", JOptionPane.ERROR_MESSAGE);
					} else {
						zoomStepSize = newZoomStepSize;
						panel.setZoomStepSize(zoomStepSize);
					}
				}
			}
		});
		edit.add(zoomsize);

		// BUTTON TO REMOVE TRANSIENT EDGES
		blockers = new JRadioButton("Remove Transient Edges");
		blockers.setMnemonic('R');
		blockers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (blockers.isSelected()) {
					panel.setVisibilityTest(true);
					statusBar.setText("Removing Transient Edges");
				} else {
					panel.setVisibilityTest(false);
					statusBar.setText("Showing Transient Edges");
				}
			}
		});
		blockers.setSelected(true);
		panel.setVisibilityTest(true);
		edit.add(blockers);

		bar.add(edit);

		// -----------------------------------------------------
		// HIGHLIGHT EVENTS
		// -----------------------------------------------------

		JMenu highlight = new JMenu("Highlight");
		highlight.setMnemonic('H');

		// CLICK TO ZOOM
		zoomHighlighted = new JRadioButtonMenuItem("Zoom Point");
		zoomHighlighted.setMnemonic('Z');
		zoomHighlighted.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (zoomHighlighted.isSelected()) {
					panel.prepZoomPoint(true);
					statusMessage = "Enabled Zoom Point";
					setMode(modes.nomode);
					
				} else {
					panel.prepZoomPoint(false);
					statusMessage = "Disabled Zoom Point";
				}
				statusBar.setText(statusMessage);
			}
		});
		zoomHighlighted.setSelected(false);
		highlight.add(zoomHighlighted);

		// HIGHLIGHTING A REGION TO ZOOM INTO
		zoomRegion = new JRadioButtonMenuItem("Zoom Region");
		zoomRegion.setMnemonic('R');
		zoomRegion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (zoomRegion.isSelected()) {
					panel.prepZoomRegion(true);
					statusMessage = "Enabled Zoom Region";
					setMode(modes.nomode);
				} else {
					panel.prepZoomRegion(false);
					statusMessage = "Disabled Zoom Region";
				}
				statusBar.setText(statusMessage);
			}
		});
		zoomRegion.setSelected(false);
		highlight.add(zoomRegion);

		// HIGHLIGHT TO SHOW IDS
		showIdsHighlighted = new JRadioButtonMenuItem("ID Region");
		showIdsHighlighted.setMnemonic('I');
		showIdsHighlighted.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (panel.getVertices() != null) {
					if (showIdsHighlighted.isSelected()) {
						panel.prepIdRegion(true);
						statusMessage = "Enabled ID Region";
					} else {
						panel.prepIdRegion(false);
						statusMessage = "Disabled ID Region";
					}
					statusBar.setText(statusMessage);
				}
			}
		});
		showIdsHighlighted.setSelected(false);
		highlight.add(showIdsHighlighted);

		bar.add(highlight);

		// -----------------------------------------------------
		// FORMAT ISSUES ( SHOWING IDS , COLORS ETC. )
		// -----------------------------------------------------

		JMenu formatMenu = new JMenu("Format");
		formatMenu.setMnemonic('F');

		// FONT ISSUES
		JMenu fontIssues = new JMenu("Font");
		fontIssues.setMnemonic('F');

		// CHANGING FONT COLOR
		JMenuItem idColor = new JMenuItem("Color");
		idColor.setMnemonic('C');
		idColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = JColorChooser.showDialog(EdgesFrame.this,
						"Choose The Font Color", fontColor);
				if (c != null) {
					fontColor = c;
					panel.setFontColor(fontColor);
					// TODO: SESS - Check if something related to font is
					// rendered
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				}
			}
		});
		fontIssues.add(idColor);

		// CHANGING ID SIZE
		JMenuItem idSize = new JMenuItem("Size");
		idSize.setMnemonic('S');
		idSize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String newSize = JOptionPane.showInputDialog(EdgesFrame.this,
						"Enter the new Font Size",
						Float.toString(font.getSize2D()));
				if (newSize != null) {
					float newFontSize = Float.parseFloat(newSize);
					if (newFontSize <= 0.0) {
						JOptionPane.showMessageDialog(null, "Illegal Value",
								"Error", JOptionPane.ERROR_MESSAGE);
					} else {
						font = font.deriveFont(newFontSize);
						panel.setFont(font);
						// TODO: SESS - Check if something related to font is
						// rendered
						panel.setPaintImage(); // TODO: move inside
												// panel.xxxx()?
						panel.repaint();
					}
				}
			}
		});
		fontIssues.add(idSize);

		fontIssues.addSeparator();

		// CHANGING ID STYLE
		String styles[] = { "Bold", "Italic" };
		idStyles = new JRadioButtonMenuItem[styles.length];
		StyleHandler stylehandler = new StyleHandler();
		for (int ii = 0; ii < styles.length; ++ii) {
			idStyles[ii] = new JRadioButtonMenuItem(styles[ii]);
			fontIssues.add(idStyles[ii]);
			idStyles[ii].addItemListener(stylehandler);
		}
		// Bold is on by default
		idStyles[0].setSelected(true);

		formatMenu.add(fontIssues);

		// VERTEX ISSUES
		JMenu vertexIssues = new JMenu("Vertices");
		vertexIssues.setMnemonic('V');

		// CLEAR ALL VERTEX COLORS
		JMenuItem vertexClear = new JMenuItem("Clear Vertex Colors");
		vertexClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.clearAllVertexColors();
				edgesio.clearAllVertexColors();
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		vertexIssues.add(vertexClear);

		// CHANGING THE VERTEX COLOR
		JMenuItem vertexC = new JMenuItem("Color");
		vertexC.setMnemonic('C');
		vertexC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = JColorChooser.showDialog(EdgesFrame.this,
						"Choose The Vertex Color", vertexColor);
				if (c != null) {
					vertexColor = c;
					panel.setVertexColor(vertexColor);
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				}
			}
		});
		vertexIssues.add(vertexC);
		formatMenu.add(vertexIssues);

		// CHANGING THE VERTEX SIZE
		JMenuItem vertexS = new JMenuItem("Size");
		vertexS.setMnemonic('S');
		vertexS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String newSize = JOptionPane.showInputDialog(EdgesFrame.this,
						"Enter the new Vertex Radius in Pixels", ""
								+ vertexRadius);
				if (newSize != null) {
					int newVSize = Integer.parseInt(newSize);
					if (newVSize <= 0) {
						JOptionPane.showMessageDialog(null, "Illegal Value",
								"Error", JOptionPane.ERROR_MESSAGE);
					} else {
						vertexRadius = newVSize;
						panel.setVertexRadius(vertexRadius);
						panel.setPaintImage(); // TODO: move inside
												// panel.xxxx()?
						panel.repaint();
					}
				}
			}
		});
		vertexIssues.add(vertexS);
		formatMenu.add(vertexIssues);

		// EDGE ISSUES
		JMenu edgeIssues = new JMenu("Edges");
		edgeIssues.setMnemonic('E');

		// CLEAR ALL EDGE COLORS
		JMenuItem edgeClear = new JMenuItem("Clear Edge Colors");
		edgeClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.clearAllEdgeColors();
				edgesio.clearAllEdgeColors();
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		edgeIssues.add(edgeClear);

		// CHANGING EDGE COLOR
		JMenuItem edgeC = new JMenuItem("Color");
		edgeC.setMnemonic('C');
		edgeC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = JColorChooser.showDialog(EdgesFrame.this,
						"Choose The Edge Color", edgeColor);
				if (c != null) {
					edgeColor = c;
					panel.setEdgeColor(edgeColor);
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				}
			}
		});
		edgeIssues.add(edgeC);
		formatMenu.add(edgeIssues);

		// CHANGING BACKGROUND COLOR
		JMenuItem backgroundC = new JMenuItem("Background Color");
		backgroundC.setMnemonic('B');
		backgroundC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = JColorChooser.showDialog(EdgesFrame.this,
						"Choose The Background Color", backgroundColor);
				if (c != null) {
					backgroundColor = c;
					panel.setBackgroundColor(backgroundColor);
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				}
			}
		});
		formatMenu.add(backgroundC);

		bar.add(formatMenu);

		// -----------------------------------------------------
		// FINDING SPECIFIC VERTICES
		// -----------------------------------------------------

		JMenu find = new JMenu("Find");
		find.setMnemonic('F');

		// FIND VERTICES
		JMenuItem findVertex = new JMenuItem("Find Vertices");
		findVertex.setMnemonic('V');
		findVertex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new FindVertexFrame("Find Vertices", panel);
			}
		});
		find.add(findVertex);

		// FIND EDGES
		JMenuItem findEdges = new JMenuItem("Find Edges");
		findEdges.setMnemonic('E');
		findEdges.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new FindEdgesFrame("Find Edges", panel);
			}
		});
		find.add(findEdges);

		bar.add(find);

	}

	@SuppressWarnings("unchecked")
	private void loadVertexDescriptions(File f) {
		boolean saidAlready = false;
		System.out.print("Loading Vertex Descriptions...");

		@SuppressWarnings("rawtypes")
		HashMap vertexDescriptions = new HashMap();

		try {
			FileInputHandler fileio = new FileInputHandler(f.getAbsolutePath());

			while (fileio.readNextLine()) {
				// The first entry is the vertex id, and the
				// remaining is part of the description
				String id = fileio.getToken(0);
				Vertex v = (Vertex) vertexIdMap.get(id);
				if (v != null) {
					String descrip = new String();
					for (int ii = 1; ii < fileio.getTokenCount(); ++ii) {
						descrip += " " + fileio.getToken(ii);
					}
					vertexDescriptions.put(v, descrip);
				} else {
					if (!saidAlready) {
						System.out
								.print("At least one id given has no matching vertex...");
						saidAlready = true;
					}
				}
			}

			// SESS: Didn't deleted because don't know what is,
			// just removed unused variable.
			new VertexDescripTable(panel, vertexDescriptions);

			System.out.println("Done.");

		} catch (FileNotFoundException ee) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException ee) {
			JOptionPane.showMessageDialog(null, "IO Error: Check File Format",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
	}

	public void update(Graphics g) {
		super.update(g);
		this.paint(g);
	}

	public void loadSHORTFile(File f) {
		edgesFile = f;
		try {
			edgesio = new ViewerIO(edgesFile);
			edgesio.loadSHORTFile(f);
			edges = edgesio.getEdges();
		} catch (FileNotFoundException ee) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException ee) {
			JOptionPane.showMessageDialog(null, "IO Error: Check File Format",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void loadEdgeColorFile(File f) {
		System.out.println("Loading Edge Color File " + f.getAbsolutePath());
		try {
			edgesio.loadEdgeColorFile(f);
			panel.addEdgeColors(edgesio.getEdgeColorMap());
			panel.setPaintImage(); // TODO: Shall we do this in setters?
			panel.repaint();
		} catch (FileNotFoundException ee) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException ee) {
			JOptionPane.showMessageDialog(null, "IO Error: Check File Format",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void loadVertexColorFile(File f) {
		System.out.println("Loading Vertex Color File " + f.getAbsolutePath());
		try {
			edgesio.loadVertexColorFile(f);
			// Update the panel with the new colors
			panel.addVertexColors(edgesio.getVertexColorMap());
			panel.repaint();
		} catch (FileNotFoundException ee) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException ee) {
			JOptionPane.showMessageDialog(null, "IO Error: Check File Format",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void  loadLabelFile(File f) {
		System.out.println("Loading Label File " + f.getAbsolutePath());
		try {
			edgesio.loadLabelFile(f);
			// Update the panel with tlabels
			panel.addLabels(edgesio.getLabels());
			panel.setPaintImage();
			panel.repaint();
		} catch (FileNotFoundException ee) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException ee) {
			JOptionPane.showMessageDialog(null, "IO Error: Check File Format",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void loadCoordsFile(File f) {
		coordsFile = f;
		try {
			edgesio.loadVertexCoords(f);
			edges = edgesio.getEdges();
			panel.setEdges(edges);
			vertices = edgesio.getVertices();
			panel.setVertices(vertices);
			panel.setVertexRadius(vertexRadius);
			panel.addEdgeColors(edgesio.getEdgeColorMap());
			vertexIdMap = edgesio.getVertexIdMap();
			panel.setVertexIdMap(vertexIdMap);
			edgeIdMap = edgesio.getEdgeIdMap();
			panel.setEdgeIdMap(edgeIdMap);
			panel.setFontColor(fontColor);
			panel.setEdgeColor(edgeColor);
			panel.setVertexColor(vertexColor);
			panel.setBackgroundColor(backgroundColor);
			panel.setFont(font);
			panel.setMoveStepSize(moveStepSize);
			panel.setZoomStepSize(zoomStepSize);
			formatter = new FormatVertex(vertices, labels,labelScale,0,0,0,0,false,edgesio.getStats(),
					windowSizes, threads);
			panel.setFormatter(formatter);
			panel.fitData();
			panel.setPaintImage(); // TODO: Shall we do this in setters?
			panel.repaint();
		} catch (FileNotFoundException ee) {
			JOptionPane.showMessageDialog(null, "File Not Found", "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(null,
					"IO Error:\n" + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Unexpected error:\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// -----------------------------------------------------
	// VIEW MENU. HANDLES ZOOMING MOVING ID'ING ETC
	// -----------------------------------------------------

	/**
	 * <p>
	 * Panel containing action buttons.
	 * </p>
	 */
	private JPanel buttonsPanel;

	private void resetZooms()
	{
		panel.prepZoomPoint(false);
		panel.prepZoomRegion(false);
		zoomHighlighted.setSelected(false);
		zoomRegion.setSelected(false);
	}

	private void inactiveButton(JToggleButton b)
	{
		b.setContentAreaFilled(true);
		b.setForeground(new JButton().getForeground());
		b.setSelected(false);

	}

	private void setMode(modes mode)
	{
		if (mode==modes.handmode)
		{
			panel.handMode();
			resetZooms();
			magnifier.setSelected(false);
			inactiveButton(magnifier);
			hand.setContentAreaFilled(false);
			hand.setOpaque(true);
			hand.setForeground(Color.green);

		}

	

		if (mode==modes.magnifiermode)
		{
			panel.magnifierMode();
			resetZooms();
			hand.setSelected(false);
			inactiveButton(hand);
			magnifier.setContentAreaFilled(false);
			magnifier.setOpaque(true);
			magnifier.setForeground(Color.green);
		}

		if (mode==modes.nomode)
		{
			inactiveButton(hand);
			inactiveButton(magnifier);
			panel.noMode();

		}

		
	}

	private void setButtons() {
		buttonsPanel = new JPanel();
		((FlowLayout) buttonsPanel.getLayout()).setAlignment(FlowLayout.LEFT);
		container.add(buttonsPanel, BorderLayout.NORTH);

		JButton undo = new JButton("Undo");
		undo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.undo();
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(undo);

		/*JButton zoomIn = new JButton("In");
		zoomIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VertexFitter f = new VertexFitter();
				panel.zoomIn(f);
				panel.applyFit(f);
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(zoomIn);

		JButton zoomOut = new JButton("Out");
		zoomOut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VertexFitter f = new VertexFitter();
				panel.zoomOut(f);
				panel.applyFit(f);
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(zoomOut);*/


		
		hand.setContentAreaFilled(false);
		hand.setOpaque(true);
		hand.setBackground(Color.black);
		
		
		//hand.setContentAreaFilled(false);
		hand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(hand.getModel().isSelected());
				if (hand.getModel().isSelected())
				{
					setMode(modes.handmode);
				}	
				else
				{
					setMode(modes.nomode);
				
					//hand.setSelected(false);
				}
				
			}
		});
		buttonsPanel.add(hand);


		
		magnifier.setContentAreaFilled(false);
		magnifier.setOpaque(true);
		magnifier.setBackground(Color.black);
			
		
		//magnifier.setContentAreaFilled(false);
		magnifier.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(magnifier.getModel().isSelected());
				if (magnifier.getModel().isSelected())
				{
					setMode(modes.magnifiermode);
				}	
				else
				{
					setMode(modes.nomode);
					//hand.setSelected(false);
				}
				
			}
		});
		buttonsPanel.add(magnifier);

		/*JButton moveup = new JButton("Up");
		moveup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.moveUp();
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(moveup);

		JButton movedown = new JButton("Down");
		movedown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.moveDown();
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(movedown);

		JButton moveleft = new JButton("Left");
		moveleft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.moveLeft();
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(moveleft);

		JButton moveright = new JButton("Right");
		moveright.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				panel.moveRight();
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(moveright);*/

		JButton fit = new JButton("Fit");
		fit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// TODO: SESS - twice?
				panel.fitData();
				panel.fitData();
				System.out.println("Fit data still buggy.");
				panel.setPaintImage(); // TODO: move inside panel.xxxx()?
				panel.repaint();
			}
		});
		buttonsPanel.add(fit);

		JButton snap = new JButton("SnapShot");
		snap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				statusBar.setText("Preparing to write Image");
				JFileChooser chooser = getFileChooser("png");
				int returnVal = chooser.showSaveDialog(EdgesFrame.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					panel.writeImage(chooser.getSelectedFile()
							.getAbsolutePath(), null);
				}
			}
		});
		buttonsPanel.add(snap);

		showFonts = new JRadioButtonMenuItem("Show All IDs");
		showFonts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (showFonts.isSelected()) {
					panel.setFontColor(fontColor);
					panel.setFont(font);
					panel.showIds(true);
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				} else {
					panel.showIds(false);
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				}
			}
		});
		showFonts.setSelected(false);
		buttonsPanel.add(showFonts);

		showVertices = new JRadioButtonMenuItem("Show All Vertices");
		showVertices.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (showVertices.isSelected()) {
					panel.showVertices(true);
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				} else {
					panel.showVertices(false);
					panel.setPaintImage(); // TODO: move inside panel.xxxx()?
					panel.repaint();
				}
			}
		});
		panel.showVertices(false);
		showVertices.setSelected(false);
		buttonsPanel.add(showVertices);
	}

	public void keyTyped(KeyEvent e) {
      //  Sys(e, "KEY TYPED: ");
    }

    /** Handle the key-pressed event from the text field. */
    public void keyPressed(KeyEvent e) {
		System.out.println(e.getKeyChar());
		if (e.getKeyChar()==' ')
		 System.out.println("yes");
       // displayInfo(e, "KEY PRESSED: ");
    }

    /** Handle the key-released event from the text field. */
    public void keyReleased(KeyEvent e) {
       // displayInfo(e, "KEY RELEASED: ");
    }

	class StyleHandler implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			int style = 0;
			if (idStyles[0].isSelected()) {
				style += Font.BOLD;
			}
			if (idStyles[1].isSelected()) {
				style += Font.ITALIC;
			}
			font = font.deriveFont(style);
			panel.setFont(font);
			panel.repaint();
		}
	}

	// ////////////////////////////////////////////////////////////////
	// ERROR HANDLING
	// ////////////////////////////////////////////////////////////////

	private boolean edgesCheck() {
		if (edges == null) {
			JOptionPane.showMessageDialog(null,
					"You must load the edges first.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	private boolean vertexCheck() {
		if (vertices == null) {
			JOptionPane.showMessageDialog(null,
					"You must load the vertices first.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

}

// ////////////////////////////////////////////////////////////////

class FileSuffixFilter extends javax.swing.filechooser.FileFilter {
	// The suffix should have a '.' at the beggining since
	// that is implicit
	private String suffix;

	public FileSuffixFilter(String s) {
		suffix = s;
	}

	// Accept all directories and all files that match the provided
	// file suffix
	public boolean accept(File f) {

		if (f.isDirectory()) {
			return true;
		}

		String extension = getExtension(f);
		if (extension != null) {
			if (extension.equals(suffix)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void setSuffix(String s) {
		suffix = s;
	}

	public String getSuffix() {
		return suffix;
	}

	// The description of this filter
	public String getDescription() {
		return "Only *" + suffix + " files.";
	}

	public String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}

		return ext;
	}

}

// ////////////////////////////////////////////////////////////////
