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

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

public class AlphaToMaskOp implements BufferedImageOp {
	private final boolean inverted;

	public AlphaToMaskOp(boolean inverted) {
		this.inverted = inverted;
	}

	public AlphaToMaskOp() {
		this(false);
	}

	public boolean isInverted() {
		return inverted;
	}

	public BufferedImage filter(BufferedImage src, BufferedImage dest) {
		ColorModel cm = src.getColorModel();

		if (dest == null) {
			dest = createCompatibleDestImage(src, cm);
		} else if (dest.getWidth() != src.getWidth() || dest.getHeight() != src.getHeight()) {
			throw new IllegalArgumentException("Source and destination images have different dimensions.");
		} else if (dest.getColorModel() != cm) {
			throw new IllegalArgumentException("Color models don't match.");
		}

		if (cm.hasAlpha()) {
			Raster srcRaster = src.getRaster();
			WritableRaster destRaster = dest.getRaster();

			for (int y = 0; y < srcRaster.getHeight(); y++) {
				for (int x = 0; x < srcRaster.getWidth(); x++) {
					int argb = cm.getRGB(srcRaster.getDataElements(x, y, null));
					int alpha = argb >>> 24;
					if (alpha >= 127 && !isInverted() || alpha < 127 && isInverted()) {
						argb |= 0xff000000;
					} else {
						argb &= 0x00ffffff;
					}
					destRaster.setDataElements(x, y, cm.getDataElements(argb, null));
				}
			}
		}

		return dest;
	}

	public Rectangle2D getBounds2D(BufferedImage src) {
		Rectangle2D bounds = new Rectangle2D.Double();
		bounds.setRect(src.getRaster().getBounds());
		return bounds;
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src,
			ColorModel destCM) {
		if (destCM == null) {
			destCM = src.getColorModel();
		}
		WritableRaster raster = destCM.createCompatibleWritableRaster(
				src.getWidth(), src.getHeight());
		boolean isRasterPremultiplied = destCM.isAlphaPremultiplied();
		Hashtable<String, Object> properties = null;
		if (src.getPropertyNames() != null) {
			properties = new Hashtable<>();
			for (String key : src.getPropertyNames()) {
				properties.put(key, src.getProperty(key));
			}
		}

		BufferedImage bimage = new BufferedImage(destCM, raster,
				isRasterPremultiplied, properties);
		src.copyData(raster);
		return bimage;
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null) {
			dstPt = new Point2D.Double();
		}
		dstPt.setLocation(srcPt);
		return dstPt;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

}

