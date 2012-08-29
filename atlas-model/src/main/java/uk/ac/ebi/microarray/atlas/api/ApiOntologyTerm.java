package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Ontology;
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
                ", ontology=" + getOntology() +
                ", term=" + getTerm() +
                '}';
    }

    public OntologyTerm toOntologyTerm() {
        final Ontology ontology = new Ontology(null, this.ontology.getName(),
                this.ontology.getSourceUri(), this.
                ontology.getDescription(),
                this.ontology.getVersion());

        return new OntologyTerm(null, ontology, this.term, this.accession, this.description);
    }
}
