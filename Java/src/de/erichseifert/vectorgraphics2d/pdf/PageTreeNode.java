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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an intermediate node in the page tree of a PDF document.
 */
class PageTreeNode implements PDFObject {
	private final PageTreeNode parent;
	private final List<Page> children;

	/**
	 * Initializes a {@code PageTreeNode} with the specified parent node.
	 * @param parent Parent node or {@code null} to create a root node.
	 */
	public PageTreeNode(PageTreeNode parent) {
		this.parent = parent;
		this.children = new LinkedList<>();
	}

	/**
	 * Returns the type of this object.
	 * The return value is always {@literal Pages}.
	 * @return The String {@literal Pages}.
	 */
	public String getType() {
		return "Pages";
	}

	/**
	 * Returns the parent of this node.
	 * If this node is a root node, the method returns {@code null}.
	 * @return Parent node or {@code null}, if this is a root node.
	 */
	public PageTreeNode getParent() {
		return parent;
	}

	/**
	 * Adds the specified {@code Page} to the node's children.
	 * @param page {@code Page} to be added.
	 */
	public void add(Page page) {
		page.setParent(this);
		children.add(page);
	}

	/**
	 * Returns all {@code Page} objects that are immediate children of this node.
	 * @return List of child pages.
	 */
	public List<Page> getKids() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * Returns the total number of {@code Page} objects in this subtree.
	 * @return Number of pages that are direct or indirect children.
	 */
	public int getCount() {
		return children.size();
	}
}

