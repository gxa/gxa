package uk.ac.ebi.gxa.efo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.apibinding.OWLManager;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;

import net.sourceforge.fluxion.utils.ReasonerSession;
import net.sourceforge.fluxion.utils.OWLUtils;
import net.sourceforge.fluxion.utils.OWLTransformationException;

/**
     * Ontology loader class reading OWL files
 */
class Loader {
    final private Logger log = LoggerFactory.getLogger(getClass());
    private OWLOntology ontology;
    private OWLReasoner reasoner;
    private Map<String,EfoNode> efomap;

    Loader()
    {

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
     * @param efomap EFO map to load into
     * @param uri URI to load ontology from
     */
    void load(Map<String,EfoNode> efomap, URI uri) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            if(uri.getScheme().equals("resource")) {
                try {
                    uri = getClass().getClassLoader().getResource(uri.getSchemeSpecificPart()).toURI();
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Can't get resource URI for " + uri);
                }
            }
            log.info("Loading ontology from " + uri.toString());
            ontology = manager.loadOntologyFromPhysicalURI(uri);
        } catch(OWLOntologyCreationException e) {
            throw new RuntimeException("Can't load EF Ontology", e);
        }

        ReasonerSession session = OWLUtils.getReasonerSession(ontology);
        try {
            this.efomap = efomap;

            reasoner = session.getReasoner();
            for(OWLClass cls : ontology.getReferencedClasses()) {
                loadClass(cls);
            }
        } catch(OWLReasonerException e) {
            throw new RuntimeException(e);
        }  finally {
            session.releaseSession();
        }

        log.info("Building part-of map");
        buildPartOfMap();

        log.info("Loading ontology done");
      }

    private OWLObjectProperty getProperty(String propertyName)
    {
        OWLObjectProperty result = null;
        for (OWLObjectProperty prpt : ontology.getReferencedObjectProperties()) {
            if (prpt.toString().equals(propertyName)) {
                result = prpt;
                break;
            }
        }
        return result;
    }

    private void buildPartOfMap()
    {
        OWLObjectProperty partOfProperty = getProperty("part_of");
        if (partOfProperty != null) {
            for (OWLClass cls : ontology.getReferencedClasses()) {
                String partId = getId(cls);
                try {
                    Set<OWLRestriction> owlRestrictions = OWLUtils.keep(ontology, cls, partOfProperty);
                    for (OWLRestriction restriction : owlRestrictions) {
                        for (OWLClass parent : OWLUtils.getReferencedClasses(restriction)) {
                            String parentId = getId(parent);
                            if (parentId.equals(partId))
                                continue;

                            EfoNode parentNode = efomap.get(parentId);
                            EfoNode node = efomap.get(partId);
                            if(parentNode != null && node != null) {
                                parentNode.children.add(node);
                                node.parents.add(parentNode);
                            }
                        }
                    }
                } catch (OWLTransformationException e1) {
                    throw new RuntimeException(e1);
                }
            }
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
