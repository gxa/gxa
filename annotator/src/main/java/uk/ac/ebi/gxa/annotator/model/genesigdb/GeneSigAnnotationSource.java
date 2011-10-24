package uk.ac.ebi.gxa.annotator.model.genesigdb;

import uk.ac.ebi.gxa.annotator.model.AnnotationSource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * User: nsklyar
 * Date: 19/10/2011
 */
@Entity
@Table(name = "A2_ANNOTATIONSRC")
public class GeneSigAnnotationSource extends AnnotationSource{

    @Column(name = "url")
    private String url;

    public GeneSigAnnotationSource() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
