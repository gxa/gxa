package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.statistics.EfAttribute;
import uk.ac.ebi.gxa.statistics.ExperimentResult;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.Set;

public class GenePageExperiment {
    private static final Logger log = LoggerFactory.getLogger(GenePageExperiment.class);

    private Experiment experiment;
    private ExperimentResult experimentInfo;

    public GenePageExperiment(Experiment experiment, ExperimentResult experimentInfo) {
        this.experiment = experiment;
        this.experimentInfo = experimentInfo;
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

    public String getPubmedId() {
        return experiment.getPubmedId();
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
