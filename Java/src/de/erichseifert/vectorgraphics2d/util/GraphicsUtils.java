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
package de.erichseifert.vectorgraphics2d.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import javax.swing.ImageIcon;

/**
 * Abstract class that contains utility functions for working with graphics.
 * For example, this includes font handling.
 */
public abstract class GraphicsUtils {
	private static final FontRenderContext FONT_RENDER_CONTEXT =
		new FontRenderContext(null, false, true);
	private static final String FONT_TEST_STRING =
		"Falsches Üben von Xylophonmusik quält jeden größeren Zwerg";

	/**
	 * Default constructor that prevents creation of class.
	 */
	GraphicsUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * This method returns {@code true} if the specified image has the
	 * possibility to store transparent pixels.
	 * Inspired by http://www.exampledepot.com/egs/java.awt.image/HasAlpha.html
	 * @param image Image that should be checked for alpha channel.
	 * @return {@code true} if the specified image can have transparent pixels,
	 *         {@code false} otherwise
	 */
	public static boolean hasAlpha(Image image) {
		ColorModel cm;
		// If buffered image, the color model is readily available
		if (image instanceof BufferedImage) {
			BufferedImage bimage = (BufferedImage) image;
			cm = bimage.getColorModel();
		} else {
			// Use a pixel grabber to retrieve the image's color model;
			// grabbing a single pixel is usually sufficient
			PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
			try {
				pg.grabPixels();
			} catch (InterruptedException e) {
				return false;
			}
			// Get the image's color model
			cm = pg.getColorModel();
		}
		return cm.hasAlpha();
	}

	/**
	 * This method returns {@code true} if the specified image has at least one
	 * pixel that is not fully opaque.
	 * @param image Image that should be checked for non-opaque pixels.
	 * @return {@code true} if the specified image has transparent pixels,
	 *         {@code false} otherwise
	 */
	public static boolean usesAlpha(Image image) {
		if (image == null) {
			return false;
		}
		BufferedImage bimage = toBufferedImage(image);
		Raster alphaRaster = bimage.getAlphaRaster();
		if (alphaRaster == null) {
			return false;
		}
		DataBuffer dataBuffer = alphaRaster.getDataBuffer();
		final int elemBits = DataBuffer.getDataTypeSize(dataBuffer.getDataType());
		final int alphaBits = elemBits/bimage.getRaster().getNumBands();
		final int alphaShift = (elemBits - alphaBits);
		for (int i = 0; i < dataBuffer.getSize(); i++) {
			int alpha = dataBuffer.getElem(i) >>> alphaShift;
			if (alpha < 255) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts a rendered image instance to a {@code BufferedImage}.
	 * @param image Rendered image that should be converted.
	 * @return a buffered image containing the image pixels, or the original
	 *         instance if the image already was of type {@code BufferedImage}.
	 */
	public static BufferedImage toBufferedImage(RenderedImage image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		ColorModel cm = image.getColorModel();
		WritableRaster raster = cm.createCompatibleWritableRaster(
				image.getWidth(), image.getHeight());
		boolean isRasterPremultiplied = cm.isAlphaPremultiplied();
		Hashtable<String, Object> properties = null;
		if (image.getPropertyNames() != null) {
			properties = new Hashtable<>();
			for (String key : image.getPropertyNames()) {
				properties.put(key, image.getProperty(key));
			}
		}

		BufferedImage bimage = new BufferedImage(cm, raster,
				isRasterPremultiplied, properties);
		image.copyData(raster);
		return bimage;
	}

	/**
	 * This method returns a buffered image with the contents of an image.
	 * Taken from http://www.exampledepot.com/egs/java.awt.image/Image2Buf.html
	 * @param image Image to be converted
	 * @return a buffered image with the contents of the specified image
	 */
	public static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}
		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon(image).getImage();
		// Determine if the image has transparent pixels
		boolean hasAlpha = hasAlpha(image);

		// Create a buffered image with a format that's compatible with the
		// screen
		BufferedImage bimage;
		GraphicsEnvironment ge = GraphicsEnvironment
			.getLocalGraphicsEnvironment();
		try {
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.TRANSLUCENT;
			}
			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(
					image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException e) {
			// The system does not have a screen
			bimage = null;
		}
		if (bimage == null) {
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(
					image.getWidth(null), image.getHeight(null), type);
		}
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bimage;
	}

