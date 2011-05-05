package uk.ac.ebi.microarray.atlas.model;

/**
 */
public class Ontology {
    private Long id;
    private String name;
    private String sourceUri;
    private String description;
    private String version;

    public Ontology(Long id, String name, String sourceUri, String description, String version) {
        this.id = id;
        this.name = name;
        this.sourceUri = sourceUri;
        this.description = description;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }
}
