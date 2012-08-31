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

package uk.ac.ebi.microarray.atlas.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AssayProperty.class, SampleProperty.class})
public class ApiShallowPropertyTest{

    public static final String PROPERTY_NAME = "PROPERTY_NAME" ;
    public static final String PROPERTY_VALUE = "PROPERTY_VALUE" ;
    public static final String TERM_ACCESSION_1 = "ASSAY_TERM_ACCESSION_1" ;
    public static final String TERM_ACCESSION_2 = "ASSAY_TERM_ACCESSION_2" ;

    private ApiShallowProperty subject;

    @Mock
    private ApiProperty apiPropertyMock;

    @Mock
    private ApiProperty otherApiPropertyMock;

    @Mock
    private AssayProperty assayProperty;

    @Mock
    private SampleProperty sampleProperty;

    private Set<ApiOntologyTerm> apiOntologyTermsMock;


    @Before
    public void initApiPropertyMock() throws Exception {
        ApiOntologyTerm apiOntologyTermMock1 = mock(ApiOntologyTerm.class);
        when(apiOntologyTermMock1.getAccession()).thenReturn(TERM_ACCESSION_1);
        ApiOntologyTerm apiOntologyTermMock2 = mock(ApiOntologyTerm.class);
        when(apiOntologyTermMock2.getAccession()).thenReturn(TERM_ACCESSION_2);
        apiOntologyTermsMock = Sets.newHashSet(apiOntologyTermMock2, apiOntologyTermMock1);

        when(apiPropertyMock.getName()).thenReturn(PROPERTY_NAME);
        when(apiPropertyMock.getValue()).thenReturn(PROPERTY_VALUE);
        when(apiPropertyMock.getTerms()).thenReturn(apiOntologyTermsMock);
    }

    @Before
    public void initAssayPropertyMock() throws Exception {
        OntologyTerm ontologyTermMock1 = mock(OntologyTerm.class);
        when(ontologyTermMock1.getAccession()).thenReturn(TERM_ACCESSION_1);
        OntologyTerm ontologyTermMock2 = mock(OntologyTerm.class);
        when(ontologyTermMock2.getAccession()).thenReturn(TERM_ACCESSION_2);
        List<OntologyTerm> ontologyTermsMock = Lists.newArrayList(ontologyTermMock2, ontologyTermMock1);

        when(assayProperty.getName()).thenReturn(PROPERTY_NAME);
        when(assayProperty.getValue()).thenReturn(PROPERTY_VALUE);
        when(assayProperty.getTerms()).thenReturn(ontologyTermsMock);
    }

    @Before
    public void initSamplePropertyMock() throws Exception {
        OntologyTerm ontologyTermMock1 = mock(OntologyTerm.class);
        when(ontologyTermMock1.getAccession()).thenReturn(TERM_ACCESSION_1);
        OntologyTerm ontologyTermMock2 = mock(OntologyTerm.class);
        when(ontologyTermMock2.getAccession()).thenReturn(TERM_ACCESSION_2);
        List<OntologyTerm> ontologyTermsMock = Lists.newArrayList(ontologyTermMock2, ontologyTermMock1);

        when(sampleProperty.getName()).thenReturn(PROPERTY_NAME);
        when(sampleProperty.getValue()).thenReturn(PROPERTY_VALUE);
        when(sampleProperty.getTerms()).thenReturn(ontologyTermsMock);
    }

    @Test
    public void apiPropertyConstructorTest(){
        //given
        subject = new ApiShallowProperty(apiPropertyMock);

        //then
        assertThat(subject.getName(), is(PROPERTY_NAME));
        assertThat(subject.getValue(), is(PROPERTY_VALUE));
        assertThat(subject.getTerms().size(), is(2));
        //and verify that terms have been reordered
        assertThat(subject.getTerms().first(), is(TERM_ACCESSION_1));
        assertThat(subject.getTerms().last(), is(TERM_ACCESSION_2));
    }

    @Test
    public void assayPropertyConstructorTest(){
        //given
        subject = new ApiShallowProperty(assayProperty);

        //then
        assertThat(subject.getName(), is(PROPERTY_NAME));
        assertThat(subject.getValue(), is(PROPERTY_VALUE));
        assertThat(subject.getTerms().size(), is(2));
        //and verify that terms have been reordered
        assertThat(subject.getTerms().first(), is(TERM_ACCESSION_1));
        assertThat(subject.getTerms().last(), is(TERM_ACCESSION_2));
    }

