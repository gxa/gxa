/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

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
    private long propertyID;

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
