package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import uk.ac.ebi.gxa.Temporary;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
public class OntologyTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ontologyTermSeq")
    @SequenceGenerator(name = "ontologyTermSeq", sequenceName = "A2_ONTOLOGYTERM_SEQ", allocationSize = 1)
    private Long ontologytermid;
    @ManyToOne
    private Ontology ontology;
    private String accession;
    private String description;

    OntologyTerm() {
    }

    public OntologyTerm(long id, Ontology ontology, String accession, String description) {
        this.ontologytermid = id;
        this.ontology = ontology;
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

    // TODO: 4alf: so far it's a String replacement, must be done properly as soon as we have all the values in place
    @Temporary
    @Override
    public String toString() {
        return accession;
    }
}
