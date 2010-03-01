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
 * Query to retrieve object by accession or id - uniform way for quick retrieval of single object
 */
public class AccessionQuery<T> {
    public AccessionQuery(){
    }
    
    public AccessionQuery(AccessionQuery value){
      this.id = value.id;
      this.accession = value.accession;
    }
    
    private String id;
    public String getId(){
        return this.id;
    }
    public T hasId(String id){
        this.id = id;
        return (T)this;
    }

    private String accession;
    public String getAccession(){
        return this.accession;
    }
    public T hasAccession(String accession){
        this.accession = accession;
        return (T)this;
    }
}
