package ae3.model;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.ae3.indexbuilder.ExperimentsTable;
import uk.ac.ebi.ae3.indexbuilder.IndexField;

import java.util.*;

public class AtlasGene {
    private SolrDocument geneSolrDocument;
    private Map<String, List<String>> geneHighlights;
    private ArrayList<AtlasGene> orthoGenes = new ArrayList<AtlasGene>();

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

    public String getGeneId() {
        return getValue("gene_id");
    }

    public String getGeneName() {
        return getValue("gene_name");
    }

    public String getGeneIdentifier() {
        return getValue("gene_identifier");
    }

    public String getGeneEnsembl() {
        return getValue("gene_ensgene");
    }

    public String getGoTerm() {
        return getValue("gene_goterm");
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

    public String getKeyword() {
        return getValue("gene_keyword");
    }

    public String getDisease(){
    	return getValue("gene_disease");
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

    public String getUniprotIds(){
    	return getValue("gene_uniprot");
    }

    public String getSynonyms(){
    	return getValue("gene_synonym");
    }

    public String getHilitSynonyms(){
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

	public HashSet<Object> getAllFactorValues(String ef) {
		HashSet<Object> efvs = new HashSet<Object>();;

		Collection<Object> fields = geneSolrDocument.getFieldValues("efvs_up_" + IndexField.encode(ef));
		if(fields!=null)
			efvs.addAll(fields);
		fields = geneSolrDocument.getFieldValues("efvs_dn_" + IndexField.encode(ef));
		if(fields!=null)
			efvs.addAll(fields);
		return efvs;
	}

	public String getOrthologsIds(){
		ArrayList orths = (ArrayList)geneSolrDocument.getFieldValues("gene_ortholog");
		return StringUtils.join(orths,"+");
	}

	public ArrayList<String> getOrthologs(){
		ArrayList orths = (ArrayList)geneSolrDocument.getFieldValues("gene_ortholog");
		return orths;
	}

    public int getCount_up(String ef, String efv){
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + IndexField.encode(ef, efv) + "_up"));
    }

    public int getCount_dn(String ef, String efv){
        return nullzero((Short)geneSolrDocument.getFieldValue("cnt_" + IndexField.encode(ef, efv) + "_dn"));
    }

    public double getMin_up(String ef, String efv){
        return nullzero((Float)geneSolrDocument.getFieldValue("minpval_" + IndexField.encode(ef, efv) + "_up"));
    }

    public double getMin_dn(String ef, String efv){
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

	private static int nullzero(Short i)
	{
		return i == null ? 0 : i;
	}

	private static double nullzero(Float d)
	{
		return d == null ? 0.0d : d;
	}
	
	public void addOrthoGene(AtlasGene ortho){
		this.orthoGenes.add(ortho);
	}

	public ArrayList<AtlasGene> getOrthoGenes(){
		return this.orthoGenes;
	}


    public ExperimentsTable getExpermientsTable() {
        return ExperimentsTable.deserialize((String)geneSolrDocument.getFieldValue("exp_info"));
    }
}
