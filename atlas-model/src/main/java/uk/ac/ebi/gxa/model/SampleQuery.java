package  uk.ac.ebi.gxa.model;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 2:06:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SampleQuery extends AccessionQuery<SampleQuery>{
    private PropertyQuery propertyQuery;

    public SampleQuery hasProperty(PropertyQuery propertyQuery){
        return this;
    }

    public PropertyQuery getPropertyQuery(){
        return this.propertyQuery;
    }
}