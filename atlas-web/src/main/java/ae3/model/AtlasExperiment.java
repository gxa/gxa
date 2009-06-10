package ae3.model;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.ae3.indexbuilder.Constants;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop, mdylag
 * Date: Apr 17, 2008
 * Time: 9:31:07 AM
 * To change this template use File | Settings | File Templates.
 */
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
    private Map<String, List<String>> experimentHighlights;

    private SolrDocument exptSolrDocument;

    private HashMap<String, String> highestRankEF = new HashMap<String, String>();

    public AtlasExperiment() {

    }

    public static AtlasExperiment load(SolrDocument exptdoc)
    {
        AtlasExperiment expt = new AtlasExperiment();
        expt.doload(exptdoc);
        expt.setExptSolrDocument(exptdoc);

        return expt;
    }

    public void doload(SolrDocument exptDoc)
    {
        setDwExpId(Long.parseLong(exptDoc.getFieldValue(Constants.FIELD_DWEXP_ID).toString()));
        setDwExpAccession((String)exptDoc.getFieldValue(Constants.FIELD_DWEXP_ACCESSION));
        setDwExpDescription((String)exptDoc.getFieldValue(Constants.FIELD_DWEXP_EXPDESC));
        setDwExpType((Collection)exptDoc.getFieldValues(Constants.FIELD_DWEXP_EXPTYPE));

        this.setExperimentFactorValues(exptDoc.getFieldValues(Constants.FIELD_DWEXP_FV));
        this.setExperimentFactors(exptDoc.getFieldValues(Constants.FIELD_DWEXP_EF));

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

    public Collection<String> getDwExpType() {
        return dwExpType;
    }

    public String getDwExpAccession() {
        return dwExpAccession;
    }

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
}
