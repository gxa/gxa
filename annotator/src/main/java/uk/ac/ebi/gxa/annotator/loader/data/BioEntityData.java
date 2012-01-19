/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class BioEntityData {

    // Multimap used because of one to many relationship between BioEntityType and BioEntity
    private Multimap<BioEntityType, BioEntity> typeToBioEntities = HashMultimap.create();
    private final Organism organism;

    private BioEntityData(Organism organism) {
        this.organism = organism;
    }

    public Collection<BioEntityType> getBioEntityTypes() {
        return Collections.unmodifiableCollection(typeToBioEntities.keySet());
    }

    public Collection<BioEntity> getBioEntitiesOfType(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToBioEntities.get(bioEntityType));
    }

    public Organism getOrganism() {
        return organism;
    }

    void addBioEntity(BioEntity bioEntity) {
        if (organism.equals(bioEntity.getOrganism())) {
            typeToBioEntities.put(bioEntity.getType(), bioEntity);
            return;
        }
        throw new IllegalStateException("Unknown bioEntity organism: " + bioEntity.getOrganism().getName());
    }

    boolean isValid(Collection<BioEntityType> types) {
        return (typeToBioEntities.isEmpty() ||
                isEqualCollection(typeToBioEntities.keySet(), types));
    }

    public static class Builder {
        private final BioEntityData data;

        public Builder(Organism organism) {
            data = new BioEntityData(organism);
        }

        public void addBioEntity(BioEntity bioEntity) {
            data.addBioEntity(bioEntity);
        }

        public BioEntityData build(Collection<BioEntityType> types) throws InvalidAnnotationDataException {
            if (data.isValid(types)) {
                return data;
            }
            throw new InvalidAnnotationDataException("BioEntity data is invalid");
        }
    }
}
