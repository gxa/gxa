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

import uk.ac.ebi.microarray.atlas.model.UpDownCondition;

import static uk.ac.ebi.microarray.atlas.model.UpDownCondition.*;

/**
 * Gene expression option
 */
public enum QueryExpression {
    UP_DOWN("up/down", CONDITION_UP_OR_DOWN),
    UP("up", CONDITION_UP),
    DOWN("down", CONDITION_DOWN),
    UP_ONLY("up only", CONDITION_UP),
    DOWN_ONLY("down only", CONDITION_DOWN),
    NON_D_E("non-d.e.", CONDITION_NONDE),
    ANY("any", CONDITION_ANY);

    private final String description;
    private final UpDownCondition condition;

    QueryExpression(String description, UpDownCondition condition) {
        this.description = description;
        this.condition = condition;
    }

    /**
     * Get human-readable option description
     *
     * @return description string
     */
    public String getDescription() {
        return description;
    }

    public UpDownCondition asUpDownCondition() {
        return condition;
    }

    static public QueryExpression parseFuzzyString(String s) {
        s = s.toLowerCase();
        if (s.contains("non")) {
            return NON_D_E;
        }

        if (s.contains("any") || // API calls
                (s.contains("up") && s.contains("down") && s.contains("non-d.e."))) { // Atlas search screens
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
