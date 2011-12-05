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

package uk.ac.ebi.gxa.dao;

import org.junit.Test;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;

import java.util.List;

public class TestBioEntityDAO extends AtlasDAOTestCase {
    @Test
    public void testGetAllGenes() throws Exception {
        int expected = 1;

        // get number of experiments from the DAO
        int actual = bioEntityDAO.getAllGenesFast().size();

        // test data contains 2 experiments, check size of returned list
        assertEquals("Wrong number of genes", expected, actual);
    }

    @Test
    public void testGetPropertiesForGenes() throws Exception {
        List<BioEntity> bioEntities = bioEntityDAO.getAllGenesFast();

        // use dao to get properties
        bioEntityDAO.getPropertiesForGenes(bioEntities);

        // now check properties on each gene, compared with dataset
        for (BioEntity bioEntity : bioEntities) {
            List<BEPropertyValue> props = bioEntity.getProperties();

            for (BEPropertyValue prop : props) {
                //loop over properties in the dataset to make sure we can find a matching one
                boolean found = false;
                int rows = getDataSet().getTable("A2_BIOENTITYPROPERTY").getRowCount();

                assertTrue(rows > 0);

                for (int i = 0; i < rows; i++) {
                    String propName =
                            getDataSet().getTable("A2_BIOENTITYPROPERTY").getValue(i, "name")
                                    .toString();

                    if (propName.equals(prop.getProperty().getName())) {
                        System.out.println(
                                "Expected property: " + propName + ", " +
                                        "actual property: " + prop.getProperty().getName());
                        found = true;
                        break;
                    }
                }

                assertTrue("Couldn't find Gene property named " + prop.getProperty().getName(),
                        found);
            }
        }
    }

    @Test
    public void testWriteBioEntityToPropertyValuesChecked() throws Exception {

    }
}
