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
    private Collection<ApiProperty> properties;

    public ApiSample() {}

    public ApiSample(final Sample sample) {
        this.accession = sample.getAccession();
        this.channel = sample.getChannel();

        this.assays = Collections2.transform(sample.getAssays(),
                TransformerUtil.instanceTransformer(Assay.class, ApiAssay.class));

        this.properties = Collections2.transform(sample.getProperties(),
                TransformerUtil.instanceTransformer(SampleProperty.class, ApiProperty.class));

    }

    public String getAccession() {
        return accession;
    }

    public ApiOrganism getOrganism() {
        return organism;
    }

    public String getChannel() {
        return channel;
    }

    public Collection<ApiProperty> getProperties() {
        return properties;
    }

    public Collection<ApiAssay> getAssays() {
        return assays;
    }
}
