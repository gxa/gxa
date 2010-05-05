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

package ae3.model;

import ae3.dao.AtlasSolrDAO;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.gxa.index.GeneExpressionAnalyticsTable;
import uk.ac.ebi.gxa.utils.*;
import static uk.ac.ebi.gxa.utils.EscapeUtil.nullzero;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.*;

public class AtlasGene {
    private SolrDocument geneSolrDocument;
    private Map<String, List<String>> geneHighlights;
    private GeneExpressionAnalyticsTable expTable;
    private static final String PROPERTY_PREFIX = "property_";

    public AtlasGene(SolrDocument geneDoc) {
        this.geneSolrDocument = geneDoc;
    }

    private String getValue(String name)
    {
        Collection fval = geneSolrDocument.getFieldValues(name);
        if(fval != null)
            return StringUtils.join(fval, ", ");
        return "";
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getValues(String name)
    {
        Collection<Object> r = geneSolrDocument.getFieldValues(name);
        return r == null ? new ArrayList<String>() : (Collection)r;
    }

    private String getHilitValue(String name) {
        List<String> val = geneHighlights.get(name);
        if(val == null || val.size() == 0)
            return StringEscapeUtils.escapeHtml(getValue(name));
        return StringUtils.join(val, ", ");
    }

    public void setGeneHighlights(Map<String, List<String>> geneHighlights) {
        this.geneHighlights = geneHighlights;
    }

    public SolrDocument getGeneSolrDocument() {
        return geneSolrDocument;
    }

    public Map<String,Collection<String>> getGeneProperties() {
        return new LazyMap<String, Collection<String>>() {
            protected Collection<String> map(String key) {
                return getValues(PROPERTY_PREFIX + key);
            }

            protected Iterator<String> keys() {
                return getGenePropertiesIterator();
            }
        };
    }

    public Map<String,String> getHilitGeneProperties() {
        return new LazyMap<String, String>() {
            protected String map(String key) {
                return getHilitValue(PROPERTY_PREFIX + key);
            }

            protected Iterator<String> keys() {
                return getGenePropertiesIterator();
            }
        };
    }

    public Iterable<String> getGenePropertyNames() {
        return new Iterable<String>() {
            public Iterator<String> iterator() {
                return getGenePropertiesIterator();
            }
        };
    }

    public Iterator<String> getGenePropertiesIterator() {
        return new FilterIterator<String, String>(geneSolrDocument.getFieldNames().iterator()) {
            public String map(String name) {
                return name.startsWith(PROPERTY_PREFIX) ? name.substring(PROPERTY_PREFIX.length()) : null;
            }
        };
    }

    public String getGeneId() {
        return getValue("id");
    }

    public String getHilitPropertyValue(String property) {
        return getHilitValue(PROPERTY_PREFIX + property);
    }

    public String getPropertyValue(String property) {
        return getValue(PROPERTY_PREFIX + property);
    }

    public Collection<String> getPropertyValues(String property) {
        return getValues(PROPERTY_PREFIX + property);
    }

    public String getGeneName() {
        return getValue("name");
    }

    public String getHilitGeneName() {
        return getHilitValue("name");
    }

    public String getGeneIdentifier() {
        return getValue("identifier");
    }

    public String getGeneSpecies() {
        return StringUtil.upcaseFirst(getValue("species"));
    }

	public Collection<String> getOrthologs() {
        return getValues("orthologs");
	}

    @SuppressWarnings("unchecked")
    public Set<String> getAllFactorValues(String ef) {
        Set<String> efvs = new HashSet<String>();

        Collection<String> fields = getValues("efvs_up_" + EscapeUtil.encode(ef));
        if(fields!=null)
            efvs.addAll(fields);
        fields = getValues("efvs_dn_" + EscapeUtil.encode(ef));
        if(fields!=null)
            efvs.addAll(fields);
        return efvs;
    }

    public int getCount_up(String ef, String efv) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + EscapeUtil.encode(ef, efv) + "_up"));
    }

    public int getCount_dn(String ef, String efv) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + EscapeUtil.encode(ef, efv) + "_dn"));
    }

    public float getMin_up(String ef, String efv) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_" + EscapeUtil.encode(ef, efv) + "_up"));
    }

    public float getMin_dn(String ef, String efv) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_" + EscapeUtil.encode(ef, efv) + "_dn"));
    }

    public int getCount_up(String efo) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_efo_" + EscapeUtil.encode(efo) + "_up"));
    }

    public int getCount_dn(String efo) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_efo_" + EscapeUtil.encode(efo) + "_dn"));
    }

    public float getMin_up(String efo) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_efo_" + EscapeUtil.encode(efo) + "_up"));
    }

    public float getMin_dn(String efo) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_efo_" + EscapeUtil.encode(efo) + "_dn"));
    }

    public GeneExpressionAnalyticsTable getExpressionAnalyticsTable() {
        if(expTable != null)
            return expTable;

        byte[] eadata = (byte[]) geneSolrDocument.getFieldValue("exp_info");
        if(eadata != null)
            expTable = GeneExpressionAnalyticsTable.deserialize((byte[])geneSolrDocument.getFieldValue("exp_info"));
        else
            expTable = new GeneExpressionAnalyticsTable();

        return expTable;
    }

    public int getNumberOfExperiments() {
        Set<Long> exps = new HashSet<Long>();
        for(ExpressionAnalysis e : getExpressionAnalyticsTable().getAll())
            exps.add(e.getExperimentID());
        return exps.size();
    }

    public Set<String> getAllEfs() {
        Set<String> efs = new HashSet<String>();
        for (String field : getGeneSolrDocument().getFieldNames()) {
            if(field.startsWith("efvs_"))
                efs.add(field.substring(8));
        }
        return efs;
    }

    private Map<Long,AtlasExperiment> experimentsMap;

    public void loadGeneExperiments(AtlasSolrDAO atlasSolrDAO) {
        experimentsMap = new HashMap<Long, AtlasExperiment>();
        for(ExpressionAnalysis exp : getExpressionAnalyticsTable().getAll())
            if(!experimentsMap.containsKey(exp.getExperimentID())) {
                AtlasExperiment aexp = atlasSolrDAO.getExperimentById(String.valueOf(exp.getExperimentID()));
                if(aexp != null)
                    experimentsMap.put(exp.getExperimentID(), aexp);
            }
    }

    public List<ListResultRow> getHeatMapRows(Collection<String> omittedEFs) {
        ListResultRow heatmapRow;
        ArrayList<ListResultRow> heatmap = new ArrayList<ListResultRow>();
        for(String ef : getAllEfs()) {
            Set<String> efvs = getAllFactorValues(ef);
            if(!efvs.isEmpty()){
                for(String efv : efvs) {
                    if(!omittedEFs.contains(ef) && !"V1".equals(efv)){
                        heatmapRow = new ListResultRow(ef, efv,
                                getCount_up(ef, efv),
                                getCount_dn(ef, efv),
                                getMin_up(ef, efv),
                                getMin_dn(ef, efv));
                        heatmapRow.setGene(this);

                        if(experimentsMap != null) {
                            List<ListResultRowExperiment> exps = new ArrayList<ListResultRowExperiment>();
                            for(ExpressionAnalysis exp : getExpressionAnalyticsTable().findByEfEfv(ef, efv)) {
                                AtlasExperiment aexp = experimentsMap.get(exp.getExperimentID());
                                if(aexp != null) {
                                    exps.add(new ListResultRowExperiment(exp.getExperimentID(), 
                                            aexp.getAccession(),
                                            aexp.getDescription(),
                                            exp.getPValAdjusted(),
                                            exp.isUp() ? Expression.UP : Expression.DOWN));
                                }
                            }
                            heatmapRow.setExp_list(exps);
                        }

                        heatmap.add(heatmapRow);
                    }
                }
            }
        }

        Collections.sort(heatmap,Collections.reverseOrder());
        return heatmap;
    }

    public List<ExpressionAnalysis> getTopFVs(long exp_id_key) {
        List<ExpressionAnalysis> result = new ArrayList<ExpressionAnalysis>();
        for(ExpressionAnalysis e : getExpressionAnalyticsTable().findByExperimentId(exp_id_key)) {
            result.add(e);
        }
        Collections.sort(result, new Comparator<ExpressionAnalysis>() {
            public int compare(ExpressionAnalysis o1, ExpressionAnalysis o2) {
                return Float.valueOf(o1.getPValAdjusted()).compareTo(o2.getPValAdjusted());
            }
        });
        return result;
    }

    public List<ExpressionAnalysis> getAtlasResultsForExperiment(long exp_id_key){
        ArrayList<ExpressionAnalysis> result = new ArrayList<ExpressionAnalysis>();
        for(ExpressionAnalysis e : getExpressionAnalyticsTable().findByExperimentId(exp_id_key)){
            result.add(e);
        }
        return result;
    }

    public Pair<String,Float> getHighestRankEF(long experimentId) {
        String ef = null;
        Float pvalue = null;
        for(ExpressionAnalysis e : getExpressionAnalyticsTable().findByExperimentId(experimentId))
            if(pvalue == null || pvalue > e.getPValAdjusted()) {
                pvalue = e.getPValAdjusted();
                ef = e.getEfName();
            }
        return new Pair<String,Float>(ef, pvalue);
    }

    public boolean getHasAnatomogram(){
        if(null==anatomogramEfoList)
            return true;
        for(String term : anatomogramEfoList){
            if(this.getCount_dn(term)>0||this.getCount_up(term)>0)
                return true;
        }
        return false;
    }
    private List<String> anatomogramEfoList = null;

    public void setAnatomogramEfoList(List<String> queryTerms){
        anatomogramEfoList = queryTerms;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        return obj != null && getGeneId().equals(((AtlasGene)obj).getGeneId());
    }

    @Override
    public int hashCode() {
        return geneSolrDocument != null ? geneSolrDocument.hashCode() : 0;
    }
}
