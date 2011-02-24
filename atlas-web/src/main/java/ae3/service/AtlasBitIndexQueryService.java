package ae3.service;

import com.google.common.base.Function;
import com.google.common.collect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.statistics.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;

/**
 * This class provides gene expression statistics query service:
 * - manages the index storage management and interaction with IndexBuider service
 * - delegates statistics queries to StatisticsQueryUtils
 */
public class AtlasBitIndexQueryService implements AtlasStatisticsQueryService {

    final private Logger log = LoggerFactory.getLogger(getClass());

    // Handler for the BitIndex builder
    private IndexBuilder indexBuilder;
    // Bitindex Object which is de-serialized from indexFileName in atlasIndexDir
    private StatisticsStorage<Long> statisticsStorage;
    private File atlasIndexDir;
    private String indexFileName;

    // Used for finding children for query efo's
    private Efo efo;

    public AtlasBitIndexQueryService(String indexFileName) {
        this.indexFileName = indexFileName;
    }

    public void setAtlasIndex(File atlasIndexDir) {
        this.atlasIndexDir = atlasIndexDir;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setStatisticsStorage(StatisticsStorage<Long> statisticsStorage) {
        this.statisticsStorage = statisticsStorage;

    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    /**
     * Index rebuild notification handler - after bit index is re-built, de-serialize it into statisticsStorage and re-populate statTypeToEfoToScores cache
     */
    public void onIndexBuildFinish() {
        StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(indexFileName);
        statisticsStorageFactory.setAtlasIndex(atlasIndexDir);
        try {
            statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
        } catch (IOException ioe) {
            String errMsg = "Failed to create statisticsStorage from " + new File(atlasIndexDir.getAbsolutePath(), indexFileName);
            log.error(errMsg, ioe);
            throw logUnexpected(errMsg, ioe);
        }
    }

    public void onIndexBuildStart() {
        // Nothing to do here
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

    /**
     * @param attribute
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(Attribute attribute, Long geneId) {
        return getExperimentCountsForGene(attribute, geneId, null, null);
    }

    /**
     * @param attribute
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(
            Attribute attribute,
            Long geneId,
            Set<Long> geneRestrictionSet,
            HashMap<String, Multiset<Integer>> scoresCacheForStatType) {

        if (!attribute.isEfo() && statisticsStorage.getIndexForAttribute(attribute) == null) {
            // TODO NB. This is currently possible as sample properties are not currently stored in statisticsStorage
            log.debug("Attribute " + attribute + " was not found in Attribute Index");
            // return experiment count == 0 if an efv attribute could not be found in AttributeIndex (note
            // that efo attributes are not explicitly stored in AttributeIndex)
            return 0;

        }

        if (geneRestrictionSet == null) { // By default restrict the experiment count query to geneId
            geneRestrictionSet = Collections.singleton(geneId);
        }

        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(geneRestrictionSet);
        statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attribute)));
        Multiset<Integer> scores = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null);

        // Cache geneRestrictionSet's scores for efvOrEfo - this cache will be re-used in heatmaps for rows other than the first one
        if (scoresCacheForStatType != null) {
            scoresCacheForStatType.put(attribute.getValue(), scores);
        }
        Integer geneIndex = statisticsStorage.getIndexForGeneId(geneId);

        if (scores != null) {
            long time = System.currentTimeMillis();
            int expCountForGene = scores.count(geneIndex);
            if (expCountForGene > 0) {
                log.debug(attribute.getStatType() + " " + attribute.getValue() + " expCountForGene: " + geneId + " (" + geneIndex + ") = " + expCountForGene + " got in:  " + (System.currentTimeMillis() - time) + " ms");
            }
            return expCountForGene;
        }
        return 0;
    }

    /**
     * @param orAttributes
     * @return StatisticsQueryOrConditions, including children of all efo's in orAttributes
     */
    public StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(List<Attribute> orAttributes) {
        List<Attribute> efoPlusChildren = includeEfoChildren(orAttributes);
        return StatisticsQueryUtils.getStatisticsOrQuery(efoPlusChildren, statisticsStorage);
    }

    /**
     * @param orAttributes
     * @return List containing all (afv and efo) attributes in orAttributes, plus the children of all efo's in orAttributes
     */
    private List<Attribute> includeEfoChildren(List<Attribute> orAttributes) {
        // LinkedHashSet for maintaining order of entry - order of processing attributes may be important
        // in multi-Attribute queries for sorted lists of experiments for the gene page
        Set<Attribute> attrsPlusChildren = new LinkedHashSet<Attribute>();
        for (Attribute attr : orAttributes) {
            if (attr.isEfo()) {
                Collection<String> efoPlusChildren = efo.getTermAndAllChildrenIds(attr.getValue());
                log.debug("Expanded efo: " + attr + " into: " + efoPlusChildren);
                for (String efoTerm : efoPlusChildren) {
                    attrsPlusChildren.add(new Attribute(efoTerm, StatisticsQueryUtils.EFO, attr.getStatType()));
                }
            } else {
                attrsPlusChildren.add(attr);
            }
        }
        return new ArrayList<Attribute>(attrsPlusChildren);
    }


    public Integer getIndexForGene(Long geneId) {
        return statisticsStorage.getIndexForGeneId(geneId);
    }

    /**
     * @param attribute
     * @return Index of Attribute within bit index
     */
    public Integer getIndexForAttribute(Attribute attribute) {
        return statisticsStorage.getIndexForAttribute(attribute);
    }

    /**
     * @param attributeIndex
     * @return Attribute corresponding to attributeIndex bit index
     */
    public Attribute getAttributeForIndex(Integer attributeIndex) {
        return statisticsStorage.getAttributeForIndex(attributeIndex);
    }


    /**
     * http://stackoverflow.com/questions/3029151/find-top-n-elements-in-a-multiset-from-google-collections
     *
     * @param multiset
     * @param <T>
     * @return
     */
    private static <T> ImmutableList<Multiset.Entry<T>> sortedByCount(Multiset<T> multiset) {
        Ordering<Multiset.Entry<T>> countComp = new Ordering<Multiset.Entry<T>>() {
            public int compare(Multiset.Entry<T> e1, Multiset.Entry<T> e2) {
                return e2.getCount() - e1.getCount();
            }
        };
        return countComp.immutableSortedCopy(multiset.entrySet());
    }

    /**
     * http://stackoverflow.com/questions/3029151/find-top-n-elements-in-a-multiset-from-google-collections
     *
     * @param multiset
     * @param min
     * @param max
     * @param <T>
     * @return
     */
    private static <T> ImmutableList<Multiset.Entry<T>> getEntriesBetweenMinMaxFromListSortedByCount(Multiset<T> multiset,
                                                                                                     int min, int max) {
        ImmutableList<Multiset.Entry<T>> sortedByCount = sortedByCount(multiset);
        if (min < 0)
            min = 0;
        if (sortedByCount.size() > max) {
            return sortedByCount.subList(min, max);
        }
        return sortedByCount.subList(min, sortedByCount.size());
    }

    /**
     * @param statsQuery
     * @param minPos
     * @param rows
     * @param sortedGenesChunk - a chunk of the overall sorted (by experiment counts - in desc order) list of genes,
     *                         starting from 'minPos' and containing maximums 'rows' genes
     * @return The overall number of genes for which counts exist in statsQuery
     */
    public Integer getSortedGenes(final StatisticsQueryCondition statsQuery, final int minPos, final int rows, List<Long> sortedGenesChunk) {
        long timeStart = System.currentTimeMillis();
        Multiset<Integer> countsForConditions = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null);
        log.debug("getSortedGenes() bit index query: " + statsQuery.prettyPrint());
        log.debug("getSortedGenes() query returned " + countsForConditions.elementSet().size() +
                " genes with counts present in : " + (System.currentTimeMillis() - timeStart) + " ms");
        List<Multiset.Entry<Integer>> sortedCounts = getEntriesBetweenMinMaxFromListSortedByCount(countsForConditions, minPos, minPos + rows);
        for (Multiset.Entry<Integer> entry : sortedCounts) {
            Long geneId = statisticsStorage.getGeneIdForIndex(entry.getElement());
            if (geneId != null) {
                sortedGenesChunk.add(geneId);
            } else {
                log.error("Failed to retrieve gene id for index: " + entry.getElement());
            }
        }

        return countsForConditions.elementSet().size();
    }

    /**
     * @param efoTerm
     * @return Set of Attributes corresponding to efoTerm. Note that efo's map to ef-efv-experiment triples. However, this method
     *         is used in AtlasStructuredQueryService for populating list view, which for efo queries shows ef-efvs those efos map to and
     *         _all_ experiments in which these ef-efvs have expressions. In other words, we don't restrict experiments shown in the list view
     *         to just those in query efo->ef-efv-experiment mapping.
     */
    public Set<Attribute> getAttributesForEfo(String efoTerm) {
        Set<Attribute> attrsForEfo = new HashSet<Attribute>();
        Map<Integer, Set<Integer>> expToAttrsForEfo = statisticsStorage.getMappingsForEfo(efoTerm);

        if (expToAttrsForEfo != null) {
            for (Collection<Integer> expToAttrIndexes : expToAttrsForEfo.values()) {

                Collection<Attribute> attrsForExp = Collections2.transform(expToAttrIndexes,
                        new Function<Integer, Attribute>() {
                            public Attribute apply(@Nonnull Integer attrIndex) {
                                return statisticsStorage.getAttributeForIndex(attrIndex);
                            }
                        });
                attrsForEfo.addAll(attrsForExp);
            }
        }
        return attrsForEfo;
    }

    /**
     * @param geneId   Gene of interest
     * @param attribute Attribute
     * @param fromRow  Used for paginating of experiment plots on gene page
     * @param toRow    ditto
     * @return List of Experiments sorted by pVal/tStat ranks from best to worst
     */
    public List<Experiment> getExperimentsSortedByPvalueTRank(
            final Long geneId,
            final Attribute attribute,
            int fromRow,
            int toRow) {

        List<Attribute> attrs;
        if (!attribute.isEfo() && attribute.getEf() == null && attribute.getEfv() == null) {
            List<String> efs = getScoringEfsForGene(geneId, StatisticsType.UP_DOWN, null);
            attrs = new ArrayList<Attribute>();
            for (String expFactor : efs) {
                Attribute attr = new Attribute(expFactor);
                attr.setStatType(attribute.getStatType());
                attrs.add(attr);
            }
        } else {
            attrs = Collections.singletonList(attribute);
        }

        // Assemble stats query that will be used to extract sorted experiments
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(Collections.singleton(geneId));
        statsQuery.and(getStatisticsOrQuery(attrs));

        // retrieve experiments sorted by pValue/tRank for statsQuery
        List<Experiment> bestExperiments = new ArrayList<Experiment>();
        StatisticsQueryUtils.getBestExperiments(statsQuery, statisticsStorage, bestExperiments);

        // Sort bestExperiments by best pVal/tStat ranks first
        Collections.sort(bestExperiments, new Comparator<Experiment>() {
            public int compare(Experiment e1, Experiment e2) {
                return e1.getpValTStatRank().compareTo(e2.getpValTStatRank());
            }
        });

        // Extract the correct chunk (Note that if toRow == fromRow == -1, the whole of bestExperiments is returned)
        int maxSize = bestExperiments.size();
        if (fromRow == -1)
            fromRow = 0;
        if (toRow == -1 || toRow > maxSize)
            toRow = maxSize;
        List<Experiment> exps = bestExperiments.subList(fromRow, toRow);

        log.debug("Sorted experiments: ");
        for (Experiment exp : exps) {
            log.debug(exp.getAccession() + ": pval=" + exp.getpValTStatRank().getPValue() +
                    "; tStat rank: " + exp.getpValTStatRank().getTStatRank() + "; highest ranking ef: " + exp.getHighestRankAttribute());
        }
        return exps;
    }


    /**
     * @param geneId
     * @param statType
     * @param ef
     * @return list all efs for which geneId has statType expression in at least one experiment
     */
    public List<String> getScoringEfsForGene(final Long geneId,
                                             final StatisticsType statType,
                                             @Nullable final String ef) {

        long timeStart = System.currentTimeMillis();
        List<String> scoringEfs = new ArrayList<String>();
        Integer geneIdx = statisticsStorage.getIndexForGeneId(geneId);
        if (geneIdx != null) {
            Set<Integer> scoringEfIndexes = statisticsStorage.getScoringEfAttributesForGene(geneIdx, statType);
            for (Integer attrIdx : scoringEfIndexes) {
                Attribute attr = statisticsStorage.getAttributeForIndex(attrIdx);
                if (attr != null && (ef == null || "".equals(ef) || ef.equals(attr.getEf()))) {
                    scoringEfs.add(attr.getEf());
                }
            }
        }
        log.debug("getScoringEfsForGene()  returned " + scoringEfs.size() + " efs for geneId: " + geneId + " in: " + (System.currentTimeMillis() - timeStart) + " ms");

        return scoringEfs;
    }

    /**
     * @param geneId
     * @param statType
     * @return list all efs for which geneId has statType expression in at least one experiment
     */
    public List<Attribute> getScoringEfvsForGene(final Long geneId,
                                                 final StatisticsType statType) {

        long timeStart = System.currentTimeMillis();
        List<Attribute> scoringEfvs = new ArrayList<Attribute>();
        Integer geneIdx = statisticsStorage.getIndexForGeneId(geneId);
        if (geneIdx != null) {
            Set<Integer> scoringEfvIndexes = statisticsStorage.getScoringEfvAttributesForGene(geneIdx, statType);
            for (Integer attrIdx : scoringEfvIndexes) {
                Attribute attr = statisticsStorage.getAttributeForIndex(attrIdx);
                if (attr.getEfv() != null && !attr.getEfv().isEmpty()) {
                    attr.setStatType(statType);
                    scoringEfvs.add(attr);
                }
            }
        }
        log.debug("getScoringEfsForGene()  returned " + scoringEfvs.size() + " efs for geneId: " + geneId + " in: " + (System.currentTimeMillis() - timeStart) + " ms");

        return scoringEfvs;
    }

    /**
     * @param ef
     * @param geneId
     * @param statType
     * @return unsorted list of experiments for which geneId has statType expression for ef attr
     */
    public List<Experiment> getExperimentsForGeneAndEf(Long geneId, @Nullable String ef, StatisticsType statType) {
        List<Experiment> exps = new ArrayList<Experiment>();
        Integer geneIdx = statisticsStorage.getIndexForGeneId(geneId);
        Integer attrIdx = null;
        // Note that if ef == null, this method returns list of experiments across all efs for which this gene has up/down exp counts
        if (ef != null)
            attrIdx = statisticsStorage.getIndexForAttribute(new Attribute(ef));
        if (geneIdx != null) {
            Set<Integer> expIdxs = statisticsStorage.getExperimentsForGeneAndAttribute(attrIdx, geneIdx, statType);
            for (Integer expIdx : expIdxs) {
                Experiment exp = statisticsStorage.getExperimentForIndex(expIdx);
                if (exp != null) {
                    exps.add(exp);
                }
            }
        }

        return exps;
    }

    /**
     * @param geneIds
     * @param statType
     * @param autoFactors set of factors of interest
     * @return Serted set of non-zero experiment counts (for at least one of geneIds and statType) per efv (note: not efo) attribute
     */
    public List<Multiset.Entry<Integer>> getScoringAttributesForGenes(Set<Long> geneIds, StatisticsType statType, Collection<String> autoFactors) {
        long timeStart = System.currentTimeMillis();

        Multiset<Integer> attrCounts = HashMultiset.create();
        Set<Attribute> allEfvAttributesForStat = statisticsStorage.getAllAttributes(statType);
        for (Attribute attr : allEfvAttributesForStat) {
            if ((autoFactors != null && !autoFactors.contains(attr.getEf())) || attr.getEfv() == null) {
                continue; // skip attribute if its factor is not of interest or it's an ef-only attribute
            }
            Integer attrIndex = statisticsStorage.getIndexForAttribute(attr);
            attr.setStatType(statType);
            StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(geneIds);
            statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attr)));
            Set<Experiment> scoringExps = new HashSet<Experiment>();
            StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExps);
            if (scoringExps.size() > 0) { // at least one gene in geneIds had an experiment count > 0 for attr
                attrCounts.add(attrIndex, scoringExps.size());
                for (Experiment exp : scoringExps) {
                    String efoTerm = statisticsStorage.getEfoTerm(attr, exp);
                    if (efoTerm != null) {
                        log.debug("Skipping efo: " + efoTerm + " for attr: " + attr + " and exp: " + exp);
                    }
                }
            }
        }
        List<Multiset.Entry<Integer>> sortedAttrCounts = getEntriesBetweenMinMaxFromListSortedByCount(attrCounts, 0, attrCounts.entrySet().size());

        log.debug("Retrieved " + sortedAttrCounts.size() + " sorted scoring attributes for statType: " + statType + " and gene ids: (" + geneIds + ") in " + (System.currentTimeMillis() - timeStart) + "ms");
        return sortedAttrCounts;
    }

    /**
     * @param geneId
     * @param statType
     * @param ef
     * @param efv
     * @return Set of Experiments in which geneId-ef-efv have statType expression
     */
    public Set<Experiment> getScoringExperimentsForGeneAndAttribute(Long geneId, StatisticsType statType, @Nonnull String ef, @Nullable String efv) {
        Attribute attr = new Attribute(ef, efv);
        attr.setStatType(statType);
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(Collections.singleton(geneId));
        statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attr)));
        Set<Experiment> scoringExps = new HashSet<Experiment>();
        StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExps);
        return scoringExps;
    }
}
