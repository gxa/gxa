package uk.ac.ebi.gxa.tasks;

import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Asset;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Date;
import java.util.List;

public class ExperimentLine {
    private final Experiment experiment;
    private final boolean analyticsComplete;
    private final boolean netcdfComplete;
    private final boolean indexComplete;

    public ExperimentLine(Experiment experiment, boolean analyticsComplete, boolean netcdfComplete, boolean indexComplete) {
        this.experiment = experiment;
        this.analyticsComplete = analyticsComplete;
        this.netcdfComplete = netcdfComplete;
        this.indexComplete = indexComplete;
    }

    public String getAccession() {
        return experiment.getAccession();
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

    public boolean isAnalyticsComplete() {
        return analyticsComplete;
    }

    public boolean isNetcdfComplete() {
        return netcdfComplete;
    }

    public boolean isIndexComplete() {
        return indexComplete;
    }
}
