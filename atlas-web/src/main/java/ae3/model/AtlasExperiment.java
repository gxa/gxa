package ae3.model;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import org.apache.solr.common.SolrDocument;

import java.util.*;

@RestOut(xmlItemName ="experiment")
public class AtlasExperiment implements java.io.Serializable {

    private HashSet<String> experimentFactors = new HashSet<String>();
    private HashSet<String> sampleCharacteristics = new HashSet<String>();
    private TreeMap<String, Collection<String>> sampleCharacterisitcValues = new TreeMap<String, Collection<String>>();
    private TreeMap<String, Collection<String>> factorValues = new TreeMap<String, Collection<String>>();

    private SolrDocument exptSolrDocument;

    private HashMap<String, String> highestRankEF = new HashMap<String, String>();

    public enum DEGStatus {UNKNOWN, EMPTY, NONEMPTY};
    private DEGStatus exptDEGStatus = DEGStatus.UNKNOWN;

    @SuppressWarnings("unchecked")
    public AtlasExperiment(SolrDocument exptdoc) {
        exptSolrDocument = exptdoc;

        for(String field : exptSolrDocument.getFieldNames()) {
            if(field.startsWith("a_property_")) {
                String property = field.substring("a_property_".length());
                Collection<String> values = new HashSet<String>();
                values.addAll((Collection)exptSolrDocument.getFieldValues(field));
                experimentFactors.add(property);
                factorValues.put(property, values);
            } else if(field.startsWith("s_property_")) {
                String property = field.substring("s_property_".length());
                Collection<String> values = new HashSet<String>();
                values.addAll((Collection)exptSolrDocument.getFieldValues(field));
                sampleCharacteristics.add(property);
                sampleCharacterisitcValues.put(property, values);
            }
        }
    }

    public HashSet<String> getSampleCharacteristics() {
        return sampleCharacteristics;
    }

    public TreeMap<String, Collection<String>> getSampleCharacterisitcValues() {
		return sampleCharacterisitcValues;
	}

    public TreeMap<String, Collection<String>> getFactorValuesForEF() {
		return factorValues;
	}

    public Integer getId()
    {
        return (Integer)exptSolrDocument.getFieldValue("id");
    }

    @RestOut(name="accession")
    public String getAccession() {
        return (String)exptSolrDocument.getFieldValue("accession");
    }

    @RestOut(name="description")
    public String getDescription() {
        return (String)exptSolrDocument.getFieldValue("description");
    }

    public HashSet<String> getExperimentFactors() {
        return experimentFactors;
    }


    public HashMap<String, String> getHighestRankEFs() {
        return highestRankEF;
    }

    public void addHighestRankEF(String geneIdentifier, String highestRankEF) {
        this.highestRankEF.put(geneIdentifier, highestRankEF);
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

}
