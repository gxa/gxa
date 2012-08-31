package uk.ac.ebi.microarray.atlas.api;

import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility class to store mappings between property values and ontology terms across all experiment assays/samples
 *
 */
public class ApiPropertyValueMatcher {

    SortedSet<ApiShallowProperty> matchingProperties;

    private String nameMatcher;
    private String valueMatcher;

    private boolean caseInsensitive;
    private boolean exactMatch;


    public ApiPropertyValueMatcher() {

        matchingProperties = new TreeSet<ApiShallowProperty>();

        caseInsensitive = true;

        exactMatch = true;

    }


    public ApiPropertyValueMatcher setCaseInsensitive(boolean caseInsensitive){

        this.caseInsensitive = caseInsensitive;
        return this;

    }


    public ApiPropertyValueMatcher setExactMatch(boolean exactMatch){

        this.exactMatch = exactMatch;
        return this;

    }


    public ApiPropertyValueMatcher setNameMatcher(String nameMatcher){

        this.nameMatcher = nameMatcher;
        return this;

    }


    public ApiPropertyValueMatcher setValueMatcher(String valueMatcher){

        this.valueMatcher = valueMatcher;
        return this;

    }


    private static final Function<ApiOntologyTerm, String> ONTOLOGY_TERM =
            new Function<ApiOntologyTerm, String>() {
                public String apply(@Nonnull ApiOntologyTerm ot) {
                    return ot.getAccession();
                }
            };


    public void add(ApiProperty property) {

        if (match(property)) {

            matchingProperties.add(new ApiShallowProperty(property));

        }

    }


    boolean match(ApiProperty property) {

        if (exactMatch){

            return (valueMatcher == null || exactMatch(property.getValue(), valueMatcher))
                    && (nameMatcher == null || exactMatch(property.getName(), nameMatcher));

        } else {

            return (valueMatcher == null || partialMatch(property.getValue(), valueMatcher)) &&
                    (nameMatcher == null || partialMatch(property.getName(), nameMatcher));

        }

    }


    boolean partialMatch(String value, String matcher) {

        String[] matchingTokens = StringUtils.split(matcher, "*");

        for (String matchingToken : matchingTokens) {

            if (!contains(value, matchingToken)){
                return false;
            }

        }

        return true;
    }


    boolean exactMatch(String stringOne, String stringTwo) {

        if (caseInsensitive) {

            return StringUtils.equalsIgnoreCase(stringOne, stringTwo);

        }

        return StringUtils.equals(stringOne, stringTwo);
    }


    boolean contains(String stringValue, String searchString) {

        if (caseInsensitive) {

            return StringUtils.containsIgnoreCase(stringValue, searchString);

        }

        return StringUtils.contains(stringValue, searchString);
    }


    public SortedSet<ApiShallowProperty> getMatchingProperties() {
        return matchingProperties;
    }



    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }


    public boolean isExactMatch() {
        return exactMatch;
    }

}
