package uk.ac.ebi.gxa.annotator.loader.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 25/08/2011
 */
public class BioEntityAnnotationData extends BioEntityData{

    private Multimap<BioEntityType, List<String>> typeToBEPropValues = HashMultimap.create();

    private final Set<BEPropertyValue> propertyValues = new HashSet<BEPropertyValue>();

    BioEntityAnnotationData(List<BioEntityType> types) {
        super(types);
    }

    void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv) {
        if (StringUtils.isNotBlank(pv.getValue()) && pv.getValue().length() < 1000 && !"NA".equals(pv.getValue())) {
            List<String> beProperty = new ArrayList<String>(3);
            beProperty.add(beIdentifier);
            beProperty.add(pv.getProperty().getName());
            beProperty.add(pv.getValue());
            typeToBEPropValues.put(type, beProperty);

            propertyValues.add(pv);
        }
    }

    public Set<BEPropertyValue> getPropertyValues() {
        return propertyValues;
    }

    public Collection<List<String>> getPropertyValuesForBioEntityType(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToBEPropValues.get(bioEntityType));
    }

}
