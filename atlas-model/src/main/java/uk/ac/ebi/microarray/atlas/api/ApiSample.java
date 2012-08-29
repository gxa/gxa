package uk.ac.ebi.microarray.atlas.api;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author Misha Kapushesky
 */
public class ApiSample {
    private String accession;
    private ApiOrganism organism;
    private String channel;
    private Collection<ApiProperty> properties;

    public ApiSample() {
    }

    public ApiSample(final Sample sample) {
        this.accession = sample.getAccession();
        this.channel = sample.getChannel();

        this.properties = Collections2.transform(sample.getProperties(), new Function<SampleProperty, ApiProperty>() {
            @Override
            public ApiProperty apply(@Nullable SampleProperty sampleProperty) {
                return new ApiProperty(sampleProperty.getPropertyValue(), sampleProperty.getTerms());
            }
        });

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

}
