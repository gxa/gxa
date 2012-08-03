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

import com.jayway.restassured.RestAssured;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.*;

public class SearchPropertiesByPropertyValueExactMatch extends CuratorApiTestExternal {

    private static final String BASE_URI_FOR_PARTIAL_MATCH = "propertyvaluemappings/exactmatch/";

    private static final String URI_THAT_SELECTS_NOT_EXISTING_PROPERTIES = BASE_URI_FOR_PARTIAL_MATCH + "unknownpropertyvalue.json";

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void statusCodeShouldBeOkWhenExperimentIsNotFound() throws Exception {
        //given
        expect().statusCode(HttpStatus.OK.value())
            .when().get(URI_THAT_SELECTS_NOT_EXISTING_PROPERTIES);

    }

    @Test
    public void bodyShouldContainAnEmptyPropertyListWhenIsNotFound() throws Exception {
        //given
        String notExistingProperties = get(URI_THAT_SELECTS_NOT_EXISTING_PROPERTIES).asString();

        //then
        with(notExistingProperties).assertThat("$.apiShallowPropertyList", is(empty()));

    }


    @Test
    public void shouldReturnOneOnlyPropertyWhenThereIsAnExactMatch() throws Exception {
        //given
        String response = get(BASE_URI_FOR_PARTIAL_MATCH + "/orGanIsm_part.json?propertyValue=rOOt").asString();

        //then
        with(response).assertThat("$.apiShallowPropertyList", hasSize(1))
                      .and()
                      .assertThat("$..name", hasItem("organism_part"))
                      .and()
                      .assertThat("$..value", hasItem("root"));

    }


}
