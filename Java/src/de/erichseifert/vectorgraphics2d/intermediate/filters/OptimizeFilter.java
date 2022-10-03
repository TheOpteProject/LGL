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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.commands.AffineTransformCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Command;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetHintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.StateCommand;

public class OptimizeFilter extends StreamingFilter {
	private final Queue<Command<?>> buffer;

	public OptimizeFilter(CommandSequence stream) {
		super(stream);
		buffer = new LinkedList<>();
	}

	@Override
	public boolean hasNext() {
		return super.hasNext();
	}

	@Override
	public Command<?> next() {
		if (buffer.isEmpty()) {
			return super.next();
		}
		return buffer.poll();
	}

	@Override
	protected List<Command<?>> filter(Command<?> command) {
		if (!isStateChange(command)) {
			return Collections.singletonList(command);
		}
		Iterator<Command<?>> i = buffer.iterator();
		Class<?> cls = command.getClass();
		while (i.hasNext()) {
			if (cls.equals(i.next().getClass())) {
				i.remove();
			}
		}
		buffer.add(command);
		return null;
	}

	private static boolean isStateChange(Command<?> command) {
		return (command instanceof StateCommand) &&
				!(command instanceof AffineTransformCommand) &&
				!(command instanceof SetHintCommand);
	}
}

