/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.requesthandlers.dump;

import org.junit.*;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;

/**
 * @author ostolop
 */
public class GeneIdentifiersDumpDownloadRequestHandlerTest extends AbstractOnceIndexTest {

    final private Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testDumpGeneIdentifiers() {
        File testDumpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "gene_identifiers.txt");

        GeneIdentifiersDumpDownloadRequestHandler svt = new GeneIdentifiersDumpDownloadRequestHandler();

        svt.setCoreContainer(getContainer());
        svt.setDumpGeneIdsFile(testDumpFile);
        
        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        AtlasProperties atlasProperties = new AtlasProperties();
        atlasProperties.setStorage(storage);
        svt.setAtlasProperties(atlasProperties);

        svt.dumpGeneIdentifiers();

        assertTrue(testDumpFile.exists());

        if(!testDumpFile.delete()) {
            log.error("Failed to delete temporary file");
        }
    }
}
