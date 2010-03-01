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
 *  All, absolutely all data retrieval functions used by Atlas app, web services and JSON api. 
 * User: Andrey
 * Date: Oct 20, 2009
 * Time: 5:53:00 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Dao {

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery) throws GxaException;
    public ArrayDesign                 getArrayDesignByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]                   getArrayDesignIDs(ArrayDesignQuery atlasArrayDesignQuery) throws GxaException;

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery) throws GxaException;
    public Assay                 getAssayByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]             getAssayIDs(AssayQuery atlasAssayQuery) throws GxaException;

    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery) throws GxaException;
    public Sample                 getSampleByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]              getSampleIDs(SampleQuery atlasSampleQuery) throws GxaException;

    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery) throws GxaException;
    public Experiment                 getExperimentByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]                  getExperimentIDs(ExperimentQuery atlasExperimentQuery) throws GxaException;

    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery) throws GxaException;
    public Property                 getPropertyByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]                getPropertyIDs(PropertyQuery atlasPropertyQuery) throws GxaException;

    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery) throws GxaException;
    public Gene                 getGeneByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]            getGeneIDs(GeneQuery atlasGeneQuery) throws GxaException;

    public QueryResultSet<Property> getGeneProperty(GenePropertyQuery atlasGenePropertyQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Property> getGeneProperty(GenePropertyQuery atlasGenePropertyQuery) throws GxaException;
    public Property                 getGenePropertyByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]                getGenePropertyIDs(GenePropertyQuery atlasPropertyQuery) throws GxaException;

    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException;
    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException;

}