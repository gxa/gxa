package uk.ac.ebi.gxa.annotator.loader.data;

import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class DesignElementDataBuilder extends BioEntityDataBuilder<DesignElementMappingData> {

    public DesignElementDataBuilder() {
    }

    @Override
    protected boolean isValidData() {
        return true;
    }

    public void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        data.addBEDesignElementMapping(beIdentifier, type, deAccession);
    }

    @Override
    public void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv) {
        throw new UnsupportedOperationException("DesignElementDataBuilder doesn't support method addPropertyValue ");
    }

    @Override
    public void createNewData(List<BioEntityType> types) {
        data = new DesignElementMappingData(types);
    }
}
