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

    public ApiOntology(final String name, final String sourceUri, final String description, final String version) {
        this.name = name;
        this.sourceUri = sourceUri;
        this.description = description;
        this.version = version;
    }

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
}
