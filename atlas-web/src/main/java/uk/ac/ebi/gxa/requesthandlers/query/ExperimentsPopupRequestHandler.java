/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.requesthandlers.query;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.Constants;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.microarray.atlas.model.Expression;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;
import static uk.ac.ebi.gxa.statistics.StatisticsType.*;

/**
 * @author pashky
 */
public class ExperimentsPopupRequestHandler extends AbstractRestRequestHandler {

    private AtlasSolrDAO atlasSolrDAO;
    private Efo efo;
    private AtlasProperties atlasProperties;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private AtlasNetCDFDAO atlasNetCDFDAO;

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public Object process(HttpServletRequest request) {

        final Map<Long, AtlasExperiment> expsCache = new HashMap<Long, AtlasExperiment>();

        Map<String, Object> jsResult = new HashMap<String, Object>();

        String geneIdKey = request.getParameter("gene");
        String factor = request.getParameter("ef");
        String factorValue = request.getParameter("efv");

        if (geneIdKey != null && factor != null && factorValue != null) {
            final long geneId = Long.parseLong(geneIdKey);
            boolean isEfo = Constants.EFO_FACTOR_NAME.equals(factor);

            jsResult.put("ef", factor);
            jsResult.put("eftext", atlasProperties.getCuratedEf(factor));
            jsResult.put("efv", factorValue);

            Attribute attr;
            if (isEfo) {
                attr = new EfoAttribute(factorValue, UP_DOWN);
                EfoTerm term = efo.getTermById(factorValue);
                if (term != null) {
                    jsResult.put("efv", term.getTerm());
                }
            } else {
                attr = new EfvAttribute(factor, factorValue, UP_DOWN);
            }

            AtlasSolrDAO.AtlasGeneResult result = atlasSolrDAO.getGeneById(geneId);
            if (!result.isFound()) {
                throw new IllegalArgumentException("Atlas gene " + geneId + " not found");
            }

            AtlasGene gene = result.getGene();

            Map<String, Object> jsGene = new HashMap<String, Object>();

            jsGene.put("id", gene.getGeneId());
            jsGene.put("identifier", gene.getGeneIdentifier());
            jsGene.put("name", gene.getGeneName());
            jsResult.put("gene", jsGene);

            List<Experiment> experiments = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(gene.getGeneId(), attr, -1, -1);

            // Now add non-de experiments
            attr.setStatType(StatisticsType.NON_D_E);
            Set<Experiment> nonDEExps = atlasStatisticsQueryService.getScoringExperimentsForGeneAndAttribute(gene.getGeneId(), attr);
            Map<Experiment, Set<EfvAttribute>> allExpsToAttrs = new HashMap<Experiment, Set<EfvAttribute>>();
            // Gather all experiment-efefv mappings for attr and all its children (if efo)
            Set<Attribute> attrAndChildren = attr.getAttributeAndChildren(efo);
            for (Attribute attribute : attrAndChildren) {
                atlasStatisticsQueryService.getEfvExperimentMappings(attribute, allExpsToAttrs);
            }

            for (Experiment exp : nonDEExps) {
                Experiment key;
                if (allExpsToAttrs.containsKey(exp)) { // attr is an efo
                    key = exp;
                } else if (allExpsToAttrs.containsKey(EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER)) { // attr is an ef-efv
                    key = EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER;
                } else {
                    // We have a nonDE experiment (exp) for (efo or efv) attr;
                    // We also have a map of all experiment-EfvAttributes pairs attr maps to (allExpsToAttrs)
                    // but we cannot find exp in allExpsToAttrs.keySet(), hence we're unable to map attr to at least one EfvAttribute
                    // that we'd like to use as highestRankingAttribute - hence report an error.
                    throw logUnexpected("Failed to retrieve an ef for " + StatisticsType.NON_D_E +
                            " expression in experiment: " + exp.getAccession() + " for attribute: " + attr);
                }
                EfvAttribute highestRankingAttribute = allExpsToAttrs.get(key).iterator().next();
                exp.setHighestRankAttribute(highestRankingAttribute);

                ExpressionAnalysis ea = atlasNetCDFDAO.getBestEAForGeneEfEfvInExperiment(
                        exp.getAccession(), gene.getGeneId(), highestRankingAttribute.getEf(), highestRankingAttribute.getEfv(), Expression.NONDE);

                if (ea != null) {
                    exp.setPvalTstatRank(new PvalTstatRank(ea.getPValAdjusted(), StatisticsQueryUtils.getTStatRank(ea.getTStatistic())));
                    experiments.add(exp);
                } else {
                    log.error("Failed to retrieve an " + StatisticsType.NON_D_E +
                            " ExpressionAnalysis in experiment: " + exp.getAccession() +
                            " (could be due to incorrect mappings in a2_ontologymapping for attribute: " + highestRankingAttribute + ")");
                }
            }

            Map<Long, Map<String, List<Experiment>>> exmap = new HashMap<Long, Map<String, List<Experiment>>>();
            for (Experiment experiment : experiments) {
                Long experimentId = experiment.getExperimentId();
                Map<String, List<Experiment>> efmap = exmap.get(experimentId);
                if (efmap == null) {
                    exmap.put(experimentId, efmap = new HashMap<String, List<Experiment>>());
                }
                List<Experiment> list = efmap.get(experiment.getHighestRankAttribute().getEf());
                if (list == null) {
                    efmap.put(experiment.getHighestRankAttribute().getEf(), list = new ArrayList<Experiment>());
                }

                list.add(experiment);
            }

            // Within each experiment entry, sort expression stats for each ef in asc order (non-de 'NA' pVals last)
            for (Map<String, List<Experiment>> efToExpressionStats : exmap.values()) {
                for (List<Experiment> expressionStatsForEf : efToExpressionStats.values()) {
                    Collections.sort(expressionStatsForEf, new Comparator<Experiment>() {
                        public int compare(Experiment o1, Experiment o2) {
                            if (Float.isNaN(o2.getpValTStatRank().getPValue()))
                                return -1;
                            return o1.getpValTStatRank().compareTo(o2.getpValTStatRank());
                        }
                    });
                }
            }

            @SuppressWarnings("unchecked")

            List<Map.Entry<Long, Map<String, List<Experiment>>>> exps =
                    new ArrayList<Map.Entry<Long, Map<String, List<Experiment>>>>(exmap.entrySet());
            Collections.sort(exps, new Comparator<Map.Entry<Long, Map<String, List<Experiment>>>>() {
                public int compare(Map.Entry<Long, Map<String, List<Experiment>>> o1,
                                   Map.Entry<Long, Map<String, List<Experiment>>> o2) {
                    float minp1 = 1;
                    float maxTstat1 = 0;
                    for (Map.Entry<String, List<Experiment>> ef : o1.getValue().entrySet()) {
                        PvalTstatRank pt = ef.getValue().get(0).getpValTStatRank();
                        if (!Float.isNaN(pt.getPValue())) {
                            minp1 = Math.min(minp1, pt.getPValue());
                            maxTstat1 = Math.max(maxTstat1, Math.abs(pt.getTStatRank()));
                        }
                    }
                    float minp2 = 1;
                    float maxTstat2 = 0;
                    for (Map.Entry<String, List<Experiment>> ef : o2.getValue().entrySet()) {
                        PvalTstatRank pt = ef.getValue().get(0).getpValTStatRank();
                        if (!Float.isNaN(pt.getPValue())) {
                            minp2 = Math.min(minp2, pt.getPValue());
                            maxTstat2 = Math.max(maxTstat2, Math.abs(pt.getTStatRank()));
                        }
                    }
                    // Within non-de only experiments, sort alphabetically by experiment accession
                    if (ExpressionAnalysis.isNo(minp1, maxTstat1) && ExpressionAnalysis.isNo(minp2, maxTstat2)) {
                        AtlasExperiment ae1 = getAtlasExperiment(o1.getKey(), expsCache);
                        AtlasExperiment ae2 = getAtlasExperiment(o2.getKey(), expsCache);
                        return ae1.getAccession().compareTo(ae2.getAccession());
                    }
                    return minp1 < minp2 ? -1 : 1;
                }
            });

            List<Map> jsExps = new ArrayList<Map>();
            for (Map.Entry<Long, Map<String, List<Experiment>>> e : exps) {
                AtlasExperiment aexp = atlasSolrDAO.getExperimentById(e.getKey());
                if (aexp != null) {
                    Map<String, Object> jsExp = new HashMap<String, Object>();
                    jsExp.put("accession", aexp.getAccession());
                    jsExp.put("name", aexp.getDescription());
                    jsExp.put("id", e.getKey());

                    List<Map> jsEfs = new ArrayList<Map>();
                    for (Map.Entry<String, List<Experiment>> ef : e.getValue().entrySet()) {
                        Map<String, Object> jsEf = new HashMap<String, Object>();
                        jsEf.put("ef", ef.getKey());
                        jsEf.put("eftext", atlasProperties.getCuratedEf(ef.getKey()));

                        List<Map> jsEfvs = new ArrayList<Map>();
                        for (Experiment exp : ef.getValue()) {
                            Map<String, Object> jsEfv = new HashMap<String, Object>();
                            boolean isNo = ExpressionAnalysis.isNo(exp.getpValTStatRank().getPValue(), exp.getpValTStatRank().getTStatRank());
                            boolean isUp = ExpressionAnalysis.isUp(exp.getpValTStatRank().getPValue(), exp.getpValTStatRank().getTStatRank());
                            jsEfv.put("efv", exp.getHighestRankAttribute().getEfv());
                            jsEfv.put("isexp", isNo ? "no" : (isUp ? "up" : "dn"));
                            jsEfv.put("pvalue", exp.getpValTStatRank().getPValue());
                            jsEfvs.add(jsEfv);
                        }
                        jsEf.put("efvs", jsEfvs);
                        if (!jsEfvs.isEmpty())
                            jsEfs.add(jsEf);
                    }
                    jsExp.put("efs", jsEfs);
                    jsExps.add(jsExp);
                }
            }

            jsResult.put("experiments", jsExps);

            // TODO: we might be better off with one entity encapsulating the expression stats
            long start = System.currentTimeMillis();
            attr.setStatType(NON_D_E);
            int numNo = atlasStatisticsQueryService.getExperimentCountsForGene(attr, geneId);
            attr.setStatType(UP);
            int numUp = atlasStatisticsQueryService.getExperimentCountsForGene(attr, geneId);
            attr.setStatType(DOWN);
            int numDn = atlasStatisticsQueryService.getExperimentCountsForGene(attr, geneId);
            log.debug("Obtained  counts for gene: " + geneId + " and attribute: " + attr + " in: " + (System.currentTimeMillis() - start) + " ms");

            jsResult.put("numUp", numUp);
            jsResult.put("numDn", numDn);
            jsResult.put("numNo", numNo);

        }

        return jsResult;
    }

    /**
     * @param experimentId
     * @param expsCache
     * @return AtlasExperiment corresponding to experimentId; populate expsCache if AtlasExperiment not already in cache
     */

    private AtlasExperiment getAtlasExperiment(final long experimentId, Map<Long, AtlasExperiment> expsCache) {
        if (!expsCache.containsKey(experimentId)) {
            expsCache.put(experimentId, atlasSolrDAO.getExperimentById(experimentId));
        }
        return expsCache.get(experimentId);
    }
}
