package uk.ac.ebi.gxa.web.controller;

import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Date;
import java.util.HashSet;
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
        final Set<String> accessions = new HashSet<String>();
        for (Assay assay : experiment.getAssays()) {
            for (Sample sample : assay.getSamples()) {
                accessions.add(sample.getAccession());
            }
        }
        return accessions.size();
    }

    public Date getLoadDate() {
        return experiment.getLoadDate();
    }

    public String getDescription() {
        return experiment.getDescription();
    }
}
