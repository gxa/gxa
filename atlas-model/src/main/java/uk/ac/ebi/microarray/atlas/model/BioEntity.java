package uk.ac.ebi.microarray.atlas.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 02/02/2011
 * Time: 12:47
 * To change this template use File | Settings | File Templates.
 */
public class BioEntity {
    private long id;
    private String identifier;
    private String type;
    private List<Property> properties;
    private String PropertyString;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public boolean addProperty(Property p) {
        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        return properties.add(p);
    }

    public String getPropertyString() {
        return PropertyString;
    }

    public void setPropertyString(String propertyString) {
        PropertyString = propertyString;
    }
}
