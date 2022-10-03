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
package de.erichseifert.vectorgraphics2d;

import de.erichseifert.vectorgraphics2d.eps.EPSProcessor;
import de.erichseifert.vectorgraphics2d.pdf.PDFProcessor;
import de.erichseifert.vectorgraphics2d.svg.SVGProcessor;

/**
 * <p>Utility class that provides simplified access to processors for different
 * file formats. At the moment three implementations of processors are available:
 * {@code "eps"}, {@code "pdf"}, and {@code "svg"}</p>
 * <p>A new processor can be retrieved by calling the {@link #get(String)}
 * method with the format name:</p>
 * <pre>Processor pdfProcessor = Processors.get("pdf");</pre>
 */
public abstract class Processors {
	/**
	 * Default constructor that prevents creation of class.
	 */
	Processors() {
		throw new UnsupportedOperationException();
	}

	public static Processor get(String format) {
		if (format == null) {
			throw new NullPointerException("Format cannot be null.");
		}
		switch (format) {
			case "eps":
				return new EPSProcessor();
			case "pdf":
				return new PDFProcessor(true);
			case "svg":
				return new SVGProcessor();
			default:
				throw new IllegalArgumentException("Unknown format \"" + format + "\"");
		}
	}
}
