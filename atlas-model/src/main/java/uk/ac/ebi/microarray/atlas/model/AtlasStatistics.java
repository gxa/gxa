package uk.ac.ebi.microarray.atlas.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06-Nov-2009
 */
public class AtlasStatistics {
    private Collection<AtlasExperiment> newExperiments;
    private int numExperiments;
    private int numAssays;
    private int numEfvs;
    private String dataRelease;

    public AtlasStatistics(int numExperiments, int numAssays, int numEfvs, String dataRelease) {
        this.dataRelease = dataRelease;
        this.newExperiments = new ArrayList<AtlasExperiment>();
        this.numExperiments = numExperiments;
        this.numAssays = numAssays;
        this.numEfvs = numEfvs;
    }

    public Collection<AtlasExperiment> getNewExperiments() {
        return newExperiments;
    }

    public int getNumExperiments() {
        return numExperiments;
    }

    public int getNumAssays() {
        return numAssays;
    }

    public int getNumEfvs() {
        return numEfvs;
    }

    void addNewExperiment(AtlasExperiment exp) {
        newExperiments.add(exp);
    }

    public String getDataRelease() {
        return dataRelease;
    }
}
