/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.service.mirbase.MiRNAEntity;
import uk.ac.ebi.gxa.index.builder.service.mirbase.MirbaseFastaParser;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Iterables.partition;
import static java.util.Collections.shuffle;

public class NewGeneAtlasIndexBuilderService extends IndexBuilderService {
    private AtlasProperties atlasProperties;

    private BioEntityDAO bioEntityDAO;
    private BioEntityTypeDAO bioEntityTypeDAO;
    private ExecutorService executor;
    private MirbaseFastaParser mirbaseParser;


    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void processCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException {
        super.processCommand(indexAll, progressUpdater);

        String status = "Indexing all genes...";
        getLog().info(status);
        progressUpdater.update(status);

//        bioEntityTypeDAO.setUseForIndexEnsprotein(true);
        List<BioEntity> allIndexedBioEntities = bioEntityDAO.getAllGenesAndProteinsFast();
//        bioEntityTypeDAO.setUseForIndexEnsprotein(false);
        indexGenes(progressUpdater, allIndexedBioEntities);
    }

    private void indexGenes(final ProgressUpdater progressUpdater,
                            final List<BioEntity> bioEntities) throws IndexBuilderException {
        shuffle(bioEntities);

        final int total = bioEntities.size();
        String status = "Found " + total + " genes to index";
        getLog().info(status);
        progressUpdater.update(status);

        final ArrayListMultimap<Long, DesignElement> allDesignElementsForGene = bioEntityDAO.getAllDesignElementsForGene();
        status = "Found " + allDesignElementsForGene.asMap().size() + " genes with de";
        getLog().info(status);
        progressUpdater.update(status);

        final AtomicInteger processed = new AtomicInteger(0);
        final long timeStart = System.currentTimeMillis();

        final int chunksize = atlasProperties.getGeneAtlasIndexBuilderChunksize();
        final int commitfreq = atlasProperties.getGeneAtlasIndexBuilderCommitfreq();

        status = "Using " + chunksize + " chunk size, committing every " + commitfreq + " genes";
        getLog().info(status);
        progressUpdater.update(status);
        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(bioEntities.size());

        // index all genes in parallel
        for (final List<BioEntity> genelist : partition(bioEntities, chunksize)) {
            // for each gene, submit a new task to the executor
            tasks.add(new Callable<Boolean>() {
                public Boolean call() throws IOException, SolrServerException {
                    try {
                        StringBuilder sblog = new StringBuilder();
                        long start = System.currentTimeMillis();

                        bioEntityDAO.getPropertiesForGenes(genelist);

                        String status;

                        List<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>(genelist.size());
                        for (BioEntity gene : genelist) {
                            List<SolrInputDocument> solrInputDocs = createGeneSolrInputDocument(gene, allDesignElementsForGene);

                            solrDocs.addAll(solrInputDocs);

                            int processedNow = processed.incrementAndGet();
                            if (processedNow % commitfreq == 0 || processedNow == total) {
                                long timeNow = System.currentTimeMillis();
                                long elapsed = timeNow - timeStart;
                                double speed = (processedNow / (elapsed / (double) commitfreq)); // (item/s)
                                double estimated = (total - processedNow) / (speed * 60);

                                status =
                                        String.format("Processed %d/%d genes %d%%, %.1f genes/sec overall, estimated %.1f min remaining",
                                                processedNow, total, (processedNow * 100 / total), speed, estimated);

                                getLog().info(status);
                                progressUpdater.update(status);
                            }
                            gene.clearProperties();
                        }

                        log(sblog, start, "adding genes to Solr index...");
                        getSolrServer().add(solrDocs);
                        log(sblog, start, "... batch complete.");
                        getLog().debug("Gene chunk done:\n" + sblog);

                        return true;
                    } catch (RuntimeException e) {
                        getLog().error("Runtime exception occurred: " + e.getMessage(), e);
                        return false;
                    }
                }
            });
        }

        bioEntities.clear();

        final List<MiRNAEntity> miRNAEntities = mirbaseParser.parse();

        tasks.add(new Callable<Boolean>() {
            public Boolean call() throws IOException, SolrServerException {
                try {
                    StringBuilder sblog = new StringBuilder();
                    long start = System.currentTimeMillis();

                    List<SolrInputDocument> solrDocs = new ArrayList<SolrInputDocument>(miRNAEntities.size());
                    for (MiRNAEntity entity : miRNAEntities) {
                        solrDocs.add(createSolrInputDocumentForMiRNAEntity(entity, entity.getAccession(), "symbol"));
                        solrDocs.add(createSolrInputDocumentForMiRNAEntity(entity, entity.getName(), "mirbase_name"));
                        solrDocs.add(createSolrInputDocumentForMiRNAEntity(entity, entity.getSequence(), "mirbase_sequence"));
                    }

                    log(sblog, start, "adding mirBase to Solr index...");
                    getSolrServer().add(solrDocs);

                    return true;
                } catch (RuntimeException e) {
                    getLog().error("Runtime exception occurred: " + e.getMessage(), e);
                    return false;
                }
            }
        });

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

    private SolrInputDocument createSolrInputDocumentForMiRNAEntity(MiRNAEntity entity, String propertyValue, String propertyType) {
        SolrInputDocument solrInputDoc = new SolrInputDocument();

        solrInputDoc.addField("type", "miRNA");
        solrInputDoc.addField("identifier", entity.getIdentifier());
        solrInputDoc.addField("species", entity.getOrganism());

        solrInputDoc.addField("property", propertyValue);
        solrInputDoc.addField("property_type", propertyType);

        return solrInputDoc;
    }

    private void log(StringBuilder sblog, long start, String message) {
        sblog.append("[ ").append(timestamp(start)).append(" ] ").append(message).append("\n");
    }

    private static long timestamp(long timeTaskStart) {
        return System.currentTimeMillis() - timeTaskStart;
    }

    private List<SolrInputDocument> createGeneSolrInputDocument(final BioEntity bioEntity, ArrayListMultimap<Long, DesignElement> allDesignElementsForGene) throws IndexBuilderException, IOException {

        getLog().debug("Updating index with properties for " + bioEntity.getIdentifier());

        List<SolrInputDocument> results = new ArrayList<SolrInputDocument>();

        for (BEPropertyValue prop : bioEntity.getProperties()) {

            String pv = prop.getValue();
            if (pv == null)
                continue;

            String p = prop.getProperty().getName();

            getLog().trace("Updating index, gene property " + p + " = " + pv);

            results.add(newSolrInputDocument(bioEntity, pv, p));
        }

        for (DesignElement de : allDesignElementsForGene.get(bioEntity.getId())) {
            results.add(newSolrInputDocument(bioEntity, de.getName(), "designelement_name"));
            results.add(newSolrInputDocument(bioEntity, de.getAccession(), "designelement_accession"));
        }

        getLog().debug("Properties for " + bioEntity.getIdentifier() + " updated");

        return results;
    }

    private SolrInputDocument newSolrInputDocument(BioEntity bioEntity, String propertyValue, String propertyType) {
        // create a new solr document for this gene
        SolrInputDocument solrInputDoc = new SolrInputDocument();

        solrInputDoc.addField("type", bioEntity.getType().getName());
        solrInputDoc.addField("identifier", bioEntity.getIdentifier());
        solrInputDoc.addField("species", bioEntity.getOrganism().getName());

        solrInputDoc.addField("property", propertyValue);
        solrInputDoc.addField("property_type", propertyType);

        return solrInputDoc;
    }

    public String getName() {
        return "newgenes";
    }

    public void setBioEntityDAO(BioEntityDAO bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }

    public void setBioEntityTypeDAO(BioEntityTypeDAO bioEntityTypeDAO) {
        this.bioEntityTypeDAO = bioEntityTypeDAO;
    }

    public void setMirbaseParser(MirbaseFastaParser mirbaseParser) {
        this.mirbaseParser = mirbaseParser;
    }

}
