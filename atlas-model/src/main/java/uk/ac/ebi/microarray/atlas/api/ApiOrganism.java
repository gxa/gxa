package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Organism;

/**
 * @author Misha Kapushesky
 */
public class ApiOrganism {
    private String name;

    public ApiOrganism() {}

    public ApiOrganism(final String name) {
        this.name = name;
    }

    public ApiOrganism(final Organism organism) {
        this.name = organism.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
