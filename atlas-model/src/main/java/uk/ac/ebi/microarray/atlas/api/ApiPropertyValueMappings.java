package uk.ac.ebi.microarray.atlas.api;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import javax.annotation.Nonnull;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;

import java.util.*;

/**
 * Utility class to store mappings between property values and ontology terms across all experiment assays/samples
 *
 * @author Robert Petryszak
 */
public class ApiPropertyValueMappings {

    private SortedMap<ApiPropertyValue, SortedSet<String>> pvToTerms;
    private String propertyName;
    private String propertyValue;
    private boolean caseInsensitive;
    private boolean exactMatch;


    public ApiPropertyValueMappings(String propertyName, String propertyValue, boolean caseInsensitive, boolean exactMatch) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.caseInsensitive = caseInsensitive;
        this.exactMatch = exactMatch;

        pvToTerms = new TreeMap<ApiPropertyValue, SortedSet<String>>(new Comparator<ApiPropertyValue>() {
            public int compare(ApiPropertyValue o1, ApiPropertyValue o2) {
                int result = o1.getProperty().getName().compareTo(o2.getProperty().getName());
                return result == 0 ? o1.getValue().compareTo(o2.getValue()) : result;
            }
        });
    }

    private static final Function<ApiOntologyTerm, String> ONTOLOGY_TERM =
            new Function<ApiOntologyTerm, String>() {
                public String apply(@Nonnull ApiOntologyTerm ot) {
                    return ot.getAccession();
                }
            };

    private static final Function<Map.Entry<ApiPropertyValue, SortedSet<String>>, ApiShallowProperty> MAPPINGS =
            new Function<Map.Entry<ApiPropertyValue, SortedSet<String>>, ApiShallowProperty>() {
                public ApiShallowProperty apply(@Nonnull Map.Entry<ApiPropertyValue, SortedSet<String>> entry) {
                    return new ApiShallowProperty(entry.getKey(), entry.getValue());
                }
            };

    public void add(ApiProperty property) {
        ApiPropertyValue pv = property.getPropertyValue();
        if (!pvQualifies(pv))
            return;

        SortedSet<String> terms = pvToTerms.get(pv);
        if (terms == null) {
            terms = new TreeSet<String>();
            pvToTerms.put(property.getPropertyValue(), terms);
        }
        terms.addAll(transform(property.getTerms(), ONTOLOGY_TERM));
    }

    private boolean pvQualifies(ApiPropertyValue pv) {
        if (exactMatch)
            if (caseInsensitive)
                return (Strings.isNullOrEmpty(propertyValue) || pv.getValue().compareToIgnoreCase(propertyValue) == 0)
                        && (Strings.isNullOrEmpty(propertyName) || pv.getProperty().getName().compareToIgnoreCase(propertyName.toUpperCase()) == 0);
            else
                return (Strings.isNullOrEmpty(propertyValue) || pv.getValue().equals(propertyValue)) &&
                        (Strings.isNullOrEmpty(propertyName) || pv.getProperty().getName().equals(propertyName));
        else if (caseInsensitive)
            return (Strings.isNullOrEmpty(propertyValue) || pv.getValue().toUpperCase().contains(propertyValue.toUpperCase())) &&
                    (Strings.isNullOrEmpty(propertyName) || pv.getProperty().getName().toUpperCase().contains(propertyName.toUpperCase()));
        else
            return (Strings.isNullOrEmpty(propertyValue) || pv.getValue().contains(propertyValue)) &&
                    (Strings.isNullOrEmpty(propertyName) || pv.getProperty().getName().contains(propertyName));
    }

    public Collection<ApiShallowProperty> getAll() {
        return transform(pvToTerms.entrySet(), MAPPINGS);
    }
}
