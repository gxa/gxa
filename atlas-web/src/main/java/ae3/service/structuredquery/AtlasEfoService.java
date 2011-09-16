/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package ae3.service.structuredquery;

import ae3.service.AtlasStatisticsQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.statistics.Attribute;
import uk.ac.ebi.gxa.statistics.EfoAttribute;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * EFO value list helper class, implementing autocompletion and value listing for EFO
 *
 * @author pashky
 */
public class AtlasEfoService implements AutoCompleter, IndexBuilderEventHandler, DisposableBean {
    final private Logger log = LoggerFactory.getLogger(getClass());

    private Efo efo;
    private IndexBuilder indexBuilder;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    private final Map<String, Long> counts = new HashMap<String, Long>();

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    public synchronized void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    /**
     * Count genes for ID
     *
     * @param id term ID
     * @return number of matching genes
     */
    private synchronized Long getCount(String id) {
        if (counts.isEmpty()) {
            log.info("Getting counts for ontology");
            Set<String> availIds = efo.getAllTermIds();

            for (String efoTerm : availIds) {
                Attribute attr = new EfoAttribute(efoTerm);
                int geneCount = atlasStatisticsQueryService.getBioEntityCountForEfoAttribute(attr, StatisticsType.UP_DOWN);
                if (geneCount > 0)
                    counts.put(attr.getValue(), (long) geneCount);

            }
            log.info("Done getting counts for ontology");
        }

        return counts.get(id);
    }

    public Collection<AutoCompleteItem> autoCompleteValues(String property, @Nonnull String prefix, int limit) {
        return autoCompleteValues(property, prefix, limit, null);
    }

    /**
     * Autocomplete by EFO
     *
     * @param property factor or property to autocomplete values for, can be empty for any factor
     * @param prefix   prefix
     * @param limit    maximum number of values to find
     * @param filters  prefix filters. Unused here.
     * @return collection of AutoCompleteItem's
     */
    public Collection<AutoCompleteItem> autoCompleteValues(String property, @Nonnull String prefix, int limit, Map<String, String> filters) {

        EfoTermRanking efoTermRanking = new EfoTermRanking(prefix);
        Map<String, Rank> found = new HashMap<String, Rank>();
        for (EfoTerm efoTerm : efo.searchTermPrefix(prefix)) {
            if (getCount(efoTerm.getId()) != null) {
                found.put(efoTerm.getId(), efoTermRanking.getRank(efoTerm));
            }
        }

        Set<String> all = new HashSet<String>(found.keySet());
        for (String id : found.keySet()) {
            all.addAll(efo.getTermParents(id, true));
        }

        List<EfoTerm> subTree = efo.getSubTree(all);

        Stack<EfoAutoCompleteItem> stack = new Stack<EfoAutoCompleteItem>();
        Map<String, AutoCompleteItem> result = new HashMap<String, AutoCompleteItem>();

        for (EfoTerm term : subTree) {
            while (stack.size() > term.getDepth()) {
                stack.pop();
            }

            Long pcount = getCount(term.getId());

            Rank rank = found.get(term.getId());

            EfoAutoCompleteItem item = new EfoAutoCompleteItem(Constants.EFO_FACTOR_NAME,
                    term.getId(), term.getTerm(), pcount,
                    term.getAlternativeTerms());

            if (rank != null) {
                AutoCompleteItem tmp = result.get(item.getId());
                /**
                 * Getting the shorter path makes the trees in the auto-complete drop-down
                 * list shorter and easier to observe. Any other way of choosing
                 * preferable efo path is welcome here.
                 */
                if (tmp == null || stack.size() < tmp.getPath().size()) {
                    result.put(item.getId(), new EfoAutoCompleteItem(item, stack, rank));
                }
            }

            stack.push(item);
        }

        return result.values();
    }

    /**
     * Wrapping class, enriching term information with gene count
     */
    public static class EfoTermCount {
        private EfoTerm term;
        private long count;

        /**
         * Constructor
         *
         * @param term  term
         * @param count gene count
         */
        public EfoTermCount(EfoTerm term, long count) {
            this.term = term;
            this.count = count;
        }

        /**
         * Returns term id
         *
         * @return term id
         */
        public String getId() {
            return term.getId();
        }

