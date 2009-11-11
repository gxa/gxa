package  uk.ac.ebi.gxa.model;

import java.util.List;
import java.util.ArrayList;

/**
 * Retrieving list of properties. 
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 9:45:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class AbstractPropertyQuery<T> extends AccessionQuery<T>{
     private List<String> values = new ArrayList<String>();
    private List<String> fullTextQueries = new ArrayList<String>();

    public T hasValue(String value) {
        values.add(value);
        return (T)this;
    }


    public T fullTextQuery(String keyword) {
        fullTextQueries.add(keyword);
        return (T)this;
    }

    public List<String> getValues() {
        return values;
    }

    public List<String> getFullTextQueries() {
        return fullTextQueries;
    }

}
