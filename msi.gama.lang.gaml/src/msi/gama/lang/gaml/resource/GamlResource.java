/**
 * Created by drogoul, 24 avr. 2012
 * 
 */
package msi.gama.lang.gaml.resource;

import java.util.*;
import msi.gama.common.interfaces.ISyntacticElement;
import msi.gama.common.util.GuiUtils;
import msi.gama.lang.gaml.parsing.*;
import msi.gama.lang.gaml.parsing.GamlSyntacticParser.GamlParseResult;
import msi.gama.lang.gaml.validation.IGamlBuilderListener;
import msi.gaml.descriptions.ModelDescription;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.*;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.linking.lazy.LazyLinkingResource;

/**
 * The class GamlResource.
 * 
 * @author drogoul
 * @since 24 avr. 2012
 * 
 */
public class GamlResource extends LazyLinkingResource {

	private IGamlBuilderListener listener;

	@Override
	public String toString() {
		return "resource" + "[" + getURI() + "]";
	}

	@Override
	protected void addSyntaxErrors() {
		super.addSyntaxErrors();
		getWarnings().addAll(getParseResult().getWarnings());
	}

	@Override
	public GamlParseResult getParseResult() {
		return (GamlParseResult) super.getParseResult();
	}

	public void setModelDescription(final boolean withErrors, final ModelDescription model) {
		if ( listener != null ) {
			Set<String> exp = model == null ? Collections.EMPTY_SET : model.getExperimentNames();
			listener.validationEnded(exp, withErrors);
		}
	}

	public void setListener(final IGamlBuilderListener listener) {
		this.listener = listener;
	}

	public void removeListener() {
		listener = null;
	}

	public IGamlBuilderListener getListener() {
		return listener;
	}

	@Override
	protected void unload(final EObject oldRootObject) {
		GuiUtils.debug("GamlResource.unload");
		if ( oldRootObject != null ) {
			EList<Adapter> list = new BasicEList<Adapter>(oldRootObject.eAdapters());
			for ( Adapter adapter : list ) {
				if ( adapter instanceof ModelDescription ) {
					((ModelDescription) adapter).dispose();
					oldRootObject.eAdapters().remove(adapter);
				}
			}
		}
		super.unload(oldRootObject);
	}

	public ISyntacticElement getSyntacticContents() {
		GamlParseResult parseResult = getParseResult();
		if ( parseResult == null ) { // Should not happen, but in case...
			Set<org.eclipse.xtext.diagnostics.Diagnostic> errors = new LinkedHashSet();
			ISyntacticElement result = GamlCompatibilityConverter.buildSyntacticContents(getContents().get(0), errors);
			getErrors().addAll(errors);
			return result;
		}

		return parseResult.getSyntacticContents();
	}

}
