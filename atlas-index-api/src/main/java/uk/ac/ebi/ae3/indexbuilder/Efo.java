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
 * @author pashky
 */
public class Efo {

    private static Efo instance = new Efo();

    public static Efo getEfo() {
        return instance;
    }

    private Map<String,EfoNode> efomap = new HashMap<String,EfoNode>();
    private SortedSet<EfoNode> roots = new TreeSet<EfoNode>(EfoNode.termAlphaComp);

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

    public static class Term {
        private String id;
        private String term;
        private boolean expandable;
        private boolean branchRoot;
        private int depth;

        private Term(EfoNode node) {
            this(node, 0);
        }

        public Term(Term other, int depth) {
            this.id = other.getId();
            this.term = other.getTerm();
            this.expandable = other.isExpandable();
            this.branchRoot = other.isBranchRoot();
            this.depth = depth;
        }

        private Term(EfoNode node, int depth) {
            this.id = node.id;
            this.term = node.term;
            this.expandable = !node.children.isEmpty();
            this.branchRoot = node.branchRoot;
            this.depth = depth;
        }

        public String getId() {
            return id;
        }

        public String getTerm() {
            return term;
        }

        public boolean isExpandable() {
            return expandable;
        }

        public boolean isBranchRoot() {
            return branchRoot;
        }

        public int getDepth() {
            return depth;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Term term = (Term) o;

            if (id != null ? !id.equals(term.id) : term.id != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return id + "(" + term + ")";
        }
    }

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

    private Efo() {
        Loader loader = new Loader();
        loader.load(this.efomap);
        for(EfoNode n : efomap.values()) {
            if(n.parents.isEmpty())
                roots.add(n);
        }
    }

    public String getTermNameById(String id) {
        EfoNode node = efomap.get(id);
        return node == null ? null : node.term;
    }

    public boolean hasTerm(String id) {
        EfoNode node = efomap.get(id);
        return node != null;
    }

    public Term getTermById(String id) {
        EfoNode node = efomap.get(id);
        return node == null ? null : new Term(node);
    }

    private void collectChildren(Collection<String> result, EfoNode node) {
        for(EfoNode n : node.children) {
            result.add(n.id);
            collectChildren(result, n);
        }
    }

    public Collection<String> getTermAndAllChildrenIds(String id) {
        EfoNode node = efomap.get(id);
        List<String> ids = new ArrayList<String>(node == null ? 0 : node.children.size());
        if(node != null) {
            collectChildren(ids, node);
            ids.add(node.id);
        }
        return ids;
    }

    public Collection<Term> getTermChildren(String id) {
        EfoNode node = efomap.get(id);
        if(node == null)
            return null;

        List<Term> result = new ArrayList<Term>(node.children.size());
        for(EfoNode n : node.children)
            result.add(new Term(n));

        return result;
    }

    public Collection<Term> getAllTerms() {
        List<Term> result = new ArrayList<Term>(efomap.size());
        for(EfoNode n : efomap.values())
            result.add(new Term(n));
        return result;
    }

    public Set<String> getAllTermIds() {
        return new HashSet<String>(efomap.keySet());
    }

    public Set<String> searchTermPrefix(String prefix) {
        String lprefix = prefix.toLowerCase();
        Set<String> result = new HashSet<String>();
        for(EfoNode n : efomap.values())
            if(n.term.toLowerCase().startsWith(lprefix) || n.id.toLowerCase().startsWith(lprefix)) {
                result.add(n.id);
            }
        return result;
    }

    public Collection<Term> searchTerm(String text) {
        final String ltext = text.trim().toLowerCase();
        String regex = ".*\\b\\Q" + ltext.replace("\\E", "").replaceAll("\\s+", "\\\\E\\\\b\\\\s+\\\\b\\\\Q") + "\\E\\b.*";
        List<Term> result = new ArrayList<Term>(efomap.size());
        for(EfoNode n : efomap.values()) {
            if(n.id.toLowerCase().equals(ltext) || n.term.toLowerCase().equals(ltext))
                result.add(0, new Term(n)); // exact matches go first
            else if(n.term.toLowerCase().matches(regex))
                result.add(new Term(n));
        }
        return result;
    }

    public List<List<Term>> getTermParentPaths(Term term, boolean stopOnBranchRoot) {
        return getTermParentPaths(term.getId(), stopOnBranchRoot);
    }

    private void collectPaths(EfoNode node, Collection<List<Term>> result, List<Term> current, boolean stopOnBranchRoot)
    {
        for(EfoNode p : node.parents) {
            List<Term> next = new ArrayList<Term>(current);
            next.add(new Term(p));
            if(stopOnBranchRoot && p.branchRoot)
                result.add(next);
            else
                collectPaths(p, result, next, stopOnBranchRoot);
        }
        if(node.parents.isEmpty())
            result.add(current);
    }

    public List<List<Term>> getTermParentPaths(String id, boolean stopOnBranchRoot) {
        EfoNode node = efomap.get(id);
        if(node == null)
            return null;

        List<List<Term>> result = new ArrayList<List<Term>>();
        collectPaths(node, result, new ArrayList<Term>(), stopOnBranchRoot);
        return result;
    }

    public Set<String> getTermFirstParents(String id, boolean stopOnBranchRoot) {
        EfoNode node = efomap.get(id);
        Set<String> parents = new HashSet<String>();
        while(!node.parents.isEmpty()) {
            node = node.parents.first();
            parents.add(node.id);
            if(node.branchRoot && stopOnBranchRoot)
                break;
        }
        return parents;
    }

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
    
    private void collectSubTree(EfoNode currentNode, List<Term> result, Set<String> allNodes, int depth, boolean printing) {
        if(printing && !allNodes.contains(currentNode.id))
            return;

        if(!printing && allNodes.contains(currentNode.id))
            printing = true;

        if(printing) {
            result.add(new Term(currentNode, depth));
            for (EfoNode child : currentNode.children)
                collectSubTree(child, result, allNodes, depth + 1, true);
        } else {
            for (EfoNode child : currentNode.children)
                collectSubTree(child, result, allNodes, 0, false);
        }
    }

    public List<Term> getSubTree(Set<String> ids) {
        List<Term> result = new ArrayList<Term>();

        for(EfoNode root : roots) {
            collectSubTree(root, result, ids, 0, false);
        }
        return result;
    }

    private void collectTreeDownTo(Iterable<EfoNode> nodes, Stack<EfoNode> path, List<Term> result, int depth)
    {
        EfoNode next = path.pop();
        for(EfoNode n : nodes) {
            result.add(new Term(n, depth));
            if(n.equals(next) && !path.empty())
                collectTreeDownTo(n.children, path, result, depth + 1);
        }
    }

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

    public Set<String> getRootIds() {
        Set<String> result = new HashSet<String>();
        for(EfoNode n : roots) {
            result.add(n.id);
        }
        return result;
    }

    public List<Term> getRoots() {
        List<Term> result = new ArrayList<Term>(roots.size());
        for(EfoNode n : roots)
            result.add(new Term(n));

        return result;
    }


    public Collection<String> getBranchRootIds() {
        List<String> result = new ArrayList<String>();
        for(EfoNode n : efomap.values())
            if(n.branchRoot)
                result.add(n.id);
        return result;
    }

}
