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

package uk.ac.ebi.gxa.requesthandlers.base.restutil;

import java.util.Map;
import java.util.Collections;

class AcceptAllFieldFilter implements FieldFilter {
    public boolean accepts(String fieldName) {
        return true;
    }

    public FieldFilter getSubFilter(String fieldName) {
        return this;
    }
}

class RejectAllFieldFilter implements FieldFilter {
    public boolean accepts(String fieldName) {
        return false;
    }

    public FieldFilter getSubFilter(String fieldName) {
        return this;
    }
}

public class MapBasedFieldFilter implements FieldFilter {
    public static FieldFilter createFilter(Map map) {
        return map != null
            ? new MapBasedFieldFilter(map)
            : new AcceptAllFieldFilter();
    }

    private final Map map;

    private MapBasedFieldFilter(Map map) {
        this.map = map;
    }

    public boolean accepts(String fieldName) {
        Object o = map.get(fieldName);
        if (o == null) {
            o = map.get("*");
        }
        return o instanceof Map || "ALL".equals(o) || "FIELD".equals(o);
    }

    public FieldFilter getSubFilter(String fieldName) {
        Object o = map.get(fieldName);
        if (o == null) {
            o = map.get("*");
        }
        if (o instanceof Map) {
            return new MapBasedFieldFilter((Map)o);
        } else if ("ALL".equals(o)) {
            return new AcceptAllFieldFilter();
        } else {
            return new RejectAllFieldFilter();
        }
    }
}
