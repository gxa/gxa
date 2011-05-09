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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static uk.ac.ebi.microarray.atlas.model.UpDownCondition.*;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.*;

/**
 * @author Olga Melnichuk
 */
public class UpDownConditionTest {

    @Test
    public void testUpCondition() {
        checkCondition(true, CONDITION_UP, UP);
        checkCondition(false, CONDITION_UP, DOWN, NONDE, NA);
    }

    @Test
    public void testDownCondition() {
        checkCondition(true, CONDITION_DOWN, DOWN);
        checkCondition(false, CONDITION_DOWN, UP, NONDE, NA);
    }

    @Test
    public void testNonDeCondition() {
        checkCondition(true, CONDITION_NONDE, NONDE);
        checkCondition(false, CONDITION_NONDE, UP, DOWN, NA);
    }

    @Test
    public void testUpOrDownCondition() {
        checkCondition(true, CONDITION_UP_OR_DOWN, UP, DOWN);
        checkCondition(false, CONDITION_UP_OR_DOWN, NA, NONDE);
    }

    @Test
    public void testAnyCondition() {
        checkCondition(true, CONDITION_ANY, UP, DOWN, NONDE, NA);
    }

    private static void checkCondition(boolean expected, UpDownCondition cond, UpDownExpression... expressions) {
        for (UpDownExpression expr : expressions) {
            assertFalse(xor(expected, cond.apply(expr)));
        }
    }

    private static boolean xor(boolean p, boolean q) {
        return !(p && q) && (p || q);
    }
}
