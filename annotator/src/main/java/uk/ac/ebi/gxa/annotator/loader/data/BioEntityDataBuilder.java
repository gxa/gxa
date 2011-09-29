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

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public abstract class BioEntityDataBuilder<T extends BioEntityData> {

    protected T data;

    protected BioEntityDataBuilder() {
    }

    public abstract void createNewData(List<BioEntityType> types);

    public T getBioEntityData() throws AtlasAnnotationException {
        if (isValidData())
            return data;
        else
            throw new AtlasAnnotationException("Annotation/Mapping data is not valid");
    }

    protected boolean isValidData() {
        if (data.getTypeToBioEntities().isEmpty()) {
            return true;
        } else
            return CollectionUtils.isEqualCollection(data.getTypeToBioEntities().keySet(), data.getBioEntityTypes());
    }


    public BioEntity addBioEntity(String identifier, String name, BioEntityType type, Organism organism) {
        return data.addBioEntity(identifier, name, type, organism);
    }

    public abstract void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession);

    public abstract void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv);
}
