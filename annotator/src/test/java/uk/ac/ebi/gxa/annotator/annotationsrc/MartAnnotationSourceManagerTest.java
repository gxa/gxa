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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.VersionFinder;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.SoftwareDAO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 27/10/2011
 */
@ContextConfiguration
public class MartAnnotationSourceManagerTest extends AtlasDAOTestCase {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    @Autowired
    private SoftwareDAO softwareDAO;

    @Autowired
    private BioMartAnnotationSourceConverter converter;

    private MartAnnotationSourceManager manager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        manager = new MartAnnotationSourceManager();
        manager.setAnnSrcDAO(annSrcDAO);
        manager.setSoftwareDAO(softwareDAO);
        manager.setConverter(converter);
    }

    @Test
    public void testGetAnnSrcString() throws Exception {
        final String annSrcString = manager.getAnnSrcString("1000");
        assertEquals(BioMartAnnotationSourceConverterTest.ANN_SRC_DB, annSrcString.trim());
    }

    @Test
    public void testCreateUpdatedAnnotationSource() throws Exception {
        manager.setVersionFinder(versionFinder);
        final BioMartAnnotationSource annSrc = annSrcDAO.getById(1000, BioMartAnnotationSource.class);
        final UpdatedAnnotationSource<BioMartAnnotationSource> result = manager.createUpdatedAnnotationSource(annSrc);
        assertNotNull(result);
        assertTrue(result.isUpdated());
        assertEquals("100", result.getAnnotationSource().getSoftware().getVersion());
    }

    private static VersionFinder versionFinder = new VersionFinder() {
        @Override
        public String fetchOnLineVersion(AnnotationSource annSrc) {
            return "100";
        }
    };
}
