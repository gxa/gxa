package ae3.service.structuredquery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ae3.service.structuredquery.GeneProperties.Prop;
import ae3.service.structuredquery.GeneProperties.PropType;

import java.util.*;

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

    public Iterable<AutoCompleteItem> autoCompleteValues(String property, String query, int limit) {
        if(query.startsWith("\""))
            query = query.substring(1);
        if(query.endsWith("\""))
            query = query.substring(0, query.length() - 1);

        return getGenePropertyValues(property, query, limit);
    }

    public Iterable<String> listAllValues(String property) {
        List<String> result = new ArrayList<String>();
        for(AutoCompleteItem i : getGenePropertyValues(property, null, -1))
            result.add(i.getValue());
        return result;
    }

    private List<String> generateCaseVariants(String s)
    {
        List<String> pfxs = new ArrayList<String>();
        if(s.length() == 1)
        {
            String up = s.substring(0,1).toLowerCase();
            String lw = s.substring(0,1).toUpperCase();
            if(!up.equals(lw))
                pfxs.add(up);
            pfxs.add(lw);
        } else for(String ps : generateCaseVariants(s.substring(1, s.length()))) {
            String up = s.substring(0,1).toLowerCase();
            String lw = s.substring(0,1).toUpperCase();
            if(!up.equals(lw))
                pfxs.add(up + ps);
            pfxs.add(lw + ps);
        }

        return pfxs;
    }

    private List<AutoCompleteItem> getGenePropertyValues(String property, String query, int limit) {

        boolean hasPrefix = query != null && !"".equals(query);
        if(hasPrefix)
            query = query.toLowerCase();
        
        boolean anyProp = property == null || property.equals("");

        List<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();
        try {
            SolrQuery q = new SolrQuery("gene_id:[1 TO *]");

            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);
            q.setFacetLimit(hasPrefix ? -1 : limit);
            q.setFacetSort(true);

            if(anyProp) {
                for(Prop p : GeneProperties.allProperties())
                    q.addFacetField(p.facetField);
            } else {
                q.addFacetField(GeneProperties.convertPropertyToFacetField(property));
            }

            List<String> prefixes;
            if(hasPrefix) {
                prefixes = generateCaseVariants(query.substring(0, Math.min(2, query.length())));
            } else {
                prefixes = new ArrayList<String>();
                prefixes.add("");
            }

            if(anyProp) {
                EnumMap<PropType, List<AutoCompleteItem>> resmap = new EnumMap<PropType, List<AutoCompleteItem>>(PropType.class);
                for(PropType e : PropType.values())
                    resmap.put(e, new ArrayList<AutoCompleteItem>());

                for(String prefix : prefixes) {
                    q.setFacetPrefix(prefix);

                    QueryResponse qr = solrAtlas.query(q);
                    if(qr.getFacetFields() == null)
                        return result;


                    for(FacetField ff : qr.getFacetFields())
                        if(ff.getValues() != null)
                        {
                            Prop p = GeneProperties.findPropByFacetField(ff.getName());
                            if(p != null) {
                                for (FacetField.Count ffc : ff.getValues())
                                    if(ffc.getName().length() > 0 && ffc.getName().toLowerCase().startsWith(query))
                                    {
                                        resmap.get(p.type).add(new AutoCompleteItem(p.id, ffc.getName(), ffc.getCount()));
                                    }
                            }
                        }
                }

                for(PropType p : PropType.values())
                {
                    List<AutoCompleteItem> l = p.limit > 0 && p.limit < resmap.get(p).size()
                            ? resmap.get(p).subList(0, p.limit) : resmap.get(p);

                    Collections.sort(l);
                    result.addAll(l);
                    if(result.size() >= limit)
                        break;
                }
                result = result.subList(0, Math.min(result.size(), limit));
            } else {
                for(String prefix : prefixes) {
                    q.setFacetPrefix(prefix);

                    QueryResponse qr = solrAtlas.query(q);
                    if(qr.getFacetFields().get(0).getValues() != null) {
                        int i = 0;
                        for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues())
                            if(ffc.getName().length() > 0 && ffc.getName().toLowerCase().startsWith(query))
                            {
                                result.add(new AutoCompleteItem(property,
                                        ffc.getName(), ffc.getCount()));
                                if(++i >= limit)
                                    break;
                            }
                    }

                    Collections.sort(result);
                    result = result.subList(0, Math.min(result.size(), limit));
                }
            }
        } catch (SolrServerException e) {
            log.error(e);
        }

        return result;
    }
}
