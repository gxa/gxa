package ae3.service.structuredquery;

import ae3.service.structuredquery.GeneProperties.Prop;
import ae3.service.structuredquery.GeneProperties.PropType;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import uk.ac.ebi.gxa.utils.EscapeUtil;

/**
 * Gene properties values listing and autocompletion helper implementation
 * @author pashky
 * @see ae3.service.structuredquery.IValueListHelper
 */
public class GenePropValueListHelper implements IValueListHelper {
    private SolrServer solrAtlas;
    private Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String,PrefixNode> prefixTrees = new HashMap<String,PrefixNode>();

    public GenePropValueListHelper(SolrServer solrAtlas)
    {
        this.solrAtlas = solrAtlas;
    }

    private Collection<GeneAutoCompleteItem> treeAutocomplete(final String property, final String prefix, final int limit) {
        PrefixNode root = treeGetOrLoad(property);

        final List<GeneAutoCompleteItem> result = new ArrayList<GeneAutoCompleteItem>();
        if(root != null) {
            root.walk(prefix, 0, "", new PrefixNode.WalkResult() {
                public void put(String name, int count) {
                    result.add(new GeneAutoCompleteItem(property, name, (long)count, null, null, null, null));
                }
                public boolean enough() {
                    return limit >=0 && result.size() >= limit;
                }
            });
        }
        return result;
    }

    public void preloadData() {
        for(String property : GeneProperties.allPropertyIds()) {
            treeGetOrLoad(property);
        }
    }

