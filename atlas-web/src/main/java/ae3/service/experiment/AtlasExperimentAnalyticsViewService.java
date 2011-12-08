package ae3.service.experiment;

import ae3.dao.GeneSolrDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import java.util.*;


/**
 * This class us used to populate the best genes table on the experiment page
 *
 * @author Robert Petryszak
 */
public class AtlasExperimentAnalyticsViewService {

    private static final Logger log = LoggerFactory.getLogger(AtlasExperimentAnalyticsViewService.class);

    private GeneSolrDAO geneSolrDAO;

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
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
     * @return an instance of {@link ae3.service.experiment.BestDesignElementsResult}
     * @throws uk.ac.ebi.gxa.data.AtlasDataException,
     *          StatisticsNotFoundException if data could not be ready from ncdf
     */
    public BestDesignElementsResult findBestGenesForExperiment(
            final @Nonnull ExperimentPart expPart,
            final @Nonnull List<Long> geneIds,
            final @Nonnull Set<Pair<String, String>> factorValues,
            final @Nonnull UpDownCondition upDownCondition,
            final int offset,
            final int limit) throws AtlasDataException, StatisticsNotFoundException {

        final BestDesignElementsResult result = new BestDesignElementsResult();
        result.setArrayDesignAccession(expPart.getArrayDesign().getAccession());

        long startTime = System.currentTimeMillis();

        // Set bounds of the window through the matching design elements
        int deCount = 1;
        int from = Math.max(1, offset);
        int to = offset + limit - 1;

        // Retrieved data from ncdf
        long startTime1 = System.currentTimeMillis();
        List<Long> allGeneIds = new ArrayList<Long>(expPart.getGeneIds());
        final List<KeyValuePair> uEFVs = expPart.getUniqueEFVs();
        final TwoDFloatArray pvals = expPart.getPValues();
        final TwoDFloatArray tstat = expPart.getTStatistics();
        String[] designElementAccessions = expPart.getDesignElementAccessions();
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
                result.add(
                        geneSolrDAO.getGeneById(allGeneIds.get(deIndex)).getGene(),
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
        if (upDownCondition != UpDownCondition.CONDITION_ANY && !upDownCondition.apply(UpDownExpression.valueOf(pValue, tStatistic)))
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
