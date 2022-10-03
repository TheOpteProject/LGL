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
package de.erichseifert.vectorgraphics2d.intermediate.commands;

import java.awt.Image;
import java.util.Locale;

public class DrawImageCommand extends Command<Image> {
	private final int imageWidth;
	private final int imageHeight;
	private final double x;
	private final double y;
	private final double width;
	private final double height;

	public DrawImageCommand(Image image, int imageWidth, int imageHeight,
			double x, double y, double width, double height) {
		super(image);
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public int getImageWidth() {
		return imageWidth;
	}
	public int getImageHeight() {
		return imageHeight;
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

	@Override
	public String toString() {
		return String.format((Locale) null,
				"%s[value=%s, imageWidth=%d, imageHeight=%d, x=%f, y=%f, width=%f, height=%f]",
				getClass().getName(), getValue(),
				getImageWidth(), getImageHeight(),
				getX(), getY(), getWidth(), getHeight());
	}
}

