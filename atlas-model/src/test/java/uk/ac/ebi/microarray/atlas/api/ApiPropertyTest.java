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
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertyValue.class, AssayProperty.class, SampleProperty.class, Property.class})

public class ApiPropertyTest {

    public static final String PROPERTY_NAME = "PROPERTY_NAME";
    public static final String PROPERTY_VALUE = "PROPERTY_VALUE";

    private ApiProperty subject; //B: ambiguous/generic name, what is an ApiProperty? A property of the APIs? What is it representing?

    @Mock
    private ApiProperty apiPropertyMock;

    @Mock
    private ApiPropertyValue apiPropertyValueMock;

    @Mock
    private ApiPropertyName apiPropertyNameMock;

    @Mock
    private ApiOntologyTerm ontologyTermMock;

    private PropertyValue propertyValueMock;

    private Property propertyMock;

    private Set<OntologyTerm> ontologyTermsMock;

    //B: 7 mock objects required to test a simple javabean (well it looks like a bean but is not)

    @Before
    public void initializeTestSubject(){

        propertyValueMock = PowerMockito.mock(PropertyValue.class); //B: PowerMock extension required because PropertyValue class is final

        propertyMock = PowerMockito.mock(Property.class); //B: PowerMock extension required because PropertyValue class is final

        when(propertyMock.getName()).thenReturn(PROPERTY_NAME);

        when(propertyValueMock.getDefinition()).thenReturn(propertyMock);

        when(propertyValueMock.getValue()).thenReturn(PROPERTY_VALUE);

        when(propertyValueMock.getDisplayValue()).thenReturn("DISPLAY_VALUE");

        when(propertyValueMock.getId()).thenReturn(0L);

        when(apiPropertyValueMock.getProperty()).thenReturn(apiPropertyNameMock);

        when(apiPropertyValueMock.getValue()).thenReturn(PROPERTY_VALUE);

        when(apiPropertyMock.getValue()).thenReturn(PROPERTY_VALUE);

        when(apiPropertyMock.getName()).thenReturn(PROPERTY_NAME);

        ontologyTermsMock = new HashSet<OntologyTerm>();

        subject = new ApiProperty(propertyValueMock, ontologyTermsMock);

    }

    @After
    public void tearDown() throws Exception {

    }

    //B: too many constructors, another symptom of data mapping galore

    @Test
    public void constructorThatAcceptsPropertyValueShouldSetTheRightNameAndValue(){

        //when
        ApiProperty apiProperty = new ApiProperty(propertyValueMock, ontologyTermsMock);

        //then
        assertThat(apiProperty.getPropertyValue().getValue(), equalTo(propertyValueMock.getValue()));
        assertThat(apiProperty.getPropertyValue().getProperty().getName(), equalTo(propertyValueMock.getDefinition().getName())); //B: 4 level of delegation to get to a property name

    }


    @Test
    public void setPropertyValueShouldDoWhatItSays() throws Exception {

        //when
        subject.setPropertyValue(apiPropertyValueMock);

        //then
        assertThat(subject.getPropertyValue(), equalTo(apiPropertyValueMock));

    }


    @Test
    public void getNameShouldGoThroughTheDelegationChainToRetrieveItsOwnName(){ //B: too bad

        //given
        given(apiPropertyValueMock.getProperty()).willReturn(apiPropertyNameMock);
        //given
        subject.setPropertyValue(apiPropertyValueMock);

        //when
        subject.getName();

        //then
        verify(apiPropertyValueMock.getProperty()).getName();
    }

    @Test
    public void getValueShouldGoThroughTheDelegationChainToRetrieveItsOwnName(){ //B: too bad

        //given
        subject.setPropertyValue(apiPropertyValueMock);

        //when
        subject.getValue();

        //then
        verify(apiPropertyValueMock).getValue();
    }


    @Test
    public void comparisonShouldReturnZeroIfNameAndValueAreEquals(){

        //given
        given(apiPropertyMock.getName()).willReturn(PROPERTY_NAME);
        given(apiPropertyMock.getValue()).willReturn(PROPERTY_VALUE);

        //then
        assertThat(subject.compareTo(apiPropertyMock), is(0));

    }


    @Test
    public void comparisonShouldBePrioritizedOnPropertyName(){

        //given
        String propertyName = apiPropertyMock.getName();
        String propertyValue = apiPropertyMock.getValue();
        given(apiPropertyMock.getName()).willReturn("AROPERTY_NAME"); // P > A
        given(apiPropertyMock.getValue()).willReturn("PROPERTY_VALUE");

        //then
        assertThat(subject.compareTo(apiPropertyMock), greaterThan(0));

        //given
        given(apiPropertyMock.getName()).willReturn("ZROPERTY_NAME"); // P < Z
        given(apiPropertyMock.getValue()).willReturn("AROPERTY_VALUE");

        //then
        assertThat(subject.compareTo(apiPropertyMock), lessThan(0));

    }


    @Test
    public void comparisonShouldBeBasedOnPropertyValueIfPropertyNamesAreEqual(){

        //given
        String propertyName = apiPropertyMock.getName();
        String propertyValue = apiPropertyMock.getValue();
        given(apiPropertyMock.getName()).willReturn("PROPERTY_NAME");
        given(apiPropertyMock.getValue()).willReturn("AROPERTY_VALUE"); // P > A

        //then
        assertThat(subject.compareTo(apiPropertyMock), greaterThan(0));

        //given
        given(apiPropertyMock.getName()).willReturn("PROPERTY_NAME");
        given(apiPropertyMock.getValue()).willReturn("ZROPERTY_VALUE"); // Z > A

        //then
        assertThat(subject.compareTo(apiPropertyMock), lessThan(0));

    }


    @Test
    public void propertiesShouldBeOrderedAutomaticallyWhenAddedToASortedSet(){

        //given
        ApiProperty apiProperty1 = buildApiProperty("A", "NOT_RELEVANT", ontologyTermsMock);
        //given
        ApiProperty apiProperty2 = buildApiProperty("B", "NOT_RELEVANT", ontologyTermsMock);
        //given
        ApiProperty apiProperty3 = buildApiProperty("C", "NOT_RELEVANT", ontologyTermsMock);
        //given
        SortedSet<ApiProperty> apiProperties = new TreeSet<ApiProperty>();

        //when
        apiProperties.add(apiProperty3);
        apiProperties.add(apiProperty2);
        apiProperties.add(apiProperty1);

        //then
        assertThat(apiProperties.first(), is(apiProperty1));
        //and
        assertThat(apiProperties.last(), is(apiProperty3));

    }

    private ApiProperty buildApiProperty(String name, String value, Set<OntologyTerm> terms){

        Property property1 = mock(Property.class);
        PropertyValue propertyValue1 = mock(PropertyValue.class);
        when(propertyValue1.getDefinition()).thenReturn(property1);
        given(property1.getName()).willReturn(name);
        given(propertyValue1.getValue()).willReturn(value);
        return new ApiProperty(propertyValue1,terms);

    }
}