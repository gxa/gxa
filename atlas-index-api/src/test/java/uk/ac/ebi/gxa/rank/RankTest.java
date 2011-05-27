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

package uk.ac.ebi.gxa.rank;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Olga Melnichuk
 */
public class RankTest {
    @Test
    public void minMaxTest() {
        assertTrue((new Rank(1.0)).isMax());
        assertTrue((new Rank(0.0)).isMin());

        assertTrue(Rank.maxRank().isMax());
        assertTrue(Rank.minRank().isMin());
    }

    @Test(expected = IllegalArgumentException.class)
    public void highBoundaryTest() {
        new Rank(2.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lowBoundaryTest() {
        new Rank(-2.0);
    }

    @Test
    public void comparisonTest() {
        Rank r1 = new Rank(0.6);
        Rank r2 = new Rank(0.5);

        assertTrue(r1.compareTo(r2) > 0);
        assertTrue(r2.compareTo(r1) < 0);

        assertEquals(r1, r1.max(r2));
        assertEquals(r1, r2.max(r1));
        assertEquals(r1, Rank.max(r1, r2));

        assertEquals(r1, r1.max(null));
        assertEquals(r2, r2.max(null));
    }
}
