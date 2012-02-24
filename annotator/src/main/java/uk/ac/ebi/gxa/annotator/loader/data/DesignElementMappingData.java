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
import uk.ac.ebi.microarray.atlas.model.DesignElement;
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
public class DesignElementMappingData {

    private static final int DE_ACCESSION_DB_FIELD_SIZE = 255;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //Keeps a Set of Pairs for each BioEntityType, each pair contains a mapping design element acc to bioentity identifier
    private final Multimap<BioEntityType, Pair<String, String>> typeToDesignElementBEMapping = HashMultimap.create();
    private final Set<DesignElement> designElements = new HashSet<DesignElement>();

    private DesignElementMappingData() {
    }

    public Collection<Pair<String, String>> getDesignElementToBioEntity(BioEntityType type) {
        return Collections.unmodifiableCollection(typeToDesignElementBEMapping.get(type));
    }

    public Set<DesignElement> getDesignElements() {
        return Collections.unmodifiableSet(designElements);
    }

    public Collection<BioEntityType> getBioEntityTypes(){
        return Collections.unmodifiableCollection(typeToDesignElementBEMapping.keySet());
    }

    void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        if (!isNullOrEmpty(deAccession)) {
            if (deAccession.length() < DE_ACCESSION_DB_FIELD_SIZE) {
                Pair<String, String> de2be = Pair.create(deAccession, beIdentifier);
                typeToDesignElementBEMapping.put(type, de2be);

                DesignElement designElement = new DesignElement(deAccession, deAccession);
                designElements.add(designElement);
            } else {
                log.warn("Invalid (longer then accepted in DB) deAccession: {}", deAccession);
            }
        }
    }

    boolean isValid(Collection<BioEntityType> types) {
        return (typeToDesignElementBEMapping.isEmpty() ||
                isEqualCollection(typeToDesignElementBEMapping.keySet(), types));
    }

    public static class Builder {
        private final DesignElementMappingData data = new DesignElementMappingData();

        public void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
            data.addBEDesignElementMapping(beIdentifier, type, deAccession);
        }

        public DesignElementMappingData build(Collection<BioEntityType> types) throws InvalidAnnotationDataException {
            if (data.isValid(types)) {
                return data;
            }
            throw new InvalidAnnotationDataException("De-to-be mappings data is invalid");
        }
    }
}
