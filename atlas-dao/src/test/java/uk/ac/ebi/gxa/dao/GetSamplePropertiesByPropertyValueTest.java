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

package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.orm.hibernate3.HibernateTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GetSamplePropertiesByPropertyValueTest {

    private static final String PROPERTY_NAME_SELECTOR_EXPRESSION = "property_name_selector";
    private static final String PROPERTY_VALUE_SELECTOR_EXPRESSION = "property_value_selector";
    private static final String VALUE_ONE = "value_one";
    private static final String VALUE_TWO = "value_two";

    private static final String QUERY_STRING_WITH_EXACT_MATCH_AND_CASE_INSENSITIVE =
        "select p from Sample t left join t.properties p where upper()";

    @Mock
    private FindPropertiesQueryBuilder queryBuilderMock;

    @Mock
    private HibernateTemplate hibernateTemplateMock;

    @Mock
    private SessionFactory sessionFactory;

    private SampleDAO subject;

    //private SampleDAO subjectSpy;


    @Before
    public void initializeQueryBuilderMock() throws Exception {

        when(queryBuilderMock.setCaseInsensitive(anyBoolean())).thenReturn(queryBuilderMock);
        when(queryBuilderMock.setExactMatch(anyBoolean())).thenReturn(queryBuilderMock);
        when(queryBuilderMock.setPropertyEntityName(anyString())).thenReturn(queryBuilderMock);

    }



    @Before
    public void initializeSubject() throws Exception {

        subject = new SampleDAO(sessionFactory);
        subject.template = hibernateTemplateMock;
        subject.setFindPropertiesQueryBuilder(queryBuilderMock);

    }


    @Test
    public void shouldInitializeQueryBuilderWithTheRightEntityName() throws Exception {
        //when
        subject.getSamplePropertiesByPropertyValue(null, VALUE_TWO, true, true);

        //then
        verify(queryBuilderMock).setPropertyEntityName("SampleProperty");
    }


    @Test
    public void shouldPropagateRequestParametersToQueryBuilder() throws Exception {
        //when
        subject.getSamplePropertiesByPropertyValue(null, VALUE_TWO, true, true);

        //then
        verify(queryBuilderMock).setExactMatch(true);
        verify(queryBuilderMock).setCaseInsensitive(true);

        //when
        subject.getSamplePropertiesByPropertyValue(null, VALUE_TWO, false, false);

        //then
        verify(queryBuilderMock).setExactMatch(false);
        verify(queryBuilderMock).setCaseInsensitive(false);

    }

    @Test
    public void shouldGetAQueryThatSelectsPropertiesByValueWhenPropertyNameIsNull() throws Exception {
        //when
        subject.getSamplePropertiesByPropertyValue(null, VALUE_TWO, true, true);

        //then
        verify(queryBuilderMock).getQueryThatSelectsPropertiesByValue();
        //and
        verify(queryBuilderMock, never()).getQueryThatSelectsPropertiesByNameAndValue();
    }


    @Test
    public void shouldGetAQueryThatSelectsPropertiesByNameAndValueWhenPropertyNameIsNotNull() throws Exception {

        //when
        subject.getSamplePropertiesByPropertyValue(VALUE_ONE, VALUE_TWO, true, true);

        //then
        verify(queryBuilderMock, times(1)).getQueryThatSelectsPropertiesByNameAndValue();
        //and
        verify(queryBuilderMock, never()).getQueryThatSelectsPropertiesByValue();
    }


    @Test
    public void shouldPropagateRequestParametersToQueryBuilderAndThenGetAQueryThatSelectsPropertiesByValueWhenPropertyNameIsNull() throws Exception {
        //when
        subject.getSamplePropertiesByPropertyValue(null, VALUE_TWO, true, true);

        //then
        verify(queryBuilderMock).setPropertyEntityName("SampleProperty");
        verify(queryBuilderMock).setExactMatch(true);
        verify(queryBuilderMock).setCaseInsensitive(true);
        verify(queryBuilderMock).getQueryThatSelectsPropertiesByValue();
        //and
        verify(queryBuilderMock, never()).getQueryThatSelectsPropertiesByNameAndValue();
    }


    @Captor
    private ArgumentCaptor<String> valueOneCaptor;

    @Captor
    private ArgumentCaptor<String> valueTwoCaptor;

    @Captor
    private ArgumentCaptor<String> queryStringCaptor;

    @Test
    public void shouldInvokeFindPassingUppercaseParametersWhenCaseInsensitiveIsTrue() throws Exception {

        //when
        subject.getSamplePropertiesByPropertyValue(VALUE_ONE, VALUE_TWO, true, true);

        //then
        verify(hibernateTemplateMock).find(queryStringCaptor.capture(), valueOneCaptor.capture(), valueTwoCaptor.capture());
        assertThat(valueOneCaptor.getValue(), is(VALUE_ONE.toUpperCase()));
        assertThat(valueTwoCaptor.getValue(), is(VALUE_TWO.toUpperCase()));

    }

    @Test
    public void shouldInvokeFindPassingParametersInTheirOriginalCasingWhenCaseInsensitiveIsFalse() throws Exception {

        //when
        subject.getSamplePropertiesByPropertyValue(VALUE_ONE, VALUE_TWO, true, false);

        //then
        verify(hibernateTemplateMock).find(queryStringCaptor.capture(), valueOneCaptor.capture(), valueTwoCaptor.capture());
        assertThat(valueOneCaptor.getValue(), is(VALUE_ONE));
        assertThat(valueTwoCaptor.getValue(), is(VALUE_TWO));

    }


}




