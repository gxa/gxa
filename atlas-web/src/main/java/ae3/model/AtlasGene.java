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
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.Pair;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.gxa.index.GeneExpressionAnalyticsTable;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.utils.StringUtil;
import static uk.ac.ebi.gxa.utils.EscapeUtil.nullzero;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.*;

public class AtlasGene {
    private AtlasProperties atlasProperties;
    private SolrDocument geneSolrDocument;
    private Map<String, List<String>> geneHighlights;
    private GeneExpressionAnalyticsTable expTable;
    private static final String PROPERTY_PREFIX = "property_";

    public AtlasGene(AtlasProperties atlasProperties, SolrDocument geneDoc) {
        this.atlasProperties = atlasProperties;
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
        Map<String,Collection<String>> result = new HashMap<String, Collection<String>>();
        for(String name : geneSolrDocument.getFieldNames())
            if(name.startsWith(PROPERTY_PREFIX)) {
                String property = name.substring(PROPERTY_PREFIX.length());
                result.put(property, getValues(name));
            }
        return result;
    }

    public String getGeneId() {
        return getValue("id");
    }

    public String getHilitInterProTerm() {
        return getHilitValue("property_interproterm");
    }

    public String getHilitGoTerm() {
        return getHilitValue("property_goterm");
    }

    public String getHilitGeneName() {
        return getHilitValue("name");
    }

    public String getHilitKeyword() {
        return getHilitValue("property_keyword");
    }

    public String getHilitSynonym(){
    	return getHilitValue("property_synonym");
    }

    @RestOut(name="name")
    public String getGeneName() {
        return getValue("name");
    }

    @RestOut(name="id")
    public String getGeneIdentifier() {
        return getValue("identifier");
    }

    @RestOut(name="organism")
    public String getGeneSpecies() {
        return StringUtil.upcaseFirst(getValue("species"));
    }

    @RestOut(name="ensemblGeneId", exposeEmpty = false)
    public String getGeneEnsembl() {
        return getValue("property_ensgene");
    }

    @RestOut(name="goTerms", exposeEmpty = false)
    public Collection<String> getGoTerms() {
        return getValues("property_goterm");
    }

    @RestOut(name="interProIds", exposeEmpty = false)
    public Collection<String> getInterProIds() {
        return getValues("property_interpro");
    }

    @RestOut(name="interProTerms", exposeEmpty = false)
    public Collection<String> getInterProTerms() {
        return getValues("property_interproterm");
    }

    @RestOut(name="keywords", exposeEmpty = false)
    public Collection<String> getKeywords() {
        return getValues("property_keyword");
    }

    @RestOut(name="diseases", exposeEmpty = false)
    public Collection<String> getDiseases(){
    	return getValues("property_disease");
    }

    @RestOut(name="uniprotIds", exposeEmpty = false)
    public Collection<String> getUniprotIds(){
    	return getValues("property_uniprot");
    }

    @RestOut(name="synonyms", exposeEmpty = false)
    public Collection<String> getSynonyms(){
    	return getValues("property_synonym");
    }

    @RestOut(name="orthologs", exposeEmpty = false)
	public Collection<String> getOrthologs() {
        return getValues("orthologs");
	}

    @RestOut(name="proteins", exposeEmpty = false)
	public Collection<String> getProteins() { return getValues("property_proteinname"); }

    @RestOut(name="goIds", exposeEmpty = false)
	public Collection<String> getGoIds() { return getValues("property_go"); }

    @RestOut(name="dbxrefs", exposeEmpty = false)
	public Collection<String> getDbxRefs() { return getValues("property_dbxref"); }

    @RestOut(name="emblIds", exposeEmpty = false)
	public Collection<String> getEmblIds() { return getValues("property_embl"); }

    @RestOut(name="ensemblFamilyIds", exposeEmpty = false)
	public Collection<String> getEnsFamilies() { return getValues("property_ensfamily"); }

    @RestOut(name="ensemblProteinIds", exposeEmpty = false)
	public Collection<String> getEnsProteins() { return getValues("property_ensprotein"); }

    @RestOut(name="images", exposeEmpty = false)
	public Collection<String> getImages() { return getValues("property_image"); }

    @RestOut(name="locuslinks", exposeEmpty = false)
	public Collection<String> getLocuslinks() { return getValues("property_locuslink"); }

    @RestOut(name="omimiIds", exposeEmpty = false)
	public Collection<String> getOmimiIds() { return getValues("property_omim"); }

    @RestOut(name="orfIds", exposeEmpty = false)
	public Collection<String> getOrfs() { return getValues("property_orf"); }

    @RestOut(name="refseqIds", exposeEmpty = false)
	public Collection<String> getRefseqIds() { return getValues("property_refseq"); }

    @RestOut(name="unigeneIds", exposeEmpty = false)
	public Collection<String> getUnigeneIds() { return getValues("property_unigene"); }

    @RestOut(name="hmdbIds", exposeEmpty = false)
	public Collection<String> getHmdbIds() { return getValues("property_hmdb"); }

    @RestOut(name="cas", exposeEmpty = false)
	public Collection<String> getCass() { return getValues("property_cas"); }

    @RestOut(name="uniprotMetenzs", exposeEmpty = false)
	public Collection<String> getUniprotMetenzIds() { return getValues("property_uniprometenz"); }

    @RestOut(name="chebiIds", exposeEmpty = false)
	public Collection<String> getChebiIds() { return getValues("property_chebi"); }

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
        return expTable = GeneExpressionAnalyticsTable.deserialize((byte[])geneSolrDocument.getFieldValue("exp_info"));
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

    public String getGeneDescription() {
       return new AtlasGeneDescription(atlasProperties, this).toString();
    }

    public AtlasGeneDescription getGeneDescriptionObject(){
        return new AtlasGeneDescription(atlasProperties, this);
    }
}
