package ae3.service.experiment;

import ae3.dao.GeneSolrDAO;
import com.google.common.base.Predicate;
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

        StatisticsCursor stats = expPart.getStatisticsIterator(geneIdPredicate, fvPredicate);

        List<StatisticsSnapshot> result = newArrayList();
        while (stats.nextBioEntity()) {
            Best<StatisticsSnapshot> bestDE = Best.create();
            while (stats.nextEFV()) {
                if (upDownPredicate.apply(stats.getExpression())) {
                    bestDE.offer(stats.getSnapshot());
                }
            }
            if (bestDE.isFound())
                result.add(bestDE.get());
        }
        sort(result);

        return convert(expPart, boundSafeSublist(result, offset, offset + limit), result.size());
    }

    private BestDesignElementsResult convert(ExperimentPart expPart, List<StatisticsSnapshot> sublist, int totalSize)
            throws AtlasDataException, StatisticsNotFoundException {
        final BestDesignElementsResult result = new BestDesignElementsResult();
        result.setArrayDesignAccession(expPart.getArrayDesign().getAccession());
        result.setTotalSize(totalSize);
        for (StatisticsSnapshot de : sublist) {
            final GeneSolrDAO.AtlasGeneResult gene = geneSolrDAO.getGeneById(de.getBioEntityId());
            if (gene.isFound())
                result.add(gene.getGene(), de);
        }
        return result;
    }
}
