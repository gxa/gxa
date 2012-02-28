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

package uk.ac.ebi.gxa.efo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * External view for node class
 */
public class EfoTerm implements Serializable {
    private final String id;
    private final String term;
    private final List<String> alternativeTerms;
    private final boolean branchRoot;
    private final boolean root;
    private final int depth;
    private boolean expandable;

    public EfoTerm(String id, String term, Collection<String> alternativeTerms, boolean expandable, boolean branchRoot, boolean root, int depth) {
        this.id = id;
        this.term = term;
        this.expandable = expandable;
        this.branchRoot = branchRoot;
        this.alternativeTerms = new ArrayList<String>();
        this.alternativeTerms.addAll(alternativeTerms);
        this.depth = depth;
        this.root = root;
    }

    /**
     * Constructor to create term from internal node
     *
     * @param node  original node
     * @param depth required depth
     * @param root  true if this node is root
     */
    public EfoTerm(EfoNode node, int depth, boolean root) {
        this(node.id, node.term, node.alternativeTerms, node.hasChildren(), node.branchRoot, root, depth);
    }

    public EfoTerm(EfoNode node, boolean root) {
        this(node, 0, root);
    }

    /**
     * Constructor to create a term from another one and custom depth
     *
     * @param other original node to clone
     * @param depth depth to set (we can have depth relative to something, not from real root all the time)
     */
    public EfoTerm(EfoTerm other, int depth) {
        this(other.id, other.term, other.alternativeTerms, other.expandable, other.branchRoot, other.root, depth);
    }

    /**
     * Return id of the term
     *
     * @return id of the term
     */
    public String getId() {
        return id;
    }

    /**
     * Returns term description string of the term
     *
     * @return term description string of the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * Returns if node is expandable (contains children)
     *
     * @return if node is expandable (contains children)
     */
    public boolean isExpandable() {
        return expandable;
    }

    /**
     * Method to override EFO default expandable flag (has efo children ==> Exapandable)
     * with false in the case when no children have experiment counts in bit index.
     * This functionality is used to prevent shown '+' sign against efos with no scoring children
     * in the efv/efo condition drop-down on Atlas main search page.
     */
    public void setNonExpandable() {
        this.expandable = false;
    }

    /**
     * Returns if node is branch root node
     *
     * @return if node is branch root node
     */
    public boolean isBranchRoot() {
        return branchRoot;
    }

    /**
     * Returns if node is root node
     *
     * @return if node is root node
     */
    public boolean isRoot() {
        return root;
    }

    /**
     * Returns node depth
     *
     * @return node depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns list of laternative terms (if any)
     *
     * @return list of strings, may be empty
     */
    public List<String> getAlternativeTerms() {
        return alternativeTerms;
    }

    /**
     * Equality check method
     *
     * @param o other term
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EfoTerm term = (EfoTerm) o;

        if (id != null ? !id.equals(term.id) : term.id != null) return false;

        return true;
    }

    /**
     * Returns hash code
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Returns nice string representation
     *
     * @return printable string
     */
    @Override
    public String toString() {
        return id + "(" + term + ")";
    }
}
