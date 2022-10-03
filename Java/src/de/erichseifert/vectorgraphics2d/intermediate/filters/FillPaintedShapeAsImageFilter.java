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
package de.erichseifert.vectorgraphics2d.intermediate.filters;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Command;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DisposeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawImageCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.FillShapeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetPaintCommand;

public class FillPaintedShapeAsImageFilter extends StreamingFilter {
	private SetPaintCommand lastSetPaintCommand;

	public FillPaintedShapeAsImageFilter(CommandSequence stream) {
		super(stream);
	}

	@Override
	public Command<?> next() {
		Command<?> nextCommand = super.next();

		if (nextCommand instanceof SetPaintCommand) {
			lastSetPaintCommand = (SetPaintCommand) nextCommand;
		} else if (nextCommand instanceof DisposeCommand) {
			lastSetPaintCommand = null;
		}

		return nextCommand;
	}

	private DrawImageCommand getDrawImageCommand(FillShapeCommand shapeCommand, SetPaintCommand paintCommand) {
		Shape shape = shapeCommand.getValue();
		Rectangle2D shapeBounds = shape.getBounds2D();
		double x = shapeBounds.getX();
		double y = shapeBounds.getY();
		double width = shapeBounds.getWidth();
		double height = shapeBounds.getHeight();
		int imageWidth = (int) Math.round(width);
		int imageHeight = (int) Math.round(height);
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D imageGraphics = (Graphics2D) image.getGraphics();
		imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		imageGraphics.scale(imageWidth/width, imageHeight/height);
		imageGraphics.translate(-shapeBounds.getX(), -shapeBounds.getY());
		imageGraphics.setPaint(paintCommand.getValue());
		imageGraphics.fill(shape);
		imageGraphics.dispose();

		return new DrawImageCommand(image, imageWidth, imageHeight, x, y, width, height);
	}

	@Override
	protected List<Command<?>> filter(Command<?> command) {
		if (lastSetPaintCommand != null && command instanceof FillShapeCommand) {
			FillShapeCommand fillShapeCommand = (FillShapeCommand) command;
			DrawImageCommand drawImageCommand = getDrawImageCommand(fillShapeCommand, lastSetPaintCommand);
			return Collections.singletonList(drawImageCommand);
		}

		return Collections.singletonList(command);
	}
}

