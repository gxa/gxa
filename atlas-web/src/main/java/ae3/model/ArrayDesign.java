package ae3.model;

import ae3.restresult.RestOut;

/**
 * Class, representing array design for {@link ae3.model.ExperimentalData} object
 * @author pashky
 */
public class ArrayDesign {
    private String accession;

    /**
     * Constructor
     * @param accession array design accession string
     */
    public ArrayDesign(String accession) {
        this.accession = accession;
    }

    /**
     * Gets accession string
     * @return accession string
     */
    @RestOut(name="accession")
    public String getAccession() {
        return accession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArrayDesign that = (ArrayDesign) o;

        if (!accession.equals(that.accession)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return accession.hashCode();
    }

    @Override
    public String toString() {
        return "ArrayDesign{" + accession + '}';
    }
}
