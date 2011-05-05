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

package ae3.service.structuredquery;

import ae3.dao.ExperimentSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.model.ListResultRowExperiment;
import ae3.service.AtlasStatisticsQueryService;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.utils.Maker;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Expression;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;


/**
 * Structured query support class. The main query engine of the Atlas.
 *
 * @author pashky
 */
public class AtlasStructuredQueryService implements IndexBuilderEventHandler, DisposableBean {

    // This variable acts as a place holder for a heatmap column index that has not been set yet
    private static final int POS_NOT_SET = -1;
    // Maximum number of efv columns to be shown by default in web (non-full heatmap) queries.
    // In web queries, if some efvs had been trimmed in a given ef, the user can expand that ef to see all
    // trimmed efvs.
    // Note that MAX_EFV_COLUMNS columns restriction does not apply to API (full heatmap) queries.
    private static final int MAX_EFV_COLUMNS = 120;
    private static final boolean INCLUDE_EFO_PARENTS_IN_HEATMAP = true;

    final private Logger log = LoggerFactory.getLogger(getClass());

    private SolrServer solrServerAtlas;
    private SolrServer solrServerExpt;
    private SolrServer solrServerProp;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;

    private AtlasEfvService efvService;
    private AtlasEfoService efoService;
    private AtlasGenePropertyService genePropService;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    private ExperimentSolrDAO experimentSolrDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;

    private CoreContainer coreContainer;

    private Efo efo;

    private final Set<String> cacheFill = new HashSet<String>();
    private SortedSet<String> allSpecies = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Hack: prevents OOMs by clearing Lucene field cache by closing the searcher which closes the IndexReader
     * (it's the only way now if we don't hack Lucene)
     */
    private void controlCache() {
        if (coreContainer == null)
            return;

        synchronized (cacheFill) {
            if (cacheFill.size() > 500) {
                SolrCore core = coreContainer.getCore(Constants.CORE_ATLAS);
                if (core != null) {
                    core.closeSearcher();
                    core.close();
                }
                cacheFill.clear();
            }
        }
    }

    /**
     * Adds field to cache watcher (it's supposed to estimate number of fields which actually end up in Lucene cache,
     * which we can't check directly)
     *
     * @param field the name of field to add
     */
    private void notifyCache(String field) {
        synchronized (cacheFill) {
            cacheFill.add(field);
        }
    }

    public void setSolrServerAtlas(SolrServer solrServerAtlas) {
        this.solrServerAtlas = solrServerAtlas;
    }

    public void setSolrServerExpt(SolrServer solrServerExpt) {
        this.solrServerExpt = solrServerExpt;
    }

    public void setSolrServerProp(SolrServer solrServerProp) {
        this.solrServerProp = solrServerProp;
    }

    public void setCoreContainer(CoreContainer coreContainer) {
        this.coreContainer = coreContainer;
    }

    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    public void setEfvService(AtlasEfvService efvService) {
        this.efvService = efvService;
    }

