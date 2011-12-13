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

package ae3.service;

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import com.google.common.collect.Multimap;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.statistics.EfAttribute;
import uk.ac.ebi.gxa.statistics.ExperimentInfo;
import uk.ac.ebi.gxa.statistics.StatisticsQueryCondition;
import uk.ac.ebi.gxa.statistics.StatisticsType;
import uk.ac.ebi.gxa.utils.Pair;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Represents a download event for exporting atlas list results to files
 *
 * @author iemam
 */
public class Download implements Runnable {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AtlasStructuredQueryService atlasStructuredQueryService;
    private final AtlasStatisticsQueryService atlasStatisticsQueryService;
    private final GeneSolrDAO geneSolrDAO;

    private final int id;
    private final AtlasStructuredQuery query;

    private File outputFile;

    private final AtomicInteger totalResults = new AtomicInteger();
    private final AtomicInteger resultsRetrieved = new AtomicInteger();
    private String dataVersion;

    public Download(int id,
                    AtlasStructuredQueryService atlasStructuredQueryService,
                    AtlasStatisticsQueryService atlasStatisticsQueryService,
                    GeneSolrDAO geneSolrDAO,
                    AtlasStructuredQuery query, String dataVersion) throws IOException {
        this.query = query;
        this.id = id;
        this.atlasStructuredQueryService = atlasStructuredQueryService;
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
        this.geneSolrDAO = geneSolrDAO;

        this.outputFile = File.createTempFile("listdl", ".zip");
        this.outputFile.deleteOnExit();
        this.dataVersion = dataVersion;
    }

    public String getQuery() {
        return query.toString();
    }

    @RestOut(name = "progress")
    public double getProgress() {
        long total = totalResults.get();
        if (0 == total)
            return 0;

        long retrieved = resultsRetrieved.get();
        if (retrieved == total)
            return 100;

        return Math.floor(100.0 * retrieved / total);
    }

    public void run() {
        if (query == null || query.isNone())
            return;

        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(new FileOutputStream(getOutputFile()));
            zout.putNextEntry(new ZipEntry("listdl.tab"));
            writeData(query, zout);
            zout.closeEntry();
        } catch (IOException e) {
            log.error("Error executing download for query {}, error {}", query, e.getMessage());
        } finally {
            Closeables.closeQuietly(zout);
        }
    }

    /**
     * Implement equality on query; prevents identical queries (within session) from being downloaded multiple times.
     */
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Download))
            return false;
        Download d = (Download) o;
        return d.getQuery().equals(this.getQuery());
    }

    /**
     * {@see {@link #equals}}
     */
    public int hashCode() {
        return getQuery().hashCode();
    }

    private void writeHeader(OutputStream out) throws IOException {
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss");
        StringBuilder strBuf = new StringBuilder()
                .append("# Atlas data version: ").append(dataVersion).append("\n")
                .append("# Query: ").append(query).append("\n")
                .append("# Note that to download non-differential expression data the 'non-d.e. in' option needs to be selected on Atlas search page\n")
                .append("# Timestamp: ").append(formatter.format(today)).append("\n")
                .append("Gene name").append("\t")
                .append("Gene identifier").append("\t")
                .append("Organism").append("\t")
                .append("Experimental factor").append("\t")
                .append("Factor value").append("\t")
                .append("Experiment accession").append("\t")
                .append("Array Design accession").append("\t")
                .append("Expression").append("\t")
                .append("P-value").append("\n");

        out.write(strBuf.toString().getBytes("UTF-8"));
    }

    private void incrementResultsRetrieved() {
        resultsRetrieved.getAndIncrement();
    }

    public int getId() {
        return id;
    }

    public File getOutputFile() {
        return outputFile;
    }

    protected void finalize() throws Throwable {
        try {
            if (getOutputFile().exists() && !getOutputFile().delete()) {
                log.warn("Failed to delete " + getOutputFile());
            }
        } finally {
            super.finalize();
        }
    }

    private void writeData(@Nonnull AtlasStructuredQuery query, @Nonnull OutputStream zout) throws IOException {
        writeHeader(zout);

        long start = System.currentTimeMillis();
        // Get gene ids by Gene Conditions and Species - will be empty if user is querying by factor conditions only
        Set<Integer> geneIds = atlasStructuredQueryService.getGenesByGeneConditionsAndSpecies(query.getGeneConditions(), query.getSpecies());
        log.debug("Called getGenesByGeneConditionsAndSpecies() - size: {}", geneIds.size());

        // Populate statsQuery with factor conditions
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition();
        atlasStructuredQueryService.appendEfvsQuery(query, null, statsQuery);
        if (statsQuery.getStatisticsType() == null) {
            statsQuery.setStatisticsType(StatisticsType.UP_DOWN);
        }

        // Restrict geneIds by stats query for factor conditions
        geneIds = atlasStatisticsQueryService.restrictGenesByStatsQuery(statsQuery, geneIds);
        log.debug("Restrict geneIds by stats query for factor conditions - size: {}", geneIds.size());

        // Now retrieve experiment-statistic-attribute data for stats query for geneIds and factor conditions
        statsQuery.setBioEntityIdRestrictionSet(geneIds);
        Multimap<ExperimentInfo, Pair<StatisticsType, EfAttribute>> scoringExpsAttrs =
                atlasStatisticsQueryService.getScoringExpsAttrs(statsQuery);
        log.debug("Overall bit index query time: {} of # experiments: {}", (System.currentTimeMillis() - start), scoringExpsAttrs.asMap().size());

        log.info("Getting genes out of Solr for query: '{}' ...", query.toString());
        // Retrieve genes from Solr
        start = System.currentTimeMillis();
        Map<Long, String> id2GeneInfo = new HashMap<Long, String>();
        Iterable<AtlasGene> genes = geneSolrDAO.getGenesByIds(geneIds);
        for (AtlasGene gene : genes) {
            id2GeneInfo.put((long) gene.getGeneId(), getGeneInfo(gene));
        }
        log.info("Solr gene retrieval time: {} ms", (System.currentTimeMillis() - start));

        // Now traverse the experiments
        start = System.currentTimeMillis();
        Map<ExperimentInfo, Collection<Pair<StatisticsType, EfAttribute>>> expToAttrs = scoringExpsAttrs.asMap();
        // Note that the download progress bar will be reporting how many experiments in expToAttrs the download got through
        totalResults.set(expToAttrs.entrySet().size());
        log.info("Downloading data for query '{}' - now collecting data from {} experiments...", query.toString(), totalResults);
        for (Map.Entry<ExperimentInfo, Collection<Pair<StatisticsType, EfAttribute>>> expAttr : expToAttrs.entrySet()) {
            zout.write(atlasStructuredQueryService.getDataFromExperiment(expAttr.getKey().getAccession(), expAttr.getValue(), id2GeneInfo).getBytes("UTF-8"));
            incrementResultsRetrieved();
        }
        log.info("Collected data from {} experiments in: {} ms ", totalResults, (System.currentTimeMillis() - start));
    }

    /**
     * @param gene
     * @return Information for gene that will be included in the Download output
     */
    private String getGeneInfo(AtlasGene gene) {
        return new StringBuilder()
                .append(gene.getGeneName()).append("\t")
                .append(gene.getGeneIdentifier()).append("\t")
                .append(gene.getGeneSpecies()).append("\t")
                .toString();
    }
}
