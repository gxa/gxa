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

import com.google.common.collect.ArrayListMultimap;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.gxa.dao.BioEntityDAOInterface;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.BioEntity;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Iterables.partition;
import static java.util.Collections.shuffle;

/**
* An {@link IndexBuilderService} that generates index documents from the genes in the Atlas database, and enriches the
* data with expression values, links to EFO and other useful measurements.
* <p/>
* This is a heavily modified version of an original class first adapted to Atlas purposes by Pavel Kurnosov.
* <p/>
* Note that this implementation does NOT support updates - regardless of whether the update flag is set to true, this
* will rebuild the index every time.
*
* @author Tony Burdett
*/
public class GeneAtlasIndexBuilderService extends IndexBuilderService {
    private Map<String, Collection<String>> ontomap =
            new HashMap<String, Collection<String>>();
    private AtlasProperties atlasProperties;

    private BioEntityDAOInterface bioEntityDAOInterface;
    private ExecutorService executor;

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void processCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(indexAll, progressUpdater);

        getLog().info("Indexing all genes...");
        indexGenes(progressUpdater, bioEntityDAOInterface.getAllGenesFast());
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd, ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(cmd, progressUpdater);

        getLog().info("Indexing genes for experiment " + cmd.getAccession() + "...");
        indexGenes(progressUpdater, bioEntityDAOInterface.getGenesByExperimentAccession(cmd.getAccession()));
    }

    private void indexGenes(final ProgressUpdater progressUpdater,
                            final List<BioEntity> bioEntities) throws IndexBuilderException {
        shuffle(bioEntities);

        final int total = bioEntities.size();
        getLog().info("Found " + total + " genes to index");

        final ArrayListMultimap<Long,DesignElement> allDesignElementsForGene = bioEntityDAOInterface.getAllDesignElementsForGene();
        getLog().info("Found " + allDesignElementsForGene.asMap().size() + " genes with de");

        loadEfoMapping();

        final AtomicInteger processed = new AtomicInteger(0);
        final long timeStart = System.currentTimeMillis();

        final int chunksize = atlasProperties.getGeneAtlasIndexBuilderChunksize();
        final int commitfreq = atlasProperties.getGeneAtlasIndexBuilderCommitfreq();

        getLog().info("Using {} chunk size, committing every {} genes", chunksize, commitfreq);
        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(bioEntities.size());

        // index all genes in parallel
        for (final List<BioEntity> genelist : partition(bioEntities, chunksize)) {
            // for each gene, submit a new task to the executor
            tasks.add(new Callable<Boolean>() {
                public Boolean call() throws IOException, SolrServerException {
                    try {
                        StringBuilder sblog = new StringBuilder();
                        long start = System.currentTimeMillis();

                        bioEntityDAOInterface.getPropertiesForGenes(genelist);

                        List<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>(genelist.size());
                        for (BioEntity gene : genelist) {
                            SolrInputDocument solrInputDoc = createGeneSolrInputDocument(gene);

                            Set<String> designElements = new HashSet<String>();
                            for (DesignElement de : allDesignElementsForGene.get(gene.getId())) {
// for (DesignElement de : bioEntityDAOInterface.getDesignElementsByGeneID(gene.getId())) {
                                designElements.add(de.getName());
                                designElements.add(de.getAccession());
                            }
                            solrInputDoc.addField("property_designelement", designElements);
                            solrInputDoc.addField("properties", "designelement");

                            solrDocs.add(solrInputDoc);

                            int processedNow = processed.incrementAndGet();
                            if (processedNow % commitfreq == 0 || processedNow == total) {
                                long timeNow = System.currentTimeMillis();
                                long elapsed = timeNow - timeStart;
                                double speed = (processedNow / (elapsed / (double) commitfreq)); // (item/s)
                                double estimated = (total - processedNow) / (speed * 60);

                                getLog().info(
                                        String.format("Processed %d/%d genes %d%%, %.1f genes/sec overall, estimated %.1f min remaining",
                                                processedNow, total, (processedNow * 100 / total), speed, estimated));

                                progressUpdater.update(processedNow + "/" + total);
                            }
                            gene.clearProperties();
                        }

                        log(sblog, start, "adding genes to Solr index...");
                        getSolrServer().add(solrDocs);
                        log(sblog, start, "... batch complete.");
                        getLog().info("Gene chunk done:\n" + sblog);

                        return true;
                    } catch (RuntimeException e) {
                        getLog().error("Runtime exception occurred: " + e.getMessage(), e);
                        return false;
                    }
                }
            });
        }

        bioEntities.clear();
        allDesignElementsForGene.clear();

        try {
            List<Future<Boolean>> results = executor.invokeAll(tasks);
            Iterator<Future<Boolean>> iresults = results.iterator();
            while (iresults.hasNext()) {
                Future<Boolean> result = iresults.next();
                result.get();
                iresults.remove();
            }
        } catch (InterruptedException e) {
            getLog().error("Indexing interrupted!", e);
        } catch (ExecutionException e) {
            throw new IndexBuilderException("Error in indexing!", e.getCause());
        }
    }

    private void log(StringBuilder sblog, long start, String message) {
        sblog.append("[ ").append(timestamp(start)).append(" ] ").append(message).append("\n");
    }

    private static long timestamp(long timeTaskStart) {
        return System.currentTimeMillis() - timeTaskStart;
    }

    private SolrInputDocument createGeneSolrInputDocument(final BioEntity bioEntity) {
        // create a new solr document for this gene
        SolrInputDocument solrInputDoc = new SolrInputDocument();
        getLog().debug("Updating index with properties for " + bioEntity.getIdentifier());

        // add the gene id field
        solrInputDoc.addField("id", bioEntity.getId());
        solrInputDoc.addField("species", bioEntity.getSpecies());
        solrInputDoc.addField("name", bioEntity.getName());
        solrInputDoc.addField("identifier", bioEntity.getIdentifier());

        Set<String> propNames = new HashSet<String>();
        for (Property prop : bioEntity.getProperties()) {
            String pv = prop.getValue();
            String p = prop.getName();
            if (pv == null)
                continue;
            if (p.toLowerCase().contains("ortholog")) {
                solrInputDoc.addField("orthologs", pv);
            } else {
                getLog().trace("Updating index, gene property " + p + " = " + pv);
                solrInputDoc.addField("property_" + p, pv);
                propNames.add(p);
            }
        }
        if (!propNames.isEmpty())
            solrInputDoc.setField("properties", propNames);

        getLog().debug("Properties for " + bioEntity.getIdentifier() + " updated");

        return solrInputDoc;
    }


    private void loadEfoMapping() {
        getLog().info("Fetching ontology mappings...");

        // we don't support enything else yet
        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            String mapKey = mapping.getExperimentId() + "_" +
                    mapping.getProperty() + "_" +
                    mapping.getPropertyValue();

            if (ontomap.containsKey(mapKey)) {
                // fetch the existing array and add this term
                // fixme: should actually add ontology term accession
                ontomap.get(mapKey).add(mapping.getOntologyTerm());
            } else {
                // add a new array
                Collection<String> values = new HashSet<String>();
                // fixme: should actually add ontology term accession
                values.add(mapping.getOntologyTerm());
                ontomap.put(mapKey, values);
            }
        }

        getLog().info("Ontology mappings loaded");
    }

    @Override
    public void finalizeCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand, ProgressUpdater progressUpdater) throws IndexBuilderException {
        commit(); // do not optimize
    }

    public String getName() {
        return "genes";
    }

    public void setBioEntityDAO(BioEntityDAOInterface bioEntityDAOInterface) {
        this.bioEntityDAOInterface = bioEntityDAOInterface;
    }
}
