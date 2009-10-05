package ae3.model;

import ae3.dao.NetCDFReader;
import ae3.restresult.RestOut;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.ae3.indexbuilder.Constants;

import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop, mdylag
 * Date: Apr 17, 2008
 * Time: 9:31:07 AM
 * To change this template use File | Settings | File Templates.
 */
@RestOut(xmlItemName ="experiment")
public class AtlasExperiment implements java.io.Serializable {
    private Long dwExpId;
    private String dwExpAccession;
    private String dwExpDescription;
    private Collection<String> dwExpType;

    //assay
    //private 
    //samples

    private Collection<String> experimentFactorValues;
    private HashSet<String> experimentFactors;
    private HashSet<String> sampleCharacteristics;
    private TreeMap<String, List<String>> sampleCharacterisitcValues;
    private TreeMap<String, List<String>> factorValues;
    private Map<String, List<String>> experimentHighlights;

    private SolrDocument exptSolrDocument;

    private HashMap<String, String> highestRankEF = new HashMap<String, String>();

    public enum DEGStatus {UNKNOWN, EMPTY, NONEMPTY};
    private DEGStatus exptDEGStatus = DEGStatus.UNKNOWN;

    private ExperimentalData expData;

    public AtlasExperiment(SolrDocument exptdoc) {
        doload(exptdoc);
        setExptSolrDocument(exptdoc);
    }

    @RestOut(xmlItemName ="factorValue")
    public static class FactorValueList extends ArrayList<String> {}

    @RestOut(xmlItemName ="value")
    public static class SampleValueList extends ArrayList<String> {}

    public void doload(SolrDocument exptDoc)
    {
        setDwExpId(Long.parseLong(exptDoc.getFieldValue(Constants.FIELD_DWEXP_ID).toString()));
        setDwExpAccession((String)exptDoc.getFieldValue(Constants.FIELD_DWEXP_ACCESSION));
        setDwExpDescription((String)exptDoc.getFieldValue(Constants.FIELD_DWEXP_EXPDESC));
        setDwExpType((Collection)exptDoc.getFieldValues(Constants.FIELD_DWEXP_EXPTYPE));

        this.setExperimentFactorValues(exptDoc.getFieldValues(Constants.FIELD_DWEXP_FV));
        this.setExperimentFactors(exptDoc.getFieldValues(Constants.FIELD_DWEXP_EF));
        
        this.setSampleCharacteristics(exptDoc.getFieldValues(Constants.FIELD_DWSAMPLE_CHAR));
        
        if(getSampleCharacteristics().size()!=0){
        	for(String characteristic: getSampleCharacteristics()){
                List<String> values = new SampleValueList();
        		values.addAll((Collection)exptDoc.getFieldValues(Constants.PREFIX_SAMPLE+characteristic));
        		addSampleCharacterisitcValue(characteristic,values);
        	}
        }
        
        if(getExperimentFactors().size()!=0){
        	for(String factor: getExperimentFactors()){
                List<String> values = new FactorValueList();
                values.addAll((Collection)exptDoc.getFieldValues(Constants.PREFIX_ASSAY+factor));
        		addFactorValue(factor,values);
        	}
        }

        Object o = exptDoc.getFieldValue("timestamp");
        
        setLoadDate((Date)o);
    }

    public void setDwExpType(Collection<String> experimentTypes) {
        this.dwExpType = experimentTypes;
    }

    public void setDwExpAccession(String experimentAccession) {
        this.dwExpAccession = experimentAccession;
    }

    public void setDwExpDescription(String experimentDescription) {
        this.dwExpDescription = experimentDescription;
    }

    public void setExperimentFactorValues(Collection experimentFactorValues) {
        this.experimentFactorValues = experimentFactorValues;
    }

    public void setExperimentFactors(Collection experimentFactors) {

        this.experimentFactors = experimentFactors != null ? new HashSet<String>(experimentFactors) : new HashSet<String>();
    }

    public HashSet<String> getSampleCharacteristics() {
        return sampleCharacteristics;
    }

