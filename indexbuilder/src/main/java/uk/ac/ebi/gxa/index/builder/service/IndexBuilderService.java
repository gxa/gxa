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

package uk.ac.ebi.gxa.index.builder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.index.builder.*;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;

import java.io.IOException;
import java.util.*;

/**
 * An abstract IndexBuilderService, that provides convenience methods for getting and setting parameters required across
 * all (SOLR and other) index building implementations.   Implementing classes have
 * access to an {@link uk.ac.ebi.gxa.dao.AtlasDAO} that provides interaction with the Atlas database (following an Atlas 2
 * schema).
 * <p/>
 *
 * @author Miroslaw Dylag (original version)
 * @author Tony Burdett (atlas 2 revision)
 */
public abstract class IndexBuilderService {
    final private Logger log = LoggerFactory.getLogger(this.getClass());
    private AtlasDAO atlasDAO;

    private Map<String, Collection<String>> ontologyMap = new HashMap<String, Collection<String>>();

    final public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    final public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    final protected Logger getLog() {
        return log;
    }

    public interface ProgressUpdater {
        void update(String progress);
    }

    /**
     * Build the index for this particular IndexBuilderService implementation. Once the index has been built, this
     * method will automatically commit any changes and release any resources held by the SOLR server.
     *
     * @param command         command
     * @param progressUpdater listener for passing progress updates
     * @throws uk.ac.ebi.gxa.index.builder.IndexBuilderException
     *          if the is a problem whilst generating the index
     */
    public void build(final IndexBuilderCommand command, final ProgressUpdater progressUpdater) throws IndexBuilderException {
        command.visit(new IndexBuilderCommandVisitor() {
            public void process(IndexAllCommand cmd) throws IndexBuilderException {
                processCommand(cmd, progressUpdater);
                finalizeCommand(cmd, progressUpdater);
            }

            public void process(UpdateIndexForExperimentCommand cmd) throws IndexBuilderException {
                processCommand(cmd, progressUpdater);
                finalizeCommand(cmd, progressUpdater);
            }
        });
    }


    protected Map<String, Collection<String>> getOntologyMap() {
        if (ontologyMap.isEmpty()) {
            loadEfoMapping();
        }
        return ontologyMap;
    }

    private void loadEfoMapping() {
        getLog().info("Fetching ontology mappings...");

        // we don't support enything else yet
        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            String mapKey = mapping.getExperimentId() + "_" +
                    mapping.getProperty() + "_" +
                    mapping.getPropertyValue();

            if (ontologyMap.containsKey(mapKey)) {
                // fetch the existing array and add this term
                // fixme: should actually add ontology term accession
                ontologyMap.get(mapKey).add(mapping.getOntologyTerm());
            } else {
                // add a new array
                Collection<String> values = new HashSet<String>();
                // fixme: should actually add ontology term accession
                values.add(mapping.getOntologyTerm());
                ontologyMap.put(mapKey, values);
            }
        }

        getLog().info("Ontology mappings loaded");
    }

    public abstract void processCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException;

    public abstract void processCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand, ProgressUpdater progressUpdater) throws IndexBuilderException;

    public abstract void finalizeCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException;

    public abstract void finalizeCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand, ProgressUpdater progressUpdater) throws IndexBuilderException;

    /**
     * Returns index name, which this service builds
     *
     * @return text string
     */
    public abstract String getName();

}
