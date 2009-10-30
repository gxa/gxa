import uk.ac.ebi.gxa.model.PropertyCollection;
import uk.ac.ebi.gxa.model.Property;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 30, 2009
 * Time: 11:43:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasPropertyCollection implements PropertyCollection {
    public AtlasPropertyCollection(Collection<Property> properties){
        this.properties = properties;
    }

    private Collection<Property> properties;

    public Collection<Property> getProperties(){
        return this.properties;
    }

    public Property getByName(String name){
        for(Property p : properties){
            if(name == p.getName()){
                return p;
            }
        }
        return null;
    }

}
