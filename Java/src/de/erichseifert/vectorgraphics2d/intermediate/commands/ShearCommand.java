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

public class ShearCommand extends AffineTransformCommand {
	private final double shearX;
	private final double shearY;

	public ShearCommand(double shearX, double shearY) {
		super(AffineTransform.getShearInstance(shearX, shearY));
		this.shearX = shearX;
		this.shearY = shearY;
	}

	public double getShearX() {
		return shearX;
	}

	public double getShearY() {
		return shearY;
	}

	@Override
	public String toString() {
		return String.format((Locale) null,
				"%s[shearX=%f, shearY=%f, value=%s]", getClass().getName(),
				getShearX(), getShearY(), getValue());
	}
}

