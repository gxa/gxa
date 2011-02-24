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
 * Gene expression option
 */
public enum QueryExpression {
    UP_DOWN("up/down"),
    UP("up"),
    DOWN("down"),
    UP_ONLY("up only"),
    DOWN_ONLY("down only"),
    NON_D_E("non-d.e."),
    ANY("any");

    private String description;

    QueryExpression(String description) {
        this.description = description;
    }

    /**
     * Get human-readable option description
     *
     * @return description string
     */
    public String getDescription() {
        return description;
    }

    static public QueryExpression parseFuzzyString(String s) {
        s = s.toLowerCase();
        if (s.contains("non")) {
            return NON_D_E;
        }

        if (s.contains("any")) {
            return ANY;
        }

        boolean hasOnly = s.contains("only");
        boolean hasUp = s.contains("up");
        boolean hasDn = s.contains("dn") || s.contains("down");
        if (hasUp == hasDn) {
            return UP_DOWN;
        }
        return hasOnly ? (hasUp ? UP_ONLY : DOWN_ONLY) : (hasUp ? UP : DOWN);
    }
}
