package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06-Nov-2009
 */
public class AtlasExperiment {
    private String accession;
    private int assayCount;
    private String descr;

    public AtlasExperiment(String accession, int assayCount, String descr) {
        this.accession = accession;
        this.assayCount = assayCount;
        this.descr = descr;
    }

    public String getAccession() {
        return accession;
    }

    public int getAssayCount() {
        return assayCount;
    }

    public String getDescr() {
        return descr;
    }
}
