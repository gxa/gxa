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

package uk.ac.ebi.gxa.index;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.apache.solr.core.CoreContainer;

import java.io.File;

import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * Abstract test case class loading a temporary SOLR instance from xml dumps provided in the resources
 * once for all the class's tests. Use it only for read-only testing to speed-up.
 * 
 * Use getSolrPath() method to get solr path
 * @author pashky
 */
public abstract class AbstractOnceIndexTest {

    private static CoreContainer container;
    private static File indexPath;

    public static CoreContainer getContainer() {
        return container;
    }

    @BeforeClass
    public static void installSolr() throws Exception {
        indexPath = FileUtil.createTempDirectory("solr");
        SolrContainerFactory factory = new SolrContainerFactory();
        factory.setAtlasIndex(indexPath);
        factory.setTemplatePath("solr");
        container = factory.createContainer();
        MockIndexLoader.populateTemporarySolr(getContainer());
        System.gc();
    }

    @AfterClass
    public static void cleanupSolr() throws Exception {
        container.shutdown();
        FileUtil.deleteDirectory(indexPath);
    }
}
