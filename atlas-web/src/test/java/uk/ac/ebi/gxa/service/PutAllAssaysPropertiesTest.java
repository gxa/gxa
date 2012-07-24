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

package uk.ac.ebi.gxa.service;

import com.google.common.collect.Lists;
import org.codehaus.jackson.map.ser.PropertyBuilder;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.args4j.Argument;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import uk.ac.ebi.gxa.dao.*;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.test.TestData;
import uk.ac.ebi.microarray.atlas.api.ApiProperty;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PutAllAssaysPropertiesTest {

    private static final String FAKE_EXPERIMENT_ACCESSION = "FAKE-ACCESSION";

    private ApiProperty[] testProperties;

    private CurationService subject = new CurationService();

    @Mock
    private AssayDAO assayDAO;

    @Mock
    private AtlasDAO atlasDAO;

    @Mock
    private PropertyValueDAO propertyValueDAO;

    @Mock
    private PropertyDAO propertyDAO;

    @Mock
    private OntologyTermDAO ontologyTermDAO;

    @Mock
    private OntologyDAO ontologyDAO;

    @Mock
    private Experiment experiment;

    @Mock
    private Assay assayOne;

    @Mock
    private Assay assayTwo;


    @Before
    public void initializeApiProperties(){

        testProperties = new TestData().readJSonProperties("two_test_properties.json");

    }

    @Before
    public void setupMocks(){

        //BAD, private field injection requiring me to use a reflection tool to instrument my subject
        //BAD, 5 different service providers required to test one single flow of execution for one method
        ReflectionTestUtils.setField(subject, "atlasDAO", atlasDAO);
        ReflectionTestUtils.setField(subject, "assayDAO", assayDAO);
        ReflectionTestUtils.setField(subject, "propertyValueDAO", propertyValueDAO);
        ReflectionTestUtils.setField(subject, "ontologyDAO", ontologyDAO);
        ReflectionTestUtils.setField(subject, "ontologyTermDAO", ontologyTermDAO);

        when(experiment.getAssays()) //BAD some lack of encapsulation, Demeter violation
            .thenReturn(Lists.newArrayList(assayOne, assayTwo));

        when(assayOne.getProperties())
            .thenReturn(new ArrayList<AssayProperty>());

        when(assayTwo.getProperties())
            .thenReturn(new ArrayList<AssayProperty>());

    }

    //BAD, If things were KISS I wouldn't need to capture arguments in order to test subject behavior
    @Captor
    private ArgumentCaptor<List<OntologyTerm>> termsArgument;

    @Captor
    private ArgumentCaptor<PropertyValue> propertyValueArgument;


    @Test
    public void shouldAddTwoNewPropertiesToAllTheAssaysOfAGivenExperiment() throws ResourceNotFoundException, RecordNotFoundException {

        //given
        given(atlasDAO.getExperimentByAccession(FAKE_EXPERIMENT_ACCESSION))
            .willReturn(experiment);

        given(ontologyTermDAO.getByName(anyString()))
            .willThrow(new RecordNotFoundException()); //none of the ontology terms is found in the database

        given(propertyValueDAO.getOrCreatePropertyValue(anyString(), anyString()))
            .will(new Answer<PropertyValue>() {

                        @Override
                        public PropertyValue answer(InvocationOnMock invocationOnMock) throws Throwable {
                            String propertyName = (String)invocationOnMock.getArguments()[0];
                            String propertyValue = (String)invocationOnMock.getArguments()[1];
                            return new PropertyValue(
                                                    null,
                                                    Property.createProperty(null,    //BAD, Property nested in PropertyValue and not viceversa?
                                                                                     // Looks like a typical case of Abstraction Inversion.
                                                                            propertyName, //BAD, property second param is a 'sanitized' version of the third param. So it should not be passed at all, the Property class should simply derive it.
                                                                            propertyName)
                                                    , propertyValue);

                        }

            });

        //when
        subject.putAllAssaysProperties(FAKE_EXPERIMENT_ACCESSION, testProperties);


        //then verify that propertyValueDAO is being used properly
        verify(propertyValueDAO, times(2))
            .getOrCreatePropertyValue(testProperties[0].getPropertyValue().getProperty().getName()
                ,testProperties[0].getPropertyValue().getValue());

        verify(propertyValueDAO, times(2))
            .getOrCreatePropertyValue(testProperties[1].getPropertyValue().getProperty().getName()
                ,testProperties[1].getPropertyValue().getValue());

        //and... (the same kind of expectation should be defined also for OntologyDAO and OntologyTermDAO)

        //and that each assay is being used properly...
        for (Assay assay: experiment.getAssays()) {

            verify(assay, times(2)).addOrUpdateProperty(propertyValueArgument.capture(), termsArgument.capture());

            PropertyValue firstPropertyValue = propertyValueArgument.getAllValues().get(0);

            assertThat(firstPropertyValue.getId(), is(nullValue()));
            assertThat(firstPropertyValue.getDisplayValue(), is("PROPERTY_VALUE_1")); //BAD, PropertyValue has a private attribute DisplayValue that is never set, getDisplayValue always returns Value
            assertThat(firstPropertyValue.getValue(), is("PROPERTY_VALUE_1"));
            assertThat(firstPropertyValue.getDefinition().getId(), is(nullValue()));
            assertThat(firstPropertyValue.getDefinition().getName(), is("PROPERTY_NAME_1"));
            assertThat(firstPropertyValue.getDefinition().getDisplayName(), is("PROPERTY_NAME_1")); //BAD, it is being set to the same value as Name
            assertThat(firstPropertyValue.getDefinition().getValues(), is(empty())); //BAD, there is no way to set Values but there is a getValues() method exposed that always returns an empty list

            PropertyValue secondPropertyValue = propertyValueArgument.getAllValues().get(1);

            assertThat(secondPropertyValue.getId(), is(nullValue()));
            assertThat(secondPropertyValue.getDisplayValue(), is("PROPERTY_VALUE_2"));
            assertThat(secondPropertyValue.getValue(), is("PROPERTY_VALUE_2"));
            assertThat(secondPropertyValue.getDefinition().getId(), is(nullValue()));
            assertThat(secondPropertyValue.getDefinition().getName(), is("PROPERTY_NAME_2"));
            assertThat(secondPropertyValue.getDefinition().getDisplayName(), is("PROPERTY_NAME_2"));
            assertThat(secondPropertyValue.getDefinition().getValues(), is(empty()));

            List<OntologyTerm> firstPropertyTerms = termsArgument.getAllValues().get(0);

            assertThat(firstPropertyTerms,hasSize(1));

            List<OntologyTerm> secondPropertyTerms = termsArgument.getAllValues().get(1);

            assertThat(secondPropertyTerms,hasSize(1));

        }

    }

    @Test(expected=ResourceNotFoundException.class)
    public void shouldThrowAnExceptionWhenTheExperimentIsNotFound() throws ResourceNotFoundException, RecordNotFoundException {

        when(atlasDAO.getExperimentByAccession(anyString())).thenThrow(RecordNotFoundException.class);

        subject.putAllAssaysProperties(FAKE_EXPERIMENT_ACCESSION, testProperties);

    }


}
