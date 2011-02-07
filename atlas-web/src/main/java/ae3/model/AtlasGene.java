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

package ae3.model;

import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.UpdownCounter;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.index.GeneExpressionAnalyticsTable;
import uk.ac.ebi.gxa.statistics.Attribute;
import uk.ac.ebi.gxa.statistics.Experiment;
import uk.ac.ebi.gxa.statistics.StatisticsType;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static uk.ac.ebi.gxa.statistics.StatisticsType.UP_DOWN;
import static uk.ac.ebi.gxa.utils.EscapeUtil.nullzero;

/**
 * View class for Atlas gene SOLR document
 */
public class AtlasGene {
    private SolrDocument geneSolrDocument;
    private Map<String, List<String>> geneHighlights;
    private GeneExpressionAnalyticsTable expTable;
    private static final String PROPERTY_PREFIX = "property_";

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Local cache preventing duplicate access to bit index
    // NB. Under a null key a heatmap across all ef's is stored
    private Map<String, EfvTree<UpdownCounter>> efToHeatmapCache = new HashMap<String, EfvTree<UpdownCounter>>();

    /**
     * Constructor
     *
     * @param geneDoc SOLR document for the gene
     */
    public AtlasGene(SolrDocument geneDoc) {
        this.geneSolrDocument = geneDoc;
    }

    /**
     * Safe value getter correctly dealing with multiple values
     *
     * @param name      field name
     * @param separator
     * @return string value or empty string if not found
     */
    private String getValue(String name, String separator) {
        Collection fval = geneSolrDocument.getFieldValues(name);
        return fval == null ? "" : StringUtils.join(fval, separator);
    }

    /**
     * Safe value getter correctly dealing with multiple values
     *
     * @param name field name
     * @return string value or empty string if not found
     */
    private String getValue(String name) {
        return getValue(name, ", ");
    }

    /**
     * Safely gets collection of field values
     *
     * @param name field name
     * @return collection (maybe empty but never null)
     */
    @SuppressWarnings("unchecked")
    private Collection<String> getValues(String name) {
        Collection<Object> r = geneSolrDocument.getFieldValues(name);
        return r == null ? Collections.EMPTY_LIST : r;
    }

    /**
     * Safely gets highlighted string value of the field
     *
     * @param name
     * @return highlighted HTML value of the field or empty string if not found
     */
    private String getHilitValue(String name) {
        List<String> val = geneHighlights.get(name);
        if (val == null || val.size() == 0)
            return StringEscapeUtils.escapeHtml(getValue(name));
        return StringUtils.join(val, ", ");
    }

    /**
     * Set highlights map from SOLR QueryResponse to use for field value highlighting
     *
     * @param geneHighlights highlighting map
     */
    public void setGeneHighlights(Map<String, List<String>> geneHighlights) {
        this.geneHighlights = geneHighlights;
    }

    /**
     * Returns document object
     *
     * @return SOLR document object
     */
    public SolrDocument getGeneSolrDocument() {
        return geneSolrDocument;
    }

    /**
     * Returns map of gene property values
     *
     * @return map of gene property values
     */
    public Map<String, Collection<String>> getGeneProperties() {
        return new LazyMap<String, Collection<String>>() {
            protected Collection<String> map(String key) {
                return getValues(PROPERTY_PREFIX + key);
            }

            protected Iterator<String> keys() {
                return getGenePropertiesIterator();
            }
        };
    }

    /**
     * Returns map of highlighted gene property values
     *
     * @return map of highlighted gene property values
     */
    public Map<String, String> getHilitGeneProperties() {
        return new LazyMap<String, String>() {
            protected String map(String key) {
                return getHilitValue(PROPERTY_PREFIX + key);
            }

            protected Iterator<String> keys() {
                return getGenePropertiesIterator();
            }
        };
    }

