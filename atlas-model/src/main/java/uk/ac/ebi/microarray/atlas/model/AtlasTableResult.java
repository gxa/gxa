package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06-Nov-2009
 */
public class AtlasTableResult {
    private int experimentID;
    private int geneID;
    private String property;
    private String propertyValue;
    private String upOrDown;
    private double pValAdj;

    public int getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(int experimentID) {
        this.experimentID = experimentID;
    }

    public int getGeneID() {
        return geneID;
    }

    public void setGeneID(int geneID) {
        this.geneID = geneID;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getUpOrDown() {
        return upOrDown;
    }

    public void setUpOrDown(String upOrDown) {
        this.upOrDown = upOrDown;
    }

    public double getPValAdj() {
        return pValAdj;
    }

    public void setPValAdj(double pValAdj) {
        this.pValAdj = pValAdj;
    }
}
