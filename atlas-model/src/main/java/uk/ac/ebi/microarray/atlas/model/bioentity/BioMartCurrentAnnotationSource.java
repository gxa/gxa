package uk.ac.ebi.microarray.atlas.model.bioentity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * User: nsklyar
 * Date: 02/06/2011
 */
@Entity
@DiscriminatorValue("biomart")
public class BioMartCurrentAnnotationSource extends CurrentAnnotationSource<BioMartAnnotationSource>{

    BioMartCurrentAnnotationSource(BioMartAnnotationSource source, BioEntityType type) {
        super(source, type);
    }

    @ManyToOne(targetEntity = BioMartAnnotationSource.class)
    @Override
    public BioMartAnnotationSource getSource() {
        return super.getSource();
    }
}
