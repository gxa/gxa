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

package uk.ac.ebi.gxa.model.impl;

import uk.ac.ebi.gxa.model.Assay;
import uk.ac.ebi.gxa.model.PropertyCollection;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 30, 2009
 * Time: 10:56:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasAssay implements Assay {

    private String experimentAccession;
    private String accession;
    private long id;
    private Collection<String> sampleAccessions;
    private PropertyCollection properties;

    public String getExperimentAccession(){
        return experimentAccession;
    }
    public void setExperimentAccession(String experimentAccession){
        this.experimentAccession = experimentAccession;
    }

    public String getAccession(){
        return accession;
    }
    public void setAccession(String accession){
        this.accession = accession;
    }

    public long getId(){
        return id;
    }
    public void setid(long id){
        this.id = id;
    }

    public Collection<String> getSampleAccessions(){
        return this.sampleAccessions;
    }
    public void setSampleAccessions(Collection<String> sampleAccessions){
        this.sampleAccessions = sampleAccessions;
    }

    public PropertyCollection getProperties(){
        return this.properties;
    }
    public void setProperties(PropertyCollection properties){
        this.properties = properties;
    }
    
    public int getPositionInMatrix(){
        return 0;
    }
}
