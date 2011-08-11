package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.PropertyValue;

/**
 * @author Misha Kapushesky
 */
public class ApiPropertyValue {
    private ApiProperty property;
    private String value;

    public ApiPropertyValue() {}

    public ApiPropertyValue(final ApiProperty apiProperty, final String value) {
        this.property = apiProperty;
        this.value = value;
    }

    public ApiPropertyValue(final PropertyValue propertyValue) {
        this.property = new ApiProperty(propertyValue.getDefinition());
        this.value = propertyValue.getValue();
    }

    public ApiProperty getProperty() {
        return property;
    }

    public void setProperty(ApiProperty property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
