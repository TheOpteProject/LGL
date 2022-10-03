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
package de.erichseifert.vectorgraphics2d.eps;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.color.ColorSpace;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.erichseifert.vectorgraphics2d.GraphicsState;
import de.erichseifert.vectorgraphics2d.SizedDocument;
import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Command;
import de.erichseifert.vectorgraphics2d.intermediate.commands.CreateCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DisposeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawImageCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawShapeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawStringCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.FillShapeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.RotateCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.ScaleCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetClipCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetColorCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetCompositeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetFontCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetPaintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetStrokeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetTransformCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.ShearCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.TransformCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.TranslateCommand;
import de.erichseifert.vectorgraphics2d.util.ASCII85EncodeStream;
import de.erichseifert.vectorgraphics2d.util.AlphaToMaskOp;
import de.erichseifert.vectorgraphics2d.util.DataUtils;
import de.erichseifert.vectorgraphics2d.util.FlateEncodeStream;
import de.erichseifert.vectorgraphics2d.util.GraphicsUtils;
import de.erichseifert.vectorgraphics2d.util.ImageDataStream;
import de.erichseifert.vectorgraphics2d.util.ImageDataStream.Interleaving;
import de.erichseifert.vectorgraphics2d.util.LineWrapOutputStream;
import de.erichseifert.vectorgraphics2d.util.PageSize;

/**
 * Represents a {@code Document} in the <i>Encapsulated PostScript&reg;</i>
 * (EPS) format.
 */
class EPSDocument extends SizedDocument {
	/** Constant to convert values from millimeters to PostScript® units
	(1/72th inch). */
	private static final double UNITS_PER_MM = 72.0 / 25.4;
	private static final String CHARSET = "ISO-8859-1";
	private static final String EOL = "\n";
	private static final int MAX_LINE_WIDTH = 255;
	private static final Pattern ELEMENT_SEPARATION_PATTERN = Pattern.compile("(.{1," + MAX_LINE_WIDTH + "})(\\s+|$)");

	/** Mapping of stroke endcap values from Java to PostScript®. */
	private static final Map<Integer, Integer> STROKE_ENDCAPS = DataUtils.map(
		new Integer[] { BasicStroke.CAP_BUTT, BasicStroke.CAP_ROUND, BasicStroke.CAP_SQUARE },
		new Integer[] { 0, 1, 2 }
	);

	/** Mapping of line join values for path drawing from Java to
	PostScript®. */
	private static final Map<Integer, Integer> STROKE_LINEJOIN = DataUtils.map(
		new Integer[] { BasicStroke.JOIN_MITER, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_BEVEL },
		new Integer[] { 0, 1, 2 }
	);

	private static final String FONT_LATIN1_SUFFIX = "Lat";

	private final List<String> elements;

	public EPSDocument(CommandSequence commands, PageSize pageSize) {
		super(pageSize, true);
		elements = new LinkedList<>();
		addHeader();
		for (Command<?> command : commands) {
			handle(command);
		}
	}

