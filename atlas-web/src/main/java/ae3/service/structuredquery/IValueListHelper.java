package ae3.service.structuredquery;

import java.util.Map;

/**
 * @author pashky
 */
public interface IValueListHelper {
    public Map<String, Long> autoCompleteValues(String property, String query, int limit);
    public Iterable<String> listAllValues(String property);
}
