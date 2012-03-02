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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;

/**
 * User: nsklyar
 * Date: 25/08/2011
 */
public class BioEntityAnnotationData {

    public static final int PROPERTY_VALUE_DB_FIELD_SIZE = 1000;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //Keeps a Set of Pairs for each bioentity type, each Pair contains bioentity identifier and bioentity property value
    private final Multimap<BioEntityType, Pair<String, BEPropertyValue>> typeToBEPropValues = HashMultimap.create();
    private final Set<BEPropertyValue> propertyValues = new HashSet<BEPropertyValue>();

    private BioEntityAnnotationData() {
    }

    public Set<BEPropertyValue> getPropertyValues() {
        return propertyValues;
    }

    public Collection<Pair<String, BEPropertyValue>> getPropertyValuesForBioEntityType(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToBEPropValues.get(bioEntityType));
    }

    public Collection<BioEntityType> getBioEntityTypes() {
        return Collections.unmodifiableCollection(typeToBEPropValues.keySet());
    }

    void addPropertyValue(String beIdentifier, BioEntityType bioEntityType, BEPropertyValue pv) {
        String value = pv.getValue();
        if (!isNullOrEmpty(value)) {
            if (value.length() < PROPERTY_VALUE_DB_FIELD_SIZE) {
                Pair<String, BEPropertyValue> beProperty = Pair.create(beIdentifier, pv);
                typeToBEPropValues.put(bioEntityType, beProperty);
                propertyValues.add(pv);
            } else {
                log.warn("Invalid (longer then accepted in DB) BE property value: {}", value);
            }
        }
    }

    boolean isValid(Collection<BioEntityType> types) {
        return (typeToBEPropValues.isEmpty() ||
                isEqualCollection(typeToBEPropValues.keySet(), types));
    }

    public static class Builder {
        private final BioEntityAnnotationData data = new BioEntityAnnotationData();

        public void addPropertyValue(String beIdentifier, BioEntityType bioEntityType, BEPropertyValue pv) {
            data.addPropertyValue(beIdentifier, bioEntityType, pv);
        }

        public BioEntityAnnotationData build(Collection<BioEntityType> types) throws InvalidAnnotationDataException {
            if (data.isValid(types)) {
                return data;
            }
            throw new InvalidAnnotationDataException("BE annotation data is invalid");
        }
    }
}
