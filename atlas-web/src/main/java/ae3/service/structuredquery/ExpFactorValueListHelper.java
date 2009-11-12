package ae3.service.structuredquery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.*;

/**
 * EFVs listing and autocompletion helper implementation
 * @author pashky
 * @see ae3.service.structuredquery.IValueListHelper
 */
public class ExpFactorValueListHelper implements IValueListHelper {

    private SolrServer solrAtlas;
    private SolrServer solrExpt;
    final private Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String,PrefixNode> prefixTrees = new HashMap<String,PrefixNode>();
    private Set<String> allFactors;

    public ExpFactorValueListHelper(SolrServer solrAtlas, SolrServer solrExpt, Collection<String> allFactors)
    {
        this.solrAtlas = solrAtlas;
        this.solrExpt = solrExpt;
        this.allFactors = new HashSet<String>(allFactors);
    }

    public void preloadData() {
        for(String property : allFactors) {
            treeGetOrLoad(property);
        }
        treeGetOrLoad(Constants.EXP_FACTOR_NAME);
    }

    private PrefixNode treeGetOrLoad(String property) {
        PrefixNode root;
        synchronized(prefixTrees) {
            if(!prefixTrees.containsKey(property)) {
                log.info("Loading factor values and counts for " + property);
                SolrQuery q = new SolrQuery("gene_id:[* TO *]");
                q.setRows(0);
                q.setFacet(true);
                q.setFacetMinCount(1);
                q.setFacetLimit(-1);
                q.setFacetSort(true);

                try {
                    Map<String,String> valMap = new HashMap<String,String>();
                    if(Constants.EXP_FACTOR_NAME.equals(property)) {
                        q.addFacetField("exp_ud_ids");

                        SolrQuery exptMapQ = new SolrQuery("dwe_exp_id:[* TO *]");
                        exptMapQ.setRows(1000000);
                        exptMapQ.addField("dwe_exp_id");
                        exptMapQ.addField("dwe_exp_accession");
                        QueryResponse qr = solrExpt.query(exptMapQ);
                        for(SolrDocument doc : qr.getResults())
                        {
                            Object id = doc.getFieldValue("dwe_exp_id");
                            String accession = (String)doc.getFieldValue("dwe_exp_accession");
                            if(id != null && accession != null)
                                valMap.put(id.toString(), accession);
                        }
                    } else if(!allFactors.contains(property))
                        return null;
                    else
                        q.addFacetField("efvs_ud_" + EscapeUtil.encode(property));

                    QueryResponse qr = solrAtlas.query(q);
                    root = new PrefixNode();
                    if(qr.getFacetFields() != null && qr.getFacetFields().get(0) != null
                            && qr.getFacetFields().get(0).getValues() != null) {
                        for(FacetField.Count ffc : qr.getFacetFields().get(0).getValues())
                            if(ffc.getName().length() > 0 && ffc.getCount() > 0) {
                                if(valMap.size() == 0)
                                    root.add(ffc.getName(), (int)ffc.getCount());
                                else if(valMap.containsKey(ffc.getName()))
                                    root.add(valMap.get(ffc.getName()), (int)ffc.getCount());
                            }
                    }
                    prefixTrees.put(property, root);

                } catch (SolrServerException e) {
                    throw new RuntimeException(e);
                }
                log.info("Done loading factor values and counts for " + property);
            }
            root = prefixTrees.get(property);
        }
        return root;
    }

    public Collection<String> listAllValues(String property) {
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

    public Collection<AutoCompleteItem> autoCompleteValues(final String property, String query, final int limit, Map<String,String> filters) {

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
                    result.add(new AutoCompleteItem(property, name, name, (long) count));
                }

                public boolean enough() {
                    return limit >= 0 && result.size() >= limit;
                }
            });
        }
    }
}
