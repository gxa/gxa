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
import uk.ac.ebi.gxa.test.TestData;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

public class PutAllExperimentsPropertiesTestExternal extends CuratorApiTestExternal {

    private static final String JSON_PROPERTIES = new TestData().readJSon("odd_property.json");

    private static final String E_TABM_1007 = "E-TABM-1007";

    //We run the test on a property with an odd name because we don't want the state of database to change after test
    //execution (i.e. flat down existing properties)
    private static final String ODD_PROPERTY_NAME = "xyzzyx";

    private static final String ALL_PROPERTY_NAMES_URI = "properties.json";

    private static final String PROPERTIES_FOR_ALL_EXPERIMENTS_URI = "experiments/properties";

    private static final String MODIFIED_EXPERIMENT_URI = "experiments/" + E_TABM_1007 + ".json";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS = "$..assays.properties[?(@.name=='genotype')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_SAMPLES = "$..samples.properties[?(@.name=='genotype')]";


    @After
    public void deleteAllExperimentsProperties() throws Exception {

        String allPropertyNames = get(ALL_PROPERTY_NAMES_URI).asString();

        with(allPropertyNames)
            .assertThat("$..properties", hasItem(ODD_PROPERTY_NAME));

        given().header("Content-Type", "application/json")
           .body(JSON_PROPERTIES)
           .delete(PROPERTIES_FOR_ALL_EXPERIMENTS_URI);

        allPropertyNames = get(ALL_PROPERTY_NAMES_URI).asString();

        with(allPropertyNames)
            .assertThat("$..properties", not(hasItem(ODD_PROPERTY_NAME)));

    }


    @Test
    public void shouldAddThePropertiesToAssaysAndSamplesForAllExperiments() throws Exception {

        putAllExperimentsProperties();

        //The only simple way that I can think now to test this service is pick a specific experiment
        // and runs verifications on that

        String modifiedExperiment = get(MODIFIED_EXPERIMENT_URI).asString();

        for (int i = 0; i <= 5; i++){ //E_TABM_1007 contains 6 assays

            with(modifiedExperiment)
                .assertThat("$..assays[" + i + "].properties[?(@.name=='" + ODD_PROPERTY_NAME + "')].value", hasItem("female"))
                .and()
                .assertThat("$..assays[" + i + "].properties[?(@.name=='" + ODD_PROPERTY_NAME + "')].terms", hasItem("EFO_0001265"))
                .and()
                .assertThat("$..assays.properties", hasSize(12))
                .and()
                .assertThat("$..samples[" + i + "].properties[?(@.name=='" + ODD_PROPERTY_NAME + "')].value", hasItem("female"))
                .and()
                .assertThat("$..samples[" + i + "].properties[?(@.name=='" + ODD_PROPERTY_NAME + "')].terms", hasItem("EFO_0001265"))
                .and()
                .assertThat("$..samples.properties", hasSize(30));

        }
    }


    @Test
    public void shouldNotReplaceOtherExistingPropertiesInAssaysAndSamples() throws Exception {

        putAllExperimentsProperties();

        String modifiedExperiment = get(MODIFIED_EXPERIMENT_URI).asString();

        with(modifiedExperiment)
            .assertThat(JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS, hasSize(6))
            .and()
            .assertThat(JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS, hasSize(6));

    }


    private void putAllExperimentsProperties(){

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES)
            .expect().statusCode(HttpStatus.CREATED.value())
            .and()
            .body(isEmptyString())
            .when()
            .put(PROPERTIES_FOR_ALL_EXPERIMENTS_URI);

    }

}