    private PrefixNode treeGetOrLoad(String property) {
        PrefixNode root;
        synchronized(prefixTrees) {
            if(!prefixTrees.containsKey(property)) {
                log.info("Loading gene property values and counts for " + property);
                SolrQuery q = new SolrQuery("gene_id:[* TO *]");
                q.setRows(0);
                q.setFacet(true);
                q.setFacetMinCount(1);
                q.setFacetLimit(-1);
                q.setFacetSort(true);
                String field = GeneProperties.convertPropertyToFacetField(property);
                if(field == null)
                    return null;
                
                q.addFacetField(field);
                try {
                    QueryResponse qr = solrAtlas.query(q);
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
                log.info("Done loading gene property values and counts for " + property);
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
            for(String prop : GeneProperties.allPropertyIds()) {
                PrefixNode root = treeGetOrLoad(prop);
                if(root != null)
                    root.collect("", rc);
            }
        } else if(GeneProperties.isNameProperty(property) || GeneProperties.GENE_PROPERTY_NAME.equals(property))  {
            for(Prop p : GeneProperties.allProperties())
                if(p.type == PropType.NAME) {
                    PrefixNode root = treeGetOrLoad(p.id);
                    if(root != null)
                        root.collect("", rc);
                }
        } else {
            PrefixNode root = treeGetOrLoad(property);
            if(root != null)
                root.collect("", rc);
        }
        return result;
    }

    public Collection<AutoCompleteItem> autoCompleteValues(String property, String query, int limit, Map<String,String> filters) {

        boolean hasPrefix = query != null && !"".equals(query);
        if(hasPrefix)
            query = query.toLowerCase();

        boolean anyProp = property == null || property.equals("");

        List<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();
        try {
            if(anyProp) {

                EnumMap<PropType, List<GeneAutoCompleteItem>> resmap = new EnumMap<PropType, List<GeneAutoCompleteItem>>(PropType.class);
                for(PropType e : PropType.values())
                    resmap.put(e, new ArrayList<GeneAutoCompleteItem>());

                for(Prop p : GeneProperties.allProperties()) {
                    resmap.get(p.type).addAll(treeAutocomplete(p.id, query, p.type.limit));
                }

                joinGeneNames(query, result, resmap.get(PropType.NAME), filters.get("species"), PropType.NAME.limit);

                for(PropType p : PropType.values())
                    if(p != PropType.NAME)
                    {
                        List<GeneAutoCompleteItem> l = p.limit > 0 && p.limit < resmap.get(p).size()
                                ? resmap.get(p).subList(0, p.limit) : resmap.get(p);

                        Collections.sort(l);
                        result.addAll(l);
                        if(limit > 0 && result.size() >= limit)
                            break;
                    }
                result = result.subList(0, Math.min(result.size(), limit));
            } else {
                if(GeneProperties.isNameProperty(property) || GeneProperties.GENE_PROPERTY_NAME.equals(property)) {
                    List<GeneAutoCompleteItem> list = new ArrayList<GeneAutoCompleteItem>();
                    for(Prop p : GeneProperties.allProperties())
                        if(p.type == PropType.NAME)
                            list.addAll(treeAutocomplete(p.id, query, limit));
                    joinGeneNames(query, result, list, null, limit);
                } else if(null == GeneProperties.convertPropertyToFacetField(property)) {
                    return result;
                } else {
                    result.addAll(treeAutocomplete(property, query, limit));
                }

                Collections.sort(result);
                if(limit > 0)
                    result = result.subList(0, Math.min(result.size(), limit));
            }
        } catch (SolrServerException e) {
            log.error("Error querying Atlas index", e);
        }

        return result;
    }

    private void joinGeneNames(String query, List<AutoCompleteItem> result, Iterable<GeneAutoCompleteItem> source, String speciesFilter, int limit) throws SolrServerException {
        Iterator<GeneAutoCompleteItem> iterator = source.iterator();
        if(!iterator.hasNext())
            return;

        Set<String> ids = new HashSet<String>();
        List<AutoCompleteItem> res = new ArrayList<AutoCompleteItem>();
        while(iterator.hasNext()) {
            SolrQuery q;
            StringBuffer sb = new StringBuffer();

            int num = 0;
            while(num++ < 50 && iterator.hasNext()) {
                GeneAutoCompleteItem i = iterator.next();
                if(sb.length() > 0)
                    sb.append(" ");

                sb.append(GeneProperties.convertPropertyToFacetField(i.getProperty()))
                        .append(":")
                        .append(EscapeUtil.escapeSolr(i.getValue()));
            }

            if(speciesFilter != null && speciesFilter.length() > 0)
                sb.insert(0, "(").append(") AND gene_species:(").append(EscapeUtil.escapeSolr(speciesFilter)).append(")");

            q = new SolrQuery(sb.toString());
            q.setStart(0);
            q.setRows(50);
            for(Prop p : GeneProperties.allProperties())
                if(p.type == PropType.NAME)
                    q.addField(p.searchField);
            q.addField("gene_species");
            q.addField("gene_identifier");
            QueryResponse qr = solrAtlas.query(q);
            for(SolrDocument doc : qr.getResults())
            {
                String name = null;
                String nameSource = null;

                String species = (String)doc.getFieldValue("gene_species");
                if(species == null)
                    species = "";
                else
                    species = species.substring(0,1).toUpperCase().concat(species.substring(1).toLowerCase()).replace("$","");

                String geneId = (String)doc.getFieldValue("gene_identifier");

                List<String> names = new ArrayList<String>();
                for(String s : doc.getFieldNames())
                    if(!s.equals("gene_species")) {
                        Collection c = doc.getFieldValues(s);
                        if(c != null)
                            for(String v : (Collection<String>)c) {
                                if(name == null && v.toLowerCase().startsWith(query)) {
                                    nameSource = GeneProperties.findPropBySearchField(s).id;
                                    name = v;
                                } else if(name != null && v.toLowerCase().startsWith(query) && v.toLowerCase().length() < name.length()) {
                                    names.add(name);
                                    nameSource = GeneProperties.findPropBySearchField(s).id;
                                    name = v;
                                } else if(!v.equals(name) && !names.contains(v))
                                    names.add(v);
                            }
                    }

                if(name != null && !ids.contains(geneId)) {
                    ids.add(geneId);
                    res.add(new GeneAutoCompleteItem(GeneProperties.GENE_PROPERTY_NAME, name, 1L, species, geneId, names, nameSource));
                }
            }
        }
        Collections.sort(res);
        result.addAll(res.subList(0, Math.min(limit >= 0 ? limit : res.size(), res.size())));
    }
}
