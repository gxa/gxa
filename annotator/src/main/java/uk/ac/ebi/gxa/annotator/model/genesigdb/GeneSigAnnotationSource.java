package uk.ac.ebi.gxa.annotator.model.genesigdb;

import uk.ac.ebi.gxa.annotator.loader.filebased.FileBasedConnection;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashSet;

/**
 * User: nsklyar
 * Date: 19/10/2011
 */
@Entity
@Table(name = "A2_GENESIGANNOTATIONSRC")
public class GeneSigAnnotationSource extends AnnotationSource {

    @Column(name = "url")
    private String url;

    @ManyToOne()
    private BioEntityProperty bioEntityProperty;

    GeneSigAnnotationSource() {
    }

    public GeneSigAnnotationSource(Software software) {
        super(software);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public BioEntityProperty getBioEntityProperty() {
        return bioEntityProperty;
    }

    //ToDo:(NS) to have it more generic there should be a Collection of Properties
    public void setBioEntityProperty(BioEntityProperty bioEntityProperty) {
        this.bioEntityProperty = bioEntityProperty;
    }

    @Override
    public GeneSigAnnotationSource createCopyForNewSoftware(Software newSoftware) {
        GeneSigAnnotationSource result = new GeneSigAnnotationSource(newSoftware);
        result.setUrl(this.url);
        result.setBioEntityProperty(this.bioEntityProperty);

        return result;
    }

    @Override
    public FileBasedConnection createConnection() {
        return new FileBasedConnection(this.getUrl());
    }

    @Override
    public Collection<String> findInvalidProperties() {
        Collection<String> result = new HashSet<String>();
        return result;
    }

}
