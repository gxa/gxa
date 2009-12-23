package ae3.service.structuredquery;

import java.util.Map;
import java.util.Collection;

/**
 * Interface for value listers, allowing to listing and autocompletion of values for gene properties or EFs
 * @author pashky
 */
public interface AutoCompleter {
    /**
     * Auto-completion helper method, returning list of matching values and counters for specific factor/property and prefix
     * @param property factor or property to autocomplete values for, can be empty for any factor
     * @param query prefix
     * @param limit maximum number of values to find
     * @return map of values and counters
     */
    public Collection<AutoCompleteItem> autoCompleteValues(String property, String query, int limit, Map<String,String> filters);

    /**
     * Utility method called after creation to populate internal data structures with cached values
     * (optional)
     */
    public void preloadData();
}
