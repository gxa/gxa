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

package uk.ac.ebi.gxa.dao.arraydesign;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SynonymsServiceClientTestIT {

    private static final String ARRAY_DESIGN_ACCESSION = "A-GEOD-9419";

    private SynonymsServiceClient subject = new SynonymsServiceClient();

    @Test
    public void rightAccessionMasterShouldBeReturnedInCaseOfKnownSynonym() throws Exception {
        String accessionMaster = subject.fetchAccessionMaster(ARRAY_DESIGN_ACCESSION);
        assertThat(accessionMaster, is("A-AFFY-44"));
    }

    @Test
    public void accessionMasterShouldBeEmptyStringInCaseOfUnknownSynonym() throws Exception {
        String accessionMaster = subject.fetchAccessionMaster("XYZ");
        assertThat(accessionMaster, is(nullValue()));
    }

}
