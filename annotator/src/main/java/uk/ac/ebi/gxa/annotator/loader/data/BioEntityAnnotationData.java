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
public class BioEntityAnnotationData extends BioEntityData {

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    //Keeps a Set of Pairs for each bioentity type, each Pair contains bioentity identifier and bioentity property value
    final Multimap<BioEntityType, Pair<String, BEPropertyValue>> typeToBEPropValues = HashMultimap.create();

    private final Set<BEPropertyValue> propertyValues = new HashSet<BEPropertyValue>();

    BioEntityAnnotationData(List<BioEntityType> bioEntityTypes) {
        super(bioEntityTypes);
    }

    void addPropertyValue(String beIdentifier, BioEntityType bioEntityType, BEPropertyValue pv) {
        if (StringUtils.isNotBlank(pv.getValue())) {
            //Value's length is limited by the length of corresponding DB field
            if (pv.getValue().length() < 1000) {
                Pair<String, BEPropertyValue> beProperty = Pair.create(beIdentifier, pv);
                typeToBEPropValues.put(bioEntityType, beProperty);

                propertyValues.add(pv);
            } else {
                log.info("BioEntity property value is too long (>1000) " + pv.getValue());
            }
        }
    }

    public Set<BEPropertyValue> getPropertyValues() {
        return propertyValues;
    }

    public Collection<Pair<String, BEPropertyValue>> getPropertyValuesForBioEntityType(BioEntityType bioEntityType) {
        return Collections.unmodifiableCollection(typeToBEPropValues.get(bioEntityType));
    }

}
