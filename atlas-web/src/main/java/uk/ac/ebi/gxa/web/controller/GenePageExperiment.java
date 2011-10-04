package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.statistics.EfAttribute;
import uk.ac.ebi.gxa.statistics.ExperimentResult;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class GenePageExperiment {
    private static final Logger log = LoggerFactory.getLogger(GenePageExperiment.class);

    private Experiment experiment;
    private ExperimentResult experimentInfo;

    public GenePageExperiment(Experiment experiment, ExperimentResult experimentInfo) {
        this.experiment = experiment;
        this.experimentInfo = experimentInfo;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public EfAttribute getHighestRankAttribute() {
        EfAttribute attribute = experimentInfo.getHighestRankAttribute();
        if (attribute == null || attribute.getEf() == null) {
            log.error("Failed to find highest rank attribute in: " + experimentInfo);
        }
        return attribute;
    }

    public String getAccession() {
        return experiment.getAccession();
    }

    public long getId() {
        return experiment.getId();
    }

    public String getDescription() {
        return experiment.getDescription();
    }

    public String getAbstract() {
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

    public List<Asset> getAssets() {
        return experiment.getAssets();
    }

    public List<Assay> getAssays() {
        return experiment.getAssays();
    }

    public List<Sample> getSamples() {
        return experiment.getSamples();
    }

    public List<String> getSpecies() {
        return experiment.getSpecies();
    }

    public boolean isPrivate() {
        return experiment.isPrivate();
    }

    public boolean isCurated() {
        return experiment.isCurated();
    }

    /**
     * Returns set of experiment factors
     *
     * @return all factors from the experiment
     */
    public Set<String> getExperimentFactors() {
        return experiment.getExperimentFactors();
    }
}
