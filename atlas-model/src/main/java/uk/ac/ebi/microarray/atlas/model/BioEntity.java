package uk.ac.ebi.microarray.atlas.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 02/02/2011
 * Time: 12:47
 * To change this template use File | Settings | File Templates.
 */
public class BioEntity {
    private long id;
    private String identifier;
    private String type;
    private List<Property> properties;

    private String organism;

    public BioEntity(String identifier) {
        this.identifier = identifier;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public boolean addProperty(Property p) {
        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        return properties.add(p);
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioEntity bioEntity = (BioEntity) o;

        if (!identifier.equals(bioEntity.identifier)) return false;
        if (organism != null ? !organism.equals(bioEntity.organism) : bioEntity.organism != null) return false;
        if (type != null ? !type.equals(bioEntity.type) : bioEntity.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (organism != null ? organism.hashCode() : 0);
        return result;
    }
}
