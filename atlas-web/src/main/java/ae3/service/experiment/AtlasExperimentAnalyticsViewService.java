package ae3.service.experiment;

import ae3.dao.GeneSolrDAO;
import com.google.common.base.Predicate;
import it.uniroma3.mat.extendedset.FastSet;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.utils.Best;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.sort;
import static uk.ac.ebi.gxa.utils.CollectionUtil.boundSafeSublist;
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
            final @Nullable ExperimentPart expPart,
            final @Nonnull Predicate<Long> geneIdPredicate,
            final @Nonnull Predicate<UpDownExpression> upDownPredicate,
            final @Nonnull Predicate<Pair<String, String>> fvPredicate,
            final int offset,
            final int limit) throws AtlasDataException, StatisticsNotFoundException {
        if (expPart == null)
            return new BestDesignElementsResult();

        final List<Pair<String, String>> uEFVs = expPart.getUniqueEFVs();
        final TwoDFloatArray pvals = expPart.getPValues();
        final TwoDFloatArray tstat = expPart.getTStatistics();

        List<DesignElementStatistics> stats = newArrayList();
        for (int deidx : selectedDesignElements(expPart.getGeneIds(), geneIdPredicate)) {
            Best<DesignElementStatistics> result = Best.create();
            for (int uefidx = 0; uefidx < uEFVs.size(); uefidx++) {
                float p = pvals.get(deidx, uefidx);
                float t = tstat.get(deidx, uefidx);

                if (fvPredicate.apply(uEFVs.get(uefidx)) && upDownPredicate.apply(valueOf(p, t))) {
                    result.offer(new DesignElementStatistics(p, t, deidx, uefidx));
                }
            }
            if (result.isFound())
                stats.add(result.get());
        }
        sort(stats);

        return convert(expPart, stats, offset, limit);
    }

    private BestDesignElementsResult convert(ExperimentPart expPart, List<DesignElementStatistics> stats,
                                             int offset, int limit)
            throws AtlasDataException, StatisticsNotFoundException {
        final List<Pair<String, String>> uEFVs = expPart.getUniqueEFVs();
        final List<Long> allGeneIds = expPart.getGeneIds();
        final String[] designElementAccessions = expPart.getDesignElementAccessions();

        final BestDesignElementsResult result = new BestDesignElementsResult();
        result.setArrayDesignAccession(expPart.getArrayDesign().getAccession());
        result.setTotalSize(stats.size());
        for (DesignElementStatistics de : boundSafeSublist(stats, offset, offset + limit)) {
            result.add(geneSolrDAO.getGeneById(allGeneIds.get(de.getDEIndex())).getGene(),
                    de.getDEIndex(),
                    designElementAccessions[de.getDEIndex()],
                    de.getPValue(),
                    de.getTStat(),
                    uEFVs.get(de.getUEFVIndex()));
        }
        return result;
    }

    private static FastSet selectedDesignElements(List<Long> allGeneIds, final Predicate<Long> geneIdPredicate) {
        FastSet result = new FastSet();
        for (int deidx = 0; deidx < allGeneIds.size(); deidx++) {
            final Long geneId = allGeneIds.get(deidx);
            if (geneId > 0 && geneIdPredicate.apply(geneId)) {
                result.add(deidx);
            }
        }
        return result;
    }
}
