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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/**
 * Represents a stream object in the sense of the PDF specification.
 * The {@code Stream} has a defined length.
 */
class Stream extends OutputStream implements PDFObject {
	public enum Filter {
		FLATE
	}

	private final ByteArrayOutputStream data;
	private final List<Filter> filters;
	private OutputStream filteredData;
	private boolean closed;

	/**
	 * Initializes a new {@code Stream}.
	 */
	public Stream(Filter... filters) {
		data = new ByteArrayOutputStream();
		this.filters = new ArrayList<>(filters.length);
		this.filters.addAll(Arrays.asList(filters));

		filteredData = data;
		for (Filter filter : filters) {
			if (filter == Filter.FLATE) {
				filteredData = new DeflaterOutputStream(filteredData);
			}
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (isClosed()) {
			throw new IOException("Unable to write to closed stream.");
		}
		try {
			this.filteredData.write(b);
		} catch (IOException e) {
			throw new RuntimeException("Unable to write to the output stream", e);
		}
	}

	/**
	 * Appends the specified byte array to the {@code Stream}.
	 * @param data Data to be appended.
	 */
	public void write(byte[] data) throws IOException {
		if (isClosed()) {
			throw new IOException("Unable to write to closed stream.");
		}
		try {
			this.filteredData.write(data);
		} catch (IOException e) {
			throw new RuntimeException("Unable to write to the output stream", e);
		}
	}

	/**
	 * Returns the size of the stream contents in bytes.
	 * @return Number of bytes.
	 * @throws IllegalStateException if the stream is still open.
	 */
	public int getLength() {
		if (!isClosed()) {
			throw new IllegalStateException("Unable to determine the length of an open Stream. Close the stream first.");
		}
		return data.size();
	}

	/**
	 * Returns the content that has been written to this {@code Stream}.
	 * @return Stream content.
	 * @throws IllegalStateException if the stream is still open.
	 */
	public byte[] getContent() {
		if (!isClosed()) {
			throw new IllegalStateException("Unable to retrieve the content of an open Stream. Close the stream first.");
		}
		return data.toByteArray();
	}

	private boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		closed = true;
		try {
			filteredData.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Filter> getFilters() {
		return Collections.unmodifiableList(filters);
	}
}

