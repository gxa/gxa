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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Olga Melnichuk
 * @version 1/14/12 12:29 PM
 */
public class MartRegistryTest {

    @Test
    public void testMartRegistryParser() throws IOException, SAXException, ParserConfigurationException {
        MartRegistry registry = MartRegistry.parse(MartRegistryTest.class.getResource("mart-registry.xml").openStream());

        MartRegistry.MartUrlLocation loc = registry.find("ensembl_mart_65");
        assertNotNull(loc);
        assertEquals("ensembl_mart_65", loc.getDatabase());
        assertEquals("ensembl", loc.getName());
        assertEquals("default", loc.getVirtualSchema());
    }
}
