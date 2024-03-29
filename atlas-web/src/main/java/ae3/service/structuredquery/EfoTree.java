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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ae3.service.structuredquery;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.utils.Maker;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

/**
 * EFO tree handling helper class
 *
 * @author pashky
 */
public class EfoTree<PayLoad extends Comparable<PayLoad>> {
    private Efo efo;
    private Map<String, PayLoad> efos = new HashMap<String, PayLoad>();
    private Set<String> marked = new HashSet<String>();
    private Set<String> nonExpandable = new HashSet<String>();
    private Set<String> explicitEfos = new HashSet<String>();
    private Set<String> autoChildren = new HashSet<String>();

    /**
     * Constructs objects
     *
     * @param efo reference to EFO for use
     */
    public EfoTree(Efo efo) {
        this.efo = efo;
    }

    private Iterator<PayLoad> efoMapper(Iterator<String> idIter) {
        return Iterators.transform(idIter, new Function<String, PayLoad>() {
            public PayLoad apply(@Nullable String id) {
                return efos.get(id);
            }
        });
    }

    /**
     * This method is used when heatmap column ordering needs to be imposed after an efo tree was created
     * (c.f. AtlasStructuredQueryService.processResultGenes())
     *
     * @param id      efo term whose payload will be overriden with payload
     * @param payload override
     */
    public void setPayload(String id, PayLoad payload) {
        efos.put(id, payload);
    }

    /**
     * @param id efo term
     * @return current payload of id
     */
    public PayLoad getPayload(String id) {
        return efos.get(id);
    }

    /**
     * Add element by ID and all relevant nodes (currently, optionally one level up and/or all children recursively)
     *
     * @param id                         ID string
     * @param plCreator                  payload creator factory
     * @param maxEfoDescendantGeneration Specifies the generation down to which this efo's descendants should be included
     * @param withParents                add parents or not
     * @return iterable of all payloads affected by this addition
     */
    public Iterable<PayLoad> add(final String id, final Maker<PayLoad> plCreator, final int maxEfoDescendantGeneration, final boolean withParents) {
        Iterable<PayLoad> payloads = new Iterable<PayLoad>() {
            public Iterator<PayLoad> iterator() {
                return Iterators.concat(
                        withParents ?
                                efoMapper(efo.getTermFirstParents(id).iterator()) :
                                Collections.<PayLoad>emptySet().iterator(),
                        Collections.<PayLoad>singletonList(efos.get(id)).iterator(),
                        efoMapper(efo.getTermAndAllChildrenIds(id, maxEfoDescendantGeneration).iterator()));
            }
        };

        if (withParents) {
            Set<String> parents = efo.getTermFirstParents(id);
            if (parents == null) // it's not in EFO, don't add it
                return Collections.emptySet();

            for (String pId : parents)
                if (!efos.containsKey(pId))
                    efos.put(pId, plCreator.make());
        }

        for (String c : efo.getTermAndAllChildrenIds(id, maxEfoDescendantGeneration)) {
            if (!c.equals(id) && !efos.containsKey(c))
                efos.put(c, plCreator.make());
            autoChildren.add(c);
        }


        if (efos.containsKey(id) && explicitEfos.contains(id)) {
            return payloads;
        }

        explicitEfos.add(id);


        if (!efos.containsKey(id))
            efos.put(id, plCreator.make());

        return payloads;
    }

    /**
     * Returns number of elements in tree including all automatically added
     *
     * @return number of elements
     */
    public int getNumEfos() {
        return efos.size();
    }

    /**
     * Return number of elements explicitly added
     *
     * @return number of elements
     */
    public int getNumExplicitEfos() {
        return explicitEfos.size();
    }

    /**
     * Return set of all element IDs
     *
     * @return set of string element IDs
     */
    public Set<String> getEfoIds() {
        return efos.keySet();
    }

    /**
     * Return set of explicitly added element IDs
     *
     * @return set of string element IDs
     */
    public Set<String> getExplicitEfos() {
        return explicitEfos;
    }

    /**
     * View helper class representing one tree node
     *
     * @param <PayLoad> payload type
     */
    public static class EfoItem<PayLoad> implements Serializable {
        private EfoTerm term;
        private PayLoad payload;
        private boolean explicit;
        // This variable is used to override term's isExpandable() value in the case when
        // an efo term displayed at the top of heatmap is itself expandable (i.e. has children)
        // but because the heatmap contains all its scoring children for the user's query, there is
        // no point allowing user to expand it. In such case isExpandable is set to false, which in turn
        // prevents query-result.jsp from creating a clickable map over such efo term to make it expandable.
        // It also prevents DiagonalTextRender from drawing a '+' sign in the same spot as the clickable map
        // (the '+' sign alerts the user that the efo term can be expanded)
        private Boolean isExpandable = null;

        private EfoItem(EfoTerm term, PayLoad payload, boolean explicit) {
            this.term = term;
            this.payload = payload;
            this.explicit = explicit;
        }

        private EfoItem(EfoTerm term, PayLoad payload, boolean explicit, Boolean isExpandable) {
            this.term = term;
            this.payload = payload;
            this.explicit = explicit;
            this.isExpandable = isExpandable;
        }