	public static Shape clone(Shape shape) {
		if (shape == null) {
			return null;
		}
		Shape clone;
		if (shape instanceof Line2D) {
			clone = (shape instanceof Line2D.Float) ?
					new Line2D.Float() : new Line2D.Double();
			((Line2D) clone).setLine((Line2D) shape);
		} else if (shape instanceof Rectangle) {
			clone = new Rectangle((Rectangle) shape);
		} else if (shape instanceof Rectangle2D) {
			clone = (shape instanceof Rectangle2D.Float) ?
					new Rectangle2D.Float() : new Rectangle2D.Double();
			((Rectangle2D) clone).setRect((Rectangle2D) shape);
		} else if (shape instanceof RoundRectangle2D) {
			clone = (shape instanceof RoundRectangle2D.Float) ?
					new RoundRectangle2D.Float() : new RoundRectangle2D.Double();
			((RoundRectangle2D) clone).setRoundRect((RoundRectangle2D) shape);
		} else if (shape instanceof Ellipse2D) {
			clone = (shape instanceof Ellipse2D.Float) ?
					new Ellipse2D.Float() : new Ellipse2D.Double();
			((Ellipse2D) clone).setFrame(((Ellipse2D) shape).getFrame());
		} else if (shape instanceof Arc2D) {
			clone = (shape instanceof Arc2D.Float) ?
					new Arc2D.Float() : new Arc2D.Double();
			((Arc2D) clone).setArc((Arc2D) shape);
		} else if (shape instanceof Polygon) {
			Polygon p = (Polygon) shape;
			clone = new Polygon(p.xpoints, p.ypoints, p.npoints);
		} else if (shape instanceof CubicCurve2D) {
			clone = (shape instanceof CubicCurve2D.Float) ?
					new CubicCurve2D.Float() : new CubicCurve2D.Double();
			((CubicCurve2D) clone).setCurve((CubicCurve2D) shape);
		} else if (shape instanceof QuadCurve2D) {
			clone = (shape instanceof QuadCurve2D.Float) ?
					new QuadCurve2D.Float() : new QuadCurve2D.Double();
			((QuadCurve2D) clone).setCurve((QuadCurve2D) shape);
		} else if (shape instanceof Path2D.Float) {
			clone = new Path2D.Float(shape);
		} else {
			clone = new Path2D.Double(shape);
		}
		return clone;
	}

	private static class FontExpressivenessComparator implements Comparator<Font> {
		private static final int[] STYLES = {
			Font.PLAIN, Font.ITALIC, Font.BOLD, Font.BOLD | Font.ITALIC
		};
		public int compare(Font font1, Font font2) {
			if (font1 == font2) {
				return 0;
			}
			Set<String> variantNames1 = new HashSet<>();
			Set<String> variantNames2 = new HashSet<>();
			for (int style : STYLES) {
				variantNames1.add(font1.deriveFont(style).getPSName());
				variantNames2.add(font2.deriveFont(style).getPSName());
			}
			if (variantNames1.size() < variantNames2.size()) {
				return 1;
			} else if (variantNames1.size() > variantNames2.size()) {
				return -1;
			}
			return font1.getName().compareTo(font2.getName());
		}
	}

	private static final FontExpressivenessComparator FONT_EXPRESSIVENESS_COMPARATOR =
			new FontExpressivenessComparator();

	private static boolean isLogicalFontFamily(String family) {
		return (Font.DIALOG.equals(family) ||
				Font.DIALOG_INPUT.equals(family) ||
				Font.SANS_SERIF.equals(family) ||
				Font.SERIF.equals(family) ||
				Font.MONOSPACED.equals(family));
	}

