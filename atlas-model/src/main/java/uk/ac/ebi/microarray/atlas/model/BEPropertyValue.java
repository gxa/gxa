package uk.ac.ebi.microarray.atlas.model;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 15/02/2011
 * Time: 10:42
 * To change this template use File | Settings | File Templates.
 */
public class BEPropertyValue {
    private final String value;


    private final String name;


    public BEPropertyValue(String value, String name) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BEPropertyValue that = (BEPropertyValue) o;

        if (!name.equals(that.name)) return false;
        if (!value.equals(that.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
