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

package uk.ac.ebi.gxa.index.builder.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import java.io.IOException;
import java.util.concurrent.Executors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class TestGeneAtlasIndexBuilderService extends IndexBuilderServiceTestCase {
    private GeneAtlasIndexBuilderService gaibs;

    public void setUp() throws Exception {
        super.setUp();

        final AtlasProperties atlasProperties = createMock(AtlasProperties.class);
        expect(atlasProperties.getGeneAtlasIndexBuilderChunksize()).andReturn(100);
        expect(atlasProperties.getGeneAtlasIndexBuilderCommitfreq()).andReturn(1000);
        replay(atlasProperties);

        gaibs = new GeneAtlasIndexBuilderService();
        gaibs.setAtlasDAO(atlasDAO);
        gaibs.setBioEntityDAO(bioEntityDAO);
        gaibs.setSolrServer(getAtlasSolrServer());
        gaibs.setAtlasProperties(atlasProperties);
        gaibs.setExecutor(Executors.newSingleThreadExecutor());
    }

    public void tearDown() throws Exception {
        super.tearDown();

        gaibs = null;
    }

    @Test
    public void testCreateIndexDocs() throws IndexBuilderException, IOException, SolrServerException {
        // create the docs
        gaibs.build(new IndexAllCommand(), new IndexBuilderService.ProgressUpdater() {
            public void update(String progress) {
            }
        });

        // commit the results
        gaibs.getSolrServer().commit();

        // todo - now test that all the docs we'd expect were created
    }
}
