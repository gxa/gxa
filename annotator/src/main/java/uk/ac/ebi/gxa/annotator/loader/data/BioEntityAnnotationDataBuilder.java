package uk.ac.ebi.gxa.annotator.loader.data;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class BioEntityAnnotationDataBuilder extends BioEntityDataBuilder<BioEntityAnnotationData> {

    public BioEntityAnnotationDataBuilder() {
    }

    @Override
    protected boolean isValidData() {
        return super.isValidData() &&
                (!data.typeToBEPropValues.isEmpty() &&
                        CollectionUtils.isEqualCollection(data.typeToBEPropValues.keySet(), data.bioEntityTypes));
    }

    @Override
    public void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        throw new UnsupportedOperationException(this.getClass().getName() + " doesn't implement method addBEDesignElementMapping");
    }

    public void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv) {
        data.addPropertyValue(beIdentifier, type, pv);
    }

    @Override
    public void createNewData(List<BioEntityType> types) {
        data = new BioEntityAnnotationData(types);
    }
}
