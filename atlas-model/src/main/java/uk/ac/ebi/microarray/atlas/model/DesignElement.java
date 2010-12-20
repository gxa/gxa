package uk.ac.ebi.microarray.atlas.model;

/**
 * @author pashky
 */
public class DesignElement {
    private final String accession;
    private final String name;

    public DesignElement(String accession, String name) {
        this.accession = accession;
        this.name = name;
    }

    public String getAccession() {
        return accession;
    }

    public String getName() {
        return name;
    }
}