        /**
         * Returns node depth relative to subtree root
         *
         * @return depth value
         */
        public int getDepth() {
            return term.getDepth();
        }

        /**
         * Returns element id
         *
         * @return string node ID
         */
        public String getId() {
            return term.getId();
        }

        /**
         * Returns element payload
         *
         * @return payload value
         */
        public PayLoad getPayload() {
            return payload;
        }

        /**
         * Returns string term description
         *
         * @return term description
         */
        public String getTerm() {
            return term.getTerm();
        }

        /**
         * Returns whether node is root one (in absolute tree)
         *
         * @return true if yes
         */
        public boolean isRoot() {
            return term.isRoot();
        }

        /**
         * Returns whether node is a branch root
         *
         * @return true if yes
         */
        public boolean isBranchRoot() {
            return term.isBranchRoot();
        }

        /**
         * Returns whether node is expandable (on override value is consulted first; if not set - isExpandable() is called on term)
         *
         * @return true if yes
         */
        public boolean isExpandable() {
            if (isExpandable != null)
                return isExpandable;
            return term.isExpandable();
        }

        /**
         * Returns is node was explicitly added to tree
         *
         * @return true if yes
         */
        public boolean isExplicit() {
            return explicit;
        }

        /**
         * Returns list of laternative terms (if any)
         *
         * @return list of strings, may be empty
         */
        public List<String> getAlternativeTerms() {
            return term.getAlternativeTerms();
        }
    }

    /**
     * Returns flattened representation of the marked nodes subtrees as list ordered in print order
     * Each subtree starts from depth=0
     *
     * @return list of EfoItems
     */
    public List<EfoItem<PayLoad>> getMarkedSubTreeList() {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        for (EfoTerm t : efo.getSubTree(marked)) {
            result.add(new EfoItem<PayLoad>(t, efos.get(t.getId()), explicitEfos.contains(t.getId()), !nonExpandable.contains(t.getId())));
        }
        return result;
    }

    /**
     * Returns nodes list view as a list, ordered by payload value
     *
     * @return list of EfoItems
     */
    public List<EfoItem<PayLoad>> getValueOrderedList() {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        List<String> ids = new ArrayList<String>(efos.keySet());
        Collections.sort(ids, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return efos.get(o1).compareTo(efos.get(o2));
            }
        });
        for (String id : ids) {
            EfoTerm t = efo.getTermById(id);
            result.add(new EfoItem<PayLoad>(t, efos.get(t.getId()), explicitEfos.contains(t.getId())));
        }
        return result;
    }

    /**
     * Returns flat list of explicitly added nodes
     *
     * @return list of EfoItems
     */
    public List<EfoItem<PayLoad>> getExplicitList() {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        for (String id : explicitEfos) {
            EfoTerm t = efo.getTermById(id);
            result.add(new EfoItem<PayLoad>(t, efos.get(t.getId()), explicitEfos.contains(t.getId())));
        }
        return result;
    }

    /**
     * Nice string representation of the tree
     *
     * @return string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (EfoItem<PayLoad> i : getExplicitList()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(i.getId()).append("(").append(i.getTerm()).append(")=").append(i.getPayload());
        }
        return sb.toString();
    }

    /**
     * Mark a node by ID and all relevant nodes too (according to rules specified in add() method)
     *
     * @param id string ID of node to mark
     */
    public void mark(String id, final boolean withParents) {
        if (marked.contains(id))
            return;

        if (explicitEfos.contains(id)) {
            if (withParents)
                marked.addAll(efo.getTermFirstParents(id));
            marked.add(id);
        } else if (autoChildren.contains(id)) {
            marked.add(id);
        }
    }

    /**
     * 1. If efo term id is being shown in heatmap header, than mark all its immediate parents as non-expandable
     * 2. If efo term id's child is being shown in heatmap header,  make id non-expandable
     * 3. If no children of id are marked to be shown in heatmap header, check if any children with non-zero experiment counts
     *     in bit index exist. If not, tag id as non-expandable.
     * @param id Efo term's id
     */
    public void setNonExpandableIfApplicable(String id, Set<String> scoringEfos) {
        if (nonExpandable.contains(id)) {
            return;
        }
        if (marked.contains(id)) {
            // If id is being shown, than mark all its immediate parents as non-expandable
            for (String parent : efo.getTermFirstParents(id)) {
                nonExpandable.add(parent);
            }
        }

        Set<String> childrenEfos = new HashSet<String>();
        for (EfoTerm child : efo.getTermChildren(id)) {
            childrenEfos.add(child.getId());
            if (marked.contains(child.getId())) {
                // If id's child is already being shown, make id non-expandable
                nonExpandable.add(id);
                return;
            }
        }

        // If no children of id are marked, check if any children with non-zero experiment counts in bit index in fact exist
        if (!containsAtLeastOne(childrenEfos, scoringEfos))
            // if not, tag id as non-expandable
            nonExpandable.add(id);
    }

    /**
     * @param subset
     * @param superset
     * @return true if superset contains at least one of subset's elements; false otherwise
     */
    private boolean containsAtLeastOne(final Collection<String> subset, final Collection<String> superset) {
        for (String element : subset) {
            if (superset.contains(element)) {
                return true;
            }
        }
        return false;
    }

}
