/*
 * VectorGraphics2D: Vector export for Java(R) Graphics2D
 *
 * (C) Copyright 2010-2019 Erich Seifert <dev[at]erichseifert.de>,
 * Michael Seifert <mseifert[at]error-reports.org>
 *
 * This file is part of VectorGraphics2D.
 *
 * VectorGraphics2D is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VectorGraphics2D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VectorGraphics2D.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.erichseifert.vectorgraphics2d.svg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.erichseifert.vectorgraphics2d.GraphicsState;
import de.erichseifert.vectorgraphics2d.SizedDocument;
import de.erichseifert.vectorgraphics2d.VectorHints;
import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.commands.AffineTransformCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Command;
import de.erichseifert.vectorgraphics2d.intermediate.commands.CreateCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DisposeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawImageCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawShapeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawStringCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.FillShapeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Group;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetBackgroundCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetClipCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetColorCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetCompositeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetFontCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetHintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetPaintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetStrokeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetTransformCommand;
import de.erichseifert.vectorgraphics2d.util.Base64EncodeStream;
import de.erichseifert.vectorgraphics2d.util.DataUtils;
import de.erichseifert.vectorgraphics2d.util.GraphicsUtils;
import de.erichseifert.vectorgraphics2d.util.PageSize;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

/**
 * Represents a {@code Document} in the <i>Scaled Vector Graphics</i> (SVG)
 * format.
 */
// TODO Implement composite support for SVG (filters?)
// TODO Implement paint support for SVG
class SVGDocument extends SizedDocument {
	private static final String SVG_DOCTYPE_QNAME = "svg";
	private static final String SVG_DOCTYPE_PUBLIC_ID = "-//W3C//DTD SVG 1.1//EN";
	private static final String SVG_DOCTYPE_SYSTEM_ID = "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd";
	private static final String SVG_NAMESPACE_URI = "http://www.w3.org/2000/svg";
	private static final String XLINK_NAMESPACE = "xlink";
	private static final String XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";

	private static final String PREFIX_CLIP = "clip";

	// TODO Resolution settings
	private static final String CHARSET = "UTF-8";

	private final Stack<GraphicsState> states;

	private final Document doc;
	private final Element root;
	private Element group;
	private boolean groupAdded;

	private Element defs;
	private final Map<Integer, Element> clippingPathElements;

	/** Mapping of stroke endcap values from Java to SVG. */
	private static final Map<Integer, String> STROKE_ENDCAPS = DataUtils.map(
		new Integer[] { BasicStroke.CAP_BUTT, BasicStroke.CAP_ROUND, BasicStroke.CAP_SQUARE },
		new String[] { "butt", "round", "square" }
	);

	/** Mapping of line join values for path drawing from Java to SVG. */
	private static final Map<Integer, String> STROKE_LINEJOIN = DataUtils.map(
		new Integer[] { BasicStroke.JOIN_MITER, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_BEVEL },
		new String[] { "miter", "round", "bevel" }
	);

	public SVGDocument(CommandSequence commands, PageSize pageSize) {
		super(pageSize, true);

		states = new Stack<>();
		states.push(new GraphicsState());
		clippingPathElements = new HashMap<>();

		// Prepare DOM
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setValidating(false);
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("Could not create XML builder.");
		}

		// Create XML document with DOCTYPE
		DOMImplementation domImpl = docBuilder.getDOMImplementation();
		DocumentType docType = domImpl.createDocumentType(SVG_DOCTYPE_QNAME, SVG_DOCTYPE_PUBLIC_ID, SVG_DOCTYPE_SYSTEM_ID);
		doc = domImpl.createDocument(SVG_NAMESPACE_URI, "svg", docType);
		doc.setXmlStandalone(false);

		root = doc.getDocumentElement();
		initRoot();

