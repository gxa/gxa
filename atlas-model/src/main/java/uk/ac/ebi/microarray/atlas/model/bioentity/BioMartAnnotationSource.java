package uk.ac.ebi.microarray.atlas.model.bioentity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
public class BioMartAnnotationSource extends AnnotationSource {

    public static final String DATA_SET_PH = "$DATA_SET";
    public static final String PROP_NAME_PH = "$PROP_NAME";

    /**
     * Location of biomart martservice, e.g.:
     * "http://www.ensembl.org/biomart/martservice?"
     * "http://plants.ensembl.org/biomart/martservice?"
     */
    private String url;

    /**
     * Template for property query, e.g. :
     * "<pre>
     * {@code
     * query=<?xml version="1.0" encoding="UTF-8"?>
     * <!DOCTYPE Query>
     * <Query  virtualSchemaName = "default" formatter = "TSV" header = "0" uniqueRows = "1" count = "" datasetConfigVersion = "0.6" >
     * <p/>
     * <Dataset name = "$DATA_SET" interface = "default" >
     * <Filter name = "with_go" excluded = "0"/>
     * <Attribute name = "ensembl_gene_id" />
     * <Attribute name = "ensembl_transcript_id" />
     * <Attribute name = "$PROP_NAME" />
     * </Dataset>
     * </Query>
     * }</pre>"
     */
    private String propertyQueryTemplate;

    /**
     * e.g. "hsapiens_gene_ensembl", "spombe_eg_gene"
     */
    private String datasetName;
    private Map<String, String> martToAtlasProperties = new HashMap<String, String>();

    public BioMartAnnotationSource(String name, String version, Organism organism) {
        super(name, version, organism);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getMartToAtlasProperties() {
        return Collections.unmodifiableMap(martToAtlasProperties);
    }

    public void addMartProperty(String martProperty, String atlasProperty) {
        martToAtlasProperties.put(martProperty, atlasProperty);
    }

    private String getAtlasProperty(String martProperty) {
        return martToAtlasProperties.get(martProperty);
    }

    public String getPropertyQueryTemplate() {
        return propertyQueryTemplate;
    }

    public void setPropertyQueryTemplate(String propertyQueryTemplate) {
        this.propertyQueryTemplate = propertyQueryTemplate;
    }


    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }
    /////////////////////////
    //  Helper methods
    ////////////////////////
    public BioMartAnnotationSource createCopy(String newVersion) {
        BioMartAnnotationSource result = new BioMartAnnotationSource(this.name, newVersion, this.organism);
        result.setDatasetName(this.datasetName);
        result.setUrl(this.url);
        result.martToAtlasProperties = new HashMap<String, String>(this.martToAtlasProperties);

        return result;
    }

    public String getPropertyURLLocation(String martProperty) {
        return url + propertyQueryTemplate.replace(DATA_SET_PH, datasetName).
                replace(PROP_NAME_PH, martProperty);
    }
}
