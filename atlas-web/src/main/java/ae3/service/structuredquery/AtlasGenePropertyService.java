package ae3.service.structuredquery;

import ae3.util.AtlasProperties;
import ae3.util.HtmlHelper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.*;

/**
 * Gene properties values listing and autocompletion helper implementation
 * @author pashky
 * @see AutoCompleter
 */
public class AtlasGenePropertyService implements AutoCompleter {
    private SolrServer solrServerAtlas;

    private final Set<String> idProperties;
    private final Set<String> descProperties;
    private final List<String> nameFields;
    private final int nameLimit;
    private final int idLimit;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String,PrefixNode> prefixTrees = new HashMap<String,PrefixNode>();

    public SolrServer getSolrServerAtlas() {
        return solrServerAtlas;
    }

    public void setSolrServerAtlas(SolrServer solrServiceAtlas) {
        this.solrServerAtlas = solrServiceAtlas;
    }

    public AtlasGenePropertyService()
    {
        this.idProperties = new HashSet<String>(Arrays.asList(AtlasProperties.getProperty("atlas.gene.autocomplete.ids").split(",")));
        this.descProperties = new HashSet<String>(Arrays.asList(AtlasProperties.getProperty("atlas.gene.autocomplete.descs").split(",")));

        this.idLimit = AtlasProperties.getIntProperty("atlas.gene.autocomplete.ids.limit");
        this.nameLimit = AtlasProperties.getIntProperty("atlas.gene.autocomplete.names.limit");

        this.nameFields = new ArrayList<String>();
        nameFields.add("identifier");
        nameFields.add("name_f");
        for(String nameProp : AtlasProperties.getProperty("atlas.gene.autocomplete.names").split(","))
            nameFields.add("property_f_" + nameProp);
    }

    private Collection<GeneAutoCompleteItem> treeAutocomplete(final String property, final String prefix, final int limit) {
        PrefixNode root = treeGetOrLoad("property_f_" + property);

        final List<GeneAutoCompleteItem> result = new ArrayList<GeneAutoCompleteItem>();
        if(root != null) {
            root.walk(prefix, 0, "", new PrefixNode.WalkResult() {
                public void put(String name, int count) {
                    result.add(new GeneAutoCompleteItem(property, name, (long)count, null, null, null));
                }
                public boolean enough() {
                    return limit >=0 && result.size() >= limit;
                }
            });
        }
        return result;
    }

    public void preloadData() {
        for(String property : idProperties)
            treeGetOrLoad("property_f_" + property + "_f");
        for(String property : descProperties)
            treeGetOrLoad("property_f_" + property + "_f");
        for(String field : nameFields)
            treeGetOrLoad(field);
    }

    private PrefixNode treeGetOrLoad(String field) {
        PrefixNode root;
        synchronized(prefixTrees) {
            if(!prefixTrees.containsKey(field)) {
                log.info("Loading gene property values and counts for " + field);
                SolrQuery q = new SolrQuery("id:[* TO *]");
                q.setRows(0);
                q.setFacet(true);
                q.setFacetMinCount(1);
                q.setFacetLimit(-1);
                q.setFacetSort(true);
                q.addFacetField(field);
                
                try {
                    QueryResponse qr = solrServerAtlas.query(q);
                    root = new PrefixNode();
                    if(qr.getFacetFields() != null && qr.getFacetFields().get(0) != null
                            && qr.getFacetFields().get(0).getValues() != null) {
                        for(FacetField.Count ffc : qr.getFacetFields().get(0).getValues())
                            if(ffc.getName().length() > 0) {
                                root.add(ffc.getName(), (int)ffc.getCount());
                            }
                    }
                    prefixTrees.put(field, root);
                } catch (SolrServerException e) {
                    throw new RuntimeException(e);
                }
                log.info("Done loading gene property values and counts for " + field);
            }
            root = prefixTrees.get(field);
        }
        return root;
    }

