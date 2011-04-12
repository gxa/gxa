package ae3.service.experiment;

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.experiment.rcommand.RCommand;
import ae3.service.experiment.rcommand.RCommandResult;
import ae3.service.experiment.rcommand.RCommandStatement;
import ae3.service.structuredquery.ExpFactorQueryCondition;
import ae3.service.structuredquery.QueryExpression;
import ae3.service.structuredquery.QueryResultSortOrder;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * The query engine for the experiment page
 *
 * @author rpetry
 */

public class AtlasExperimentAnalyticsViewService {

    final private Logger log = LoggerFactory.getLogger(getClass());

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
     * @param experiment    the experiment in question
     * @param geneIds         list of AtlasGene's to get best Expression Analytics data for
     * @param ncdf          the netCDF proxy's path from which findBestGenesInExperimentR() will retrieve data
     * @param conditions    Experimental factor conditions
     * @param statFilter    Up/down expression filter
     * @param sortOrder     Result set sort order
     * @param start         Start position within the result set (related to result set pagination on the experiment page)
     * @param numOfTopGenes topN determines how many top genes should be found, given the specified sortOrder
     * @return analytics view table data for top genes in this experiment
     * @throws uk.ac.ebi.gxa.analytics.compute.ComputeException
     *          if an error happened during R function call
     */
    public BestDesignElementsResult findBestGenesForExperiment(
            final @Nonnull AtlasExperiment experiment,
            final @Nonnull Collection<Long> geneIds,
            final @Nonnull NetCDFDescriptor ncdf,
            final @Nonnull Collection<ExpFactorQueryCondition> conditions,
            final @Nonnull QueryExpression statFilter,
            final @Nonnull QueryResultSortOrder sortOrder,
            final int start,
            final int numOfTopGenes) throws ComputeException {

        long startTime = System.currentTimeMillis();

        Collection<String> factors = Collections.emptyList();
        Collection<String> factorValues = Collections.emptyList();
        if (!conditions.isEmpty()) {
            factors = Arrays.asList(conditions.iterator().next().getFactor());
            factorValues = conditions.iterator().next().getFactorValues();
        }

        RCommand command = new RCommand(computeService, "R/analytics.R");
        RCommandResult rResult = command.execute(new RCommandStatement("find.best.design.elements")
                .addParam(ncdf.getPathForR())
                .addParam(geneIds)
                .addParam(factors)
                .addParam(factorValues)
                .addParam(statFilter.toString())
                .addParam(sortOrder.toString())
                .addParam(start)
                .addParam(numOfTopGenes));

        log.info("Finished find.best.design.elements in:  " + (System.currentTimeMillis() - startTime) + " ms");

        BestDesignElementsResult result = new BestDesignElementsResult();

        if (!rResult.isEmpty()) {

            int[] deIndexes = rResult.getIntValues("deindexes");
            // TODO gIds and deIds should be long[]
            int[] deIds = rResult.getIntValues("designelements");
            int[] gIds = rResult.getIntValues("geneids");
            double[] pvals = rResult.getNumericValues("minpvals");
            double[] tstats = rResult.getNumericValues("maxtstats");
            String[] uvals = rResult.getStringValues("uvals");
            long total = (long)rResult.getIntAttribute("total")[0];

            result.setTotalSize(total);

            Map<Long, AtlasGene> geneMap = new HashMap<Long, AtlasGene>();
            Iterable<AtlasGene> solrGenes = geneSolrDAO.getGenesByIdentifiers(Ints.asList(gIds));
            for (AtlasGene gene : solrGenes) {
                geneMap.put(gene.getGeneId(), gene);
            }

            for (int i = 0; i < gIds.length; i++) {
                int gId = gIds[i];
                long geneId = (long) gId;

                AtlasGene gene = geneMap.get(geneId);
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

                result.add(gene, deIds[i], deIndexes[i] - 1, pvals[i], tstats[i], ef, efv);
            }
        } else {
            log.error("No could be found in experiment: " + experiment.getAccession());
        }

        log.info("Finished findBestGenesForExperiment in:  " + (System.currentTimeMillis() - startTime) + " ms");
        return result;
    }
}
