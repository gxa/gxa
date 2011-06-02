package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.Organism;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
@Entity
@DiscriminatorValue("biomart")
public class BioMartAnnotationSource extends AnnotationSource {
    /**
     * Location of biomart martservice, e.g.:
     * "http://www.ensembl.org/biomart/martservice?"
     * "http://plants.ensembl.org/biomart/martservice?"
     */

    @Column(name = "url")
    private String url;

    /**
     * e.g. "hsapiens_gene_ensembl", "spombe_eg_gene"
     */
    @Column(name="biomartorganismname")
    private String datasetName;

    @ManyToMany
    @JoinTable(name = "A2_ANNSRC_BIOMARTPROPERTY",
            joinColumns = @JoinColumn(name = "annotationsrcid", referencedColumnName = "annotationsrcid"),
            inverseJoinColumns = @JoinColumn(name = "BIOMARTPROPERTYID", referencedColumnName = "BIOMARTPROPERTYID"))
    private List<BioMartProperty> bioMartProperties = new ArrayList<BioMartProperty>();

//    private Map<String, String> martToAtlasProperties = new HashMap<String, String>();

    /**
     * Those properties are read from biomart registry
     */
    @Transient
    private String bioMartName;
    @Transient
    private String serverVirtualSchema;

    private static final String DATA_SET_PH = "$DATA_SET";
    private static final String PROP_NAME_PH = "$PROP_NAME";
    private static final String VIRTUAL_SCHEMA_PH = "$VIRTUAL_SCHEMA";
    private static final String PROPERTY_QUERY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<!DOCTYPE Query>" +
                    "<Query  virtualSchemaName = \"$VIRTUAL_SCHEMA\" formatter = \"TSV\" header = \"0\" uniqueRows = \"1\" count = \"\" >" +
                    "<Dataset name = \"$DATA_SET\" interface = \"default\" >" +
                    "<Attribute name = \"ensembl_gene_id\" />" +
                    "<Attribute name = \"ensembl_transcript_id\" />" +
                    "<Attribute name = \"$PROP_NAME\" />" +
                    "</Dataset>" +
                    "</Query>";


    BioMartAnnotationSource() {
    }

    public BioMartAnnotationSource(Software software, Organism organism) {
        super(software, organism);
    }

    public BioMartAnnotationSource(Long annotationSrcId, Software software, Organism organism) {
        super(annotationSrcId, software, organism);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<BioMartProperty> getBioMartProperties() {
        return Collections.unmodifiableList(bioMartProperties);
    }

    public void setBioMartProperties(List<BioMartProperty> bioMartProperties) {
        this.bioMartProperties = bioMartProperties;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getBioMartName() {
        return bioMartName;
    }

    public void setBioMartName(String bioMartName) {
        this.bioMartName = bioMartName;
    }

    public String getServerVirtualSchema() {
        return serverVirtualSchema;
    }

    public void setServerVirtualSchema(String serverVirtualSchema) {
        this.serverVirtualSchema = serverVirtualSchema;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    protected CurrentAnnotationSource<? extends AnnotationSource> createCurrAnnSrc(BioEntityType bioEntityType) {
        return new BioMartCurrentAnnotationSource(this, bioEntityType);
    }

    /////////////////////////
    //  Helper methods
    ////////////////////////
    public BioMartAnnotationSource createCopy(Software newSoftware) {
        BioMartAnnotationSource result = new BioMartAnnotationSource(newSoftware, this.organism);
        result.setDatasetName(this.datasetName);
        result.setUrl(this.url);
        result.bioMartProperties = new ArrayList<BioMartProperty>(this.bioMartProperties);

        return result;
    }

    public String getPropertyURLLocation(String martProperty) {
        return url + PROPERTY_QUERY.replace(DATA_SET_PH, datasetName).
                replace(PROP_NAME_PH, martProperty).replace(VIRTUAL_SCHEMA_PH, serverVirtualSchema);
    }
}
