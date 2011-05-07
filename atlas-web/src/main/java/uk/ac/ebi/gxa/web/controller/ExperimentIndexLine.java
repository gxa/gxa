package uk.ac.ebi.gxa.web.controller;

import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExperimentIndexLine {
    private Experiment experiment;

    public ExperimentIndexLine(Experiment experiment) {
        this.experiment = experiment;
    }

    public String getAccession() {
        return experiment.getAccession();
    }

    public Long getPubmedId() {
        return experiment.getPubmedId();
    }

    public Set<String> getExperimentFactors() {
        final Set<String> result = new LinkedHashSet<String>();
        for (Assay assay : experiment.getAssays()) {
            result.addAll(assay.getPropertyNames());
        }
        return result;
    }

    public int getNumSamples() {
        return experiment.getSamples().size();
    }

    public Date getLoadDate() {
        return experiment.getLoadDate();
    }

    public String getDescription() {
        return experiment.getDescription();
    }
}
