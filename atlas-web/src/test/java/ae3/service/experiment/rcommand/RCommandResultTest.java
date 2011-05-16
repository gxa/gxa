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

package ae3.service.experiment.rcommand;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.R.RType;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class RCommandResultTest {

    private static AtlasComputeService computeService;
    private static RCommand rCommand;

    @BeforeClass
    public static void setUp() throws InstantiationException {
        AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.BIOCEP);
        computeService = new AtlasComputeService();
        computeService.setAtlasRFactory(rFactory);

        rCommand = new RCommand(computeService,
                "ae3/service/experiment/rcommand/RCommandResultTest.R");
    }

    @AfterClass
    public static void tearDown() {
        assertNotNull(computeService);
        computeService.shutdown();
    }

    @Test
    public void emptyResultSetTest() {
        RCommandResult res = runCommand(new RCommandStatement("returnEmptyDataFrame"));
        assertNotNull(res);
        assertTrue(res.isEmpty());

        double[] numbers = res.getNumericValues("numericColumn");
        int[] integers = res.getIntValues("integerColumn");
        String[] strings = res.getStringValues("stringColumn");
        double[] test = res.getNumericValues("testColumn");
        String[] nonExisted = res.getStringValues("nonExistedColumn");

        assertArrayEquals(new int[]{10}, res.getIntAttribute("intAttr"));
        assertNotNull(res.getIntAttribute("emptyAttr"));
        assertNotNull(res.getIntAttribute("nullAttr"));
        assertNotNull(res.getIntAttribute("nonExistedAttribute"));

        assertNotNull(numbers);
        assertEquals(0, numbers.length);

        assertNotNull(integers);
        assertEquals(0, integers.length);

        assertNotNull(strings);
        assertEquals(0, strings.length);

        assertNotNull(test);
        assertEquals(0, test.length);

        assertNotNull(nonExisted);
        assertEquals(0, nonExisted.length);
    }

    @Test
    public void nonEmptyResultSetTest() {
        RCommandResult res = runCommand(new RCommandStatement("returnNonEmptyDataFrame"));
        assertNotNull(res);
        assertFalse(res.isEmpty());

        double[] numbers = res.getNumericValues("numericColumn");
        int[] integers = res.getIntValues("integerColumn");
        String[] strings = res.getStringValues("stringColumn");
        double[] test = res.getNumericValues("testColumn");

        assertArrayEquals(new int[]{10}, res.getIntAttribute("intAttr"));
        assertNotNull(res.getIntAttribute("nonExistedAttribute"));

        assertNotNull(test);
        assertEquals(3, test.length);
        assertArrayEquals(new double[]{1.2, 2.3, 3.4}, test, 1e-6);

        assertNotNull(numbers);
        assertEquals(3, numbers.length);
        assertArrayEquals(new double[]{1.2, 2.3, 3.4}, numbers, 1e-6);

        assertNotNull(integers);
        assertEquals(3, integers.length);
        assertArrayEquals(new int[]{1, 2, 3}, integers);

        assertNotNull(strings);
        assertEquals(3, strings.length);
        assertArrayEquals(new String[]{"test", "test", "test"}, strings);

        try {
            res.getIntValues("nonExistedColumn");
            fail();
        } catch (IllegalArgumentException e) {
            //OK
        }
    }

    @Test
    public void nullResultSetTest() {
        RCommandResult res = new RCommandResult(null);
        assertTrue(res.isEmpty());

        int[] values = res.getIntValues("nonExistedColumn");
        assertNotNull(values);
        assertEquals(0, values.length);

        values = res.getIntValues("nonExisted");
        assertNotNull(values);
        assertEquals(0, values.length);
    }

    private RCommandResult runCommand(RCommandStatement stmt) {
        assertNotNull(rCommand);
        return rCommand.execute(stmt);
    }

}
