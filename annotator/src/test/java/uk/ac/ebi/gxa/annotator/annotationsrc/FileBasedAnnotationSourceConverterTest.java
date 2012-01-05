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
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.annotationsrc.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.FileBasedAnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

/**
 * User: nsklyar
 * Date: 22/08/2011
 */
@ContextConfiguration
public class FileBasedAnnotationSourceConverterTest extends AtlasDAOTestCase {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;
    @Autowired
    private OrganismDAO organismDAO;
    @Autowired
    private SoftwareDAO softwareDAO;
    @Autowired
    private BioEntityTypeDAO typeDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;
    @Autowired
    private ArrayDesignService arrayDesignService;

    private FileBasedAnnotationSourceConverter converter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        converter = new FileBasedAnnotationSourceConverter();
        converter.setAnnSrcDAO(annSrcDAO);
        converter.setOrganismDAO(organismDAO);
        converter.setPropertyDAO(propertyDAO);
        converter.setSoftwareDAO(softwareDAO);
        converter.setTypeDAO(typeDAO);
        converter.setArrayDesignService(arrayDesignService);
    }

    @Test
    @Transactional
    public void testEditOrCreateAnnotationSourceCreate() throws Exception {
        FileBasedAnnotationSource annotationSource = converter.editOrCreateAnnotationSource(null, ANN_SRC);
        assertNotNull(annotationSource);
        assertEquals(new Software("GeneSigDB", "test"), annotationSource.getSoftware());
        assertEquals(1, annotationSource.getExternalBioEntityProperties().size());
    }

    @Test(expected = AnnotationLoaderException.class)
    @Transactional
    public void testEditOrCreateAnnotationSourceEditWithException() throws Exception {
        converter.editOrCreateAnnotationSource("1001", ANN_SRC);
    }

    //ToDo: the test fails in the end because there are some problems with sequences.
//    @Test
//    @Transactional
//    public void testEditOrCreateAnnotationSourceEdit() throws Exception {
//        GeneSigAnnotationSource annotationSource = converter.editOrCreateAnnotationSource("1001", ANN_SRC_EDITED);
//        assertNotNull(annotationSource);
//
//    }

    @Test
    public void testConvertToString() throws Exception {
        String annSrcAsString = converter.convertToString("1001");
        assertEquals(ANN_SRC_DB, annSrcAsString.trim());
    }

    protected static final String ANN_SRC = "software.name = GeneSigDB\n" +
            "software.version = test\n" +
            "url = file://genesigdb\n" +
            "types = ensgene\n" +
            "property.genesigdb = genesigdb";

    private static final String ANN_SRC_DB =
            "software.name = GeneSigDB\n" +
                    "software.version = 4\n" +
                    "url = file://genesigdb\n" +
                    "types = ensgene\n" +
                    "property.genesigdb = genesigdbid";

    private static final String ANN_SRC_EDITED=
            "software.name = GeneSigDB\n" +
                    "software.version = 4\n" +
                    "url = file://genesigdb\n" +
                    "types = ensgene\n" +
                    "property.newprop = newprop";
}