    /**
     * Returns iterator of all available gene properties
     *
     * @return iterator of all available gene properties
     */
    public Iterator<String> getGenePropertiesIterator() {
        return Iterators.transform(
                Iterators.filter(
                        geneSolrDocument.getFieldNames().iterator(),
                        new Predicate<String>() {
                            public boolean apply(@Nullable String input) {
                                return input != null && input.startsWith(PROPERTY_PREFIX);
                            }
                        }),
                new Function<String, String>() {
                    public String apply(@Nonnull String input) {
                        return input.substring(PROPERTY_PREFIX.length());
                    }
                });
    }

    /**
     * Returns internal numeric gene ID
     *
     * @return internal numeric gene ID (in string apparently)
     */
    public long getGeneId() {
        return Long.parseLong(getValue("id"));
    }

    /**
     * Returns highlighted gene property value
     *
     * @param property property name
     * @return highlighted gene property value
     */
    public String getHilitPropertyValue(String property) {
        return getHilitValue(PROPERTY_PREFIX + property);
    }

    /**
     * Returns string gene propery value
     *
     * @param property property name
     * @return string property value
     */
    public String getPropertyValue(String property) {
        return getValue(PROPERTY_PREFIX + property);
    }

    /**
     * Returns string gene propery value
     *
     * @param property  property name
     * @param separator
     * @return string property value
     */
    public String getPropertyValue(String property, String separator) {
        return getValue(PROPERTY_PREFIX + property, separator);
    }

    /**
     * Returns collection of property values
     *
     * @param property property name
     * @return collection of property values
     */
    public Collection<String> getPropertyValues(String property) {
        return getValues(PROPERTY_PREFIX + property);
    }

    /**
     * Returns gene name
     *
     * @return gene name
     */
    public String getGeneName() {
        return getValue("name");
    }

    /**
     * Returns highlighted gene name
     *
     * @return highlighted gene name
     */
    public String getHilitGeneName() {
        return getHilitValue("name");
    }

    /**
     * Returns primary gene identifier
     *
     * @return gene identifier
     */
    public String getGeneIdentifier() {
        return getValue("identifier");
    }

    /**
     * Returns gene organism
     *
     * @return organism
     */
    public String getGeneSpecies() {
        return StringUtil.upcaseFirst(getValue("species"));
    }

    /**
     * Returns collection of ortholog identifiers
     *
     * @return collection of ortholog identifiers
     */
    public Collection<String> getOrthologs() {
        return getValues("orthologs");
    }

    /**
     * Returns number of experiments, where gene is UP expressed in EFO accession
     *
     * @param efo accession
     * @return number
     */
    public int getCount_up(String efo) {
        return nullzero((Number) geneSolrDocument.getFieldValue("cnt_efo_" + EscapeUtil.encode(efo) + "_up"));
    }

    /**
     * Returns number of experiments, where gene is DOWN expressed in EFO accession
     *
     * @param efo accession
     * @return number
     */
    public int getCount_dn(String efo) {
        return nullzero((Number) geneSolrDocument.getFieldValue("cnt_efo_" + EscapeUtil.encode(efo) + "_dn"));
    }

    /**
     * Returns number of experiments gene studied in
     *
     * @param atlasStatisticsQueryService
     * @return number
     */
    public int getNumberOfExperiments(@Nonnull AtlasStatisticsQueryService atlasStatisticsQueryService) {
        return getExperimentIds(atlasStatisticsQueryService).size();
    }

    /**
     * Returns number of experiments gene studied in
     *
     * @param ef                          Experimental Factor name for which to retrieve experiments; if nul, return all experiments for this gene
     * @param atlasStatisticsQueryService
     * @return number
     */
    public int getNumberOfExperiments(String ef, @Nonnull AtlasStatisticsQueryService atlasStatisticsQueryService) {
        return getExperimentIds(ef, atlasStatisticsQueryService).size();
    }

    /**
     * Returns number of experiments gene studied in
     *
     * @param atlasStatisticsQueryService
     * @return number
     */
    public Set<Long> getExperimentIds(@Nonnull AtlasStatisticsQueryService atlasStatisticsQueryService) {
        return getExperimentIds(null, atlasStatisticsQueryService);
    }


