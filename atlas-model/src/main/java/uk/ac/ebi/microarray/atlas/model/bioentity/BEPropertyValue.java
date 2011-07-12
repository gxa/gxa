package uk.ac.ebi.microarray.atlas.model.bioentity;

public class BEPropertyValue {
    private Long bepropertyvalueid;
    //    private final String name;
    private String value;
    private BioEntityProperty property;

    private Software software;


    BEPropertyValue() {
    }

    public BEPropertyValue(Long bepropertyvalueid, BioEntityProperty property, String value) {
        this.bepropertyvalueid = bepropertyvalueid;
        this.value = value;
        this.property = property;
    }

    public BEPropertyValue(BioEntityProperty property, String value) {
        this.value = value;
        this.property = property;
    }

    public Long getId() {
        return bepropertyvalueid;
    }

    void setId(Long id) {
        this.bepropertyvalueid = id;
    }

    public BioEntityProperty getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BEPropertyValue that = (BEPropertyValue) o;

        if (property != null ? !property.equals(that.property) : that.property != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (property != null ? property.hashCode() : 0);
        return result;
    }
}
