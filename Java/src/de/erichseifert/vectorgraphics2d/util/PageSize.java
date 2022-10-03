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

import java.awt.geom.Rectangle2D;

/**
 * <p>Class that represents a page with a specified origin and size.
 * The class is immutable and can be initialized with coordinates and
 * dimensions or only dimensions:</p>
 * <pre>PageSize a3 = new PageSize(0.0, 0.0, 297.0, 420.0);
 *PageSize a4 = new PageSize(210.0, 297.0);</pre>
 *
 * <p>For convenience the class contains static constants for common page
 * sizes:</p>
 * <pre>PageSize a4 = PageSize.A4;
 *PageSize letter = PageSize.LETTER;</pre>
 */
public class PageSize {
	private static final double MM_PER_INCH = 2.54;

	public static final PageSize A3 = new PageSize(297.0, 420.0);
	public static final PageSize A4 = new PageSize(210.0, 297.0);
	public static final PageSize A5 = new PageSize(148.0, 210.0);
	public static final PageSize LETTER = new PageSize(8.5*MM_PER_INCH, 11.0*MM_PER_INCH);
	public static final PageSize LEGAL = new PageSize(8.5*MM_PER_INCH, 14.0*MM_PER_INCH);
	public static final PageSize TABLOID = new PageSize(11.0*MM_PER_INCH, 17.0*MM_PER_INCH);
	public static final PageSize LEDGER = TABLOID.getLandscape();

	private final double x;
	private final double y;
	private final double width;
	private final double height;

	public PageSize(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public PageSize(double width, double height) {
		this(0.0, 0.0, width, height);
	}

	public PageSize(Rectangle2D size) {
		this(size.getX(), size.getY(), size.getWidth(), size.getHeight());
	}

	public PageSize getPortrait() {
		if (width <= height) {
			return this;
		}
		return new PageSize(x, y, height, width);
	}

	public PageSize getLandscape() {
		if (width >= height) {
			return this;
		}
		return new PageSize(x, y, height, width);
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}
}

