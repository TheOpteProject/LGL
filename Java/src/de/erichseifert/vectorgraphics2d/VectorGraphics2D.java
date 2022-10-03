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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.Map.Entry;

import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.MutableCommandSequence;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Command;
import de.erichseifert.vectorgraphics2d.intermediate.commands.CreateCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DisposeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawImageCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawShapeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.DrawStringCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.FillShapeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.RotateCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.ScaleCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetBackgroundCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetClipCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetColorCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetCompositeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetFontCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetHintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetPaintCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetStrokeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetTransformCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.SetXORModeCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.ShearCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.TransformCommand;
import de.erichseifert.vectorgraphics2d.intermediate.commands.TranslateCommand;
import de.erichseifert.vectorgraphics2d.util.GraphicsUtils;

/**
 * <p>Implementation of the Graphics2D interface to Java to generate a sequence of
 * commands. An instance of {@code VectorGraphics2D} can be used to replace any
 * {@code Graphics2D} object. It can be created with its standard constructor:</p>
 * <pre>Graphics2D g = new VectorGraphics2D();</pre>
 *
 * @see <a href="http://www.java2s.com/Code/Java/2D-Graphics-GUI/YourownGraphics2D.htm">http://www.java2s.com/Code/Java/2D-Graphics-GUI/YourownGraphics2D.htm</a>
 */
public class VectorGraphics2D extends Graphics2D implements Cloneable {
	private final MutableCommandSequence commands;
	/** Device configuration settings. */
	private final GraphicsConfiguration deviceConfig;
	/** Context settings used to render fonts. */
	private final FontRenderContext fontRenderContext;
	/** Flag that tells whether this graphics object has been disposed. */
	private boolean disposed;

	private GraphicsState state;

