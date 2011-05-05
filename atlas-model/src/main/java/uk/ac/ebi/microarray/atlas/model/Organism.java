package uk.ac.ebi.microarray.atlas.model;

public class Organism {
    private Long id;
    private String name;

    public Organism(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
