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

package acceptance.rest;

import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.*;

public class SearchPropertiesByPropertyValueWildcard extends CuratorApiTestExternal {

    private static final String BASE_URI_FOR_PARTIAL_MATCH = "propertyvaluemappings/partialmatch";

    private static final String URI_THAT_SELECTS_NOT_EXISTING_PROPERTIES = BASE_URI_FOR_PARTIAL_MATCH + "/unknown*propertyvalue.json";

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void statusCodeShouldStillBeOkWhenNoPropertyIsFound() throws Exception {
        //given
        expect().statusCode(HttpStatus.OK.value())
            .when().get(URI_THAT_SELECTS_NOT_EXISTING_PROPERTIES);

    }

    @Test
    public void bodyShouldContainAnEmptyPropertyListWhenIsNotFound() throws Exception {
        //given
        String notExistingProperties = get(URI_THAT_SELECTS_NOT_EXISTING_PROPERTIES).asString();

        //then
        with(notExistingProperties)
            .assertThat("$.apiShallowPropertyList", is(empty()));

    }

    @Test
    public void statusCodeShuldBeOkWhenPropertiesExists() throws Exception {
        //given
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + "/organism_part.json?propertyValue=R*t";

        //then
        expect().statusCode(HttpStatus.OK.value())
            .when().get(uriThatSelectsSomeProperties);

    }


    @Test
    public void searchShouldReturnAllPropertiesWhosePropertyNameMatchesTheGivenPropertyNameAndWhosePropertyValueMatchesTheGivenWildcardValue() throws Exception {
        //given
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + "/orgaNisM_part.json?propertyValue=R*t"; //* is the wildcard character

        //when
        String properties = get(uriThatSelectsSomeProperties).asString();

        //then
        with(properties).assertThat("$.apiShallowPropertyList", hasSize(greaterThan(1)))
            .and()
            .assertThat("$..name", hasItem("organism_part"))
            .and()
            .assertThat("$..value", hasItem("root"));

    }


    @Test
    public void searchShouldReturnAllPropertiesWhosePropertyValueMatchesTheGivenWildcardValue() throws Exception {
        //given
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + ".json?propertyValue=R*t"; //* is the wildcard character

        //when
        String properties = get(uriThatSelectsSomeProperties).asString();

        //then
        with(properties).assertThat("$.apiShallowPropertyList", hasSize(greaterThan(1)))
            .and()
            .assertThat("$..name", hasItem("organism_part"))
            .and()
            .assertThat("$..value", hasItem("root"));

    }


    @Test
    public void searchShouldReturnAllPropertiesWhosePropertyNameMatchesTheGivenPropertyNameAndWhosePropertyValueContainsTheGivenPropertyValue() throws Exception {
        //given
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + "/orgaNisM_part.json?propertyValue=RoO";

        //when
        String properties = get(uriThatSelectsSomeProperties).asString();

        //then
        with(properties).assertThat("$.apiShallowPropertyList", hasSize(greaterThan(1)))
            .and()
            .assertThat("$..name", hasItem("organism_part"))
            .and()
            .assertThat("$..value", hasItem("root"));

    }


    @Test
    public void searchShouldReturnAllPropertiesWhosePropertyValueContainsTheGivenPropertyValue() throws Exception {
        //given
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + "/orgaNisM_part.json?propertyValue=RoO";

        //when
        String properties = get(uriThatSelectsSomeProperties).asString();

        //then
        with(properties).assertThat("$.apiShallowPropertyList", hasSize(greaterThan(1)))
            .and()
            .assertThat("$..name", hasItem("organism_part"))
            .and()
            .assertThat("$..value", hasItem("root"));

    }
}