	public VectorGraphics2D() {
		this.commands = new MutableCommandSequence();
		emit(new CreateCommand(this));
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		if (!graphicsEnvironment.isHeadlessInstance()) {
			GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
			deviceConfig = graphicsDevice.getDefaultConfiguration();
		} else {
			deviceConfig = null;
		}
		fontRenderContext = new FontRenderContext(null, false, true);

		state = new GraphicsState();

		// Ensure that document state matches default state of Graphics2D
		// TODO: Default graphics state does not need to be printed in the document.
		// Use filters in the appropriate documents
		setColor(Color.BLACK); // Required for EPS, PDF, and SVG
		setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, null, 0f)); // EPS and PDF
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		VectorGraphics2D clone = (VectorGraphics2D) super.clone();
		clone.state = (GraphicsState) state.clone();
		return clone;
	}

	@Override
	public void addRenderingHints(Map<?, ?> hints) {
		if (isDisposed()) {
			return;
		}
		for (Entry<?, ?> entry : hints.entrySet()) {
			setRenderingHint((Key) entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clip(Shape s) {
		Shape clip = getClip();
		if ((clip != null) && (s != null)) {
			s = intersectShapes(clip, s);
		}
		setClip(s);
	}

	private static Shape intersectShapes(Shape s1, Shape s2) {
		if (s1 instanceof Rectangle2D && s2 instanceof Rectangle2D) {
			Rectangle2D r1 = (Rectangle2D) s1;
			Rectangle2D r2 = (Rectangle2D) s2;
	        double x1 = Math.max(r1.getMinX(), r2.getMinX());
	        double y1 = Math.max(r1.getMinY(), r2.getMinY());
	        double x2 = Math.min(r1.getMaxX(), r2.getMaxX());
	        double y2 = Math.min(r1.getMaxY(), r2.getMaxY());

	        Rectangle2D intersection = new Rectangle2D.Double();
	        if ((x2 < x1) || (y2 < y1)) {
	        	intersection.setFrameFromDiagonal(0, 0, 0, 0);
	        } else {
	        	intersection.setFrameFromDiagonal(x1, y1, x2, y2);
	        }
	        return intersection;
        } else {
			Area intersection = new Area(s1);
			intersection.intersect(new Area(s2));
			return intersection;
        }
	}

	@Override
	public void draw(Shape s) {
		if (isDisposed() || s == null) {
			return;
		}
		emit(new DrawShapeCommand(s));
	}

	@Override
	public void drawGlyphVector(GlyphVector g, float x, float y) {
		Shape s = g.getOutline(x, y);
		draw(s);
	}

	@Override
	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		BufferedImage bimg = getTransformedImage(img, xform);
		return drawImage(bimg, bimg.getMinX(), bimg.getMinY(),
			bimg.getWidth(), bimg.getHeight(), null, null);
	}

	/**
	 * Returns a transformed version of an image.
	 * @param image Image to be transformed
	 * @param xform Affine transform to be applied
	 * @return Image with transformed content
	 */
	private BufferedImage getTransformedImage(Image image,
			AffineTransform xform) {
		Integer interpolationType =
			(Integer) getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		if (RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
				.equals(interpolationType)) {
			interpolationType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
		} else if (RenderingHints.VALUE_INTERPOLATION_BILINEAR
				.equals(interpolationType)) {
			interpolationType = AffineTransformOp.TYPE_BILINEAR;
		} else {
			interpolationType = AffineTransformOp.TYPE_BICUBIC;
		}
		AffineTransformOp op = new AffineTransformOp(xform, interpolationType);
		BufferedImage bufferedImage = GraphicsUtils.toBufferedImage(image);
		return op.filter(bufferedImage, null);
	}

	@Override
	public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
		if (op != null) {
			img = op.filter(img, null);
		}
		drawImage(img, x, y, img.getWidth(), img.getHeight(), null, null);
	}

	@Override
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		drawRenderedImage(img.createDefaultRendering(), xform);
	}

	@Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		BufferedImage bimg = GraphicsUtils.toBufferedImage(img);
		drawImage(bimg, xform, null);
	}

	@Override
	public void drawString(String str, int x, int y) {
		drawString(str, (float) x, (float) y);
	}

	@Override
	public void drawString(String str, float x, float y) {
		if (isDisposed() || str == null || str.trim().length() == 0) {
			return;
		}
		boolean isTextAsVectors = false;
		if (isTextAsVectors) {
			TextLayout layout = new TextLayout(str, getFont(),
					getFontRenderContext());
			Shape s = layout.getOutline(
					AffineTransform.getTranslateInstance(x, y));
			fill(s);
		} else {
			emit(new DrawStringCommand(str, x, y));
		}

	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		drawString(iterator, (float) x, (float) y);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x,
			float y) {
		// TODO Draw styled text
		StringBuilder buf = new StringBuilder();
		for (char c = iterator.first(); c != AttributedCharacterIterator.DONE;
				c = iterator.next()) {
			buf.append(c);
		}
		drawString(buf.toString(), x, y);
	}

	@Override
	public void fill(Shape s) {
		if (isDisposed() || s == null) {
			return;
		}
		emit(new FillShapeCommand(s));
	}

	@Override
	public Color getBackground() {
		return state.getBackground();
	}

	@Override
	public Composite getComposite() {
		return state.getComposite();
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		return deviceConfig;
	}

	@Override
	public FontRenderContext getFontRenderContext() {
		return fontRenderContext;
	}

	@Override
	public Paint getPaint() {
		return state.getPaint();
	}

	@Override
	public Object getRenderingHint(Key hintKey) {
		if (RenderingHints.KEY_ANTIALIASING.equals(hintKey)) {
			return RenderingHints.VALUE_ANTIALIAS_OFF;
		} else if (RenderingHints.KEY_TEXT_ANTIALIASING.equals(hintKey)) {
			return RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
		} else if (RenderingHints.KEY_FRACTIONALMETRICS.equals(hintKey)) {
			return RenderingHints.VALUE_FRACTIONALMETRICS_ON;
		}
		return state.getHints().get(hintKey);
	}

	@Override
	public RenderingHints getRenderingHints() {
		return (RenderingHints) state.getHints().clone();
	}

	@Override
	public Stroke getStroke() {
		return state.getStroke();
	}

	@Override
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		Shape hitShape = s;
		if (onStroke) {
			hitShape = getStroke().createStrokedShape(hitShape);
		}
		hitShape = state.transformShape(hitShape);
		return hitShape.intersects(rect);
	}

	@Override
	public void setBackground(Color color) {
		if (isDisposed() || color == null || getColor().equals(color)) {
			return;
		}
		emit(new SetBackgroundCommand(color));
		state.setBackground(color);
	}

	@Override
	public void setComposite(Composite comp) {
		if (isDisposed()) {
			return;
		}
		if (comp == null) {
			throw new IllegalArgumentException("Cannot set a null composite.");
		}
		emit(new SetCompositeCommand(comp));
		state.setComposite(comp);
	}

	@Override
	public void setPaint(Paint paint) {
		if (isDisposed() || paint == null) {
			return;
		}
		if (paint instanceof Color) {
			setColor((Color) paint);
			return;
		}
		if (getPaint().equals(paint)) {
			return;
		}
		emit(new SetPaintCommand(paint));
		state.setPaint(paint);
	}

	@Override
	public void setRenderingHint(Key hintKey, Object hintValue) {
		if (isDisposed()) {
			return;
		}
		state.getHints().put(hintKey, hintValue);
		emit(new SetHintCommand(hintKey, hintValue));
	}

	@Override
	public void setRenderingHints(Map<?, ?> hints) {
		if (isDisposed()) {
			return;
		}
		state.getHints().clear();
		for (Entry<?, ?> hint : hints.entrySet()) {
			setRenderingHint((Key) hint.getKey(), hint.getValue());
		}
	}

	@Override
	public void setStroke(Stroke s) {
		if (isDisposed()) {
			return;
		}
		if (s == null) {
			throw new IllegalArgumentException("Cannot set a null stroke.");
		}
		emit(new SetStrokeCommand(s));
		state.setStroke(s);
	}

	@Override
	public AffineTransform getTransform() {
		return new AffineTransform(state.getTransform());
	}

	@Override
	public void setTransform(AffineTransform tx) {
		if (isDisposed() || tx == null || state.getTransform().equals(tx)) {
			return;
		}
		emit(new SetTransformCommand(tx));
		state.setTransform(tx);
	}

	@Override
	public void shear(double shx, double shy) {
		if (shx == 0.0 && shy == 0.0) {
			return;
		}
		AffineTransform txNew = getTransform();
		txNew.shear(shx, shy);
		emit(new ShearCommand(shx, shy));
		state.setTransform(txNew);
	}

	@Override
	public void transform(AffineTransform tx) {
		if (tx.isIdentity()) {
			return;
		}
		AffineTransform txNew = getTransform();
		txNew.concatenate(tx);
		emit(new TransformCommand(tx));
		state.setTransform(txNew);
	}

	@Override
	public void translate(int x, int y) {
		translate((double) x, (double) y);
	}

	@Override
	public void translate(double tx, double ty) {
		if (tx == 0.0 && ty == 0.0) {
			return;
		}
		AffineTransform txNew = getTransform();
		txNew.translate(tx, ty);
		emit(new TranslateCommand(tx, ty));
		state.setTransform(txNew);
	}

	@Override
	public void rotate(double theta) {
		rotate(theta, 0.0, 0.0);
	}

	@Override
	public void rotate(double theta, double x, double y) {
		if (theta == 0.0) {
			return;
		}

		AffineTransform txNew = getTransform();
		if (x == 0.0 && y == 0.0) {
			txNew.rotate(theta);
		} else {
			txNew.rotate(theta, x, y);
		}

		emit(new RotateCommand(theta, x, y));
		state.setTransform(txNew);
	}

	@Override
	public void scale(double sx, double sy) {
		if (sx == 1.0 && sy == 1.0) {
			return;
		}
		AffineTransform txNew = getTransform();
		txNew.scale(sx, sy);
		emit(new ScaleCommand(sx, sy));
		state.setTransform(txNew);
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		Color colorOld = getColor();
		setColor(getBackground());
		fillRect(x, y, width, height);
		setColor(colorOld);
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		clip(new Rectangle(x, y, width, height));
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		// TODO Implement
		//throw new UnsupportedOperationException("copyArea() isn't supported by VectorGraphics2D.");
	}

	@Override
	public Graphics create() {
		if (isDisposed()) {
			return null;
		}
		VectorGraphics2D clone = null;
		try {
			clone = (VectorGraphics2D) this.clone();
			emit(new CreateCommand(clone));
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}

	@Override
	public void dispose() {
		if (isDisposed()) {
			return;
		}

		emit(new DisposeCommand(this));

		disposed = true;
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle,
			int arcAngle) {
		draw(new Arc2D.Double(x, y, width, height,
			startAngle, arcAngle, Arc2D.OPEN));
	}

	@Override
	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		return drawImage(img, x, y, img.getWidth(observer),
			img.getHeight(observer), null, observer);
	}

	@Override
	public boolean drawImage(Image img, int x, int y, Color bgcolor,
			ImageObserver observer) {
		return drawImage(img, x, y, img.getWidth(observer),
			img.getHeight(observer), bgcolor, observer);
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height,
			ImageObserver observer) {
		return drawImage(img, x, y, width, height, null, observer);
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height,
			Color bgcolor, ImageObserver observer) {
		if (isDisposed() || img == null) {
			return true;
		}

		int imageWidth = img.getWidth(observer);
		int imageHeight = img.getHeight(observer);
		Rectangle bounds = new Rectangle(x, y, width, height);

		if (bgcolor != null) {
			// Fill rectangle with bgcolor
			Color bgcolorOld = getColor();
			setColor(bgcolor);
			fill(bounds);
			setColor(bgcolorOld);
		}

		emit(new DrawImageCommand(img, imageWidth, imageHeight, x, y, width, height));
		return true;
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
			int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
		return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null,
			observer);
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
			int sx1, int sy1, int sx2, int sy2, Color bgcolor,
			ImageObserver observer) {
		if (img == null) {
			return true;
		}

		int sx = Math.min(sx1, sx2);
		int sy = Math.min(sy1, sy2);
		int sw = Math.abs(sx2 - sx1);
		int sh = Math.abs(sy2 - sy1);
		int dx = Math.min(dx1, dx2);
		int dy = Math.min(dy1, dy2);
		int dw = Math.abs(dx2 - dx1);
		int dh = Math.abs(dy2 - dy1);

		// Draw image on rectangle
		BufferedImage bufferedImg = GraphicsUtils.toBufferedImage(img);
		Image cropped = bufferedImg.getSubimage(sx, sy, sw, sh);
		return drawImage(cropped, dx, dy, dw, dh, bgcolor, observer);
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		draw(new Line2D.Double(x1, y1, x2, y2));
	}

	@Override
	public void drawOval(int x, int y, int width, int height) {
		draw(new Ellipse2D.Double(x, y, width, height));
	}

	@Override
	public void drawPolygon(Polygon p) {
		draw(p);
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		draw(new Polygon(xPoints, yPoints, nPoints));
	}

	@Override
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
		Path2D p = new Path2D.Float();
		for (int i = 0; i < nPoints; i++) {
			if (i > 0) {
				p.lineTo(xPoints[i], yPoints[i]);
			} else {
				p.moveTo(xPoints[i], yPoints[i]);
			}
		}
		draw(p);
	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		draw(new Rectangle(x, y, width, height));
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height,
			int arcWidth, int arcHeight) {
		draw(new RoundRectangle2D.Double(x, y, width, height,
				arcWidth, arcHeight));
	}

	@Override
	public void fillArc(int x, int y, int width, int height,
			int startAngle, int arcAngle) {
		fill(new Arc2D.Double(x, y, width, height,
				startAngle, arcAngle, Arc2D.PIE));
	}

	@Override
	public void fillOval(int x, int y, int width, int height) {
		fill(new Ellipse2D.Double(x, y, width, height));
	}

	@Override
	public void fillPolygon(Polygon p) {
		fill(p);
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		fill(new Polygon(xPoints, yPoints, nPoints));
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		fill(new Rectangle(x, y, width, height));
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height,
			int arcWidth, int arcHeight) {
		fill(new RoundRectangle2D.Double(x, y, width, height,
				arcWidth, arcHeight));
	}

	@Override
	public Shape getClip() {
		return state.getClip();
	}

	@Override
	public Rectangle getClipBounds() {
		if (getClip() == null) {
			return null;
		}
		return getClip().getBounds();
	}

	@Override
	public Color getColor() {
		return state.getColor();
	}

	@Override
	public Font getFont() {
		return state.getFont();
	}

	@Override
	public FontMetrics getFontMetrics(Font f) {
		BufferedImage bi =
			new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
	    Graphics g = bi.getGraphics();
	    FontMetrics fontMetrics = g.getFontMetrics(f);
	    g.dispose();
	    return fontMetrics;
	}

	@Override
	public void setClip(Shape clip) {
		if (isDisposed()) {
			return;
		}
		emit(new SetClipCommand(clip));
		state.setClip(clip);
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		setClip(new Rectangle(x, y, width, height));
	}

	@Override
	public void setColor(Color c) {
		if (isDisposed() || c == null || getColor().equals(c)) {
			return;
		}
		emit(new SetColorCommand(c));
		state.setColor(c);
		state.setPaint(c);
	}

	@Override
	public void setFont(Font font) {
		if (isDisposed() || (font != null && getFont().equals(font))) {
			return;
		}
		emit(new SetFontCommand(font));
		state.setFont(font);
	}

	@Override
	public void setPaintMode() {
		setComposite(AlphaComposite.SrcOver);
	}

	public Color getXORMode() {
		return state.getXorMode();
	}

	@Override
	public void setXORMode(Color c1) {
		if (isDisposed() || c1 == null) {
			return;
		}
		emit(new SetXORModeCommand(c1));
		state.setXorMode(c1);
	}

	private void emit(Command<?> command) {
		commands.add(command);
	}

	protected boolean isDisposed() {
		return disposed;
	}

	/**
	 * Returns a {@code CommandSequence} representing all calls that were issued to this {@code VectorGraphics2D} object.
	 * @return Sequence of commands since.
	 */
	public CommandSequence getCommands() {
		return commands;
	}
}
