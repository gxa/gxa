package uk.ac.ebi.microarray.atlas.model.annotation;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    @Column(name = "biomartorganismname")
    private String datasetName;

    /**
     * Value of property "database" in BioMart registry, version number is removed.
     * e.g. "metazoa", "fungal"
     */
    @Column(name = "databaseName")
    private String databaseName;

    @OneToMany(targetEntity = BioMartProperty.class
           , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
    private Set<BioMartProperty> bioMartProperties = new HashSet<BioMartProperty>();

    @OneToMany(targetEntity = BioMartArrayDesign.class
           , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
    private Set<BioMartArrayDesign> bioMartArrayDesigns = new HashSet<BioMartArrayDesign>();

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

    public Set<BioMartProperty> getBioMartProperties() {
        return Collections.unmodifiableSet(bioMartProperties);
    }

    public Set<String> getBioMartPropertyNames() {
        Set<String> answer = new HashSet<String>(bioMartProperties.size());
        for (BioMartProperty bioMartProperty : bioMartProperties) {
            answer.add(bioMartProperty.getName());
        }
        return answer;
    }

    void setBioMartProperties(Set<BioMartProperty> bioMartProperties) {
        this.bioMartProperties = bioMartProperties;
    }

    public void addBioMartProperty(String biomartPropertyName, BioEntityProperty bioEntityProperty) {
        BioMartProperty bioMartProperty = new BioMartProperty(biomartPropertyName, bioEntityProperty, this);
        this.bioMartProperties.add(bioMartProperty);
    }

    public void addBioMartProperty(BioMartProperty bioMartProperty) {
        bioMartProperty.setAnnotationSrc(this);
        this.bioMartProperties.add(bioMartProperty);
    }

    public boolean removeBioMartProperty(BioMartProperty property) {
        return bioMartProperties.remove(property);
    }

    public Set<BioMartArrayDesign> getBioMartArrayDesigns() {
        return bioMartArrayDesigns;
    }

    void setBioMartArrayDesigns(Set<BioMartArrayDesign> bioMartArrayDesigns) {
        this.bioMartArrayDesigns = bioMartArrayDesigns;
    }

    public void addBioMartArrayDesign(BioMartArrayDesign bioMartArrayDesign) {
        bioMartArrayDesign.setAnnotationSrc(this);
        this.bioMartArrayDesigns.add(bioMartArrayDesign);
    }

    public boolean removeBioMartArrayDesign(BioMartArrayDesign bioMartArrayDesign) {
        return bioMartArrayDesigns.remove(bioMartArrayDesign);
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }


    @Override
    public boolean isUpdatable() {
        return true;
    }

    /////////////////////////
    //  Helper methods
    ////////////////////////
    public BioMartAnnotationSource createCopyForNewSoftware(Software newSoftware) {
        BioMartAnnotationSource result = new BioMartAnnotationSource(newSoftware, this.organism);
        result.setDatasetName(this.datasetName);
        result.setUrl(this.url);
        for (BioMartProperty bioMartProperty : bioMartProperties) {
            result.addBioMartProperty(bioMartProperty.getName(), bioMartProperty.getBioEntityProperty());
        }
        for (BioEntityType type : types) {
            result.addBioentityType(type);
        }
//        result.bioMartProperties = new HashSet<BioMartProperty>(this.bioMartProperties);
        result.setDatabaseName(this.databaseName);

        return result;
    }

    @Override
    public String toString() {
        return "BioMartAnnotationSource{" + '\'' +
                super.toString() + '\'' +
                "url='" + url + '\'' +
                ", datasetName='" + datasetName + '\'' +
                ", bioMartProperties=" + bioMartProperties +
                "} ";
    }
}
