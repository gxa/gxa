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
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.SoftwareDAO;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * User: nsklyar
 * Date: 27/10/2011
 */
@ContextConfiguration
public class AnnotationSourceManagerTest extends AtlasDAOTestCase {
//
    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    @Autowired
    private SoftwareDAO softwareDAO;
//
//    @Autowired
//    private AnnotationSourceManager manager;
//
//    @Override
//    public void setUp() throws Exception {
//        super.setUp();
//        manager = new AnnotationSourceManager();
//        manager.setAnnSrcDAO(annSrcDAO);
//        manager.setSoftwareDAO(softwareDAO);
//    }
//
//    @Test
//    public void testGetAnnSrcString() throws Exception {
//        final String annSrcString = manager.getAnnSrcString("1000", AnnotationSourceType.BIOMART);
//        assertEquals(BioMartAnnotationSourceConverterTest.ANN_SRC_DB, annSrcString.trim());
//    }
//
//    @Test
//    public void testSaveAnnSrc() throws Exception {
//        manager.saveAnnSrc(null, AnnotationSourceType.GENESIGDB, GeneSigAnnotationSourceConverterTest.ANN_SRC);
//        final Collection<? extends AnnotationSource> sources = annSrcDAO.getAnnotationSourcesOfType(AnnotationSourceType.GENESIGDB.getClazz());
//        assertEquals(2, sources.size());
//    }
}
