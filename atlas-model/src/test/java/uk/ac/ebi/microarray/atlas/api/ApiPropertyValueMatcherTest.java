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

import com.google.common.collect.Sets;
import org.apache.bcel.generic.ObjectType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import uk.ac.ebi.microarray.atlas.model.OntologyTerm;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiPropertyValueMatcherTest {

    @Mock
    private ApiProperty propertyOneMock;

    @Mock
    private ApiProperty propertyTwoMock;

    @Mock
    private ApiProperty propertyThreeMock;

    @Mock
    private ApiOntologyTerm ontologyTermOneMock;

    @Mock
    private ApiOntologyTerm ontologyTermTwoMock;

    private Set<ApiOntologyTerm> ontologyTermsPropertyOneMock;

    private Set<ApiOntologyTerm> ontologyTermsPropertyTwoMock;

    private ApiPropertyValueMatcher subject = new ApiPropertyValueMatcher();


    @Before
    public void initializePropertyOne() throws Exception {

        when(propertyOneMock.getName()).thenReturn("PROPERTY_NAME_ONE");
        when(propertyOneMock.getValue()).thenReturn("PROPERTY_VALUE_ONE");
        when(ontologyTermOneMock.getAccession()).thenReturn("TERM_ONE_ACCESSION");
        when(ontologyTermTwoMock.getAccession()).thenReturn("TERM_TWO_ACCESSION");
        ontologyTermsPropertyOneMock = Sets.newHashSet(ontologyTermOneMock, ontologyTermTwoMock);
        when(propertyOneMock.getTerms()).thenReturn(ontologyTermsPropertyOneMock);
    }

    @Before
    public void initializePropertyTwo() throws Exception {
        when(propertyTwoMock.getValue()).thenReturn("PROPERTY_VALUE_TWO");
        when(propertyTwoMock.getName()).thenReturn("PROPERTY_NAME_TWO");
        when(ontologyTermOneMock.getAccession()).thenReturn("TERM_ONE_ACCESSION");
        ontologyTermsPropertyTwoMock = Sets.newHashSet(ontologyTermOneMock);
        when(propertyTwoMock.getTerms()).thenReturn(ontologyTermsPropertyTwoMock);
    }

    @Before
    public void initializePropertyThree() throws Exception {
        when(propertyThreeMock.getValue()).thenReturn("PROPERTY_VALUE_THREE");
        when(propertyThreeMock.getName()).thenReturn("PROPERTY_NAME_THREE");
        when(propertyThreeMock.getTerms()).thenReturn(new HashSet<ApiOntologyTerm>());
    }

    @Before
    public void initializeSubject(){
        subject.setNameMatcher("PROPERTY_NAME_ONE");
        subject.setValueMatcher("PROPERTY_VALUE_ONE");
    }


    @Test
    public void newApiPropertyValueMatcherShouldHaveExactMatchAndCaseInsensitiveSetToTrueByDefault(){
        //given
        ApiPropertyValueMatcher matcher = new ApiPropertyValueMatcher();

        //then
        assertThat(matcher.isCaseInsensitive(), is(true));
        assertThat(matcher.isExactMatch(), is(true));

    }


    @Test
    public void addShouldDiscardNonMatchingProperties() throws Exception {

        //given
        subject.add(propertyTwoMock); // First we add property two, so that we can check that properties will be reodered
        subject.add(propertyThreeMock);
        subject.add(propertyOneMock);

        //when
        Collection<ApiShallowProperty> properties = subject.getMatchingProperties();

        //then
        assertThat(properties.size(), is(1));
        Iterator<ApiShallowProperty> iterator = properties.iterator();
        ApiShallowProperty firstProperty = iterator.next();
        assertThat(firstProperty.getName(), is("PROPERTY_NAME_ONE"));
        assertThat(firstProperty.getTerms().size(), is(2));

    }


    @Test
    public void propertyMatchingShouldBeBasedOnStringContainmnentIfExactMatchIsSetToFalse() throws Exception {

        //given
        subject.setExactMatch(false);
        subject.setNameMatcher("PROPERTY_NAME");
        subject.setValueMatcher("PROPERTY_VALUE");

        subject.add(propertyTwoMock); // First we add property two, so that we can check that properties will be reodered
        subject.add(propertyThreeMock);
        subject.add(propertyOneMock);

        //when
        Collection<ApiShallowProperty> properties = subject.getMatchingProperties();

        //then
        assertThat(properties.size(), is(3));

    }

    @Test
         public void propertyMatchingShouldBeBasedOnCaseInsensitiveStringContainmnentIfExactMatchIsSetToFalseAndCaseInsensitiveToTrue() throws Exception {

        //given
        subject.setExactMatch(false);
        subject.setNameMatcher("PROperty_NAME");
        subject.setValueMatcher("PROPErTY_VALUE");

        subject.add(propertyTwoMock);
        subject.add(propertyThreeMock);
        subject.add(propertyOneMock);

        //when
        Collection<ApiShallowProperty> properties = subject.getMatchingProperties();

        //then
        assertThat(properties.size(), is(3));

    }

    @Test
    public void propertyMatchingShouldBeBasedOnCaseSensitiveStringContainmnentIfExactMatchIsSetToFalseAndCaseInsensitiveToFalse() throws Exception {

        //given
        subject.setExactMatch(false).setCaseInsensitive(false);
        subject.setNameMatcher("PROPerTY_NAME");
        subject.setValueMatcher("PROPErTY_VALUE");

        subject.add(propertyTwoMock); // First we add property two, so that we can check that properties will be reodered
        subject.add(propertyThreeMock);
        subject.add(propertyOneMock);

        //when
        Collection<ApiShallowProperty> properties = subject.getMatchingProperties();

        //then
        assertThat(properties.size(), is(0));

    }

    @Test
    public void containsShouldBeCaseInsensitiveByDefault(){

        assertThat(subject.contains("oneword", "nEwoR"), is(true));

    }


    @Test
    public void containsShouldBeCaseSensitiveAfterSettingCaseInsensitiveToFalse(){

        subject.setCaseInsensitive(false);
        assertThat(subject.contains("oneword", "nEwoR"), is(false));

    }


    @Test
    public void exactMatchShouldBeCaseInsensitiveByDefault(){

        assertThat(subject.exactMatch("one", "OnE"), is(true));

    }


    @Test
    public void exactMatchShouldBeCaseSensitiveAfterSettingCaseInsensitiveToFalse(){

        subject.setCaseInsensitive(false);
        assertThat(subject.exactMatch("one", "OnE"), is(false));

    }


    @Test
    public void wildcardCharacterShouldRequireContainmentOfTheSplittedChunksOfTheMatcher(){
        //when
        boolean match = subject.partialMatch("VALUE_TO_BE_MATCHED", "E_TO_BE");

        //then
        assertThat(match, is(true));


        //when
        match = subject.partialMatch("VALUE_TO_BE_MATCHED", "HEDS");

        //then
        assertThat(match, is(false));


        //when
        match = subject.partialMatch("VALUE_TO_BE_MATCHED", "VALUE_TO_BE_MATCHED");

        //then
        assertThat(match, is(true));


        //when
        match = subject.partialMatch("VALUE_TO_BE_MATCHED", "VALUE_*AT*CHED");

        //then
        assertThat(match, is(true));



        //when
        match = subject.partialMatch("VALUE_TO_BE_MATCHED", "VALUE_*OAT*CHED");

        //then
        assertThat(match, is(false));
    }

}
