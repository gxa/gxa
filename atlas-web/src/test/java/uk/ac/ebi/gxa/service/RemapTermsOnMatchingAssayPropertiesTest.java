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

import acceptance.rest.TestData;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import uk.ac.ebi.microarray.atlas.api.ApiProperty;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RemapTermsOnMatchingAssayPropertiesTest {

    private static final String FAKE_EXPERIMENT_ACCESSION = "FAKE-ACCESSION";

    private ApiProperty[] testProperties;

    private CurationService subject = new CurationService();

    @Mock
    private AssayDAO assayDAOMock;

    @Mock
    private AtlasDAO atlasDAOMock;

    @Mock
    private PropertyValueDAO propertyValueDAOMock;

    @Mock
    private PropertyDAO propertyDAOMock;

    @Mock
    private OntologyTermDAO ontologyTermDAOMock;

    @Mock
    private OntologyDAO ontologyDAOMock;

    @Mock
    private Experiment experimentMock;

    @Mock
    private Assay assayOneMock;

    @Mock
    private Assay assayTwoMock;


    @Before
    public void initializeApiProperties(){

        testProperties = new TestData().readJSonProperties("two_test_properties.json");

    }

    @Before
    public void setupMocks(){

        //B: private field injection requiring me to use a reflection tool to instrument my subject
        //B: 5 different service providers required to test one single flow of execution for one method
        ReflectionTestUtils.setField(subject, "atlasDAO", atlasDAOMock);
        ReflectionTestUtils.setField(subject, "assayDAO", assayDAOMock);
        ReflectionTestUtils.setField(subject, "propertyValueDAO", propertyValueDAOMock);
        ReflectionTestUtils.setField(subject, "ontologyDAO", ontologyDAOMock);
        ReflectionTestUtils.setField(subject, "ontologyTermDAO", ontologyTermDAOMock);

        when(experimentMock.getAssays()) //BAD some lack of encapsulation, Demeter violation
            .thenReturn(Lists.newArrayList(assayOneMock, assayTwoMock));

        when(assayOneMock.getProperties())
            .thenReturn(new ArrayList<AssayProperty>());

        when(assayTwoMock.getProperties())
            .thenReturn(new ArrayList<AssayProperty>());

    }

    //B: If things were KISS I wouldn't need to capture arguments in order to test subject behavior
    @Captor
    private ArgumentCaptor<List<OntologyTerm>> termsArgument;

    @Captor
    private ArgumentCaptor<PropertyValue> propertyValueArgument;


    @Test
    public void shouldAddTwoNewPropertiesToAllTheAssaysOfAGivenExperiment() throws ResourceNotFoundException, RecordNotFoundException {

        //given
        given(atlasDAOMock.getExperimentByAccession(FAKE_EXPERIMENT_ACCESSION))
            .willReturn(experimentMock);

        given(ontologyTermDAOMock.getByName(anyString()))
            .willThrow(new RecordNotFoundException()); //none of the ontology terms is found in the database

        given(propertyValueDAOMock.getOrCreatePropertyValue(anyString(), anyString()))
            .will(new Answer<PropertyValue>() {

                        @Override
                        public PropertyValue answer(InvocationOnMock invocationOnMock) throws Throwable {
                            String propertyName = (String)invocationOnMock.getArguments()[0];
                            String propertyValue = (String)invocationOnMock.getArguments()[1];
                            return new PropertyValue(
                                                    null,
                                                    Property.createProperty(null,    //B: property nested in PropertyValue and not viceversa?
                                                                                     // Looks like a typical case of Abstraction Inversion.
                                                                            propertyName, //B: property second param is a 'sanitized' version of the third param. So it should not be passed at all, the Property class should simply derive it.
                                                                            propertyName)
                                                    , propertyValue);

                        }

            });

        //when
        subject.remapTermsOnMatchingAssayProperties(FAKE_EXPERIMENT_ACCESSION, testProperties);

        //then
        verify(atlasDAOMock).getExperimentByAccession(FAKE_EXPERIMENT_ACCESSION);
        //and that each assay is being used properly...
        for (ApiProperty property: testProperties){
            for (Assay assayMock: experimentMock.getAssays()) {

                verify(assayMock).getProperties(property.getName(), property.getValue());

                for (AssayProperty assayProperty: assayMock.getProperties()){
                    verify(assayProperty).setTerms(anyListOf(OntologyTerm.class));
                    verify(assayDAOMock).saveAssayProperty(assayProperty);
                }

            }
        }

    }

    @Test(expected=ResourceNotFoundException.class)
    public void shouldThrowAnExceptionWhenTheExperimentIsNotFound() throws ResourceNotFoundException, RecordNotFoundException {
        //given
        given(atlasDAOMock.getExperimentByAccession(anyString())).willThrow(RecordNotFoundException.class);

        //when
        subject.remapTermsOnMatchingAssayProperties(FAKE_EXPERIMENT_ACCESSION, testProperties);

        //then a ResourceNotFoundException will be thrown

    }


}
