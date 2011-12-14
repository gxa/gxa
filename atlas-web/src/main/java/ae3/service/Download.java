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
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.Pair;

import javax.annotation.Nonnull;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Represents a download event for exporting atlas list results to files
 *
 * @author iemam
 */
public class Download implements Runnable {

    private static final Function<String, Attribute> ATTRIBUTE_FUNC =
            new Function<String, Attribute>() {
                public Attribute apply(@Nonnull String efoTerm) {
                    return new EfoAttribute(efoTerm);
                }
            };

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private AtlasStructuredQueryService atlasStructuredQueryService;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private GeneSolrDAO geneSolrDAO;

    private int id;
    private final AtlasStructuredQuery query;

    private File outputFile;

    private long totalResults = 0;
    private long resultsRetrieved = 0;
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
        if (0 == getTotalResults()) return 0;
        if (getResultsRetrieved() == getTotalResults()) return 100;

        return Math.floor(100.0 * getResultsRetrieved() / getTotalResults());
    }

    public void run() {
        if (query == null || query.isNone())
            return;

        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(new FileOutputStream(getOutputFile()));
            zout.putNextEntry(new ZipEntry("listdl.tab"));
            getData(query, zout);
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

    private void outputHeader(OutputStream out) throws IOException {
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss");
        StringBuilder strBuf = new StringBuilder();

        strBuf.append("# Atlas data version: ").append(dataVersion).append("\n");
        strBuf.append("# Query: ").append(query).append("\n");
        strBuf.append("# Note that to download non-differential expression data the 'non-d.e. in' option needs to be selected on Atlas search page\n");
        strBuf.append("# Timestamp: ").append(formatter.format(today)).append("\n");

        strBuf.append("Gene name").append("\t").append("Gene identifier").append("\t").append("Organism").append("\t");
        strBuf.append("Experimental factor").append("\t").append("Factor value").append("\t");
        strBuf.append("Experiment accession").append("\t").append("Array Design accession").append("\t");
        strBuf.append("Expression").append("\t").append("P-value").append("\n");

        out.write(strBuf.toString().getBytes("UTF-8"));
    }

    private void incrementResultsRetrieved() {
        resultsRetrieved++;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(long total) {
        this.totalResults = total;
    }

    public int getId() {
        return id;
    }

    public long getResultsRetrieved() {
        return resultsRetrieved;
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

    public void getData(@Nonnull AtlasStructuredQuery query, @Nonnull OutputStream zout) throws IOException {
        outputHeader(zout);

        long start = System.currentTimeMillis();
        // Construct a bit index query from all genes, species and factors conditions
        StatisticsQueryCondition statsQuery = constructStatsQuery(query);
        // Now retrieve experiment-statistic-attribute data for statsQuery
        ArrayListMultimap<ExperimentInfo, Pair<StatisticsType, EfAttribute>> scoringExpsAttrs =
                atlasStatisticsQueryService.getScoringExpsAttrs(statsQuery);
        log.debug("Overall bit index query time: {} of # experiments: {}", (System.currentTimeMillis() - start), scoringExpsAttrs.asMap().size());

        log.info("Getting genes out of Solr for query: '{}' ...", query.toString());
        // Retrieve genes from Solr
        start = System.currentTimeMillis();
        Map<Long, String> id2GeneInfo = new HashMap<Long, String>();
        Iterable<AtlasGene> genes = geneSolrDAO.getGenesByIds(statsQuery.getBioEntityIdRestrictionSet());
        for (AtlasGene gene : genes) {
            id2GeneInfo.put((long) gene.getGeneId(), getGeneInfo(gene));
        }
        log.info("Solr gene retrieval time: {} ms", (System.currentTimeMillis() - start));

        // Now traverse the experiments
        start = System.currentTimeMillis();
        Map<ExperimentInfo, Collection<Pair<StatisticsType, EfAttribute>>> expToAttrs = scoringExpsAttrs.asMap();
        // Note that the download progress bar will be reporting how many experiments in expToAttrs the download got through
        setTotalResults(expToAttrs.entrySet().size());
        log.info("Downloading data for query '{}' - now collecting data from {} experiments...", query.toString(), getTotalResults());
        for (Map.Entry<ExperimentInfo, Collection<Pair<StatisticsType, EfAttribute>>> expAttr : expToAttrs.entrySet()) {
            zout.write(atlasStructuredQueryService.getDataFromExperiment(expAttr.getKey().getAccession(), expAttr.getValue(), id2GeneInfo).getBytes("UTF-8"));
            incrementResultsRetrieved();
        }
        log.info("Collected data from {} experiments in: {} ms ", getTotalResults(), (System.currentTimeMillis() - start));
    }

    /**
     * @param query
     * @return StatisticsQueryCondition containing all genes, species and factors conditions in query
     */
    private StatisticsQueryCondition constructStatsQuery(AtlasStructuredQuery query) {

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


        // If this is a gene conditions (plus optional species) only query, populate it with all efo terms that are scoring for geneIds
        if (statsQuery.getConditions().isEmpty()) {
            Set<String> scoringEfos = atlasStatisticsQueryService.getScoringEfosForBioEntities(new HashSet<Integer>(geneIds), statsQuery.getStatisticsType());
            statsQuery.and(atlasStatisticsQueryService.getStatisticsOrQuery(Collections2.transform(scoringEfos, ATTRIBUTE_FUNC), statsQuery.getStatisticsType(), 1));
        }

        // Finally, restrict query by geneIds
        statsQuery.setBioEntityIdRestrictionSet(geneIds);

        return statsQuery;
    }

    /**
     * @param gene
     * @return Information for gene that will be included in the Download output
     */
    private String getGeneInfo(AtlasGene gene) {
        StringBuilder sb = new StringBuilder();
        sb.append(gene.getGeneName()).append("\t");
        sb.append(gene.getGeneIdentifier()).append("\t");
        sb.append(gene.getGeneSpecies()).append("\t");
        return sb.toString();
    }
}
