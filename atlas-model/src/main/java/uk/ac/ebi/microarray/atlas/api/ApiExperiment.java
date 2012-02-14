package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.TransformerUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Collection;
import java.util.Date;

/**
 * @author Misha Kapushesky
 */
public class ApiExperiment {
    protected Experiment experiment;

    public ApiExperiment(final Experiment experiment) {
        this.experiment = experiment;
    }

    public String getAccession() {
        return experiment.getAccession();
    }

    public String getDescription() {
        return experiment.getDescription();
    }

    public String getArticleAbstract() {
        return experiment.getAbstract();
    }

    public String getPerformer() {
        return experiment.getPerformer();
    }

    public String getLab() {
        return experiment.getLab();
    }

    public Date getLoadDate() {
        return experiment.getLoadDate();
    }

    public String getPubmedId() {
        return experiment.getPubmedId();
    }

    public Collection<ApiAsset> getAssets() {
        return Collections2.transform(experiment.getAssets(),
                TransformerUtil.instanceTransformer(Asset.class, ApiAsset.class));
    }

    public Collection<ApiAssay> getAssays() {
        return Collections2.transform(experiment.getAssays(),
                TransformerUtil.instanceTransformer(Assay.class, ApiAssay.class));
    }


    public Collection<ApiSample> getSamples() {
        return Collections2.transform(experiment.getSamples(),
                TransformerUtil.instanceTransformer(Sample.class, ApiSample.class));
    }

    public boolean isPrivate() {
        return experiment.isPrivate();
    }

}
