package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Ontology;

/**
 * @author Misha Kapushesky
 */
public class ApiOntology {
    private String name;
    private String sourceUri;
    private String description;
    private String version;

    public ApiOntology() {}

    public ApiOntology(final Ontology ontology) {
        this.name = ontology.getName();
        this.sourceUri = ontology.getSourceUri();
        this.description = ontology.getDescription();
        this.version = ontology.getVersion();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ApiOntology{" +
                "name=" + getName() +
                ", description='" + getDescription() + "'" +
                ", sourceUri=" + getSourceUri() +
                ", version=" + getVersion() +
                '}';
    }
}
