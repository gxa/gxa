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

package uk.ac.ebi.microarray.atlas.model;

import java.util.EnumSet;

import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.*;

/**
 * @author Olga Melnichuk
 */
public enum UpDownCondition {
    CONDITION_UP("up", of(UP)),
    CONDITION_DOWN("down", of(DOWN)),
    CONDITION_NONDE("non-d.e", of(NONDE)),
    CONDITION_UP_OR_DOWN("up/down", of(UP, DOWN)),
    CONDITION_ANY("any", allOf(UpDownExpression.class));

    private final String name;
    private final EnumSet<UpDownExpression> matching;

    private UpDownCondition(String name, EnumSet<UpDownExpression> matching) {
        this.name = name;
        this.matching = matching;
    }

    public String getName() {
        return name;
    }

    public final boolean apply(UpDownExpression expression) {
        return matching.contains(expression);
    }
}
