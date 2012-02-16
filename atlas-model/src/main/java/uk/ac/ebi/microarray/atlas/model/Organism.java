package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
public class Organism implements Comparable<Organism> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organismSeq")
    @SequenceGenerator(name = "organismSeq", sequenceName = "A2_ORGANISM_SEQ", allocationSize = 1)
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

    @Override
    public int compareTo(Organism o) {
        return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Organism organism = (Organism) o;

        if (name != null ? !name.equals(organism.name) : organism.name != null) return false;
        if (organismid != null ? !organismid.equals(organism.organismid) : organism.organismid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = organismid != null ? organismid.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
