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
public class PropertyQuery extends AbstractPropertyQuery<PropertyQuery>{
    private Boolean sampleProperty;
    private Boolean assayProperty;
    private List<SampleQuery> sampleQueries = new ArrayList<SampleQuery>();
    private List<AssayQuery> assayQueries = new ArrayList<AssayQuery>();
    private List<ExperimentQuery> experimentQueries = new ArrayList<ExperimentQuery>();
    private int propertyID;

    public PropertyQuery(){};
    public PropertyQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }

    public PropertyQuery isSampleProperty(Boolean isSampleProperty) {
        sampleProperty = isSampleProperty;
        return this;
    }

    public PropertyQuery isAssayProperty(Boolean isAssayProperty) {
        assayProperty = isAssayProperty;
        return this;
    }

    public PropertyQuery usedInSamples(SampleQuery sampleQuery) {
        sampleQueries.add(sampleQuery);
        return this;
    }

    public PropertyQuery usedInAssay(AssayQuery assayQuery) {
        assayQueries.add(assayQuery);
        return this;
    }

    public PropertyQuery usedInExperiments(ExperimentQuery experimentQuery) {
        experimentQueries.add(experimentQuery);
        return this;
    }

    /** redundant - use AccessionQuery.hasId()
    public PropertyQuery hasPropertyID(int propertyID){
        this.propertyID = propertyID;
        return this;
    }
    **/

    public List<ExperimentQuery> getExperimentQueries() {
        return experimentQueries;
    }

    public List<AssayQuery> getAssayQueries() {
        return assayQueries;
    }

    public List<SampleQuery> getSampleQueries() {
        return sampleQueries;
    }

    public Boolean isAssayProperty() {
        return assayProperty;
    }

    public Boolean isSampleProperty() {
        return sampleProperty;
    }

    /**
    public int getPropertyID(){
        return this.propertyID;
    }
    **/ 
}
