package ae3.service.structuredquery;

import uk.ac.ebi.ae3.indexbuilder.efo.Efo;
import uk.ac.ebi.ae3.indexbuilder.efo.EfoTerm;
import ae3.service.structuredquery.Constants;

import java.util.*;

import java.util.ArrayList;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EFO value list helper class, implementing autocompletion and value listing for EFO
 * @author pashky
 */
public class EfoValueListHelper implements AutoCompleter {
    final private Logger log = LoggerFactory.getLogger(getClass());
    private SolrServer solrAtlas;
    private final Map<String,Long> counts = new HashMap<String,Long>();

    /**
     * Constructor needs atlas solr server reference to proceed with gene counts
     * @param solrAtlas atlas solr server reference
     */
    public EfoValueListHelper(SolrServer solrAtlas) {
        this.solrAtlas = solrAtlas;
    }

    /**
     * Count genes for ID
     * @param id term ID
     * @return number of matching genes
     */
    private synchronized Long getCount(String id)
    {
        if(counts.isEmpty()) {
            log.info("Getting counts for ontoltogy");
            Set<String> availIds = getEfo().getAllTermIds();

            SolrQuery q = new SolrQuery("id:[* TO *]");
            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);
            q.setFacetLimit(-1);
            q.setFacetSort(true);
            q.addFacetField("efos_ud");
            try {
                QueryResponse qr = solrAtlas.query(q);
                if(qr.getFacetFields() != null && qr.getFacetFields().get(0) != null
                        && qr.getFacetFields().get(0).getValues() != null) {
                    for(FacetField.Count ffc : qr.getFacetFields().get(0).getValues())
                        if(ffc.getName().length() > 0 && ffc.getCount() > 0 && availIds.contains(ffc.getName())) {
                            counts.put(ffc.getName(), ffc.getCount());
                        }
                }

            } catch (SolrServerException e) {
                throw new RuntimeException(e);
            }
            log.info("Done getting counts for ontoltogy");
        }

