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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;

import de.erichseifert.vectorgraphics2d.util.GraphicsUtils;

/**
 * Representation of the Graphics2D status at a certain point in time.
 */
public class GraphicsState implements Cloneable {
	/** Default background color. */
	public static final Color DEFAULT_BACKGROUND = Color.BLACK;
	/** Default color. */
	public static final Color DEFAULT_COLOR = Color.WHITE;
	/** Default clipping shape. */
	public static final Shape DEFAULT_CLIP = null;
	/** Default composite mode. */
	public static final Composite DEFAULT_COMPOSITE = AlphaComposite.SrcOver;
	/** Default font. */
	public static final Font DEFAULT_FONT = Font.decode(null);
	/** Default paint. */
	public static final Color DEFAULT_PAINT = DEFAULT_COLOR;
	/** Default stroke. */
	public static final Stroke DEFAULT_STROKE = new BasicStroke();
	/** Default transformation. */
	public static final AffineTransform DEFAULT_TRANSFORM =
			new AffineTransform();
	/** Default XOR mode. */
	public static final Color DEFAULT_XOR_MODE = Color.BLACK;

	/** Rendering hints. */
	private RenderingHints hints;
	/** Current background color. */
	private Color background;
	/** Current foreground color. */
	private Color color;
	/** Shape used for clipping paint operations. */
	private Shape clip;
	/** Method used for compositing. */
	private Composite composite;
	/** Current font. */
	private Font font;
	/** Paint used to fill shapes. */
	private Paint paint;
	/** Stroke used for drawing shapes. */
	private Stroke stroke;
	/** Current transformation matrix. */
	private AffineTransform transform;
	/** XOR mode used for rendering. */
	private Color xorMode;

	public GraphicsState() {
		hints = new RenderingHints(null);
		background = DEFAULT_BACKGROUND;
		color = DEFAULT_COLOR;
		clip = DEFAULT_CLIP;
		composite = DEFAULT_COMPOSITE;
		font = DEFAULT_FONT;
		paint = DEFAULT_PAINT;
		stroke = DEFAULT_STROKE;
		transform = new AffineTransform(DEFAULT_TRANSFORM);
		xorMode = DEFAULT_XOR_MODE;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		GraphicsState clone = (GraphicsState) super.clone();
		clone.hints = (RenderingHints) hints.clone();
		clone.clip = GraphicsUtils.clone(clip);
		clone.transform = new AffineTransform(transform);
		return clone;
	}

	private static Shape transformShape(Shape s, AffineTransform tx) {
		if (s == null) {
			return null;
		}
        if (tx == null || tx.isIdentity()) {
            return GraphicsUtils.clone(s);
        }
		boolean isRectangle = s instanceof Rectangle2D;
		int nonRectlinearTxMask = AffineTransform.TYPE_GENERAL_TRANSFORM |
				AffineTransform.TYPE_GENERAL_ROTATION;
		boolean isRectlinearTx = (tx.getType() & nonRectlinearTxMask) == 0;
		if (isRectangle && isRectlinearTx) {
			Rectangle2D rect = (Rectangle2D) s;
			double[] corners = new double[] {
				rect.getMinX(), rect.getMinY(),
				rect.getMaxX(), rect.getMaxY()
			};
			tx.transform(corners, 0, corners, 0, 2);
			rect = new Rectangle2D.Double();
			rect.setFrameFromDiagonal(corners[0], corners[1], corners[2],
					corners[3]);
			return rect;
		}
		return tx.createTransformedShape(s);
	}

	private static Shape untransformShape(Shape s, AffineTransform tx) {
		if (s == null) {
			return null;
		}
		try {
			AffineTransform inverse = tx.createInverse();
			return transformShape(s, inverse);
	    } catch (NoninvertibleTransformException e) {
			return null;
	    }
	}

	public Shape transformShape(Shape shape) {
		return transformShape(shape, transform);
	}

	public Shape untransformShape(Shape shape) {
		return untransformShape(shape, transform);
	}

	public RenderingHints getHints() {
		return hints;
	}

	public Color getBackground() {
		return background;
	}

	public void setBackground(Color background) {
		this.background = background;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Shape getClip() {
		return untransformShape(clip);
	}

	public void setClip(Shape clip) {
		this.clip = transformShape(clip);
	}

	public Composite getComposite() {
		return composite;
	}

	public void setComposite(Composite composite) {
		this.composite = composite;
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Paint getPaint() {
		return paint;
	}

	public void setPaint(Paint paint) {
		this.paint = paint;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	public AffineTransform getTransform() {
		return new AffineTransform(transform);
	}

	public void setTransform(AffineTransform tx) {
		transform.setTransform(tx);
	}

	public Color getXorMode() {
		return xorMode;
	}

	public void setXorMode(Color xorMode) {
		this.xorMode = xorMode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof GraphicsState)) {
			return false;
		}
		GraphicsState o = (GraphicsState) obj;
		// Compare all attributes
		return !(!hints.equals(o.hints) || !background.equals(o.background) ||
				!color.equals(o.color) || !composite.equals(o.composite) ||
				!font.equals(o.font) || !paint.equals(o.paint) ||
				!stroke.equals(o.stroke) || !transform.equals(o.transform) ||
				!xorMode.equals(o.xorMode) ||
				((clip == null || o.clip == null) && clip != o.clip) ||
				(clip != null && !clip.equals(o.clip)));
	}

	public boolean isDefault() {
		return hints.isEmpty() && background.equals(DEFAULT_BACKGROUND) &&
			color.equals(DEFAULT_COLOR) && composite.equals(DEFAULT_COMPOSITE) &&
			font.equals(DEFAULT_FONT) && paint.equals(DEFAULT_PAINT) &&
			stroke.equals(DEFAULT_STROKE) && transform.equals(DEFAULT_TRANSFORM) &&
			xorMode.equals(DEFAULT_XOR_MODE) && clip == DEFAULT_CLIP;
	}
}

