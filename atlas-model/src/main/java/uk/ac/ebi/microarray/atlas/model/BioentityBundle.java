package uk.ac.ebi.microarray.atlas.model;

import java.util.ArrayList;
import java.util.List;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public class BioentityBundle {

    private String organism;
    private String type;

    private String source;
    private String version;

    private List<Object[]> batch = new ArrayList<Object[]>();

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Object[]> getBatch() {
        return batch;
    }

    public void setBatch(List<Object[]> batch) {
        this.batch = batch;
    }

    @Override
    public String toString() {
        return "BioentityBundle{" +
                "organism='" + organism + '\'' +
                ", source='" + source + '\'' +
                ", version='" + version + '\'' +
                ", beAnnotations=[]" +
                '}';
    }
}


