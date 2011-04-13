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

import ae3.dao.ExperimentSolrDAO;
import ae3.dao.GeneSolrDAO;
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
import uk.ac.ebi.gxa.Experiment;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;
import static uk.ac.ebi.gxa.statistics.StatisticsType.*;

/**
 * @author pashky
 */
public class ExperimentsPopupRequestHandler extends AbstractRestRequestHandler {

    private GeneSolrDAO geneSolrDAO;
    private ExperimentSolrDAO experimentSolrDAO;
    private Efo efo;
    private AtlasProperties atlasProperties;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private AtlasNetCDFDAO atlasNetCDFDAO;

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
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
        final Map<Long,Experiment> expsCache = new HashMap<Long,Experiment>();

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

            GeneSolrDAO.AtlasGeneResult result = geneSolrDAO.getGeneById(geneId);
            if (!result.isFound()) {
                throw new IllegalArgumentException("Atlas gene " + geneId + " not found");
            }

            AtlasGene gene = result.getGene();

            Map<String, Object> jsGene = new HashMap<String, Object>();

            jsGene.put("id", gene.getGeneId());
            jsGene.put("identifier", gene.getGeneIdentifier());
            jsGene.put("name", gene.getGeneName());
            jsResult.put("gene", jsGene);

            List<ExperimentInfo> allExperiments = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(gene.getGeneId(), attr, -1, -1);

            // Now add non-de experiments
            attr.setStatType(StatisticsType.NON_D_E);
            Set<ExperimentInfo> nonDEExps = atlasStatisticsQueryService.getScoringExperimentsForGeneAndAttribute(gene.getGeneId(), attr);
            Map<ExperimentInfo, Set<EfvAttribute>> allExpsToAttrs = new HashMap<ExperimentInfo, Set<EfvAttribute>>();
            // Gather all experiment-efefv mappings for attr and all its children (if efo)
            Set<Attribute> attrAndChildren = attr.getAttributeAndChildren(efo);
            for (Attribute attribute : attrAndChildren) {
                atlasStatisticsQueryService.getEfvExperimentMappings(attribute, allExpsToAttrs);
            }
            // Now retrieve PvalTstatRank for each exp in nonDEExps and then add to experiments
            for (ExperimentInfo exp : nonDEExps) {
                ExperimentInfo key;
                if (allExpsToAttrs.containsKey(exp)) { // attr is an efo
                    key = exp;
                } else if (allExpsToAttrs.containsKey(EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER)) { // attr is an ef-efv
                    key = EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER;
                } else {
                    // We know that gene is non-differentially expressed in exp for attr, and yet we cannot find exp
                    // in attr's efv-experiment mappings - report an error
                    throw logUnexpected(
                            gene.getGeneName() + " is non-differentially expressed in " + exp + " for " + attr +
                                    " but this experiment cannot be found in efv-experiment mappings for this Attribute");
                }

                ExpressionAnalysis ea = null;
                // As we don't know exactly which efv that attr maps to for exp has non-de expression, we traverse trough all mapped
                // efvs until we find a non-de expression. For example, in exp E-GEOD-6883, 'normal' (EFO_0000761)  maps to both
                // disease_state:normal (expression: UP) and cell_type:normal (expression: NON_D_E). If we considered just one
                // efv and it happened to be disease_state:normal, we would have failed to find a non-de expression and would
                // have reported an error.
                for (EfvAttribute attrCandidate : allExpsToAttrs.get(key)) {
                    ea = atlasNetCDFDAO.getBestEAForGeneEfEfvInExperiment(
                            exp.getAccession(), gene.getGeneId(), attrCandidate.getEf(), attrCandidate.getEfv(), Expression.NONDE);
                    if (ea != null) {
                        exp.setHighestRankAttribute(attrCandidate);
                        break;
                    }
                }

                if (ea != null) {
                    exp.setPvalTstatRank(new PvalTstatRank(ea.getPValAdjusted(), StatisticsQueryUtils.getTStatRank(ea.getTStatistic())));
                    allExperiments.add(exp);
                } else {
                    throw logUnexpected("Failed to retrieve an " + StatisticsType.NON_D_E +
                            " ExpressionAnalysis for gene: '" + gene.getGeneName() + "' + in experiment: " + exp.getAccession() +
                            " and any attribute in: " + allExpsToAttrs.get(key));
                }

            }

