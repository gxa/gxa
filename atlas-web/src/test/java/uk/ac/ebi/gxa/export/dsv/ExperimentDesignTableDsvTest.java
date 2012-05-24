/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.export.dsv;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.easymock.EasyMock;
import org.junit.Test;
import uk.ac.ebi.gxa.utils.dsv.DsvColumn;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;
import uk.ac.ebi.gxa.web.controller.ExperimentDesignUI;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.ObjectArrays.concat;
import static java.util.Arrays.asList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.export.dsv.ExperimentDesignTableDsv.permanentColumns;

/**
 * @author Olga Melnichuk
 */
public class ExperimentDesignTableDsvTest {

    private static final SortedSet<Property> twoProperties = new TreeSet<Property>();
    private static final List<Assay> oneAssay = new ArrayList<Assay>();

    static {
        Property prop1 = Property.createProperty(1L, "PROP_ACCESSION_1", "PROPERTY 1");
        PropertyValue propValue11 = new PropertyValue(1L, prop1, "PROPERTY_VALUE_1");
        PropertyValue propValue12 = new PropertyValue(2L, prop1, "PROPERTY_VALUE_2");

        Property prop2 = Property.createProperty(2L, "PROP_ACCESSION_2", "PROPERTY 2");
        PropertyValue propValue21 = new PropertyValue(3L, prop2, "PROPERTY_VALUE_3");

        Assay assay = new Assay("ASSAY-ACCESSION", null, new ArrayDesign("ARRAY-DESIGN-ACCESSION"));
        assay.addProperty(propValue11);
        assay.addProperty(propValue12);
        assay.addProperty(propValue21);

        oneAssay.add(assay);
        twoProperties.addAll(asList(prop1, prop2));
    }

    @Test
    public void testEmptyDesign() {
        DsvRowIterator<ExperimentDesignUI.Row> iter =
                ExperimentDesignTableDsv.createDsvDocument(
                        new ExperimentDesignUI(
                                mockExperiment(new TreeSet<Property>(), new ArrayList<Assay>())
                        ));

        assertListEquals(permanentColumnNames(), iter.getColumnNames());

        assertEquals(0, iter.getTotalRowCount());
        assertFalse(iter.hasNext());

        try {
            iter.next();
            fail("Iterator should throw NoSuchElementException if there are no more elements");
        } catch (NoSuchElementException e) {
            //OK
        }
    }

    @Test
    public void testOneAssayDesign() {
        ExperimentDesignUI expDesign = new ExperimentDesignUI(
                mockExperiment(twoProperties, oneAssay));

        DsvRowIterator<ExperimentDesignUI.Row> iter =
                ExperimentDesignTableDsv.createDsvDocument(expDesign);

        String[] expectedColumnNames = concat(
                toStringArray(permanentColumnNames()), toStringArray(getPropertyNames(twoProperties)), String.class);
        assertArrayEquals(expectedColumnNames, toStringArray(iter.getColumnNames()));

        assertEquals(1, iter.getTotalRowCount());
        assertTrue(iter.hasNext());

        List<String> values = iter.next();
        assertListContains(values, valuesOf(oneAssay.iterator().next(), twoProperties));
    }

    private String[] toStringArray(Collection<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private List<String> valuesOf(Assay assay, SortedSet<Property> properties) {
        List<String> values = new ArrayList<String>();
        for(Property prop : properties) {
            Collection<PropertyValue> propValues = assay.getEffectiveValues(prop);
            Collection<String> strings = Collections2.transform(propValues, new Function<PropertyValue, String>() {
                @Override
                public String apply(@Nullable PropertyValue input) {
                   return input.getDisplayValue();
                }
            });

            values.add(on(",").join(strings));
        }
        return values;
    }

    private static Experiment mockExperiment(SortedSet<Property> properties, List<Assay> assays) {
        Experiment experiment = EasyMock.createMock(Experiment.class);
        expect(experiment.getProperties())
                .andReturn(properties)
                .anyTimes();

        expect(experiment.getAssays())
                .andReturn(assays)
                .anyTimes();

        replay(experiment);
        return experiment;
    }

    private static List<String> permanentColumnNames() {
        return transform(permanentColumns(),
                new Function<DsvColumn<ExperimentDesignUI.Row>, String>() {
                    @Override
                    public String apply(@Nullable DsvColumn<ExperimentDesignUI.Row> column) {
                        return column.getName();
                    }
                });
    }

    private static Collection<String> getPropertyNames(Collection<Property> properties) {
         return Collections2.transform(properties, new Function<Property, String>() {
            @Override
            public String apply(@Nullable Property prop) {
                return prop.getDisplayName();
            }
        });
    }

    private static void assertListEquals(List<String> list1, List<String> list2) {
        assertArrayEquals(list1.toArray(), list2.toArray());
    }

    private static void assertListContains(List<String> list, List<String> expectedValues) {
        for(String v : expectedValues) {
            assertTrue(list.contains(v));
        }
    }
}
