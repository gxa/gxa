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
import java.util.*;

import static com.google.common.base.Joiner.on;

/**
 * Internal node representation structure
 */
class EfoNode implements Serializable {
    final String id;
    final String term;
    final List<String> alternativeTerms;
    final boolean branchRoot;

    private static class TermComparator implements Comparator<EfoNode>, Serializable {
        public int compare(EfoNode o1, EfoNode o2) {
            return o1.term.compareTo(o2.term);
        }
    }

    final static Comparator<EfoNode> termAlphaComp = new TermComparator();

    final SortedSet<EfoNode> children = new TreeSet<EfoNode>(termAlphaComp);
    final SortedSet<EfoNode> parents = new TreeSet<EfoNode>(termAlphaComp);

    public EfoNode(String id, String term, boolean branchRoot, List<String> alternativeTerms) {
        this.id = id;
        this.term = term;
        this.branchRoot = branchRoot;

        List<String> list = new ArrayList<String>();
        list.addAll(alternativeTerms);
        this.alternativeTerms = Collections.unmodifiableList(list);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EfoNode)) return false;

        EfoNode efoNode = (EfoNode) o;

        if (id != null ? !id.equals(efoNode.id) : efoNode.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return id + "(" + term + " " + on(",").join(alternativeTerms) + ")" + (hasChildren() ? "+" : "");
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }
}
