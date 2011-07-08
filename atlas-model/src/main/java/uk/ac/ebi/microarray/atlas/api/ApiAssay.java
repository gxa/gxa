package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.GuavaUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Collection;

/**
 * @author Misha Kapushesky
 */
public class ApiAssay {
    private String accession;
    private ArrayDesign arrayDesign;
    private Collection<ApiSample> samples;
    private Collection<ApiAssayProperty> properties;

    public ApiAssay(final Assay assay) {
        this.accession = assay.getAccession();
        this.arrayDesign = new ApiArrayDesign(assay.getArrayDesign());

        this.samples = Collections2.transform(assay.getSamples(),
                GuavaUtil.instanceTransformer(Sample.class, ApiSample.class));

        this.properties = Collections2.transform(assay.getProperties(),
                GuavaUtil.instanceTransformer(AssayProperty.class, ApiAssayProperty.class));
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign(ArrayDesign arrayDesign) {
        this.arrayDesign = arrayDesign;
    }

    public Collection<ApiSample> getSamples() {
        return samples;
    }

    public void setSamples(Collection<ApiSample> samples) {
        this.samples = samples;
    }

    public Collection<ApiAssayProperty> getProperties() {
        return properties;
    }

    public void setProperties(Collection<ApiAssayProperty> properties) {
        this.properties = properties;
    }
}
