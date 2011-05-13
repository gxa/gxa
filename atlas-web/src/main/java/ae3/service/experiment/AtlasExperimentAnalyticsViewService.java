package ae3.service.experiment;

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import ae3.service.experiment.rcommand.RCommand;
import ae3.service.experiment.rcommand.RCommandResult;
import ae3.service.experiment.rcommand.RCommandStatement;
import com.google.common.base.Function;
import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The query engine for the experiment page
 *
 * @author rpetry
 */

public class AtlasExperimentAnalyticsViewService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Function<UpDownCondition, String> SUPPORTED_UP_DOWN = new Function<UpDownCondition, String>() {
        @Override
        public String apply(@Nullable UpDownCondition input) {
            switch (input) {
                case CONDITION_UP_OR_DOWN:
                    return "UP_DOWN";
                case CONDITION_DOWN:
                    return "DOWN";
                case CONDITION_UP:
                    return "UP";
                case CONDITION_NONDE:
                    return "NON_D_E";
                case CONDITION_ANY:
                    return "ANY";
            }
            throw new IllegalArgumentException("Unsupported up/down condition: " + input);
        }
    };

    private GeneSolrDAO geneSolrDAO;

    private AtlasComputeService computeService;

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setComputeService(AtlasComputeService computeService) {
        this.computeService = computeService;
    }

    /**
     * Returns list of top genes found for particular experiment
     * ('top' == with a minimum pValue across all ef-efvs in this experiment)
     *
     * @param ncdfDescr       the netCDF file descriptor
     * @param geneIds         list of AtlasGene's to get best Expression Analytics data for
     * @param factors         a list of factors to find best statistics for
     * @param factorValues    a list of factor values to find best statatistics for
     * @param upDownCondition Up/down expression filter
     * @param offset          Start position within the result set (related to result set pagination on the experiment page)
     * @param limit           topN determines how many top genes should be found, given the specified sortOrder
     * @return analytics view table data for top genes in this experiment
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
                .addParam(SUPPORTED_UP_DOWN.apply(upDownCondition))
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
}
