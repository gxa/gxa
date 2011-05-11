package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Organism {
    @Id
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
