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

import java.util.ArrayList;
import java.util.List;

/**
     * Gene epxression option
 */
public enum QueryExpression {
    UP_DOWN("up/down"),
    UP("up"),
    DOWN("down");

    private String description;
    QueryExpression(String description) { this.description = description; }

    /**
     * Get human-readable option description
     * @return description string
     */
    public String getDescription() { return description; }

    /**
     * Lists all available options and their human-readable representation
     * @return list of gene expression options
     */
    static public List<String[]> getOptionsList() {
        List<String[]> result = new ArrayList<String[]>();
        for(QueryExpression r : values())
        {
           result.add(new String[] { r.name(), r.getDescription() });
        }
        return result;
    }

    static public QueryExpression parseFuzzyString(String s) {
        s = s.toLowerCase();
        boolean hasUp = s.contains("up");
        boolean hasDn = s.contains("dn") || s.contains("down");
        if(!(hasUp ^ hasDn))
            return UP_DOWN;
        return hasUp ? UP : DOWN;
    }
}
