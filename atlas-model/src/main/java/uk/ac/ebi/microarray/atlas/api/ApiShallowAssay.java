package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.TransformerUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;

import java.util.Collection;

/**
 * A minimal version of ApiAssay
 *
 * @author Robert Petryszak
 */
public class ApiShallowAssay {

    private String accession;
    private ApiArrayDesign arrayDesign;
    private Collection<ApiShallowProperty> properties;

    public ApiShallowAssay() {
    }

    public ApiShallowAssay(final Assay assay) {
        this.accession = assay.getAccession();
        this.arrayDesign = new ApiArrayDesign(assay.getArrayDesign());

        this.properties = Collections2.transform(assay.getProperties(),
                TransformerUtil.instanceTransformer(AssayProperty.class, ApiShallowProperty.class));

    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public ApiArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public Collection<ApiShallowProperty> getProperties() {
        return properties;
    }
}
