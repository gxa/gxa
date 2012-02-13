package uk.ac.ebi.microarray.atlas.api;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;

/**
 * A minimal version of ApiProperty
 *
 * @author Robert Petryszak
 */
public class ApiShallowProperty {

    private String name;
    private String value;
    private List<String> terms;

    public ApiShallowProperty(final ApiPropertyValue pv, Set<String> terms) {
        name = pv.getProperty().getName();
        value = pv.getValue();
        this.terms = Lists.newArrayList(terms);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public List<String> getTerms() {
        return terms;
    }
}
