package  uk.ac.ebi.gxa.model;

/**
 * Retrieving list of properties. 
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 9:45:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyQuery extends AccessionQuery<PropertyQuery>{
    public PropertyQuery hasValue(String value){
        return this;
    }
    public PropertyQuery isSampleProperty(Boolean isSampleProperty){
        return this;
    }
    public PropertyQuery isAssayProperty(Boolean isSampleProperty){
        return this;
    }
    public PropertyQuery usedInSamples(SampleQuery sampleQuery){
        return this;
    }
    public PropertyQuery usedInAssay(AssayQuery assayQuery){
        return this;
    }
    public PropertyQuery usedInExperiments(ExperimentQuery experimentQuery){
        return this;
    }

    public PropertyQuery fullextQuery(String keyword){
        return this;
    }
}
