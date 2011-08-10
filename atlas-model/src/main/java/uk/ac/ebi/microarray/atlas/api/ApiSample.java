package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.TransformerUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.Collection;
import java.util.List;

/**
 * @author Misha Kapushesky
 */
public class ApiSample {
    private String accession;
    private ApiOrganism organism;
    private String channel;
    private Collection<ApiAssay> assays;
    private Collection<ApiSampleProperty> properties;

    public ApiSample() {}

    public ApiSample(final String accession, final ApiOrganism organism, final String channel,
                     final Collection<ApiAssay> assays, final Collection<ApiSampleProperty> properties) {
        this.accession = accession;
        this.organism = organism;
        this.channel = channel;
        this.assays = assays;
        this.properties = properties;
    }

    public ApiSample(final Sample sample) {
        this.accession = sample.getAccession();
        this.channel = sample.getChannel();

        this.assays = Collections2.transform(sample.getAssays(),
                TransformerUtil.instanceTransformer(Assay.class, ApiAssay.class));

        this.properties = Collections2.transform(sample.getProperties(),
                TransformerUtil.instanceTransformer(SampleProperty.class, ApiSampleProperty.class));

    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public ApiOrganism getOrganism() {
        return organism;
    }

    public void setOrganism(ApiOrganism organism) {
        this.organism = organism;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Collection<ApiAssay> getAssays() {
        return assays;
    }

    public void setAssays(Collection<ApiAssay> assays) {
        this.assays = assays;
    }

    public Collection<ApiSampleProperty> getProperties() {
        return properties;
    }

    public void setProperties(Collection<ApiSampleProperty> properties) {
        this.properties = properties;
    }
}
