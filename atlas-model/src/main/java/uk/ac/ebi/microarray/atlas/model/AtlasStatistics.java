package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06-Nov-2009
 */
public class AtlasStatistics {
    private String dataRelease;
    private int experimentCount;
    private int assayCount;
    private int propertyValueCount;
    private int newExperimentCount;

    public int getExperimentCount() {
        return experimentCount;
    }

    public void setExperimentCount(int experimentCount) {
        this.experimentCount = experimentCount;
    }

    public int getAssayCount() {
        return assayCount;
    }

    public void setAssayCount(int assayCount) {
        this.assayCount = assayCount;
    }

    public int getPropertyValueCount() {
        return propertyValueCount;
    }

    public void setPropertyValueCount(int propertyValueCount) {
        this.propertyValueCount = propertyValueCount;
    }

    public String getDataRelease() {
        return dataRelease;
    }

    public void setDataRelease(String dataRelease) {
        this.dataRelease = dataRelease;
    }

    public int getNewExperimentCount() {
        return newExperimentCount;
    }

    public void setNewExperimentCount(int newExperimentCount) {
        this.newExperimentCount = newExperimentCount;
    }
}