    @Test
    public void samplePropertyConstructorTest(){
        //given
        subject = new ApiShallowProperty(sampleProperty);

        //then
        assertThat(subject.getName(), is(PROPERTY_NAME));
        assertThat(subject.getValue(), is(PROPERTY_VALUE));
        assertThat(subject.getTerms().size(), is(2));
        //and verify that terms have been reordered
        assertThat(subject.getTerms().first(), is(TERM_ACCESSION_1));
        assertThat(subject.getTerms().last(), is(TERM_ACCESSION_2));

    }

    @Test
    public void compareToShouldReturnZeroWhenObjectAreEquals(){
        //given
        subject = new ApiShallowProperty(apiPropertyMock);
        //and
        ApiShallowProperty other = new ApiShallowProperty(apiPropertyMock);

        //then
        assertThat(subject.compareTo(other), CoreMatchers.is(0));
        //and
        assertThat(subject.compareTo(other), CoreMatchers.is(0));

    }


    @Test
    public void comparisonShouldBePrioritizedOnPropertyName(){

        //given
        subject = new ApiShallowProperty(apiPropertyMock);
        //and
        given(otherApiPropertyMock.getName()).willReturn("AROPERTY_NAME"); // P > A
        given(otherApiPropertyMock.getValue()).willReturn(PROPERTY_VALUE);
        given(otherApiPropertyMock.getTerms()).willReturn(apiOntologyTermsMock);
        //and
        ApiShallowProperty other = new ApiShallowProperty(otherApiPropertyMock);

        //when
        int comparison = subject.compareTo(other);

        //then
        assertThat(comparison, greaterThan(0));

        //given
        given(otherApiPropertyMock.getName()).willReturn("ZROPERTY_NAME"); // P < Z
        given(otherApiPropertyMock.getValue()).willReturn(PROPERTY_VALUE);
        given(otherApiPropertyMock.getTerms()).willReturn(apiOntologyTermsMock);
        //and
        other = new ApiShallowProperty(otherApiPropertyMock);

        //when
        comparison = subject.compareTo(other);

        //then
        assertThat(comparison, lessThan(0));

    }


    @Test
    public void comparisonShouldBePrioritizedOnPropertyValueIfOtherHasSameName(){

        //given
        subject = new ApiShallowProperty(apiPropertyMock);
        //and
        given(otherApiPropertyMock.getName()).willReturn(PROPERTY_NAME);
        given(otherApiPropertyMock.getValue()).willReturn("AROPERTY_VALUE");  // P > A
        given(otherApiPropertyMock.getTerms()).willReturn(apiOntologyTermsMock);
        //and
        ApiShallowProperty other = new ApiShallowProperty(otherApiPropertyMock);

        //when
        int comparison = subject.compareTo(other);

        //then
        assertThat(comparison, greaterThan(0));

        //given
        given(otherApiPropertyMock.getName()).willReturn(PROPERTY_NAME);
        given(otherApiPropertyMock.getValue()).willReturn("ZROPERTY_VALUE");  // P < Z
        given(otherApiPropertyMock.getTerms()).willReturn(apiOntologyTermsMock);
        //and
        other = new ApiShallowProperty(otherApiPropertyMock);

        //when
        comparison = subject.compareTo(other);

        //then
        assertThat(comparison, lessThan(0));

    }

    @Test
    public void comparisonShouldBePrioritizedOnTermsInLastInstance(){

        //given
        subject = new ApiShallowProperty(apiPropertyMock);
        //and
        given(otherApiPropertyMock.getName()).willReturn(PROPERTY_NAME);
        given(otherApiPropertyMock.getValue()).willReturn(PROPERTY_VALUE);

        //and
        ApiOntologyTerm newTermMock = mock(ApiOntologyTerm.class);
        given(newTermMock.getAccession()).willReturn("NEW_TERM_ACCESSION");
        apiOntologyTermsMock.add(newTermMock);
        given(otherApiPropertyMock.getTerms()).willReturn(apiOntologyTermsMock); //so otherApiPropertyMock has one more term

        //and
        ApiShallowProperty other = new ApiShallowProperty(otherApiPropertyMock);

        //when
        int comparison = subject.compareTo(other);

        //then
        assertThat(comparison, greaterThan(0));

        //given
        apiOntologyTermsMock = Sets.newHashSet(newTermMock);
        given(otherApiPropertyMock.getTerms()).willReturn(apiOntologyTermsMock); //so otherApiPropertyMock has one more term

        //and
        other = new ApiShallowProperty(otherApiPropertyMock);

        //when
        comparison = subject.compareTo(other);

        //then
        assertThat(comparison, lessThan(0));

    }
}
