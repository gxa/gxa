package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.TransformerUtil;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.Collection;

/**
 * A minimal version of ApiSample
 *
 * @author Robert Petryszak
 */
public class ApiShallowSample {

    private String accession;
    private Collection<ApiShallowProperty> properties;

    public ApiShallowSample() {
    }

    public ApiShallowSample(final Sample sample) {
        this.accession = sample.getAccession();
        this.properties = Collections2.transform(sample.getProperties(),
                TransformerUtil.instanceTransformer(SampleProperty.class, ApiShallowProperty.class));

    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public Collection<ApiShallowProperty> getProperties() {
        return properties;
    }
}
