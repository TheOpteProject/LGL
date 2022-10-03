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

public class RotateCommand extends AffineTransformCommand {
	private final double theta;
	private final double centerX;
	private final double centerY;

	public RotateCommand(double theta, double centerX, double centerY) {
		super(AffineTransform.getRotateInstance(theta, centerX, centerY));
		this.theta = theta;
		this.centerX = centerX;
		this.centerY = centerY;
	}

	public double getTheta() {
		return theta;
	}

	public double getCenterX() {
		return centerX;
	}

	public double getCenterY() {
		return centerY;
	}

	@Override
	public String toString() {
		return String.format((Locale) null,
				"%s[theta=%f, centerX=%f, centerY=%f, value=%s]",
				getClass().getName(), getTheta(), getCenterX(), getCenterY(),
				getValue());
	}
}

