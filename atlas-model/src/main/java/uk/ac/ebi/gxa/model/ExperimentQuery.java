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
 * Simple search object, used to retrieve {@link Experiment}.
 * User: Andrey
 * Date: Oct 14, 2009
 * Time: 5:07:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExperimentQuery extends AccessionQuery<ExperimentQuery>  {

    private PropertyQuery propertyQuery;
    private GeneQuery geneQuery;
    private String lab;
    private String performer;

    private String solrQuery;
    public String getSolrQuery(){
        return solrQuery;
    }

    public ExperimentQuery(){};

    public ExperimentQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
    }

    /**
     * rudimentary Solr query string
     */
    public void doSolrQuery(String query){
        this.solrQuery = query;
    }

    /**
     * filter genes participating in the experiment
     */
    public ExperimentQuery hasGene(GeneQuery geneQuery){
        this.geneQuery = geneQuery;
        return this;
    }

    /**
     * performer
     */
    public ExperimentQuery hasPerformer(String performer){
        this.performer = performer;
        return this;
    }

    /**
     * lab
     */
    public ExperimentQuery hasLab(String lab){
        this.lab = lab;
        return this;
    }

    /**
     * filter by properties
     */
    public ExperimentQuery hasProperties(PropertyQuery propertyQuery){
        this.propertyQuery = propertyQuery;
        return this;
    }

    public String getPerformer(){
        return this.performer;
    }

    public String getLab(){
        return this.lab;
    }

    public PropertyQuery getPropertyQuery(){
        return this.propertyQuery;
    }

    public GeneQuery getGeneQuery(){
        return this.geneQuery;
    }

}