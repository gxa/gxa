package ae3.model;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.ae3.indexbuilder.Experiment;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;
import uk.ac.ebi.ae3.indexbuilder.IndexField;
import static uk.ac.ebi.ae3.indexbuilder.IndexField.nullzero;

import java.util.*;

import ae3.util.Pair;
import ae3.restresult.RestOut;
import ae3.dao.AtlasDao;

public class AtlasGene {
    private SolrDocument geneSolrDocument;
    private Map<String, List<String>> geneHighlights;
    private ArrayList<AtlasGene> orthoGenes = new ArrayList<AtlasGene>();
    private ExperimentsTable expTable;

    public AtlasGene(SolrDocument geneDoc) {
        this.geneSolrDocument = geneDoc;
    }

    public String getGeneSpecies() {
        Collection fval = geneSolrDocument.getFieldValues("gene_species");
        if(fval != null && fval.size() > 0) {
            String species = (String)fval.iterator().next();
            return species.substring(0, 1).toUpperCase() + species.substring(1, species.length()).toLowerCase();
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
        return getValue("gene_id");
    }

    @RestOut(name="name")
    public String getGeneName() {
        return getValue("gene_name");
    }

    @RestOut(name="id")
    public String getGeneIdentifier() {
        return getValue("gene_identifier");
    }

    @RestOut(name="ensemblGeneId", exposeEmpty = false)
    public String getGeneEnsembl() {
        return getValue("gene_ensgene");
    }

    public String getGoTerm() {
        return getValue("gene_goterm");
    }

    @RestOut(name="goTerms", exposeEmpty = false)
    public Collection<String> getGoTerms() {
        return getValues("gene_goterm");
    }

    public String getShortValue(String name){
    	ArrayList fval = (ArrayList)geneSolrDocument.getFieldValues(name);
    	if(fval.size()>5)
    		return StringUtils.join(fval.subList(0, 5),", ");
    	else
    		return StringUtils.join(fval,", ");
    }

    public boolean fieldAvailable(String field){
    	return geneSolrDocument.getFieldNames().contains(field);
    }

    public String getInterProTerm() {
        return getValue("gene_interproterm");
    }

    @RestOut(name="interProIds", exposeEmpty = false)
    public Collection<String> getInterProIds() {
        return getValues("gene_interproid");
    }

    @RestOut(name="interProTerms", exposeEmpty = false)
    public Collection<String> getInterProTerms() {
        return getValues("gene_interproterm");
    }

    public String getKeyword() {
        return getValue("gene_keyword");
    }

    @RestOut(name="keywords", exposeEmpty = false)
    public Collection<String> getKeywords() {
        return getValues("gene_keyword");
    }

    public String getDisease(){
    	return getValue("gene_disease");
    }

    @RestOut(name="diseases", exposeEmpty = false)
    public Collection<String> getDiseases(){
    	return getValues("gene_disease");
    }

    private String getHilitValue(String name) {
        List<String> val = geneHighlights.get(name);
        if(val == null || val.size() == 0)
            return StringEscapeUtils.escapeHtml(getValue(name));
        return StringUtils.join(val, ", ");
    }

    public String getHilitInterProTerm() {
        return getHilitValue("gene_interproterm");
    }

    public String getHilitGoTerm() {
        return getHilitValue("gene_goterm");
    }

    public String getHilitGeneName() {
        return getHilitValue("gene_name");
    }

    public String getHilitKeyword() {
        return getHilitValue("gene_keyword");
    }

    public void setGeneHighlights(Map<String, List<String>> geneHighlights) {
        this.geneHighlights = geneHighlights;
    }

    public String getShortGOTerms(){
    	return getShortValue("gene_goterm");
    }

    public String getShortInterProTerms(){
    	return getShortValue("gene_interproterm");
    }

    public String getShortDiseases(){
    	return getShortValue("gene_disease");
    }

    public SolrDocument getGeneSolrDocument() {
        return geneSolrDocument;
    }

    public String getUniprotId(){
    	return getValue("gene_uniprot");
    }

    @RestOut(name="uniprotIds", exposeEmpty = false)
    public Collection<String> getUniprotIds(){
    	return getValues("gene_uniprot");
    }

    public String getSynonym(){
    	return getValue("gene_synonym");
    }

    @RestOut(name="synonyms", exposeEmpty = false)
    public Collection<String> getSynonyms(){
    	return getValues("gene_synonym");
    }

    public String getHilitSynonym(){
    	return getHilitValue("gene_synonym");
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

    public HashMap serializeForWebServices() {
        HashMap h = new HashMap();
        SolrDocument gene = this.getGeneSolrDocument();

        if(gene != null) {
            Map m = gene.getFieldValuesMap();
            for (Object key : m.keySet()) {
                Collection<String> s = (Collection<String>) m.get(key);
                h.put(key, StringUtils.join(s, "\t"));
            }
        }

        return h;
    }

	public Set<String> getAllFactorValues(String ef) {
		Set<String> efvs = new HashSet<String>();;

		Collection<String> fields = (Collection)geneSolrDocument.getFieldValues("efvs_up_" + IndexField.encode(ef));
		if(fields!=null)
			efvs.addAll(fields);
		fields = (Collection)geneSolrDocument.getFieldValues("efvs_dn_" + IndexField.encode(ef));
		if(fields!=null)
			efvs.addAll(fields);
		return efvs;
	}

	public String getOrthologsIds() {
		ArrayList orths = (ArrayList)geneSolrDocument.getFieldValues("gene_ortholog");
		return StringUtils.join(orths, "+");
	}

    @RestOut(name="orthologs", exposeEmpty = false)
	public List<String> getOrthologs() {
        Collection orths = geneSolrDocument.getFieldValues("gene_ortholog");
		return orths == null ? new ArrayList<String>() : new ArrayList<String>(orths);
	}

    @RestOut(name="proteins", exposeEmpty = false)
	public Collection<String> getProteins() { return getValues("gene_proteins"); }

    @RestOut(name="goIds", exposeEmpty = false)
	public Collection<String> getGoIds() { return getValues("gene_goid"); }

    @RestOut(name="dbxrefs", exposeEmpty = false)
	public Collection<String> getDbxRefs() { return getValues("gene_dbxref"); }

    @RestOut(name="emblIds", exposeEmpty = false)
	public Collection<String> getEmblIds() { return getValues("gene_embl"); }

    @RestOut(name="ensemblFamilyIds", exposeEmpty = false)
	public Collection<String> getEnsFamilies() { return getValues("gene_ensfamily"); }

    @RestOut(name="ensemblProteinIds", exposeEmpty = false)
	public Collection<String> getEnsProteins() { return getValues("gene_ensprotein"); }

    @RestOut(name="images", exposeEmpty = false)
	public Collection<String> getImages() { return getValues("gene_image"); }

    @RestOut(name="locuslinks", exposeEmpty = false)
	public Collection<String> getLocuslinks() { return getValues("gene_locuslink"); }

    @RestOut(name="omimiIds", exposeEmpty = false)
	public Collection<String> getOmimiIds() { return getValues("gene_omimid"); }

    @RestOut(name="orfIds", exposeEmpty = false)
	public Collection<String> getOrfs() { return getValues("gene_orf"); }

    @RestOut(name="refseqIds", exposeEmpty = false)
	public Collection<String> getRefseqIds() { return getValues("gene_refseq"); }

    @RestOut(name="unigeneIds", exposeEmpty = false)
	public Collection<String> getUnigeneIds() { return getValues("gene_unigene"); }

    @RestOut(name="hmdbIds", exposeEmpty = false)
	public Collection<String> getHmdbIds() { return getValues("gene_hmdb"); }

    @RestOut(name="cas", exposeEmpty = false)
	public Collection<String> getCass() { return getValues("gene_cas"); }

    @RestOut(name="uniprotMetenzs", exposeEmpty = false)
	public Collection<String> getChebiIds() { return getValues("gene_uniprotmetenz"); }

    public int getCount_up(String ef, String efv) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + IndexField.encode(ef, efv) + "_up"));
    }

    public int getCount_dn(String ef, String efv) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + IndexField.encode(ef, efv) + "_dn"));
    }

    public double getMin_up(String ef, String efv) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_" + IndexField.encode(ef, efv) + "_up"));
    }

    public double getMin_dn(String ef, String efv) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_" + IndexField.encode(ef, efv) + "_dn"));
    }

    public int getCount_up(String efo) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_efo_" + IndexField.encode(efo) + "_up"));
    }

    public int getCount_dn(String efo) {
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_efo_" + IndexField.encode(efo) + "_dn"));
    }

    public double getMin_up(String efo) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_efo_" + IndexField.encode(efo) + "_up"));
    }

    public double getMin_dn(String efo) {
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_efo_" + IndexField.encode(efo) + "_dn"));
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
                                            aexp.getDwExpAccession(),
                                            aexp.getDwExpDescription(),
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

    public List<AtlasTuple> getTopFVs(long exp_id_key) {
        List<AtlasTuple> result = new ArrayList<AtlasTuple>();
        for(Experiment e : getExperimentsTable().findByExperimentId(exp_id_key)) {
            result.add(new AtlasTuple(e.getEf(), e.getEfv(), e.getExpression().isUp() ? 1 : -1, e.getPvalue()));
        }
        Collections.sort(result, new Comparator<AtlasTuple>() {
            public int compare(AtlasTuple o1, AtlasTuple o2) {
                return o1.getPval().compareTo(o2.getPval());
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
