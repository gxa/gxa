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

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 2:06:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SampleQuery extends AccessionQuery<SampleQuery>{
    private PropertyQuery propertyQuery;

    public SampleQuery(){};

    public SampleQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }

    public SampleQuery hasProperty(PropertyQuery propertyQuery){
        this.propertyQuery = propertyQuery;
        return this;
    }

    public PropertyQuery getPropertyQuery(){
        return this.propertyQuery;
    }
}