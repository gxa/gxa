package uk.ac.ebi.gxa.loader.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 18-Feb-2010
 */
public class ArrayDesignBundle {
    private String accession;
    private String type;
    private String name;
    private String provider;
    private List<String> designElementNames;
    private Map<String, List<String[]>> designElementNVPs;

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

    public List<String> getDesignElementNames() {
        return designElementNames;
    }

    public void addDesignElementName(String designElementName) {
        if (designElementNames == null) {
            designElementNames = new ArrayList<String>();
        }
        this.designElementNames.add(designElementName);
    }

    public void addDesignElementNameValuePair(String designElement, String type, String value) {
        if (designElementNVPs == null) {
            designElementNVPs = new HashMap<String, List<String[]>>();
        }
        if (!designElementNVPs.containsKey(designElement)) {
            designElementNVPs.put(designElement, new ArrayList<String[]>());
        }
        this.designElementNVPs.get(designElement).add(new String[]{type, value});
    }
}