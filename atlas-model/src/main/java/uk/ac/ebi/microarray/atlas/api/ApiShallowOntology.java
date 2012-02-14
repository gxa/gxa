package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Ontology;

/**
 * A minimal version of ApiOntology
 *
 * @author Robert Petryszak
 */
public class ApiShallowOntology {
    private Ontology ontology;

    public ApiShallowOntology() {
    }

    public ApiShallowOntology(final Ontology ontology) {
        this.ontology = ontology;
    }

    public String getName() {
        return ontology.getName();
    }
}
