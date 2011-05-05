package uk.ac.ebi.microarray.atlas.model;

import uk.ac.ebi.gxa.Temporary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 */
public class OntologyTerm {
    private Long id;
    private Ontology ontology;
    private String accession;
    private String description;
    private String term;

    private OntologyTerm(String name) {
        term = name;
    }

    // TODO: 4alf: just a temporary solution, get rid of it at the earliest convenience
    @Temporary
    public static List<OntologyTerm> parseTerms(String names) {
        if (names == null)
            return Collections.emptyList();

        List<OntologyTerm> result = new ArrayList<OntologyTerm>();
        for (String name : names.split(",")) {
            if (!isNullOrEmpty(name))
                result.add(new OntologyTerm(name));
        }
        return result;
    }

    // TODO: 4alf: so far it's a String replacement, must be done properly as soon as we have all the values in place
    @Temporary
    @Override
    public String toString() {
        return term;
    }
}
