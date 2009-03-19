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

    private final Map<String,PrefixNode> prefixTrees = new HashMap<String,PrefixNode>();
    private Set<String> allFactors;
    
    public ExpFactorValueListHelper(SolrServer solrExpt, Collection<String> allFactors)
    {
        this.solrExpt = solrExpt;
        this.allFactors = new HashSet<String>(allFactors);
    }

    public void preloadData() {
        for(String property : allFactors) {
            treeGetOrLoad(property);
        }
    }

    private PrefixNode treeGetOrLoad(String property) {
        PrefixNode root;
        synchronized(prefixTrees) {
            if(!prefixTrees.containsKey(property)) {
                SolrQuery q = new SolrQuery("exp_in_dw:true");
                q.setRows(0);
                q.setFacet(true);
                q.setFacetMinCount(1);
                q.setFacetLimit(-1);
                q.setFacetSort(true);

                if(AtlasStructuredQueryService.EXP_FACTOR_NAME.equals(property))
                    q.addFacetField("dwe_exp_accession");
                else if(!allFactors.contains(property))
                    return null;
                else
                    q.addFacetField(AtlasStructuredQueryService.FIELD_FACTOR_PREFIX  + property);
                
                try {
                    QueryResponse qr = solrExpt.query(q);
                    root = new PrefixNode();
                    if(qr.getFacetFields() != null && qr.getFacetFields().get(0) != null
                            && qr.getFacetFields().get(0).getValues() != null) {
                        for(FacetField.Count ffc : qr.getFacetFields().get(0).getValues())
                            if(ffc.getName().length() > 0) {
                                root.add(ffc.getName(), (int)ffc.getCount());
                            }
                    }
                    prefixTrees.put(property, root);
                } catch (SolrServerException e) {
                    throw new RuntimeException(e);
                }
            }
            root = prefixTrees.get(property);
        }
        return root;
    }

    public Iterable<String> listAllValues(String property) {
        final List<String> result = new ArrayList<String>();
        PrefixNode.WalkResult rc = new PrefixNode.WalkResult() {
            public void put(String name, int count) {
                result.add(name);
            }
            public boolean enough() {
                return false;
            }
        };
        if(null == property || "".equals(property)) {
            for(String prop : allFactors) {
                PrefixNode root = treeGetOrLoad(prop);
                if(root != null)
                    root.collect("", rc);
            }
            Collections.sort(result);
        } else {
            PrefixNode root = treeGetOrLoad(property);
            if(root != null)
                root.collect("", rc);
        }
        return result;
    }

    public Iterable<AutoCompleteItem> autoCompleteValues(final String property, String query, final int limit) {
        if(query.startsWith("\""))
            query = query.substring(1);
        if(query.endsWith("\""))
            query = query.substring(0, query.length() - 1);

        boolean hasPrefix = query != null && !"".equals(query);
        if(hasPrefix)
            query = query.toLowerCase();

        boolean anyProp = property == null || property.equals("");

        Collection<AutoCompleteItem> result;
        if(anyProp) {
            result = new TreeSet<AutoCompleteItem>();
            for(final String prop : allFactors)
                treeAutocomplete(prop, query, limit, result);
        } else {
            result = new ArrayList<AutoCompleteItem>();
            treeAutocomplete(property, query, limit, result);
        }
        return result;
    }

    private void treeAutocomplete(final String property, String query, final int limit, final Collection<AutoCompleteItem> result) {
        PrefixNode root = treeGetOrLoad(property);
        if(root != null) {
            root.walk(query, 0, "", new PrefixNode.WalkResult() {
                public void put(String name, int count) {
                    result.add(new AutoCompleteItem(property, name, (long) count, ""));
                }

                public boolean enough() {
                    return limit >= 0 && result.size() >= limit;
                }
            });
        }
    }
}
