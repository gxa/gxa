package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.TransformerUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Collection;

/**
 * A minimal version of ApiExperiment
 *
 * @author Robert Petryszak
 */
public class ApiShallowExperiment {
    private Experiment experiment;

    public ApiShallowExperiment(final Experiment experiment) {
        this.experiment = experiment;

    }

    public String getAccession() {
        return experiment.getAccession();
    }


    public Collection<ApiShallowAssay> getAssays() {
        return Collections2.transform(experiment.getAssays(),
                TransformerUtil.instanceTransformer(Assay.class, ApiShallowAssay.class));
    }


    public Collection<ApiShallowSample> getSamples() {
        return Collections2.transform(experiment.getSamples(),
                TransformerUtil.instanceTransformer(Sample.class, ApiShallowSample.class));
    }


}
