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

/**
 * @author Olga Melnichuk
 */
public class CsvFormatTest {

    @Test
    public void testDefaultSettings() throws Exception {
        checkDblQuotesDisabled(new CsvFormat());
    }

    @Test
    public void testDblQuotesEnabled() throws Exception {
        CsvFormat format = new CsvFormat();
        format.setEncloseInDblQuotes(true);
        checkDblQuotesEnabled(format);
    }

    @Test
    public void testDblQuotesDisabled() throws Exception {
        CsvFormat format = new CsvFormat();
        format.setEncloseInDblQuotes(false);
        checkDblQuotesDisabled(format);
    }

    private void checkDblQuotesEnabled(CsvFormat format) {
        assertEquals("\"test\"", format.sanitizeFieldValue("test"));
        assertEquals("\"test test\"", format.sanitizeFieldValue("test test"));
        assertEquals("\"test\ttest\"", format.sanitizeFieldValue("test\ttest"));
        assertEquals("\"test,test\"", format.sanitizeFieldValue("test,test"));
        assertEquals("\"test\ntest\"", format.sanitizeFieldValue("test\ntest"));
        assertEquals("\"test\"\"test\"", format.sanitizeFieldValue("test\"test"));
    }

    private void checkDblQuotesDisabled(CsvFormat format) {
        assertEquals("test", format.sanitizeFieldValue("test"));
        assertEquals("test test", format.sanitizeFieldValue("test test"));
        assertEquals("test\ttest", format.sanitizeFieldValue("test\ttest"));
        assertEquals("\"test,test\"", format.sanitizeFieldValue("test,test"));
        assertEquals("\"test\ntest\"", format.sanitizeFieldValue("test\ntest"));
        assertEquals("\"test\"\"test\"", format.sanitizeFieldValue("test\"test"));
    }
}
