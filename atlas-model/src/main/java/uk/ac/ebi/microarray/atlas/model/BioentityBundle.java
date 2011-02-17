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

    private List<BioEntity> geneToInsert = new ArrayList<BioEntity>();
    private List<BioEntity> geneToUpdate = new ArrayList<BioEntity>();

    private List<BioEntity> toUpdate = new ArrayList<BioEntity>();
    private List<BioEntity> toInsert = new ArrayList<BioEntity>();

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

    public List<BioEntity> getGeneToInsert() {
        return geneToInsert;
    }

    public void setGeneToInsert(List<BioEntity> geneToInsert) {
        this.geneToInsert = geneToInsert;
    }

    public void addGeneToInsert(BioEntity bioEntity) {
        geneToInsert.add(bioEntity);
    }

    public List<BioEntity> getGeneToUpdate() {
        return geneToUpdate;
    }

    public void setGeneToUpdate(List<BioEntity> geneToUpdate) {
        this.geneToUpdate = geneToUpdate;
    }

    public List<BioEntity> getToUpdate() {
        return toUpdate;
    }

    public void addGeneToUpdate(BioEntity bioEntity){
        geneToUpdate.add(bioEntity);
    }

    public void setToUpdate(List<BioEntity> toUpdate) {
        this.toUpdate = toUpdate;
    }

    public void addToUpdate(BioEntity bioEntity) {
        toUpdate.add(bioEntity);
    }

    public List<BioEntity> getToInsert() {
        return toInsert;
    }

    public void setToInsert(List<BioEntity> toInsert) {
        this.toInsert = toInsert;
    }

    public void addToInsert(BioEntity bioEntity) {
        toInsert.add(bioEntity);
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