    public void setSampleCharacteristics(HashSet<String> sampleCharacteristics) {
        this.sampleCharacteristics = sampleCharacteristics != null ? new HashSet<String>(sampleCharacteristics) : new HashSet<String>();
    }
    
    public void setSampleCharacteristics(Collection sampleCharacteristics) {
		this.sampleCharacteristics = sampleCharacteristics != null ? new HashSet<String>(sampleCharacteristics) : new HashSet<String>();
	}

	public TreeMap<String, List<String>> getSampleCharacterisitcValues() {
		return sampleCharacterisitcValues;
	}

	public void addSampleCharacterisitcValue(String characterisitc, List<String>values) {
		if(sampleCharacterisitcValues==null)
			sampleCharacterisitcValues = new TreeMap<String, List<String>>();
		this.sampleCharacterisitcValues.put(characterisitc, values);
	}
	
	public void addFactorValue(String characterisitc, List<String>values) {
		if(factorValues==null)
			factorValues = new TreeMap<String, List<String>>();
		this.factorValues.put(characterisitc, values);          
	}

	public TreeMap<String, List<String>> getFactorValuesForEF() {
		return factorValues;
	}

    @RestOut(name="types")
    public Collection<String> getDwExpType() {
        return dwExpType;
    }

    @RestOut(name="accession")
    public String getDwExpAccession() {
        return dwExpAccession;
    }

    @RestOut(name="description")
    public String getDwExpDescription() {
        return dwExpDescription;
    }

    public Collection<String> getExperimentFactorValues() {
        return experimentFactorValues;
    }

    public HashSet<String> getExperimentFactors() {
        return experimentFactors;
    }

    public void setExperimentHighlights(Map<String, List<String>> experimentHighlights) {
        this.experimentHighlights = experimentHighlights;
    }

    public Map<String, List<String>> getExperimentHighlights() {
        return experimentHighlights;
    }

    public Long getDwExpId()
    {
        return dwExpId;
    }

    public void setDwExpId(Long dwExpId)
    {
        this.dwExpId = dwExpId;
    }

    public HashMap serializeForWebServices() {
        HashMap h = new HashMap();

        SolrDocument expt = this.getExptSolrDocument();

        if(expt != null){
            Map m = expt.getFieldValuesMap();
            for (Object key : m.keySet()) {
                Collection<String> s = (Collection<String>) m.get(key);
                h.put(key, StringUtils.join(s, "\t"));
            }
        }
        return h;
    }

    public SolrDocument getExptSolrDocument() {
        return exptSolrDocument;
    }

    public void setExptSolrDocument(SolrDocument exptSolrDocument) {
        this.exptSolrDocument = exptSolrDocument;
    }


    public HashMap<String, String> getHighestRankEFs() {
        return highestRankEF;
    }

    public void addHighestRankEF(String geneIdentifier, String highestRankEF) {
        this.highestRankEF.put(geneIdentifier, highestRankEF);
    }

    private Date loadDate;

    public void setLoadDate(Date value){
            loadDate = value;
    }

    public Date getLoadDate(){

        return loadDate;
        /*
        try{
            //exptSolrDocument.getFieldValue("")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
                //new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);

        return sdf.parse("2008-01-01");
        }
        catch(ParseException ex){ return null; } //TODO
        */
    }

    public void setDEGStatus(DEGStatus degStatus) {
        this.exptDEGStatus = degStatus;
    }

    /**
     * Returns one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN,
     * if experiment doesn't have any d.e. genes, has some d.e. genes, or if this is unknown
     * @return one of DEGStatus.EMPTY, DEGStatus.NONEMPTY, DEGStatus.UNKNOWN
     */
    public DEGStatus getDEGStatus() {
        return this.exptDEGStatus;
    }

    /**
     * Attempts to load and return experimental data from NetCDF file using default Atlas location
     * @return experimental data or null if failed to find data
     * @throws RuntimeException if i/o error occurs
     */
    public ExperimentalData getExperimentalData() {
        if(expData == null) {
            try {
                expData = NetCDFReader.loadExperiment(getDwExpId());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read experiment data", e);
            }
        }
        return expData;
    }
    
}
