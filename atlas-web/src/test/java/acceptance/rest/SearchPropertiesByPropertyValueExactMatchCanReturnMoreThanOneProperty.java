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
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

public class SearchPropertiesByPropertyValueExactMatchCanReturnMoreThanOneProperty extends CuratorApiTestExternal {

    private static final String BASE_URI_FOR_PARTIAL_MATCH = "propertyvaluemappings/exactmatch/";

    private static final String JSON_PROPERTIES = new TestData().readJSon("one_mapping_to_be_added_to_assay_Low_168h_5_H8.json");

    private static final String E_TABM_105 = "E-TABM-105";

    private static final String A_SPECIFIC_ASSAY = "Low_168h_5_H8";

    private static final String PROPERTIES_FOR_A_SPECIFIC_SAMPLE_URI = "experiments/" + E_TABM_105 + "/samples/" + A_SPECIFIC_ASSAY + "/properties";

    private static final String MODIFIED_EXPERIMENT_URI = "experiments/" + E_TABM_105 + ".json";


    @Before
    public  void putAssayProperties(){

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES)
            .expect().statusCode(HttpStatus.CREATED.value())
            .and()
            .body(isEmptyString())
            .when()
            .put(PROPERTIES_FOR_A_SPECIFIC_SAMPLE_URI);

    }

    @After
    public void deletePropertiesFromSpecificAssay() throws Exception {
        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES)
            .delete(PROPERTIES_FOR_A_SPECIFIC_SAMPLE_URI);

        String modifiedExperiment = get(MODIFIED_EXPERIMENT_URI).asString();

    }


    @Test
    public void shouldReturnMoreThanOnePropertyWhenThereAreMultipleExactMatches() throws Exception {
        //given
        String response = get(BASE_URI_FOR_PARTIAL_MATCH + "/orGanIsm_part.json?propertyValue=liver").asString();

        //then
        with(response).assertThat("$.apiShallowPropertyList", hasSize(3))
            .and()
            .assertThat("$..name", hasItem("organism_part"))
            .and()
            .assertThat("$..value", hasItem("liver"));

    }


}
