package uk.ac.ebi.microarray.atlas.model;

/**
 * @author pashky
 */
public class DesignElement {
    private final long id;
    private final long arrayDesignId;
    private final String accession;
    private final String name;

    public DesignElement(long id, long arrayDesignId, String accession, String name) {
        this.id = id;
        this.arrayDesignId = arrayDesignId;
        this.accession = accession;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public long getArrayDesignId() {
        return arrayDesignId;
    }

    public String getAccession() {
        return accession;
    }

    public String getName() {
        return name;
    }
}