    public void setEfoService(AtlasEfoService efoService) {
        this.efoService = efoService;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public Efo getEfo() {
        return efo;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    public void setGenePropService(AtlasGenePropertyService genePropService) {
        this.genePropService = genePropService;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public Set<String> getAllFactors() {
        return efvService.getAllFactors();
    }

    /**
     * SOLR query builder class. Collects necessary part of SOLR query string as we go through conditions.
     * Can't use just StringBuilder as we need to maintain two separate chains - query itself and scoring function.
     * <p/>
     * Can be used as chain of calls as all appendXX() methods return self
     */
    private static class SolrQueryBuilder {
        /**
         * Query string
         */
        private StringBuilder solrq = new StringBuilder();

        /**
         * Appends AND to query only if it is needed
         *
         * @return self
         */
        public SolrQueryBuilder appendAnd() {
            if (solrq.length() > 0)
                solrq.append(" AND ");
            return this;
        }

        /**
         * Appends string to query
         *
         * @param s string
         * @return self
         */
        public SolrQueryBuilder append(String s) {
            solrq.append(s);
            return this;
        }

        /**
         * Appends object to query
         *
         * @param s object
         * @return self
         */
        public SolrQueryBuilder append(Object s) {
            solrq.append(s);
            return this;
        }

        /**
         * Appends other SB to query
         *
         * @param s SB
         * @return self
         */
        public SolrQueryBuilder append(StringBuilder s) {
            solrq.append(s);
            return this;
        }

        /**
         * Returns assembled query string
         *
         * @return string
         */
        @Override
        public String toString() {
            return solrq.toString();
        }

        /**
         * Checks if query is empty
         *
         * @return true or false
         */
        public boolean isEmpty() {
            return solrq.length() == 0;
        }
    }

    /**
     * Column information class to be used as paylod in result EFV tree. Base version storing just position
     * of EFV data in result counters array
     */
    private static class BaseColumnInfo implements ColumnInfo {
        private int position;

        private BaseColumnInfo(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

        public int compareTo(ColumnInfo o) {
            return getPosition() - o.getPosition();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BaseColumnInfo that = (BaseColumnInfo) o;

            if (position != that.position) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return position;
        }

        public boolean isQualified(UpdownCounter ud) {
            return !ud.isZero();
        }

        /**
         * @return true, if the heatmap column position has been set to a valid (>= 0) value
         */
        public boolean isPositionSet() {
            return position != POS_NOT_SET;
        }
    }

    /**
     * Extended version of columninfo, checking required minimum number of experiments
     */
    private static class QueryColumnInfo extends BaseColumnInfo {
        private int minUpExperiments = Integer.MAX_VALUE;
        private int minDnExperiments = Integer.MAX_VALUE;
        private int minOrExperiments = Integer.MAX_VALUE;
        private int minNoExperiments = Integer.MAX_VALUE;
        private boolean displayNonDECounts = false;

        /**
         * This constructor is used when QueryState is populated with efos/efvs - using POS_NOT_SET emphasizes the fact
         * that no final heatmap column positionality is set in QueryState. All such information is worked out in processResultGenes()
         * method call. The order of efo's displayed in the heatmap reflect their order of entry into QueryState. The order of efv's
         * (within each ef grouping) however is dictated by the cumulative column experiment counts for each efv and cannot
         * be decided until experiment counts for al heatmap cells have been calculated.
         */
        private QueryColumnInfo() {
            super(POS_NOT_SET);
        }

        /**
         * Update column minimum requirements with provided query information
         * (to be called on each query condition)
         *
         * @param expression     query expression
         * @param minExperiments minimum number of experiments for this expression
         */
        public void update(QueryExpression expression, int minExperiments) {
            switch (expression) {
                case UP:
                case UP_ONLY:
                    minUpExperiments = Math.min(minExperiments, this.minUpExperiments);
                    break;
                case DOWN:
                case DOWN_ONLY:
                    minDnExperiments = Math.min(minExperiments, this.minDnExperiments);
                    break;
                case UP_DOWN:
                    minOrExperiments = Math.min(minExperiments, this.minOrExperiments);
                    break;
                case NON_D_E:
                    minNoExperiments = Math.min(minExperiments, this.minNoExperiments);
                    displayNonDECounts = true;
                    break;
                case ANY:
                    minOrExperiments = Math.min(minExperiments, this.minOrExperiments);
                    minNoExperiments = Math.min(minExperiments, this.minNoExperiments);
                    displayNonDECounts = true;
                    break;
            }
        }

        /**
         * @return true if non-de counts should be displayed in this column; false otherwise
         */
        public boolean displayNonDECounts() {
            return displayNonDECounts;
        }

        /**
         * Here it checks counter against minimal numbers
         *
         * @param ud counter
         * @return true or false
         */
        public boolean isQualified(UpdownCounter ud) {
            return ud.getUps() >= minUpExperiments ||
                    ud.getDowns() >= minDnExperiments ||
                    ud.getNones() >= minNoExperiments ||
                    ud.getUps() >= minOrExperiments ||
                    ud.getDowns() >= minOrExperiments;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            QueryColumnInfo that = (QueryColumnInfo) o;

            if (minDnExperiments != that.minDnExperiments) return false;
            if (minNoExperiments != that.minNoExperiments) return false;
            if (minOrExperiments != that.minOrExperiments) return false;
            if (minUpExperiments != that.minUpExperiments) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + minUpExperiments;
            result = 31 * result + minDnExperiments;
            result = 31 * result + minOrExperiments;
            result = 31 * result + minNoExperiments;
            return result;
        }
    }

    /**
     * Internal class to pass query state around methods (the main class itself is stateless hence thread-safe)
     */
    private class QueryState {
        private final SolrQueryBuilder solrq = new SolrQueryBuilder();
        private final EfvTree<ColumnInfo> efvs = new EfvTree<ColumnInfo>();
        private final EfoTree<ColumnInfo> efos = new EfoTree<ColumnInfo>(getEfo());
        private final Set<Long> experiments = new HashSet<Long>();
        private final Set<String> scoringEfos = new HashSet<String>();

        /**
         * Column numberer factory used to add new EFV columns into heatmap
         */
        private Maker<ColumnInfo> numberer = new Maker<ColumnInfo>() {
            public ColumnInfo make() {
                return new QueryColumnInfo();
            }
        };

        /**
         * Returns SOLR query builder
         *
         * @return solr query builder
         */
        public SolrQueryBuilder getSolrq() {
            return solrq;
        }

        /**
         * Adds experiment IDs to query
         *
         * @param ids identifiers of experiments to be added to the query
         */
        public void addExperiments(Collection<Long> ids) {
            experiments.addAll(ids);
        }

        /**
         * Add a Collection of efos with non-zero bit index experiment counts for the genes to be displayed on the heatmap
         *
         * @param scoringEfos
         */
        public void addScoringEfos(Collection<String> scoringEfos) {
            this.scoringEfos.addAll(scoringEfos);
        }


        /**
         * Adds EFV to query EFV tree
         *
         * @param ef             factor
         * @param efv            value
         * @param minExperiments required minimum number of experiments
         * @param expression     query expression
         */
        public void addEfv(String ef, String efv, int minExperiments, QueryExpression expression) {
            ((QueryColumnInfo) efvs.getOrCreate(ef, efv, numberer)).update(expression, minExperiments);
        }

        /**
         * Adds EFO accession to query EFO tree, (including its efo children for ViewType.LIST)
         *
         * @param id                         EFO accession
         * @param minExperiments             required minimum number of experiments
         * @param expression                 query expression
         * @param maxEfoDescendantGeneration Specifies the generation down to which this efo's descendants should be included;
         *                                   Integer.MAX_VALUE indicates that all descendants
         *                                   should be included recursively
         */
        public void addEfo(String id, int minExperiments, QueryExpression expression, int maxEfoDescendantGeneration) {
            for (ColumnInfo ci : efos.add(id, numberer, maxEfoDescendantGeneration, !INCLUDE_EFO_PARENTS_IN_HEATMAP))
                ((QueryColumnInfo) ci).update(expression, minExperiments);
        }

        /**
         * Returns set of experiments mentioned in the query
         *
         * @return set of experiment IDs
         */
        public Set<Long> getExperiments() {
            return experiments;
        }

        /**
         * @return Set of efos with non-zero bit index experiment counts for the genes to be displayed on the heatmap
         */
        public Set<String> getScoringEfos() {
            return scoringEfos;
        }

        /**
         * Returns query EFV tree
         *
         * @return query EFV tree
         */
        public EfvTree<ColumnInfo> getEfvs() {
            return efvs;
        }

        /**
         * Returns query EFO tree
         *
         * @return query EFO tree
         */
        public EfoTree<ColumnInfo> getEfos() {
            return efos;
        }

        /**
         * Checks if query is empty
         *
         * @return true or false
         */
        public boolean isEmpty() {
            return solrq.isEmpty();
        }

        /**
         * Checks if query has any condition EFV/EFOs
         *
         * @return true if query has EFV or EFO conditions, false otherwise
         */
        public boolean hasQueryEfoEfvs() {
            return efvs.getNumEfvs() > 0 || efos.getNumEfos() > 0;
        }

        /**
         * Informative string representing the query
         *
         * @return string representation of the object
         */
        @Override
        public String toString() {
            return "SOLR query: <" + solrq.toString() + ">, Experiments: [" + StringUtils.join(experiments, ", ") + "]";
        }
    }


    /**
     * Creates SOLR query from atlas query
     *
     * @param solrq
     * @return solr query object
     */
    private SolrQuery getFastGeneSolrQuery(SolrQueryBuilder solrq) {
        SolrQuery q = new SolrQuery(solrq.toString());

        q.addFacetField("id");
        q.setRows(0);
        q.setFacet(true);
        q.setFacetLimit(-1);
        q.setFacetMinCount(1);
        log.debug("Simple gene query: " + solrq.toString());
        log.debug("Expanded simple gene query: " + q.toString());
        return q;
    }

    private Set<Integer> getGenesByGeneConditionsAndSpecies(Collection<GeneQueryCondition> geneConditions, Collection<String> species) {
        Set<Integer> geneIds = new HashSet<Integer>();
        SolrQueryBuilder solrq = new SolrQueryBuilder();
        appendGeneQuery(geneConditions, solrq);
        appendSpeciesQuery(species, solrq);
        if (solrq.isEmpty()) {
            return geneIds;
        }
        SolrQuery q = getFastGeneSolrQuery(solrq);

        try {
            long start = System.currentTimeMillis();
            QueryResponse qr = solrServerAtlas.query(q);
            if (qr.getFacetFields().get(0).getValues() != null) {
                for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                    geneIds.add(Integer.parseInt(ffc.getName()));
                }
            }
            log.info("Simple gene query: " + solrq.toString() + " returned " + geneIds.size() + " gene ids in " + (System.currentTimeMillis() - start) + " ms");
        } catch (SolrServerException e) {
            throw logUnexpected("Failed to fetch genes by conditions and species using query: '" + solrq.toString() + "'", e);
        }

        return geneIds;

    }

    /**
     * Process structured Atlas query
     *
     * @param query parsed query
     * @return matching results
     */
    public AtlasStructuredQueryResult doStructuredAtlasQuery(final AtlasStructuredQuery query) {

        // Flag to indicate if pvals/tstats should be retrieved from bit index and used for heatmap row ordering - for more
        // information see documentation for atlas.structured.query.max.* constants in atlas.properties
        boolean usePvalsInHeatmapOrdering = true;
        final QueryState qstate = new QueryState();
        AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(query.getStart(), query.getRowsPerPage(), query.getExpsPerGene());

        // Get genes ids from genes index by gene and species query conditions
        Set<Integer> genesByGeneConditionsAndSpecies = getGenesByGeneConditionsAndSpecies(query.getGeneConditions(), query.getSpecies());
        if (query.getGeneConditions().size() > 0 && genesByGeneConditionsAndSpecies.size() == 0) {
            // if the user searched for a non-existent gene - return an empty result set
            return result;
        }

        // Now refine the gene set by retrieving the requested batch size from a list sorted by experiment counts found in bit index
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition();
        Collection<ExpFactorResultCondition> conditions = appendEfvsQuery(query, qstate, statsQuery);
        if (statsQuery.getStatisticsType() == null) {
            statsQuery.setStatisticsType(StatisticsType.UP_DOWN);
        }

        int mappingCount = 0;
        for (EfoTree.EfoItem efoItem : qstate.getEfos().getValueOrderedList()) {
            mappingCount += atlasStatisticsQueryService.getMappingsCountForEfo(efoItem.getId());

        }

        log.debug("genes: " + genesByGeneConditionsAndSpecies.size() + "; efos: " + qstate.getEfos().getNumEfos());


        List<Integer> genesByConditions = new ArrayList<Integer>();
        Pair<Integer, Integer> counts = atlasStatisticsQueryService.getSortedBioEntities(statsQuery, query.getStart(), query.getRowsPerPage(), genesByGeneConditionsAndSpecies, genesByConditions);

        Integer numOfResults = counts.getFirst();
        Integer totalExperimentCount = counts.getSecond();

        log.info("Total efo mappings count: " + mappingCount + "; total experiment count: " + totalExperimentCount);

        // Impose restrictions on mappingCount and totalExperimentCount - for more information see documentation for
        // atlas.structured.query.max.* constants in atlas.properties
        if (mappingCount > atlasProperties.getMaxEfoMappingsCountForStructuredQuery() ||
                totalExperimentCount > atlasProperties.getMaxExperimentCountForStructuredQuery()) {
            if (query.isFullHeatmap()) { // API queries
                StringBuilder errMsg = new StringBuilder();
                errMsg.append("Atlas cannot handle this query in a timely fashion. ");
                if (mappingCount > atlasProperties.getMaxEfoMappingsCountForStructuredQuery()) {
                    errMsg.append("Please try again after restricting the condition part of your query.");
                    log.warn("API query too complex - efo mapping count: " + mappingCount + " > " + atlasProperties.getMaxEfoMappingsCountForStructuredQuery());
                } else {
                    errMsg.append("Please try again after restricting the gene and/or condition part of your query.");
                    log.warn("API query too complex - heatmap's total experiment count: " + totalExperimentCount + " > " + atlasProperties.getMaxExperimentCountForStructuredQuery());
                }
                result.setUserErrorMsg(errMsg.toString());
                return result;
            } else { // web queries
                usePvalsInHeatmapOrdering = false;
            }
        }

        appendGeneQuery(genesByConditions, qstate.getSolrq());

        result.setConditions(conditions);

        if (!qstate.isEmpty()) {

            // scoringEfos is used for deciding if an efo term in heatmap header should be made expandable
            Set<String> scoringEfos = atlasStatisticsQueryService.getScoringEfosForBioEntities(new HashSet<Integer>(genesByConditions), statsQuery.getStatisticsType());
            qstate.addScoringEfos(scoringEfos);

            try {

                controlCache();

                SolrQuery q = setupSolrQuery(query.getRowsPerPage(), qstate);
                long timeStart = System.currentTimeMillis();

                QueryResponse response = solrServerAtlas.query(q);
                log.info("Solr query: " + query.getApiUrl() + ": " + qstate.toString() + " took: " + (System.currentTimeMillis() - timeStart) + " ms");
                timeStart = System.currentTimeMillis();
                processResultGenes(response, result, qstate, query, numOfResults, statsQuery, usePvalsInHeatmapOrdering);
                log.info("processResultGenes took: " + (System.currentTimeMillis() - timeStart) + " ms");

                Set<String> expandableEfs = new HashSet<String>();
                EfvTree<ColumnInfo> trimmedEfvs = trimColumns(query, result, expandableEfs);
                result.setResultEfvs(trimmedEfvs);
                result.setExpandableEfs(expandableEfs);

                if (response.getFacetFields() != null) {
                    for (String p : genePropService.getDrilldownProperties()) {
                        Set<String> hasVals = new HashSet<String>();
                        for (GeneQueryCondition qc : query.getGeneConditions())
                            if (qc.getFactor().equals(p))
                                hasVals.addAll(qc.getFactorValues());

                        Iterable<FacetCounter> facet = getGeneFacet(response, "property_f_" + p, hasVals);
                        if (facet.iterator().hasNext())
                            result.setGeneFacet(p, facet);
                    }
                    if (!query.getSpecies().iterator().hasNext())
                        result.setGeneFacet("species", getGeneFacet(response, "species", new HashSet<String>()));
                }
            } catch (SolrServerException e) {
                log.error("Error in structured query!", e);
            }
        }

        return result;
    }


    /**
     * Trims factors to contain only small amount of EFVs if too many of them were requested
     * User can ask to expand some of them
     *
     * @param query         query to process
     * @param result        result to process
     * @param expandableEfs which EFs to expand in result
     * @return trimmed result EFV tree
     */
    private EfvTree<ColumnInfo> trimColumns(final AtlasStructuredQuery query,
                                            final AtlasStructuredQueryResult result,
                                            Collection<String> expandableEfs) {
        final Set<String> expand = query.getExpandColumns();
        EfvTree<ColumnInfo> trimmedEfvs = new EfvTree<ColumnInfo>(result.getResultEfvs());
        if (expand.contains("*"))
            return trimmedEfvs;

        if (query.isFullHeatmap() || trimmedEfvs.getNumEfvs() < MAX_EFV_COLUMNS)
            return trimmedEfvs;


        int threshold = Math.max(1, MAX_EFV_COLUMNS / trimmedEfvs.getNumEfs());

        for (EfvTree.Ef<ColumnInfo> ef : trimmedEfvs.getEfValueSortedTree()) {
            if (expand.contains(ef.getEf()) || ef.getEfvs().size() < threshold)
                continue;

            Map<EfvTree.Efv<ColumnInfo>, Double> scores = new HashMap<EfvTree.Efv<ColumnInfo>, Double>();
            for (EfvTree.Efv<ColumnInfo> efv : ef.getEfvs())
                scores.put(efv, 0.0);

            for (StructuredResultRow row : result.getResults()) {
                for (EfvTree.Efv<ColumnInfo> efv : ef.getEfvs()) {
                    UpdownCounter c = row.getCounters().get(efv.getPayload().getPosition());
                    scores.put(efv, scores.get(efv) + c.getDowns() * (1.0 - c.getMpvDn()) + c.getUps() * (1.0 - c.getMpvUp()));
                }
            }

            @SuppressWarnings("unchecked")
            Map.Entry<EfvTree.Efv<ColumnInfo>, Double>[] scoreset = scores.entrySet().toArray(new Map.Entry[1]);
            Arrays.sort(scoreset, new Comparator<Map.Entry<EfvTree.Efv<ColumnInfo>, Double>>() {
                public int compare(Map.Entry<EfvTree.Efv<ColumnInfo>, Double> o1, Map.Entry<EfvTree.Efv<ColumnInfo>, Double> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });

            for (int i = threshold; i < scoreset.length; ++i) {
                trimmedEfvs.removeEfv(ef.getEf(), scoreset[i].getKey().getEfv());
                expandableEfs.add(ef.getEf());
            }
        }

        return trimmedEfvs;
    }

    /**
     * Finds experiment by search string
     *
     * @param query    search strings
     * @param condEfvs EFV tree to fill with EFVs mentioned in experiment
     * @return collection of found experiment IDs
     * @throws SolrServerException in case of any problem with SOLR
     */
    private Collection<Long> findExperiments(String query, EfvTree<Boolean> condEfvs) throws SolrServerException {

        List<Long> result = new ArrayList<Long>();
        if (query.length() == 0)
            return result;

        SolrQuery q = new SolrQuery("id:(" + query + ") accession:(" + query + ")");
        q.addField("*");
        q.setRows(50);
        q.setStart(0);

        QueryResponse qr = solrServerExpt.query(q);
        for (SolrDocument doc : qr.getResults()) {
            String id = String.valueOf(doc.getFieldValue("id"));
            if (id != null) {
                result.add(Long.parseLong(id));
                for (String name : doc.getFieldNames())
                    if (name.startsWith("a_property_"))
                        for (Object val : doc.getFieldValues(name))
                            condEfvs.put(name.substring("a_property_".length()), String.valueOf(val), true);
            }
        }

        return result;
    }

    /**
     * @param expression
     * @return StatisticsType equivalent of expression
     */
    private StatisticsType getStatisticsTypeForExpression(QueryExpression expression) {
        if (QueryExpression.ANY == expression) {
            // If the user selects ANY expression, we still default to UP_DOWN, with the proviso that
            // non-de counts will be shown in heatmap (if the user select UP_DOWN, non-de counts are excluded from heatmap)
            expression = QueryExpression.UP_DOWN;
        }
        return StatisticsType.valueOf(expression.toString());
    }

    /**
     * Appends conditions part of the query to query state. Finds matching EFVs/EFOs and appends them to SOLR query string.
     *
     * @param query  query
     * @param qstate state
     * @return iterable conditions resulted from this append
     */
    private Collection<ExpFactorResultCondition> appendEfvsQuery(final AtlasStructuredQuery query, final QueryState qstate, StatisticsQueryCondition statsQuery) {
        final List<ExpFactorResultCondition> conds = new ArrayList<ExpFactorResultCondition>();
        // TODO SolrQueryBuilder solrq = qstate.getSolrq();

        for (ExpFactorQueryCondition c : query.getConditions()) {
            if (statsQuery.getStatisticsType() == null) {
                statsQuery.setStatisticsType(getStatisticsTypeForExpression(c.getExpression()));
            }

            List<Attribute> orAttributes = null;
            if (c.isAnything() || c.isAnyValue()) {
                // do nothing
            } else if (c.isOnly() && !c.isAnyFactor()
                    && !Constants.EFO_FACTOR_NAME.equals(c.getFactor())) {
                try {
                    EfvTree<Boolean> condEfvs = getCondEfvsForFactor(c.getFactor(), c.getFactorValues());
                    EfvTree<Boolean> allEfvs = getCondEfvsAllForFactor(c.getFactor());
                    if (condEfvs.getNumEfs() + allEfvs.getNumEfs() > 0) {
                        // TODO solrq.appendAnd().append("((");
                        for (EfvTree.EfEfv<Boolean> condEfv : condEfvs.getNameSortedList()) {
                            // TODO solrq.append(" ");

                            String efefvId = condEfv.getEfEfvId();
                            // TODO solrq.appendExpFields(efefvId, c.getExpression(), c.getMinExperiments());
                            // TODO solrq.appendExpScores(efefvId, c.getExpression());

                            notifyCache(efefvId + c.getExpression());
                            qstate.addEfv(condEfv.getEf(), condEfv.getEfv(), c.getMinExperiments(), c.getExpression());
                        }
                        // TODO solrq.append(")");
                        for (EfvTree.EfEfv<Boolean> allEfv : allEfvs.getNameSortedList())
                            if (!condEfvs.has(allEfv.getEf(), allEfv.getEfv())) {
                                String efefvId = allEfv.getEfEfvId();
                                // TODO solrq.append(" AND NOT (");
                                // TODO solrq.appendExpFields(efefvId, c.getExpression(), 1);
                                // TODO solrq.append(")");
                                notifyCache(efefvId + c.getExpression());
                                qstate.addEfv(allEfv.getEf(), allEfv.getEfv(), 1, QueryExpression.UP_DOWN);
                            }
                        // TODO solrq.append(")");
                        conds.add(new ExpFactorResultCondition(c,
                                Collections.<List<AtlasEfoService.EfoTermCount>>emptyList(),
                                false));
                    }
                } catch (SolrServerException e) {
                    log.error("Error querying Atlas index", e);
                }

            } else {
                orAttributes = new ArrayList<Attribute>();
                try {
                    boolean nonemptyQuery = false;
                    EfvTree<Boolean> condEfvs = getConditionEfvs(c);
                    if (condEfvs.getNumEfs() > 0) {
                        // If the number of efv/efo conditions matching user's query exceeds MAX_EFV_COLUMNS, we don't
                        // search with/show efos.
                        // Note that efos are also not shown on heatmap for gene condition-only queries,
                        // except that only DAS ef's are shown in the heatmap with three efvs per factor.
                        // If user's query does contain efv/efo conditions, all matching efvs are searched for/included in heatmap,
                        // and though heatmap is trimmed to max MAX_EFV_COLUMNS the user can click on 'expand' link under
                        // each factor to see all efvs.
                        List<EfvTree.EfEfv<Boolean>> conditions = condEfvs.getNameSortedList();
                        boolean excludeEfos = false;
                        int totalConditions = conditions.size();
                        log.info("User query matched: " + totalConditions + " efv/efo conditions");
                        if (totalConditions > MAX_EFV_COLUMNS) {
                            excludeEfos = true;
                        }

                        for (EfvTree.EfEfv<Boolean> condEfv : conditions) {
                            String efefvId = condEfv.getEfEfvId();

                            notifyCache(efefvId + c.getExpression());
                            Attribute attribute;

                            // If ef key equals EFO_WITH_CHILDREN_PREAMBLE (c.f. getCondEfvsForFactor()), set
                            // includeEfoChildren flag for condEfv.getEfv() efo term.
                            String ef = condEfv.getEf();
                            // includeEfoDescendantGeneration == 0 ==> don't include any children
                            // includeEfoDescendantGeneration == 1 ==> include immediate children only
                            // includeEfoDescendantGeneration == 2 ==> include immediate children and grandchildren only
                            // ...
                            // includeEfoDescendantGeneration == Integer.MAX_VALUE ==> include all descendants recursively

                            // For List view and for API queries, include all children recursively; otherwise (i.e. for heatmap web queries).
                            // always include immediate children and grandchildren only
                            int maxEfoDescendantGeneration = (query.getViewType() == ViewType.LIST || query.isFullHeatmap() ? Integer.MAX_VALUE : 2);

                            if (Constants.EFO_FACTOR_NAME.equals(ef) || Constants.EFO_WITH_CHILDREN_PREAMBLE.equals(ef)) {
                                if (!excludeEfos) {
                                    qstate.addEfo(condEfv.getEfv(), c.getMinExperiments(), c.getExpression(), maxEfoDescendantGeneration);
                                    attribute = new EfoAttribute(condEfv.getEfv(), getStatisticsTypeForExpression(c.getExpression()));
                                    orAttributes.add(attribute);
                                }
                            } else {
                                qstate.addEfv(condEfv.getEf(), condEfv.getEfv(), c.getMinExperiments(), c.getExpression());
                                attribute = new EfvAttribute(condEfv.getEf(), condEfv.getEfv(), getStatisticsTypeForExpression(c.getExpression()));
                                orAttributes.add(attribute);
                            }
                        }
                        nonemptyQuery = true;
                    }
                    Collection<List<AtlasEfoService.EfoTermCount>> efoPaths = new ArrayList<List<AtlasEfoService.EfoTermCount>>();
                    Collection<EfvTree.Efv<Boolean>> condEfos = condEfvs.getEfvs(Constants.EFO_FACTOR_NAME);
                    for (EfvTree.Efv<Boolean> efv : condEfos) {
                        efoPaths.addAll(efoService.getTermParentPaths(efv.getEfv()));
                    }
                    conds.add(new ExpFactorResultCondition(c, efoPaths, !nonemptyQuery));
                } catch (SolrServerException e) {
                    log.error("Error querying Atlas index", e);
                }
            }
            if (orAttributes != null) {
                statsQuery.and(atlasStatisticsQueryService.getStatisticsOrQuery(orAttributes, c.getMinExperiments()));
                log.debug("Adding the following " + orAttributes.size() + " attributes to stats query: " + orAttributes);
            }
        }

        return conds;
    }


    /**
     * Appends gene part of the query. Parses query condtions and appends them to SOLR query string.
     *
     * @param geneConditions
     * @param solrq          solr query
     */
    private void appendGeneQuery(Collection<GeneQueryCondition> geneConditions, SolrQueryBuilder solrq) {
        for (GeneQueryCondition geneQuery : geneConditions) {
            String escapedQ = geneQuery.getSolrEscapedFactorValues();
            if (geneQuery.isAnyFactor()) {
                solrq.appendAnd();
                if (geneQuery.isNegated())
                    solrq.append(" NOT ");
                solrq.append("(name:(").append(escapedQ).append(") species:(").append(escapedQ)
                        .append(") identifier:(").append(escapedQ).append(") id:(").append(escapedQ).append(")");
                for (String p : genePropService.getIdNameDescProperties())
                    solrq.append(" property_").append(p).append(":(").append(escapedQ).append(")");
                solrq.append(") ");
            } else if (Constants.GENE_PROPERTY_NAME.equals(geneQuery.getFactor())) {
                solrq.appendAnd();
                if (geneQuery.isNegated())
                    solrq.append(" NOT ");
                solrq.append("(name:(").append(escapedQ).append(") ");
                solrq.append("identifier:(").append(escapedQ).append(") ");
                solrq.append("id:(").append(escapedQ).append(") ");
                for (String nameProp : genePropService.getNameProperties())
                    solrq.append("property_" + nameProp + ":(").append(escapedQ).append(") ");
                solrq.append(")");
            } else if (genePropService.getDescProperties().contains(geneQuery.getFactor())
                    || genePropService.getIdProperties().contains(geneQuery.getFactor())) {
                solrq.appendAnd();
                if (geneQuery.isNegated())
                    solrq.append(" NOT ");
                String field = "property_" + geneQuery.getFactor();
                solrq.append(field).append(":(").append(escapedQ).append(")");
            }
        }
    }

    /**
     * Appends species part of the query to SOLR query
     *
     * @param speciesConditions
     * @param solrq             solr query
     */
    private void appendSpeciesQuery(Collection<String> speciesConditions, SolrQueryBuilder solrq) {
        Set<String> species = new HashSet<String>();
        for (String s : speciesConditions)
            for (String as : getSpeciesOptions())
                if (as.toLowerCase().contains(s.toLowerCase()))
                    species.add(as);

        if (!species.isEmpty()) {
            solrq.appendAnd().append("species:(").append(EscapeUtil.escapeSolrValueList(species)).append(")");
        }
    }


    /**
     * Appends gene part of the query. Parses query conditions and appends them to SOLR query string.
     *
     * @param geneIds
     * @param solrq   solr query
     */
    private void appendGeneQuery(List<Integer> geneIds, SolrQueryBuilder solrq) {
        solrq.appendAnd();
        for (Integer geneId : geneIds) {
            solrq.append("(id:(").append(geneId).append(")").append(") ");

        }
    }

    /**
     * Returns tree of EFO/EFVs matching one specified query condition
     * EFOs are stored under "magic" factor named "efo"  at this point, they will go to EfoTree later
     * <p/>
     * This is dispatcher function calling one of specific for several query condtion cases. See the code.
     *
     * @param c condition
     * @return tree of EFVs/EFO
     * @throws SolrServerException in case of any problems with SOLR
     */
    private EfvTree<Boolean> getConditionEfvs(QueryCondition c) throws SolrServerException {
        if (c.isAnyValue())
            return getCondEfvsAllForFactor(c.getFactor());

        if (c.isAnyFactor())
            return getCondEfvsForFactor(null, c.getFactorValues());

        return getCondEfvsForFactor(c.getFactor(), c.getFactorValues());
    }

    /**
     * Returns all EFVs/EFOs for specified factor
     *
     * @param factor factor
     * @return tree of EFVs/EFO
     */
    private EfvTree<Boolean> getCondEfvsAllForFactor(String factor) {
        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();
        if (Constants.EFO_FACTOR_NAME.equals(factor)) {
            Efo efo = getEfo();
            int i = 0;
            for (String v : efo.getRootIds()) {
                condEfvs.put(Constants.EFO_FACTOR_NAME, v, true);
                if (++i >= MAX_EFV_COLUMNS) {
                    break;
                }
            }
        } else {
            int i = 0;
            for (String v : efvService.listAllValues(factor)) {
                condEfvs.put(factor, v, true);
                if (++i >= MAX_EFV_COLUMNS) {
                    break;
                }
            }
        }
        return condEfvs;
    }

    /**
     * Returns matching EFVs/EFOs for factor
     *
     * @param factor factor
     * @param values values search strings
     * @return tree of EFVs/EFO
     * @throws SolrServerException in case of any problems with SOLR
     */
    private EfvTree<Boolean> getCondEfvsForFactor(final String factor, final Iterable<String> values) throws SolrServerException {

        EfvTree<Boolean> condEfvs = new EfvTree<Boolean>();

        if (Constants.EFO_FACTOR_NAME.equals(factor) || null == factor) {
            Efo efo = getEfo();
            for (String v : values) {
                String efKey = Constants.EFO_FACTOR_NAME;
                // If v (efo id) is pre-ambled with EFO_WITH_CHILDREN_PREAMBLE, flag it in condEfvs for inclusion
                // of children by using EFO_WITH_CHILDREN_PREAMBLE as the key pointing to the EfoTerm corresponding to v
                if (v.startsWith(Constants.EFO_WITH_CHILDREN_PREAMBLE)) {
                    efKey = Constants.EFO_WITH_CHILDREN_PREAMBLE;
                    v = v.substring(Constants.EFO_WITH_CHILDREN_PREAMBLE.length());
                }
                for (EfoTerm term : efo.searchTerm(EscapeUtil.escapeSolr(v))) {
                    condEfvs.put(efKey, term.getId(), true);
                }
            }
        }

        if (Constants.EFO_FACTOR_NAME.equals(factor))
            return condEfvs;

        String queryString = EscapeUtil.escapeSolrValueList(values);
        if (factor != null)
            queryString = "(" + queryString + ") AND property:" + EscapeUtil.escapeSolr(factor);

        SolrQuery q = new SolrQuery(queryString);
        q.setRows(10000);
        q.setStart(0);
        q.setFields("*");

        QueryResponse qr = solrServerProp.query(q);

        for (SolrDocument doc : qr.getResults()) {
            String ef = (String) doc.getFieldValue("property");
            String efv = (String) doc.getFieldValue("value");
            condEfvs.put(ef, efv, true);
        }
        return condEfvs;
    }


    /**
     * This method returns a local cache to avoid re-loading bit stats for a given efo.efv term in
     * consecutive heat map rows
     *
     * @return Map: stat type -> Map: efo/efv -> Multiset<Integer> of aggregate scores for gene indexes
     */
    public Map<StatisticsType, HashMap<String, Multiset<Integer>>> getScoresCache() {

        Map<StatisticsType, HashMap<String, Multiset<Integer>>> statTypeToEfoToScores
                = new HashMap<StatisticsType, HashMap<String, Multiset<Integer>>>();
        Set<StatisticsType> statTypesToBeCached = new HashSet<StatisticsType>();

        statTypesToBeCached.add(StatisticsType.UP);
        statTypesToBeCached.add(StatisticsType.DOWN);
        statTypesToBeCached.add(StatisticsType.NON_D_E);
        for (StatisticsType statisticsType : statTypesToBeCached) {
            statTypeToEfoToScores.put(statisticsType, new HashMap<String, Multiset<Integer>>());
        }
        return statTypeToEfoToScores;
    }

    /**
     * @param scoresCache
     * @param statType
     * @param efoOrEfv
     * @return Multiset<Integer> of aggregate scores for gene indexes stored in cache under statType-> efoOrEfv
     */
    public Multiset<Integer> getScoresFromCache(
            Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache,
            StatisticsType statType,
            String efoOrEfv) {
        if (scoresCache != null)
            return scoresCache.get(statType).get(efoOrEfv);
        return null;
    }

    /**
     * @param scoresCache               - cache that stores experiment counts for geneIndexes - if it doesn't contain the required count, populate it.
     *                                  geneIndexes contains indexes of all genes of interest for the current query (including geneId)
     * @param attribute
     * @param bioEntityId
     * @param bioEntityIdRestrictionSet
     * @return experiment count for statType, efvOrEfo, geneId
     */
    private int getExperimentCountsForBioEntity(
            Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache,
            Attribute attribute,
            Integer bioEntityId,
            Set<Integer> bioEntityIdRestrictionSet) {
        Multiset<Integer> scores = getScoresFromCache(scoresCache, attribute.getStatType(), attribute.getValue());
        if (scores != null) {
            return scores.count(bioEntityId);
        }
        return atlasStatisticsQueryService.getExperimentCountsForBioEntity(attribute, bioEntityId, bioEntityIdRestrictionSet, scoresCache);

    }


    /**
     * Finds all efv attributes for which at least one gene in geneRestrictionSet has experiment counts of statisticType.
     * All found efv attributes are added to QueryState.
     * C.f. call to this method in processResultGenes().
     *
     * @param bioEntityIdRestrictionSet gene set of interest
     * @param autoFactors               list of experimental factors to be included in heatmap
     * @param qstate                    QueryState
     * @param statisticType             chosen by the user in the simple query screen (if the user has no chosen any efv/efo conditions,
     *                                  this statistic type will be used to find out scoring Attributes for that statistic type)
     */
    private void populateScoringAttributes(
            final Set<Integer> bioEntityIdRestrictionSet,
            final Collection<String> autoFactors,
            QueryState qstate,
            StatisticsType statisticType,
            boolean isFullHeatMap
    ) {
        List<Multiset.Entry<Integer>> attrCountsSortedDescByExperimentCounts =
                atlasStatisticsQueryService.getScoringAttributesForBioEntities(bioEntityIdRestrictionSet, statisticType, autoFactors);

        Multiset<Integer> efAttrCounts = HashMultiset.create();
        for (Multiset.Entry<Integer> attrCount : attrCountsSortedDescByExperimentCounts) {
            EfvAttribute attr = atlasStatisticsQueryService.getAttributeForIndex(attrCount.getElement());
            if (autoFactors.contains(attr.getEf()) && attr.getEfv() != null && !attr.getEfv().isEmpty()) {
                Integer efAttrIndex = atlasStatisticsQueryService.getIndexForAttribute(new EfvAttribute(attr.getEf(), null));
                // restrict the amount of efvs shown  for each ef to max atlasProperties.getMaxEfvsPerEfInHeatmap()
                if (isFullHeatMap || efAttrCounts.count(efAttrIndex) < atlasProperties.getMaxEfvsPerEfInHeatmap()) {
                    qstate.addEfv(attr.getEf(), attr.getEfv(), 1, QueryExpression.valueOf(statisticType.toString()));
                    efAttrCounts.add(efAttrIndex);
                }
            }
        }
    }

    /**
     * @param docs
     * @return Set of gene ids from list of Solr document retrieved from the gene Solr index.
     *         This set will serve as a gene restriction list for any subsequent StatisticsStorage queries.
     */
    private Set<Integer> getBioEntityIdRestrictionSet(SolrDocumentList docs) {
        Set<Integer> bioEntityRestrictionSet = new HashSet<Integer>();
        for (SolrDocument doc : docs) {
            Object idObj = doc.getFieldValue("id");
            if (idObj != null) {
                bioEntityRestrictionSet.add((Integer) idObj);
            }
        }
        return bioEntityRestrictionSet;
    }

    /**
     * @param scoresCache
     * @param attribute
     * @param bioEntityId
     * @param bioEntityIdRestrictionSet
     * @param showNonDEData
     * @param usePvalsInHeatmapOrdering
     * @return get up/dn/nonde stats for geneId, efo/refv attribute; restrict bitstats query to geneRestrictionSet only
     */
    public UpdownCounter getStats(
            Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache,
            Attribute attribute,
            Integer bioEntityId,
            Set<Integer> bioEntityIdRestrictionSet,
            boolean showNonDEData,
            boolean usePvalsInHeatmapOrdering
    ) {
        attribute.setStatType(StatisticsType.UP);
        int upCnt = getExperimentCountsForBioEntity(scoresCache, attribute, bioEntityId, bioEntityIdRestrictionSet);
        attribute.setStatType(StatisticsType.DOWN);
        int downCnt = getExperimentCountsForBioEntity(scoresCache, attribute, bioEntityId, bioEntityIdRestrictionSet);
        int nonDECnt = 0;
        if (showNonDEData) {
            attribute.setStatType(StatisticsType.NON_D_E);
            nonDECnt = getExperimentCountsForBioEntity(scoresCache, attribute, bioEntityId, bioEntityIdRestrictionSet);
        }

        float minPValUp = 0;
        float minPValDown = 0;

        if (usePvalsInHeatmapOrdering) {

            minPValUp = 1;
            minPValDown = 1;
            long start = System.currentTimeMillis();
            if (upCnt > 0) {
                // Get best up pValue
                attribute.setStatType(StatisticsType.UP);
                List<ExperimentInfo> bestUpExperimentsForAttribute = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioEntityId, attribute, 0, 1);
                if (bestUpExperimentsForAttribute.isEmpty()) {
                    throw logUnexpected("Failed to retrieve best UP experiment for geneId: " + bioEntityId + "); attr: " + attribute + " despite the UP count: " + upCnt);
                }
                minPValUp = bestUpExperimentsForAttribute.get(0).getpValTStatRank().getPValue();
            }

            if (downCnt > 0) {
                // Get best down pValue
                attribute.setStatType(StatisticsType.DOWN);
                List<ExperimentInfo> bestDownExperimentsForAttribute = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioEntityId, attribute, 0, 1);
                if (bestDownExperimentsForAttribute.isEmpty()) {
                    throw logUnexpected("Failed to retrieve best DOWN experiment for geneId: " + bioEntityId + "; attr: " + attribute + " despite the DOWN count: " + downCnt);
                }
                minPValDown = bestDownExperimentsForAttribute.get(0).getpValTStatRank().getPValue();
            }

            if (minPValUp != 1 || minPValDown != 1)
                log.debug("Retrieved best UP & DOWN pVals: (" + minPValUp + " : " + minPValDown + ") for geneId: " + bioEntityId + "; attr: " + attribute +
                        "' in: " + (System.currentTimeMillis() - start) + " ms");
        }
        return new UpdownCounter(
                upCnt,
                downCnt,
                nonDECnt,
                minPValUp,
                minPValDown);
    }

    /**
     * Processes SOLR query response and generates Atlas structured query result
     *
     * @param response                  SOLR response
     * @param result                    ATlas result
     * @param qstate                    query state
     * @param query                     query itself
     * @param numOfResults
     * @param statisticsQuery           specified in user's query (if the user has not chosen any efv/efo conditions,
     *                                  the statistics type in this query will be used to find out scoring Attributes for that statistic type)
     * @param usePvalsInHeatmapOrdering if true, retrieve pval/tstats from bit index; otherwise don't.
     * @throws SolrServerException
     */
    private void processResultGenes(QueryResponse response,
                                    AtlasStructuredQueryResult result,
                                    QueryState qstate,
                                    AtlasStructuredQuery query,
                                    Integer numOfResults,
                                    StatisticsQueryCondition statisticsQuery,
                                    boolean usePvalsInHeatmapOrdering
    ) throws SolrServerException {

        // Note that this method processes results from the query assembled from an already sorted list of
        // gene id () got from an earlier atlasStatisticsQueryService.getSortedGenes() call). However, by default Solr
        // returns its results in the order specified by its notion of relevancy, and I don't know of any way of
        // forcing Solr to return results in the order of explicit field values in the query.
        // In effect, the earlier sorting itself is lost, though it still has a crucial function of identifying
        // the required chunk from the overall sorted list of genes for the user's query.
        // To re-instate the sorting we enter the newly constructed StructuredResultRows into the TreeSet below (ordered
        // according to compareTo() method in StructuredResultRow)
        SortedSet<StructuredResultRow> structuredResultRows = new TreeSet<StructuredResultRow>();

        // The list below stores list of StructuredResultRow's until all the (efo and efv) counters have been added
        // to each row. Only after that is this list sorted by the cumulative experiment counts in each row.
        List<StructuredResultRow> unsortedHeatmapRows = new ArrayList<StructuredResultRow>();

        // Initialise scores cache to store efo counts for the group of genes of interest to this query.
        // For each heat map row other than the first, the cache will be hit instead of AtlasStatisticsQueryService
        Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache = getScoresCache();

        SolrDocumentList docs = response.getResults();

        EfvTree<ColumnInfo> resultEfvs = new EfvTree<ColumnInfo>();
        EfoTree<ColumnInfo> resultEfos = qstate.getEfos();

        Iterable<EfvTree.EfEfv<ColumnInfo>> efvList = qstate.getEfvs().getValueSortedList();
        Iterable<EfoTree.EfoItem<ColumnInfo>> efoList = qstate.getEfos().getValueOrderedList();
        boolean hasQueryEfoEfvs = qstate.hasQueryEfoEfvs();

        // heatmap column numberer
        Maker<ColumnInfo> numberer = new Maker<ColumnInfo>() {
            private int num = 0;

            public ColumnInfo make() {
                return new BaseColumnInfo(num++);
            }
        };

        Collection<String> autoFactors;
        if ((!query.getConditions().isEmpty() && !query.getConditions().iterator().next().isAnything())
                || query.isFullHeatmap()) {
            autoFactors = efvService.getAllFactors();
        } else {
            // If the user hasn't specified any conditions or query.isFullHeatmap() is false (the default for heatmap),
            // choose only 'usual factors of interest' - as shown in GXA DAS source
            autoFactors = atlasProperties.getDasFactors();
        }

        // timing collection variables
        long overallBitStatsProcessingTime = 0;
        long overallNcdfAccessTimeForListView = 0;
        long overallBitStatsProcessingTimeForListView = 0;

        // Retrieve from docs the gene restriction list to be used in subsequent StatisticsStorage queries.
        Set<Integer> bioEntityIdRestrictionSet = getBioEntityIdRestrictionSet(docs);

        if (!hasQueryEfoEfvs) {
            long timeStart = System.currentTimeMillis();
            populateScoringAttributes(bioEntityIdRestrictionSet, autoFactors, qstate, statisticsQuery.getStatisticsType(), query.isFullHeatmap());
            long diff = System.currentTimeMillis() - timeStart;
            overallBitStatsProcessingTime += diff;
            List<EfvTree.EfEfv<ColumnInfo>> scoringEfvs = qstate.getEfvs().getValueSortedList();
            log.info("User query contained no efv/efo conditions; collected " + scoringEfvs.size() + " scoring efv conditions in " + diff + " ms");
            efvList = scoringEfvs;
        }

        // This map stores HeatMapColumn object for each EfvTree.EfEfv processed in the loop below.
        // Note that HeatMapColumn contains a list of counters that is a vertical slice through heatmap, specific
        // to that EfvTree.EfEfv. HeatMapColumn objects wll be used to sort efv heatmap columns by their cumulative
        // experiment counts before adding appropriate counters to the horizontal slices (per gene) displayed in heatmap,
        // i.e. StructuredResultRow's in unsortedHeatmapRows.
        Map<EfvTree.EfEfv<ColumnInfo>, HeatMapColumn> efvToColumn = new HashMap<EfvTree.EfEfv<ColumnInfo>, HeatMapColumn>();

        log.debug("Processing " + numOfResults + " result bioentities...");
        result.setTotal(numOfResults);
        int added = 0;
        for (SolrDocument doc : docs) {
            long hmRowStart = System.currentTimeMillis();
            long overallBitStatsProcessingTimeForHeatMapRow = 0;
            Object idObj = doc.getFieldValue("id");
            if (idObj == null) {
                continue;
            }
            Integer bioEntityId = (Integer) idObj;

            AtlasGene gene = new AtlasGene(doc);
            if (response.getHighlighting() != null)
                gene.setGeneHighlights(response.getHighlighting().get(bioEntityId.toString()));

            // counters will contain a list of heatmap cell payloads
            List<UpdownCounter> efoCounters = new ArrayList<UpdownCounter>() {
                @Override
                public UpdownCounter get(int index) {
                    if (index < size())
                        return super.get(index);
                    else
                        return new UpdownCounter(0, 0, 0, 0, 0);
                }
            };
            // This variable will determine if a StructuredResultRow will be displayed;
            // it is set to true of at least one cell has counts greater than minExperiments - used in StructuredResultRow constructor.
            boolean rowQualifies = false;

            // Now calculate up/dn/nonde counts for all efvs/efos for the current gene
            Iterator<EfvTree.EfEfv<ColumnInfo>> itEfv = efvList.iterator();
            Iterator<EfoTree.EfoItem<ColumnInfo>> itEfo = efoList.iterator();
            // attrToCounter is used to construct list view and stores mapping between attributes derived from processed efo terms and
            // their corresponding statistics counters
            Map<EfvAttribute, UpdownCounter> attrToCounter = new HashMap<EfvAttribute, UpdownCounter>();
            EfvTree.EfEfv<ColumnInfo> efEfv = null;
            EfoTree.EfoItem<ColumnInfo> efoItem = null;

            while (itEfv.hasNext() || itEfo.hasNext() || efEfv != null || efoItem != null) {

                if (itEfv.hasNext() && efEfv == null) {
                    efEfv = itEfv.next();
                }
                if (itEfo.hasNext() && efoItem == null) {
                    efoItem = itEfo.next();
                }

                UpdownCounter counter;
                boolean usingEfv = efoItem == null || (efEfv != null && efEfv.getPayload().compareTo(efoItem.getPayload()) < 0);

                if (usingEfv) {
                    String ef = efEfv.getEf();
                    String efv = efEfv.getEfv();

                    EfvAttribute attr = new EfvAttribute(ef, efv, null);

                    if (!attrToCounter.containsKey(attr)) {
                        // 1. In the list view: the above test prevents querying bit index for the same attribute more then once,
                        // e.g. when an efo is also processed (and thus broken down into efvs it maps to - c.f. below)
                        // for the current gene that maps to that attribute
                        // 2. In the heatmap view, the use of attrToCounter is not essential, but it innocuous
                        long timeStart = System.currentTimeMillis();
                        counter = getStats(scoresCache, attr, bioEntityId, bioEntityIdRestrictionSet, ((QueryColumnInfo) efEfv.getPayload()).displayNonDECounts(), usePvalsInHeatmapOrdering);
                        long diff = System.currentTimeMillis() - timeStart;
                        overallBitStatsProcessingTime += diff;
                        overallBitStatsProcessingTimeForHeatMapRow += diff;

                        attrToCounter.put(attr, counter);
                    }

                    if (!efvToColumn.containsKey(efEfv)) {
                        efvToColumn.put(efEfv, new HeatMapColumn(efEfv));
                    }
                    efvToColumn.get(efEfv).addRowCounter(attrToCounter.get(attr));

                    if (efEfv.getPayload().isQualified(attrToCounter.get(attr))) {
                        rowQualifies = true;
                        efvToColumn.get(efEfv).setQualifies(true);
                    }

                    efEfv = null;
                } else {
                    String efoTerm = efoItem.getId();

                    if (query.getViewType() == ViewType.LIST) { // efo's in list view
                        Set<EfvAttribute> attrsForEfo = atlasStatisticsQueryService.getAttributesForEfo(efoTerm);
                        long timeStart = System.currentTimeMillis();

                        for (EfvAttribute attr : attrsForEfo) {
                            if (!attrToCounter.containsKey(attr)) {
                                // the above test prevents querying bit index for the same attribute more then once  - if more
                                // than one efo processed here maps to that attribute (e.g. an efo's term and its parent)
                                counter = getStats(scoresCache, attr, bioEntityId, bioEntityIdRestrictionSet, ((QueryColumnInfo) efoItem.getPayload()).displayNonDECounts(), usePvalsInHeatmapOrdering);
                                if (efoItem.getPayload().isQualified(counter)) {
                                    rowQualifies = true;
                                    attrToCounter.put(attr, counter);
                                }
                            }
                        }
                        long diff = System.currentTimeMillis() - timeStart;
                        overallBitStatsProcessingTime += diff;
                        overallBitStatsProcessingTimeForListView += diff;
                    } else { // efo's in heatmap
                        // Get statistics for efoTerm-gene
                        long timeStart = System.currentTimeMillis();
                        // third param is not important below in getStats() - as we get counts for all stat types anyway
                        Attribute attr = new EfoAttribute(efoTerm, null);
                        counter = getStats(scoresCache, attr, bioEntityId, bioEntityIdRestrictionSet, ((QueryColumnInfo) efoItem.getPayload()).displayNonDECounts(), usePvalsInHeatmapOrdering);
                        long diff = System.currentTimeMillis() - timeStart;
                        overallBitStatsProcessingTime += diff;
                        overallBitStatsProcessingTimeForHeatMapRow += diff;

                        if (!resultEfos.getPayload(efoTerm).isPositionSet()) {
                            // If the final heatmap column position has not yet been set (e.g. while processing
                            // a previous gene in the main loop), set it now
                            resultEfos.setPayload(efoTerm, numberer.make());
                        }
                        // Accumulate efo counters
                        efoCounters.add(counter);

                        if (efoItem.getPayload().isQualified(counter)) {
                            rowQualifies = true;
                            // Mark efo for displaying in heatmap it the experiment counts in this cell qualify it
                            resultEfos.mark(efoItem.getId(), !INCLUDE_EFO_PARENTS_IN_HEATMAP);
                            // Tag efoItem as non-expandable in heatmap header - if applicable
                            resultEfos.setNonExpandableIfApplicable(efoItem.getId(), qstate.getScoringEfos());
                        } else {
                            log.debug("Rejecting " + efoItem.getId() + " for bioentity " + bioEntityId + " as score does not satisfy min experiments condition");
                        }
                    }

                    efoItem = null;
                }
            }
            // Store a Structured row (with just efo counters in it for now) in unsortedHeatmapRows. Efv counters will be added
            // below once the efv columns have been sorted by their cumulative experiment counts.
            /** TODO
             * In advanced (though never simple) i/f queries, heatmap columns will correspond to ef-efv with different expressions in the user's query.
             * For example, in the case of 'UP in heart and DOWN in lung' query, the required expression for a group of heart-related efv columns
             * will be UP, and the required expression for another group of lung-related columns will be DOWN.
             * In such cases, ef-efv-specific StatisticsType(s) should be passed to StructuredResultRow constructor. Then the sorting of unsortedHeatmapRows
             * in the final heatmap would truly reflect the user's query. As things stand now, a simplification has been made that only
             * the first clause's stat type (stored in statisticsQuery.getStatisticsType()) is passed to StructuredResultRow constructor. Consequently,
             * unsortedHeatmapRows are currently sorted by the aggregate counts corresponding to statistics type of the first AND clause only.
             */
            unsortedHeatmapRows.add(new StructuredResultRow(gene, efoCounters, rowQualifies, statisticsQuery.getStatisticsType()));

            // Now process for list view all attributes in attrToCounter (mapped to by efo's processed above)
            if (query.getViewType() == ViewType.LIST) {
                for (Map.Entry<EfvAttribute, UpdownCounter> entry : attrToCounter.entrySet()) {
                    final EfvAttribute attribute = entry.getKey();
                    boolean displayNonDECounts = entry.getValue().getNones() > 0;
                    Pair<Long, Long> queryTimes = loadListExperiments(result, gene, attribute.getEf(), attribute.getEfv(), entry.getValue(), qstate.getExperiments(), displayNonDECounts);
                    overallBitStatsProcessingTime += queryTimes.getFirst();
                    overallBitStatsProcessingTimeForListView += queryTimes.getFirst();
                    overallNcdfAccessTimeForListView += queryTimes.getSecond();
                }
            }
            log.debug("Processed gene: " + gene.getGeneName() + " in: " + (System.currentTimeMillis() - hmRowStart) + "; bit stats time: " + overallBitStatsProcessingTimeForHeatMapRow);
        }

        // So far we accumulated rows of counters for all efos into unsortedHeatmapRows
        // We have also accumulated Efv columns date in efvToColumn
        // What we need to do now is the following:
        // 1. Eliminate columns that don't qualify to be displayed in heatmap
        // 2. Sort HeatMapColumns in efvToColumn.values() according to the cumulative column counter and
        // 3. Transfer efv counters to rows in unsortedHeatmapRows and transfer efvs to resultEfvs, now with the correct sorted
        // column positions as payloads

        // Remove non-qualifying columns
        Collection<HeatMapColumn> qualifyingColumns = Collections2.filter(efvToColumn.values(),
                new Predicate<HeatMapColumn>() {
                    public boolean apply(HeatMapColumn col) {
                        return col.qualifies();
                    }
                });

        List<HeatMapColumn> efvColumns = new ArrayList<HeatMapColumn>(qualifyingColumns);
        // Sort efv columns by the their cumulative experiment counts in each column
        Collections.sort(efvColumns);

        // Transfer efv counters to rows in unsortedHeatmapRows and transfer efvs to resultEfvs, now with the correct sorted
        // column positions as payloads.
        for (HeatMapColumn hmColumn : efvColumns) {
            EfvTree.EfEfv<ColumnInfo> efEfv = hmColumn.getEfEfv();
            efEfv.setPayload(numberer.make()); // Set position for each efv column after the above sort

            int row = 0;
            for (StructuredResultRow structuredRow : unsortedHeatmapRows) {
                UpdownCounter counter = hmColumn.getRowCounter(row++);
                structuredRow.addCounter(counter);
                if (efEfv.getPayload().isQualified(counter)) {
                    resultEfvs.put(efEfv);
                }
            }
        }

        // Now that unsortedHeatmapRows has both efo and efv data, where:
        // efo columns are sorted by the order in which they were entered into QueryState
        // efv columns are sorted by their cumulative column experiment counts (efv's with highest experiment counts moving to the left)
        // - we can transfer unsortedHeatmapRows to structuredResultRows. Because the latter is a TreeSet, heatmap rows
        // will now be sorted according to their aggregated row counts, with genes with highest experiment counts moving
        // to the top of the heatmap
        structuredResultRows.addAll(unsortedHeatmapRows);
        log.debug("structuredResultRows.size() = " + structuredResultRows.size() + "; added = " + added);


        // Returned results sorted by geneScore, eliminating that had zero qualifying score (i.e. all the scores added for
        // all efvs where the counts were >= min experiments)
        for (StructuredResultRow row : structuredResultRows) {
            if (row.qualifies()) {
                result.addResult(row);
            } else {
                log.info("Excluding from heatmap row for gene: " + row.getGene().getGeneName());
            }
        }

        log.info("Overall bitstats processing time: " + overallBitStatsProcessingTime + " ms");
        if (query.getViewType() == ViewType.LIST) {
            log.info("Overall listview-related bitstats processing time: " + overallBitStatsProcessingTimeForListView + " ms");
            log.info("Overall listview-related ncdf querying time: " + overallNcdfAccessTimeForListView + " ms");
        }

        result.setResultEfvs(resultEfvs);
        result.setResultEfos(resultEfos);

        log.info("Retrieved query completely: " + result.getSize() + " records of " +
                result.getTotal() + " total starting from " + result.getStart());

        log.debug("Resulting EFVs are: " + resultEfvs.getNameSortedList().size());
        log.debug("Resulting EFOs are: " + resultEfos.getMarkedSubTreeList().size());

    }

    /**
     * Loads experiments data for list view, where each list row corresponds to a single gene-ef-efv combination and each gene
     * can have at most result.getRowsPerGene() list rows
     *
     * @param result      atlas result
     * @param gene        gene id
     * @param ef          ef
     * @param efv         efv
     * @param counter     up/down/nonde expression experiment counts
     * @param experiments query experiments
     * @return Pair of total times spent on index and ncdf queries respectively
     */

    private Pair<Long, Long> loadListExperiments(
            AtlasStructuredQueryResult result,
            final AtlasGene gene,
            final String ef,
            final String efv,
            final UpdownCounter counter,
            Set<Long> experiments,
            boolean showNonDEData) {

        long totalBitIndexQueryTime = 0;
        long totalNcdfQueryTime = 0;

        long start = System.currentTimeMillis();
        // Retrieve experiments in which geneId-ef-efv have UP or DOWN expression
        EfvAttribute attr = new EfvAttribute(ef, efv, StatisticsType.UP_DOWN);
        Set<ExperimentInfo> scoringExps =
                atlasStatisticsQueryService.getScoringExperimentsForBioEntityAndAttribute(gene.getGeneId(), attr);
        totalBitIndexQueryTime += System.currentTimeMillis() - start;

        Long designElementId = null;
        List<ListResultRowExperiment> experimentsForRow = new ArrayList<ListResultRowExperiment>();

        // Store minimum up/down pValues for across all scoring experiments
        float pup = 1, pdn = 1;

        for (ExperimentInfo exp : scoringExps) {
            if ((!experiments.isEmpty() && !experiments.contains(exp.getExperimentId())) ||
                    // We currently allow up to result.getRowsPerGene() list view rows per gene (where each list row corresponds to a single ef-efv)
                    result.getNumberOfListResultsForGene(gene) > result.getRowsPerGene())
                continue;
            // Get AtlasExperiment to get experiment description, needed in list view
            // TODO: we use bot experimentSolrDAO and underlying Solr server in this class.
            // That means we're using two different levels of abstraction in the same class
            // That means we're not structuring out application properly
            AtlasExperiment aexp = experimentSolrDAO.getExperimentById(exp.getExperimentId());
            if (aexp == null)
                continue;

            List<ExpressionAnalysis> upDnEAs = new ArrayList<ExpressionAnalysis>();
            boolean isUp = true;
            // Note that it is possible for the same geneId-ef-efv to be both up and down in the same experiment (and proxy) - in two
            // different design elements
            if (counter.getUps() > 0) {
                start = System.currentTimeMillis();
                ExpressionAnalysis ea = atlasNetCDFDAO.getBestEAForGeneEfEfvInExperiment(exp.getAccession(), (long) gene.getGeneId(), ef, efv, Expression.UP);
                totalNcdfQueryTime += System.currentTimeMillis() - start;
                if (ea != null) {
                    upDnEAs.add(ea);
                }
            }
            if (counter.getDowns() > 0) {
                start = System.currentTimeMillis();
                ExpressionAnalysis ea = atlasNetCDFDAO.getBestEAForGeneEfEfvInExperiment(exp.getAccession(), (long) gene.getGeneId(), ef, efv, Expression.DOWN);
                totalNcdfQueryTime += System.currentTimeMillis() - start;
                if (ea != null) {
                    upDnEAs.add(ea);
                }
            }
            // Assemble experiment rows for the ListResultRow corresponding to geneId-ef-efv
            for (ExpressionAnalysis ea : upDnEAs) {
                if (designElementId == null) {
                    designElementId = ea.getDesignElementID();
                }

                if (ea.isUp()) {
                    pup = Math.min(pup, ea.getPValAdjusted());
                } else if (ea.isDown()) {
                    pdn = Math.min(pdn, ea.getPValAdjusted());
                }

                ListResultRowExperiment experiment = new ListResultRowExperiment(
                        exp.getExperimentId(),
                        exp.getAccession(),
                        aexp.getDescription(),
                        ea.getPValAdjusted(),
                        ea.isUp() ? Expression.UP : Expression.DOWN);

                experimentsForRow.add(experiment);
            }
        }

        if (showNonDEData) {
            // Now retrieve experiments in which geneId-ef-efv have NON_D_E expression
            attr.setStatType(StatisticsType.NON_D_E);
            scoringExps = atlasStatisticsQueryService.getScoringExperimentsForBioEntityAndAttribute(gene.getGeneId(), attr);
            for (ExperimentInfo exp : scoringExps) {
                if ((!experiments.isEmpty() && !experiments.contains(exp.getExperimentId())) ||
                        // We currently allow up to result.getRowsPerGene() list view rows per gene (where each list row corresponds to a single ef-efv)
                        result.getNumberOfListResultsForGene(gene) > result.getRowsPerGene())
                    continue;
                // Get AtlasExperiment to get experiment description, needed in list view
                // TODO: we use bot experimentSolrDAO and underlying Solr server in this class.
                // That means we're using two different levels of abstraction in the same class
                // That means we're not structuring out application properly
                AtlasExperiment aexp = experimentSolrDAO.getExperimentById(exp.getExperimentId());
                if (aexp == null)
                    continue;

                start = System.currentTimeMillis();
                ExpressionAnalysis ea = atlasNetCDFDAO.getBestEAForGeneEfEfvInExperiment(exp.getAccession(), (long) gene.getGeneId(), ef, efv, Expression.NONDE);
                totalNcdfQueryTime += System.currentTimeMillis() - start;
                if (ea != null) {
                    ListResultRowExperiment experiment = new ListResultRowExperiment(
                            exp.getExperimentId(),
                            exp.getAccession(),
                            aexp.getDescription(),
                            // This is just a placeholder as pValues for nonDE expressions are currently (not available here
                            // and therefore) not displayed in experiment pop-ups off the list view
                            ea.getPValAdjusted(),
                            Expression.NONDE);
                    experimentsForRow.add(experiment);
                }
            }
        }

        // if more than experiment rows were created, sort the list by pValue (in asc order)
        if (experimentsForRow.size() > 1) {
            Collections.sort(experimentsForRow, new Comparator<ListResultRowExperiment>() {
                public int compare(ListResultRowExperiment o1, ListResultRowExperiment o2) {
                    return Float.valueOf(o1.getPvalue()).compareTo(o2.getPvalue());
                }
            });
        }
        // If at least one experiment row was created add to result ListResultRow corresponding to geneId-ef-efv
        if (experimentsForRow.size() > 0) {
            ListResultRow row = new ListResultRow(ef, efv, counter.getUps(), counter.getDowns(), counter.getNones(), pup, pdn, designElementId);
            row.setGene(gene);
            row.setExp_list(experimentsForRow);
            result.addListResult(row);
        }

        // Return timings to be logged later
        return Pair.create(totalBitIndexQueryTime, totalNcdfQueryTime);
    }

    /**
     * Creates SOLR query from atlas query
     *
     * @param rowsPerPage
     * @param qstate      query state
     * @return solr query object
     */
    private SolrQuery setupSolrQuery(Integer rowsPerPage, QueryState qstate) {
        SolrQuery q = new SolrQuery(qstate.getSolrq().toString());

        q.setRows(rowsPerPage);
        q.setFacet(true);

        int max = 0;
        q.addField("score");
        q.addField("id");
        q.addField("name");
        q.addField("identifier");
        q.addField("species");
        for (String p : genePropService.getIdNameDescProperties())
            q.addField("property_" + p);
        q.setFacetLimit(5 + max);
        q.setFacetMinCount(2);

        for (String p : genePropService.getDrilldownProperties()) {
            q.addFacetField("property_f_" + p);
        }

        q.addFacetField("species");

        q.setHighlight(true);
        q.setHighlightSnippets(100);
        q.setParam("hl.usePhraseHighlighter", "true");
        q.setParam("hl.mergeContiguous", "true");
        q.setHighlightRequireFieldMatch(true);
        q.addHighlightField("id");
        q.addHighlightField("name");
        q.addHighlightField("synonym");
        q.addHighlightField("identifier");
        for (String p : genePropService.getIdNameDescProperties())
            q.addHighlightField("property_" + p);
        log.debug("Expanded query: " + q.toString());
        return q;
    }

    /**
     * Retrieves gene facets from SOLR response
     *
     * @param response solr response
     * @param name     field name to exptract
     * @param values   query values (to clear off the facet)
     * @return iterable collection of facet values with counters
     */
    private Iterable<FacetCounter> getGeneFacet(QueryResponse response, final String name, Set<String> values) {
        List<FacetCounter> facet = new ArrayList<FacetCounter>();
        FacetField ff = response.getFacetField(name);
        if (ff == null || ff.getValueCount() < 2 || ff.getValues() == null)
            return new ArrayList<FacetCounter>();

        for (FacetField.Count ffc : ff.getValues())
            if (!values.contains(ffc.getName()))
                facet.add(new FacetCounter(ffc.getName(), (int) ffc.getCount()));
        if (facet.size() < 2)
            return new ArrayList<FacetCounter>();

        Collections.sort(facet);
        return facet.subList(0, Math.min(facet.size(), 5));

    }

    /**
     * Returns set of experimental factor for drop-down, fileterd by config
     *
     * @return set of strings representing experimental factors
     */
    public Collection<String> getExperimentalFactorOptions() {
        List<String> factors = new ArrayList<String>();
        factors.addAll(efvService.getOptionsFactors());
        Collections.sort(factors, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return atlasProperties.getCuratedEf(o1).compareToIgnoreCase(atlasProperties.getCuratedGeneProperty(o2));
            }
        });
        return factors;
    }

