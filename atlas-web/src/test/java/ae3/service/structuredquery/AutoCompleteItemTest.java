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

package ae3.service.structuredquery;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class AutoCompleteItemTest {

    private static final AutoCompleteItem SAMPLE = new AutoCompleteItem("property", "id", "value", 1L);

    @Test
    public void itemCreationTest() {
        try {
            new AutoCompleteItem("prop", "id", "val", 1L, null, null);
            fail();
        } catch (NullPointerException e) {
            //OK
        }

        AutoCompleteItem item = new AutoCompleteItem("prop", "id", "val", 1L);
        assertTrue(item.getPath().isEmpty());

        item = new AutoCompleteItem("prop", "id", "val", 1L, null, Arrays.asList(item));
        assertEquals(1, item.getPath().size());
    }

    @Test
    public void itemOrderTest() {
        Rank rank = new Rank(0.1);
        AutoCompleteItem item1 = varyValue(SAMPLE, "value1", rank);
        AutoCompleteItem item2 = varyValue(SAMPLE, "value1", rank);
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertEquals(0, item1.compareTo(item2));

        item2 = varyValue(item2, "value2", rank);
        assertFalse(item1.equals(item2));
        assertTrue(item1.compareTo(item2) < 0);
        assertTrue(item2.compareTo(item1) > 0);

        item1 = varyCount(item2, 2L, rank);
        assertFalse(item1.equals(item2));
        assertTrue(item1.compareTo(item2) < 0);
        assertTrue(item2.compareTo(item1) > 0);

        item1 = varyValue(item2, "value2", new Rank(0.2));
        assertFalse(item1.equals(item2));
        assertTrue(item1.compareTo(item2) < 0);
        assertTrue(item2.compareTo(item1) > 0);
    }

    private AutoCompleteItem varyValue(AutoCompleteItem item, String value, Rank rank) {
        return new AutoCompleteItem(item.getProperty(), item.getId(), value, item.getCount(), rank, item.getPath());
    }

    private AutoCompleteItem varyCount(AutoCompleteItem item, long count, Rank rank) {
        return new AutoCompleteItem(item.getProperty(), item.getId(), item.getValue(), count, rank, item.getPath());
    }

}
