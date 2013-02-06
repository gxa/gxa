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

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

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

        @Override
        public void visit(OWLAnnotation node) {
            if (!(node.getValue() instanceof OWLLiteral))
                return;

            final String literal = ((OWLLiteral) node.getValue()).getLiteral();
            final String iri = node.getProperty().getIRI().toString();

            if (node.getProperty().isLabel()) {
                term = literal;
            } else if (iri.contains("branch_class")) {
                branchRoot = Boolean.valueOf(literal);
            } else if (iri.contains("organizational_class")) {
                organizational = Boolean.valueOf(literal);
            } else if (iri.contains("ArrayExpress_label")) {
                term = literal;
            } else if (iri.contains("alternative_term") || iri.contains("definition_citation")) {
                alternativeTerms.add(preprocessAlternativeTermString(literal));
            }
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
        System.setProperty("entityExpansionLimit", "100000000");


        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            try {
                if (uri.getScheme().equals("resource")) {
                    uri = getClass().getClassLoader().getResource(uri.getSchemeSpecificPart()).toURI();
                }
                log.info("Loading ontology from " + uri.toString());

                ontology = manager.loadOntologyFromOntologyDocument((IRI.create(uri)));

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
                throw createUnexpected("Can't load EFO Ontology", e);
            }

            //get map and call each class to add it to the map
            this.efomap = efo.getEfomap();
            for (OWLClass cls : ontology.getClassesInSignature(true)){
                addClassToMap(cls);
            }

            System.out.println("Ontology loaded");
            log.info("Loading ontology done");
        } catch (URISyntaxException e) {
            throw LogUtil.createUnexpected("Can't get resource URI for " + uri);
        }
    }

    private String getId(OWLClass cls) {
        return cls.getIRI().toString().replaceAll("^.*?([^#/=?]+)$", "$1");
    }

    private Collection<EfoNode> addClassToMap(OWLClass cls){

        //get class ID
        String id = getId(cls);
        //try to get node corresponding to this class from map
        EfoNode en = efomap.get(id);
        //if this node is not already in the map then add it and subclass information
        if (en == null) {
            ClassAnnoVisitor cannov = new ClassAnnoVisitor();
            for (OWLAnnotation annotation : cls.getAnnotations(ontology)) {
                annotation.accept(cannov);
            }
            String term = cannov.getTerm();
            if (term == null)
                term = "undefined";
            en = new EfoNode(id, term, cannov.isBranchRoot(), cannov.alternativeTerms);

            //get all asserted subclasses
            Set<OWLClassExpression> childClassExpressions = cls.getSubClasses(ontology);
            //for each of this child classes
            for (OWLClassExpression childClass : childClassExpressions) {
                    //check that child class is not same as one we are considering
                    if (!childClass.equals(cls)) {

                        //check to make sure we can cast the child class as an OWLClass
                        if(childClass instanceof OWLClass){
                            //cast it as such
                            OWLClass childAsOWLClass = childClass.asOWLClass();

                            //iteratively call to get rest of subclasses of this child
                            Collection<EfoNode> cnc = addClassToMap(childAsOWLClass);
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
        return Collections.singletonList(en) ;

    }
}
