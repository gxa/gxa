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

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.Serializable;

/**
     * Internal node representation structure
 */
class EfoNode implements Serializable {
    String id;
    String term;
    boolean branchRoot;

    private static class TermComparator implements Comparator<EfoNode>, Serializable {
        public int compare(EfoNode o1, EfoNode o2) {
            return o1.term.compareTo(o2.term);
        }
    }

    static Comparator<EfoNode> termAlphaComp = new TermComparator();

    SortedSet<EfoNode> children = new TreeSet<EfoNode>(termAlphaComp);
    SortedSet<EfoNode> parents = new TreeSet<EfoNode>(termAlphaComp);

    EfoNode(String id, String term, boolean branchRoot) {
        this.id = id;
        this.term = term;
        this.branchRoot = branchRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EfoNode efoNode = (EfoNode) o;

        if (id != null ? !id.equals(efoNode.id) : efoNode.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (term != null ? term.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return id + "(" + term + ")" + (children.isEmpty() ? "" : "+");
    }
}
