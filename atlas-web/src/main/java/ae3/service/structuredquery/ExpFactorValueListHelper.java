package ae3.service.structuredquery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author pashky
 */
public class ExpFactorValueListHelper implements IValueListHelper {

    private SolrServer solrExpt;
    private Log log = LogFactory.getLog(ExpFactorValueListHelper.class);

    public ExpFactorValueListHelper(SolrServer solrExpt)
    {
        this.solrExpt = solrExpt;
    }

    public Map<String, Long> autoCompleteValues(String factor, String query, int limit) {
        Map<String,Long> map = new TreeMap<String,Long>();

        if(query.startsWith("\""))
            query = query.substring(1);
        if(query.endsWith("\""))
            query = query.substring(0, query.length() - 1);

        for(String fv : getFactorValues(factor, query, limit))
        {
            map.put(fv, 123L);
        }
        return map;
    }

    public Iterable<String> listAllValues(String factor) {
        return getFactorValues(factor, null, -1);
    }

    private SortedSet<String> getFactorValues(String factor, String query, int limit) {
        SortedSet<String> s = new TreeSet<String>();
        try {
            SolrQuery q = new SolrQuery("exp_in_dw:true");
            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);
            q.setFacetPrefix(query != null ? query : "");

            if (factor == null || factor.equals(""))
                factor = "exp_factor_values_exact";
            else
                factor = AtlasStructuredQueryService.FIELD_FACTOR_PREFIX  + factor;
            q.addFacetField(factor);

            q.setFacetLimit(limit);
            q.setFacetSort(true);
            QueryResponse qr = solrExpt.query(q);

            if (null == qr.getFacetFields().get(0).getValues())
                return s;

            for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                s.add(ffc.getName());
            }

        } catch (SolrServerException e) {
            log.error(e);
        }

        return s;
    }
}