        /**
         * Returns term string
         *
         * @return term string
         */
        public String getTerm() {
            return term.getTerm();
        }

        /**
         * Returns if term is expandable
         *
         * @return true if term is expandable
         */
        public boolean isExpandable() {
            return term.isExpandable();
        }

        /**
         * Method to override EFO default expandable flag (has efo children ==> Exapandable)
         * with false in the case when no children have experiment counts in bit index.
         * This functionality is used to prevent shown '+' sign against efos with no scoring children
         * in the efv/efo condition drop-down on Atlas main search page.
         */
        public void setNonExpandable() {
            term.setNonExpandable();
        }

        /**
         * Returns if term is branch root
         *
         * @return true if term is branch root
         */
        public boolean isBranchRoot() {
            return term.isBranchRoot();
        }

        /**
         * Returns gene count
         *
         * @return number of matching genes
         */
        public long getCount() {
            return count;
        }

        /**
         * Returns term depth
         *
         * @return term depth
         */
        public int getDepth() {
            return term.getDepth();
        }

        /**
         * Returns if term is root
         *
         * @return true if term is root
         */
        public boolean isRoot() {
            return term.isRoot();
        }

        /**
         * Returns alternative terms
         *
         * @return list of ids
         */
        public List<String> getAlternativeTerms() {
            return term.getAlternativeTerms();
        }
    }

    /**
     * Returns term direct children with counts
     *
     * @param id term id
     * @return collection of EfoTermCount
     */
    public Collection<EfoTermCount> getTermChildren(String id) {
        List<EfoTermCount> result = new ArrayList<EfoTermCount>();
        if (id == null) {
            for (EfoTerm root : efo.getRoots()) {
                Long count = getCount(root.getId());
                if (count != null)
                    result.add(new EfoTermCount(root, count));
            }
        } else {
            Collection<EfoTerm> children = efo.getTermChildren(id);
            if (children != null)
                for (EfoTerm term : children) {
                    Long count = getCount(term.getId());
                    if (count != null)
                        result.add(new EfoTermCount(term, count));
                }
        }
        return result;
    }

    /**
     * Returns term parent paths with counts
     *
     * @param id term id
     * @return collection of lists of EfoTermCount
     */
    public Collection<List<EfoTermCount>> getTermParentPaths(String id) {
        Collection<List<EfoTerm>> paths = efo.getTermParentPaths(id, true);
        if (paths == null)
            return null;

        List<List<EfoTermCount>> result = new ArrayList<List<EfoTermCount>>();
        for (List<EfoTerm> path : paths) {
            int depth = 0;
            List<EfoTermCount> current = new ArrayList<EfoTermCount>();
            Collections.reverse(path);
            for (EfoTerm term : path) {
                Long count = getCount(term.getId());
                if (count != null) {
                    current.add(new EfoTermCount(new EfoTerm(term, depth++), count));
                }
            }
            if (!current.isEmpty()) {
                Long count = getCount(id);
                if (count != null) {
                    current.add(new EfoTermCount(new EfoTerm(efo.getTermById(id), depth), count));
                    result.add(current);
                }
            }
        }
        return result;
    }

    /**
     * Returns tree down to term
     *
     * @param id term id
     * @return collection of EfoTermCount
     */
    public Collection<EfoTermCount> getTreeDownToTerm(String id) {

        for (EfoTerm found : efo.searchTerm(id))
            if (getCount(found.getId()) != null) {
                Collection<EfoTerm> tree = efo.getTreeDownTo(found.getId());

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
     *
     * @param values list of search strings
     * @return collection of EfoTermCount
     */
    public Collection<EfoTermCount> searchTerms(Collection<String> values) {
        List<EfoTermCount> result = new ArrayList<EfoTermCount>();
        Set<String> ids = new HashSet<String>();
        for (String val : values) {
            for (EfoTerm term : efo.searchTerm(val)) {
                Long count = getCount(term.getId());
                if (count != null && !ids.contains(term.getId())) {
                    result.add(new EfoTermCount(term, count));
                    ids.add(term.getId());
                }
            }
        }
        return result;
    }

    public void onIndexBuildFinish() {
        counts.clear();
    }

    public void onIndexBuildStart() {
    }

    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}
