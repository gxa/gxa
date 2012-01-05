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

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.List;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class BioEntityAnnotationDataBuilder extends BioEntityDataBuilder<BioEntityAnnotationData> {

    public BioEntityAnnotationDataBuilder() {
    }

    @Override
    protected boolean isValidData() {
        return super.isValidData() &&
                (data.typeToBEPropValues.isEmpty() ||
                        CollectionUtils.isEqualCollection(data.typeToBEPropValues.keySet(), data.getBioEntityTypes()));
    }

    @Override
    public void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        throw new UnsupportedOperationException(this.getClass().getName() + " doesn't implement method addBEDesignElementMapping");
    }

    @Override
    public void addPropertyValue(String beIdentifier, BioEntityType type, BEPropertyValue pv) {
        data.addPropertyValue(beIdentifier, type, pv);
    }

    @Override
    public void createNewData(List<BioEntityType> types) {
        data = new BioEntityAnnotationData(types);
    }
}
