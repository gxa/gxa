package uk.ac.ebi.gxa.annotator.loader.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.*;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class BioEntityData {

    private final List<BioEntityType> bioEntityTypes;

    // Multimap used because of one to many relationship between BioEntityType and BioEntity
    private Multimap<BioEntityType, BioEntity> typeToBioEntities = HashMultimap.create();
    private Set<Organism> organisms = new HashSet<Organism>();

    public Multimap<BioEntityType, BioEntity> getTypeToBioEntities() {
        return typeToBioEntities;
    }

    BioEntityData(List<BioEntityType> bioEntityTypes) {
        this.bioEntityTypes = bioEntityTypes;
    }

    BioEntity addBioEntity(String identifier, String name, BioEntityType type, Organism organism) {
        BioEntity bioEntity = new BioEntity(identifier, name, type, organism);
        typeToBioEntities.put(type, bioEntity);
        organisms.add(organism);
        return bioEntity;
    }

    public List<BioEntityType> getBioEntityTypes() {
        return Collections.unmodifiableList(bioEntityTypes);
    }

    public Collection<BioEntity> getBioEntitiesOfType(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToBioEntities.get(bioEntityType));
    }

    public Set<Organism> getOrganisms() {
        return organisms;
    }
}
