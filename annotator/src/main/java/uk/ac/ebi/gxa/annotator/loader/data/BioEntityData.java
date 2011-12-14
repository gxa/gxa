/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

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

    BioEntity addBioEntity(String identifier, BioEntityType type, Organism organism) {
        BioEntity bioEntity = new BioEntity(identifier, type, organism);
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
