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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class OntologyTermEqualityAndHashCodeTest {

    private OntologyTerm subject;

    @Mock
    OntologyTerm ontologyTermMock;


    @Mock
    Ontology ontologyMock;


    @Before
    public void initializeOntologyTerm(){

        subject = new OntologyTerm(null, ontologyMock, "TERM_VALUE", "ACCESSION_VALUE", "DESCRIPTION_VALUE");

    }

    @Test
    public void hashCodeShouldBeTheSameAsAccessionHashCode() throws Exception {

        //when
        int hashCode = subject.hashCode();

        //then
        assertThat(hashCode, is("ACCESSION_VALUE".hashCode()));

    }


    @Test
    public void equalsShouldSimplyMatchTheAccessionValue() throws Exception {

        //given
        OntologyTerm other = new OntologyTerm(null, ontologyMock, "--", "ACCESSION_VALUE", "--");


        //then
        assertThat(subject.equals(other), is(true));
        //and
        assertThat(other.equals(subject), is(true));

    }


    @Test
    public void equalsShouldFailWhenAccessionValuesAreDifferent() throws Exception {

        //given
        given(ontologyTermMock.getAccession())
            .willReturn("---");

        //then
        assertThat(subject.equals(ontologyTermMock), is(false));

    }


    @Test
    public void equalsShouldFailWhenComparedObjectsIsNull() throws Exception {

        //given
        OntologyTerm other = null;

        //then
        assertThat(subject.equals(other), is(false));

    }


    @Test
    public void equalityTestShouldFailWhenEitherObjectsHasANullAccession() throws Exception {

        //given
        OntologyTerm other = new OntologyTerm(null, ontologyMock, "TERM_VALUE_X", "ACCESSION_VALUE", "DESCRIPTION_VALUE_X");

        //when
        subject.setAccession(null);

        //then
        assertThat(subject.equals(other), is(false));
        //and
        assertThat(other.equals(subject), is(false));

    }


    @Test
    public void equalityTestShouldSucceedWhenBothObjectsHaveANullAccession() throws Exception {

        //given
        OntologyTerm other = new OntologyTerm(null, ontologyMock, "TERM_VALUE_X", null, "DESCRIPTION_VALUE_X");

        //when
        subject.setAccession(null);

        //then
        assertThat(subject.equals(other), is(true));

    }


    @Test
    public void equalityTransitivity() throws Exception {

        //given
        OntologyTerm other1 = new OntologyTerm(null, ontologyMock, null, "ACCESSION_VALUE", "DESCRIPTION_VALUE_A");
        OntologyTerm other2 = new OntologyTerm(null, ontologyMock, null, "ACCESSION_VALUE",  "DESCRIPTION_VALUE_B");

        //then
        assertThat(subject.equals(other1), is(true));
        assertThat(other2.equals(other1), is(true));
        assertThat(other2.equals(subject), is(true));

    }

}
