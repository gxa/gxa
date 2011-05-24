package uk.ac.ebi.microarray.atlas.model.bioentity;

/**
 * User: nsklyar
 * Date: 23/05/2011
 */
public class BioMartProperty {
    private Long id;
    private String propertyName;
    private String biomartPropertyName;

    public BioMartProperty(Long id, String propertyName, String biomartPropertyName) {
        this.id = id;
        this.propertyName = propertyName;
        this.biomartPropertyName = biomartPropertyName;
    }

    public Long getId() {
        return id;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getBiomartPropertyName() {
        return biomartPropertyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioMartProperty that = (BioMartProperty) o;

        if (biomartPropertyName != null ? !biomartPropertyName.equals(that.biomartPropertyName) : that.biomartPropertyName != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (propertyName != null ? propertyName.hashCode() : 0);
        result = 31 * result + (biomartPropertyName != null ? biomartPropertyName.hashCode() : 0);
        return result;
    }
}
