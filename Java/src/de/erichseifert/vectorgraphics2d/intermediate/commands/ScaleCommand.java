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

import java.awt.geom.AffineTransform;
import java.util.Locale;

public class ScaleCommand extends AffineTransformCommand {
	private final double scaleX;
	private final double scaleY;

	public ScaleCommand(double scaleX, double scaleY) {
		super(AffineTransform.getScaleInstance(scaleX, scaleY));
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}

	public double getScaleX() {
		return scaleX;
	}

	public double getScaleY() {
		return scaleY;
	}

	@Override
	public String toString() {
		return String.format((Locale) null,
				"%s[scaleX=%f, scaleY=%f, value=%s]", getClass().getName(),
				getScaleX(), getScaleY(), getValue());
	}
}

