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
package msi.gama.util.matrix;

import java.util.*;
import msi.gama.common.util.RandomUtils;
import msi.gama.metamodel.shape.*;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.*;
import msi.gaml.operators.Cast;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.*;
import com.google.common.primitives.Doubles;
import com.vividsolutions.jts.index.quadtree.IntervalSize;

public class GamaFloatMatrix extends GamaMatrix<Double> {

	static public GamaFloatMatrix from(final IScope scope, final IMatrix m) {
		if ( m instanceof GamaFloatMatrix ) { return (GamaFloatMatrix) m; }
		if ( m instanceof GamaObjectMatrix ) { return new GamaFloatMatrix(scope, m.getCols(scope), m.getRows(scope),
			((GamaObjectMatrix) m).getMatrix()); }
		if ( m instanceof GamaIntMatrix ) { return new GamaFloatMatrix(m.getCols(scope), m.getRows(scope),
			((GamaIntMatrix) m).matrix); }
		return null;
	}

	static public GamaFloatMatrix from(final IScope scope, final int c, final int r, final IMatrix m) {
		if ( m instanceof GamaFloatMatrix ) { return new GamaFloatMatrix(c, r, ((GamaFloatMatrix) m).getMatrix()); }
		if ( m instanceof GamaObjectMatrix ) { return new GamaFloatMatrix(scope, c, r,
			((GamaObjectMatrix) m).getMatrix()); }
		if ( m instanceof GamaIntMatrix ) { return new GamaFloatMatrix(c, r, ((GamaIntMatrix) m).matrix); }
		return null;
	}

	private double[] matrix;

	public GamaFloatMatrix(final RealMatrix rm) {
		super(rm.getColumnDimension(), rm.getRowDimension());
		matrix = new double[rm.getColumnDimension() * rm.getRowDimension()];
		updateMatrix(rm);
	}

	public GamaFloatMatrix(final double[] mat) {
		super(1, mat.length);
		setMatrix(mat);
	}

	public GamaFloatMatrix(final GamaPoint p) {
		this((int) p.x, (int) p.y);
	}

	public GamaFloatMatrix(final int cols, final int rows) {
		super(cols, rows);
		setMatrix(new double[cols * rows]);
	}

	public GamaFloatMatrix(final int cols, final int rows, final double[] objects) {
		this(cols, rows);
		java.lang.System.arraycopy(objects, 0, getMatrix(), 0, Math.min(objects.length, rows * cols));
	}

	public GamaFloatMatrix(final int cols, final int rows, final int[] objects) {
		this(cols, rows);
		java.lang.System.arraycopy(objects, 0, getMatrix(), 0, Math.min(objects.length, rows * cols));
	}

	public GamaFloatMatrix(final IScope scope, final int cols, final int rows, final Object[] objects) {
		this(cols, rows);
		for ( int i = 0, n = Math.min(objects.length, rows * cols); i < n; i++ ) {
			getMatrix()[i] = Cast.asFloat(null, objects[i]);
		}
	}

	public GamaFloatMatrix(final IScope scope, final List objects, final boolean flat, final GamaPoint preferredSize)
		throws GamaRuntimeException {
		super(scope, objects, flat, preferredSize);
		setMatrix(new double[numRows * numCols]);
		if ( preferredSize != null ) {
			for ( int i = 0, stop = Math.min(getMatrix().length, objects.size()); i < stop; i++ ) {
				getMatrix()[i] = Cast.asFloat(scope, objects.get(i));
			}
		} else if ( flat || GamaMatrix.isFlat(objects) ) {
			for ( int i = 0, stop = objects.size(); i < stop; i++ ) {
				getMatrix()[i] = Cast.asFloat(scope, objects.get(i));
			}
		} else {
			for ( int i = 0; i < numRows; i++ ) {
				for ( int j = 0; j < numCols; j++ ) {
					set(scope, j, i, Cast.asFloat(scope, ((List) objects.get(j)).get(i)));
				}
			}
		}
	}

