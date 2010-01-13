package ae3.model;

import ae3.dao.NetCDFReader;
import ae3.restresult.RestOut;
import org.apache.solr.common.SolrDocument;

import java.io.IOException;
import java.util.*;

@RestOut(xmlItemName ="experiment")
public class AtlasExperiment implements java.io.Serializable {

    private HashSet<String> experimentFactors = new HashSet<String>();
    private HashSet<String> sampleCharacteristics = new HashSet<String>();
    private TreeMap<String, List<String>> sampleCharacterisitcValues = new TreeMap<String, List<String>>();
    private TreeMap<String, List<String>> factorValues = new TreeMap<String, List<String>>();

    private SolrDocument exptSolrDocument;

    private HashMap<String, String> highestRankEF = new HashMap<String, String>();

    public enum DEGStatus {UNKNOWN, EMPTY, NONEMPTY};
    private DEGStatus exptDEGStatus = DEGStatus.UNKNOWN;

    private ExperimentalData expData;

    public AtlasExperiment(SolrDocument exptdoc) {
        exptSolrDocument = exptdoc;

        for(String field : exptSolrDocument.getFieldNames()) {
            if(field.startsWith("a_property_")) {
                String property = field.substring("a_property_".length());
                List<String> values = new ArrayList<String>();
                values.addAll((Collection)exptSolrDocument.getFieldValues(field));
                experimentFactors.add(property);
                factorValues.put(property, values);
            } else if(field.startsWith("s_property_")) {
                String property = field.substring("s_property_".length());
                List<String> values = new ArrayList<String>();
                values.addAll((Collection)exptSolrDocument.getFieldValues(field));
                sampleCharacteristics.add(property);
                sampleCharacterisitcValues.put(property, values);
            }
        }
    }

    public HashSet<String> getSampleCharacteristics() {
        return sampleCharacteristics;
    }

    public TreeMap<String, List<String>> getSampleCharacterisitcValues() {
		return sampleCharacterisitcValues;
	}

    public TreeMap<String, List<String>> getFactorValuesForEF() {
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

    /**
     * Attempts to load and return experimental data from NetCDF file using default Atlas location
     * @return experimental data or null if failed to find data
     * @throws RuntimeException if i/o error occurs
     */
    public ExperimentalData getExperimentalData() {
        if(expData == null) {
            try {
                expData = NetCDFReader.loadExperiment(getId());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read experiment data", e);
            }
        }
        return expData;
    }
    
}
