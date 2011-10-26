package uk.ac.ebi.gxa.annotator.model.genesigdb;

import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * User: nsklyar
 * Date: 19/10/2011
 */
@Entity
@Table(name = "A2_GENESIGANNOTATIONSRC")
public class GeneSigAnnotationSource extends AnnotationSource{

    @Column(name = "url")
    private String url;

    @ManyToOne()
    private BioEntityProperty bioEntityProperty;

    public GeneSigAnnotationSource() {
    }

    @Override
    public AnnotationSource createCopyForNewSoftware(Software newSoftware) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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

    public void setBioEntityProperty(BioEntityProperty bioEntityProperty) {
        this.bioEntityProperty = bioEntityProperty;
    }
}
