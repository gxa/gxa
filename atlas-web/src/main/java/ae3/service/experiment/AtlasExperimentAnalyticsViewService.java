package ae3.service.experiment;

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import ae3.service.experiment.rcommand.RCommand;
import ae3.service.experiment.rcommand.RCommandResult;
import ae3.service.experiment.rcommand.RCommandStatement;
import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static uk.ac.ebi.microarray.atlas.model.UpDownCondition.*;
import static uk.ac.ebi.microarray.atlas.model.UpDownCondition.CONDITION_ANY;

/**
 * This class provides access to the statistical functions defined in R scripts.
 *
 * @author rpetry
 * @author Olga Melnichuk
 */

public class AtlasExperimentAnalyticsViewService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static EnumMap<UpDownCondition, String> upDownConditionInR =
            new EnumMap<UpDownCondition, String>(UpDownCondition.class);

    static {
        upDownConditionInR.put(CONDITION_UP_OR_DOWN, "UP_DOWN");
        upDownConditionInR.put(CONDITION_UP, "UP");
        upDownConditionInR.put(CONDITION_DOWN, "DOWN");
        upDownConditionInR.put(CONDITION_NONDE, "NON_D_E");
        upDownConditionInR.put(CONDITION_ANY, "ANY");
    }

    private GeneSolrDAO geneSolrDAO;

    private AtlasComputeService computeService;

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setComputeService(AtlasComputeService computeService) {
        this.computeService = computeService;
    }

    /**
     * Returns the list of top design elements found in the experiment. The search is based on the pre-calculated
     * statistic values (e.g. T-value, P-value): the better the statics, the higher the element in the list.
     * <p/>
     * A note regarding geneIds, factors and factorValues parameters:
     * - If all parameters are empty the search is done for all data (design elements, ef and efv pairs);
     * - Filling any parameter narrows one of the search dimensions.
     * (for more details of search implementation, please see analytics.R).
     *
     * @param ncdfDescr       the netCDF file descriptor
     * @param geneIds         list of geneIds to find best statistics for
     * @param factors         a list of factors to find best statistics for
     * @param factorValues    a list of factor values to find best statistics for
     * @param upDownCondition an up/down expression filter
     * @param offset          Start position within the result set
     * @param limit           how many design elements to return
     * @return an instance of {@link BestDesignElementsResult}
     * @throws uk.ac.ebi.gxa.analytics.compute.ComputeException
     *          if an error happened during R function call
     */
    public BestDesignElementsResult findBestGenesForExperiment(
            final @Nonnull NetCDFDescriptor ncdfDescr,
            final @Nonnull Collection<Long> geneIds,
            final @Nonnull Collection<String> factors,
            final @Nonnull Collection<String> factorValues,
            final @Nonnull UpDownCondition upDownCondition,
            final int offset,
            final int limit) throws ComputeException {

        BestDesignElementsResult result = new BestDesignElementsResult();

        long startTime = System.currentTimeMillis();

        RCommand command = new RCommand(computeService, "R/analytics.R");
        RCommandResult rResult = command.execute(new RCommandStatement("find.best.design.elements")
                .addParam(ncdfDescr.getPathForR())
                .addParam(geneIds)
                .addParam(factors)
                .addParam(factorValues)
                .addParam(findUpDownConditionInR(upDownCondition))
                .addParam("PVAL")
                .addParam(offset)
                .addParam(limit));

        log.info("Finished find.best.design.elements in:  " + (System.currentTimeMillis() - startTime) + " ms");

        if (!rResult.isEmpty()) {

            int[] deIndexes = rResult.getIntValues("deindexes");
            String[] deAccessions = rResult.getStringValues("deaccessions");
            int[] gIds = rResult.getIntValues("geneids");
            double[] pvals = rResult.getNumericValues("minpvals");
            double[] tstats = rResult.getNumericValues("maxtstats");
            String[] uvals = rResult.getStringValues("uvals");
            long total = (long) rResult.getIntAttribute("total")[0];

            result.setTotalSize(total);

            Map<Integer, AtlasGene> geneMap = new HashMap<Integer, AtlasGene>();
            Iterable<AtlasGene> solrGenes = geneSolrDAO.getGenesByIdentifiers(Ints.asList(gIds));
            for (AtlasGene gene : solrGenes) {
                geneMap.put(gene.getGeneId(), gene);
            }

            for (int i = 0; i < gIds.length; i++) {
                int gId = gIds[i];

                AtlasGene gene = geneMap.get(gId);
                if (gene == null) {
                    continue;
                }

                String[] uval = uvals[i].split(NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX);
                if (uval.length < 2) {
                    log.error("Illegal <ef||efv> value: " + uvals[i]);
                    continue;
                }
                String ef = uval[0];
                String efv = uval[1];

                result.add(gene, deIndexes[i] - 1, deAccessions[i], pvals[i], tstats[i], ef, efv);
            }
        }

        log.info("Finished findBestGenesForExperiment in:  " + (System.currentTimeMillis() - startTime) + " ms");
        return result;
    }

    private static String findUpDownConditionInR(UpDownCondition upDownCondition) {
        String s = upDownConditionInR.get(upDownCondition);
        if (s == null) {
            throw new IllegalArgumentException("Unsupported up/down condition: " + upDownCondition);
        }
        return s;
    }
}
