package ae3.service.structuredquery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * EFVs listing and autocompletion helper implementation
 * @author pashky
 * @see ae3.service.structuredquery.IValueListHelper
 */
public class ExpFactorValueListHelper implements IValueListHelper {

    private SolrServer solrExpt;
    private Log log = LogFactory.getLog(ExpFactorValueListHelper.class);

    public ExpFactorValueListHelper(SolrServer solrExpt)
    {
        this.solrExpt = solrExpt;
    }

    public Iterable<AutoCompleteItem> autoCompleteValues(String factor, String query, int limit) {
        List<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();

        if(query.startsWith("\""))
            query = query.substring(1);
        if(query.endsWith("\""))
            query = query.substring(0, query.length() - 1);

        for(String fv : getFactorValues(factor, query, limit))
        {
            result.add(new AutoCompleteItem(null, fv, 1L, null));
        }
        return result;
    }

    public Iterable<String> listAllValues(String factor) {
        return getFactorValues(factor, null, -1);
    }

    private SortedSet<String> getFactorValues(String factor, String query, int limit) {

        boolean hasPrefix = query != null && !"".equals(query);
        if(hasPrefix)
            query = query.toLowerCase();

        SortedSet<String> s = new TreeSet<String>();
        try {
            SolrQuery q = new SolrQuery("exp_in_dw:true");
            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);

            if (factor == null || factor.equals(""))
            {
                q.addFacetField("exp_factor_values_exact");
                q.addFacetField("dwe_exp_accession");
            } else if(AtlasStructuredQueryService.EXP_FACTOR_NAME.equals(factor)) {
                q.addFacetField("dwe_exp_accession");
            } else {                
                q.addFacetField(AtlasStructuredQueryService.FIELD_FACTOR_PREFIX  + factor);
            }

            q.setFacetLimit(hasPrefix ? -1 : limit);
            q.setFacetSort(true);
            QueryResponse qr = solrExpt.query(q);

            for(FacetField ff : qr.getFacetFields())
                if(ff.getValues() != null) {
                    for (FacetField.Count ffc : ff.getValues())
                        if(ffc.getName().length() > 0 && (!hasPrefix || ffc.getName().toLowerCase().startsWith(query)))
                        {
                            s.add(ffc.getName());
                            if(s.size() == limit && limit > 0)
                                break;
                        }
                }

        } catch (SolrServerException e) {
            log.error(e);
        }

        return s;
    }

    public void preloadData() {
        // no caches here by now
    }
}
