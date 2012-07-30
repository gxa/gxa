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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertyValue.class, AssayProperty.class, SampleProperty.class, Property.class})

public class ApiPropertyTest {

    private ApiProperty subject; //B: ambiguous/generic name, what is an ApiProperty? A property of the APIs? What is it representing?

    @Mock
    private ApiPropertyValue apiPropertyValueMock;

    @Mock
    private ApiPropertyName apiPropertyNameMock;

    @Mock
    private ApiOntologyTerm ontologyTermMock;

    private PropertyValue propertyValueMock;

    private Property propertyMock;

    private Set<ApiOntologyTerm> ontologyTermsMock;

    //B: 7 mock objects required to test a simple javabean (well it looks like a bean but is not)

    @Before
    public void initializeTestSubject(){

        subject = new ApiProperty(apiPropertyValueMock, ontologyTermsMock);

    }

    @Before
    public void initializePropertyValueMock() throws Exception {

        propertyValueMock = PowerMockito.mock(PropertyValue.class); //B: PowerMock extension required because PropertyValue class is final

        propertyMock = PowerMockito.mock(Property.class); //B: PowerMock extension required because PropertyValue class is final

        when(propertyValueMock.getDefinition()).thenReturn(propertyMock);

        when(propertyValueMock.getValue()).thenReturn("A_VALUE");

        when(propertyValueMock.getDisplayValue()).thenReturn("A_DISPLAY_VALUE");

        when(propertyValueMock.getId()).thenReturn(0L);

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void constructorThatAcceptsPropertyValueAndTermsShouldSimplySetThem(){

        //when
        ApiProperty apiProperty = new ApiProperty(apiPropertyValueMock, ontologyTermsMock);

        //then
        assertThat(apiProperty.getPropertyValue(), equalTo(apiPropertyValueMock));
        assertThat(apiProperty.getTerms(), equalTo(ontologyTermsMock));

    }

    //B: too many constructors, another symptom of data mapping galore

    @Test
    public void constructorThatAcceptsAssayPropertyShouldSetTheRightPropertyValue(){

        AssayProperty assayPropertyMock = PowerMockito.mock(AssayProperty.class); //B: PowerMock extension required because AssayProperty class is final

        //given
        given(assayPropertyMock.getPropertyValue()).willReturn(propertyValueMock);

        //when
        ApiProperty apiProperty = new ApiProperty(assayPropertyMock);

        //then
        assertThat(apiProperty.getPropertyValue().getValue(), equalTo(assayPropertyMock.getPropertyValue().getValue()));
        assertThat(apiProperty.getPropertyValue().getProperty().getName(), equalTo(assayPropertyMock.getPropertyValue().getDefinition().getName())); //B: 4 level of delegation to get to a property name

    }

    @Test
    public void constructorThatAcceptsSamplePropertyShouldSetTheRightPropertyValue(){

        SampleProperty samplePropertyMock = PowerMockito.mock(SampleProperty.class); //B: PowerMock extension required because SampleProperty class is final

        //given
        given(samplePropertyMock.getPropertyValue()).willReturn(propertyValueMock);

        //when
        ApiProperty apiProperty = new ApiProperty(samplePropertyMock);

        //then
        assertThat(apiProperty.getPropertyValue().getValue(), equalTo(samplePropertyMock.getPropertyValue().getValue()));
        assertThat(apiProperty.getPropertyValue().getProperty().getName(), equalTo(samplePropertyMock.getPropertyValue().getDefinition().getName()));

    }

    @Test
    public void getPropertyValueShouldReturnNullWhenThePropertyIsNotInitialized() throws Exception {

        //given
        subject = new ApiProperty();

        //when
        ApiPropertyValue propertyValue = subject.getPropertyValue();

        //then
        assertThat(propertyValue, is(nullValue()));

    }

    @Test
    public void setPropertyValueShouldDoWhatItSays() throws Exception {

        //when
        subject.setPropertyValue(apiPropertyValueMock);

        //then
        assertThat(subject.getPropertyValue(), equalTo(apiPropertyValueMock));

    }

    @Test
    public void getTermsShouldReturnNullWhenThePropertyIsNotInitialized() throws Exception { //B:exposing structure, why not simply exposing terms by being an Iterable<ApiOntologyTerms>

        //given
        subject = new ApiProperty();

        //when
        Set<ApiOntologyTerm> terms = subject.getTerms();

        //then
        assertThat(terms, is(nullValue())); //B: why not return an empty set? Calling for a NullPointer

    }

    @Test
    public void setTermsShouldDoWhatItSays() throws Exception {

        //when
        subject.setTerms(ontologyTermsMock);

        //then
        assertThat(subject.getTerms(), equalTo(ontologyTermsMock));

    }

    @Test
    public void getNameShouldGoThroughTheNastyDelegationChainToRetrieveItsOwnName(){ //B: too bad

        //given
        given(apiPropertyValueMock.getProperty()).willReturn(apiPropertyNameMock);

        //when
        subject.getName();

        //then
        verify(apiPropertyValueMock.getProperty()).getName();
    }

    @Test
    public void getValueShouldGoThroughTheNastyDelegationChainToRetrieveItsOwnName(){ //B: too bad

        //when
        subject.getValue();

        //then
        verify(apiPropertyValueMock).getValue();
    }



}
