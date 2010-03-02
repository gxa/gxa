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

/**
 * Query object for ArrayDesign object; features hasName(), hasType() and hasProvider() methods. 
 * User: Andrey
 * Date: Oct 21, 2009
 * Time: 2:07:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArrayDesignQuery extends AccessionQuery<ArrayDesignQuery> {

    public ArrayDesignQuery(){}; 
    public ArrayDesignQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }

    private String name;
    public String getName(){
        return name;
    }
    public ArrayDesignQuery hasName(String name){
        this.name = name;
        return this;
    }

    private String type;
    public String getType(){
        return type;
    }
    public ArrayDesignQuery hasType(String type){
        this.type = type;
        return this;
    }

    private String provider;
    public String getProvider(){
        return provider;
    }
    public ArrayDesignQuery hasProvider(String provider){
        this.provider = provider;
        return this;
    }

}