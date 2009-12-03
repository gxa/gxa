package uk.ac.ebi.gxa.model.impl;

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

    public Property getByAccession(String accession){
        for(Property p : properties){
            if(p.getAccession().equals(accession)){
                return p;
            }
        }
        return null;
    }

}
