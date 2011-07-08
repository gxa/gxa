package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.Property;

/**
 * @author Misha Kapushesky
 */
public class ApiProperty {
    private String name;

    public ApiProperty() {}

    public ApiProperty(final String name) {
        this.name = name;
    }

    public ApiProperty(final Property definition) {
        this.name = definition.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