    /**
     * Returns number of experiments gene studied in
     *
     * @param ef                          Experimental Factor name for which to retrieve experiments; if nul, return all experiments for this gene
     * @param atlasStatisticsQueryService
     * @return number
     */
    public Set<Long> getExperimentIds(@Nullable String ef, @Nonnull AtlasStatisticsQueryService atlasStatisticsQueryService) {
        List<Experiment> experiments = atlasStatisticsQueryService.getExperimentsForGeneAndEf(getGeneId(), ef, UP_DOWN);
        Set<Long> expIds = new HashSet<Long>();
        for (Experiment exp : experiments) {
            expIds.add(Long.parseLong(exp.getExperimentId()));
        }
        return expIds;
    }

    /**
     * Returns expression heatmap for gene
     *
     * @param omittedEfs                  factors to skip
     * @param atlasStatisticsQueryService bit index query service - used to retrieve experiment counts (currently nonDE only)
     * @param fetchNonDECounts            if true, fetch nonDE counts
     * @return EFV tree of up/down counters for gene
     */
    public EfvTree<UpdownCounter> getHeatMap(Collection<String> omittedEfs, AtlasStatisticsQueryService atlasStatisticsQueryService, boolean fetchNonDECounts) {
        return getHeatMap(null, omittedEfs, atlasStatisticsQueryService, fetchNonDECounts, 0);
    }


    /**
     * get heatmap for one factor only
     *
     * @param efName
     * @param omittedEfs
     * @param atlasStatisticsQueryService
     * @param fetchNonDECounts            if true, fetch nonDE counts
     * @param maxNonDEFactors             if fetchNonDECounts true, specifies the maximum number of factors to get nonDE counts for (as getting nonDE counts is expensive,
     *                                    this prevents getting nonDE counts for factor values that won't be displayed to the user); -1 means that nonDE values should be retrieved for all efvs.
     * @return
     */
    public EfvTree<UpdownCounter> getHeatMap(
            String efName,
            Collection<String> omittedEfs,
            AtlasStatisticsQueryService atlasStatisticsQueryService,
            boolean fetchNonDECounts,
            int maxNonDEFactors) {

        if (efToHeatmapCache.containsKey(efName)) { // retrieve heatmap from cache if it's there
            return efToHeatmapCache.get(efName);
        }

        EfvTree<UpdownCounter> result = new EfvTree<UpdownCounter>();

        Maker<UpdownCounter> maker = new Maker<UpdownCounter>() {
            public UpdownCounter make() {
                return new UpdownCounter();
            }
        };

        long bitIndexAccessTime = 0;
        List<Attribute> scoringEfvsForGene = atlasStatisticsQueryService.getScoringEfvsForGene(getGeneId(), UP_DOWN);
        for (Attribute attr : scoringEfvsForGene) {
            if (omittedEfs.contains(attr.getEf()) || (efName != null && !efName.equals(attr.getEf())))
                continue;
            List<Experiment> allExperimentsForAttribute = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(
                    getGeneId(), UP_DOWN, attr.getEf(), attr.getEfv(), false, -1, -1);
            UpdownCounter counter = result.getOrCreate(attr.getEf(), attr.getEfv(), maker);
            // Retrieve all up/down counts and pvals/tStatRanks
            for (Experiment exp : allExperimentsForAttribute) {
                boolean isNo = ExpressionAnalysis.isNo(exp.getpValTStatRank().getPValue(), exp.getpValTStatRank().getTStatRank());
                if (!isNo) {
                    counter.add(ExpressionAnalysis.isUp(exp.getpValTStatRank().getPValue(), exp.getpValTStatRank().getTStatRank()),
                            exp.getpValTStatRank().getPValue());
                }
                counter.addExperiment(Long.parseLong(exp.getExperimentId()));
            }
        }
        log.debug("Retrieved up/down counts from bit index for " + getGeneName() + "'s heatmap " + (efName != null ? "for ef: " + efName : "across all efs") + " in: " + bitIndexAccessTime + " ms");

        // Having processed all up/down stats from Solr gene index, now fill in non-de experiment counts from atlasStatisticsQueryService
        if (fetchNonDECounts) {
            bitIndexAccessTime = 0;
            int i = 0;
            for (EfvTree.EfEfv<UpdownCounter> f : result.getNameSortedList()) {
                if (maxNonDEFactors != ExperimentalFactor.NONDE_COUNTS_FOR_ALL_EFVS && i >= maxNonDEFactors) {
                    // if no factor was specified, we only display maximum RESULT_ALL_VALUES_SIZE per factor - no point getting
                    // non-DE counts for favtor values that won't be displayed to the user.
                    break;
                }
                long start = System.currentTimeMillis();
                Attribute attr;
                if (f.getEfv() != null && !f.getEfv().isEmpty()) {
                    attr = new Attribute(f.getEf(), f.getEfv());
                } else {
                    attr = new Attribute(f.getEf());
                }
                int numNo = atlasStatisticsQueryService.getExperimentCountsForGene(attr.getValue(), StatisticsType.NON_D_E, false, getGeneId());
                f.getPayload().setNones(numNo);
                bitIndexAccessTime += System.currentTimeMillis() - start;
                i++;
            }
            log.info("Retrieved non-de counts from bit index for " + getGeneName() + "'s heatmap " + (efName != null ? "for ef: " + efName : "across all efs") + " in: " + bitIndexAccessTime + " ms");
        }

        efToHeatmapCache.put(efName, result); // store heatmap in cache

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        return getGeneId() == ((AtlasGene) obj).getGeneId();
    }

