package uk.ac.ebi.microarray.atlas.api;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;


/**
 * @author Misha Kapushesky
 */
public class ApiArrayDesign extends ArrayDesign {
    private String accession;
    private String name;
    private String provider;
    private String type;

//    private long mappingSoftwareId;
//    private Map<String, Long> designElements = new HashMap<String, Long>();
//    private Map<Long, List<Long>> genes = new HashMap<Long, List<Long>>();

    public ApiArrayDesign(final ArrayDesign arrayDesign) {
        this.accession = arrayDesign.getAccession();
        this.name = arrayDesign.getName();
        this.provider = arrayDesign.getProvider();
        this.type = arrayDesign.getType();
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}