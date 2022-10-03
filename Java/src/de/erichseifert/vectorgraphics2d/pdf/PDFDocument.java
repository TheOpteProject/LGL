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
package de.erichseifert.vectorgraphics2d.pdf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.erichseifert.vectorgraphics2d.GraphicsState;
import de.erichseifert.vectorgraphics2d.SizedDocument;
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
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetFontCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetHintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetPaintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetStrokeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetTransformCommand;
import de.erichseifert.vectorgraphics2d.util.DataUtils;
import de.erichseifert.vectorgraphics2d.util.FlateEncodeStream;
import de.erichseifert.vectorgraphics2d.util.FormattingWriter;
import de.erichseifert.vectorgraphics2d.util.GraphicsUtils;
import de.erichseifert.vectorgraphics2d.util.ImageDataStream;
import de.erichseifert.vectorgraphics2d.util.ImageDataStream.Interleaving;
import de.erichseifert.vectorgraphics2d.util.PageSize;

/**
 * Represents a {@code Document} in the <i>Portable Document Format</i> (PDF).
 */
// TODO Support for different image formats (binary, grayscale, etc.)
class PDFDocument extends SizedDocument {
	private static final String CHARSET = "ISO-8859-1";
	private static final String HEADER = "%PDF-1.4";
	private static final String FOOTER = "%%EOF";
	private static final String EOL = "\n";

	/** Constant to convert values from millimeters to PostScript®/PDF units (1/72th inch). */
	private static final double MM_IN_UNITS = 72.0/25.4;

	/** Mapping of stroke endcap values from Java to PDF. */
	private static final Map<Integer, Integer> STROKE_ENDCAPS = DataUtils.map(
		new Integer[] { BasicStroke.CAP_BUTT, BasicStroke.CAP_ROUND, BasicStroke.CAP_SQUARE },
		new Integer[] { 0, 1, 2 }
	);

	/** Mapping of line join values for path drawing from Java to PDF. */
	private static final Map<Integer, Integer> STROKE_LINEJOIN = DataUtils.map(
		new Integer[] { BasicStroke.JOIN_MITER, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_BEVEL },
		new Integer[] { 0, 1, 2 }
	);

	private final List<PDFObject> objects;
	/** Cross-reference table ("xref"). */
	private final Map<PDFObject, Long> crossReferences;

	private final Stream contents;
	private Resources resources;
	private final Map<Integer, PDFObject> images;

	private final Stack<GraphicsState> states;
	private boolean transformed;

