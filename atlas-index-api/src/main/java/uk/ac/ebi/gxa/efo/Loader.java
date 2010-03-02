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

import net.sourceforge.fluxion.utils.OWLTransformationException;
import net.sourceforge.fluxion.utils.OWLUtils;
import net.sourceforge.fluxion.utils.ReasonerSession;
import net.sourceforge.fluxion.utils.ReasonerSessionManager;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.inference.OWLReasoner;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owl.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ontology loader class reading OWL files
 */
class Loader {
    final private Logger log = LoggerFactory.getLogger(getClass());
    private OWLOntology ontology;
//    private OWLReasoner reasoner;
    private Map<String, EfoNode> efomap;

    Loader() {

    }

    private static class ClassAnnoVisitor implements OWLAnnotationVisitor {
        private String term;
        private boolean branchRoot;
        private boolean organizational;

        public void visit(OWLConstantAnnotation annotation) {
            if (annotation.isLabel()) {
                OWLConstant c = annotation.getAnnotationValue();
                if (term == null) {
                    term = c.getLiteral();
                }
            }
            else if (annotation.getAnnotationURI().toString().contains("branch_class")) {
                branchRoot = Boolean.valueOf(annotation.getAnnotationValue().getLiteral());
            }
            else if (annotation.getAnnotationURI().toString().contains("organizational_class")) {
                organizational = Boolean.valueOf(annotation.getAnnotationValue().getLiteral());
            }
            else if (annotation.getAnnotationURI().toString().contains("ArrayExpress_label")) {
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
     *
     * @param efo EFO to load into
     * @param uri URI to load ontology from
     */
    void load(Efo efo, URI uri) {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {
            if (uri.getScheme().equals("resource")) {
                try {
                    uri = getClass().getClassLoader().getResource(uri.getSchemeSpecificPart()).toURI();
                }
                catch (URISyntaxException e) {
                    throw new RuntimeException("Can't get resource URI for " + uri);
                }
            }
            log.info("Loading ontology from " + uri.toString());
            ontology = manager.loadOntologyFromPhysicalURI(uri);

            efo.version = "unknown";

            StringBuilder versionInfo = new StringBuilder();
            for (OWLAnnotationAxiom annotation : ontology.getAnnotations(ontology)) {
                OWLAnnotation a = annotation.getAnnotation();
                if (a.getAnnotationURI().toString().contains("versionInfo")) {
                    String value = a.getAnnotationValueAsConstant().getLiteral();
                    Matcher m = Pattern.compile(".*?(\\d+(\\.\\d+)+).*").matcher(value);
                    if (m.matches()) {
                        efo.version = m.group(1);
                    }
                    if (versionInfo.length() > 0) {
                        versionInfo.append(" ");
                    }
                    versionInfo.append(value);
                }
            }

            efo.versionInfo = versionInfo.toString();

            log.info("EFO version " + efo.version + " (" + efo.versionInfo + ")");

        }
        catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Can't load EF Ontology", e);
        }

        // acquire a reasoner session and use fluxion utils to build the partonomy
        ReasonerSession session = ReasonerSessionManager.createManager().acquireReasonerSession(ontology);
        try {
            // first, load each class
            this.efomap = efo.efomap;
            for (OWLClass cls : ontology.getReferencedClasses()) {
                loadClass(session.getReasoner(), cls);
            }

            // trhen build part-of map
            log.info("Building part-of map");
            buildPartOfMap(session);
        }
        catch (OWLReasonerException e) {
            throw new RuntimeException(e);
        }
        finally {
            session.releaseSession();
        }

        log.info("Loading ontology done");
    }

    private OWLObjectProperty getProperty(String propertyName) {
        OWLObjectProperty result = null;
        for (OWLObjectProperty prpt : ontology.getReferencedObjectProperties()) {
            if (prpt.toString().equals(propertyName)) {
                result = prpt;
                break;
            }
        }
        return result;
    }

    private void buildPartOfMap(ReasonerSession session) {
        OWLObjectProperty partOfProperty = getProperty("part_of");
        if (partOfProperty != null) {
            for (OWLClass cls : ontology.getReferencedClasses()) {
                String partId = getId(cls);
                try {
                    Set<OWLRestriction> owlRestrictions = OWLUtils.keep(session, ontology, cls, partOfProperty);
                    for (OWLRestriction restriction : owlRestrictions) {
                        for (OWLClass parent : OWLUtils.getReferencedClasses(session, restriction)) {
                            String parentId = getId(parent);
                            if (parentId.equals(partId)) {
                                continue;
                            }

                            EfoNode parentNode = efomap.get(parentId);
                            EfoNode node = efomap.get(partId);
                            if (parentNode != null && node != null) {
                                parentNode.children.add(node);
                                node.parents.add(parentNode);
                            }
                        }
                    }
                }
                catch (OWLTransformationException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }
    }

    private String getId(OWLClass cls) {
        return cls.getURI().toString().replaceAll("^.*?([^#/=?]+)$", "$1");
    }

    private Collection<EfoNode> loadClass(OWLReasoner reasoner, OWLClass cls) throws OWLReasonerException {
        if (reasoner.isSatisfiable(cls)) {
            String id = getId(cls);
            EfoNode en = efomap.get(id);
            if (en == null) {
                ClassAnnoVisitor cannov = new ClassAnnoVisitor();
                for (OWLAnnotation annotation : cls.getAnnotations(ontology)) {
                    annotation.accept(cannov);
                }
                String term = cannov.getTerm();
                boolean branchRoot = cannov.isBranchRoot();
                en = new EfoNode(id, term, branchRoot);
                Set<Set<OWLClass>> children = reasoner.getSubClasses(cls);
                for (Set<OWLClass> setOfClasses : children) {
                    for (OWLClass child : setOfClasses) {
                        if (!child.equals(cls)) {
                            Collection<EfoNode> cnc = loadClass(reasoner, child);
                            for (EfoNode cn : cnc) {
                                en.children.add(cn);
                                if (!cannov.isOrganizational()) {
                                    cn.parents.add(en);
                                }
                            }
                        }
                    }
                }
                if (cannov.isOrganizational()) {
                    return en.children;
                }
                else {
                    efomap.put(id, en);
                }
            }
            return Collections.singletonList(en);
        }
        return new ArrayList<EfoNode>();
    }

}
