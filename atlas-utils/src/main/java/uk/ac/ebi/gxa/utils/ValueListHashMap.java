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
package uk.ac.ebi.gxa.utils;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Helper class for creating key -> [value list] maps, automatically adding new lists for entries
 * @author pashky
 */
public class ValueListHashMap<From,To> extends HashMap<From, List<To>> {
    /**
     * Puts value under key either by adding to existing list or creating a new list
     * @param key key
     * @param value value
     * @return stored list
     */
    public List<To> put(From key, To value) {
        List<To> list = get(key);
        if(list == null)
            put(key, list = new ArrayList<To>());
        list.add(value);
        return list;
    }
}
