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
import java.util.List;

import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Command;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Group;


public abstract class GroupingFilter extends StreamingFilter {
	private Group group;

	public GroupingFilter(CommandSequence stream) {
		super(stream);
	}

	@Override
	public boolean hasNext() {
		return group != null || super.hasNext();
	}

	@Override
	public Command<?> next() {
		if (group == null) {
			return super.next();
		}
		Group g = group;
		group = null;
		return g;
	}

	@Override
	protected List<Command<?>> filter(Command<?> command) {
		boolean grouped = isGrouped(command);
		if (grouped) {
			if (group == null) {
				group = new Group();
			}
			group.add(command);
			return null;
		}
		return Collections.singletonList(command);
	}

	protected abstract boolean isGrouped(Command<?> command);
}

