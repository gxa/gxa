package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
public class Organism {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organismSeq")
    @SequenceGenerator(name = "organismSeq", sequenceName = "A2_ORGANISM_SEQ")
    private Long organismid;
    private String name;

    Organism() {
    }

    public Organism(Long id, String name) {
        this.organismid = id;
        this.name = name;
    }

    public Long getId() {
        return organismid;
    }

    public String getName() {
        return name;
    }
}
