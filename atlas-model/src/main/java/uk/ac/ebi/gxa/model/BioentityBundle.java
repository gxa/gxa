package uk.ac.ebi.gxa.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public class BioentityBundle {

    private String organism;
    private String source;
    private String version;

    private Map<String, Map<String, List<String>>> beAnnotations = new HashMap<String, Map<String, List<String>>>(200000);

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Map<String, List<String>>> getBeAnnotations() {
        return beAnnotations;
    }

    public void addBEAnnotations(String bioentity, Map<String, List<String>> annotations) {
        beAnnotations.put(bioentity, annotations);
    }

    public String getType() {
        return source;
    }

    @Override
    public String toString() {
        return "BioentityBundle{" +
                "organism='" + organism + '\'' +
                ", source='" + source + '\'' +
                ", version='" + version + '\'' +
                ", beAnnotations=" + beAnnotations +
                '}';
    }
}


