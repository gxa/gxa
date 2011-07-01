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

package ae3.service.structuredquery;

/**
 * @author Olga Melnichuk
 */
public class EfvAutoCompleteItem extends AutoCompleteItem {

    private final String factorName;

    public EfvAutoCompleteItem(String factorId, String factorName, String factorValue, Long count, Rank rank) {
        super(factorId, factorValue, factorValue, count, rank);
        this.factorName = factorName;
    }

    public String getFactorName() {
        return factorName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EfvAutoCompleteItem)) return false;
        if (!super.equals(o)) return false;

        EfvAutoCompleteItem that = (EfvAutoCompleteItem) o;

        if (factorName != null ? !factorName.equals(that.factorName) : that.factorName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (factorName != null ? factorName.hashCode() : 0);
        return result;
    }
}