	private void addHeader() {
		double x=getPageSize().getX()*UNITS_PER_MM,
			y=getPageSize().getY()*UNITS_PER_MM,
			width=getPageSize().getWidth()*UNITS_PER_MM,
			height=getPageSize().getHeight()*UNITS_PER_MM;
		elements.addAll(Arrays.asList(
			"%!PS-Adobe-3.0 EPSF-3.0",
			"%%BoundingBox: " + ((int) Math.floor(x)) + " " + ((int) Math.floor(y)) + " " + ((int) Math.ceil(x + width)) + " " + ((int) Math.ceil(y + height)),
			"%%HiResBoundingBox: " + x + " " + y + " " + (x + width) + " " + (y + height),
			"%%LanguageLevel: 3",
			"%%Pages: 1",
			"%%EndComments",
			"%%Page: 1 1",
			"/M /moveto load def",
			"/L /lineto load def",
			"/C /curveto load def",
			"/Z /closepath load def",
			"/RL /rlineto load def",
			"/rgb /setrgbcolor load def",
			"/cmyk /setcmykcolor load def",
			"/rect { /height exch def /width exch def /y exch def /x exch def x y M width 0 RL 0 height RL width neg 0 RL } bind def",
			"/ellipse { /endangle exch def /startangle exch def /ry exch def /rx exch def /y exch def /x exch def /savematrix matrix currentmatrix def x y translate rx ry scale 0 0 1 startangle endangle arcn savematrix setmatrix } bind def",
			"/imgdict { /datastream exch def /hasdata exch def /decodeScale exch def /bits exch def /bands exch def /imgheight exch def /imgwidth exch def << /ImageType 1 /Width imgwidth /Height imgheight /BitsPerComponent bits /Decode [bands {0 decodeScale} repeat] /ImageMatrix [imgwidth 0 0 imgheight 0 0] hasdata { /DataSource datastream } if >> } bind def",
			"/latinize { /fontName exch def /fontNameNew exch def fontName findfont 0 dict copy begin /Encoding ISOLatin1Encoding def fontNameNew /FontName def currentdict end dup /FID undef fontNameNew exch definefont pop } bind def",
			getOutput(GraphicsState.DEFAULT_FONT),
			"gsave",
			"clipsave",
			"/DeviceRGB setcolorspace",
			"0 " + height + " translate",
			UNITS_PER_MM + " " + (-UNITS_PER_MM) + " scale",
			"/basematrix matrix currentmatrix def"
		));
	}

	public void writeTo(OutputStream out) throws IOException {
		OutputStreamWriter o = new OutputStreamWriter(out, CHARSET);
		for (String element : elements) {
			if (element == null) {
				continue;
			}

			// Write current element in lines of 255 bytes (excluding line terminators)
			// Numbers must not be separated by line breaks or errors will occur
			// TODO: Integrate functionality into LineWrapOutputStream
			Matcher chunkMatcher = ELEMENT_SEPARATION_PATTERN.matcher(element);

			boolean chunkFound = false;
			while (chunkMatcher.find()) {
				chunkFound = true;
				String chunk = chunkMatcher.group();
				o.write(chunk, 0, chunk.length());
				o.append(EOL);
			}
			if (!chunkFound) {
				// TODO: Exception, if no whitespace can be found in the chunk
				System.err.println("Unable to divide eps element into lines: " + element);
			}
		}
		o.append("%%EOF");
		o.flush();
	}

