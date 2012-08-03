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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.microarray.atlas.api.ApiProperty;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentDAOTest {


    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private HibernateTemplate hibernateTemplateMock;

    @Mock
    private ApiProperty property1Mock;

    @Mock
    private ApiProperty property2Mock;

    @Mock
    private ApiProperty property3Mock;

    private ApiProperty[] apiPropertiesMock;

    private ExperimentDAO subject;

    @Before
    public void initializeSubject() throws Exception {

       subject = new ExperimentDAO(sessionFactory);

    }

    @Before
    public void initializeMocks() throws Exception {

        when(property1Mock.getName()).thenReturn("N1");
        when(property2Mock.getName()).thenReturn("N2");
        when(property3Mock.getName()).thenReturn("N3");
        when(property1Mock.getValue()).thenReturn("V1");
        when(property2Mock.getValue()).thenReturn("V2");
        when(property3Mock.getValue()).thenReturn("V3");

        apiPropertiesMock = new ApiProperty[]{

            property1Mock, property2Mock, property3Mock

        };
    }

    @Captor
    private ArgumentCaptor<String> queryString;

    @Captor
    private ArgumentCaptor<String[]> queryArguments;

    @Test
    public void testGetExperimentsByProperties() throws Exception {

        subject.template = hibernateTemplateMock; //B: bad, and I had to remove the final keyword in order to mock template

        //when
        subject.getExperimentsByProperties(apiPropertiesMock);

        //then
        verify(hibernateTemplateMock).find(anyString(), eq("N1"), eq("V1"), eq("N2"), eq("V2"), eq("N3"), eq("V3")) ;

    }


    @Test
    public void getPropertyMatchingArguments() {

        //when
        subject.getPropertyMatchingArguments(apiPropertiesMock);

        //then
        InOrder inOrder = inOrder(property1Mock, property2Mock, property3Mock);

        inOrder.verify(property1Mock).getName();
        inOrder.verify(property1Mock).getValue();
        inOrder.verify(property2Mock).getName();
        inOrder.verify(property2Mock).getValue();
        inOrder.verify(property3Mock).getName();
        inOrder.verify(property3Mock).getValue();

    }


}
