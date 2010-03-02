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

package uk.ac.ebi.gxa.index;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.apache.solr.core.CoreContainer;

import java.io.File;

import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * Abstract test case class loading a temporary SOLR instance from xml dumps provided in the resources
 * for EVERY test. Use it for write operations.
 *
 * Use getSolrPath() method to get solr path
 * @author pashky
 */
public abstract class AbstractEveryIndexTest {

    private CoreContainer container;
    private File indexPath;

    public CoreContainer getContainer() {
        return container;
    }

    @Before
    public void installSolr() throws Exception {
        indexPath = FileUtil.createTempDirectory("solr");
        SolrContainerFactory factory = new SolrContainerFactory();
        factory.setAtlasIndex(indexPath);
        factory.setTemplatePath("solr");
        container = factory.createContainer();
        MockIndexLoader.populateTemporarySolr(getContainer());
        System.gc();
    }

    @After
    public void cleanupSolr() throws Exception {
        container.shutdown();
        FileUtil.deleteDirectory(indexPath);
    }

}
