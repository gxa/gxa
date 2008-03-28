package uk.ac.ebi.ae3.indexbuilder;

import java.util.Iterator;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;

public class IndexQueryTest extends AbstractIndexBuilderTest
{
	public void testQueryByAccession() throws SolrServerException
	{
		  //QueryResponse res=getSolrExpt().query(params);
		  String query = ConfigurationService.FIELD_EXP_ACCESSION + ":A*";
		  SolrQuery q = new SolrQuery(query);
          QueryResponse resp=getSolrExpt().query(q);
          SolrDocumentList sList=resp.getResults();
          Iterator<SolrDocument> it =sList.iterator();
          while (it.hasNext())
          {
        	  SolrDocument doc=it.next();
          }
          System.out.println("===============" + sList.size());
	}
}
