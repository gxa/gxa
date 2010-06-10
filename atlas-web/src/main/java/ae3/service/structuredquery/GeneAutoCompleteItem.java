/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package ae3.service.structuredquery;

import java.util.Collection;

/**
 * Gene property autocomplete item
 * @author pashky
 */
public class GeneAutoCompleteItem extends AutoCompleteItem {
    private String species;
    private Collection<String> otherNames;

    public GeneAutoCompleteItem(String property, String value, Long count, final String species, final String geneId, final Collection<String> otherNames) {
        super(property, geneId != null ? geneId : value, value, count);
        this.species = species;
        this.otherNames = otherNames;
    }

    public String getSpecies() {
        return species;
    }

    public Collection<String> getOtherNames() {
        return otherNames;
    }

}