	/**
	 * Try to guess physical font from the properties of a logical font, like
	 * "Dialog", "Serif", "Monospaced" etc.
	 * @param logicalFont Logical font object.
	 * @param testText Text used to determine font properties.
	 * @return An object of the first matching physical font. The original font
	 * object is returned if it was a physical font or no font matched.
	 */
	public static Font getPhysicalFont(Font logicalFont, String testText) {
		String logicalFamily = logicalFont.getFamily();
		if (!isLogicalFontFamily(logicalFamily)) {
			return logicalFont;
		}

		final TextLayout logicalLayout =
			new TextLayout(testText, logicalFont, FONT_RENDER_CONTEXT);

		// Create a list of matches sorted by font expressiveness (in descending order)
		Queue<Font> physicalFonts =
				new PriorityQueue<>(1, FONT_EXPRESSIVENESS_COMPARATOR);

		Font[] allPhysicalFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for (Font physicalFont : allPhysicalFonts) {
			String physicalFamily = physicalFont.getFamily();
			// Skip logical fonts
			if (isLogicalFontFamily(physicalFamily)) {
				continue;
			}

			// Derive identical variant of physical font
			physicalFont = physicalFont.deriveFont(
					logicalFont.getStyle(), logicalFont.getSize2D());
			TextLayout physicalLayout =
					new TextLayout(testText, physicalFont, FONT_RENDER_CONTEXT);

			// Compare various properties of physical and logical font
			if (physicalLayout.getBounds().equals(logicalLayout.getBounds()) &&
					physicalLayout.getAscent() == logicalLayout.getAscent() &&
					physicalLayout.getDescent() == logicalLayout.getDescent() &&
					physicalLayout.getLeading() == logicalLayout.getLeading() &&
					physicalLayout.getAdvance() == logicalLayout.getAdvance() &&
					physicalLayout.getVisibleAdvance() == logicalLayout.getVisibleAdvance()) {
				// Store matching font in list
				physicalFonts.add(physicalFont);
			}
		}

		// Return a valid font even when no matching font could be found
		if (physicalFonts.isEmpty()) {
			return logicalFont;
		}

		return physicalFonts.poll();
	}

	public static Font getPhysicalFont(Font logicalFont) {
		return getPhysicalFont(logicalFont, FONT_TEST_STRING);
	}

	public static BufferedImage getAlphaImage(BufferedImage image) {
		WritableRaster alphaRaster = image.getAlphaRaster();
		int width = image.getWidth();
		int height = image.getHeight();

		if (alphaRaster == null) {
			BufferedImage opaqueAlphaImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			Graphics g = opaqueAlphaImage.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			return opaqueAlphaImage;
		}

		ColorModel cm;
		WritableRaster raster;
		// TODO Handle bitmap masks (work on ImageDataStream is necessary)
		ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		int[] bits = {8};
		cm = new ComponentColorModel(colorSpace, bits, false, true,
				Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		raster = cm.createCompatibleWritableRaster(width, height);

		BufferedImage alphaImage = new BufferedImage(cm, raster, false, null);

		int[] alphaValues = new int[image.getWidth()*alphaRaster.getNumBands()];
		for (int y = 0; y < image.getHeight(); y++) {
			alphaRaster.getPixels(0, y, image.getWidth(), 1, alphaValues);
			// FIXME Don't force 8-bit alpha channel (see TODO above)
			if (image.getTransparency() == BufferedImage.BITMASK) {
				for (int i = 0; i < alphaValues.length; i++) {
					if (alphaValues[i] > 0) {
						alphaValues[i] = 255;
					}
				}
			}
			alphaImage.getRaster().setPixels(0, y, image.getWidth(), 1, alphaValues);
		}

		return alphaImage;
	}

	public static boolean equals(Shape shapeA, Shape shapeB) {
		PathIterator pathAIterator = shapeA.getPathIterator(null);
		PathIterator pathBIterator = shapeB.getPathIterator(null);

		if (pathAIterator.getWindingRule() != pathBIterator.getWindingRule()) {
			return false;
		}
		double[] pathASegment = new double[6];
		double[] pathBSegment = new double[6];
		while (!pathAIterator.isDone()) {
			int pathASegmentType = pathAIterator.currentSegment(pathASegment);
			int pathBSegmentType = pathBIterator.currentSegment(pathBSegment);
			if (pathASegmentType != pathBSegmentType) {
				return false;
			}
			for (int segmentIndex = 0; segmentIndex < pathASegment.length; segmentIndex++) {
				if (pathASegment[segmentIndex] != pathBSegment[segmentIndex]) {
					return false;
				}
			}

			pathAIterator.next();
			pathBIterator.next();
		}
		// When the iterator of shapeA is done and shapeA equals shapeB,
		// the iterator of shapeB must also be done
		if (!pathBIterator.isDone()) {
			return false;
		}
		return true;
	}
}
