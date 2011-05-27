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

import uk.ac.ebi.gxa.rank.Rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Gene property auto-complete item
 *
 * @author pashky
 */
public class GeneAutoCompleteItem extends AutoCompleteItem {

    private final String species;
    private final List<String> otherNames = new ArrayList<String>();

    public GeneAutoCompleteItem(String property, String value, Long count, final String species, final String geneId,
                                final Collection<String> otherNames, Rank rank) {
        super(property, geneId != null ? geneId : value, value, count, rank);
        this.species = species;
        this.otherNames.addAll(otherNames);
    }

    public GeneAutoCompleteItem(String property, String value, Long count, final String species, final String geneId,
                                final Collection<String> otherNames) {
        this(property, value, count, species, geneId, otherNames, null);
    }

    public GeneAutoCompleteItem(String property, String value, Long count) {
        this(property, value, count, null, null, Collections.<String>emptyList(), null);
    }

    public GeneAutoCompleteItem(GeneAutoCompleteItem item, Rank rank) {
        this(item.getProperty(), item.getValue(), item.getCount(), item.getSpecies(), item.getId(), item.getOtherNames(), rank);
    }

    public String getSpecies() {
        return species;
    }

    public Collection<String> getOtherNames() {
        return Collections.unmodifiableList(otherNames);
    }

    public boolean hasSpecies() {
        return !isNullOrEmpty(species);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeneAutoCompleteItem)) return false;
        if (!super.equals(o)) return false;

        GeneAutoCompleteItem that = (GeneAutoCompleteItem) o;

        if (otherNames != null ? !otherNames.equals(that.otherNames) : that.otherNames != null) return false;
        if (species != null ? !species.equals(that.species) : that.species != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (species != null ? species.hashCode() : 0);
        result = 31 * result + (otherNames != null ? otherNames.hashCode() : 0);
        return result;
    }
}
