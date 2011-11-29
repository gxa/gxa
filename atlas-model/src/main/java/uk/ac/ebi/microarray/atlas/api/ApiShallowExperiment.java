package uk.ac.ebi.microarray.atlas.api;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import uk.ac.ebi.gxa.utils.TransformerUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import javax.annotation.Nonnull;
import java.util.Collection;

import static com.google.common.collect.Collections2.transform;

/**
 * @author Robert Petryszak
 *         <p/>
 *         A minimal version of ApiExperiment
 */
public class ApiShallowExperiment {

    private static final Function<ApiAssay, String> ASSAY =
            new Function<ApiAssay, String>() {
                @Override
                public String apply(@Nonnull ApiAssay input) {
                    return input.getAccession();
                }
            };

    private static final Function<ApiSample, String> SAMPLE =
            new Function<ApiSample, String>() {
                @Override
                public String apply(@Nonnull ApiSample input) {
                    return input.getAccession();
                }
            };

    private String accession;
    private Collection<String> assayAccessions;
    private Collection<String> sampleAccessions;

    public ApiShallowExperiment() {
    }

    public ApiShallowExperiment(final String accession, final Collection<ApiAssay> assays, final Collection<ApiSample> samples) {
        this.accession = accession;
        populateAssayAccessions(assays);
        populateSampleAccessions(samples);
    }

    public ApiShallowExperiment(final Experiment experiment) {
        this.accession = experiment.getAccession();
        populateAssayAccessions(Collections2.transform(experiment.getAssays(),
                TransformerUtil.instanceTransformer(Assay.class, ApiAssay.class)));
        populateSampleAccessions(Collections2.transform(experiment.getSamples(),
                TransformerUtil.instanceTransformer(Sample.class, ApiSample.class)));

    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public Collection<String> getAssayAccessions() {
        return assayAccessions;
    }

    public Collection<String> getSampleAccessions() {
        return sampleAccessions;
    }

    private void populateAssayAccessions(final Collection<ApiAssay> assays) {
        this.assayAccessions = Lists.newArrayList(transform(assays, ASSAY));
    }

    private void populateSampleAccessions(final Collection<ApiSample> samples) {
        this.sampleAccessions = Lists.newArrayList(transform(samples, SAMPLE));
    }
}