	public void handle(Command<?> command) {
		if (command instanceof SetClipCommand) {
			SetClipCommand c = (SetClipCommand) command;
			Shape clip = c.getValue();
			elements.add("cliprestore");
			if (clip != null) {
				elements.add(getOutput(clip) + " clip");
			}
		} else if (command instanceof SetColorCommand) {
			SetColorCommand c = (SetColorCommand) command;
			elements.add(getOutput(c.getValue()));
		} else if (command instanceof SetCompositeCommand) {
			SetCompositeCommand c = (SetCompositeCommand) command;
			// TODO Implement composite rendering for EPS
			elements.add("% composite not yet implemented: " + c.getValue());
		} else if (command instanceof SetFontCommand) {
			SetFontCommand c = (SetFontCommand) command;
			elements.add(getOutput(c.getValue()));
		} else if (command instanceof SetPaintCommand) {
			SetPaintCommand c = (SetPaintCommand) command;
			// TODO Implement paint rendering for EPS
			elements.add("% paint not yet implemented: " + c.getValue());
		} else if (command instanceof SetStrokeCommand) {
			SetStrokeCommand c = (SetStrokeCommand) command;
			elements.add(getOutput(c.getValue()));
		} else if (command instanceof SetTransformCommand) {
			SetTransformCommand c = (SetTransformCommand) command;
			StringBuilder e = new StringBuilder();
			double[] matrix = new double[6];
			c.getValue().getMatrix(matrix);
			e.append("basematrix setmatrix [")
				.append(DataUtils.join(" ", matrix)).append("] concat");
			elements.add(e.toString());
		} else if (command instanceof RotateCommand) {
			RotateCommand c = (RotateCommand) command;
			StringBuilder e = new StringBuilder();
			double x = c.getCenterX();
			double y = c.getCenterY();
			boolean translated = x != 0.0 || y != 0.0;
			if (translated) {
				e.append(x).append(" ").append(y).append(" translate ");
			}
			e.append(Math.toDegrees(c.getTheta())).append(" rotate");
			if (translated) {
				e.append(" ");
				e.append(-x).append(" ").append(-y).append(" translate");
			}
			elements.add(e.toString());
		} else if (command instanceof ScaleCommand) {
			ScaleCommand c = (ScaleCommand) command;
			elements.add(DataUtils.format(c.getScaleX()) + " " + DataUtils.format(c.getScaleY()) + " scale");
		} else if (command instanceof ShearCommand) {
			ShearCommand c = (ShearCommand) command;
			elements.add("[1 " + DataUtils.format(c.getShearY()) + " " + DataUtils.format(c.getShearX()) + " 1 0 0] concat");
		} else if (command instanceof TransformCommand) {
			TransformCommand c = (TransformCommand) command;
			StringBuilder e = new StringBuilder();
			double[] matrix = new double[6];
			c.getValue().getMatrix(matrix);
			e.append("[").append(DataUtils.join(" ", matrix))
				.append("] concat");
			elements.add(e.toString());
		} else if (command instanceof TranslateCommand) {
			TranslateCommand c = (TranslateCommand) command;
			elements.add(String.valueOf(c.getDeltaX()) + " " + c.getDeltaY() + " translate");
		} else if (command instanceof DrawImageCommand) {
			DrawImageCommand c = (DrawImageCommand) command;
			String e = getOutput(c.getValue(),
					c.getImageWidth(), c.getImageHeight(),
					c.getX(), c.getY(), c.getWidth(), c.getHeight());
			elements.add(e);
		} else if (command instanceof DrawShapeCommand) {
			DrawShapeCommand c = (DrawShapeCommand) command;
			elements.add(getOutput(c.getValue()) + " stroke");
		} else if (command instanceof DrawStringCommand) {
			DrawStringCommand c = (DrawStringCommand) command;
			elements.add(getOutput(c.getValue(), c.getX(), c.getY()));
		} else if (command instanceof FillShapeCommand) {
			FillShapeCommand c = (FillShapeCommand) command;
			String fillMethod = " fill";
			Shape shape = c.getValue();
			if (shape instanceof Path2D && ((Path2D) shape).getWindingRule() == Path2D.WIND_EVEN_ODD) {
				fillMethod = " eofill";
			}
			elements.add(getOutput(c.getValue()) + fillMethod);
		} else if (command instanceof CreateCommand) {
			elements.add("gsave");
		} else if (command instanceof DisposeCommand) {
			elements.add("grestore");
		}
	}

