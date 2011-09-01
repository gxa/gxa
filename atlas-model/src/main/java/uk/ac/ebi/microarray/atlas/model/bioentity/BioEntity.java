package uk.ac.ebi.microarray.atlas.model.bioentity;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.microarray.atlas.model.Organism;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class BioEntity {
    private Long id;
    private String identifier;
    private String name;
    private BioEntityType type;
    private List<BEPropertyValue> properties = new ArrayList<BEPropertyValue>();

    private Organism organism;

    public BioEntity(String identifier, String name, BioEntityType type, Organism organism) {
        this.identifier = identifier;
        this.name = name;
        this.type = type;
        this.organism = organism;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BioEntityType getType() {
        return type;
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

    public Organism getOrganism() {
        return organism;
    }


    public String getName() {
        if (StringUtils.isEmpty(name)){
            name = identifier;
        }
        return name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioEntity bioEntity = (BioEntity) o;

        if (!identifier.equals(bioEntity.identifier)) return false;
        if (name != null ? !name.equals(bioEntity.name) : bioEntity.name != null) return false;
        if (!organism.equals(bioEntity.organism)) return false;
        if (!type.equals(bioEntity.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + type.hashCode();
        result = 31 * result + organism.hashCode();
        return result;
    }
}
