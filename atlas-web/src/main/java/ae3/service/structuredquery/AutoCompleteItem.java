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

/**
 * Auto-complete item container class used for auto-completion API
 * @author pashky
 */
public class AutoCompleteItem implements Comparable<AutoCompleteItem> {
    private final String property;
    private final String value;
    private final Long count;
    private final String id;
    // Note: to ensure that AutoCompleteItems show up after GeneAutoCompleteItems associated with Species,MAX_SPECIES_SORT_POSITION
    // must remain greater than the length of atlas.gene.autocomplete.species.order in AtlasProperties
    private final static Integer MAX_SPECIES_SORT_POSITION = 10000;  

    /**
     * Default constructor
     * @param property property
     * @param id item id
     * @param value property value
     * @param count number of genes having this property
     */
    public AutoCompleteItem(String property, final String id, String value, Long count) {
        this.property = property;
        this.value = value;
        this.count = count;
        this.id = id;
    }

    /**
     * 
     * @return
     */
    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }

    public Long getCount() {
        return count;
    }

    public String getId() {
        return id;
    }

    public int compareTo(AutoCompleteItem o) {
        // Note that items with smaller getPositionForSpecies() value will appear earlier in the autocomplete list, but for the
        // same value of getPositionForSpecies() Collections.sort() (c.f. AtlasGenePropertyService) will result in alphabetical sort order.
        int cmpSpeciesPos = getPositionForSpecies().compareTo(o.getPositionForSpecies());

        if (cmpSpeciesPos != 0) {
            return cmpSpeciesPos;
        }

        return value.toLowerCase().compareTo(o.getValue().toLowerCase());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AutoCompleteItem that = (AutoCompleteItem) o;

        if (value == null) {
            if (that.value != null) return false;
        } else {
             // Note that for two AutoCompleteItems to be equal, both value and getPositionForSpecies() must be equal
            if (!getPositionForSpecies().equals(that.getPositionForSpecies()) || !value.equals(that.value))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = property != null ? property.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (count != null ? count.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (getPositionForSpecies() != null ? getPositionForSpecies().hashCode() : 0);
        return result;
    }

    /**
     * @return value that will be used to decide the position of this item in autocomplete list, c.f. compareTo() and
     *         equals() methods
     */
    protected Integer getPositionForSpecies() {
        // Default behaviour - place any item not associated with a Species after all the Species-specific items
        return MAX_SPECIES_SORT_POSITION;

    }
}
