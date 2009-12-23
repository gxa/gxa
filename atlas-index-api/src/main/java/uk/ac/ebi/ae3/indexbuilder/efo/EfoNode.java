package uk.ac.ebi.ae3.indexbuilder.efo;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
     * Internal node representation structure
 */
class EfoNode {
    String id;
    String term;
    boolean branchRoot;

    static Comparator<EfoNode> termAlphaComp = new Comparator<EfoNode>() {
        public int compare(EfoNode o1, EfoNode o2) {
            return o1.term.compareTo(o2.term);
        }
    };

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