    @Override
    public int hashCode() {
        return geneSolrDocument != null ? geneSolrDocument.hashCode() : 0;
    }

    /**
     * @param omittedEfs
     * @param ef
     * @param atlasStatisticsQueryService
     * @return List of experimental factors with up/down differential expression for this gene. If ef != null, this method serves to check it
     *         ef is one of the factors with up/down differential expression for this gene and, if so, returns a singleton List containing
     *         ExperimentalFactor corresponding to ef. Each ExperimentalFactor in the returned list is populated with a list of experiments in which this gene
     *         is up/down differentially expressed for that factor.
     */
    public List<ExperimentalFactor> getDifferentiallyExpressedFactors(
            Collection<String> omittedEfs,
            @Nullable String ef,
            AtlasStatisticsQueryService atlasStatisticsQueryService) {
        List<ExperimentalFactor> result = new ArrayList<ExperimentalFactor>();
        List<String> efs = atlasStatisticsQueryService.getScoringEfsForGene(getGeneId(), UP_DOWN, ef);
        efs.removeAll(omittedEfs);

        // Now retrieve (unsorted) set all experiments for in which efs have up/down expression
        long start = System.currentTimeMillis();
        for (String factorName : efs) {
            Set<Experiment> experiments = atlasStatisticsQueryService.getScoringExperimentsForGeneAndAttribute(getGeneId(), UP_DOWN, factorName, null);
            ExperimentalFactor factor = new ExperimentalFactor(this, factorName, omittedEfs, atlasStatisticsQueryService);
            for (Experiment exp : experiments) {
                factor.addExperiment(Long.parseLong(exp.getExperimentId()), exp.getAccession());
            }
            result.add(factor);
        }
        log.debug("Retrieved " + result.size() + " differentially expressed factor(s) for gene: " + getGeneName() +
                (ef != null ? " and ef: " + ef : "") + " in: " + (System.currentTimeMillis() - start) + " ms");

        Collections.sort(result, new Comparator<ExperimentalFactor>() {
            private int SortOrder(String name) {
                if (name.equals("organism_part"))
                    return 0;
                else if (name.equals("cell_line"))
                    return 1;
                else if (name.equals("cell_type"))
                    return 2;
                else if (name.equals("disease_state"))
                    return 3;
                else if (name.equals("compound"))
                    return 4;
                else
                    return 999;
            }

            public int compare(ExperimentalFactor f1, ExperimentalFactor f2) {
                int i = SortOrder(f1.getName()) - SortOrder(f2.getName());
                return (i == 0 ? f1.getName().compareTo(f2.getName()) : i);
            }
        });

        return result;
    }
}
