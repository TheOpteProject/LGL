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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class FormattingWriter implements Closeable, Flushable, AutoCloseable {
	private final OutputStream out;
	private final String encoding;
	private final byte[] eolBytes;
	private long position;

	public FormattingWriter(OutputStream out, String encoding, String eol) throws UnsupportedEncodingException {
		if (out == null) {
			throw new IllegalArgumentException("Output stream cannot be null.");
		}
		if (eol == null || eol.isEmpty()) {
			throw new IllegalArgumentException("End-of-line string cannot be empty.");
		}
		this.out = out;
		this.encoding = encoding;
		this.eolBytes = eol.getBytes(encoding);
	}

	public FormattingWriter write(byte[] bytes) throws IOException {
		out.write(bytes, 0, bytes.length);
		position += bytes.length;
		return this;
	}

	public FormattingWriter write(String str) throws IOException {
		byte[] bytes = str.getBytes(encoding);
		return write(bytes);
	}

	public FormattingWriter write(String format, Object... args) throws IOException {
		return write(String.format(null, format, args));
	}

	public FormattingWriter write(Number number) throws IOException {
		return write(DataUtils.format(number));
	}

	public FormattingWriter writeln() throws IOException {
		return write(eolBytes);
	}

	public FormattingWriter writeln(byte[] bytes) throws IOException {
		write(bytes);
		return writeln();
	}

	public FormattingWriter writeln(String string) throws IOException {
		write(string);
		return writeln();
	}

	public FormattingWriter writeln(String format, Object... args) throws IOException {
		write(String.format(null, format, args));
		return writeln();
	}

	public FormattingWriter writeln(Number number) throws IOException {
		write(number);
		return writeln();
	}

	public void flush() throws IOException {
		out.flush();
	}

	public void close() throws IOException {
		out.close();
	}

	public long tell() {
		return position;
	}
}
