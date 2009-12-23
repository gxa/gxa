package uk.ac.ebi.ae3.indexbuilder.efo;

/**
     * External view for node class
 */
public class EfoTerm {
    private String id;
    private String term;
    private boolean expandable;
    private boolean branchRoot;
    private boolean root;
    private int depth;

    EfoTerm(EfoNode node, boolean root) {
        this(node, 0, root);
    }

    /**
     * Constructor to create a term from another one and custom depth
     * @param other original node to clone
     * @param depth depth to set (we can have depth relative to something, not from real root all the time)
     */
    public EfoTerm(EfoTerm other, int depth) {
        this.id = other.getId();
        this.term = other.getTerm();
        this.expandable = other.isExpandable();
        this.branchRoot = other.isBranchRoot();
        this.depth = depth;
        this.root = other.isRoot();
    }

    /**
     * Constructor to create term from internal node
     * @param node original node
     * @param depth required depth
     * @param root true if this node is root
     */
    EfoTerm(EfoNode node, int depth, boolean root) {
        this.id = node.id;
        this.term = node.term;
        this.expandable = !node.children.isEmpty();
        this.branchRoot = node.branchRoot;
        this.depth = depth;
        this.root = root;
    }

    /**
     * Return id of the term
     * @return id of the term
     */
    public String getId() {
        return id;
    }

    /**
     * Returns term description string of the term
     * @return term description string of the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * Returns if node is expandable (contains children)
     * @return if node is expandable (contains children)
     */
    public boolean isExpandable() {
        return expandable;
    }

    /**
     * Returns if node is branch root node
     * @return if node is branch root node
     */
    public boolean isBranchRoot() {
        return branchRoot;
    }

    /**
     * Returns if node is root node
     * @return if node is root node
     */
    public boolean isRoot() {
        return root;
    }

    /**
     * Returns node depth
     * @return node depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Equality check method
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
     * @return hash code
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Returns nice string representation
     * @return printable string
     */
    @Override
    public String toString() {
        return id + "(" + term + ")";
    }
}