        return counts.get(id);
    }

    /**
     * Autocomplete by EFO
     * @param property factor or property to autocomplete values for, can be empty for any factor
     * @param query prefix
     * @param limit maximum number of values to find
     * @param filters query filters. Unused here.
     * @return collection of AutoCompleteItem's
     */
    public Collection<AutoCompleteItem> autoCompleteValues(String property, String query, int limit, Map<String,String> filters) {

        Efo efo = getEfo();

        List<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();
        Set<String> found = efo.searchTermPrefix(query);
        for(Iterator<String> i = found.iterator(); i.hasNext();) {
            if (getCount(i.next()) == null)
                i.remove();
        }
        Set<String> all = new HashSet<String>(found);
        for(String id : found) {
            all.addAll(efo.getTermParents(id, true));
        }
        for(EfoTerm term : efo.getSubTree(all)) {
            if(limit >= 0 && limit-- <= 0)
                break;

            Long pcount = getCount(term.getId());
            if(pcount != null)
                result.add(new EfoAutoCompleteItem(Constants.EFO_FACTOR_NAME, term.getId(), term.getTerm(), pcount, term.getDepth()));
        }
        return result;
    }

    /**
     * List all EFO terms
     * @param property factor
     * @return collection of all EFO terms
     */
    public Collection<String> listAllValues(String property) {
        List<String> result = new ArrayList<String>();
        for(EfoTerm term : getEfo().getAllTerms()) {
            result.add(term.getTerm());
        }
        return result;
    }

    /**
     * Load all counts
     */
    public void preloadData() {
        getCount("");
    }

    /**
     * Returns reference to EFO
     * @return
     */
    public Efo getEfo() {
        return Efo.getEfo();
    }

    /**
     * Wrapping class, enriching term information with gene count
     */
    public static class EfoTermCount {
        private EfoTerm term;
        private long count;

        /**
         * Constructor
         * @param term term
         * @param count gene count
         */
        public EfoTermCount(EfoTerm term, long count) {
            this.term = term;
            this.count = count;
        }

        /**
         * Returns term id
         * @return term id
         */
        public String getId() {
            return term.getId();
        }

        /**
         * Returns term string
         * @return term string
         */
        public String getTerm() {
            return term.getTerm();
        }

        /**
         * Returns if term is expandable
         * @return true if term is expandable
         */
        public boolean isExpandable() {
            return term.isExpandable();
        }

        /**
         * Returns if term is branch root
         * @return true if term is branch root
         */
        public boolean isBranchRoot() {
            return term.isBranchRoot();
        }

        /**
         * Returns gene count
         * @return number of matching genes
         */
        public long getCount() {
            return count;
        }

        /**
         * Returns term depth
         * @return term depth
         */
        public int getDepth() {
            return term.getDepth();
        }

        /**
         * Returns if term is root
         * @return true if term is root
         */
        public boolean isRoot() {
            return term.isRoot();
        }
    }

    /**
     * Returns term direct children with counts
     * @param id term id
     * @return collection of EfoTermCount
     */
    public Collection<EfoTermCount> getTermChildren(String id) {
        List<EfoTermCount> result = new ArrayList<EfoTermCount>();
        if(id == null) {
            for(EfoTerm root : getEfo().getRoots()) {
                Long count = getCount(root.getId());
                if(count != null)
                    result.add(new EfoTermCount(root, count));
            }
        } else  {
            Collection<EfoTerm> children = getEfo().getTermChildren(id);
            if(children != null)
                for(EfoTerm term : children) {
                    Long count = getCount(term.getId());
                    if(count != null)
                        result.add(new EfoTermCount(term, count));
                }
        }
        return result;
    }

    /**
     * Returns term parent paths with counts
     * @param id term id
     * @return collection of lists of EfoTermCount
     */
    public Collection<List<EfoTermCount>> getTermParentPaths(String id) {
        Collection<List<EfoTerm>> paths = getEfo().getTermParentPaths(id, true);
        if(paths == null)
            return null;

        List<List<EfoTermCount>> result = new ArrayList<List<EfoTermCount>>();
        for(List<EfoTerm> path : paths) {
            int depth = 0;
            List<EfoTermCount> current = new ArrayList<EfoTermCount>();
            Collections.reverse(path);
            for(EfoTerm term : path) {
                Long count = getCount(term.getId());
                if(count != null) {
                    current.add(new EfoTermCount(new EfoTerm(term, depth++), count));
                }
            }
            if(!current.isEmpty()) {
                Long count = getCount(id);
                if(count != null) {
                    current.add(new EfoTermCount(new EfoTerm(getEfo().getTermById(id), depth), count));
                    result.add(current);
                }
            }
        }
        return result;
    }

    /**
     * Returns tree down to term
     * @param id term id
     * @return collection of EfoTermCount
     */
    public Collection<EfoTermCount> getTreeDownToTerm(String id) {

        for(EfoTerm found : getEfo().searchTerm(id))
            if(getCount(found.getId()) != null) {
                Collection<EfoTerm> tree = getEfo().getTreeDownTo(found.getId());

                List<EfoTermCount> result = new ArrayList<EfoTermCount>();
                if (tree != null) {
                    for (EfoTerm term : tree) {
                        Long count = getCount(term.getId());
                        if (count != null) {
                            result.add(new EfoTermCount(term, count));
                        }
                    }
                }
                return result;
            }
        return getTermChildren(null);
    }

    /**
     * Searches for term texts
     * @param values list of search strings
     * @return collection of EfoTermCount
     */
    public Collection<EfoTermCount> searchTerms(Collection<String> values) {
        List<EfoTermCount> result = new ArrayList<EfoTermCount>();
        Set<String> ids = new HashSet<String>();
        for(String val : values) {
            for (EfoTerm term : getEfo().searchTerm(val)) {
                Long count = getCount(term.getId());
                if (count != null && !ids.contains(term.getId())) {
                    result.add(new EfoTermCount(term, count));
                    ids.add(term.getId());
                }
            }
        }
        return result;
    }
}
