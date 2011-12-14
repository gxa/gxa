package ae3.service.experiment;

import ae3.dao.GeneSolrDAO;
import com.google.common.base.Predicate;
import it.uniroma3.mat.extendedset.FastSet;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.utils.Best;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.min;
import static java.util.Collections.sort;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.valueOf;


/**
 * This class is used to populate the best genes table on the experiment page
 *
 * @author Robert Petryszak
 */
public class AtlasExperimentAnalyticsViewService {
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
     * @param expPart         experiment part to retrieve data from
     * @param geneIdPredicate a gene (id) filter
     * @param upDownPredicate an up/down expression filter
     * @param fvPredicate     an experiment factor filter
     * @param offset          Start position within the result set
     * @param limit           how many design elements to return
     * @return an instance of {@link BestDesignElementsResult}
     * @throws AtlasDataException          if data could not be read from NetCDF
     * @throws StatisticsNotFoundException if there's no P/T stats in the data
     */
    public BestDesignElementsResult findBestGenesForExperiment(
            final @Nonnull ExperimentPart expPart,
            final @Nonnull Predicate<Long> geneIdPredicate,
            final @Nonnull Predicate<UpDownExpression> upDownPredicate,
            final @Nonnull Predicate<Pair<String, String>> fvPredicate,
            final int offset,
            final int limit) throws AtlasDataException, StatisticsNotFoundException {

        final List<Pair<String, String>> uEFVs = expPart.getUniqueEFVs();
        final TwoDFloatArray pvals = expPart.getPValues();
        final TwoDFloatArray tstat = expPart.getTStatistics();

        List<BestDesignElementCandidate> candidates = newArrayList();
        for (int deidx : selectedDesignElements(expPart.getGeneIds(), geneIdPredicate)) {
            Best<BestDesignElementCandidate> result = Best.create();
            for (int uefidx = 0; uefidx < uEFVs.size(); uefidx++) {
                float p = pvals.get(deidx, uefidx);
                float t = tstat.get(deidx, uefidx);

                if (fvPredicate.apply(uEFVs.get(uefidx)) && upDownPredicate.apply(valueOf(p, t))) {
                    result.offer(new BestDesignElementCandidate(p, t, deidx, uefidx));
                }
            }
            if (result.isFound())
                candidates.add(result.get());
        }
        sort(candidates);

        return convert(expPart, expPart.getGeneIds(), candidates, offset, limit);
    }

    private BestDesignElementsResult convert(ExperimentPart expPart, List<Long> allGeneIds, List<BestDesignElementCandidate> bestDesignElementCandidates, int offset, int limit) throws AtlasDataException, StatisticsNotFoundException {
        final List<Pair<String, String>> uEFVs = expPart.getUniqueEFVs();

        final BestDesignElementsResult result = new BestDesignElementsResult();
        result.setArrayDesignAccession(expPart.getArrayDesign().getAccession());

        final String[] designElementAccessions = expPart.getDesignElementAccessions();
        for (BestDesignElementCandidate c : sublist(bestDesignElementCandidates, offset, offset + limit - 1)) {
            result.add(geneSolrDAO.getGeneById(allGeneIds.get(c.getDEIndex())).getGene(),
                    c.getDEIndex(),
                    designElementAccessions[c.getDEIndex()],
                    c.getPValue(),
                    c.getTStat(),
                    uEFVs.get(c.getUEFVIndex()).getKey(),
                    uEFVs.get(c.getUEFVIndex()).getValue());
        }
        result.setTotalSize(bestDesignElementCandidates.size());
        return result;
    }

    private static FastSet selectedDesignElements(List<Long> allGeneIds, final Predicate<Long> geneIdPredicate) {
        FastSet result = new FastSet();
        for (int deidx = 0; deidx < allGeneIds.size(); deidx++) {
            if (isMappedDE(allGeneIds, deidx) && geneIdPredicate.apply(allGeneIds.get(deidx))) {
                result.add(deidx);
            }
        }
        return result;
    }

    private static boolean isMappedDE(List<Long> allGeneIds, int deIndex) {
        return allGeneIds.get(deIndex) > 0;
    }

    private static <T> List<T> sublist(List<T> data, int from, int to) {
        return data.subList(min(data.size(), from), min(data.size(), to));
    }
}
