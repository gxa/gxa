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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.requesthandlers.query;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.structuredquery.Constants;
import ae3.util.CuratedTexts;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.index.Experiment;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author pashky
 */
public class ExperimentsPopupRequestHandler extends AbstractRestRequestHandler {

    private AtlasDao dao;
    private Efo efo;

    public AtlasDao getDao() {
        return dao;
    }

    public void setDao(AtlasDao dao) {
        this.dao = dao;
    }

    public Efo getEfo() {
        return efo;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    public Object process(HttpServletRequest request) {
        Map<String, Object> jsResult = new HashMap<String, Object>();

        String geneIdKey = request.getParameter("gene");
        String factor = request.getParameter("ef");
        String factorValue = request.getParameter("efv");

        if (geneIdKey != null && factor != null && factorValue != null) {
            boolean isEfo = Constants.EFO_FACTOR_NAME.equals(factor);

            jsResult.put("ef", factor);
            jsResult.put("eftext", CuratedTexts.get("head.ef." + factor));
            jsResult.put("efv", factorValue);

            if (isEfo) {
                EfoTerm term = efo.getTermById(factorValue);
                if (term != null) {
                    jsResult.put("efv", term.getTerm());
                }
            }

            AtlasDao.AtlasGeneResult result = dao.getGeneById(geneIdKey);
            if (!result.isFound()) {
                throw new IllegalArgumentException("Atlas gene " + geneIdKey + " not found");
            }

            AtlasGene gene = result.getGene();

            Map<String, Object> jsGene = new HashMap<String, Object>();

            jsGene.put("id", geneIdKey);
            jsGene.put("identifier", gene.getGeneIdentifier());
            jsGene.put("name", gene.getGeneName());
            jsResult.put("gene", jsGene);

            Map<Long, Map<String, List<Experiment>>> exmap = new HashMap<Long, Map<String, List<Experiment>>>();
            for (Experiment exp : isEfo ?
                    gene.getExperimentsTable().findByEfoSet(efo.getTermAndAllChildrenIds(factorValue)) :
                    gene.getExperimentsTable().findByEfEfv(factor, factorValue)) {
                Map<String, List<Experiment>> efmap = exmap.get(exp.getId());
                if (efmap == null) {
                    exmap.put(exp.getId(), efmap = new HashMap<String, List<Experiment>>());
                }
                List<Experiment> list = efmap.get(exp.getEf());
                if (list == null) {
                    efmap.put(exp.getEf(), list = new ArrayList<Experiment>());
                }

                list.add(exp);

            }


            for (Map<String, List<Experiment>> ef : exmap.values()) {
                for (List<Experiment> e : ef.values()) {
                    Collections.sort(e, new Comparator<Experiment>() {
                        public int compare(Experiment o1, Experiment o2) {
                            return o1.getPvalue() - o2.getPvalue() < 0 ? -1 : 1;
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
                    double minp1 = 1;
                    for (Map.Entry<String, List<Experiment>> ef : o1.getValue().entrySet()) {
                        minp1 = Math.min(minp1, ef.getValue().get(0).getPvalue());
                    }
                    double minp2 = 1;
                    for (Map.Entry<String, List<Experiment>> ef : o2.getValue().entrySet()) {
                        minp2 = Math.min(minp2, ef.getValue().get(0).getPvalue());
                    }
                    return minp1 < minp2 ? -1 : 1;
                }
            });

            int numUp = 0, numDn = 0;

            List<Map> jsExps = new ArrayList<Map>();
            for (Map.Entry<Long, Map<String, List<Experiment>>> e : exps) {
                AtlasExperiment aexp = dao.getExperimentById(e.getKey());
                if (aexp != null) {
                    Map<String, Object> jsExp = new HashMap<String, Object>();
                    jsExp.put("accession", aexp.getAccession());
                    jsExp.put("name", aexp.getDescription());
                    jsExp.put("id", e.getKey());

                    boolean wasup = false;
                    boolean wasdn = false;
                    List<Map> jsEfs = new ArrayList<Map>();
                    for (Map.Entry<String, List<Experiment>> ef : e.getValue().entrySet()) {
                        Map<String, Object> jsEf = new HashMap<String, Object>();
                        jsEf.put("ef", ef.getKey());
                        jsEf.put("eftext", CuratedTexts.get("head.ef." + ef.getKey()));

                        List<Map> jsEfvs = new ArrayList<Map>();
                        for (Experiment exp : ef.getValue()) {
                            Map<String, Object> jsEfv = new HashMap<String, Object>();
                            jsEfv.put("efv", exp.getEfv());
                            jsEfv.put("isup", exp.getExpression().isUp());
                            jsEfv.put("pvalue", exp.getPvalue());
                            jsEfvs.add(jsEfv);

                            if (exp.getExpression().isUp()) {
                                wasup = true;
                            }
                            else {
                                wasdn = true;
                            }
                        }
                        jsEf.put("efvs", jsEfvs);
                        jsEfs.add(jsEf);
                    }
                    jsExp.put("efs", jsEfs);

                    if (wasup) {
                        ++numUp;
                    }
                    if (wasdn) {
                        ++numDn;
                    }
                    jsExps.add(jsExp);
                }
            }

            jsResult.put("experiments", jsExps);

            jsResult.put("numUp", numUp);
            jsResult.put("numDn", numDn);

        }

        return jsResult;
    }

}
