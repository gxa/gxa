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
 * Auto-complete item class extended to represent EFO attributes
 *
 * @author pashky
 */
public class EfoAutoCompleteItem extends AutoCompleteItem {

    private final List<String> alternativeTerms = new ArrayList<String>();

    public EfoAutoCompleteItem(String property, String id, String value, Long count, Collection<String> alternativeTerms, Rank rank, Collection<EfoAutoCompleteItem> path) {
        super(property, id, value, count, rank, path);
        this.alternativeTerms.addAll(alternativeTerms);
    }

    public EfoAutoCompleteItem(String property, String id, String value, Long count, Collection<String> alternativeTerms) {
        this(property, id, value, count, alternativeTerms, null, Collections.<EfoAutoCompleteItem>emptyList());
    }

    public EfoAutoCompleteItem(EfoAutoCompleteItem item, Collection<EfoAutoCompleteItem> path, Rank rank) {
        this(item.getProperty(), item.getId(), item.getValue(), item.getCount(), item.getAlternativeTerms(), rank, path);
    }

    public List<String> getAlternativeTerms() {
        return Collections.unmodifiableList(alternativeTerms);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EfoAutoCompleteItem)) return false;
        if (!super.equals(o)) return false;

        EfoAutoCompleteItem that = (EfoAutoCompleteItem) o;

        if (alternativeTerms != null ? !alternativeTerms.equals(that.alternativeTerms) : that.alternativeTerms != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (alternativeTerms != null ? alternativeTerms.hashCode() : 0);
        return result;
    }
}
