package uk.ac.ebi.microarray.atlas.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nsklyar
 */
public class BioentityBundle {

    private String organism;
    private String type;

    private String source;
    private String version;

    private String geneField;

    private String bioentityField;

    private List<Object[]> batchWithProp = new ArrayList<Object[]>();

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

    public String getGeneField() {
        return geneField;
    }

    public void setGeneField(String geneField) {
        this.geneField = geneField;
    }

    public String getBioentityField() {
        return bioentityField;
    }

    public void setBioentityField(String bioentityField) {
        this.bioentityField = bioentityField;
    }

    public List<Object[]> getBatchWithProp() {
        return batchWithProp;
    }

    public void setBatchWithProp(List<Object[]> batchWithProp) {
        this.batchWithProp = batchWithProp;
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


