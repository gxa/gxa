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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

/**
 * Interface for value auto-completer services, allowing auto-completion of values for gene properties or EFs
 *
 * @author pashky
 */
public interface AutoCompleter {
    /**
     * Auto-completion helper method, returning list of matching values and counters for specific factor/property and prefix
     *
     * @param property factor or property to auto-complete values for, can be empty for any factor
     * @param prefix    prefix
     * @param limit    maximum number of values to find
     * @param filters  custom filters for results. implementation defines handling (if any)
     * @return collection of auto completed items
     */
    public Collection<AutoCompleteItem> autoCompleteValues(String property, @Nonnull String prefix, int limit, Map<String, String> filters);

    /**
     * Auto-completion helper method, returning list of matching values and counters for specific factor/property and prefix
     *
     * @param property factor or property to auto-complete values for, can be empty for any factor
     * @param prefix    prefix
     * @param limit    maximum number of values to find
     * @return collection of auto completed items
     */
    public Collection<AutoCompleteItem> autoCompleteValues(String property, @Nonnull String prefix, int limit);
}
