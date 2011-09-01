package uk.ac.ebi.gxa.annotator.loader.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class BioEntityData {

    final protected List<BioEntityType> types;

    protected Multimap<BioEntityType, BioEntity> typeToBioEntities = HashMultimap.create();
    protected Set<Organism> organisms = new HashSet<Organism>();

    BioEntityData(List<BioEntityType> types) {
        this.types = types;
    }

    BioEntity addBioEntity(String identifier, String name, BioEntityType type, Organism organism) {
        BioEntity bioEntity = new BioEntity(identifier, name, type, organism);
        typeToBioEntities.put(type, bioEntity);
        organisms.add(organism);
        return bioEntity;
    }

    public List<BioEntityType> getBioEntityTypes() {
        return Collections.unmodifiableList(types);
    }

    public Collection<BioEntity> getBioEntitiesOfType(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToBioEntities.get(bioEntityType));
    }

    public Set<Organism> getOrganisms() {
        return organisms;
    }
}
