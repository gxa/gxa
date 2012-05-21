package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.PropertyValue;

/**
 * @author Misha Kapushesky
 */
public class ApiPropertyValue {
    private ApiPropertyName property;
    private String value;

    public ApiPropertyValue() {}

    public ApiPropertyValue(final ApiPropertyName apiProperty, final String value) {
        this.property = apiProperty;
        this.value = value;
    }

    public ApiPropertyValue(final PropertyValue propertyValue) {
        this.property = new ApiPropertyName(propertyValue.getDefinition());
        this.value = propertyValue.getValue();
    }

    public ApiPropertyName getProperty() {
        return property;
    }

    public void setProperty(ApiPropertyName property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        return "ApiPropertyValue{" + "property='" + getProperty() + "', value='" + getValue() + "'}";
    }
}
