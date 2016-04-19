/*********************************************************************************************
 *
 *
 * 'MyTexture.java', in plugin 'msi.gama.jogl2', is part of the source code of the
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 *
 *
 **********************************************************************************************/
package ummisco.gama.opengl;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.jogamp.opengl.GL2;
import com.vividsolutions.jts.geom.*;
import msi.gama.common.interfaces.IDisplaySurface;
import msi.gama.metamodel.shape.IShape;
import msi.gama.util.file.GamaGeometryFile;
import ummisco.gama.opengl.files.GamaObjFile;
import ummisco.gama.opengl.jts.JTSDrawer;

public class GeometryCache {

	private final Map<String, Integer> cache;


	public GeometryCache() {
		cache = new ConcurrentHashMap<>(100, 0.75f, 4);

		// drawer = new GeometryDrawer(renderer);
	}

	public Integer get(final GL2 gl, final JOGLRenderer renderer, final GamaGeometryFile file) {
		Integer index = cache.get(file.getPath());
		if ( index == null ) {
			try {
				index = buildList(gl, renderer, file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			cache.put(file.getPath(), index);
		}
		return index;
	}

	private Integer buildList(final GL2 gl, final JOGLRenderer renderer, final GamaGeometryFile file)
		throws FileNotFoundException {
		String extension = file.getExtension();
		// We generate the list first
		Integer index = gl.glGenLists(1);
		gl.glNewList(index, GL2.GL_COMPILE);
		// We push the matrix
		gl.glPushMatrix();
		// We draw the file in the list
		if ( extension.equals("obj") ) {
			((GamaObjFile) file).drawToOpenGL(gl);
		} else {
			IDisplaySurface surface = renderer.getSurface();
			IShape shape = file.getGeometry(surface.getDisplayScope());
			if ( shape == null ) { return index; }
			Geometry g = shape.getInnerGeometry();
			drawSimpleGeometry(gl, renderer, g);
		}
		// We then pop the matrix
		gl.glPopMatrix();
		// And close the list,
		gl.glEndList();
		// Before returning its index
		return index;
	}

	private final Color defaultColor = new Color(0, 0, 0);

	void drawSimpleGeometry(final GL2 gl, final JOGLRenderer renderer, final Geometry g) {
		if ( g.getNumGeometries() > 1 ) {
			for ( int i = 0; i < g.getNumGeometries(); i++ ) {
				Geometry jts = g.getGeometryN(i);
				drawSimpleGeometry(gl, renderer, jts);
			}
		} else {
			JTSDrawer drawer = new JTSDrawer(renderer);
			if ( g instanceof Polygon ) {
				drawer.drawTesselatedPolygon(gl, (Polygon) g, 1, defaultColor, 1);
			} else if ( g instanceof LineString ) {
				drawer.drawLineString(gl, (LineString) g, 0, JOGLRenderer.getLineWidth(), defaultColor, 1);
			}
		}
	}

	/**
	 * @param gl
	 */
	public void dispose(final GL2 gl) {
		for ( Integer i : cache.values() ) {
			gl.glDeleteLists(i, 1);
		}
		cache.clear();
	}

}