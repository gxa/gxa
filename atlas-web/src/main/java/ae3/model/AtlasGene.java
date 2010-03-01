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

package ae3.model;

import ae3.dao.AtlasDao;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.Pair;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.gxa.index.Experiment;
import uk.ac.ebi.gxa.index.ExperimentsTable;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.utils.StringUtil;
import static uk.ac.ebi.gxa.utils.EscapeUtil.nullzero;

import java.util.*;

public class AtlasGene {
    private SolrDocument geneSolrDocument;
    private Map<String, List<String>> geneHighlights;
    private ArrayList<AtlasGene> orthoGenes = new ArrayList<AtlasGene>();
    private ExperimentsTable expTable;

    public AtlasGene(SolrDocument geneDoc) {
        this.geneSolrDocument = geneDoc;
    }

    public String getGeneSpecies() {
        Collection fval = geneSolrDocument.getFieldValues("species");
        if(fval != null && fval.size() > 0) {
            String species = (String)fval.iterator().next();
            return StringUtil.upcaseFirst(species);
        }
        return "";
    }

    private String getValue(String name)
    {
        Collection fval = geneSolrDocument.getFieldValues(name);
        if(fval != null)
            return StringUtils.join(fval, ", ");
        return "";
    }

    private Collection<String> getValues(String name)
    {
        return (Collection)geneSolrDocument.getFieldValues(name);
    }

    public String getGeneId() {
        return getValue("id");
    }

    @RestOut(name="name")
    public String getGeneName() {
        return getValue("name");
    }

    @RestOut(name="id")
    public String getGeneIdentifier() {
        return getValue("identifier");
    }

    @RestOut(name="ensemblGeneId", exposeEmpty = false)
    public String getGeneEnsembl() {
        return getValue("property_ENSGENE");
    }

    public String getGoTerm() {
        return getValue("property_GOTERM");
    }

    @RestOut(name="goTerms", exposeEmpty = false)
    public Collection<String> getGoTerms() {
        return getValues("property_GOTERM");
    }

    public String getShortValue(String name){
    	ArrayList fval = (ArrayList)geneSolrDocument.getFieldValues(name);
        if(fval == null)
            return "";
    	if(fval.size()>5)
    		return StringUtils.join(fval.subList(0, 5),", ");
    	else
    		return StringUtils.join(fval,", ");
    }

    public String getInterProTerm() {
        return getValue("property_INTERPROTERM");
    }

    @RestOut(name="interProIds", exposeEmpty = false)
    public Collection<String> getInterProIds() {
        return getValues("property_INTERPRO");
    }

    @RestOut(name="interProTerms", exposeEmpty = false)
    public Collection<String> getInterProTerms() {
        return getValues("property_INTERPRO");
    }

    public String getKeyword() {
        return getValue("property_KEYWORD");
    }

    @RestOut(name="keywords", exposeEmpty = false)
    public Collection<String> getKeywords() {
        return getValues("property_KEYWORD");
    }

    public String getDisease(){
    	return getValue("property_DISEASE");
    }

    @RestOut(name="diseases", exposeEmpty = false)
    public Collection<String> getDiseases(){
    	return getValues("property_DISEASE");
    }

    public void setGeneHighlights(Map<String, List<String>> geneHighlights) {
        this.geneHighlights = geneHighlights;
    }

    private String getHilitValue(String name) {
        List<String> val = geneHighlights.get(name);
        if(val == null || val.size() == 0)
            return StringEscapeUtils.escapeHtml(getValue(name));
        return StringUtils.join(val, ", ");
    }

    public String getHilitInterProTerm() {
        return getHilitValue("property_INTERPROTERM");
    }

    public String getHilitGoTerm() {
        return getHilitValue("property_GOTERM");
    }

    public String getHilitGeneName() {
        return getHilitValue("name");
    }

    public String getHilitKeyword() {
        return getHilitValue("property_KEYWORD");
    }

    public String getShortGOTerms(){
    	return getShortValue("property_GOTERM");
    }

    public String getShortInterProTerms(){
    	return getShortValue("property_INTERPROTERM");
    }

    public String getShortDiseases(){
    	return getShortValue("property_DISEASE");
    }

    public SolrDocument getGeneSolrDocument() {
        return geneSolrDocument;
    }

    public String getUniprotId(){
    	return getValue("property_UNIPROT");
    }

    @RestOut(name="uniprotIds", exposeEmpty = false)
    public Collection<String> getUniprotIds(){
    	return getValues("property_UNIPROT");
    }

