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

import net.sourceforge.fluxion.utils.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerException;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;
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
    private Map<String, EfoNode> efomap;

    Loader() {
    }

    private static class ClassAnnoVisitor extends OWLObjectVisitorAdapter {
        private String term;
        private List<String> alternativeTerms = new ArrayList<String>();
        private boolean branchRoot;
        private boolean organizational;

        public String getTerm() {
            return term;
        }

        public boolean isBranchRoot() {
            return branchRoot;
        }

        public boolean isOrganizational() {
            return organizational;
        }

        @Override
        public void visit(OWLAnnotation node) {
            if (!(node.getValue() instanceof OWLLiteral))
                return;

            final String literal = ((OWLLiteral) node.getValue()).getLiteral();
            final String iri = node.getProperty().getIRI().toString();

            if (node.getProperty().isLabel())
                term = literal;
            else if (iri.contains("branch_class"))
                branchRoot = Boolean.valueOf(literal);
            else if (iri.contains("organizational_class"))
                organizational = Boolean.valueOf(literal);
            else if (iri.contains("ArrayExpress_label"))
                term = literal;
            else if (iri.contains("alternative_term"))
                alternativeTerms.add(preprocessAlternativeTermString(literal));
        }
    }

    public static String preprocessAlternativeTermString(String str) {
        if (null == str) {
            return "";
        }
        // removing service
        return str.replaceAll("(\\[accessedResource:[^\\]]+\\])|(\\[accessDate:[^\\]]+\\])", "").trim();
    }

    /**
     * Loads ontology into map id -> internal node implementation
     *
     * @param efo EFO to load into
     * @param uri URI to load ontology from
     */
    void load(Efo efo, URI uri) {
        ReasonerSessionManager sessionManager = ReasonerSessionManager.createManager();
        sessionManager.setRecycleAfter(0);

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            try {
                if (uri.getScheme().equals("resource")) {
                    try {
                        uri = getClass().getClassLoader().getResource(uri.getSchemeSpecificPart()).toURI();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException("Can't get resource URI for " + uri);
                    }
                }
                log.info("Loading ontology from " + uri.toString());
                ontology = manager.loadOntology(IRI.create(uri));

                efo.setVersion("unknown");

                StringBuilder versionInfo = new StringBuilder();
                for (OWLAnnotation annotation : ontology.getAnnotations()) {
                    if (annotation.getValue() instanceof OWLLiteral
                            &&
                            "versionInfo".equals(annotation.getProperty().getIRI().getFragment())) {
                        String value = ((OWLLiteral) annotation.getValue()).getLiteral();
                        Matcher m = Pattern.compile(".*?(\\d+(\\.\\d+)+).*").matcher(value);
                        if (m.matches()) {
                            efo.setVersion(m.group(1));
                        }
                        if (versionInfo.length() > 0) {
                            versionInfo.append(" ");
                        }
                        versionInfo.append(value);
                    }
                }

                efo.setVersionInfo(versionInfo.toString());

                log.info("EFO version " + efo.getVersion() + " (" + efo.getVersionInfo() + ")");

            } catch (OWLOntologyCreationException e) {
                throw new RuntimeException("Can't load EF Ontology", e);
            }

            // acquire a reasoner session and use fluxion utils to build the partonomy
            ReasonerSession session = sessionManager.acquireReasonerSession(ontology, ReasonerType.HERMIT);
            try {
                OWLReasoner reasoner = session.getReasoner();
                try {
                    // first, load each class
                    this.efomap = efo.getEfomap();
                    for (OWLClass cls : ontology.getClassesInSignature(true)) {
                        loadClass(reasoner, cls);
                    }

                    log.info("Building partonomy");
                    buildPartOfMap(session);
                } catch (OWLReasonerException e) {
                    throw new RuntimeException(e);
                } finally {
                    reasoner.dispose();
                }
            } finally {
                session.releaseSession();
            }

            log.info("Loading ontology done");
        } finally {
            sessionManager.destroy();
        }
    }

    private OWLObjectProperty getProperty(String propertyName) {
        OWLObjectProperty result = null;
        for (OWLObjectProperty prpt : ontology.getObjectPropertiesInSignature(true)) {
            if (prpt.toString().contains(propertyName)) {
                result = prpt;
                break;
            }
        }
        return result;
    }

    // TODO this is very slow, committing it to keep it around and then will toss it. till better days.
    private void buildPartOfMap(ReasonerSession session) {
        OWLObjectProperty partOfProperty = getProperty("part_of");
        if (partOfProperty != null) {
            ArrayList<OWLClass> clss = new ArrayList<OWLClass>(ontology.getClassesInSignature(true));
            for (int i = 0; i < clss.size(); i++) {
                OWLClass cls = clss.get(i);
                System.out.println("Part " + i + " of " + clss.size());
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
        return cls.getIRI().toString().replaceAll("^.*?([^#/=?]+)$", "$1");
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
                if (term == null)
                    term = "undefined";
                en = new EfoNode(id, term, cannov.isBranchRoot(), cannov.alternativeTerms);
                NodeSet<OWLClass> children = reasoner.getSubClasses(cls, true);
                for (Node<OWLClass> setOfClasses : children) {
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
                } else {
                    efomap.put(id, en);
                }
            }
            return Collections.singletonList(en);
        }
        return new ArrayList<EfoNode>();
    }
}
