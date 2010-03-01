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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.index.builder.service;

import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestGeneAtlasIndexBuilderService
        extends IndexBuilderServiceTestCase {
    private GeneAtlasIndexBuilderService gaibs;

    public void setUp() throws Exception {
        super.setUp();

        Efo efo = new Efo();

        gaibs = new GeneAtlasIndexBuilderService();
        gaibs.setEfo(efo);
        gaibs.setAtlasDAO(getAtlasDAO());
        gaibs.setSolrServer(getAtlasSolrServer());
    }

    public void tearDown() throws Exception {
        super.tearDown();

        gaibs = null;
    }

    public void testCreateIndexDocs() {
        try {
            // create the docs
            gaibs.createIndexDocs(new IndexBuilderService.ProgressUpdater() {
                public void update(String progress) {}
            });

            // commit the results
            gaibs.getSolrServer().commit();

            // todo - now test that all the docs we'd expect were created
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
