package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import uk.ac.ebi.gxa.Temporary;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class OntologyTerm {
    @Id
    private Long ontologytermid;
    @ManyToOne
    private Ontology ontology;
    private String accession;
    private String description;
    private String term;

    OntologyTerm() {
    }

    private OntologyTerm(String name) {
        term = name;
    }

    public OntologyTerm(long id, Ontology ontology, String term, String accession, String description) {
        this.ontologytermid = id;
        this.ontology = ontology;
        this.term = term;
        this.accession = accession;
        this.description = description;
    }

    public Long getId() {
        return ontologytermid;
    }

    public Ontology getOntology() {
        return ontology;
    }

    public String getAccession() {
        return accession;
    }

    public String getDescription() {
        return description;
    }

    public String getTerm() {
        return term;
    }

    // TODO: 4alf: so far it's a String replacement, must be done properly as soon as we have all the values in place
    @Temporary
    @Override
    public String toString() {
        return term;
    }
}
