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

import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;

import java.util.*;
import java.io.Serializable;

/**
 * EFO tree handling helper class
 * @author pashky
 */
public class EfoTree<PayLoad extends Comparable<PayLoad>> {
    private Efo efo;
    private Map<String,PayLoad> efos = new HashMap<String,PayLoad>();
    private Set<String> marked = new HashSet<String>();
    private Set<String> explicitEfos = new HashSet<String>();
    private Set<String> autoChildren = new HashSet<String>();

    /**
     * Constructs objects
     * @param efo reference to EFO for use
     */
    public EfoTree(Efo efo) {
        this.efo = efo;
    }

    /**
     * Add element by ID and all relevant nodes (currently, one level up and optionally all children recursively)
     * @param id ID string
     * @param plCreator payload creator factory
     * @param withChildren add children or not
     */
    public void add(String id, EfoEfvPayloadCreator<PayLoad> plCreator, boolean withChildren)
    {

        if(efos.containsKey(id) && explicitEfos.contains(id))
            return;

        Iterable<String> parents = efo.getTermFirstParents(id);
        if(parents == null) // it's not in EFO, don't add it
            return;

        explicitEfos.add(id);

        for(String pId : parents)
            if (!efos.containsKey(pId))
                efos.put(pId, plCreator.make());

        if (!efos.containsKey(id))
            efos.put(id, plCreator.make());

        if(withChildren)
            for(String c : efo.getTermAndAllChildrenIds(id)) {
                if (!c.equals(id) && !efos.containsKey(c))
                    efos.put(c, plCreator.make());
                autoChildren.add(c);
            }
    }

    /**
     * Returns number of elements in tree including all automatically added
     * @return number of elements
     */
    public int getNumEfos() {
        return efos.size();
    }

    /**
     * Return number of elements explicitly added
     * @return number of elements
     */
    public int getNumExplicitEfos() {
        return explicitEfos.size();
    }

    /**
     * Return set of all element IDs
     * @return set of string element IDs
     */
    public Set<String> getEfoIds() {
        return efos.keySet();
    }

    /**
     * Return set of explicitly added element IDs
     * @return set of string element IDs
     */
    public Set<String> getExplicitEfos() {
        return explicitEfos;
    }

    /**
     * View helper class representing one tree node
     * @param <PayLoad> payload type
     */
    public static class EfoItem<PayLoad extends Comparable<PayLoad>> implements Serializable {
        private EfoTerm term;
        private PayLoad payload;
        private boolean explicit;

        private EfoItem(EfoTerm term, PayLoad payload, boolean explicit) {
            this.term = term;
            this.payload = payload;
            this.explicit = explicit;
        }

        /**
         * Returns node depth relative to subtree root
         * @return depth value
         */
        public int getDepth() {
            return term.getDepth();
        }

        /**
         * Returns element id
         * @return string node ID
         */
        public String getId() {
            return term.getId();
        }

        /**
         * Returns element payload
         * @return payload value
         */
        public PayLoad getPayload() {
            return payload;
        }

        /**
         * Returns string term description
         * @return term description
         */
        public String getTerm() {
            return term.getTerm();
        }

        /**
         * Returns whether node is root one (in absolute tree)
         * @return true if yes
         */
        public boolean isRoot() {
            return term.isRoot();
        }

        /**
         * Returns whether node is a branch root
         * @return true if yes
         */
        public boolean isBranchRoot() {
            return term.isBranchRoot();
        }

        /**
         * Returns is node was explicitly added to tree
         * @return true if yes
         */
        public boolean isExplicit() {
            return explicit;
        }
    }

    /**
     * Returns flattened representation of the marked nodes subtrees as list ordered in print order
     * Each subtree starts from depth=0
     * @return list of EfoItems
     */
    public List<EfoItem<PayLoad>> getMarkedSubTreeList()
    {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        for (EfoTerm t : efo.getSubTree(marked)) {
            result.add(new EfoItem<PayLoad>(t, efos.get(t.getId()), explicitEfos.contains(t.getId())));
        }
        return result;
    }

    /**
     * Returns nodes list view as a list, ordered by payload value
     * @return list of EfoItems
     */
    public List<EfoItem<PayLoad>> getValueOrderedList()
    {
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
     * @return list of EfoItems
     */
    public List<EfoItem<PayLoad>> getExplicitList()
    {
        List<EfoItem<PayLoad>> result = new ArrayList<EfoItem<PayLoad>>();
        for (String id : explicitEfos) {
            EfoTerm t = efo.getTermById(id);
            result.add(new EfoItem<PayLoad>(t, efos.get(t.getId()), explicitEfos.contains(t.getId())));
        }
        return result;
    }

    /**
     * Nice string representation of the tree
     * @return string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(EfoItem<PayLoad> i : getExplicitList()) {
            if(sb.length() > 0)
                sb.append(", ");
            sb.append(i.getId()).append("(").append(i.getTerm()).append(")=").append(i.getPayload());
        }
        return sb.toString();
    }

    /**
     * Mark a node by ID and all relevant nodes too (according to rules specified in add() method)
     * @param id string ID of node to mark
     */
    public void mark(String id) {
        if(marked.contains(id))
            return;

        if(explicitEfos.contains(id)) {
            for(String p : efo.getTermFirstParents(id))
                 marked.add(p);
            marked.add(id);
        } else if(autoChildren.contains(id)) {
            marked.add(id);
        }
    }
}
