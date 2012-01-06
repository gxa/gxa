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
import org.apache.commons.collections.CollectionUtils;
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
public class DesignElementMappingData extends BioEntityData {

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    //Keeps a Set of Pairs for each BioEntityType, each pair contains a mapping design element acc to bioentity identifier
    final Multimap<BioEntityType, Pair<String, String>> typeToDesignElementBEMapping = HashMultimap.create();
    private final Set<DesignElement> designElements = new HashSet<DesignElement>();

    DesignElementMappingData(List<BioEntityType> bioEntityTypes) {
        super(bioEntityTypes);
    }

    void addBEDesignElementMapping(String beIdentifier, BioEntityType type, String deAccession) {
        if (StringUtils.isNotBlank(deAccession)) {
            //Value's length is limited by the length of corresponding DB field
            if (deAccession.length() < 255) {
                Pair<String, String> de2be = Pair.create(deAccession, beIdentifier);
                typeToDesignElementBEMapping.put(type, de2be);

                DesignElement designElement = new DesignElement(deAccession, deAccession);
                designElements.add(designElement);
            } else {
                log.info("Design element accession is too long (>255)" + deAccession);
            }
        }
    }

    public Collection<Pair<String, String>> getDesignElementToBioEntity(BioEntityType type) {
        return Collections.unmodifiableCollection(typeToDesignElementBEMapping.get(type));
    }

    public Set<DesignElement> getDesignElements() {
        return Collections.unmodifiableSet(designElements);
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                (typeToDesignElementBEMapping.isEmpty() ||
                        CollectionUtils.isEqualCollection(typeToDesignElementBEMapping.keySet(), getBioEntityTypes()));
    }
}
