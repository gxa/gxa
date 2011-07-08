package uk.ac.ebi.microarray.atlas.api;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.GuavaUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Misha Kapushesky
 */
public class ApiAssayProperty {
    private ApiPropertyValue propertyValue;
    private Set<ApiOntologyTerm> terms;

    public ApiAssayProperty() {}

    public ApiAssayProperty(final ApiPropertyValue apiPropertyValue, final Set<ApiOntologyTerm> terms) {
        this.propertyValue = apiPropertyValue;
        this.terms = terms;
    }

    public ApiAssayProperty(final AssayProperty assayProperty) {
        this.propertyValue = new ApiPropertyValue(assayProperty.getPropertyValue());

        this.terms = new TreeSet<ApiOntologyTerm>
            (Collections2.transform(assayProperty.getTerms(),
                GuavaUtil.instanceTransformer(OntologyTerm.class, ApiOntologyTerm.class)));
    }

    public ApiPropertyValue getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(ApiPropertyValue propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Collection<ApiOntologyTerm> getTerms() {
        return terms;
    }

    public void setTerms(Set<ApiOntologyTerm> terms) {
        this.terms = terms;
    }
}
