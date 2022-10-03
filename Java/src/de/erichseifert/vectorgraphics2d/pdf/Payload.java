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
import java.lang.reflect.InvocationTargetException;

import de.erichseifert.vectorgraphics2d.util.FlateEncodeStream;

class Payload extends OutputStream {
	private final ByteArrayOutputStream byteStream;
	private OutputStream filteredStream;
	private boolean empty;

	public Payload() {
		byteStream = new ByteArrayOutputStream();
		filteredStream = byteStream;
		empty = true;
	}

	public byte[] getBytes() {
		return byteStream.toByteArray();
	}

	@Override
	public void write(int b) throws IOException {
		filteredStream.write(b);
		empty = false;
	}

	public boolean isEmpty() {
		return empty;
	}

	@Override
	public void close() throws IOException {
		super.close();
		filteredStream.close();
	}

	public void addFilter(Class<FlateEncodeStream> filterClass) {
		if (!empty) {
			throw new IllegalStateException("Cannot add filter after writing to payload.");
		}
		try {
			filteredStream = filterClass.getConstructor(OutputStream.class)
					.newInstance(filteredStream);
		} catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

