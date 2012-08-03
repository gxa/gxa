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

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FindPropertiesQueryBuilderTest {

    private static final String SELECTOR_ONE_EXPRESSION = "selector_one";
    private static final String SELECTOR_TWO_EXPRESSION = "selector_two";
    private static final String VALUE_WITH_WILDCARD = "value*three";
    private static final String PARENT_ENTITY_NAME = "parent_entity";


    private static final String EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE_AND_CASE_INSENSITIVE_FALSE =
        SELECTOR_ONE_EXPRESSION + " = ?";

    private static final String EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE_AND_CASE_INSENSITIVE_TRUE =
        "upper(" + SELECTOR_ONE_EXPRESSION + ") = ?";

    private static final String EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_FALSE_AND_CASE_INSENSITIVE_FALSE =
        SELECTOR_ONE_EXPRESSION + " like ?";

    private static final String EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_FALSE_AND_CASE_INSENSITIVE_TRUE =
        "upper(" + SELECTOR_ONE_EXPRESSION + ") like ?";


    private FindPropertiesQueryBuilder subject;

    private FindPropertiesQueryBuilder subjectSpy;


    @Before
    public void initializeSubject() throws Exception {

        subject = new FindPropertiesQueryBuilder().setParentEntityName(PARENT_ENTITY_NAME);

        subjectSpy = spy(subject);
    }

    @Test
    public void defaultValueForExactMatchAndCaseInsensitiveShouldBeTrue() throws Exception {
        subject = new FindPropertiesQueryBuilder();
        assertThat(subject.isCaseInsensitive(), is(true));
        assertThat(subject.isExactMatch(), is(true));

    }

    @Test
    public void matcherConditionShouldBeEqualityWhenExactMatchIsTrueAndCaseInsensitiveIsFalse(){

        assertThat(subject.getMatcherCondition(SELECTOR_ONE_EXPRESSION, true, false)
            , is(EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE_AND_CASE_INSENSITIVE_FALSE));

    }

    @Test
    public void matcherConditionShouldBeUppercaseEqualityWhenExactMatchIsTrueAndCaseInsensitiveIsTrue(){

        assertThat(subject.getMatcherCondition(SELECTOR_ONE_EXPRESSION, true, true)
            , is(EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_TRUE_AND_CASE_INSENSITIVE_TRUE));

    }

    @Test
    public void matcherConditionShouldBeALeftAndRightLikeWhenExactMatchIsFalseAndCaseInsensitiveIsFalse(){

        assertThat(subject.getMatcherCondition(SELECTOR_ONE_EXPRESSION, false, false)
            , is(EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_FALSE_AND_CASE_INSENSITIVE_FALSE));

    }

    @Test
    public void matcherConditionShouldBeAnUppercaseLeftAndRightWhenExactMatchIsFalseAndCaseInsensitiveIsTrue(){

        assertThat(subject.getMatcherCondition(SELECTOR_ONE_EXPRESSION, false, true)
            , is(EXPECTED_MATCHER_CONDITION_FOR_EXACT_MATCH_FALSE_AND_CASE_INSENSITIVE_TRUE));

    }


    @Test
    public void queryStringForSelectPropertiesByNameAndValueShouldUseHqlBuilderTwiceAndShouldContainTheAndKeyword(){

        //when
        String queryString = subjectSpy.getQueryThatSelectsPropertiesByNameAndValue();

        //then
        assertThat(queryString, startsWith("select p from " + PARENT_ENTITY_NAME + " t left join t.properties p where "));
        //and
        assertThat(queryString, containsString(" and "));
        //and
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_VALUE_SELECTOR, true, true);
        //and the match condition for property name must be NOT case insensitive
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_NAME_SELECTOR, true, true);

    }


    @Test
    public void queryStringForSelectPropertiesByNameAndValueWithExactMatchFalseShouldNotInfluenceTheMatcherConditionOnPropertyName(){
        //given
        subjectSpy.setExactMatch(false);

        //when
        String queryString = subjectSpy.getQueryThatSelectsPropertiesByNameAndValue();

        //then
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_VALUE_SELECTOR, false, true);
        //and getMatcherCondition for property name will be invoked with exact match true anyway, because we don't want to apply an HQL like on property name
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_NAME_SELECTOR, true, true);

    }


    @Test
    public void queryStringForSelectPropertiesByValueShouldUseHqlBuildeOnlyOnce(){
        //when
        String queryString = subjectSpy.getQueryThatSelectsPropertiesByValue();

        //then
        verify(subjectSpy, times(1)).getMatcherCondition(subject.PROPERTY_VALUE_SELECTOR, true, true);

    }


    @Test
    public void queryStringForSelectPropertiesByNameAndValueShouldContainUpperHqlFunctionWhenCaseInsensitiveIsTrueAndViceversa(){

        //when
        String queryString = subject.getQueryThatSelectsPropertiesByNameAndValue();

        //then
        assertThat(queryString, containsString(" where upper("+ subject.PROPERTY_VALUE_SELECTOR +") = ? and upper("
                                                                        + subject.PROPERTY_NAME_SELECTOR + ") = ?"));

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




