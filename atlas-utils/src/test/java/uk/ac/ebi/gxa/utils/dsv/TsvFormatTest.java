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

package uk.ac.ebi.gxa.utils.dsv;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Olga Melnichuk
 */
public class TsvFormatTest {

    @Test
    public void testDefaultSettings() throws Exception {
        checkStrictEnabled(new TsvFormat());
    }

    @Test
    public void testStrictEnabled() throws Exception {
        TsvFormat format = new TsvFormat();
        format.setStrict(true);
        checkStrictEnabled(format);
    }

    @Test
    public void testStrictDisabled() throws Exception {
        TsvFormat format = new TsvFormat();
        format.setStrict(false);
        checkStrictDisabled(format);
    }

    private void checkStrictEnabled(TsvFormat format) {
        assertEquals("test", format.sanitizeFieldValue("test"));
        assertEquals("test test", format.sanitizeFieldValue("test test"));
        assertEquals("test\"test", format.sanitizeFieldValue("test\"test"));
        assertEquals("test,test", format.sanitizeFieldValue("test,test"));

        try {
            format.sanitizeFieldValue("test\ttest");
            fail("tab char: in strict TSV mode an exception should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }

        try {
            format.sanitizeFieldValue("test\ntest");
            fail("end-of-line char: in strict TSV mode an exception should be thrown");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private void checkStrictDisabled(TsvFormat format) {
        assertEquals("test", format.sanitizeFieldValue("test"));
        assertEquals("test test", format.sanitizeFieldValue("test test"));
        assertEquals("test\"test", format.sanitizeFieldValue("test\"test"));
        assertEquals("test,test", format.sanitizeFieldValue("test,test"));
        assertEquals("test test", format.sanitizeFieldValue("test\ttest"));
        assertEquals("test test", format.sanitizeFieldValue("test\ntest"));
    }
}
