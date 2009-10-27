package  uk.ac.ebi.gxa.model;

import java.util.Collection;

/**
 * Collection of properties plus shortcuts for quick access.
 * User: Andrey
 * Date: Oct 26, 2009
 * Time: 5:04:13 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PropertyCollection {
    public Collection<Property> getProperties();

    public Property getByName(String name);


}
