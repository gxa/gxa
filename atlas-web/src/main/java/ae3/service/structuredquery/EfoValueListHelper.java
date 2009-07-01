package ae3.service.structuredquery;

import uk.ac.ebi.ae3.indexbuilder.Efo;
import uk.ac.ebi.ae3.indexbuilder.Constants;

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
 * @author pashky
 */
public class EfoValueListHelper implements IValueListHelper {
    final private Logger log = LoggerFactory.getLogger(getClass());
    private SolrServer solrAtlas;
    private final Map<String,Long> counts = new HashMap<String,Long>();

    public EfoValueListHelper(SolrServer solrAtlas) {
        this.solrAtlas = solrAtlas;
    }

    private synchronized Long getCount(String id)
    {
        if(counts.isEmpty()) {
            log.info("Getting counts for ontoltogy");
            Set<String> availIds = getEfo().getAllTermIds();

            SolrQuery q = new SolrQuery("gene_id:[* TO *]");
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
        for(Efo.Term term : efo.getSubTree(all)) {
            if(limit >= 0 && limit-- <= 0)
                break;

            Long pcount = getCount(term.getId());
            if(pcount != null)
                result.add(new EfoAutoCompleteItem(Constants.EFO_FACTOR_NAME, term.getId(), term.getTerm(), pcount, term.getDepth()));
        }
        return result;
    }

    public Collection<String> listAllValues(String property) {
        List<String> result = new ArrayList<String>();
        for(Efo.Term term : getEfo().getAllTerms()) {
            result.add(term.getTerm());
        }
        return result;
    }

    public void preloadData() {
        getCount("");
    }

    public Efo getEfo() {
        return Efo.getEfo();
    }

    public static class EfoTermCount {
        private Efo.Term term;
        private long count;

        public EfoTermCount(Efo.Term term, long count) {
            this.term = term;
            this.count = count;
        }

        public String getId() {
            return term.getId();
        }

        public String getTerm() {
            return term.getTerm();
        }

        public boolean isExpandable() {
            return term.isExpandable();
        }

        public boolean isBranchRoot() {
            return term.isBranchRoot();
        }

        public long getCount() {
            return count;
        }

        public int getDepth() {
            return term.getDepth();
        }
    }

    public Collection<EfoTermCount> getTermChildren(String id) {
        List<EfoTermCount> result = new ArrayList<EfoTermCount>();
        if(id == null) {
            for(Efo.Term root : getEfo().getRoots()) {
                Long count = getCount(root.getId());
                if(count != null)
                    result.add(new EfoTermCount(root, count));
            }
        } else  {
            Collection<Efo.Term> children = getEfo().getTermChildren(id);
            if(children != null)
                for(Efo.Term term : children) {
                    Long count = getCount(term.getId());
                    if(count != null)
                        result.add(new EfoTermCount(term, count));
                }
        }
        return result;
    }

    public Collection<List<EfoTermCount>> getTermParentPaths(String id) {
        Collection<List<Efo.Term>> paths = getEfo().getTermParentPaths(id, true);
        if(paths == null)
            return null;

        List<List<EfoTermCount>> result = new ArrayList<List<EfoTermCount>>();
        for(List<Efo.Term> path : paths) {
            int depth = 0;
            List<EfoTermCount> current = new ArrayList<EfoTermCount>();
            Collections.reverse(path);
            for(Efo.Term term : path) {
                Long count = getCount(term.getId());
                if(count != null) {
                    current.add(new EfoTermCount(new Efo.Term(term, depth++), count));
                }
            }
            if(!current.isEmpty()) {
                Long count = getCount(id);
                if(count != null) {
                    current.add(new EfoTermCount(new Efo.Term(getEfo().getTermById(id), depth), count));
                    result.add(current);
                }
            }
        }
        return result;
    }

    public Collection<EfoTermCount> getTreeDownToTerm(String id) {

        for(Efo.Term found : getEfo().searchTerm(id))
            if(getCount(found.getId()) != null) {
                Collection<Efo.Term> tree = getEfo().getTreeDownTo(found.getId());

                List<EfoTermCount> result = new ArrayList<EfoTermCount>();
                if (tree != null) {
                    for (Efo.Term term : tree) {
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

    public Collection<EfoTermCount> searchTerms(Collection<String> values) {
        List<EfoTermCount> result = new ArrayList<EfoTermCount>();
        Set<String> ids = new HashSet<String>();
        for(String val : values) {
            for (Efo.Term term : getEfo().searchTerm(val)) {
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
