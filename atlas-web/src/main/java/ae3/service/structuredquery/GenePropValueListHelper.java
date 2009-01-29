package ae3.service.structuredquery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Gene properties values listing and autocompletion helper implementation
 * @author pashky
 * @see ae3.service.structuredquery.IValueListHelper
 */
public class GenePropValueListHelper implements IValueListHelper {
    private SolrServer solrAtlas;
    private Log log = LogFactory.getLog(getClass());

    public GenePropValueListHelper(SolrServer solrAtlas)
    {
        this.solrAtlas = solrAtlas;
    }

    public Map<String, Long> autoCompleteValues(String property, String query, int limit) {
        if(query.startsWith("\""))
            query = query.substring(1);
        if(query.endsWith("\""))
            query = query.substring(0, query.length() - 1);

        return getGenePropertyValues(property, query, limit);
    }

    public Iterable<String> listAllValues(String property) {
        return getGenePropertyValues(property, null, -1).keySet();
    }

    private SortedMap<String,Long> getGenePropertyValues(String property, String query, int limit) {
        SortedMap<String,Long> s = new TreeMap<String,Long>();
        try {
            SolrQuery q = new SolrQuery("gene_id:[1 TO *]");

            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);
            q.setFacetPrefix(query != null ? query : "");
            q.setFacetLimit(limit);
            q.setFacetSort(true);

            if(property == null || property.equals("")) {
                q.addFacetField("gene_name");
                for(String gf : AtlasStructuredQueryService.GENE_FACETS)
                    q.addFacetField("gene_" + gf);
            } else {
                q.addFacetField("gene_" + property);
            }

            QueryResponse qr = solrAtlas.query(q);

            if (null == qr.getFacetFields().get(0).getValues())
                return s;

            for(FacetField ff : qr.getFacetFields())
            {
                for (FacetField.Count ffc : ff.getValues()) {
                    s.put(ffc.getName(), ffc.getCount());
                }
            }
        } catch (SolrServerException e) {
            log.error(e);
        }

        return s;
    }
}
