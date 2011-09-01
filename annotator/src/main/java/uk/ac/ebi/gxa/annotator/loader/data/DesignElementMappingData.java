package uk.ac.ebi.gxa.annotator.loader.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 25/08/2011
 */
public class DesignElementMappingData extends BioEntityData{

    private Multimap<BioEntityType, Pair<String, String>> typeToDesignElementBEMapping = HashMultimap.create();
    private Set<DesignElement> designElements = new HashSet<DesignElement>();

    DesignElementMappingData(List<BioEntityType> types) {
        super(types);
    }

    void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        if (StringUtils.isNotBlank(deAccession) && deAccession.length() < 1000 && !"NA".equals(deAccession)) {
            Pair<String, String> de2be = Pair.create(deAccession, beIdentifier);
            typeToDesignElementBEMapping.put(type, de2be);

            DesignElement designElement = new DesignElement(deAccession, deAccession);
            designElements.add(designElement);
        }
    }

    public Collection<Pair<String, String>> getDesignElementToBioEntity(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToDesignElementBEMapping.get(bioEntityType));
    }
  
    public Set<DesignElement> getDesignElements() {
        return Collections.unmodifiableSet(designElements);
    }
    
    public void clear() {
        typeToDesignElementBEMapping.clear();
        designElements.clear();
    }
}
