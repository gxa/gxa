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
import java.util.List;

/**
 * Gene property autocomplete item
 * @author pashky
 */
public class GeneAutoCompleteItem extends AutoCompleteItem {
    private String species;
    private Collection<String> otherNames;
    private List<String> speciesOrder;

    public GeneAutoCompleteItem(String property, String value, Long count, final String species, final String geneId,
                                final Collection<String> otherNames, final List<String> speciesOrder) {
        super(property, geneId != null ? geneId : value, value, count);
        this.species = species;
        this.otherNames = otherNames;
        this.speciesOrder = speciesOrder;
    }

    public String getSpecies() {
        return species;
    }

    public Collection<String> getOtherNames() {
        return otherNames;
    }

    /**
     * The autocomplete list of items is sorted according to a user-specified ordering of species associated with
     * autocomplete items, stored in speciesOrder list.
     * For each species in speciesOrder:
     * - all items associated with the same species will be sorted alphabetically
     * - items associated with species earlier in speciesOrder will be shown before items associated with species occurring
     * later in speciesOrder.
     * Items associated with species that don't occur in speciesOrder, or those not associated with any species,
     * will be shown after items associated with species in speciesOrder, and will be sorted alphabetically.
     *
     * @return value that will be used to decide the position of this item in autocomplete list, c.f. compareTo() and
     *         equals() methods in AutoCompleteItem
     */
    @Override
    protected Integer getPositionForSpecies() {
        // Default value - will cause this AutoCompleteItem to be shown after all species-specific items -
        // c.f. compareTo() and equals() methods in AutoCompleteItem
        int ret = super.getPositionForSpecies();

        if (species != null) {
            String speciesSearchKey = species.split(" ")[0].toLowerCase(); // Get first term of species String, in lower case
            int pos = speciesOrder.indexOf(speciesSearchKey);

            if (pos != -1) { // speciesOrder list does contain an element containing speciesSearchKey                
                ret = pos;
            }
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GeneAutoCompleteItem that = (GeneAutoCompleteItem) o;

        if (otherNames != null ? !otherNames.equals(that.otherNames) : that.otherNames != null) return false;
        if (species != null ? !species.equals(that.species) : that.species != null) return false;
        if (speciesOrder != null ? !speciesOrder.equals(that.speciesOrder) : that.speciesOrder != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (species != null ? species.hashCode() : 0);
        result = 31 * result + (otherNames != null ? otherNames.hashCode() : 0);
        result = 31 * result + (speciesOrder != null ? speciesOrder.hashCode() : 0);
        return result;
    }
}
