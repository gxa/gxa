package ae3.service.structuredquery;

import java.util.Collection;
import java.util.Map;

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
     * @param filters custom filters for results. implementation defines handling (if any)
     * @return map of values and counters
     */
    public Collection<AutoCompleteItem> autoCompleteValues(String property, String query, int limit, Map<String,String> filters);
}
