package uk.ac.ebi.gxa.annotator.loader.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.*;

/**
 * User: nsklyar
 * Date: 25/08/2011
 */
public class BioEntityAnnotationData extends BioEntityData{

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    //Keeps a Set of Pairs for each bioentity type, each Pair contains bioentity identifier and bioentity property value
    protected final Multimap<BioEntityType, Pair<String, BEPropertyValue>> typeToBEPropValues = HashMultimap.create();

    private final Set<BEPropertyValue> propertyValues = new HashSet<BEPropertyValue>();

    BioEntityAnnotationData(List<BioEntityType> types) {
        super(types);
    }

    void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv) {
        if (StringUtils.isNotBlank(pv.getValue()) && pv.getValue().length() < 1000) {
            Pair<String, BEPropertyValue> beProperty = Pair.create(beIdentifier, pv);
            typeToBEPropValues.put(type, beProperty);

            propertyValues.add(pv);
        } else {
            log.info("BioEntity property value is too long (>1000) " + pv.getValue());
        }
    }

    public Set<BEPropertyValue> getPropertyValues() {
        return propertyValues;
    }

    public Collection<Pair<String, BEPropertyValue>> getPropertyValuesForBioEntityType(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToBEPropValues.get(bioEntityType));
    }

}