	public GamaFloatMatrix(final IScope scope, final Object[] mat) {
		this(1, mat.length);
		for ( int i = 0; i < mat.length; i++ ) {
			getMatrix()[i] = Cast.asFloat(null, mat[i]);
		}
	}

	@Override
	protected void _clear() {
		Arrays.fill(getMatrix(), 0d);
	}

	@Override
	public boolean _contains(final IScope scope, final Object o) {
		if ( o instanceof Double ) {
			final Double d = (Double) o;
			for ( int i = 0; i < getMatrix().length; i++ ) {
				if ( IntervalSize.isZeroWidth(getMatrix()[i], d) ) { return true; }
			}
		}
		return false;
	}

	@Override
	public Double _first(final IScope scope) {
		if ( getMatrix().length == 0 ) { return 0d; }
		return getMatrix()[0];
	}

	@Override
	public Double _last(final IScope scope) {
		if ( getMatrix().length == 0 ) { return 0d; }
		return getMatrix()[getMatrix().length - 1];
	}

	@Override
	public Integer _length(final IScope scope) {
		return getMatrix().length;
	}

	// @Override
	// public Double _max(final IScope scope) {
	// Double max = -Double.MAX_VALUE;
	// for ( int i = 0; i < matrix.length; i++ ) {
	// if ( matrix[i] > max ) {
	// max = Double.valueOf(matrix[i]);
	// }
	// }
	// return max;
	// }
	//
	// @Override
	// public Double _min(final IScope scope) {
	// Double min = Double.MAX_VALUE;
	// for ( int i = 0; i < matrix.length; i++ ) {
	// if ( matrix[i] < min ) {
	// min = Double.valueOf(matrix[i]);
	// }
	// }
	// return min;
	// }
	//
	// @Override
	// public Double _product(final IScope scope) {
	// double result = 1.0;
	// for ( int i = 0, n = matrix.length; i < n; i++ ) {
	// result *= matrix[i];
	// }
	// return result;
	// }
	//
	// @Override
	// public Double _sum(final IScope scope) {
	// double result = 0.0;
	// for ( int i = 0, n = matrix.length; i < n; i++ ) {
	// result += matrix[i];
	// }
	// return result;
	// }
	//
	@Override
	public boolean _isEmpty(final IScope scope) {
		for ( int i = 0; i < getMatrix().length; i++ ) {
			if ( getMatrix()[i] != 0d ) { return false; }
		}
		return true;
	}

	@Override
	public GamaList _listValue(final IScope scope) {
		return new GamaList(getMatrix());
	}

	@Override
	protected IMatrix _matrixValue(final IScope scope, final ILocation preferredSize) {
		if ( preferredSize == null ) { return this; }
		final int cols = (int) preferredSize.getX();
		final int rows = (int) preferredSize.getY();
		return new GamaFloatMatrix(cols, rows, getMatrix());
	}

	@Override
	public IMatrix _reverse(final IScope scope) throws GamaRuntimeException {
		final GamaFloatMatrix result = new GamaFloatMatrix(numRows, numCols);
		for ( int i = 0; i < numCols; i++ ) {
			for ( int j = 0; j < numRows; j++ ) {
				double val = get(scope, i, j);
				result.set(scope, j, i, val);
			}
		}
		return result;
	}

	@Override
	public GamaFloatMatrix copy(final IScope scope) {
		return new GamaFloatMatrix(numCols, numRows, getMatrix());
	}

	@Override
	public boolean equals(final Object m) {
		if ( this == m ) { return true; }
		if ( !(m instanceof GamaFloatMatrix) ) { return false; }
		final GamaFloatMatrix mat = (GamaFloatMatrix) m;
		return Arrays.equals(this.getMatrix(), mat.getMatrix());
	}

	@Override
	public int hashCode() {
		return getMatrix().hashCode();
	}

