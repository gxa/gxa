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

    public AssayQuery(){};

    public AssayQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }
    //AZ:10-Nov : lets have PropertyQuery member
    //private Map<String, Property> properties;
    private PropertyQuery propertyQuery;

    public PropertyQuery getPropertyQuery(){
        return propertyQuery;
    }

    public AssayQuery hasProperty(PropertyQuery propertyQuery){
        this.propertyQuery = propertyQuery; 
        return this;
    }
}