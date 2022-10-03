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
package de.erichseifert.vectorgraphics2d.pdf;

import de.erichseifert.vectorgraphics2d.Document;
import de.erichseifert.vectorgraphics2d.Processor;
import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.filters.AbsoluteToRelativeTransformsFilter;
import de.erichseifert.vectorgraphics2d.intermediate.filters.FillPaintedShapeAsImageFilter;
import de.erichseifert.vectorgraphics2d.intermediate.filters.StateChangeGroupingFilter;
import de.erichseifert.vectorgraphics2d.util.PageSize;

/**
 * {@code Processor} implementation that translates {@link CommandSequence}s to
 * a {@code Document} in the <i>Portable Document Format</i> (PDF).
 */
public class PDFProcessor implements Processor {
	private final boolean compressed;

	/**
	 * Initializes a {@code PDFProcessor} for compressed PDF documents.
	 */
	public PDFProcessor() {
		this(true);
	}

	/**
	 * Initializes a {@code PDFProcessor} with the specified compression settings.
	 * @param compressed {@code true} if compression is enabled, {@code false} otherwise.
	 */
	public PDFProcessor(boolean compressed) {
		this.compressed = compressed;
	}

	/**
	 * Returns whether the current PDF document is compressed.
	 * @return {@code true} if the document is compressed, {@code false} otherwise.
	 */
	public boolean isCompressed() {
		return compressed;
	}

	@Override
	public Document getDocument(CommandSequence commands, PageSize pageSize) {
		AbsoluteToRelativeTransformsFilter absoluteToRelativeTransformsFilter = new AbsoluteToRelativeTransformsFilter(commands);
		FillPaintedShapeAsImageFilter paintedShapeAsImageFilter = new FillPaintedShapeAsImageFilter(absoluteToRelativeTransformsFilter);
		CommandSequence filtered = new StateChangeGroupingFilter(paintedShapeAsImageFilter);
		return new PDFDocument(filtered, pageSize, isCompressed());
	}
}
