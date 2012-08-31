package uk.ac.ebi.microarray.atlas.api;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;


public class ApiShallowProperty implements Comparable<ApiShallowProperty>{


    private static Function<OntologyTerm, String> termToAccession = new OntologyTermToAccession();

    private String name;
    private String value;
    private SortedSet<String> terms;

    private ApiShallowProperty(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public ApiShallowProperty(ApiProperty propertyValue) {
        this(propertyValue.getName(), propertyValue.getValue());

        List<ApiOntologyTerm> ontologyTerms = Lists.newArrayList(propertyValue.getTerms());
        this.terms = Sets.newTreeSet();
        for (ApiOntologyTerm apiOntologyTerm: propertyValue.getTerms()){
            terms.add(apiOntologyTerm.getAccession());
        }
    }

    //Important: this constructor can't be removed because it is suddenly required by the REST layer to build properties given a json input
    public ApiShallowProperty(AssayProperty assayProperty) {
        this(assayProperty.getName(), assayProperty.getValue());

        this.terms = getTermAccessions(assayProperty.getTerms());
    }

    //Important: this constructor can't be removed because it is suddenly required by the REST layer to build properties given a json input
    public ApiShallowProperty(SampleProperty sampleProperty) {
        this(sampleProperty.getName(), sampleProperty.getValue());

        this.terms = getTermAccessions(sampleProperty.getTerms());
    }

    private SortedSet getTermAccessions(Collection<OntologyTerm> ontologyTerms){
        SortedSet sortedTermAccessions = Sets.newTreeSet();
        for (OntologyTerm ontologyTerm: ontologyTerms){
            sortedTermAccessions.add(ontologyTerm.getAccession());
        }
        return sortedTermAccessions;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public SortedSet<String> getTerms() {
        return terms;
    }

    @Override
    public int compareTo(ApiShallowProperty other) {
        int result = Ordering.natural().compare(this.getName() , other.getName());

        if (result != 0) {

            return result;
        }

        result = Ordering.natural().compare(this.getValue() , other.getValue());

        if (result != 0) {

            return result;

        }

        return Ordering.natural().compare(this.getTerms().toString(), other.getTerms().toString());
    }

    private static class ApiOntologyTermToAccession implements Function<ApiOntologyTerm, String> {

        @Override
        public String apply(@Nullable ApiOntologyTerm apiOntologyTerm) {
            return apiOntologyTerm.getAccession();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, value, terms);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ApiShallowProperty other = (ApiShallowProperty) obj;
        return Objects.equal(this.name, other.name) && Objects.equal(this.value, other.value) && Objects.equal(this.terms, other.terms);
    }


    private static class OntologyTermToAccession implements Function<OntologyTerm, String> {

        @Override
        public String apply(@Nullable OntologyTerm ontologyTerm) {
            return ontologyTerm.getAccession();
        }
    }

}
