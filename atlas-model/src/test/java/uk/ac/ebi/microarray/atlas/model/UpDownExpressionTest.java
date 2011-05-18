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
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.*;

/**
 * @author Olga Melnichuk
 */
public class UpDownExpressionTest {

    @Test
    public void testUpExpression() {
        assertTrue(isUp(0.05f, 1.0f));
        assertTrue(isUp(0.04f, 1.0f));
        assertTrue(isUp(-0.04f, 1.0f));

        assertFalse(isUp(0.051f, 1.0f));
        assertFalse(isUp(0.05f, 0.0f));
        assertFalse(isUp(0.05f, -1.0f));
    }

    @Test
    public void testDownExpression() {
        assertTrue(isDown(0.05f, -1.0f));
        assertTrue(isDown(0.04f, -1.0f));
        assertTrue(isDown(-0.05f, -1.0f));

        assertFalse(isDown(0.05f, 1.0f));
        assertFalse(isDown(0.05f, 0.0f));
        assertFalse(isDown(0.051f, -1.0f));
    }

    @Test
    public void testNonDeExpression() {
        assertTrue(isNonDe(0.05f, 0.0f));
        assertTrue(isNonDe(0.04f, 0.0f));
        assertTrue(isNonDe(-0.04f, 0.0f));
    }

    @Test
    public void testNAExpression() {
        assertTrue(isNA(Float.NaN, 1.0f));
        assertTrue(isNA(1.0f, Float.NaN));

        assertFalse(isNA(1.0f, 0.5f));
    }

    @Test
    public void testUpOrDownExpression() {
        assertTrue(isUpOrDown(0.05f, -1.0f));
        assertTrue(isUpOrDown(0.04f, -1.0f));
        assertTrue(isUpOrDown(-0.05f, -1.0f));


        assertTrue(isUpOrDown(0.05f, 1.0f));
        assertTrue(isUpOrDown(0.04f, 1.0f));
        assertTrue(isUpOrDown(-0.045f, 1.0f));

        assertFalse(isUpOrDown(Float.NaN, 1.0f));
        assertFalse(isUpOrDown(1.2f, 0.0f));
    }
}
