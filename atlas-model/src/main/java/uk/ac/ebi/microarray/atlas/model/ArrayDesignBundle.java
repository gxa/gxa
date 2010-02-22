package uk.ac.ebi.microarray.atlas.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Map<String, List<String[]>> designElementDBEs;

    private List<String> identifierPriorityOrder;

    private Logger log = LoggerFactory.getLogger(this.getClass());

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
        if (designElementNames == null) {
            designElementNames = new ArrayList<String>();
        }
        return designElementNames;
    }

    public void addDesignElementName(String designElementName) {
        if (designElementNames == null) {
            designElementNames = new ArrayList<String>();
        }
        this.designElementNames.add(designElementName);
    }

    public Map<String, String> getDatabaseEntriesForDesignElement(String designElementName) {
        Map<String, String> response = new HashMap<String, String>();

        List<String[]> nvps = designElementDBEs.get(designElementName);
        for (String[] nvp : nvps) {
            if (nvp.length != 2) {
                log.warn("Unexpected array length - name value pairs should be 1:1");
            }
            else {
                response.put(nvp[0], nvp[1]);
            }
        }

        return response;
    }

    public void addDatabaseEntriesForDesignElement(String designElement, String type, String value) {
        if (designElementDBEs == null) {
            designElementDBEs = new HashMap<String, List<String[]>>();
        }
        if (!designElementDBEs.containsKey(designElement)) {
            designElementDBEs.put(designElement, new ArrayList<String[]>());
        }
        this.designElementDBEs.get(designElement).add(new String[]{type, value});
    }
}