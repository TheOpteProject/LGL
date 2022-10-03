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
import java.util.Arrays;

public class Base64EncodeStream extends FilterOutputStream {
	private static final int BASE = 64;
	private static final int[] POW_64 =
		{ BASE*BASE*BASE, BASE*BASE, BASE, 1 };
	private static final char[] CHAR_MAP =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
			.toCharArray();

	private boolean closed;

	private final byte[] data;
    private int dataSize;

	private final byte[] encoded;

	public Base64EncodeStream(OutputStream out) {
		super(out);
		data = new byte[3];
		encoded = new byte[4];
	}

	@Override
	public void write(int b) throws IOException {
		if (closed) {
			return;
		}
		if (dataSize == data.length) {
	        writeChunk();
			dataSize = 0;
		}
		data[dataSize++] = (byte) (b & 0xff);
	}

	private void writeChunk() throws IOException {
		if (dataSize == 0) {
			return;
		}
	    long uint32 = toUInt32(data, dataSize);
	    int padByteCount = data.length - dataSize;
	    int encodedSize = encodeChunk(uint32, padByteCount);
		out.write(encoded, 0, encodedSize);
	}

	private static long toUInt32(byte[] bytes, int size) {
		long uint32 = 0L;
		int offset = (3 - size)*8;
	    for (int i = size - 1; i >= 0; i--) {
	        uint32 |= (bytes[i] & 0xff) << offset;
	        offset += 8;
	    }
	    return toUnsignedInt(uint32);
	}

	private static long toUnsignedInt(long x) {
	    return x & 0x00000000ffffffffL;
	}

	private int encodeChunk(long uint32, int padByteCount) {
		Arrays.fill(encoded, (byte) '=');
	    int size = encoded.length - padByteCount;
	    for (int i = 0; i < size; i++) {
	        encoded[i] = (byte) CHAR_MAP[(int) (uint32/POW_64[i]%BASE)];
	    }
	    return encoded.length;
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}

		writeChunk();

		super.close();
		closed = true;
	}
}

