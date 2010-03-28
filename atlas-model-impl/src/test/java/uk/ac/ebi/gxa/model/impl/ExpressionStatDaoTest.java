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

import org.junit.*;
import static org.junit.Assert.*;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.model.*;

import java.util.Collections;
import java.util.Collection;
import java.util.Date;

/**
 * @author pashky
 */
public class ExpressionStatDaoTest extends AbstractOnceIndexTest {
    static SolrServer geneServer;
    
    ExpressionStatDao statDao;

    @BeforeClass
    public static void makeServer() {
        geneServer = new EmbeddedSolrServer(getContainer(), "atlas");
    }

    @Before
    public void setUp() {
        statDao = new ExpressionStatDao();
        statDao.setGeneServer(geneServer);
        statDao.setDao(new Dao() {
            public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery, PageSortParams pageSortParams) throws GxaException {
                return null;
            }

            public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery) throws GxaException {
                return null;
            }

            public ArrayDesign getArrayDesignByAccession(AccessionQuery accession) throws GxaException {
                return null;
            }

            public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery, PageSortParams pageSortParams) throws GxaException {
                return null;
            }

            public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery) throws GxaException {
                return null;
            }

            public Assay getAssayByAccession(AccessionQuery accession) throws GxaException {
                return null;
            }

            public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery, PageSortParams pageSortParams) throws GxaException {
                return null;
            }

            public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery) throws GxaException {
                return null;
            }

            public Sample getSampleByAccession(AccessionQuery accession) throws GxaException {
                return null;
            }

            public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery, PageSortParams pageSortParams) throws GxaException {
                return null;
            }

            public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery) throws GxaException {
                QueryResultSet<Experiment> res = new QueryResultSet<Experiment>();
                res.setIsMulti(false);
                res.setItem(new Experiment() {
                    public String getDescription() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getType() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Date getLoadDate() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public DEGStatus getDEGStatus() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getPerformer() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getLab() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getAssayAccessions() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public Collection<String> getSampleAccessions() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public long getId() {
                        return 0;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public String getAccession() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }

                    public PropertyCollection getProperties() {
                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                });
                return res;
            }

            public Experiment getExperimentByAccession(AccessionQuery accession) throws GxaException {
                return getExperiment(null).getItem();
            }

            public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery, PageSortParams pageSortParams) throws GxaException {
                return getProperty(atlasPropertyQuery);
            }

            public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery) throws GxaException {
                QueryResultSet<Property> res = new QueryResultSet<Property>();
                res.setIsMulti(false);
                res.setItem(new Property() {

                    public Collection<String> getValues() {
                        return Collections.singleton("liver");
                    }

                    public long getId() {
                        return 0;
                    }

                    public String getAccession() {
                        return "organismpart";
                    }
                });
                return res;
            }

            public Property getPropertyByAccession(AccessionQuery accession) throws GxaException {
                return null;
            }

            public Integer[] getPropertyIDs(PropertyQuery propertyQuery) throws GxaException{
                return null;
            }

            public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery, PageSortParams pageSortParams) throws GxaException {
                return null;
            }

            public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery) throws GxaException {
                return null;
            }

            public Gene getGeneByAccession(AccessionQuery accession) throws GxaException {
                return null;
            }

            public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public QueryResultSet<Property> getGeneProperty(GenePropertyQuery atlasGenePropertyQuery, PageSortParams pageSortParams) throws GxaException{
                return null;
            }
            public QueryResultSet<Property> getGeneProperty(GenePropertyQuery atlasGenePropertyQuery) throws GxaException{
                return null;
            }
            public Property                 getGenePropertyByAccession(AccessionQuery accession) throws GxaException{
                return null;
            }
        });
    }

    @After
    public void tearDown() {
        statDao = null;
    }

    @Test
    public void testGetExpressionStat_gene_id() throws GxaException {

        QueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>> result = statDao.getExpressionStat(
                new ExpressionStatQuery().hasGene(new GeneQuery().hasId("153070758")), new PageSortParams());
        assertNotNull(result);

        assertEquals(true, result.isFound());
        assertEquals(1, result.getNumberOfResults());
        assertNotNull(result.getItems());
        assertNotNull(result.getItem());
        assertEquals("153070758", result.getItem().getGene());
    }

    @Test
    public void testGetExpressionStat_gene_accession() throws GxaException {

        QueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>>
                result = statDao.getExpressionStat(
                new ExpressionStatQuery().hasGene(new GeneQuery().hasAccession("ENSG00000066279")),
                new PageSortParams());
        assertNotNull(result);

        assertEquals(true, result.isFound());
        assertEquals(1, result.getNumberOfResults());
        assertNotNull(result.getItems());
        assertNotNull(result.getItem());
        assertEquals("153070758", result.getItem().getGene());
    }

    @Test
    public void testGetExpressionStat_gene_notfound() throws GxaException {

        QueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>>
                result = statDao.getExpressionStat(
                new ExpressionStatQuery().hasGene(new GeneQuery().hasAccession("nevernevergene")),
                new PageSortParams()
        );
        assertNotNull(result);

        assertEquals(false, result.isFound());
        assertEquals(0, result.getNumberOfResults());
    }

    @Test
    public void testGetExpressionStat_gene_species() throws GxaException, SolrServerException {

        QueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>>
                result = statDao.getExpressionStat(
                new ExpressionStatQuery().hasGene(new GeneQuery().hasProperty(
                        new GenePropertyQuery().hasAccession("species").fullTextQuery("HOMO SAPIENS"))
                ),
                new PageSortParams()
        );

        assertNotNull(result);
        assertEquals(true, result.isFound());
        assertEquals(13, result.getNumberOfResults());
        assertNotNull(result.getItems());
        assertNotNull(result.getItem());

        for(GeneExpressionStat stat : result.getItems()) {
            SolrQuery q = new SolrQuery("id:" + stat.getGene());
            q.setFields("species");
            q.setRows(1);
            QueryResponse qr = geneServer.query(q);
            assertEquals("HOMO SAPIENS", qr.getResults().get(0).getFieldValue("species"));
        }
    }

    @Test
    public void testGetExpressionStat_activein() throws GxaException {

        QueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>>
                result = statDao.getExpressionStat(
                new ExpressionStatQuery().activeIn(ExpressionQuery.UP,
                        new PropertyQuery()
                                .hasAccession("organismpart")
                                .fullTextQuery("liver")
                ),
                new PageSortParams()
        );

        assertNotNull(result);
        assertEquals(true, result.isFound());
        assertEquals(9, result.getNumberOfResults());
        assertNotNull(result.getItems());
        assertNotNull(result.getItem());

        for(GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>> gstat : result.getItems()) {
            for(PropertyExpressionStat pstat : gstat.getDrillDown()) {
                if(pstat.getProperty().getAccession().equals("organismpart") && pstat.getProperty().getValues().iterator().next().equals("liver"))
                    assertTrue(pstat.getUpExperimentsCount() > 0);
            }
        }

        result = statDao.getExpressionStat(
                new ExpressionStatQuery().activeIn(ExpressionQuery.DOWN,
                        new PropertyQuery()
                                .hasAccession("organismpart")
                                .fullTextQuery("liver")
                ),
                new PageSortParams()
        );

        assertNotNull(result);
        assertEquals(true, result.isFound());
        assertEquals(15, result.getNumberOfResults());
        assertNotNull(result.getItems());
        assertNotNull(result.getItem());

        for(GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>> gstat : result.getItems()) {
            for(PropertyExpressionStat pstat : gstat.getDrillDown()) {
                if(pstat.getProperty().getAccession().equals("organismpart") && pstat.getProperty().getValues().iterator().next().equals("liver"))
                    assertTrue(pstat.getDnExperimentsCount() > 0);
            }
        }

        result = statDao.getExpressionStat(
                new ExpressionStatQuery().activeIn(ExpressionQuery.UP_OR_DOWN,
                        new PropertyQuery()
                                .hasAccession("organismpart")
                                .fullTextQuery("liver")
                ),
                new PageSortParams()
        );

        assertNotNull(result);
        assertEquals(true, result.isFound());
        assertEquals(17, result.getNumberOfResults());
        assertNotNull(result.getItems());
        assertNotNull(result.getItem());

        for(GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>> gstat : result.getItems()) {
            for(PropertyExpressionStat pstat : gstat.getDrillDown()) {
                if(pstat.getProperty().getAccession().equals("organismpart") && pstat.getProperty().getValues().iterator().next().equals("liver"))
                    assertTrue(pstat.getUpExperimentsCount() > 0 || pstat.getDnExperimentsCount() > 0);
            }
        }

    }

}
