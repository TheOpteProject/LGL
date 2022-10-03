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

/**
 * Represents a TrueType font in the sense of the PDF specification.
 */
class TrueTypeFont implements PDFObject {
	private final String encoding;
	private final String baseFont;

	/**
	 * Creates a {@code TrueTypeFont} with the specified encoding and base font.
	 * @param encoding Used encoding.
	 * @param baseFont Base font name.
	 */
	public TrueTypeFont(String encoding, String baseFont) {
		this.encoding = encoding;
		this.baseFont = baseFont;
	}

	/**
	 * Returns the encoding of this font.
	 * @return Encoding.
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Returns the type of this object.
	 * Always returns "Font".
	 * @return The String "Font".
	 */
	public String getType() {
		return "Font";
	}

	/**
	 * Returns the subtype of this object.
	 * Always returns "TrueType".
	 * @return The String "TrueType".
	 */
	public String getSubtype() {
		return "TrueType";
	}

	/**
	 * Returns the name of the base font.
	 * @return Base font name.
	 */
	public String getBaseFont() {
		return baseFont;
	}
}

