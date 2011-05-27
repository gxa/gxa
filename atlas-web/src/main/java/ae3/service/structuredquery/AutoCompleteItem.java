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

/**
 * Auto-complete item container class used for auto-completion API
 *
 * @author pashky
 */
public class AutoCompleteItem implements Comparable<AutoCompleteItem> {
    private final String property;
    private final String value;
    private final Long count;
    private final String id;
    private final Rank rank;
    private final List<AutoCompleteItem> path = new ArrayList<AutoCompleteItem>();

    /**
     * Default constructor
     *
     * @param property property
     * @param id       item id
     * @param value    property value
     * @param count    number of genes having this property
     * @param rank     rank of an item to sort list of items by
     * @param path     a tree path if applicable
     */
    public AutoCompleteItem(String property, String id, String value, Long count, Rank rank, Collection<? extends AutoCompleteItem> path) {
        this.property = property;
        this.value = value;
        this.count = count;
        this.id = id;
        this.rank = rank == null ? Rank.minRank() : rank;
        this.path.addAll(path);
    }

    public AutoCompleteItem(String property, String id, String value, Long count, Rank rank) {
        this(property, id, value, count, rank, Collections.<AutoCompleteItem>emptyList());
    }

    public AutoCompleteItem(String property, String id, String value, Long count) {
        this(property, id, value, count, null, Collections.<AutoCompleteItem>emptyList());
    }

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

    public List<AutoCompleteItem> getPath() {
        return Collections.unmodifiableList(path);
    }

    public int compareTo(AutoCompleteItem o) {
        int compareByRanks = rank.compareTo(o.rank);
        if (compareByRanks != 0) {
            return -compareByRanks;
        }

        int compareByValue = value.toLowerCase().compareTo(o.getValue().toLowerCase());
        if (compareByValue != 0) {
            return compareByValue;
        }
        return -Long.valueOf(count).compareTo(o.count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutoCompleteItem)) return false;

        AutoCompleteItem that = (AutoCompleteItem) o;

        if (count != null ? !count.equals(that.count) : that.count != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (property != null ? !property.equals(that.property) : that.property != null) return false;
        if (rank != null ? !rank.equals(that.rank) : that.rank != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = property != null ? property.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (count != null ? count.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (rank != null ? rank.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}