		group = root;
		for (Command<?> command : commands) {
			handle(command);
		}
	}

	private GraphicsState getCurrentState() {
		return states.peek();
	}

	private void initRoot() {
		double x = getPageSize().getX();
		double y = getPageSize().getY();
		double width = getPageSize().getWidth();
		double height = getPageSize().getHeight();

		// Add svg element
		root.setAttribute("xmlns:" + XLINK_NAMESPACE, XLINK_NAMESPACE_URI);
		root.setAttribute("version", "1.1");
		root.setAttribute("x", DataUtils.format(x) + "px");
		root.setAttribute("y", DataUtils.format(y) + "px");
		root.setAttribute("width", DataUtils.format(width) + "px");
		root.setAttribute("height", DataUtils.format(height) + "px");
		root.setAttribute("viewBox", DataUtils.join(" ", new double[] {x, y, width, height}));
	}

	public void writeTo(OutputStream out) throws IOException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		try {
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.ENCODING, CHARSET);
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
					doc.getDoctype().getPublicId());
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
					doc.getDoctype().getSystemId());
			transformer.transform(new DOMSource(doc), new StreamResult(out));
		} catch (TransformerException e) {
			throw new IOException(e.getMessage());
		}
	}

	@Override
	public String toString() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeTo(out);
			return out.toString(CHARSET);
		} catch (IOException e) {
			return "";
		}
	}

	private void newGroup() {
		group = doc.createElement("g");
		groupAdded = false;

		Shape clip = getCurrentState().getClip();
		if (clip != GraphicsState.DEFAULT_CLIP) {
			Element clipElem = getClipElement(clip);
			String ref = "url(#" + clipElem.getAttribute("id") + ")";
			group.setAttribute("clip-path", ref);
		}

		AffineTransform tx = getCurrentState().getTransform();
		if (!GraphicsState.DEFAULT_TRANSFORM.equals(tx)) {
			group.setAttribute("transform", getOutput(tx));
		}
	}

	private Element getClipElement(Shape clip) {
		// Look for existing entries
		Element path = clippingPathElements.get(clip.hashCode());
		if (path != null) {
			return path;
		}

		// Make sure <defs> exists
		if (defs == null) {
			defs = doc.createElement("defs");
			root.insertBefore(defs, root.getFirstChild());
		}

		// Store clipping path in <defs> without styling information
		path = doc.createElement("clipPath");
		path.setAttribute("id", PREFIX_CLIP + clip.hashCode());
		Element shape = getElement(clip);
		shape.removeAttribute("style");
		path.appendChild(shape);
		defs.appendChild(path);

		// Register path
		clippingPathElements.put(clip.hashCode(), path);

		return path;
	}

	private void addToGroup(Element e) {
		group.appendChild(e);
		if (!groupAdded && group != root) {
			root.appendChild(group);
			groupAdded = true;
		}
	}

	public void handle(Command<?> command) {
		if (command instanceof Group) {
			Group c = (Group) command;
			applyStateCommands(c.getValue());
			if (containsGroupCommand(c.getValue())) {
				newGroup();
			}
		} else if (command instanceof DrawImageCommand) {
			DrawImageCommand c = (DrawImageCommand) command;
			Element e = getElement(c.getValue(),
					c.getX(), c.getY(), c.getWidth(), c.getHeight());
			addToGroup(e);
		} else if (command instanceof DrawShapeCommand) {
			DrawShapeCommand c = (DrawShapeCommand) command;
			Element e = getElement(c.getValue());
			e.setAttribute("style", getStyle(false));
			addToGroup(e);
		} else if (command instanceof DrawStringCommand) {
			DrawStringCommand c = (DrawStringCommand) command;
			Element e = getElement(c.getValue(), c.getX(), c.getY());
			e.setAttribute("style", getStyle(getCurrentState().getFont()));
			addToGroup(e);
		} else if (command instanceof FillShapeCommand) {
			FillShapeCommand c = (FillShapeCommand) command;
			Shape shape = c.getValue();
			Element e = getElement(shape);
			if (shape instanceof Path2D) {
				Path2D path = (Path2D) shape;
				e.setAttribute("style", getStyle(true, path.getWindingRule() == Path2D.WIND_NON_ZERO));
			} else {
				e.setAttribute("style", getStyle(true));
			}
			addToGroup(e);
		}
	}

	private void applyStateCommands(List<Command<?>> commands) {
		for (Command<?> command : commands) {
			GraphicsState state = getCurrentState();
			if (command instanceof SetBackgroundCommand) {
				SetBackgroundCommand c = (SetBackgroundCommand) command;
				state.setBackground(c.getValue());
			} else if (command instanceof SetClipCommand) {
				SetClipCommand c = (SetClipCommand) command;
				state.setClip(c.getValue());
			} else if (command instanceof SetColorCommand) {
				SetColorCommand c = (SetColorCommand) command;
				state.setColor(c.getValue());
			} else if (command instanceof SetCompositeCommand) {
				SetCompositeCommand c = (SetCompositeCommand) command;
				state.setComposite(c.getValue());
			} else if (command instanceof SetFontCommand) {
				SetFontCommand c = (SetFontCommand) command;
				state.setFont(c.getValue());
			} else if (command instanceof SetPaintCommand) {
				SetPaintCommand c = (SetPaintCommand) command;
				state.setPaint(c.getValue());
			} else if (command instanceof SetStrokeCommand) {
				SetStrokeCommand c = (SetStrokeCommand) command;
				state.setStroke(c.getValue());
			} else if (command instanceof SetTransformCommand) {
				SetTransformCommand c = (SetTransformCommand) command;
				state.setTransform(c.getValue());
			} else if (command instanceof AffineTransformCommand) {
				AffineTransformCommand c = (AffineTransformCommand) command;
				AffineTransform stateTransform = state.getTransform();
				AffineTransform transformToBeApplied = c.getValue();
				stateTransform.concatenate(transformToBeApplied);
				state.setTransform(stateTransform);
			} else if (command instanceof SetHintCommand) {
				SetHintCommand c = (SetHintCommand) command;
				state.getHints().put(c.getKey(), c.getValue());
			} else if (command instanceof CreateCommand) {
				try {
					states.push((GraphicsState) getCurrentState().clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			} else if (command instanceof DisposeCommand) {
				states.pop();
			}
		}
	}

	private boolean containsGroupCommand(List<Command<?>> commands) {
		for (Command<?> command : commands) {
			if ((command instanceof SetClipCommand) ||
					(command instanceof SetTransformCommand) ||
					(command instanceof AffineTransformCommand)) {
				return true;
			}
		}
		return false;
	}

	private String getStyle(boolean filled) {
		return getStyle(filled, true);
	}
	
	private String getStyle(boolean filled, boolean fillRullNonZero) {
		StringBuilder style = new StringBuilder();

		Color color = getCurrentState().getColor();
		String colorOutput = getOutput(color);
		double opacity = color.getAlpha()/255.0;

		if (filled) {
			appendStyle(style, "fill", colorOutput);
			if (color.getAlpha() < 255) {
				appendStyle(style, "fill-opacity", opacity);
			}
			if (!fillRullNonZero) {
				// nonzero is the default; only need to set the style rule for non-default evenodd winding rule.
				appendStyle(style, "fill-rule", "evenodd");
			}
		} else {
			appendStyle(style, "fill", "none");
		}

		if (!filled) {
			appendStyle(style, "stroke", colorOutput);
			if (color.getAlpha() < 255) {
				appendStyle(style, "stroke-opacity", opacity);
			}
			Stroke stroke = getCurrentState().getStroke();
			if (stroke instanceof BasicStroke) {
				BasicStroke bs = (BasicStroke) stroke;
				if (bs.getLineWidth() != 1f) {
					appendStyle(style, "stroke-width", bs.getLineWidth());
				}
				if (bs.getMiterLimit() != 4f) {
					appendStyle(style, "stroke-miterlimit", bs.getMiterLimit());
				}
				if (bs.getEndCap() != BasicStroke.CAP_BUTT) {
					appendStyle(style, "stroke-linecap", STROKE_ENDCAPS.get(bs.getEndCap()));
				}
				if (bs.getLineJoin() != BasicStroke.JOIN_MITER) {
					appendStyle(style, "stroke-linejoin", STROKE_LINEJOIN.get(bs.getLineJoin()));
				}
				if (bs.getDashArray() != null) {
					appendStyle(style, "stroke-dasharray", DataUtils.join(",", bs.getDashArray()));
					if (bs.getDashPhase() != 0f) {
						appendStyle(style, "stroke-dashoffset", bs.getDashPhase());
					}
				}
			}
		} else {
			appendStyle(style, "stroke", "none");
		}

		return style.toString();
	}

	private String getStyle(Font font) {
		String style = getStyle(true);
		if (!GraphicsState.DEFAULT_FONT.equals(font)) {
			style += getOutput(font);
		}
		return style;
	}

	private static void appendStyle(StringBuilder style, String attribute, Object value) {
		style.append(attribute).append(":")
			.append(DataUtils.format(value)).append(";");
	}

	private static String getOutput(AffineTransform tx) {
		StringBuilder out = new StringBuilder();
		// FIXME: Use tx.getType() to check for transformation components
		if (AffineTransform.getTranslateInstance(tx.getTranslateX(),
				tx.getTranslateY()).equals(tx)) {
			out.append("translate(")
				.append(DataUtils.format(tx.getTranslateX())).append(" ")
				.append(DataUtils.format(tx.getTranslateY())).append(")");
		} else {
			double[] matrix = new double[6];
			tx.getMatrix(matrix);
			out.append("matrix(").append(DataUtils.join(" ", matrix)).append(")");
		}
		return out.toString();
	}

	private static String getOutput(Color color) {
		if (color.getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
			float[] cmyk = color.getComponents(null);
			return String.format((Locale) null,
					"rgb(%d,%d,%d) icc-color(Generic-CMYK-profile,%f,%f,%f,%f)",
					color.getRed(), color.getGreen(), color.getBlue(),
					cmyk[0], cmyk[1], cmyk[2], cmyk[3]);
		} else {
			return String.format((Locale) null, "rgb(%d,%d,%d)",
					color.getRed(), color.getGreen(), color.getBlue());
		}
	}

	private static String getOutput(Shape shape) {
		StringBuilder out = new StringBuilder();
		PathIterator segments = shape.getPathIterator(null);
		double[] coords = new double[6];
		for (int i = 0; !segments.isDone(); i++, segments.next()) {
			if (i > 0) {
				out.append(" ");
			}
			int segmentType = segments.currentSegment(coords);
			switch (segmentType) {
			case PathIterator.SEG_MOVETO:
				out.append("M").append(DataUtils.format(coords[0])).append(",").append(DataUtils.format(coords[1]));
				break;
			case PathIterator.SEG_LINETO:
				out.append("L").append(coords[0]).append(",").append(DataUtils.format(coords[1]));
				break;
			case PathIterator.SEG_CUBICTO:
				out.append("C")
					.append(DataUtils.format(coords[0])).append(",").append(DataUtils.format(coords[1])).append(" ")
					.append(DataUtils.format(coords[2])).append(",").append(DataUtils.format(coords[3])).append(" ")
					.append(DataUtils.format(coords[4])).append(",").append(DataUtils.format(coords[5]));
				break;
			case PathIterator.SEG_QUADTO:
				out.append("Q")
					.append(DataUtils.format(coords[0])).append(",").append(DataUtils.format(coords[1])).append(" ")
					.append(DataUtils.format(coords[2])).append(",").append(DataUtils.format(coords[3]));
				break;
			case PathIterator.SEG_CLOSE:
				out.append("Z");
				break;
			default:
				throw new IllegalStateException("Unknown path operation.");
			}
		}
		return out.toString();
	}

	private static String getOutput(Font font) {
		StringBuilder out = new StringBuilder();
		if (!GraphicsState.DEFAULT_FONT.getFamily().equals(font.getFamily())) {
			String physicalFamily = GraphicsUtils.getPhysicalFont(font).getFamily();
			out.append("font-family:\"").append(physicalFamily).append("\";");
		}
		if (font.getSize2D() != GraphicsState.DEFAULT_FONT.getSize2D()) {
			out.append("font-size:").append(DataUtils.format(font.getSize2D())).append("px;");
		}
		if ((font.getStyle() & Font.ITALIC) != 0) {
			out.append("font-style:italic;");
		}
		if ((font.getStyle() & Font.BOLD) != 0) {
			out.append("font-weight:bold;");
		}
		return out.toString();
	}

	private static String getOutput(Image image, boolean lossyAllowed) {
		BufferedImage bufferedImage = GraphicsUtils.toBufferedImage(image);

		String encoded = encodeImage(bufferedImage, "png");
		if (!GraphicsUtils.usesAlpha(bufferedImage) && lossyAllowed) {
			String encodedLossy = encodeImage(bufferedImage, "jpeg");
			if (encodedLossy.length() > 0 && encodedLossy.length() < encoded.length()) {
				encoded = encodedLossy;
			}
		}

		return encoded;
	}

	private static String encodeImage(BufferedImage bufferedImage, String format) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		Base64EncodeStream encodeStream = new Base64EncodeStream(byteStream);
		try {
			ImageIO.write(bufferedImage, format, encodeStream);
			encodeStream.close();
			String encoded = byteStream.toString("ISO-8859-1");
			return String.format("data:image/%s;base64,%s", format, encoded);
		} catch (IOException e) {
			return "";
		}
	}

	private Element getElement(Shape shape) {
		Element elem;
		if (shape instanceof Line2D) {
			Line2D s = (Line2D) shape;
			elem = doc.createElement("line");
			elem.setAttribute("x1", DataUtils.format(s.getX1()));
			elem.setAttribute("y1", DataUtils.format(s.getY1()));
			elem.setAttribute("x2", DataUtils.format(s.getX2()));
			elem.setAttribute("y2", DataUtils.format(s.getY2()));
		} else if (shape instanceof Rectangle2D) {
			Rectangle2D s = (Rectangle2D) shape;
			elem = doc.createElement("rect");
			elem.setAttribute("x", DataUtils.format(s.getX()));
			elem.setAttribute("y", DataUtils.format(s.getY()));
			elem.setAttribute("width", DataUtils.format(s.getWidth()));
			elem.setAttribute("height", DataUtils.format(s.getHeight()));
		} else if (shape instanceof RoundRectangle2D) {
			RoundRectangle2D s = (RoundRectangle2D) shape;
			elem = doc.createElement("rect");
			elem.setAttribute("x", DataUtils.format(s.getX()));
			elem.setAttribute("y", DataUtils.format(s.getY()));
			elem.setAttribute("width", DataUtils.format(s.getWidth()));
			elem.setAttribute("height", DataUtils.format(s.getHeight()));
			elem.setAttribute("rx", DataUtils.format(s.getArcWidth()/2.0));
			elem.setAttribute("ry", DataUtils.format(s.getArcHeight()/2.0));
		} else if (shape instanceof Ellipse2D) {
			Ellipse2D s = (Ellipse2D) shape;
			elem = doc.createElement("ellipse");
			elem.setAttribute("cx", DataUtils.format(s.getCenterX()));
			elem.setAttribute("cy", DataUtils.format(s.getCenterY()));
			elem.setAttribute("rx", DataUtils.format(s.getWidth()/2.0));
			elem.setAttribute("ry", DataUtils.format(s.getHeight()/2.0));
		} else {
			elem = doc.createElement("path");
			elem.setAttribute("d", getOutput(shape));
		}
		return elem;
	}

	private Element getElement(String text, double x, double y) {
		Element elem = doc.createElement("text");
		elem.appendChild(doc.createTextNode(text));
		elem.setAttribute("x", DataUtils.format(x));
		elem.setAttribute("y", DataUtils.format(y));
		return elem;
	}

	private Element getElement(Image image, double x, double y, double width, double height) {
		Element elem = doc.createElement("image");
		elem.setAttribute("x", DataUtils.format(x));
		elem.setAttribute("y", DataUtils.format(y));
		elem.setAttribute("width", DataUtils.format(width));
		elem.setAttribute("height", DataUtils.format(height));
		elem.setAttribute("preserveAspectRatio", "none");
		boolean lossyAllowed = getCurrentState().getHints().get(VectorHints.KEY_EXPORT) ==
				VectorHints.VALUE_EXPORT_SIZE;
		elem.setAttribute("xlink:href", getOutput(image, lossyAllowed));
		return elem;
	}
}