            Map<Long, Map<String, List<ExperimentInfo>>> exmap = new HashMap<Long, Map<String, List<ExperimentInfo>>>();
            for (ExperimentInfo experiment : allExperiments) {
                Long experimentId = experiment.getExperimentId();
                Map<String, List<ExperimentInfo>> efmap = exmap.get(experimentId);
                if (efmap == null) {
                    exmap.put(experimentId, efmap = new HashMap<String, List<ExperimentInfo>>());
                }
                List<ExperimentInfo> list = efmap.get(experiment.getHighestRankAttribute().getEf());
                if (list == null) {
                    efmap.put(experiment.getHighestRankAttribute().getEf(), list = new ArrayList<ExperimentInfo>());
                }

                list.add(experiment);
            }

            // Within each experiment entry, sort expression stats for each ef in asc order (non-de 'NA' pVals last)
            for (Map<String, List<ExperimentInfo>> efToExpressionStats : exmap.values()) {
                for (List<ExperimentInfo> expressionStatsForEf : efToExpressionStats.values()) {
                    Collections.sort(expressionStatsForEf, new Comparator<ExperimentInfo>() {
                        public int compare(ExperimentInfo o1, ExperimentInfo o2) {
                            if (Float.isNaN(o2.getpValTStatRank().getPValue()))
                                return -1;
                            return o1.getpValTStatRank().compareTo(o2.getpValTStatRank());
                        }
                    });
                }
            }

            @SuppressWarnings("unchecked")

            List<Map.Entry<Long, Map<String, List<ExperimentInfo>>>> exps =
                    new ArrayList<Map.Entry<Long, Map<String, List<ExperimentInfo>>>>(exmap.entrySet());
            Collections.sort(exps, new Comparator<Map.Entry<Long, Map<String, List<ExperimentInfo>>>>() {
                public int compare(Map.Entry<Long, Map<String, List<ExperimentInfo>>> o1,
                                   Map.Entry<Long, Map<String, List<ExperimentInfo>>> o2) {
                    float minp1 = 1;
                    float maxTstat1 = 0;
                    for (Map.Entry<String, List<ExperimentInfo>> ef : o1.getValue().entrySet()) {
                        PvalTstatRank pt = ef.getValue().get(0).getpValTStatRank();
                        if (!Float.isNaN(pt.getPValue())) {
                            minp1 = Math.min(minp1, pt.getPValue());
                            maxTstat1 = Math.max(maxTstat1, Math.abs(pt.getTStatRank()));
                        }
                    }
                    float minp2 = 1;
                    float maxTstat2 = 0;
                    for (Map.Entry<String, List<ExperimentInfo>> ef : o2.getValue().entrySet()) {
                        PvalTstatRank pt = ef.getValue().get(0).getpValTStatRank();
                        if (!Float.isNaN(pt.getPValue())) {
                            minp2 = Math.min(minp2, pt.getPValue());
                            maxTstat2 = Math.max(maxTstat2, Math.abs(pt.getTStatRank()));
                        }
                    }
                    // Within non-de only experiments, sort alphabetically by experiment accession
                    if (ExpressionAnalysis.isNo(minp1, maxTstat1) && ExpressionAnalysis.isNo(minp2, maxTstat2)) {
                        Experiment ae1 = getAtlasExperiment(o1.getKey(), expsCache);
                        Experiment ae2 = getAtlasExperiment(o2.getKey(), expsCache);
                        return ae1.getAccession().compareTo(ae2.getAccession());
                    }
                    return minp1 < minp2 ? -1 : 1;
                }
            });

            List<Map> jsExps = new ArrayList<Map>();
            for (Map.Entry<Long, Map<String, List<ExperimentInfo>>> e : exps) {
                Experiment aexp = experimentSolrDAO.getExperimentById(e.getKey());
                if (aexp != null) {
                    Map<String, Object> jsExp = new HashMap<String, Object>();
                    jsExp.put("accession", aexp.getAccession());
                    jsExp.put("name", aexp.getDescription());
                    jsExp.put("id", e.getKey());

                    List<Map> jsEfs = new ArrayList<Map>();
                    for (Map.Entry<String, List<ExperimentInfo>> ef : e.getValue().entrySet()) {
                        Map<String, Object> jsEf = new HashMap<String, Object>();
                        jsEf.put("ef", ef.getKey());
                        jsEf.put("eftext", atlasProperties.getCuratedEf(ef.getKey()));

                        List<Map> jsEfvs = new ArrayList<Map>();
                        for (ExperimentInfo exp : ef.getValue()) {
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
     * @return Experiment corresponding to experimentId; populate expsCache if AtlasExperiment not already in cache
     */

    private Experiment getAtlasExperiment(final long experimentId, Map<Long,Experiment> expsCache) {
        if (!expsCache.containsKey(experimentId)) {
            expsCache.put(experimentId, experimentSolrDAO.getExperimentById(experimentId));
        }
        return expsCache.get(experimentId);
    }
}
