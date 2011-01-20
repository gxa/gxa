package ae3.service.experiment;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.structuredquery.ExpFactorQueryCondition;
import ae3.service.structuredquery.QueryExpression;
import ae3.service.structuredquery.QueryResultSortOrder;
import com.google.common.io.Resources;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RDataFrame;
import uk.ac.ebi.rcloud.server.RType.RFactor;
import uk.ac.ebi.rcloud.server.RType.RInteger;
import uk.ac.ebi.rcloud.server.RType.RNumeric;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.*;

import static com.google.common.base.Joiner.on;

/**
 * The query engine for the experiment page
 *
 * @author rpetry
 */

public class AtlasExperimentAnalyticsViewService {

    final private Logger log = LoggerFactory.getLogger(getClass());

    private AtlasSolrDAO atlasSolrDAO;
    private AtlasComputeService computeService;

    public void setAtlasSolrDAO(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setComputeService(AtlasComputeService computeService) {
        this.computeService = computeService;
    }

    /**
     * Returns list of top genes found for particular experiment
     * ('top' == with a minimum pValue across all ef-efvs in this experiment)
     *
     * @param experiment
     * @param genes         list of AtlasGene's to get best Expression Analytics data for
     * @param pathToNetCDF  the netCDF proxy's path from which findBestGenesInExperimentR() will retrieve data
     * @param conditions    Experimental factor conditions
     * @param sortOrder     Result set sort order
     * @param start         Start position within the result set (related to result set pagination on the experiment page)
     * @param numOfTopGenes topN determines how many top genes should be found, given the specified sortOrder
     * @return List<Pair<AtlasGene,ExpressionAnalysis>> analytics view table data for top genes in this experiment
     */
    public List<Pair<AtlasGene, ExpressionAnalysis>> findGenesForExperiment(
            final AtlasExperiment experiment,
            final Collection<AtlasGene> genes,
            final File pathToNetCDF,
            final Collection<ExpFactorQueryCondition> conditions,
            final QueryResultSortOrder sortOrder,
            final int start,
            final int numOfTopGenes) {
        List<Pair<AtlasGene, ExpressionAnalysis>> topGenes = new ArrayList<Pair<AtlasGene, ExpressionAnalysis>>();

        // Retrieve geneIds from geneIdsStr, if there are any
        Map<Long, AtlasGene> geneIdGeneMap = new HashMap<Long, AtlasGene>();
        for (AtlasGene gene : genes) {
            geneIdGeneMap.put(Long.valueOf(gene.getGeneId()), gene);
        }

        String efFilter = "c()";
        String efvFilter = "c()";
        QueryExpression statFilter = QueryExpression.UP_DOWN;

        if (!conditions.isEmpty()) {
            efFilter = "c('" + conditions.iterator().next().getFactor() + "')";
            efvFilter = "c('" + StringUtils.join(conditions.iterator().next().getFactorValues(), "','") + "')";
            statFilter = conditions.iterator().next().getExpression();
        }

        // bestDEIndexes is a list of design element indexes, sorted in sortOrder
        long startTime = System.currentTimeMillis();
        List<Pair<Long, ExpressionAnalysis>> bestGeneIdsToEA =
                findBestGenesInExperimentR(experiment.getAccession(), geneIdGeneMap.keySet(), pathToNetCDF, efFilter, efvFilter, statFilter, sortOrder, start, numOfTopGenes);
        log.info("Finished findBestGenesInExperimentR in:  " + (System.currentTimeMillis() - startTime) + " ms; found " + bestGeneIdsToEA.size() + " results.");

        if (bestGeneIdsToEA.isEmpty())
            return topGenes;

        Set<Long> geneIds = new HashSet<Long>();
        for (Pair<Long, ExpressionAnalysis> geneIdToEA : bestGeneIdsToEA) {
            if (!geneIdGeneMap.containsKey(geneIdToEA.getFirst()))
                geneIds.add(geneIdToEA.getFirst());
        }

        Iterable<AtlasGene> solrGenes = atlasSolrDAO.getGenesByIdentifiers(geneIds);
        Map<Long, AtlasGene> solrGeneMap = new HashMap<Long, AtlasGene>();
        for (AtlasGene solrGene : solrGenes)
            solrGeneMap.put(Long.valueOf(solrGene.getGeneId()), solrGene);

        for (Pair<Long, ExpressionAnalysis> geneIdToEA : bestGeneIdsToEA) {
            Long geneId = geneIdToEA.getFirst();
            ExpressionAnalysis ea = geneIdToEA.getSecond();

            AtlasGene gene = geneIdGeneMap.get(geneId);
            if (null == gene) {
                gene = solrGeneMap.get(geneId);
            }

            if (null != gene)
                topGenes.add(Pair.create(gene, ea));
        }

        if (topGenes.isEmpty()) {
            log.error("No top genes could be found in experiment: " + experiment.getAccession());
        }

        log.info("Finished findGenesForExperiment in:  " + (System.currentTimeMillis() - startTime) + " ms");
        return topGenes;
    }

    /**
     * @param expAcc        Experiment Accession
     * @param geneIds       gene ids among which the best gene entries should be found
     * @param pathToNetCDF  full path to the NetCDF file to be searched
     * @param ef            Experimental factor
     * @param efv           Experimental factor value
     * @param udFilter      Up/down expression filter
     * @param sortOrder     Result set sort order
     * @param start         Start position within the result set (related to result set pagination on the experiment page)
     * @param numOfTopGenes topN determines how many top genes should be found, given the specified sortOrder
     * @return List of design element indexes containing the best expression data, according to ef, efv, udFilter, start, numOfTopGenes
     *         found among design element indexes deids in netCDF file, pathToNetCDF.
     */
    private List<Pair<Long, ExpressionAnalysis>> findBestGenesInExperimentR(
            final String expAcc,
            final Set<Long> geneIds,
            final File pathToNetCDF,
            final String ef,
            final String efv,
            final QueryExpression udFilter,
            final QueryResultSortOrder sortOrder,
            final int start,
            final int numOfTopGenes) {
        List<Pair<Long, ExpressionAnalysis>> expressionAnalyses = new ArrayList<Pair<Long, ExpressionAnalysis>>();

        // Create R list of deIds, e.g. "c(1473434,3493430)"
        final String rListOfGeneIds = "c(" + on(",").join(geneIds) + ")";

        // find.best.design.elements <<-
        // function(ncdf, deids=NULL, ef=NULL, efv=NULL, statfilter=NULL, statsort="PVAL", from=1, rows=10) {
        final String callExpGenes = "find.best.design.elements('" +
                pathToNetCDF.getAbsolutePath() + "'," +
                rListOfGeneIds + "," +
                ef + "," +
                efv + ",'" +
                udFilter + "','" +
                sortOrder + "'," +
                start + "," +
                numOfTopGenes + ")";
        try {
            RDataFrame df = computeService.computeTask(new ComputeTask<RDataFrame>() {
                public RDataFrame compute(RServices rs) throws RemoteException {
                    rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));
                    return (RDataFrame) rs.getObject(callExpGenes);
                }
            });

            RInteger deIndexes = (RInteger) df.getData().getValueByName("deindexes");
            RInteger deIds = (RInteger) df.getData().getValueByName("designelements");
            RInteger gnIds = (RInteger) df.getData().getValueByName("geneids");
            RNumeric minPvals = (RNumeric) df.getData().getValueByName("minpvals");
            RNumeric maxTstats = (RNumeric) df.getData().getValueByName("maxtstats");
            RFactor uefvs = (RFactor) df.getData().getValueByName("uefvs");

            if (deIndexes != null) {
                for (int j = 0; j < deIndexes.length(); j++) {
                    Long gn = (long) gnIds.getValue()[j];

                    if (gn == 0)
                        continue;

                    ExpressionAnalysis ea = new ExpressionAnalysis();
                    ea.setDesignElementIndex(deIndexes.getValue()[j] - 1);
                    ea.setDesignElementID(deIds.getValue()[j]);
                    ea.setPValAdjusted((float) minPvals.getValue()[j]);
                    ea.setTStatistic((float) maxTstats.getValue()[j]);
                    ea.setProxyId(pathToNetCDF.getAbsolutePath());

                    String efName = uefvs.asData()[j].split("\\|\\|")[0];
                    String efvName = uefvs.asData()[j].split("\\|\\|")[1];

                    ea.setEfName(efName);
                    ea.setEfvName(efvName);

                    expressionAnalyses.add(
                            Pair.create(gn, ea));
                }
            }
        } catch (Exception e) {
            log.error("Problem retrieving best gene expression data for experiment: " + expAcc + " via function: " + callExpGenes, e);
        }

        return expressionAnalyses;
    }

    private String getRCodeFromResource(String resourcePath) throws ComputeException {
        try {
            return Resources.toString(getClass().getClassLoader().getResource(resourcePath), Charset.defaultCharset());
        } catch (IOException e) {
            throw new ComputeException("Error while reading in R code from " + resourcePath, e);
        }
    }
}
