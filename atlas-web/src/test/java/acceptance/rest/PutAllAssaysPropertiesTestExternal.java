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

import com.jayway.jsonassert.JsonAssert;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

public class PutAllAssaysPropertiesTestExternal extends CuratorApiTestExternal {

    private static final String JSON_PROPERTIES = new TestData().readJSon("sex_property.json");

    private static final String E_TABM_1007 = "E-TABM-1007";

    private static final String PROPERTIES_FOR_ALL_ASSAYS_URI = "experiments/" + E_TABM_1007 + "/assays/properties";

    private static final String MODIFIED_EXPERIMENT_URI = "experiments/" + E_TABM_1007 + ".json";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_SEX_IN_ASSAYS = "$..assays.properties[?(@.name=='sex')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS = "$..assays.properties[?(@.name=='genotype')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_SEX_IN_SAMPLES = "$..samples.properties[?(@.name=='sex')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_IN_SAMPLES = "$..samples.properties";


    @After
    public void deleteAllAssaysProperties() throws Exception {
        given().header("Content-Type", "application/json")
           .body(JSON_PROPERTIES)
           .delete(PROPERTIES_FOR_ALL_ASSAYS_URI);

        String modifiedExperiment = get(MODIFIED_EXPERIMENT_URI).asString();

        with(modifiedExperiment)
            .assertThat("$..assays.properties", hasSize(6));

    }


    @Test
    public void shouldAddThePropertiesToAllTheAssays() throws Exception {


        putAllAssaysProperties();

        String modifiedExperiment = get(MODIFIED_EXPERIMENT_URI).asString();

        for (int i = 0; i <= 5; i++){ //E_TABM_1007 contains 6 assays

            with(modifiedExperiment)
                .assertThat("$..assays["+i+"].properties[?(@.name=='sex')].value", hasItem("female"))
                .and()
                .assertThat("$..assays["+i+"].properties[?(@.name=='sex')].terms", hasItem("EFO_0001265"))
                .and()
                .assertThat("$..assays.properties", hasSize(12));

        }
    }


    @Test
    public void shouldNotReplaceOtherExistingPropertiesInAssays() throws Exception {

        putAllAssaysProperties();

        String modifiedExperiment = get(MODIFIED_EXPERIMENT_URI).asString();

        with(modifiedExperiment)
            .assertThat(JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS, hasSize(6));

    }


    @Test
    public void shouldNotAddPropertiesToSamples() throws Exception {

        putAllAssaysProperties();

        String modifiedExperiment = get(MODIFIED_EXPERIMENT_URI).asString();

        with(modifiedExperiment)
            .assertThat(JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_SEX_IN_SAMPLES, is(empty()))
            .and()
            .assertThat(JSONPATH_FOR_ALL_PROPERTIES_IN_SAMPLES, hasSize(24));
    }


    private void putAllAssaysProperties(){

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES)
            .expect().statusCode(HttpStatus.CREATED.value())
            .and()
            .body(isEmptyString())
            .when()
            .put(PROPERTIES_FOR_ALL_ASSAYS_URI);

    }

}
