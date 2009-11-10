package  uk.ac.ebi.gxa.model;

/**
 * Retrieving list of properties. 
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 9:45:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class AbstractPropertyQuery<T> extends AccessionQuery<T>{
    public T hasValue(String value){
        return (T)this;
    }
    public T isSampleProperty(Boolean isSampleProperty){
        return (T)this;
    }
    public T isAssayProperty(Boolean isSampleProperty){
        return (T)this;
    }
    public T usedInSamples(SampleQuery sampleQuery){
        return (T)this;
    }
    public T usedInAssay(AssayQuery assayQuery){
        return (T)this;
    }
    public T usedInExperiments(ExperimentQuery experimentQuery){
        return (T)this;
    }

    public T fullextQuery(String keyword){
        return (T)this;
    }
}
