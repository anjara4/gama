/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.gui.displays.layers;

import java.awt.Color;
import java.util.*;
import msi.gama.common.interfaces.*;
import msi.gama.common.util.ImageUtils;
import msi.gama.gui.parameters.EditorFactory;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.GamaPoint;
import msi.gama.metamodel.topology.grid.GamaSpatialMatrix;
import msi.gama.outputs.layers.*;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import org.eclipse.swt.widgets.Composite;

public class GridLayer extends ImageLayer {

	private boolean turnGridOn;

	public GridLayer(final ILayerStatement layer) {
		super(layer);
		turnGridOn = ((GridLayerStatement) layer).drawLines();
	}

	@Override
	public void outputChanged() {
		super.outputChanged();
		if ( image != null ) {
			image.flush();
			image = null;
		}
	}

	@Override
	public void fillComposite(final Composite compo, final IDisplaySurface container) {
		super.fillComposite(compo, container);
		EditorFactory.create(compo, "Draw grid:", turnGridOn, new EditorListener<Boolean>() {

			@Override
			public void valueModified(final Boolean newValue) throws GamaRuntimeException {
				turnGridOn = newValue;
				container.forceUpdateDisplay();
			}
		});
	}

	@Override
	protected void buildImage() {
		GridLayerStatement g = (GridLayerStatement) definition;
		GamaSpatialMatrix m = g.getEnvironment();
		if ( image == null ) {
			image = ImageUtils.createCompatibleImage(m.numCols, m.numRows);
		}
		image.setRGB(0, 0, m.numCols, m.numRows, m.getDisplayData(), 0, m.numCols);
	}

	@Override
	public void privateDrawDisplay(final IGraphics dg) {
		buildImage();
		if ( image == null ) { return; }
		Color lineColor = null;
		if ( turnGridOn ) {
			lineColor = ((GridLayerStatement) definition).getLineColor();
			if ( lineColor == null ) {
				lineColor = Color.black;
			}
		}
		dg.drawImage(null, image, null, null, lineColor, null, 0.0, true);

	}

	private IAgent getPlaceAt(final GamaPoint loc) {
		return ((GridLayerStatement) definition).getEnvironment().getPlaceAt(loc).getAgent();
	}

	@Override
	public Set<IAgent> collectAgentsAt(final int x, final int y, IDisplaySurface g) {
		Set<IAgent> result = new HashSet();
		result.add(getPlaceAt(this.getModelCoordinatesFrom(x, y, g)));
		return result;
	}

	@Override
	protected String getType() {
		return "Grid layer";
	}

}
