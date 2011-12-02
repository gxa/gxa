/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.data;

import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * @author Olga Melnichuk
 */
public class ExperimentPart {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ExperimentWithData ewd;
    private final ArrayDesign arrayDesign;

    public ExperimentPart(ExperimentWithData ewd, ArrayDesign arrayDesign) {
        this.ewd = ewd;
        this.arrayDesign = arrayDesign;
    }

    public List<KeyValuePair> getUniqueEFVs() throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getUniqueEFVs(arrayDesign);
    }

    public TwoDFloatArray getPValues() throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getPValues(arrayDesign);
    }

    public TwoDFloatArray getTStatistics() throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getTStatistics(arrayDesign);
    }

    public String[] getDesignElementAccessions() throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getDesignElementAccessions(arrayDesign);
    }

    public Collection<Long> getGeneIds() throws AtlasDataException {
        return Longs.asList(ewd.getGenes(arrayDesign));
    }

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(Collection<Long> geneIds)
            throws AtlasDataException, StatisticsNotFoundException {
        return ewd.getExpressionAnalysesForGeneIds(geneIds, arrayDesign);
    }

    public String[] getFactorValues(String ef) throws AtlasDataException {
        return ewd.getFactorValues(arrayDesign, ef);
    }

    public List<ExpressionValue> getBestGeneExpressionValues(Long geneId, String ef, String efv) throws AtlasDataException, StatisticsNotFoundException {
        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> bestEAValues =
                ewd.getExpressionAnalysesForGeneIds(Arrays.asList(geneId), arrayDesign);

        if (bestEAValues == null) {
            return Collections.emptyList();
        }
        Map<String, Map<String, ExpressionAnalysis>> eaByEfEfv = bestEAValues.get(geneId);
        if (eaByEfEfv == null) {
            return Collections.emptyList();
        }
        Map<String, ExpressionAnalysis> eaByEf = eaByEfEfv.get(ef);
        if (eaByEf == null) {
            return Collections.emptyList();
        }
        ExpressionAnalysis bestEA = eaByEf.get(efv);
        return getDeExpressionValues(bestEA.getDesignElementIndex(), ef);
    }

    public List<ExpressionValue> getDeExpressionValues(String deAccession, String ef) throws AtlasDataException {
        String[] deAccessions = ewd.getDesignElementAccessions(arrayDesign);
        for (int i = 0; i < deAccessions.length; i++) {
            if (deAccessions[i].equals(deAccession)) {
                return getDeExpressionValues(i + 1, ef);
            }
        }
        return Collections.emptyList();
    }

    private List<ExpressionValue> getDeExpressionValues(int deIndex, String ef) throws AtlasDataException {
        List<ExpressionValue> res = new ArrayList<ExpressionValue>();
        float[] expressions = ewd.getExpressionDataForDesignElementAtIndex(arrayDesign, deIndex);
        String[] assayEfvs = ewd.getFactorValues(arrayDesign, ef);
        if (expressions.length != assayEfvs.length) {
            throw createUnexpected("Inconsistent parallel arrays in " + toString() + ": " + expressions.length + " != " + assayEfvs.length);
        }
        for (int i = 0; i < expressions.length; i++) {
            res.add(new ExpressionValue(ef, assayEfvs[i], expressions[i]));
        }
        return res;
    }

    @Override
    public String toString() {
        return "ExperimentPart@{" +
                ewd.getExperiment().getAccession() + "/" +
                arrayDesign.getAccession() + "}";
    }

    public boolean containsDeAccessions(Collection<String> list) throws AtlasDataException {
        String[] deAccessions = ewd.getDesignElementAccessions(arrayDesign);
        return Arrays.asList(deAccessions).containsAll(list);
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    /**
     * Returns the list of top design elements found in the experiment. The search is based on the pre-calculated
     * statistic values (e.g. T-value, P-value): the better the statistics, the higher the element in the list.
     * <p/>
     * A note regarding geneIds, factors and factorValues parameters:
     * - If all parameters are empty the search is done for all data (design elements, ef and efv pairs);
     * - Filling any parameter narrows one of the search dimensions.
     *
     * @param geneIds         a list of gene ids of interest
     * @param factorValues    a list of Pairs of factor-factor value to find best statistics for
     *                        Note also that ee don't currently allow search for best design elements by either just an ef
     *                        or just an efv - both need to be specified
     * @param upDownCondition an up/down expression filter
     * @param offset          Start position within the result set
     * @param limit           how many design elements to return
     * @return an instance of {@link BestDesignElementsResult}
     * @throws AtlasDataException, StatisticsNotFoundException if data could not be ready from ncdf
     */
    public BestDesignElementsResult findBestGenesForExperiment(
            final @Nonnull List<Long> geneIds,
            final @Nonnull Set<Pair<String, String>> factorValues,
            final @Nonnull UpDownCondition upDownCondition,
            final int offset,
            final int limit) throws AtlasDataException, StatisticsNotFoundException {

        final BestDesignElementsResult result = new BestDesignElementsResult();
        result.setArrayDesignAccession(getArrayDesign().getAccession());

        long startTime = System.currentTimeMillis();

        // Set bounds of the window through the matching design elements
        int deCount = 1;
        int from = Math.max(1, offset);
        int to = offset + limit - 1;

        // Retrieved data from ncdf
        long startTime1 = System.currentTimeMillis();
        List<Long> allGeneIds = new ArrayList<Long>(getGeneIds());
        final List<KeyValuePair> uEFVs = getUniqueEFVs();
        final TwoDFloatArray pvals = getPValues();
        final TwoDFloatArray tstat = getTStatistics();
        String[] designElementAccessions = getDesignElementAccessions();
        log.debug("Retrieved data from ncdf in:  " + (System.currentTimeMillis() - startTime1) + " ms");

        boolean factorValuesSpecified = !factorValues.isEmpty();
        boolean genesSpecified = !geneIds.isEmpty();

        // Retrieve qualifying BestDesignElementCandidate's
        startTime1 = System.currentTimeMillis();
        List<BestDesignElementCandidate> candidates = new ArrayList<BestDesignElementCandidate>();

        for (int i = 0; i < pvals.getRowCount(); i++) {
            BestDesignElementCandidate bestSoFar = null;
            if (!designElementQualifies(genesSpecified, allGeneIds, geneIds, i))
                continue;
            for (int j = 0; j < uEFVs.size(); j++) {
                if (!efvQualifies(factorValuesSpecified, uEFVs.get(j), factorValues))
                    continue;
                if (statisticsQualify(upDownCondition, pvals.get(i, j), tstat.get(i, j))) {
                    BestDesignElementCandidate current = new BestDesignElementCandidate(pvals.get(i, j), tstat.get(i, j), i, j);
                    if (bestSoFar == null || current.compareTo(bestSoFar) < 0)
                        bestSoFar = current;
                }
            }
            if (bestSoFar != null)
                candidates.add(bestSoFar);
        }
        log.debug("Loaded " + candidates.size() + " candidates in:  " + (System.currentTimeMillis() - startTime1) + " ms");

        // Sort BestDesignElementCandidate's by pVal/tStat
        startTime1 = System.currentTimeMillis();
        Collections.sort(candidates);
        log.debug("Sorted DE candidates in:  " + (System.currentTimeMillis() - startTime1) + " ms");
        startTime1 = System.currentTimeMillis();

        // Assemble BestDesignElementsResult from candidates between to and from bounds
        for (BestDesignElementCandidate candidate : candidates) {
            final Integer deIndex = candidate.getDEIndex();
            final Integer uEfvIndex = candidate.getUEFVIndex();
            final KeyValuePair efv = uEFVs.get(uEfvIndex);
            if (deCount >= from && deCount <= to) {
                result.add(allGeneIds.get(deIndex),
                        deIndex,
                        designElementAccessions[deIndex],
                        pvals.get(deIndex, uEfvIndex),
                        tstat.get(deIndex, uEfvIndex),
                        efv.key,
                        efv.value);
            }
            deCount++;
        }
        log.debug("Assembled BestDesignElementsResult in:  " + (System.currentTimeMillis() - startTime1) + " ms");

        result.setTotalSize(candidates.size());

        log.info("Finished findBestGenesForExperiment in:  " + (System.currentTimeMillis() - startTime) + " ms");
        return result;
    }

    private boolean efvQualifies(
            boolean factorValuesSpecified,
            final @Nonnull KeyValuePair efv,
            final @Nonnull Set<Pair<String, String>> factorValues) {
        return !factorValuesSpecified || factorValues.contains(Pair.create(efv.key, efv.value));
    }

    private boolean statisticsQualify(
            final UpDownCondition upDownCondition, float pValue, float tStatistic) {
        if (upDownCondition != upDownCondition.CONDITION_ANY && !upDownCondition.apply(UpDownExpression.valueOf(pValue, tStatistic)))
            return false;
        if (pValue > 1) // Ignore NA pvals/tstats (that currently come back from ncdfs as 1.0E30)
            return false;
        return true;
    }

    private boolean designElementQualifies(
            boolean genesSpecified,
            final List<Long> allGeneIds,
            final List<Long> geneIds,
            int deIndex) {
        return allGeneIds.get(deIndex) > 0 && (!genesSpecified || geneIds.contains(allGeneIds.get(deIndex)));
    }
}