    public Collection<AutoCompleteItem> autoCompleteValues(String property, String query, int limit, Map<String,String> filters) {

        boolean hasPrefix = query != null && !"".equals(query);
        if(hasPrefix)
            query = query.toLowerCase();

        int speciesFilter = -1;
        try {
            speciesFilter = Integer.valueOf(filters.get("species"));
        } catch(Exception e) {
            // okay
        }

        boolean anyProp = property == null || property.equals("");

        List<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();
        if(anyProp) {

            for(String p : idProperties)
                result.addAll(treeAutocomplete(p, query, idLimit));
            Collections.sort(result);
            if(result.size() > idLimit)
                result = result.subList(0, idLimit);

            result.addAll(joinGeneNames(query, speciesFilter, nameLimit));

            for(String p : descProperties)
                result.addAll(treeAutocomplete(p, query, limit > 0 ? limit - result.size() : -1));

            result = result.subList(0, Math.min(result.size(), limit));
        } else {
            if(Constants.GENE_PROPERTY_NAME.equals(property)) {
                result.addAll(joinGeneNames(query, -1, limit));
            } else if(idProperties.contains(property) || descProperties.contains(property)) {
                result.addAll(treeAutocomplete(property, query, limit));
            }
            Collections.sort(result);
        }

        return result;
    }

    private List<AutoCompleteItem> joinGeneNames(final String query, final int speciesFilter, final int limit) {
        final List<AutoCompleteItem> res = new ArrayList<AutoCompleteItem>();
        final Set<String> ids = new HashSet<String>();

        final StringBuffer sb = new StringBuffer();

        for(final String field : nameFields) {
            treeGetOrLoad(field).walk(query, 0, "", new PrefixNode.WalkResult() {
                public void put(String name, int count) {
                    if(sb.length() > 0)
                        sb.append(" ");
                    sb.append(field).append(":").append(EscapeUtil.escapeSolr(name));
                    if(sb.length() > 800) {
                        if(speciesFilter >= 0)
                            sb.insert(0, "(").append(") AND species_id:").append(speciesFilter);
                        findAutoCompleteGenes(sb.toString(), query, res, ids);
                        sb.setLength(0);
                    }
                }
                public boolean enough() {
                    return res.size() >= limit;
                }
            });
        }

        if(sb.length() > 0) {
            if(speciesFilter >= 0)
                sb.insert(0, "(").append(") AND species_id:").append(speciesFilter);
            findAutoCompleteGenes(sb.toString(), query, res, ids);
        }

        Collections.sort(res);
        return res.subList(0, Math.min(limit >= 0 ? limit : res.size(), res.size()));
    }

    private void findAutoCompleteGenes(final String query, final String prefix, List<AutoCompleteItem> res, Set<String> ids) {
        SolrQuery q = new SolrQuery(query);
        q.setStart(0);
        q.setRows(50);
        for(String field : nameFields)
            q.addField(field);
        q.addField("species");
        q.addField("identifier");
        q.addField("name");
        try {
            QueryResponse qr = solrServerAtlas.query(q);
            for(SolrDocument doc : qr.getResults())
            {
                String name = null;

                String species = (String)doc.getFieldValue("species");
                if(species == null)
                    species = "";
                else
                    species = HtmlHelper.upcaseFirst(species.replace("$",""));

                String geneId = (String)doc.getFieldValue("identifier");

                List<String> names = new ArrayList<String>();
                for(String s : doc.getFieldNames())
                    if(!s.equals("species")) {
                        @SuppressWarnings("unchecked")
                        Collection<String> c = (Collection)doc.getFieldValues(s);
                        if(c != null)
                            for(String v : c) {
                                if(name == null && v.toLowerCase().startsWith(prefix)) {
                                    name = v;
                                } else if(name != null && v.toLowerCase().startsWith(prefix) && v.toLowerCase().length() < name.length()) {
                                    names.add(name);
                                    name = v;
                                } else if(!v.equals(name) && !names.contains(v))
                                    names.add(v);
                            }
                    }

                if(name != null && !ids.contains(geneId)) {
                    ids.add(geneId);
                    res.add(new GeneAutoCompleteItem(Constants.GENE_PROPERTY_NAME, name, 1L, species, geneId, names));
                }
            }
        } catch(SolrServerException e) {
            throw new RuntimeException(e);
        }
    }
}
