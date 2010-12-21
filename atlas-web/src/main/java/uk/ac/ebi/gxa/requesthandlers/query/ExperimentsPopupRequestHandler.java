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
import ae3.service.structuredquery.Constants;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author pashky
 */
public class ExperimentsPopupRequestHandler extends AbstractRestRequestHandler {

    private AtlasSolrDAO atlasSolrDAO;
    private Efo efo;
    private AtlasProperties atlasProperties;

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public Object process(HttpServletRequest request) {
        Map<String, Object> jsResult = new HashMap<String, Object>();

        String geneIdKey = request.getParameter("gene");
        String factor = request.getParameter("ef");
        String factorValue = request.getParameter("efv");

        if (geneIdKey != null && factor != null && factorValue != null) {
            boolean isEfo = Constants.EFO_FACTOR_NAME.equals(factor);

            jsResult.put("ef", factor);
            jsResult.put("eftext", atlasProperties.getCuratedEf(factor));
            jsResult.put("efv", factorValue);

            if (isEfo) {
                EfoTerm term = efo.getTermById(factorValue);
                if (term != null) {
                    jsResult.put("efv", term.getTerm());
                }
            }

            AtlasSolrDAO.AtlasGeneResult result = atlasSolrDAO.getGeneById(geneIdKey);
            if (!result.isFound()) {
                throw new IllegalArgumentException("Atlas gene " + geneIdKey + " not found");
            }

            AtlasGene gene = result.getGene();

            Map<String, Object> jsGene = new HashMap<String, Object>();

            jsGene.put("id", geneIdKey);
            jsGene.put("identifier", gene.getGeneIdentifier());
            jsGene.put("name", gene.getGeneName());
            jsResult.put("gene", jsGene);

            Map<Long, Map<String, List<ExpressionAnalysis>>> exmap = new HashMap<Long, Map<String, List<ExpressionAnalysis>>>();
            for (ExpressionAnalysis exp : isEfo ?
                    gene.getExpressionAnalyticsTable().findByEfoSet(efo.getTermAndAllChildrenIds(factorValue)) :
                    gene.getExpressionAnalyticsTable().findByEfEfv(factor, factorValue)) {
                Map<String, List<ExpressionAnalysis>> efmap = exmap.get(exp.getExperimentID());
                if (efmap == null) {
                    exmap.put(exp.getExperimentID(), efmap = new HashMap<String, List<ExpressionAnalysis>>());
                }
                List<ExpressionAnalysis> list = efmap.get(exp.getEfName());
                if (list == null) {
                    efmap.put(exp.getEfName(), list = new ArrayList<ExpressionAnalysis>());
                }

                list.add(exp);

            }


            for (Map<String, List<ExpressionAnalysis>> ef : exmap.values()) {
                for (List<ExpressionAnalysis> e : ef.values()) {
                    Collections.sort(e, new Comparator<ExpressionAnalysis>() {
                        public int compare(ExpressionAnalysis o1, ExpressionAnalysis o2) {
                            return o1.getPValAdjusted() - o2.getPValAdjusted() < 0 ? -1 : 1;
                        }
                    });
                }
            }

            @SuppressWarnings("unchecked")

            List<Map.Entry<Long, Map<String, List<ExpressionAnalysis>>>> exps =
                    new ArrayList<Map.Entry<Long, Map<String, List<ExpressionAnalysis>>>>(exmap.entrySet());
            Collections.sort(exps, new Comparator<Map.Entry<Long, Map<String, List<ExpressionAnalysis>>>>() {
                public int compare(Map.Entry<Long, Map<String, List<ExpressionAnalysis>>> o1,
                                   Map.Entry<Long, Map<String, List<ExpressionAnalysis>>> o2) {
                    double minp1 = 1;
                    for (Map.Entry<String, List<ExpressionAnalysis>> ef : o1.getValue().entrySet()) {
                        minp1 = Math.min(minp1, ef.getValue().get(0).getPValAdjusted());
                    }
                    double minp2 = 1;
                    for (Map.Entry<String, List<ExpressionAnalysis>> ef : o2.getValue().entrySet()) {
                        minp2 = Math.min(minp2, ef.getValue().get(0).getPValAdjusted());
                    }
                    return minp1 < minp2 ? -1 : 1;
                }
            });

            int numUp = 0, numDn = 0, numNo = 0;

            List<Map> jsExps = new ArrayList<Map>();
            for (Map.Entry<Long, Map<String, List<ExpressionAnalysis>>> e : exps) {
                AtlasExperiment aexp = atlasSolrDAO.getExperimentById(e.getKey());
                if (aexp != null) {
                    Map<String, Object> jsExp = new HashMap<String, Object>();
                    jsExp.put("accession", aexp.getAccession());
                    jsExp.put("name", aexp.getDescription());
                    jsExp.put("id", e.getKey());

                    boolean wasup = false;
                    boolean wasdn = false;
                    boolean wasno = false;
                    List<Map> jsEfs = new ArrayList<Map>();
                    for (Map.Entry<String, List<ExpressionAnalysis>> ef : e.getValue().entrySet()) {
                        Map<String, Object> jsEf = new HashMap<String, Object>();
                        jsEf.put("ef", ef.getKey());
                        jsEf.put("eftext", atlasProperties.getCuratedEf(ef.getKey()));

                        List<Map> jsEfvs = new ArrayList<Map>();
                        for (ExpressionAnalysis exp : ef.getValue()) {
                            Map<String, Object> jsEfv = new HashMap<String, Object>();
                            jsEfv.put("efv", exp.getEfvName());
                            jsEfv.put("isexp", exp.isNo() ? "no" : (exp.isUp() ? "up" : "dn"));
                            jsEfv.put("pvalue", exp.getPValAdjusted());
                            jsEfvs.add(jsEfv);

                            if(exp.isNo())
                                wasno = true;
                            else {
                                if (exp.isUp()) {
                                    wasup = true;
                                }
                                else {
                                    wasdn = true;
                                }
                            }
                        }
                        jsEf.put("efvs", jsEfvs);
                        if(!jsEfvs.isEmpty())
                            jsEfs.add(jsEf);
                    }
                    jsExp.put("efs", jsEfs);

                    if (wasup) {
                        ++numUp;
                    }
                    if (wasdn) {
                        ++numDn;
                    }
                    if (wasno) {
                        ++numNo;
                    }
                    jsExps.add(jsExp);
                }
            }

            jsResult.put("experiments", jsExps);

            jsResult.put("numUp", numUp);
            jsResult.put("numDn", numDn);
            jsResult.put("numNo", numNo);

        }

        return jsResult;
    }

}
