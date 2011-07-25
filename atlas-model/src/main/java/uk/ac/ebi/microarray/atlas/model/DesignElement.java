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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DesignElement that = (DesignElement) o;

        if (!accession.equals(that.accession)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return accession.hashCode();
    }
}
