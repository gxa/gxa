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

public class SearchPropertiesByPropertyNamePartialMatch extends CuratorApiTestExternal {

    private static final String BASE_URI_FOR_PARTIAL_MATCH = "propertyvaluemappings/partialmatch";

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void searchShouldReturnAllPropertiesWhosePropertyNameMatchesTheGivenWildcardValue() throws Exception {
        //given
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + "/orga*art.json"; //* is the wildcard character

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
    public void searchShouldReturnAllPropertiesWhosePropertyNameMatchesTheGivenPropertyNameA() throws Exception {
        //given
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + "/orgaNisM_part.json";

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
        String uriThatSelectsSomeProperties = BASE_URI_FOR_PARTIAL_MATCH + "/aNisM.json";

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
