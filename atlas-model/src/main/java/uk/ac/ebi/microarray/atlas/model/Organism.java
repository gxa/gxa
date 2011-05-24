package uk.ac.ebi.microarray.atlas.model;

/**
 * User: nsklyar
 * Date: 04/05/2011
 */
public class Organism {
    private Long organismid;
    private String name;

    Organism() {
    }

    public Organism(Long id, String name) {
        this.organismid = id;
        this.name = name;
    }

    public Organism(String name) {
        this.name = name;
    }

    public Long getId() {
        return organismid;
    }

    public void setId(long organismid) {
        this.organismid = organismid;
    }

    public String getName() {
        return name;
    }


}
