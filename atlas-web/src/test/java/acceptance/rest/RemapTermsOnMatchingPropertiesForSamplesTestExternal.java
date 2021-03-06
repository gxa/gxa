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
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

public class RemapTermsOnMatchingPropertiesForSamplesTestExternal extends CuratorApiTestExternal {

    private static final String JSON_PROPERTIES_TO_BE_REMAPPED = new TestData().readJSon("four_test_properties.json");

    private static final String JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST = new TestData().readJSon("property_2_and_3.json");

    private static final String E_TABM_1007 = "E-TABM-1007";

    private static final String ACCESSION_OF_THE_SAMPLE_WITH_MATCHING_PROPERTIES = "caquinof_20080327_wt_2-v4";

    private static final String URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_SAMPLE = "experiments/" + E_TABM_1007 + "/samples/" + ACCESSION_OF_THE_SAMPLE_WITH_MATCHING_PROPERTIES + "/properties";

    private static final String URI_THAT_SELECTS_PROPERTIES_FOR_ALL_SAMPLES = "experiments/" + E_TABM_1007 + "/samples/properties";

    private static final String URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST = "experiments/" + E_TABM_1007 + ".json";


    @Before //let's add two temporary properties (we will remove them in the teardown), one with one ontology term and the other without any term,
            //we will then base our test on these temporary property, to avoid corrupting pre-existing data
    public void addProperty2And3ToASpecificSample() {
        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
            .put(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_SAMPLE);

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment)
            .assertThat("$..samples.properties", hasSize(26)) //the number of property is 24 (original properties) + 2 (just added)
            .and() // the number of terms is now 12 + 1
            .assertThat("$..samples.properties.terms", hasSize(13));
    }

    @After //we must remove the temporary properties that we added in the test fixture
    public void deleteProperty2And3FromASpecificSample() throws Exception {
        given().header("Content-Type", "application/json")
           .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
           .delete(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_SAMPLE);

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment)
            .assertThat("$..samples.properties", hasSize(24))
            .and()
            .assertThat("$..samples.properties.terms", hasSize(12));

    }


    @Test
    public void shouldRemapOntologyTermsOnAllMatchingPropertiesforAllTheSamples() throws Exception {

        addOntologyMappingsToMatchingSamplesProperties();

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment) //total number of properties on all samples is still 26, no new property added
            .assertThat("$..samples.properties", hasSize(26))
            .and() //total number of terms on all terms is now 12 (original properties) + 3 (effect of the remapping)
            .assertThat("$..samples.properties.terms", hasSize(15))
            .and() //two new terms have replaced pre-existing term EFO_0001215 on property 2
            .assertThat("$..samples.properties[?(@.name=='PROPERTY_NAME_2')][?(@.value=='PROPERTY_VALUE_2')].terms", contains("TERM_ACCESSION_2_1", "TERM_ACCESSION_2_2"))
            .and() //property 3 has now one term
            .assertThat("$..samples.properties[?(@.name=='PROPERTY_NAME_3')][?(@.value=='PROPERTY_VALUE_3')].terms", hasItem("TERM_ACCESSION_3_1"));

    }


    @Test
    public void shouldNotAddNewPropertiesToAnySample() throws Exception {

        addOntologyMappingsToMatchingSamplesProperties();

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment)
            .assertThat("$..samples.properties.name", not(hasItems("PROPERTY_NAME_1", "PROPERTY_NAME_4"))); //should not contain property 1 and 4

    }


    @Test
    public void shouldNotHaveAnyEffectOnAssays() throws Exception {

        addOntologyMappingsToMatchingSamplesProperties();

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment)
            .assertThat("$..assays.properties", hasSize(6))
            .and()
            .assertThat("$..assays.properties.terms", not(hasItems("TERM_2_1", "TERM_2_2", "TERM_3_1")));
    }


    private void addOntologyMappingsToMatchingSamplesProperties(){

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_REMAPPED)
            .expect().statusCode(HttpStatus.CREATED.value())
            .and()
            .body(isEmptyString())
            .when()
            .put(URI_THAT_SELECTS_PROPERTIES_FOR_ALL_SAMPLES);

    }

}