    public String getSynonym(){
    	return getValue("property_SYNONYM");
    }

    @RestOut(name="synonyms", exposeEmpty = false)
    public Collection<String> getSynonyms(){
    	return getValues("property_SYNONYM");
    }

    public String getHilitSynonym(){
    	return getHilitValue("property_SYNONYM");
    }

    public String getGeneHighlightStringForHtml() {

        if(geneHighlights == null)
            return "";

        Set<String> hls = new HashSet<String>();
        for (String hlf : geneHighlights.keySet()) {
            hls.add(hlf + ": " + StringUtils.join(geneHighlights.get(hlf), ";"));
        }

        if(hls.size() > 0)
            return StringUtils.join(hls,"<br/>");

        return "";
    }

    public Set<String> getAllFactorValues(String ef) {
		Set<String> efvs = new HashSet<String>();;

		Collection<String> fields = (Collection)geneSolrDocument.getFieldValues("efvs_up_" + EscapeUtil.encode(ef));
		if(fields!=null)
			efvs.addAll(fields);
		fields = (Collection)geneSolrDocument.getFieldValues("efvs_dn_" + EscapeUtil.encode(ef));
		if(fields!=null)
			efvs.addAll(fields);
		return efvs;
	}

	public String getOrthologsIds() {
		ArrayList orths = (ArrayList)geneSolrDocument.getFieldValues("property_ORTHOLOG");
		return StringUtils.join(orths, "+");
	}

    @RestOut(name="orthologs", exposeEmpty = false)
	public List<String> getOrthologs() {
        Collection orths = geneSolrDocument.getFieldValues("property_ORTHOLOG");
		return orths == null ? new ArrayList<String>() : new ArrayList<String>(orths);
	}

    @RestOut(name="proteins", exposeEmpty = false)
	public Collection<String> getProteins() { return getValues("property_PROTEINNAME"); }

    @RestOut(name="goIds", exposeEmpty = false)
	public Collection<String> getGoIds() { return getValues("property_GO"); }

    @RestOut(name="dbxrefs", exposeEmpty = false)
	public Collection<String> getDbxRefs() { return getValues("property_DBXREF"); }

    @RestOut(name="emblIds", exposeEmpty = false)
	public Collection<String> getEmblIds() { return getValues("property_EMBL"); }

    @RestOut(name="ensemblFamilyIds", exposeEmpty = false)
	public Collection<String> getEnsFamilies() { return getValues("property_ENSFAMILY"); }

    @RestOut(name="ensemblProteinIds", exposeEmpty = false)
	public Collection<String> getEnsProteins() { return getValues("property_ENSPROTEIN"); }

    @RestOut(name="images", exposeEmpty = false)
	public Collection<String> getImages() { return getValues("property_IMAGE"); }

    @RestOut(name="locuslinks", exposeEmpty = false)
	public Collection<String> getLocuslinks() { return getValues("property_LOCUSLINK"); }

    @RestOut(name="omimiIds", exposeEmpty = false)
	public Collection<String> getOmimiIds() { return getValues("property_OMIM"); }

    @RestOut(name="orfIds", exposeEmpty = false)
	public Collection<String> getOrfs() { return getValues("property_ORF"); }

    @RestOut(name="refseqIds", exposeEmpty = false)
	public Collection<String> getRefseqIds() { return getValues("property_REFSEQ"); }

    @RestOut(name="unigeneIds", exposeEmpty = false)
	public Collection<String> getUnigeneIds() { return getValues("property_UNIGENE"); }

    @RestOut(name="hmdbIds", exposeEmpty = false)
	public Collection<String> getHmdbIds() { return getValues("property_HMDB"); }

    @RestOut(name="cas", exposeEmpty = false)
	public Collection<String> getCass() { return getValues("property_CAS"); }

    @RestOut(name="uniprotMetenzs", exposeEmpty = false)
	public Collection<String> getUniprotMetenzIds() { return getValues("property_UNIPROTMETENZ"); }

    @RestOut(name="chebiIds", exposeEmpty = false)
	public Collection<String> getChebiIds() { return getValues("property_CHEBI"); }

