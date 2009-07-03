package uk.ac.ebi.ae3.indexbuilder;

import org.mindswap.pellet.owlapi.PelletReasonerFactory;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.inference.OWLReasonerFactory;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.io.StreamInputSource;
import org.semanticweb.owl.model.*;

import java.util.*;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class representing EFO heirarchy
 * @author pashky
 */
public class Efo {

    private static Efo instance = new Efo();

    /**
     * Returns current EFO instance (this class is singleton)
     * @return Efo instance
     */
    public static Efo getEfo() {
        return instance;
    }

    private Map<String,EfoNode> efomap = new HashMap<String,EfoNode>();
    private SortedSet<EfoNode> roots = new TreeSet<EfoNode>(EfoNode.termAlphaComp);

    /**
     * Internal node representation structure
     */
    private static class EfoNode {
        private String id;
        private String term;
        private boolean branchRoot;

        private static Comparator<EfoNode> termAlphaComp = new Comparator<EfoNode>() {
            public int compare(EfoNode o1, EfoNode o2) {
                return o1.term.compareTo(o2.term);
            }
        };

        private SortedSet<EfoNode> children = new TreeSet<EfoNode>(termAlphaComp);
        private SortedSet<EfoNode> parents = new TreeSet<EfoNode>(termAlphaComp);

