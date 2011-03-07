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

package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dbunit.dataset.ITable;

import java.util.Collection;
import java.util.Map;

import uk.ac.ebi.gxa.index.builder.IndexAllCommand;

/**
 * Tests the documents that are created by the class {@link uk.ac.ebi.gxa.index.builder.service.ExperimentAtlasIndexBuilderService}.
 * Whilst most of the lifecycle instantiation is left to the IndexBuilder and the abstract service, the logic for the
 * creation of specific index documents is handled here.  This class tests all the documents are created correctly and
 * that they contain data that matches that from the test database.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestExperimentAtlasIndexBuilderService
        extends IndexBuilderServiceTestCase {
    private ExperimentAtlasIndexBuilderService eaibs;

    public void setUp() throws Exception {
        super.setUp();

        // create IndexBuilderServices for genes (atlas) and experiments
        eaibs = new ExperimentAtlasIndexBuilderService();
        eaibs.setAtlasDAO(getAtlasDAO());
        eaibs.setSolrServer(getExptSolrServer());
    }

    public void tearDown() throws Exception {
        super.tearDown();

        eaibs = null;
    }

    public void testCreateIndexDocs() throws Exception {
        // create the docs
        eaibs.build(new IndexAllCommand(), new IndexBuilderService.ProgressUpdater() {
            public void update(String progress) {

            }
        });
        // commit the results
        eaibs.getSolrServer().commit();

        // now test that all the docs we'd expect were created

        // query for everything, sort by experiment id
        SolrQuery q = new SolrQuery("*:*");
        q.setRows(10);
        q.setFields("");
        q.addSortField("id", SolrQuery.ORDER.asc);

        // do the query to fetch all documents
        QueryResponse queryResponse = getExptSolrServer().query(q);

        // initialise comparators
        String tableName, dbFieldName, constant;
        boolean result;

        // compares experiments table
        tableName = "A2_EXPERIMENT";

        // check experiment ids
        dbFieldName = "experimentid";
        constant = "id";
        result = checkMatches(tableName, dbFieldName, constant, queryResponse);
        assertTrue("Couldn't match field '" + dbFieldName + "' in SOLR query", result);

        // check experiment accessions
        dbFieldName = "accession";
        constant = "accession";
        result = checkMatches(tableName, dbFieldName, constant, queryResponse);
        assertTrue("Couldn't match field '" + dbFieldName + "' in SOLR query", result);

        // check experiment description
        dbFieldName = "description";
        constant = "description";
        result = checkMatches(tableName, dbFieldName, constant, queryResponse);
        assertTrue("Couldn't match field '" + dbFieldName + "' in SOLR query", result);

        // now check property fields for assays
        tableName = "A2_ASSAY";
        dbFieldName = "assayid";

        ITable assays = getDataSet().getTable(tableName);
        for (int i = 0; i < assays.getRowCount(); i++) {
            String assayId = assays.getValue(i, dbFieldName).toString();

            result = checkPropertyMatches("assayid", assayId, "A2_ASSAYPV", queryResponse);

            assertTrue("Couldn't match properties for Assay id '" + assayId + "'",
                    result);
        }

        // now check property fields for samples
        tableName = "A2_SAMPLE";
        dbFieldName = "sampleid";

        ITable samples = getDataSet().getTable(tableName);
        for (int i = 0; i < samples.getRowCount(); i++) {
            String sampleId = samples.getValue(i, dbFieldName).toString();

            result = checkPropertyMatches("sampleid", sampleId,
                    "A2_SAMPLEPV",
                    queryResponse);

            assertTrue("Couldn't match properties for Sample id '" + sampleId + "'", result);
        }

        // dump some handy output
        for (SolrDocument createdDoc : queryResponse.getResults()) {
            // print list of other fields
            for (Map.Entry<String, Object> entry : createdDoc) {
                System.out.println("Next field: " + entry.getKey() + " = " + entry.getValue().toString());
            }
        }
    }

    private boolean checkMatches(String tableName, String dbFieldName, String constant, QueryResponse queryResponse)
            throws Exception {
        // get table
        boolean allMatched = true;
        ITable expts = getDataSet().getTable(tableName);
        // check experiment id
        for (int i = 0; i < expts.getRowCount(); i++) {
            String expected = expts.getValue(i, dbFieldName).toString();

            // now look in documents for actual
            boolean matched = false;
            for (SolrDocument createdDoc : queryResponse.getResults()) {
                Object o = createdDoc.getFieldValue(constant);
                assertNotNull("Null result obtained from the index for " + expected, o);

                String actual = createdDoc.getFieldValue(constant).toString();
                // break if matched
                if (actual.equals(expected)) {
                    matched = true;
                    break;
                }
            }

            allMatched = allMatched && matched;
            if (!matched) {
                System.out.println("Couldn't find value matching " + expected + " in index for field " + dbFieldName);
            }
        }

        return allMatched;
    }

    private boolean checkPropertyMatches(String objectName, String objectId, String joinTableName,
                                         QueryResponse queryResponse)
            throws Exception {
        // get table
        boolean allMatched = true;
        ITable props = getDataSet().getTable("A2_PROPERTY");
        ITable propValues = getDataSet().getTable("A2_PROPERTYVALUE");
        ITable mapping = getDataSet().getTable(joinTableName);

        // obtain property value ids for this assay
        for (int i = 0; i < mapping.getRowCount(); i++) {
            if (mapping.getValue(i, objectName).toString().equals(objectId)) {
                String propValueID = mapping.getValue(i, "propertyvalueid").toString();

                // now use this property value id to compare to index
                for (int j = 0; j < propValues.getRowCount(); j++) {
                    if (propValues.getValue(j, "propertyvalueid").toString()
                            .equals(propValueID)) {
                        // found matching value
                        String valueName = propValues.getValue(j, "name").toString();
                        String propId = propValues.getValue(j, "propertyid").toString();

                        for (int k = 0; k < props.getRowCount(); k++) {
                            if (props.getValue(k, "propertyid").toString().equals(propId)) {
                                String propName = props.getValue(k, "name").toString();

                                // now got property name and value for the given object

                                // check against solr docs
                                boolean matched = false;
                                String constant = "a_property_" + propName;
                                for (SolrDocument createdDoc : queryResponse.getResults()) {
                                    Collection values = createdDoc.getFieldValues(constant);

                                    // check not null - not all exps will have properties
                                    if (values != null) {
                                        for (Object o : values) {
                                            // and check the property value if this property is present
                                            String actual = o.toString();

                                            System.out.println("Property from index: " + actual);

                                            // break if matched
                                            if (actual.equals(valueName)) {
                                                matched = true;
                                                break;
                                            }
                                        }
                                    }
                                }

                                allMatched = allMatched && matched;
                                if (!matched) {
                                    System.out.println(
                                            "Couldn't find property value matching " + valueName +
                                                    " in index for field " + constant);
                                }
                            }
                        }
                    }
                }
            }
        }

        return allMatched;
    }
}
