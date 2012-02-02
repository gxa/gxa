package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Ontology;

/**
 * @author Misha Kapushesky
 */
public class ApiOntology {
    private Ontology ontology;

    public ApiOntology() {
    }

    public ApiOntology(final Ontology ontology) {
        this.ontology = ontology;
    }

    public String getName() {
        return ontology.getName();
    }

    public void setName(String name) {
        this.ontology.setName(name);
    }

    public String getSourceUri() {
        return ontology.getSourceUri();
    }

    public String getDescription() {
        return ontology.getDescription();
    }

    public void setDescription(String description) {
        this.ontology.setDescription(description);
    }

    public String getVersion() {
        return ontology.getVersion();
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