	private static String getOutput(Color color) {
		// TODO Handle transparency
		if (color.getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
			float[] cmyk = color.getComponents(null);
			return String.format((Locale) null, "%f %f %f %f cmyk",
					cmyk[0], cmyk[1], cmyk[2], cmyk[3]);
		} else {
			return String.format((Locale) null, "%f %f %f rgb",
					color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
		}
	}

	private static String getOutput(Shape s) {
		StringBuilder out = new StringBuilder();
		out.append("newpath ");
		if (s instanceof Line2D) {
			Line2D l = (Line2D) s;
			out.append(l.getX1()).append(" ").append(l.getY1()).append(" M ")
				.append(l.getX2()).append(" ").append(l.getY2()).append(" L");
		} else if (s instanceof Rectangle2D) {
			Rectangle2D r = (Rectangle2D) s;
			out.append(r.getX()).append(" ").append(r.getY()).append(" ")
				.append(r.getWidth()).append(" ").append(r.getHeight())
				.append(" rect Z");
		} else if (s instanceof Ellipse2D) {
			Ellipse2D e = (Ellipse2D) s;
			double x = e.getX() + e.getWidth()/2.0;
			double y = e.getY() + e.getHeight()/2.0;
			double rx = e.getWidth()/2.0;
			double ry = e.getHeight()/2.0;
			out.append(x).append(" ").append(y).append(" ")
				.append(rx).append(" ").append(ry).append(" ")
				.append(360.0).append(" ").append(0.0)
				.append(" ellipse Z");
		} else if (s instanceof Arc2D) {
			Arc2D e = (Arc2D) s;
			double x = (e.getX() + e.getWidth()/2.0);
			double y = (e.getY() + e.getHeight()/2.0);
			double rx = e.getWidth()/2.0;
			double ry = e.getHeight()/2.0;
			double startAngle = -e.getAngleStart();
			double endAngle = -(e.getAngleStart() + e.getAngleExtent());
			out.append(x).append(" ").append(y).append(" ")
				.append(rx).append(" ").append(ry).append(" ")
				.append(startAngle).append(" ").append(endAngle)
				.append(" ellipse");
			if (e.getArcType() == Arc2D.CHORD) {
				out.append(" Z");
			} else if (e.getArcType() == Arc2D.PIE) {
				out.append(" ").append(x).append(" ").append(y).append(" L Z");
			}
		} else {
			PathIterator segments = s.getPathIterator(null);
			double[] coordsCur = new double[6];
			double[] pointPrev = new double[2];
			for (int i = 0; !segments.isDone(); i++, segments.next()) {
				if (i > 0) {
					out.append(" ");
				}
				int segmentType = segments.currentSegment(coordsCur);
				switch (segmentType) {
				case PathIterator.SEG_MOVETO:
					out.append(coordsCur[0]).append(" ").append(coordsCur[1])
						.append(" M");
					pointPrev[0] = coordsCur[0];
					pointPrev[1] = coordsCur[1];
					break;
				case PathIterator.SEG_LINETO:
					out.append(coordsCur[0]).append(" ").append(coordsCur[1])
						.append(" L");
					pointPrev[0] = coordsCur[0];
					pointPrev[1] = coordsCur[1];
					break;
				case PathIterator.SEG_CUBICTO:
					out.append(coordsCur[0]).append(" ").append(coordsCur[1])
						.append(" ").append(coordsCur[2]).append(" ")
						.append(coordsCur[3]).append(" ").append(coordsCur[4])
						.append(" ").append(coordsCur[5]).append(" C");
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
					out.append(x1).append(" ").append(y1).append(" ")
						.append(x2).append(" ").append(y2).append(" ")
						.append(x3).append(" ").append(y3).append(" C");
					pointPrev[0] = x3;
					pointPrev[1] = y3;
					break;
				case PathIterator.SEG_CLOSE:
					out.append("Z");
					break;
				default:
					throw new IllegalStateException("Unknown path operation.");
				}
			}
		}
		return out.toString();
	}

	private static String getOutput(Image image, int imageWidth, int imageHeight,
			double x, double y, double width, double height) {
		StringBuilder out = new StringBuilder();

		BufferedImage bufferedImage = GraphicsUtils.toBufferedImage(image);
		int bands = bufferedImage.getSampleModel().getNumBands();
		int bitsPerSample = DataUtils.max(bufferedImage.getSampleModel().getSampleSize());
		bitsPerSample = (int) (Math.ceil(bitsPerSample/8.0)*8.0);
		if (bands > 3) {
			bands = 3;
		}

		out.append("gsave").append(EOL);
		if (x != 0.0 || y != 0.0) {
			out.append(x).append(" ").append(y).append(" translate").append(EOL);
		}
		if (width != 1.0 || height != 1.0) {
			out.append(width).append(" ").append(height).append(" scale").append(EOL);
		}

		int decodeScale = 1;
		if (bufferedImage.getColorModel().hasAlpha()) {
			// TODO Use different InterleaveType (2 or 3) for more efficient compression
			out.append("<< /ImageType 3 /InterleaveType 1 ")
				.append("/MaskDict ")
				.append(imageWidth).append(" ").append(imageHeight).append(" ")
				.append(1).append(" ").append(bitsPerSample).append(" ").append(decodeScale).append(" ")
				.append(false).append(" ").append(0).append(" imgdict ")
				.append("/DataDict ")
				.append(imageWidth).append(" ").append(imageHeight).append(" ")
				.append(bands).append(" ").append(bitsPerSample).append(" ").append(decodeScale).append(" ")
				.append(true).append(" currentfile /ASCII85Decode filter ")
					.append("<< /BitsPerComponent ").append(bitsPerSample).append(" >> ")
					.append("/FlateDecode filter ")
				.append("imgdict ")
				.append(">> image").append(EOL);

			// Convert alpha values to binary mask
			// FIXME Do alpha conversion in a preprocessing step on commands
			bufferedImage = new AlphaToMaskOp(true).filter(bufferedImage, null);
			output(bufferedImage, out);
		} else {
			if (bands == 1) {
				out.append("/DeviceGray setcolorspace").append(EOL);
			}
			if (bufferedImage.getType() == BufferedImage.TYPE_BYTE_BINARY) {
				decodeScale = 255;
			}
			out.append(imageWidth).append(" ").append(imageHeight).append(" ")
				.append(bands).append(" ").append(bitsPerSample).append(" ").append(decodeScale).append(" ")
				.append(true).append(" currentfile /ASCII85Decode filter ")
					.append("<< /BitsPerComponent ").append(bitsPerSample).append(" >> ")
					.append("/FlateDecode filter ")
				.append("imgdict ")
				.append("image").append(EOL);
			output(bufferedImage, out);
		}

		out.append("grestore");
		return out.toString();
	}

	private static void output(BufferedImage image, StringBuilder out) {
		InputStream imageDataStream =
				new ImageDataStream(image, Interleaving.SAMPLE);
		ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
		OutputStream compressionStream = new FlateEncodeStream(
				new ASCII85EncodeStream(
						new LineWrapOutputStream(outBytes, 80)));
		try {
			DataUtils.transfer(imageDataStream, compressionStream, 1024);
			compressionStream.close();
			String compressed = outBytes.toString(CHARSET);
			out.append(compressed).append(EOL);
		} catch (IOException e) {
			// TODO Handle exception
			e.printStackTrace();
		}
	}

	private static String getOutput(String str, double x, double y) {

		return "gsave 1 -1 scale " + x + " " + -y + " M " + getOutput(str) + " show " + "grestore";
	}

	private static StringBuilder getOutput(String str) {
		StringBuilder out = new StringBuilder();

		// Escape text
		str = str.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll("\t", "\\\\t")
				.replaceAll("\b", "\\\\b")
				.replaceAll("\f", "\\\\f")
				.replaceAll("\\(", "\\\\(")
				.replaceAll("\\)", "\\\\)")
				.replaceAll("[\r\n]", "");

		out.append("(").append(str).append(")");

		return out;
	}

	private static String getOutput(Stroke s) {
		StringBuilder out = new StringBuilder();
		if (s instanceof BasicStroke) {
			BasicStroke bs = (BasicStroke) s;
			out.append(bs.getLineWidth()).append(" setlinewidth ")
				.append(STROKE_LINEJOIN.get(bs.getLineJoin())).append(" setlinejoin ")
				.append(STROKE_ENDCAPS.get(bs.getEndCap())).append(" setlinecap ")
				.append("[").append(DataUtils.join(" ", bs.getDashArray())).append("] ")
				.append(bs.getDashPhase()).append(" setdash");
		} else {
			out.append("% Custom strokes aren't supported at the moment");
		}
		return out.toString();
	}

	private static String getOutput(Font font) {
		StringBuilder out = new StringBuilder();
		font = GraphicsUtils.getPhysicalFont(font);
		String fontName = font.getPSName();

		// Convert font to ISO-8859-1 encoding
		String fontNameLatin1 = fontName + FONT_LATIN1_SUFFIX;
		out.append("/").append(fontNameLatin1).append(" ")
			.append("/").append(font.getPSName()).append(" latinize ");

		// Use encoded font
		out.append("/").append(fontNameLatin1).append(" ")
			.append(font.getSize2D()).append(" selectfont");

		return out.toString();
	}
}

