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
 * http://ostolop.github.com/gxa/
 */

package  uk.ac.ebi.gxa.model;

import java.util.Map;
import java.util.Collection;

/**
 * Assay query object
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 2:07:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssayQuery extends AccessionQuery<AssayQuery> {

    public AssayQuery(){};

    public AssayQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }
    //AZ:10-Nov : lets have PropertyQuery member
    //private Map<String, Property> properties;
    private PropertyQuery propertyQuery;

    public PropertyQuery getPropertyQuery(){
        return propertyQuery;
    }

    public AssayQuery hasProperty(PropertyQuery propertyQuery){
        this.propertyQuery = propertyQuery; 
        return this;
    }
}