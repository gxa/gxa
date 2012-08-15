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

package uk.ac.ebi.microarray.atlas.model;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertyValue.class)
public class AssayPropertyTest {

    @Mock
    private Assay assayMock;

    @Mock
    private PropertyValue propertyValueMock;

    @Mock
    private OntologyTerm term1;

    @Mock
    private OntologyTerm term2;

    @Mock
    private OntologyTerm term3;

    private List<OntologyTerm> ontologyTermsMock;

    private AssayProperty subject;

    @Before
    public void initializeMocks() throws Exception {
        propertyValueMock = PowerMockito.mock(PropertyValue.class); //B: required because the class is final :(
    }

    @Before
    public void initializeSubject() throws Exception {

        term1 = new OntologyTerm(1L, mock(Ontology.class), "TERM_1", "TERM_ACCESSION_1", "DESCRIPTION_1");
        term2 = new OntologyTerm(2L, mock(Ontology.class), "TERM_2", "TERM_ACCESSION_2", "DESCRIPTION_2");
        term3 = new OntologyTerm(3L, mock(Ontology.class), "TERM_3", "TERM_ACCESSION_3", "DESCRIPTION_3");

        subject = new AssayProperty(assayMock, propertyValueMock, Lists.newArrayList(term1, term2, term3));

    }

    @Test
    public void testRemoveTerm() throws Exception {

        //given
        OntologyTerm term = new OntologyTerm(null, null, "---", "TERM_ACCESSION_2","");

        //when
        subject.removeTerm(term);

        //then
        assertThat(subject.getTerms(), hasSize(2));
        assertThat(subject.getTerms(), not(hasItem(term)));
        assertThat(subject.getTerms(), hasItems(term1, term3));

    }

}
