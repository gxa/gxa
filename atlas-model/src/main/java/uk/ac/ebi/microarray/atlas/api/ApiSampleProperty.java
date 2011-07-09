package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.GuavaUtil;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Misha Kapushesky
 */
public class ApiSampleProperty {
    private ApiPropertyValue propertyValue;
    private Set<ApiOntologyTerm> terms;

    public ApiSampleProperty() {}

    public ApiSampleProperty(final ApiPropertyValue apiPropertyValue, final Set<ApiOntologyTerm> terms) {
        this.propertyValue = apiPropertyValue;
        this.terms = terms;
    }
    public ApiSampleProperty(final SampleProperty sampleProperty) {
        this.propertyValue = new ApiPropertyValue(sampleProperty.getPropertyValue());

        this.terms = new HashSet<ApiOntologyTerm>
            (Collections2.transform(sampleProperty.getTerms(),
                    GuavaUtil.instanceTransformer(OntologyTerm.class, ApiOntologyTerm.class)));

    }

    public ApiPropertyValue getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(ApiPropertyValue propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Set<ApiOntologyTerm> getTerms() {
        return terms;
    }

    public void setTerms(Set<ApiOntologyTerm> terms) {
        this.terms = terms;
    }
}
