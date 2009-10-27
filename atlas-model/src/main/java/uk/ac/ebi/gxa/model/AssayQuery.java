package  uk.ac.ebi.gxa.model;

import java.util.Map;
import java.util.Collection;

/**
 * Assay query object
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 2:07:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssayQuery extends AccessionQuery<AssayQuery> {
    private Map<String, Property> properties;

    public Collection<Property> getProperties(){
        return properties.values();
    }

    AssayQuery hasProperty(Property property){
        this.properties.put(property.getAccession(), property);
        return this;
    }
}