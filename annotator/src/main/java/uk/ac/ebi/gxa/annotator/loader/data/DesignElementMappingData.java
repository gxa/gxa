package uk.ac.ebi.gxa.annotator.loader.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.*;

/**
 * User: nsklyar
 * Date: 25/08/2011
 */
public class DesignElementMappingData extends BioEntityData{

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    //Keeps a Set of Pairs for each BioEntityType, each pair contains a mapping design element acc to bioentity identifier
    protected final Multimap<BioEntityType, Pair<String, String>> typeToDesignElementBEMapping = HashMultimap.create();
    protected final Set<DesignElement> designElements = new HashSet<DesignElement>();

    DesignElementMappingData(List<BioEntityType> bioEntityTypes) {
        super(bioEntityTypes);
    }

    void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        if (StringUtils.isNotBlank(deAccession) && deAccession.length() < 255) {
            Pair<String, String> de2be = Pair.create(deAccession, beIdentifier);
            typeToDesignElementBEMapping.put(type, de2be);

            DesignElement designElement = new DesignElement(deAccession, deAccession);
            designElements.add(designElement);
        } else {
            log.info("Design element accession is too long (>255)" + deAccession);
        }
    }

    public Collection<Pair<String, String>> getDesignElementToBioEntity(BioEntityType type) {
        return Collections.unmodifiableCollection(typeToDesignElementBEMapping.get(type));
    }
  
    public Set<DesignElement> getDesignElements() {
        return Collections.unmodifiableSet(designElements);
    }
    
    public void clear() {
        typeToDesignElementBEMapping.clear();
        designElements.clear();
    }
}