        private EfoNode(String id, String term, boolean branchRoot) {
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

    /**
     * External view for node class
     */
    public static class Term {
        private String id;
        private String term;
        private boolean expandable;
        private boolean branchRoot;
        private boolean root;
        private int depth;

        private Term(EfoNode node, boolean root) {
            this(node, 0, root);
        }

        /**
         * Constructor to create a term from another one and custom depth
         * @param other original node to clone
         * @param depth depth to set (we can have depth relative to something, not from real root all the time)
         */
        public Term(Term other, int depth) {
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
        private Term(EfoNode node, int depth, boolean root) {
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

            Term term = (Term) o;

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

    /**
     * Helper factory method to make term classes
     * @param node internal node to make it from
     * @return external term object
     */
    private Term newTerm(EfoNode node) {
        return new Term(node, roots.contains(node));
    }

    /**
     * Helper factory method to make term classes
     * @param node internal node to make it from
     * @param depth required depth
     * @return external term object
     */
    private Term newTerm(EfoNode node, int depth) {
        return new Term(node, depth, roots.contains(node));
    }

    /**
     * Ontology loader class reading OWL files
     */
    private static class Loader {
        final private Logger log = LoggerFactory.getLogger(getClass());
        private OWLOntologyManager manager;
        private OWLOntology ontology;
        private OWLReasoner reasoner;
        private Map<String,EfoNode> efomap;

        private Loader()
        {
            log.info("Loading ontology");
            manager = OWLManager.createOWLOntologyManager();
            try {
                ontology = manager.loadOntology(new StreamInputSource(getClass().getClassLoader().getResourceAsStream("META-INF/efo.owl")));
            } catch(OWLOntologyCreationException e) {
                throw new RuntimeException("Can't load EF Ontology", e);
            }

        }

        private static class ClassAnnoVisitor implements OWLAnnotationVisitor {
            private String term;
            private boolean branchRoot;
            private boolean organizational;

            public void visit(OWLConstantAnnotation annotation) {
                if (annotation.isLabel()) {
                    OWLConstant c = annotation.getAnnotationValue();
                    if(term == null)
                        term = c.getLiteral();
                } else if(annotation.getAnnotationURI().toString().contains("branch_class")) {
                    branchRoot = Boolean.valueOf(annotation.getAnnotationValue().getLiteral());
                } else if(annotation.getAnnotationURI().toString().contains("organizational_class")) {
                    organizational = Boolean.valueOf(annotation.getAnnotationValue().getLiteral());
                } else if(annotation.getAnnotationURI().toString().contains("ArrayExpress_label")) {
                    term = annotation.getAnnotationValue().getLiteral();
                }
            }

            public void visit(OWLObjectAnnotation annotation) {
            }

            public String getTerm() {
                return term;
            }

            public boolean isBranchRoot() {
                return branchRoot;
            }

            public boolean isOrganizational() {
                return organizational;
            }
        }


        /**
         * Loads ontology into map id -> internal node implementation
         * @param efomap map to load into
         */
        private void load(Map<String,EfoNode> efomap) {
            try {
                this.efomap = efomap;

                OWLReasonerFactory reasonerFactory = new PelletReasonerFactory();
                reasoner = reasonerFactory.createReasoner(manager);
                reasoner.loadOntologies(Collections.singleton(ontology));
                reasoner.classify();
                for(OWLClass cls : ontology.getReferencedClasses()) {
                    loadClass(cls);
                }
                reasoner.clearOntologies();
                log.info("Loading ontology done");
            } catch(OWLReasonerException e) {
                throw new RuntimeException(e);
            }
        }

        private String getId(OWLClass cls) {
            return cls.getURI().getPath().replaceAll("^.*/", "");
        }

        private Collection<EfoNode> loadClass(OWLClass cls) throws OWLReasonerException {
            if(reasoner.isSatisfiable(cls)) {
                String id = getId(cls);
                EfoNode en = efomap.get(id);
                if(en == null) {
                    ClassAnnoVisitor cannov = new ClassAnnoVisitor();
                    for(OWLAnnotation annotation : cls.getAnnotations(ontology)) {
                        annotation.accept(cannov);
                    }
                    String term = cannov.getTerm();
                    boolean branchRoot = cannov.isBranchRoot();
                    en = new EfoNode(id, term, branchRoot);
                    Set<Set<OWLClass>> children = reasoner.getSubClasses(cls);
                    for (Set<OWLClass> setOfClasses : children) {
                        for (OWLClass child : setOfClasses) {
                            if (!child.equals(cls)) {
                                Collection<EfoNode> cnc = loadClass(child);
                                for(EfoNode cn : cnc) {
                                    en.children.add(cn);
                                    if(!cannov.isOrganizational())
                                        cn.parents.add(en);
                                }
                            }
                        }
                    }
                    if(cannov.isOrganizational())
                        return en.children;
                    else
                        efomap.put(id, en);
                }
                return Collections.singletonList(en);
            }
            return new ArrayList<EfoNode>();
        }

    }

    /**
     * Private constructor loading data from OWl
     */
    private Efo() {
        Loader loader = new Loader();
        loader.load(this.efomap);
        for(EfoNode n : efomap.values()) {
            if(n.parents.isEmpty())
                roots.add(n);
        }
    }

    /**
     * Fetch term string by id
     * @param id term id
     * @return term string
     */
    public String getTermNameById(String id) {
        EfoNode node = efomap.get(id);
        return node == null ? null : node.term;
    }

    /**
     * Check if term is here
     * @param id term id
     * @return true if yes
     */
    public boolean hasTerm(String id) {
        EfoNode node = efomap.get(id);
        return node != null;
    }

    /**
     * Fetch term by id
     * @param id term id
     * @return external term representation if found in ontology, null otherwise
     */
    public Term getTermById(String id) {
        EfoNode node = efomap.get(id);
        return node == null ? null : newTerm(node);
    }

    private void collectChildren(Collection<String> result, EfoNode node) {
        for(EfoNode n : node.children) {
            result.add(n.id);
            collectChildren(result, n);
        }
    }

    /**
     * Returns collection of IDs of node itself and all its children recursively
     * @param id term id
     * @return collection of IDs, empty if term is not found
     */
    public Collection<String> getTermAndAllChildrenIds(String id) {
        EfoNode node = efomap.get(id);
        List<String> ids = new ArrayList<String>(node == null ? 0 : node.children.size());
        if(node != null) {
            collectChildren(ids, node);
            ids.add(node.id);
        }
        return ids;
    }

    /**
     * Returns collection of term's direct children
     * @param id term id
     * @return collection of terms, null if term is not found
     */
    public Collection<Term> getTermChildren(String id) {
        EfoNode node = efomap.get(id);
        if(node == null)
            return null;

        List<Term> result = new ArrayList<Term>(node.children.size());
        for(EfoNode n : node.children)
            result.add(newTerm(n));

        return result;
    }

    /**
     * Returns collection of all terms (depth=0)
     * @return collection of all terms
     */
    public Collection<Term> getAllTerms() {
        List<Term> result = new ArrayList<Term>(efomap.size());
        for(EfoNode n : efomap.values())
            result.add(newTerm(n));
        return result;
    }

    /**
     * Returns collection of all term IDs
     * @return set of all term IDs
     */
    public Set<String> getAllTermIds() {
        return new HashSet<String>(efomap.keySet());
    }

    /**
     * Searches for prefix in ontology
     * @param prefix prefix to search
     * @return set of string IDs
     */
    public Set<String> searchTermPrefix(String prefix) {
        String lprefix = prefix.toLowerCase();
        Set<String> result = new HashSet<String>();
        for(EfoNode n : efomap.values())
            if(n.term.toLowerCase().startsWith(lprefix) || n.id.toLowerCase().startsWith(lprefix)) {
                result.add(n.id);
            }
        return result;
    }

    /**
     * Searches for text in ontology
     * @param text words to search
     * @return collection of terms
     */
    public Collection<Term> searchTerm(String text) {
        final String ltext = text.trim().toLowerCase();
        String regex = ".*\\b\\Q" + ltext.replace("\\E", "").replaceAll("\\s+", "\\\\E\\\\b\\\\s+\\\\b\\\\Q") + "\\E\\b.*";
        List<Term> result = new ArrayList<Term>(efomap.size());
        for(EfoNode n : efomap.values()) {
            if(n.id.toLowerCase().equals(ltext) || n.term.toLowerCase().equals(ltext))
                result.add(0, newTerm(n)); // exact matches go first
            else if(n.term.toLowerCase().matches(regex))
                result.add(newTerm(n));
        }
        return result;
    }

    private void collectPaths(EfoNode node, Collection<List<Term>> result, List<Term> current, boolean stopOnBranchRoot)
    {
        for(EfoNode p : node.parents) {
            List<Term> next = new ArrayList<Term>(current);
            next.add(newTerm(p));
            if(stopOnBranchRoot && p.branchRoot)
                result.add(next);
            else
                collectPaths(p, result, next, stopOnBranchRoot);
        }
        if(node.parents.isEmpty())
            result.add(current);
    }

    /**
     * Returns list of term parent paths (represented as list string from node ending at root)
     * @param id term id to search
     * @param stopOnBranchRoot if true, stops on branch root, not going to real root
     * @return list of lists of Term's
     */
    public List<List<Term>> getTermParentPaths(String id, boolean stopOnBranchRoot) {
        EfoNode node = efomap.get(id);
        if(node == null)
            return null;

        List<List<Term>> result = new ArrayList<List<Term>>();
        collectPaths(node, result, new ArrayList<Term>(), stopOnBranchRoot);
        return result;
    }

    /**
     * Returns list of term parent paths (represented as list string from node ending at root)
     * @param term term to search
     * @param stopOnBranchRoot if true, stops on branch root, not going to real root
     * @return list of lists of Term's
     */
    public List<List<Term>> getTermParentPaths(Term term, boolean stopOnBranchRoot) {
        return getTermParentPaths(term.getId(), stopOnBranchRoot);
    }

    /**
     * Returns set of term's direct parent IDs
     * @param id term id
     * @return set of string IDs
     */
    public Set<String> getTermFirstParents(String id) {
        EfoNode node = efomap.get(id);
        if(node == null)
            return null;
        Set<String> parents = new HashSet<String>();
        for(EfoNode p : node.parents)
            parents.add(p.id);
        return parents;
    }

    /**
     * Returns set of term's parent IDs
     * @param id term id
     * @param stopOnBranchRoot if true, stops on branch root, not going to real root
     * @return set of string IDs
     */
    public Set<String> getTermParents(String id, boolean stopOnBranchRoot) {
        EfoNode node = efomap.get(id);
        if(node == null)
            return null;
        Set<String> parents = new HashSet<String>();
        collectParents(node, parents, stopOnBranchRoot);
        return parents;
    }

    private void collectParents(EfoNode node, Set<String> parents, boolean stopOnBranchRoot)
    {
        for(EfoNode p : node.parents) {
            parents.add(p.id);
            if(!stopOnBranchRoot || !p.branchRoot)
                collectParents(p, parents, stopOnBranchRoot);
        }
    }
    
    private void collectSubTree(EfoNode currentNode, List<Term> result, Set<String> allNodes, Set<String> visited, int depth, boolean printing) {
        if(printing && !allNodes.contains(currentNode.id))
            return;

        if(!printing && allNodes.contains(currentNode.id) && !visited.contains(currentNode.id))
            printing = true;

        if(printing) {
            result.add(newTerm(currentNode, depth));
            visited.add(currentNode.id);
            for (EfoNode child : currentNode.children)
                collectSubTree(child, result, allNodes, visited, depth + 1, true);
        } else {
            for (EfoNode child : currentNode.children)
                collectSubTree(child, result, allNodes, visited, 0, false);
        }
    }

    /**
     * Creates flat subtree representation ordered in natural print order,
     * each self-contained sub-tree starts from depth=0
     * @param ids marked IDs
     * @return list of Term's
     */
    public List<Term> getSubTree(Set<String> ids) {
        List<Term> result = new ArrayList<Term>();

        Set<String> visited = new HashSet<String>();
        for(EfoNode root : roots) {
            collectSubTree(root, result, ids, visited, 0, false);
        }
        return result;
    }

    private void collectTreeDownTo(Iterable<EfoNode> nodes, Stack<EfoNode> path, List<Term> result, int depth)
    {
        EfoNode next = path.pop();
        for(EfoNode n : nodes) {
            result.add(newTerm(n, depth));
            if(n.equals(next) && !path.empty())
                collectTreeDownTo(n.children, path, result, depth + 1);
        }
    }

    /**
     * Creates flat subtree representation of tree "opened" down to specified node,
     * hence displaying all its parents first and then a tree level, containing specified node
     * @param id term id
     * @return list of Term's
     */
    public List<Term> getTreeDownTo(String id) {
        List<Term> result = new ArrayList<Term>();

        Stack<EfoNode> path = new Stack<EfoNode>();
        EfoNode node = efomap.get(id);
        while(true) {
            path.push(node);
            if(node.parents.isEmpty())
                break;
            node = node.parents.first();
        }

        collectTreeDownTo(roots, path, result, 0);
        return result;
    }

    /**
     * Returns set of root node IDs
     * @return set of root node IDs
     */
    public Set<String> getRootIds() {
        Set<String> result = new HashSet<String>();
        for(EfoNode n : roots) {
            result.add(n.id);
        }
        return result;
    }

    /**
     * Returns list of root terms
     * @return list of terms
     */
    public List<Term> getRoots() {
        List<Term> result = new ArrayList<Term>(roots.size());
        for(EfoNode n : roots)
            result.add(newTerm(n));

        return result;
    }


    /**
     * Returns set of branch root IDs
     * @return set of branch root IDs
     */
    public Set<String> getBranchRootIds() {
        Set<String> result = new HashSet<String>();
        for(EfoNode n : efomap.values())
            if(n.branchRoot)
                result.add(n.id);
        return result;
    }

}
