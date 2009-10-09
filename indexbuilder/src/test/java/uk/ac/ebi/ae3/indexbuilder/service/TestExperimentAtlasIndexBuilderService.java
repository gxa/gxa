package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dbunit.dataset.ITable;
import uk.ac.ebi.ae3.indexbuilder.Constants;

/**
 * Tests the documents that are created by the class {@link
 * uk.ac.ebi.ae3.indexbuilder.service.ExperimentAtlasIndexBuilderService}.
 * Whilst most of the lifecycle instantiation is left to the IndexBuilder and
 * the abstract service, the logic for the creation of specific index documents
 * is handled here.  This class tests all the documents are created correctly
 * and that they contain data that matches that from the test database.
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
    eaibs =
        new ExperimentAtlasIndexBuilderService(getAtlasDAO(), getSolrServer());
  }

  public void tearDown() throws Exception {
    super.tearDown();

    eaibs = null;
  }

  public void testCreateIndexDocs() {
    try {
      // create the docs
      eaibs.createIndexDocs();
      // commit the results
      eaibs.getSolrServer().commit();

      // now test that all the docs we'd expect were created

      // query for everything, sort by experiment id
      SolrQuery q = new SolrQuery("*:*");
      q.setRows(10);
      q.setFields("");
      q.addSortField("dwe_exp_id", SolrQuery.ORDER.asc);

      // do the query to fetch all documents
      QueryResponse queryResponse = getSolrServer().query(q);

      // initialise comparators
      String tableName, dbFieldName, constant;
      boolean result;

      // compares experiments table
      tableName = "A2_EXPERIMENT";

      // check experiment ids
      dbFieldName = "experimentid";
      constant = Constants.FIELD_DWEXP_ID;
      result = checkMatches(tableName, dbFieldName, constant, queryResponse);
      assertTrue("Couldn't match field '" + dbFieldName + "' in SOLR query",
                 result);

      // check experiment accessions
      dbFieldName = "accession";
      constant = Constants.FIELD_DWEXP_ACCESSION;
      result = checkMatches(tableName, dbFieldName, constant, queryResponse);
      assertTrue("Couldn't match field '" + dbFieldName + "' in SOLR query",
                 result);

      // check experiment description
      dbFieldName = "description";
      constant = Constants.FIELD_DWEXP_EXPDESC;
      result = checkMatches(tableName, dbFieldName, constant, queryResponse);
      assertTrue("Couldn't match field '" + dbFieldName + "' in SOLR query",
                 result);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  private boolean checkMatches(String tableName, String dbFieldName,
                               String constant, QueryResponse queryResponse)
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
        System.out.println("Couldn't find value matching " + expected +
            " in index for field " + dbFieldName);
      }
    }

    return allMatched;
  }
}
