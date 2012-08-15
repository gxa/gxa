package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import uk.ac.ebi.gxa.Temporary;

import javax.persistence.*;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class OntologyTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ontologyTermSeq")
    @SequenceGenerator(name = "ontologyTermSeq", sequenceName = "A2_ONTOLOGYTERM_SEQ", allocationSize = 1)
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

    public OntologyTerm(Long id, Ontology ontology, String term, String accession, String description) {
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

    public void setOntology(Ontology ontology) {
        this.ontology = ontology;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OntologyTerm)) return false;

        OntologyTerm that = (OntologyTerm) o;

        if (accession != null ? !accession.equals(that.accession) : that.accession != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return accession != null ? accession.hashCode() : 0;
    }

    // TODO: 4alf: so far it's a String replacement, must be done properly as soon as we have all the values in place
    @Temporary
    @Override
    public String toString() {
        return accession;
    }
}
