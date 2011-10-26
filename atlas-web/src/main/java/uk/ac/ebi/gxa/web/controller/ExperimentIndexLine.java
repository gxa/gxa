package uk.ac.ebi.gxa.web.controller;

import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.util.Date;
import java.util.SortedSet;

// mostly used via display tag, which is not tracked by IntelliJ IDEA
@SuppressWarnings("unused")
public class ExperimentIndexLine {
    private Experiment experiment;

    public ExperimentIndexLine(Experiment experiment) {
        this.experiment = experiment;
    }

    public String getAccession() {
        return experiment.getAccession();
    }

    public String getPubmedId() {
        return experiment.getPubmedId();
    }

    public SortedSet<Property> getExperimentFactors() {
        return experiment.getProperties();
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