	PDFDocument(CommandSequence commands, PageSize pageSize, boolean compressed) {
		super(pageSize, compressed);

		states = new Stack<>();
		states.push(new GraphicsState());

		objects = new LinkedList<>();
		crossReferences = new HashMap<>();
		images = new HashMap<>();

		contents = initPage();
		for (Command<?> command : commands) {
			try {
				byte[] pdfStatement = toBytes(command);
				contents.write(pdfStatement);
				contents.write(EOL.getBytes(CHARSET));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		close();
	}

	private GraphicsState getCurrentState() {
		return states.peek();
	}

	/**
	 * Initializes the document and returns a {@code Stream} representing the contents.
	 * @return {@code Stream} to which the contents are written.
	 */
	private Stream initPage() {
		DefaultPDFObject catalog = addCatalog();

		List<PDFObject> pagesKids = new LinkedList<>();
		PDFObject pageTree = addPageTree(catalog, pagesKids);

		// Page
		DefaultPDFObject page = addPage(pageTree);
		pagesKids.add(page);

		// Contents
		Stream.Filter[] filters = isCompressed() ? new Stream.Filter[] {Stream.Filter.FLATE} : new Stream.Filter[0];
		Stream contents = new Stream(filters);
		objects.add(contents);
		page.dict.put("Contents", contents);

		// Initial content
		try {
			double scaleH = MM_IN_UNITS;
			double scaleV = -MM_IN_UNITS;

			PageSize pageSize = getPageSize();
			double translateX = -pageSize.getX()*MM_IN_UNITS;
			double translateY = (pageSize.getY() + pageSize.getHeight())*MM_IN_UNITS;

			FormattingWriter string = new FormattingWriter(contents, CHARSET, EOL);
			string.writeln("q");
			string.writeln(getOutput(getCurrentState().getColor()));
			string.write(scaleH).write(" ")
					.write(0.0).write(" ")
					.write(0.0).write(" ")
					.write(scaleV).write(" ")
					.write(translateX).write(" ")
					.write(translateY).writeln(" cm");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Resources
		resources = new Resources();
		objects.add(resources);
		page.dict.put("Resources", resources);

		// Create initial font
		Font font = getCurrentState().getFont();
		String fontResourceId = resources.getId(font);
		float fontSize = font.getSize2D();
		setFont(fontResourceId, fontSize, contents);
		return contents;
	}

	private void setFont(String fontId, float fontSize, Stream contents) {
		try {
			FormattingWriter string = new FormattingWriter(contents, CHARSET, EOL);
			string.write("/").write(fontId).write(" ").write(fontSize).writeln(" Tf");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DefaultPDFObject addObject(Map<String, Object> dict, Payload payload) {
		DefaultPDFObject object = new DefaultPDFObject(dict, payload, true);
		objects.add(object);
		return object;
	}

	private DefaultPDFObject addCatalog() {
		Map<String, Object> dict = DataUtils.map(
			new String[] {"Type"},
			new Object[] {"Catalog"}
		);
		return addDictionary(dict);
	}

	private PDFObject addPageTree(DefaultPDFObject catalog, List<PDFObject> pages) {
		Map<String, Object> dict = DataUtils.map(
			new String[] {"Type", "Kids", "Count"},
			new Object[] {"Pages", pages, 1}
		);
		PDFObject pageTree = addDictionary(dict);
		catalog.dict.put("Pages", pageTree);
		return pageTree;
	}

	private DefaultPDFObject addPage(PDFObject pageTree) {
		double x = 0.0;
		double y = 0.0;
		double width = getPageSize().getWidth()*MM_IN_UNITS;
		double height = getPageSize().getHeight()*MM_IN_UNITS;
		Map<String, Object> dict = DataUtils.map(
			new String[] {"Type", "Parent", "MediaBox"},
			new Object[] {"Page", pageTree, new double[] {x, y, width, height}}
		);
		return addDictionary(dict);
	}

	private DefaultPDFObject addDictionary(Map<String, Object> dict) {
		DefaultPDFObject object = new DefaultPDFObject(dict, null, false);
		objects.add(object);
		return object;
	}

	private DefaultPDFObject addObject(Image image) {
		BufferedImage bufferedImage = GraphicsUtils.toBufferedImage(image);

		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		int bitsPerSample = DataUtils.max(bufferedImage.getSampleModel().getSampleSize());
		int bands = bufferedImage.getSampleModel().getNumBands();
		String colorSpaceName = (bands == 1) ? "DeviceGray" : "DeviceRGB";

		Payload imagePayload = new Payload();

		// Compression
		String[] imageFilters = {};
		if (isCompressed()) {
			imagePayload.addFilter(FlateEncodeStream.class);
			imageFilters = new String[] {"FlateDecode"};
		}

		InputStream imageDataStream =
				new ImageDataStream(bufferedImage, Interleaving.WITHOUT_ALPHA);

		try {
			DataUtils.transfer(imageDataStream, imagePayload, 1024);
			imagePayload.close();
		} catch (IOException e) {
			// TODO Improve exception handling
			throw new RuntimeException(e);
		}

		int length = imagePayload.getBytes().length;

		Map<String, Object> imageDict = DataUtils.map(
			new String[] {"Type", "Subtype", "Width", "Height", "ColorSpace",
					"BitsPerComponent", "Length", "Filter"},
			new Object[] {"XObject", "Image", width, height, colorSpaceName,
					bitsPerSample, length, imageFilters}
		);

		DefaultPDFObject imageObject = addObject(imageDict, imagePayload);

		boolean hasAlpha = bufferedImage.getColorModel().hasAlpha();
		if (hasAlpha) {
			BufferedImage mask = GraphicsUtils.getAlphaImage(bufferedImage);

			DefaultPDFObject maskObject = addObject(mask);

			boolean isBitmask = mask.getSampleModel().getSampleSize(0) == 1;
			if (isBitmask) {
				maskObject.dict.put("ImageMask", true);
				maskObject.dict.remove("ColorSpace");
				imageObject.dict.put("Mask", maskObject);
			} else {
				imageObject.dict.put("SMask", maskObject);
			}
		}

		return imageObject;
	}

	public void writeTo(OutputStream out) throws IOException {
		FormattingWriter o = new FormattingWriter(out, CHARSET, EOL);

		o.writeln(HEADER);

		for (PDFObject obj : objects) {
			crossReferences.put(obj, o.tell());
			byte[] objectString;
			if (obj instanceof Resources) {
				objectString = toBytes((Resources) obj);
			} else if (obj instanceof Stream) {
				objectString = toBytes((Stream) obj);
			} else {
				objectString = toBytes(obj);
			}
			o.writeln(objectString);
			o.flush();
		}

		long xrefPos = o.tell();
		o.writeln("xref");
		o.write(0).write(" ").writeln(objects.size() + 1);
		o.writeln("%010d %05d f ", 0, 65535);
		for (PDFObject obj : objects) {
			o.writeln("%010d %05d n ", crossReferences.get(obj), 0);
		}
		o.flush();

		o.writeln("trailer");
		o.writeln(serialize(DataUtils.map(
			new String[] {"Size", "Root"},
			new Object[] {objects.size() + 1, objects.get(0)}
		)));

		o.writeln("startxref");
		o.writeln(xrefPos);

		o.writeln(FOOTER);
		o.flush();
	}

	private int getId(PDFObject object) {
		int index = objects.indexOf(object);
		if (index < 0) {
			throw new IllegalArgumentException("Object " + object + " is not part of this document.");
		}
		return index + 1;
	}

	/**
	 * Returns the version of the specified object.
	 * @param object {@code PDFObject} whose version should be determined.
	 * @return Version number.
	 */
	private int getVersion(PDFObject object) {
		return 0;
	}

	private byte[] toBytes(Resources resources) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			string.write(getId(resources)).write(" ").write(getVersion(resources)).writeln(" obj");
			string.writeln("<<");
			if (!resources.getProcSet().isEmpty()) {
				string.write("/ProcSet ").writeln(serialize(resources.getProcSet()));
			}
			if (!resources.getFont().isEmpty()) {
				string.write("/Font ").writeln(serialize(resources.getFont()));
			}
			if (resources.dict.get("ExtGState") != null) {
				string.write("/ExtGState ").writeln(serialize(resources.dict.get("ExtGState")));
			}
			if (resources.dict.get("XObject") != null) {
				string.write("/XObject ").writeln(serialize(resources.dict.get("XObject")));
			}
			string.writeln(">>");
			string.write("endobj");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] toBytes(Stream stream) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			string.write(getId(stream)).write(" ").write(getVersion(stream)).writeln(" obj");
			string.writeln(serialize(stream));
			string.write("endobj");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	protected static byte[] serialize(Stream stream) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter serialized = new FormattingWriter(out, CHARSET, EOL)) {
			serialized.writeln("<<");
			serialized.write("/Length ").writeln(stream.getLength());
			if (stream.getFilters().contains(Stream.Filter.FLATE)) {
				serialized.writeln("/Filter /FlateDecode");
			}
			serialized.writeln(">>");
			serialized.writeln("stream");
			serialized.writeln(stream.getContent());
			serialized.write("endstream");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	protected static byte[] serialize(TrueTypeFont font) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter serialized = new FormattingWriter(out, CHARSET, EOL)) {
			serialized.writeln("<<");
			serialized.write("/Type /").writeln(font.getType());
			serialized.write("/Subtype /").writeln(font.getSubtype());
			serialized.write("/Encoding /").writeln(font.getEncoding());
			serialized.write("/BaseFont /").writeln(font.getBaseFont());
			serialized.write(">>");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] toBytes(PDFObject object) {
		DefaultPDFObject obj = (DefaultPDFObject) object;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			string.write(getId(obj)).write(" ").write(getVersion(obj)).writeln(" obj");
			if (!obj.dict.isEmpty()) {
				string.writeln(serialize(obj.dict));
			}
			if (obj.payload != null && !obj.payload.isEmpty()) {
				if (obj.stream) {
					string.writeln("stream");
				}
				string.writeln(obj.payload.getBytes());
				if (obj.stream) {
					string.writeln("endstream");
				}
			}
			string.write("endobj");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] serialize(Object obj) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter serialized = new FormattingWriter(out, CHARSET, EOL)) {
			if (obj instanceof String) {
				serialized.write("/").write(obj.toString());
			} else if (obj instanceof float[]) {
				serialized.write(serialize(DataUtils.asList((float[]) obj)));
			} else if (obj instanceof double[]) {
				serialized.write(serialize(DataUtils.asList((double[]) obj)));
			} else if (obj instanceof Object[]) {
				serialized.write(serialize(Arrays.asList((Object[]) obj)));
			} else if (obj instanceof List) {
				List<?> list = (List<?>) obj;
				serialized.write("[");
				int i = 0;
				for (Object elem : list) {
					if (i++ > 0) {
						serialized.write(" ");
					}
					serialized.write(serialize(elem));
				}
				serialized.write("]");
			} else if (obj instanceof Map) {
				Map<?, ?> dict = (Map<?, ?>) obj;
				serialized.writeln("<<");
				for (Map.Entry<?, ?> entry : dict.entrySet()) {
					String key = entry.getKey().toString();
					Object value = entry.getValue();
					serialized.write(serialize(key)).write(" ").writeln(serialize(value));
				}
				serialized.write(">>");
			} else if (obj instanceof TrueTypeFont) {
				serialized.write(serialize((TrueTypeFont) obj));
			} else if (obj instanceof PDFObject) {
				PDFObject pdfObj = (PDFObject) obj;
				serialized.write(getId(pdfObj)).write(" ").write(getVersion(pdfObj)).write(" R");
			} else {
				serialized.write(DataUtils.format(obj));
			}
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] toBytes(Command<?> command) {
		byte[] s = {};
		if (command instanceof Group) {
			Group c = (Group) command;
			applyStateCommands(c.getValue());
			s = getOutput(getCurrentState(), resources, !transformed);
			transformed = true;
		} else if (command instanceof DrawShapeCommand) {
			DrawShapeCommand c = (DrawShapeCommand) command;
			try (ByteArrayOutputStream ba = new ByteArrayOutputStream()) {
				ba.write(getOutput(c.getValue()));
				ba.write(serialize(" S"));
				s = ba.toByteArray();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		} else if (command instanceof FillShapeCommand) {
			FillShapeCommand c = (FillShapeCommand) command;
			String fillMethod = " f";
			Shape shape = c.getValue();
			if (shape instanceof Path2D && ((Path2D) shape).getWindingRule() == Path2D.WIND_EVEN_ODD) {
				fillMethod = " f*";
			}
			try (ByteArrayOutputStream ba = new ByteArrayOutputStream()) {
				ba.write(getOutput(c.getValue()));
				ba.write(serialize(fillMethod));
				s = ba.toByteArray();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		} else if (command instanceof DrawStringCommand) {
			DrawStringCommand c = (DrawStringCommand) command;
			s = getOutput(c.getValue(), c.getX(), c.getY());
		} else if (command instanceof DrawImageCommand) {
			DrawImageCommand c = (DrawImageCommand) command;
			// Create object for image data
			Image image = c.getValue();
			PDFObject imageObject = images.get(image.hashCode());
			if (imageObject == null) {
				imageObject = addObject(image);
				images.put(image.hashCode(), imageObject);
			}
			s = getOutput(imageObject, c.getX(), c.getY(),
					c.getWidth(), c.getHeight(), resources);
		}
		return s;
	}

	private void applyStateCommands(List<Command<?>> commands) {
		for (Command<?> command : commands) {
			if (command instanceof SetHintCommand) {
				SetHintCommand c = (SetHintCommand) command;
				getCurrentState().getHints().put(c.getKey(), c.getValue());
			} else if (command instanceof SetBackgroundCommand) {
				SetBackgroundCommand c = (SetBackgroundCommand) command;
				getCurrentState().setBackground(c.getValue());
			} else if (command instanceof SetColorCommand) {
				SetColorCommand c = (SetColorCommand) command;
				getCurrentState().setColor(c.getValue());
			} else if (command instanceof SetPaintCommand) {
				SetPaintCommand c = (SetPaintCommand) command;
				getCurrentState().setPaint(c.getValue());
			} else if (command instanceof SetStrokeCommand) {
				SetStrokeCommand c = (SetStrokeCommand) command;
				getCurrentState().setStroke(c.getValue());
			} else if (command instanceof SetFontCommand) {
				SetFontCommand c = (SetFontCommand) command;
				getCurrentState().setFont(c.getValue());
			} else if (command instanceof SetTransformCommand) {
				throw new UnsupportedOperationException("The PDF format has no means of setting the transformation matrix.");
			} else if (command instanceof AffineTransformCommand) {
				AffineTransformCommand c = (AffineTransformCommand) command;
				AffineTransform stateTransform = getCurrentState().getTransform();
				AffineTransform transformToBeApplied = c.getValue();
				stateTransform.concatenate(transformToBeApplied);
				getCurrentState().setTransform(stateTransform);
			} else if (command instanceof SetClipCommand) {
				SetClipCommand c = (SetClipCommand) command;
				getCurrentState().setClip(c.getValue());
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

	private byte[] getOutput(Color color) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			if (color.getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
				float[] cmyk = color.getComponents(null);
				byte[] c = serialize(cmyk[0]);
				byte[] m = serialize(cmyk[1]);
				byte[] y = serialize(cmyk[2]);
				byte[] k = serialize(cmyk[3]);
				string.write(c).write(" ").write(m).write(" ").write(y).write(" ").write(k).write(" k ");
				string.write(c).write(" ").write(m).write(" ").write(y).write(" ").write(k).write(" K");
			} else {
				byte[] r = serialize(color.getRed()/255.0);
				byte[] g = serialize(color.getGreen()/255.0);
				byte[] b = serialize(color.getBlue()/255.0);
				string.write(r).write(" ").write(g).write(" ").write(b).write(" rg ");
				string.write(r).write(" ").write(g).write(" ").write(b).write(" RG");
			}
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] getOutput(Shape s) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			PathIterator segments = s.getPathIterator(null);
			double[] coordsCur = new double[6];
			double[] pointPrev = new double[2];
			for (int i = 0; !segments.isDone(); i++, segments.next()) {
				if (i > 0) {
					string.write(" ");
				}
				int segmentType = segments.currentSegment(coordsCur);
				switch (segmentType) {
					case PathIterator.SEG_MOVETO:
						string.write(serialize(coordsCur[0])).write(" ")
								.write(serialize(coordsCur[1])).write(" m");
						pointPrev[0] = coordsCur[0];
						pointPrev[1] = coordsCur[1];
						break;
					case PathIterator.SEG_LINETO:
						string.write(serialize(coordsCur[0])).write(" ")
								.write(serialize(coordsCur[1])).write(" l");
						pointPrev[0] = coordsCur[0];
						pointPrev[1] = coordsCur[1];
						break;
					case PathIterator.SEG_CUBICTO:
						string.write(serialize(coordsCur[0])).write(" ")
								.write(serialize(coordsCur[1])).write(" ")
								.write(serialize(coordsCur[2])).write(" ")
								.write(serialize(coordsCur[3])).write(" ")
								.write(serialize(coordsCur[4])).write(" ")
								.write(serialize(coordsCur[5])).write(" c");
						pointPrev[0] = coordsCur[4];
						pointPrev[1] = coordsCur[5];
						break;
					case PathIterator.SEG_QUADTO:
						double x1 = pointPrev[0] + 2.0/3.0*(coordsCur[0] - pointPrev[0]);
						double y1 = pointPrev[1] + 2.0/3.0*(coordsCur[1] - pointPrev[1]);
						double x2 = coordsCur[0] + 1.0/3.0*(coordsCur[2] - coordsCur[0]);
						double y2 = coordsCur[1] + 1.0/3.0*(coordsCur[3] - coordsCur[1]);
						double x3 = coordsCur[2];
						double y3 = coordsCur[3];
						string.write(serialize(x1)).write(" ")
								.write(serialize(y1)).write(" ")
								.write(serialize(x2)).write(" ")
								.write(serialize(y2)).write(" ")
								.write(serialize(x3)).write(" ")
								.write(serialize(y3)).write(" c");
						pointPrev[0] = x3;
						pointPrev[1] = y3;
						break;
					case PathIterator.SEG_CLOSE:
						string.write("h");
						break;
					default:
						throw new IllegalStateException("Unknown path operation.");
				}
			}
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] getOutput(GraphicsState state, Resources resources, boolean first) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			if (!first) {
				string.writeln("Q");
			}
			string.writeln("q");

			if (!state.getColor().equals(GraphicsState.DEFAULT_COLOR)) {
				if (state.getColor().getAlpha() != GraphicsState.DEFAULT_COLOR.getAlpha()) {
					double a = state.getColor().getAlpha()/255.0;
					String resourceId = resources.getId(a);
					string.write("/").write(resourceId).writeln(" gs");
				}
				string.writeln(getOutput(state.getColor()));
			}
			if (!state.getTransform().equals(GraphicsState.DEFAULT_TRANSFORM)) {
				string.write(getOutput(state.getTransform())).writeln(" cm");
			}
			if (!state.getStroke().equals(GraphicsState.DEFAULT_STROKE)) {
				string.writeln(getOutput(state.getStroke()));
			}
			if (state.getClip() != GraphicsState.DEFAULT_CLIP) {
				string.write(getOutput(state.getClip())).writeln(" W n");
			}
			if (!state.getFont().equals(GraphicsState.DEFAULT_FONT)) {
				Font font = state.getFont();
				String fontResourceId = resources.getId(font);
				float fontSize = font.getSize2D();
				string.write("/").write(fontResourceId).write(" ")
						.write(fontSize).writeln(" Tf");
			}
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private byte[] getOutput(Stroke s) {
		if (!(s instanceof BasicStroke)) {
			throw new UnsupportedOperationException("Only BasicStroke objects are supported.");
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			BasicStroke strokeDefault = (BasicStroke) GraphicsState.DEFAULT_STROKE;
			BasicStroke strokeNew = (BasicStroke) s;
			if (strokeNew.getLineWidth() != strokeDefault.getLineWidth()) {
				string.write(strokeNew.getLineWidth()).writeln(" w");
			}
			if (strokeNew.getLineJoin() == BasicStroke.JOIN_MITER && strokeNew.getMiterLimit() != strokeDefault.getMiterLimit()) {
				string.write(strokeNew.getMiterLimit()).writeln(" M");
			}
			if (strokeNew.getLineJoin() != strokeDefault.getLineJoin()) {
				string.write(STROKE_LINEJOIN.get(strokeNew.getLineJoin())).writeln(" j");
			}
			if (strokeNew.getEndCap() != strokeDefault.getEndCap()) {
				string.write(STROKE_ENDCAPS.get(strokeNew.getEndCap())).writeln(" J");
			}
			if (strokeNew.getDashArray() != strokeDefault.getDashArray()) {
				if (strokeNew.getDashArray() != null) {
					string.write(serialize(strokeNew.getDashArray())).write(" ")
							.write(strokeNew.getDashPhase()).writeln(" d");
				} else {
					string.writeln();
					string.writeln("[] 0 d");
				}
			}
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private static byte[] getOutput(AffineTransform transform) {
		double[] matrix = new double[6];
		transform.getMatrix(matrix);
		try {
			return DataUtils.join(" ", matrix).getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	private static byte[] getOutput(String str, double x, double y) {
		// Save current graphics state
		// Undo swapping of y axis
		// Render text
		// Restore previous graphics state

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			string.write("q 1 0 0 -1 ").write(x).write(" ").write(y)
					.write(" cm BT ").write(getOutput(str)).write(" Tj ET Q");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private static byte[] getOutput(String str) {
		// Escape string
		str = str.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll("\t", "\\\\t")
				.replaceAll("\b", "\\\\b")
				.replaceAll("\f", "\\\\f")
				.replaceAll("\\(", "\\\\(")
				.replaceAll("\\)", "\\\\)")
				.replaceAll("[\r\n]", "");

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			string.write("(").write(str).write(")");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private static byte[] getOutput(PDFObject image, double x, double y,
			double width, double height, Resources resources) {
		// Query image resource id
		String resourceId = resources.getId(image);

		// Save graphics state
		// Move image to correct position and scale it to (width, height)
		// Swap y axis
		// Draw image
		// Restore old graphics state

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (FormattingWriter string = new FormattingWriter(out, CHARSET, EOL)) {
			string.write("q ").write(width).write(" 0 0 ").write(height)
					.write(" ").write(x).write(" ").write(y)
					.write(" cm 1 0 0 -1 0 1 cm /").write(resourceId)
					.write(" Do Q");
			return out.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	public void close() {
		try {
			String footer = "Q";
			if (transformed) {
				footer += EOL + "Q";
			}
			contents.write(footer.getBytes(CHARSET));
			contents.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}

