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
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 03/04/2012
 */
@ContextConfiguration
public class SoftwareManagerTest extends AtlasDAOTestCase {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    @Autowired
    private SoftwareDAO softwareDAO;

    private SoftwareManager swManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        swManager = new SoftwareManager();
        swManager.setAnnSrcDAO(annSrcDAO);
        swManager.setSoftwareDAO(softwareDAO);
    }

    @Test
    public void testSetSoftwareActive() throws Exception {
        final Software software = softwareDAO.getById(1001l);
        assertFalse(software.isActive());
        final Software software1 = softwareDAO.getById(1000l);
        assertTrue(software1.isActive());

        swManager.activateSoftware(1001l);
        assertFalse(software1.isActive());
        assertTrue(software.isActive());
    }
}
