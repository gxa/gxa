/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FindPropertiesQueryBuilderTest {

    private static final String SELECTOR_ONE_EXPRESSION = "selector_one";
    private static final String PARENT_ENTITY_NAME = "parent_entity";


    private static final String EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE =
            "upper(" +SELECTOR_ONE_EXPRESSION + ") = ?";

    private static final String EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE_AND_CASE_INSENSITIVE_TRUE =
        "upper(" + SELECTOR_ONE_EXPRESSION + ") = ?";

    private static final String EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_FALSE =
        "upper(" + SELECTOR_ONE_EXPRESSION + ") like ?";


    private FindPropertiesQueryBuilder subject;

    private FindPropertiesQueryBuilder subjectSpy;


    @Before
    public void initializeSubject() throws Exception {

        subject = new FindPropertiesQueryBuilder().setPropertyEntityName(PARENT_ENTITY_NAME);

        subjectSpy = spy(subject);
    }

    @Test
    public void defaultValueForExactMatchAShouldBeTrue() throws Exception {
        subject = new FindPropertiesQueryBuilder();
        assertThat(subject.isExactMatch(), is(true));

    }

    @Test
    public void matcherConditionShouldBeEqualityWhenExactMatchIsTrue(){

        assertThat(subject.getMatcherCondition(SELECTOR_ONE_EXPRESSION, true)
            , is(EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE));

    }

    @Test
    public void matcherConditionShouldBeUppercaseEqualityWhenExactMatchIsTrue(){

        assertThat(subject.getMatcherCondition(SELECTOR_ONE_EXPRESSION, true)
            , is(EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE_AND_CASE_INSENSITIVE_TRUE));

    }

    @Test
    public void matcherConditionShouldBeALeftAndRightLikeWhenExactMatchIsFalse(){

        assertThat(subject.getMatcherCondition(SELECTOR_ONE_EXPRESSION, false)
            , is(EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_FALSE));

    }

    @Test
    public void queryStringForSelectPropertiesByNameShouldUseHqlBuilderAndShouldNotContainTheAndKeyword(){

        //when
        String queryString = subjectSpy.getQueryThatSelectsPropertiesByName();

        //then
        assertThat(queryString, startsWith("select p from " + PARENT_ENTITY_NAME + " p where "));
        //and
        assertThat(queryString, not(containsString(" and ")));
        //and
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_NAME_SELECTOR, true);

    }


    @Test
    public void queryStringForSelectPropertiesByNameAndValueShouldUseHqlBuilderTwiceAndShouldContainTheAndKeyword(){

        //when
        String queryString = subjectSpy.getQueryThatSelectsPropertiesByNameAndValue();

        //then
        assertThat(queryString, startsWith("select p from " + PARENT_ENTITY_NAME + " p where "));
        //and
        assertThat(queryString, containsString(" and "));
        //and
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_VALUE_SELECTOR, true);
        //and the match condition for property name must be NOT case insensitive
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_NAME_SELECTOR, true);

    }


    @Test
    public void queryStringForSelectPropertiesByNameAndValueWithExactMatchFalseShouldNotInfluenceTheMatcherConditionOnPropertyName(){
        //given
        subjectSpy.setExactMatch(false);

        //when
        String queryString = subjectSpy.getQueryThatSelectsPropertiesByNameAndValue();

        //then getMatcherCondition for property name will be invoked with exact match true anyway, because we don't want to apply an HQL like on property name
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_NAME_SELECTOR, true);
        //and
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_VALUE_SELECTOR, false);

    }


    @Test
    public void queryStringForSelectPropertiesByValueShouldUseHqlBuildeOnlyOnce(){
        //when
        String queryString = subjectSpy.getQueryThatSelectsPropertiesByValue();

        //then
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_VALUE_SELECTOR, true);

    }


    @Test
    public void queryStringForSelectPropertiesByNameAndValueShouldContainUpperHqlFunction(){

        //when
        String queryString = subject.getQueryThatSelectsPropertiesByNameAndValue();

        //then
        assertThat(queryString, containsString(" where upper(" + subject.PROPERTY_NAME_SELECTOR +") = ? and upper("
                                                                        + subject.PROPERTY_VALUE_SELECTOR + ") = ?"));

    }


    @Test
    public void replaceWildcardWithSqlLikeSymbolTest(){

        //when
        String likeString = subject.addHqlLikeSymbols("VALUE_*_*_KNOWN");

        //then
        assertThat(likeString, is("VALUE_%_%_KNOWN"));

        //when
        likeString = subject.addHqlLikeSymbols("VALUE_**_*_KNOWN");

        //then
        assertThat(likeString, is("VALUE_%%_%_KNOWN"));

        //when
        likeString = subject.addHqlLikeSymbols("VALUE_KNOWN");

        //then
        assertThat(likeString, is("%VALUE_KNOWN%"));

    }


}




