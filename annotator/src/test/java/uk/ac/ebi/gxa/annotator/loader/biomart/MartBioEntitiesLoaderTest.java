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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author Olga Melnichuk
 */
public class MartBioEntitiesLoaderTest {

    public void test() throws InvalidAnnotationDataException {
        BioMartAnnotationSource annotSource = newAnnotationSource();

        BioEntityData.Builder builder = new BioEntityData.Builder(annotSource.getOrganism());
        new MartBioEntitiesLoader(annotSource, newMartClient()).load(builder);

        BioEntityData data = builder.build(annotSource.getTypes());
        assertEquals(annotSource.getOrganism(), data.getOrganism());

        for (BioEntityType type : annotSource.getTypes()) {
            assertEquals(data.getBioEntitiesOfType(), );
        }
    }

    private BioMartAnnotationSource newAnnotationSource() {

    }
    
    private MartServiceClientImpl newMartClient() {
        return new MartServiceClientImpl() {
            @Override
            public InputStream runQuery(Collection<String> attributes) throws BioMartException, IOException {
                return super.runQuery(attributes);    //To change body of overridden methods use File | Settings | File Templates.
            }

            @Override
            public int runCountQuery(Collection<String> attributes) throws BioMartException, IOException {
                return
            }
        }
    }
}