	@Override
	public void _putAll(final IScope scope, final Object o, final Object param) throws GamaRuntimeException {
		// TODO Exception if o == null
		// TODO Verify the type
		Arrays.fill(getMatrix(), ((Double) o).doubleValue());

	}

	@Override
	public Double get(final IScope scope, final int col, final int row) {
		if ( col >= numCols || col < 0 || row >= numRows || row < 0 ) { return 0d; }
		return getMatrix()[row * numCols + col];
	}

	// public void put(final int col, final int row, final double obj) {
	// if ( !(col >= numCols || col < 0 || row >= numRows || row < 0) ) {
	// matrix[row * numCols + col] = obj;
	// }
	// }

	@Override
	public void set(final IScope scope, final int col, final int row, final Object obj) throws GamaRuntimeException {
		if ( !(col >= numCols || col < 0 || row >= numRows || row < 0) ) {
			double val = Cast.asFloat(scope, obj);
			getMatrix()[row * numCols + col] = val;
		}
		// put(col, row, Cast.asFloat(GAMA.getDefaultScope(), obj).doubleValue());
	}

	private boolean remove(final double o) {
		for ( int i = 0; i < getMatrix().length; i++ ) {
			if ( new Double(getMatrix()[i]).equals(new Double(o)) ) {
				getMatrix()[i] = 0d;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean _removeFirst(final IScope scope, final Double o) throws GamaRuntimeException {
		// Exception if o == null
		return remove(o.doubleValue());
	}

	@Override
	public Double remove(final IScope scope, final int col, final int row) {
		if ( col >= numCols || col < 0 || row >= numRows || row < 0 ) { return 0d; }
		final double o = getMatrix()[row * numCols + col];
		getMatrix()[row * numCols + col] = 0d;
		return o;
	}

	private boolean removeAll(final double o) {
		boolean removed = false;
		for ( int i = 0; i < getMatrix().length; i++ ) {
			if ( new Double(getMatrix()[i]).equals(new Double(o)) ) {
				getMatrix()[i] = 0d;
				removed = true;
			}
		}
		return removed;
	}

	@Override
	public boolean _removeAll(final IScope scope, final IContainer<?, Double> list) {
		// TODO Exception if o == null
		for ( final Double o : list ) {
			removeAll(o.doubleValue());
		}
		// TODO Make a test to verify the return
		return true;
	}

	@Override
	public void shuffleWith(final RandomUtils randomAgent) {
		setMatrix(randomAgent.shuffle(getMatrix()));
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(numRows * numCols * 5);
		sb.append('[');
		for ( int row = 0; row < numRows; row++ ) {
			for ( int col = 0; col < numCols; col++ ) {
				sb.append(get(null, col, row));
				if ( col < numCols - 1 ) {
					sb.append(',');
				}
			}
			if ( row < numRows - 1 ) {
				sb.append(';');
			}
		}
		sb.append(']');
		return sb.toString();
	}

	@Override
	public String toGaml() {
		return new GamaList(this.getMatrix()).toGaml() + " as matrix";
	}

	/**
	 * Method iterator()
	 * @see msi.gama.util.matrix.GamaMatrix#iterator()
	 */
	@Override
	public Iterator<Double> iterator() {
		return Doubles.asList(getMatrix()).iterator();
	}

	public double[] getMatrix() {
		return matrix;
	}

	void setMatrix(final double[] matrix) {
		this.matrix = matrix;
	}

	RealMatrix getRealMatrix() {
		RealMatrix realMatrix = new Array2DRowRealMatrix(this.numRows, this.numCols);
		for ( int i = 0; i < this.numRows; i++ ) {
			for ( int j = 0; j < this.numCols; j++ ) {
				realMatrix.setEntry(i, j, this.get(null, j, i));
			}
		}
		return realMatrix;
	}

	void updateMatrix(final RealMatrix realMatrix) {
		for ( int i = 0; i < this.numRows; i++ ) {
			for ( int j = 0; j < this.numCols; j++ ) {
				getMatrix()[i * numCols + j] = realMatrix.getEntry(i, j);
			}
		}
	}

	@Override
	public IMatrix plus(final IScope scope, final IMatrix other) throws GamaRuntimeException {
		GamaFloatMatrix matb = from(scope, other);
		if ( matb != null && this.numCols == matb.numCols && this.numRows == matb.numRows ) {
			GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
			for ( int i = 0; i < matrix.length; i++ ) {
				nm.matrix[i] = matrix[i] + matb.matrix[i];
			}
			return nm;
		}
		throw GamaRuntimeException.error(" The dimensions of the matrices do not correspond", scope);
	}

	@Override
	public IMatrix times(final IScope scope, final IMatrix other) throws GamaRuntimeException {
		GamaFloatMatrix matb = from(scope, other);
		if ( matb != null && this.numCols == matb.numCols && this.numRows == matb.numRows ) {
			GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
			for ( int i = 0; i < matrix.length; i++ ) {
				nm.matrix[i] = matrix[i] * matb.matrix[i];
			}
			return nm;
		}
		throw GamaRuntimeException.error(" The dimensions of the matrices do not correspond", scope);
	}

	@Override
	public IMatrix minus(final IScope scope, final IMatrix other) throws GamaRuntimeException {
		GamaFloatMatrix matb = from(scope, other);
		if ( matb != null && this.numCols == matb.numCols && this.numRows == matb.numRows ) {
			GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
			for ( int i = 0; i < matrix.length; i++ ) {
				nm.matrix[i] = matrix[i] - matb.matrix[i];
			}
			return nm;
		}
		throw GamaRuntimeException.error(" The dimensions of the matrices do not correspond", scope);
	}

	@Override
	public IMatrix times(final Double val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] * val;
		}
		return nm;
	}

	@Override
	public IMatrix times(final Integer val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] * val;
		}
		return nm;
	}

	@Override
	public IMatrix divides(final Double val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] / val;
		}
		return nm;
	}

	@Override
	public IMatrix divides(final Integer val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] / val;
		}
		return nm;
	}

	@Override
	public IMatrix divides(final IScope scope, final IMatrix other) throws GamaRuntimeException {
		GamaFloatMatrix matb = from(scope, other);
		if ( matb != null && this.numCols == matb.numCols && this.numRows == matb.numRows ) {
			GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
			for ( int i = 0; i < matrix.length; i++ ) {
				nm.matrix[i] = matrix[i] / matb.matrix[i];
			}
			return nm;
		}
		throw GamaRuntimeException.error(" The dimensions of the matrices do not correspond", scope);
	}

	@Override
	public IMatrix matrixMultiplication(final IScope scope, final IMatrix other) throws GamaRuntimeException {
		GamaFloatMatrix matb = from(scope, other);
		try {
			if ( matb != null ) { return new GamaFloatMatrix(getRealMatrix().multiply(matb.getRealMatrix())); }
		} catch (DimensionMismatchException e) {
			throw GamaRuntimeException.error("The dimensions of the matrices do not correspond", scope);
		}
		return null;
	}

	@Override
	public IMatrix plus(final Double val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] + val;
		}
		return nm;
	}

	@Override
	public IMatrix plus(final Integer val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] + val;
		}
		return nm;
	}

	@Override
	public IMatrix minus(final Double val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] - val;
		}
		return nm;
	}

	@Override
	public IMatrix minus(final Integer val) throws GamaRuntimeException {
		GamaFloatMatrix nm = new GamaFloatMatrix(this.numCols, this.numRows);
		for ( int i = 0; i < matrix.length; i++ ) {
			nm.matrix[i] = matrix[i] - val;
		}
		return nm;
	}

	//
	// @Override
	// public String toJava() {
	// return "GamaMatrixType.from(" + Cast.toJava(new GamaList(this.matrix)) + ", false)";
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see msi.gama.interfaces.IGamaContainer#checkValue(java.lang.Object)
	 */
	// @Override
	// public boolean checkValue(final Object value) {
	// return value instanceof Double;
	// }
}
