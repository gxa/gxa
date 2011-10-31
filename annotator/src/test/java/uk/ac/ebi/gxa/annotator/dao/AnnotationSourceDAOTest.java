/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.dao;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 23/08/2011
 */
@ContextConfiguration
public class AnnotationSourceDAOTest extends AtlasDAOTestCase {

    @Autowired
    protected OrganismDAO organismDAO;

    @Autowired
    protected AnnotationSourceDAO annSrcDAO;

    @Autowired
    protected BioEntityPropertyDAO propertyDAO;

    @Autowired
    private SoftwareDAO softwareDAO;

    @Autowired
    private BioEntityTypeDAO typeDAO;


    private BioMartAnnotationSource fetchAnnotationSource() {
        Software software = softwareDAO.findOrCreate("Ensembl", "60");
        Organism organism = organismDAO.getOrCreateOrganism("homo sapiens");
        return annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
    }


    @Test
    @Transactional
    public void testSave() throws Exception {
        Software software = softwareDAO.findOrCreate("plants", "8");
        Organism organism = organismDAO.getByName("arabidopsis thaliana");

        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
        annotationSource.setDatabaseName("plant");
        annotationSource.setDatasetName("athaliana_eg_gene");
        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");

        BioEntityProperty goterm = propertyDAO.findOrCreate("goterm");
        Assert.assertNotNull(goterm);
        annotationSource.addBioMartProperty("name_1006", goterm);

        annSrcDAO.save(annotationSource);
        Assert.assertNotNull(annotationSource.getAnnotationSrcId());
    }

    @Test
    public void testGetById() {
        AnnotationSource annotationSource = annSrcDAO.getById(1000);
        Assert.assertNotNull(annotationSource);
    }

    @Test
    public void testGetAnnotationSourcesOfType() throws Exception {
        Collection<BioMartAnnotationSource> annotationSources = annSrcDAO.getAnnotationSourcesOfType(BioMartAnnotationSource.class);
        assertEquals(1, annotationSources.size());
    }

    @Test
    @Transactional
    public void testFindAnnotationSource() throws Exception {
        Software software = softwareDAO.findOrCreate("Ensembl", "60");
        Organism organism = organismDAO.getByName("homo sapiens");
        BioMartAnnotationSource annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
        assertNotNull(annotationSource);

        //Not existing ann src
        software = softwareDAO.findOrCreate("animals", "8");
        annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
        assertNull(annotationSource);
    }

    @Test
    public void testRemove() throws Exception {
        removeAnnSrc();
        Collection<BioMartAnnotationSource> sources = annSrcDAO.getAnnotationSourcesOfType(BioMartAnnotationSource.class);
        assertEquals(0, sources.size());
    }

    @Transactional
    private void removeAnnSrc() {
        BioMartAnnotationSource annotationSource = fetchAnnotationSource();
        assertNotNull(annotationSource);

        annSrcDAO.remove(annotationSource);
    }

    @Test
    public void testUpdate() throws Exception {
        BioMartAnnotationSource annotationSource = fetchAnnotationSource();
        assertNotNull(annotationSource);
        assertEquals(false, annotationSource.isApplied());
        annotationSource.setApplied(true);

        annSrcDAO.update(annotationSource);

        BioMartAnnotationSource annotationSource1 = fetchAnnotationSource();
        assertTrue(annotationSource1.isApplied());
    }

    @Test
    @Transactional
    public void testFindOrCreateOrganism() throws Exception {
        Organism organism = organismDAO.getOrCreateOrganism("homo sapiens");
        assertNotNull(organism);

        organism = organismDAO.getOrCreateOrganism("new organism");
        assertNotNull(organism);
        assertNotNull(organism.getId());
    }

    @Test
    @Transactional
    public void testFindOrCreateSoftware() throws Exception {
        Software software = softwareDAO.findOrCreate("Ensembl", "60");
        assertNotNull(software);

        software = softwareDAO.findOrCreate("animals", "8");
        assertNotNull(software);
        assertNotNull(software.getSoftwareid());

    }

    @Test
    @Transactional
    public void testFindOrCreateBioEntityType() throws Exception {
        BioEntityType enstranscript = typeDAO.findOrCreate("enstranscript");
        assertNotNull(enstranscript);

        BioEntityType type = typeDAO.findOrCreate("new type");
        assertNotNull(type);
        assertNotNull(type.getNameProperty().getBioEntitypropertyId());
        assertNotNull(type.getIdentifierProperty().getBioEntitypropertyId());
        assertEquals(type.getName(), type.getNameProperty().getName());
        assertEquals(type.getName(), type.getIdentifierProperty().getName());
    }

    @Test
    @Transactional
    public void testFindOrCreateBEProperty() throws Exception {
        BioEntityProperty enstranscript = propertyDAO.findOrCreate("enstranscript");
        assertNotNull(enstranscript);

        BioEntityProperty property = propertyDAO.findOrCreate("new prop");
        assertNotNull(property);
    }

    @Test
    public void testIsAnnSrcAppliedForArrayDesignMapping() throws Exception {
        BioMartAnnotationSource annotationSource = fetchAnnotationSource();
        ArrayDesign arrayDesign = arrayDesignDAO.getArrayDesignByAccession("A-AFFY-45");
        assertTrue(annSrcDAO.isAnnSrcAppliedForArrayDesignMapping(annotationSource, arrayDesign));
    }
}
