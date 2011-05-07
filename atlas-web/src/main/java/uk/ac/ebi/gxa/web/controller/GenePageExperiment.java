package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.statistics.EfvAttribute;
import uk.ac.ebi.gxa.statistics.ExperimentInfo;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newTreeSet;

public class GenePageExperiment {
    private static final Logger log = LoggerFactory.getLogger(GenePageExperiment.class);

    private Experiment experiment;
    private ExperimentInfo experimentInfo;

    public GenePageExperiment(Experiment experiment, ExperimentInfo experimentInfo) {
        this.experiment = experiment;
        this.experimentInfo = experimentInfo;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public String getHighestRankEF() {
        EfvAttribute efAttr = experimentInfo.getHighestRankAttribute();
        if (efAttr != null && efAttr.getEf() != null) {
            return efAttr.getEf();
        } else {
            log.error("Failed to find highest rank attribute in: " + experimentInfo);
            return null;
        }
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

    public Date getReleaseDate() {
        return experiment.getReleaseDate();
    }

    public Long getPubmedId() {
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
        Set<String> result = newTreeSet();
        for (Assay assay : experiment.getAssays()) {
            result.addAll(assay.getPropertyNames());
        }
        return result;
    }
}
