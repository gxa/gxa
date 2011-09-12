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

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.Constants;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static uk.ac.ebi.gxa.statistics.StatisticsType.*;

/**
 * @author pashky
 */
public class ExperimentsPopupRequestHandler extends AbstractRestRequestHandler {
    private GeneSolrDAO geneSolrDAO;
    private ExperimentDAO experimentDAO;
    private Efo efo;
    private AtlasProperties atlasProperties;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private AtlasDataDAO atlasDataDAO;

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setExperimentDAO(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
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

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }

    public Object process(HttpServletRequest request) {
        Map<String, Object> jsResult = new HashMap<String, Object>();

        String bioEntityIdKey = request.getParameter("gene");
        String factor = request.getParameter("ef");
        String factorValue = request.getParameter("efv");

        if (bioEntityIdKey != null && factor != null && factorValue != null) {
            final Integer bioEntityId = Integer.parseInt(bioEntityIdKey);
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

            GeneSolrDAO.AtlasGeneResult result = geneSolrDAO.getGeneById(bioEntityId);
            if (!result.isFound()) {
                throw new IllegalArgumentException("Atlas gene " + bioEntityId + " not found");
            }

            AtlasGene gene = result.getGene();

            Map<String, Object> jsGene = new HashMap<String, Object>();

            jsGene.put("id", gene.getGeneId());
            jsGene.put("identifier", gene.getGeneIdentifier());
            jsGene.put("name", gene.getGeneName());
            jsResult.put("gene", jsGene);

            List<ExperimentResult> allExperiments = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(gene.getGeneId(), attr, -1, -1);

            // Now find non-de experiments
            attr = attr.withStatType(StatisticsType.NON_D_E);
            List<ExperimentResult> nonDEExps = toResults(atlasStatisticsQueryService.getScoringExperimentsForBioEntityAndAttribute(gene.getGeneId(), attr));
            // ...and sort found nonDE experiments alphabetically by accession
            Collections.sort(nonDEExps, new Comparator<ExperimentResult>() {
                public int compare(ExperimentResult e1, ExperimentResult e2) {
                    return e1.getAccession().compareTo(e2.getAccession());
                }
            });

            // Now retrieve (from netcdfs) PTRank for each exp in nonDEExps and then add to allExperiments
            Map<ExperimentInfo, Set<EfvAttribute>> allExpsToAttrs = newHashMap();
            // Gather all experiment-efefv mappings for attr and all its children (if efo)
            Set<Attribute> attrAndChildren = attr.getAttributeAndChildren(efo);
            for (Attribute attribute : attrAndChildren) {
                atlasStatisticsQueryService.getEfvExperimentMappings(attribute, allExpsToAttrs);
            }
            for (ExperimentResult exp : nonDEExps) {
                ExperimentInfo key;
                if (allExpsToAttrs.containsKey(exp.getExperimentInfo())) { // attr is an efo
                    key = exp.getExperimentInfo();
                } else if (allExpsToAttrs.containsKey(EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER)) { // attr is an ef-efv
                    key = EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER;
                } else {
                    // We know that gene is non-differentially expressed in exp for attr, and yet we cannot find exp
                    // in attr's efv-experiment mappings - report an error
                    throw LogUtil.createUnexpected(
                            gene.getGeneName() + " is non-differentially expressed in " + exp + " for " + attr +
                                    " but this experiment cannot be found in efv-experiment mappings for this Attribute");
                }

                ExpressionAnalysis ea = null;
                // As we don't know exactly which efv that attr maps to for exp has non-de expression, we traverse trough all mapped
                // efvs until we find a non-de expression. For example, in exp E-GEOD-6883, 'normal' (EFO_0000761)  maps to both
                // disease_state:normal (expression: UP) and cell_type:normal (expression: NON_D_E). If we considered just one
                // efv and it happened to be disease_state:normal, we would have failed to find a non-de expression and would
                // have reported an error.
                ExperimentWithData ewd = null;
                try {
                    Experiment experiment = experimentDAO.getByName(exp.getAccession());
                    ewd = atlasDataDAO.createExperimentWithData(experiment);

                    for (EfvAttribute attrCandidate : allExpsToAttrs.get(key)) {
                        ea = ewd.getBestEAForGeneEfEfvInExperiment((long) gene.getGeneId(), attrCandidate.getEf(), attrCandidate.getEfv(), UpDownCondition.CONDITION_NONDE);
                        if (ea != null) {
                            exp.setHighestRankAttribute(attrCandidate);
                            break;
                        }
                    }
                } catch (RecordNotFoundException e) {
                    throw LogUtil.createUnexpected(e.getMessage());
                } finally {
                    ewd.closeAllDataSources();
                }

                if (ea != null) {
                    final float p = ea.getPValAdjusted();
                    final float t = ea.getTStatistic();
                    exp.setPValTstatRank(PTRank.of(p, t));
                    allExperiments.add(exp); // Add nonDE expression statistic to allExperiments
                } else {
                    throw LogUtil.createUnexpected("Failed to retrieve an " + StatisticsType.NON_D_E +
                            " ExpressionAnalysis for gene: '" + gene.getGeneName() + "' + in experiment: " + exp.getAccession() +
                            " and any attribute in: " + allExpsToAttrs.get(key));
                }
            }

            // Group all expression statistics per each experiment (Use LinkedHashMap to preserve ordering of experiment stat entries in allExperiments)
            Map<Long, Map<String, List<ExperimentResult>>> exmap = newLinkedHashMap();
            for (ExperimentResult experimentInfo : allExperiments) {
                Long experimentId = experimentInfo.getExperimentId();
                Map<String, List<ExperimentResult>> efmap = exmap.get(experimentId);
                if (efmap == null) {
                    exmap.put(experimentId, efmap = new HashMap<String, List<ExperimentResult>>());
                }
                List<ExperimentResult> list = efmap.get(experimentInfo.getHighestRankAttribute().getEf());
                if (list == null) {
                    efmap.put(experimentInfo.getHighestRankAttribute().getEf(), list = new ArrayList<ExperimentResult>());
                }

                list.add(experimentInfo);
            }

            // Within each experiment entry, sort expression stats for each ef in asc order (non-de 'NA' pVals last)
            for (Map<String, List<ExperimentResult>> efToExpressionStats : exmap.values()) {
                for (List<ExperimentResult> expressionStatsForEf : efToExpressionStats.values()) {
                    Collections.sort(expressionStatsForEf, new Comparator<ExperimentResult>() {
                        public int compare(ExperimentResult o1, ExperimentResult o2) {
                            if (Float.isNaN(o2.getPValTStatRank().getPValue()))
                                return -1;
                            return o1.getPValTStatRank().compareTo(o2.getPValTStatRank());
                        }
                    });
                }
            }

            List<Map.Entry<Long, Map<String, List<ExperimentResult>>>> exps =
                    newArrayList(exmap.entrySet());
            List<Map> jsExps = new ArrayList<Map>();
            for (Map.Entry<Long, Map<String, List<ExperimentResult>>> e : exps) {
                Experiment aexp = experimentDAO.getById(e.getKey());
                if (aexp != null) {
                    Map<String, Object> jsExp = new HashMap<String, Object>();
                    jsExp.put("accession", aexp.getAccession());
                    jsExp.put("name", aexp.getDescription());
                    jsExp.put("id", e.getKey());

                    List<Map> jsEfs = new ArrayList<Map>();
                    for (Map.Entry<String, List<ExperimentResult>> ef : e.getValue().entrySet()) {
                        Map<String, Object> jsEf = new HashMap<String, Object>();
                        jsEf.put("ef", ef.getKey());
                        jsEf.put("eftext", atlasProperties.getCuratedEf(ef.getKey()));

                        List<Map> jsEfvs = new ArrayList<Map>();
                        for (ExperimentResult exp : ef.getValue()) {
                            Map<String, Object> jsEfv = new HashMap<String, Object>();
                            UpDownExpression upDown = UpDownExpression.valueOf(exp.getPValTStatRank().getPValue(), exp.getPValTStatRank().getTStatRank());
                            jsEfv.put("efv", exp.getHighestRankAttribute().getEfv());
                            jsEfv.put("isexp", upDown.isUpOrDown() ? (upDown.isUp() ? "up" : "dn") : "no");
                            jsEfv.put("pvalue", exp.getPValTStatRank().getPValue());
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
            attr = attr.withStatType(NON_D_E);
            int numNo = atlasStatisticsQueryService.getExperimentCountsForBioEntity(attr, bioEntityId);

            attr = attr.withStatType(UP);
            int numUp = atlasStatisticsQueryService.getExperimentCountsForBioEntity(attr, bioEntityId);

            attr = attr.withStatType(DOWN);
            int numDn = atlasStatisticsQueryService.getExperimentCountsForBioEntity(attr, bioEntityId);
            log.debug("Obtained  counts for gene: " + bioEntityId + " and attribute: " + attr + " in: " + (System.currentTimeMillis() - start) + " ms");

            jsResult.put("numUp", numUp);
            jsResult.put("numDn", numDn);
            jsResult.put("numNo", numNo);

        }

        return jsResult;
    }

    private List<ExperimentResult> toResults(Collection<ExperimentInfo> experimentInfos) {
        ArrayList<ExperimentResult> results = newArrayList();
        for (ExperimentInfo ei : experimentInfos) {
            results.add(new ExperimentResult(ei));
        }
        return results;
    }
}
