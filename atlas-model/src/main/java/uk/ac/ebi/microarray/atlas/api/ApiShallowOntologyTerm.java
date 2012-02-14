package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

/**
 * A minimal version of ApiOntology term
 *
 * @author Robert Petryszak
 */
public class ApiShallowOntologyTerm {

    private ApiShallowOntology apiShallowOntology;
    private OntologyTerm ontologyTerm;

    public ApiShallowOntologyTerm() {
    }

    public ApiShallowOntologyTerm(final OntologyTerm ontologyTerm) {
        this.ontologyTerm = ontologyTerm;
        this.apiShallowOntology = new ApiShallowOntology(ontologyTerm.getOntology());

    }

    public ApiShallowOntology getOntology() {
        return apiShallowOntology;
    }

    public String getAccession() {
        return ontologyTerm.getAccession();
    }
}
