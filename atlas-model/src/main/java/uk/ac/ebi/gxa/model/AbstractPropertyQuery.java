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

import static org.apache.solr.common.util.StrUtils.join;

import java.util.List;
import java.util.ArrayList;

/**
 * Retrieving list of properties. 
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 9:45:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class AbstractPropertyQuery<T> extends AccessionQuery<T>{
    private List<String> values = new ArrayList<String>();
    private List<String> fullTextQueries = new ArrayList<String>();

    public AbstractPropertyQuery(){} 

    public AbstractPropertyQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }  

    public T hasValue(String value) {
        values.add(value);
        return (T)this;
    }


    public T fullTextQuery(String keyword) {
        fullTextQueries.add(keyword);
        return (T)this;
    }

    public List<String> getValues() {
        return values;
    }

    public String getValue(){
        return join(values,',');
    }

    public List<String> getFullTextQueries() {
        return fullTextQueries;
    }

    public String getFullTextQuery(){
        return join(fullTextQueries, ' ');
    }

}
