package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Property;

/**
 * @author Misha Kapushesky
 */
public class ApiPropertyName {
        private String name;

    public ApiPropertyName() {}

    public ApiPropertyName(final String name) {
        this.name = name;
    }

    public ApiPropertyName(final Property definition) {
        this.name = definition.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
