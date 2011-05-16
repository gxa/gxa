package uk.ac.ebi.microarray.atlas.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class BioEntity {
    private long id;
    private String identifier;
    private String name;
    private String type;
    private List<BEPropertyValue> properties = new ArrayList<BEPropertyValue>();

    private String species;

    public static final String NAME_PROPERTY_SYMBOL = "Symbol";
    public static final String NAME_PROPERTY_MIRBASE = "miRBase: Accession Number";

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

    public List<BEPropertyValue> getProperties() {
        return unmodifiableList(properties);
    }

    public boolean addProperty(BEPropertyValue p) {
        return properties.add(p);
    }

    public void clearProperties() {
        properties.clear();
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getName() {
        if (StringUtils.isEmpty(name)){
            name = identifier;
            for (BEPropertyValue property : properties) {
                if (NAME_PROPERTY_SYMBOL.equalsIgnoreCase(property.getName())) {
                    name = property.getValue();
                    break;
                } else if (NAME_PROPERTY_MIRBASE.equalsIgnoreCase(property.getName())) {
                    name = property.getValue();
                    break;
                }
            }
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioEntity bioEntity = (BioEntity) o;

        if (!identifier.equals(bioEntity.identifier)) return false;
        if (species != null ? !species.equals(bioEntity.species) : bioEntity.species != null) return false;
        if (type != null ? !type.equals(bioEntity.type) : bioEntity.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (species != null ? species.hashCode() : 0);
        return result;
    }
}