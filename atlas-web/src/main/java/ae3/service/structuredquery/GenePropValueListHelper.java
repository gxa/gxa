package ae3.service.structuredquery;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import ae3.service.structuredquery.GeneProperties.Prop;
import ae3.service.structuredquery.GeneProperties.PropType;
import ae3.util.EscapeUtil;

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
            q.setFacetLimit(limit);
            q.setFacetSort(true);

            if(anyProp) {
                for(Prop p : GeneProperties.allProperties())
                    q.addFacetField(p.facetField);
            } else if(GeneProperties.isNameProperty(property)) {
                q.addFacetField("gene_name_exact");
            } else {
                q.addFacetField(GeneProperties.convertPropertyToFacetField(property));
            }

            List<String> prefixes;
            if(hasPrefix) {
                prefixes = generateCaseVariants(query.substring(0, query.length()));
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
                                        resmap.get(p.type).add(new AutoCompleteItem(p.id, ffc.getName(), ffc.getCount(), null));
                                    }
                            }
                        }
                }

                joinGeneNames(query, result, resmap.get(PropType.NAME));

                for(PropType p : PropType.values())
                    if(p != PropType.NAME)
                    {
                        List<AutoCompleteItem> l = p.limit > 0 && p.limit < resmap.get(p).size()
                                ? resmap.get(p).subList(0, p.limit) : resmap.get(p);

                        Collections.sort(l);
                        result.addAll(l);
                        if(limit > 0 && result.size() >= limit)
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
                            if(ffc.getName().length() > 0 && (!hasPrefix || ffc.getName().toLowerCase().startsWith(query)))
                            {
                                result.add(new AutoCompleteItem(property,
                                        ffc.getName(), ffc.getCount(), null));
                                if(limit > 0 && ++i >= limit)
                                    break;
                            }
                    }
                }

                if(GeneProperties.isNameProperty(property)) {
                    List<AutoCompleteItem> list = new ArrayList<AutoCompleteItem>();
                    joinGeneNames(query, list, result);
                    result = list;
                }

                Collections.sort(result);
                if(limit > 0)
                    result = result.subList(0, Math.min(result.size(), limit));
            }
        } catch (SolrServerException e) {
            log.error(e);
        }

        return result;
    }

    private void joinGeneNames(String query, List<AutoCompleteItem> result, Iterable<AutoCompleteItem> source) throws SolrServerException {
        if(!source.iterator().hasNext())
            return;

        SolrQuery q;
        StringBuffer sb = new StringBuffer();

        int num = 50;
        for(AutoCompleteItem i : source) {
            if(sb.length() > 0)
                sb.append(" ");

            sb.append(GeneProperties.convertPropertyToFacetField(i.getProperty()))
                    .append(":")
                    .append(EscapeUtil.escapeSolr(i.getValue()));
            if(--num == 0)
                break;
        }

        q = new SolrQuery(sb.toString());
        q.setStart(0);
        q.setRows(PropType.NAME.limit);
        for(Prop p : GeneProperties.allProperties())
            if(p.type == PropType.NAME)
                q.addField(p.searchField);
        q.addField("gene_species");
        q.addField("gene_identifier");
        QueryResponse qr = solrAtlas.query(q);
        for(SolrDocument doc : qr.getResults())
        {
            String name = null;
            String species = (String)doc.getFieldValue("gene_species");
            if(species == null)
                species = "";
            else
                species = species.substring(0,1).toUpperCase().concat(species.substring(1).toLowerCase()).replace("$","");

            String geneId = (String)doc.getFieldValue("gene_identifier");

            Set<String> names = new HashSet<String>();
            for(String s : doc.getFieldNames())
                if(!s.equals("gene_species")) {
                    Collection c = doc.getFieldValues(s);
                    if(c != null)
                        for(String v : (Collection<String>)c) {
                            if(name == null && v.toLowerCase().startsWith(query))
                                name = v;
                            else
                                names.add(v);
                        }
                }

            if(name != null)
                result.add(new AutoCompleteItem("name", name, 1L,
                        (names.size() > 0 ? "(" + StringUtils.join(names, ",").replace("$","") + ")" : "") +
                                "$" + species + "$" + geneId
                ));
        }
    }
}
