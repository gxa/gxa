package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Collections2;
import uk.ac.ebi.gxa.utils.TransformerUtil;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Misha Kapushesky
 */
public class ApiExperiment {
    private String accession;
    private String description;
    private String articleAbstract;
    private String performer;
    private String lab;
    private Date loadDate;
    private String pmid;
    private Collection<ApiAsset> assets;
    private Collection<ApiAssay> assays;
    private Collection<ApiSample> samples;
    private Boolean isPrivate;

    public ApiExperiment() {
    }

    public ApiExperiment(final String accession, final String description, final String performer, final String lab,
                         final Date loadDate, final String pmid, final Collection<ApiAsset> assets,
                         final Collection<ApiAssay> assays, final Collection<ApiSample> samples, final Boolean isPrivate) {
        this.accession = accession;
        this.description = description;
        this.performer = performer;
        this.lab = lab;
        this.loadDate = loadDate;
        this.pmid = pmid;
        this.assets = assets;
        this.assays = assays;
        this.samples = samples;
        this.isPrivate = isPrivate;
    }

    public ApiExperiment(final Experiment experiment) {
        this.accession = experiment.getAccession();
        this.description = experiment.getDescription();
        this.articleAbstract = experiment.getAbstract();
        this.performer = experiment.getPerformer();
        this.lab = experiment.getLab();
        this.loadDate = experiment.getLoadDate();
        this.pmid = experiment.getPubmedId();

        this.assets = Collections2.transform(experiment.getAssets(),
                TransformerUtil.instanceTransformer(Asset.class, ApiAsset.class));

        this.assays = Collections2.transform(experiment.getAssays(),
                TransformerUtil.instanceTransformer(Assay.class, ApiAssay.class));

        this.samples = Collections2.transform(experiment.getSamples(),
                TransformerUtil.instanceTransformer(Sample.class, ApiSample.class));

        this.isPrivate = experiment.isPrivate();
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArticleAbstract() {
        return articleAbstract;
    }

    public void setArticleAbstract(String articleAbstract) {
        this.articleAbstract = articleAbstract;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
    }

    public String getLab() {
        return lab;
    }

    public void setLab(String lab) {
        this.lab = lab;
    }

    public Date getLoadDate() {
        return loadDate;
    }

    public void setLoadDate(Date loadDate) {
        this.loadDate = loadDate;
    }

    public String getPubmedId() {
        return pmid;
    }

    public void setPubmedId(String pmid) {
        this.pmid = pmid;
    }

    public Collection<ApiAsset> getAssets() {
        return assets;
    }

    public void setAssets(List<ApiAsset> assets) {
        this.assets = assets;
    }

    public Collection<ApiAssay> getAssays() {
        return assays;
    }

    public void setAssays(List<ApiAssay> assays) {
        this.assays = assays;
    }

    public Collection<ApiSample> getSamples() {
        return samples;
    }

    public void setSamples(List<ApiSample> samples) {
        this.samples = samples;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