    /**
     * Returns list of available gene property options sorted by curated value
     *
     * @return list of strings
     */
    public List<String> getGenePropertyOptions() {
        List<String> result = new ArrayList<String>();
        for (String v : genePropService.getIdNameDescProperties())
            result.add(v);
        result.add(Constants.GENE_PROPERTY_NAME);
        Collections.sort(result, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return atlasProperties.getCuratedGeneProperty(o1).compareToIgnoreCase(atlasProperties.getCuratedGeneProperty(o2));
            }
        });
        return result;
    }

    /**
     * Returns list of available species
     *
     * @return list of species strings
     */
    public SortedSet<String> getSpeciesOptions() {
        if (allSpecies.isEmpty()) {
            SolrQuery q = new SolrQuery("*:*");
            q.setRows(0);
            q.addFacetField("species");
            q.setFacet(true);
            q.setFacetLimit(-1);
            q.setFacetMinCount(1);
            q.setFacetSort(FacetParams.FACET_SORT_COUNT);
            try {
                QueryResponse qr = solrServerAtlas.query(q);
                if (qr.getFacetFields().get(0).getValues() != null) {
                    for (FacetField.Count ffc : qr.getFacetFields().get(0).getValues()) {
                        allSpecies.add(ffc.getName());
                    }
                }
            } catch (SolrServerException e) {
                throw logUnexpected("Can't fetch all factors", e);
            }
        }
        return allSpecies;
    }

    /**
     * Index rebuild notification handler
     */
    public void onIndexBuildFinish() {
        allSpecies.clear();
    }

    public void onIndexBuildStart() {

    }

    /**
     * Destructor called by Spring
     *
     * @throws Exception
     */
    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}