    public int getCount_up(String ef, String efv) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + EscapeUtil.encode(ef, efv) + "_up"));
    }

    public int getCount_dn(String ef, String efv) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + EscapeUtil.encode(ef, efv) + "_dn"));
    }

    public double getMin_up(String ef, String efv) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_" + EscapeUtil.encode(ef, efv) + "_up"));
    }

    public double getMin_dn(String ef, String efv) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_" + EscapeUtil.encode(ef, efv) + "_dn"));
    }

    public int getCount_up(String efo) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_efo_" + EscapeUtil.encode(efo) + "_up"));
    }

    public int getCount_dn(String efo) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_efo_" + EscapeUtil.encode(efo) + "_dn"));
    }

    public double getMin_up(String efo) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_efo_" + EscapeUtil.encode(efo) + "_up"));
    }

    public double getMin_dn(String efo) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_efo_" + EscapeUtil.encode(efo) + "_dn"));
    }

	public void addOrthoGene(AtlasGene ortho){
		this.orthoGenes.add(ortho);
	}

	public ArrayList<AtlasGene> getOrthoGenes(){
		return this.orthoGenes;
	}

    public ExperimentsTable getExperimentsTable() {
        if(expTable != null)
            return expTable;
        return expTable = ExperimentsTable.deserialize((String)geneSolrDocument.getFieldValue("exp_info"));
    }

    public int getNumberOfExperiments() {
        Set<Long> exps = new HashSet<Long>();
        for(Experiment e : getExperimentsTable().getAll())
            exps.add(e.getId());
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

    private static final String omittedEFs = "age,individual,time,dose,V1";

    private Map<Long,AtlasExperiment> experimentsMap;

    public void loadGeneExperiments(AtlasDao dao) {
        experimentsMap = new HashMap<Long, AtlasExperiment>();
        for(Experiment exp : getExperimentsTable().getAll())
            if(!experimentsMap.containsKey(exp.getId())) {
                AtlasExperiment aexp = dao.getExperimentById(String.valueOf(exp.getId()));
                if(aexp != null)
                    experimentsMap.put(exp.getId(), aexp);
            }
    }

    public List<ListResultRow> getHeatMapRows() {
        ListResultRow heatmapRow;
        ArrayList<ListResultRow> heatmap = new ArrayList<ListResultRow>();
        for(String ef : getAllEfs()) {
            Set<String> efvs = getAllFactorValues(ef);
            if(!efvs.isEmpty()){
                for(String efv : efvs) {
                    if(!omittedEFs.contains(efv) && !omittedEFs.contains(ef)){
                        heatmapRow = new ListResultRow(ef, efv,
                                getCount_up(ef, efv),
                                getCount_dn(ef, efv),
                                getMin_up(ef, efv),
                                getMin_dn(ef, efv));
                        heatmapRow.setGene(this);

                        if(experimentsMap != null) {
                            List<ListResultRowExperiment> exps = new ArrayList<ListResultRowExperiment>();
                            for(Experiment exp : getExperimentsTable().findByEfEfv(ef, efv)) {
                                AtlasExperiment aexp = experimentsMap.get(exp.getId());
                                if(aexp != null) {
                                    exps.add(new ListResultRowExperiment(exp.getId(), 
                                            aexp.getAccession(),
                                            aexp.getDescription(),
                                            exp.getPvalue(), exp.getExpression()));
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

    public List<Experiment> getTopFVs(long exp_id_key) {
        List<Experiment> result = new ArrayList<Experiment>();
        for(Experiment e : getExperimentsTable().findByExperimentId(exp_id_key)) {
            result.add(e);
        }
        Collections.sort(result, new Comparator<Experiment>() {
            public int compare(Experiment o1, Experiment o2) {
                return Double.valueOf(o1.getPvalue()).compareTo(o2.getPvalue());
            }
        });
        return result;
    }

    public List<Experiment> getAtlasResultsForExperiment(long exp_id_key){
        ArrayList<Experiment> result = new ArrayList<Experiment>();
        for(Experiment e : getExperimentsTable().findByExperimentId(exp_id_key)){
            result.add(e);
        }
        return result;
    }

    public Pair<String,Double> getHighestRankEF(long experimentId) {
        String ef = null;
        Double pvalue = null;
        for(Experiment e : getExperimentsTable().findByExperimentId(experimentId))
            if(pvalue == null || pvalue > e.getPvalue()) {
                pvalue = e.getPvalue();
                ef = e.getEf();
            }
        return new Pair<String,Double>(ef, pvalue);
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

    public String getGeneDescription(){
       return (new AtlasGeneDescription(this)).toString();
    }

    public String getGeneDescriptionHtml(){
       return (new AtlasGeneDescription(this)).toString();
    }
}
