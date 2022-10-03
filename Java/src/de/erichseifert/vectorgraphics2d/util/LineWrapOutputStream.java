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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LineWrapOutputStream extends FilterOutputStream {
	public static final String STANDARD_EOL = "\r\n";

	private final int lineWidth;
	private final byte[] eolBytes;
	private int written;

	public LineWrapOutputStream(OutputStream sink, int lineWidth, String eol) {
		super(sink);
		this.lineWidth = lineWidth;
		this.eolBytes = eol.getBytes();
		if (lineWidth <= 0) {
			throw new IllegalArgumentException("Width must be at least 0.");
		}
	}

	public LineWrapOutputStream(OutputStream sink, int lineWidth) {
		this(sink, lineWidth, STANDARD_EOL);
	}

	@Override
	public void write(int b) throws IOException {
		if (written == lineWidth) {
			out.write(eolBytes);
			written = 0;
		}
		out.write(b);
		written++;
	}
}

