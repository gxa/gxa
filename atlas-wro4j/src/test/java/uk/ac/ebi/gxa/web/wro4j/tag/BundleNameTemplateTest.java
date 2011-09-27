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

package uk.ac.ebi.gxa.web.wro4j.tag;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Olga Melnichuk
 */
public class BundleNameTemplateTest {

    @Test
    public void defaultPatternTest() {
        BundleNameTemplate template = new BundleNameTemplate(null);
        assertEquals("test\\.css", template.forGroup("test", ResourceHtmlTag.CSS));
        assertEquals("test\\.js", template.forGroup("test", ResourceHtmlTag.JS));
    }

    @Test
    public void simplePatternTest() {
        String p = "@groupName@-345\\.@extension@";
        BundleNameTemplate template = new BundleNameTemplate(p);
        assertEquals("test-345\\.css", template.forGroup("test", ResourceHtmlTag.CSS));
        assertEquals("test-345\\.js", template.forGroup("test", ResourceHtmlTag.JS));
    }

    @Test
    public void invalidTypeTest() {
        try {
            BundleNameTemplate template = new BundleNameTemplate(null);
            template.forGroup("whatever", null);
            fail("Exception expected");
        } catch (NullPointerException e) {
            // expected outcome
        }
    }
}
