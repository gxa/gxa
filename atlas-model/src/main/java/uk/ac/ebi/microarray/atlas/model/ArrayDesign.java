package uk.ac.ebi.microarray.atlas.model;

import java.util.Map;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ArrayDesign {
    private String accession;
    private String type;
    private String name;
    private String provider;
    private int arrayDesignID;
    private Map<Integer, String> designElements;
    private Map<Integer, List<Integer>> genes;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getArrayDesignID() {
        return arrayDesignID;
    }

    public void setArrayDesignID(int arrayDesignID) {
        this.arrayDesignID = arrayDesignID;
    }

    public Map<Integer, String> getDesignElements() {
        return designElements;
    }

    public void setDesignElements(Map<Integer, String> designElements) {
        this.designElements = designElements;
    }

    public Map<Integer, List<Integer>> getGenes() {
        return genes;
    }

    public void setGenes(Map<Integer, List<Integer>> genes) {
        this.genes = genes;
    }
}
