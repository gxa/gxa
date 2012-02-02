package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Ontology;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

/**
 * @author Misha Kapushesky
 */
public class ApiOntologyTerm {
    private ApiOntology ontology;
    private OntologyTerm ontologyTerm;

    public ApiOntologyTerm() {
    }

    public ApiOntologyTerm(final OntologyTerm ontologyTerm) {
        this.ontologyTerm = ontologyTerm;
        this.ontology = new ApiOntology(ontologyTerm.getOntology());
    }

    public ApiOntology getOntology() {
        return ontology;
    }

    public String getAccession() {
        return ontologyTerm.getAccession();
    }

    public void setAccession(String accession) {
        this.ontologyTerm.setAccession(accession);
    }

    public String getDescription() {
        return ontologyTerm.getDescription();
    }

    public void setDescription(String description) {
        this.ontologyTerm.setDescription(description);
    }

    public String getTerm() {
        return ontologyTerm.getTerm();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiOntologyTerm term = (ApiOntologyTerm) o;

        return getAccession() == null ? term.getAccession() == null : getAccession().equals(term.getAccession());
    }

    @Override
    public int hashCode() {
        return getAccession() != null ? getAccession().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ApiOntologyTerm{" +
                "accession=" + getAccession() +
                ", description='" + getDescription() + "'" +
                ", ontology=" + ontology +
                ", term=" + getTerm() +
                '}';
    }
}
