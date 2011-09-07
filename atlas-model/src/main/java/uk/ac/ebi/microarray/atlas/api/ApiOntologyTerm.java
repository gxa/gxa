package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

/**
 * @author Misha Kapushesky
 */
public class ApiOntologyTerm {
    private ApiOntology ontology;
    private String accession;
    private String description;
    private String term;

    public ApiOntologyTerm() {
    }

    public ApiOntologyTerm(final ApiOntology ontology, final String accession, final String description, final String term) {
        this.ontology = ontology;
        this.accession = accession;
        this.description = description;
        this.term = term;
    }

    public ApiOntologyTerm(final OntologyTerm ontologyTerm) {
        this.accession = ontologyTerm.getAccession();
        this.description = ontologyTerm.getDescription();
        this.term = ontologyTerm.getTerm();
        this.ontology = new ApiOntology(ontologyTerm.getOntology());
    }

    public ApiOntology getOntology() {
        return ontology;
    }

    public void setOntology(ApiOntology ontology) {
        this.ontology = ontology;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiOntologyTerm term = (ApiOntologyTerm) o;

        return accession == null ? term.accession == null : accession.equals(term.accession);
    }

    @Override
    public int hashCode() {
        return accession != null ? accession.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ApiOntologyTerm{" +
                "accession=" + accession +
                ", description='" + description + "'" +
                ", ontology=" + ontology +
                ", term=" + term +
                '}';
    }
}
