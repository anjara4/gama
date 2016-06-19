/*
 * GeoTools - The Open Source Java GIS Toolkit
 * http://geotools.org
 *
 * (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package ummisco.gama.ui.viewers.gis.geotools.tool;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.awt.geom.Point2D;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;

import ummisco.gama.ui.viewers.gis.geotools.event.MapMouseEvent;

/**
 * A zoom-in tool for JMapPane.
 * <p>
 * For mouse clicks, the display will be zoomed-in such that the
 * map centre is the position of the mouse click and the map
 * width and height are calculated as:
 * 
 * <pre>
 *    {@code len = len.old / z}
 * </pre>
 * 
 * where {@code z} is the linear zoom increment (>= 1.0)
 * <p>
 * The tool also responds to the user drawing a box on the map mapPane with
 * mouse click-and-drag to define the zoomed-in area.
 * 
 * @author Michael Bedward
 * @author Andrea Antonello (www.hydrologis.com)
 *
 *
 *
 * @source $URL$
 */
public class ZoomInTool extends AbstractZoomTool {

	/** Tool name */
	public static final String TOOL_NAME = "Zoom in";
	/** Tool tip text */
	public static final String TOOL_TIP = "Click to zoom in or drag to define the zoom area";

	private Point2D startDragPos;
	private boolean dragged;

	/**
	 * Constructs a new zoom in tool. To activate the tool only on certain
	 * mouse events provide a single mask, e.g. {@link SWT#BUTTON1}, or
	 * a combination of multiple SWT-masks.
	 *
	 * @param triggerButtonMask Mouse button which triggers the tool's activation
	 *            or {@value #ANY_BUTTON} if the tool is to be triggered by any button
	 */
	public ZoomInTool(final int triggerButtonMask) {
		super(triggerButtonMask);

		startDragPos = new DirectPosition2D();
		dragged = false;
	}

	/**
	 * Constructs a new zoom in tool which is triggered by any mouse button.
	 */
	public ZoomInTool() {
		this(CursorTool.ANY_BUTTON);
	}

	/**
	 * Zoom in by the currently set increment, with the map
	 * centred at the location (in world coords) of the mouse
	 * click
	 * 
	 * @param e map mapPane mouse event
	 */
	@Override
	public void onMouseClicked(final MapMouseEvent e) {

		if ( !isTriggerMouseButton(e) ) { return; }

		startDragPos = new DirectPosition2D();
		startDragPos.setLocation(e.getMapPosition());
	}

	/**
	 * Records the map position of the mouse event in case this
	 * button press is the beginning of a mouse drag
	 *
	 * @param ev the mouse event
	 */
	@Override
	public void onMousePressed(final MapMouseEvent ev) {}

	/**
	 * Records that the mouse is being dragged
	 *
	 * @param ev the mouse event
	 */
	@Override
	public void onMouseDragged(final MapMouseEvent ev) {

		if ( !isTriggerMouseButton(ev) ) { return; }

		dragged = true;
	}

	/**
	 * If the mouse was dragged, determines the bounds of the
	 * box that the user defined and passes this to the mapPane's
	 * {@link org.geotools.swing.JMapPane#setDisplayArea(org.opengis.geometry.Envelope) }
	 * method
	 *
	 * @param ev the mouse event
	 */
	@Override
	public void onMouseReleased(final MapMouseEvent ev) {

		if ( !isTriggerMouseButton(ev) ) { return; }

		if ( dragged ) {
			final Envelope2D env = new Envelope2D();
			env.setFrameFromDiagonal(startDragPos, ev.getMapPosition());
			dragged = false;
			getMapPane().setDisplayArea(env);
		} else {
			final Rectangle paneArea = getMapPane().getVisibleRect();

			final double scale = getMapPane().getWorldToScreenTransform().getScaleX();
			final double newScale = scale * zoom;

			final DirectPosition2D corner = new DirectPosition2D(startDragPos.getX() - 0.5d * paneArea.width / newScale,
				startDragPos.getY() + 0.5d * paneArea.height / newScale);

			final Envelope2D newMapArea = new Envelope2D();
			newMapArea.setFrameFromCenter(startDragPos, corner);
			getMapPane().setDisplayArea(newMapArea);
		}

	}

	@Override
	public boolean canDraw() {
		return true;
	}

	@Override
	public boolean canMove() {
		return false;
	}

	@Override
	public boolean isDrawing() {
		return dragged;
	}

	public static double pythagoras(final double d1, final double d2) {
		return sqrt(pow(d1, 2.0) + pow(d2, 2.0));
	}
}
