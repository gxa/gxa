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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;

import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.utils.FileUtil.getTempDirectory;
import static uk.ac.ebi.gxa.utils.FileUtil.tempFile;

import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;
import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * @author ostolop
 */
public class GoogleSitemapXmlRequestHandlerTest extends AbstractOnceIndexTest {
    final private Logger log = LoggerFactory.getLogger(getClass());

    @After
    public void tearDown() {
        // cleanup
        String[] filesToDelete = new File(getTempDirectory()).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("geneSitemap");

            }
        });

        for (String f : filesToDelete) {
            if (!(new File(f).delete()))
                log.error("Couldn't delete temporary file " + f + " in " + getTempDirectory());
        }
    }

    @Test
    public void testWriteGeneSitemap() {
        GoogleSitemapXmlRequestHandler svt = new GoogleSitemapXmlRequestHandler();
        svt.setCoreContainer(getContainer());

        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        AtlasProperties atlasProperties = new AtlasProperties();
        atlasProperties.setStorage(storage);

        svt.setAtlasProperties(atlasProperties);

        File sitemapIndexFile = tempFile("geneSitemapIndex.xml");
        svt.setSitemapIndexFile(sitemapIndexFile);
        svt.writeGeneSitemap();

        assertTrue(sitemapIndexFile.exists());

        File geneSitemap0 = new File(sitemapIndexFile.getParentFile(), "geneSitemap0.xml.gz");
        assertTrue(geneSitemap0.exists());
    }
}
