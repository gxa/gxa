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

public class DeleteTermsFromMatchingPropertiesOnAllAssaysTestExternal extends CuratorApiTestExternal {

    private static final String JSON_PROPERTIES_TO_BE_DELETED = new TestData().readJSon("terms_to_be_deleted_from_property_2_and_3.json");

    private static final String JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST = new TestData().readJSon("four_test_properties.json");

    private static final String E_TABM_1007 = "E-TABM-1007";

    private static final String ACCESSION_OF_THE_ASSAY_WITH_MATCHING_PROPERTIES = "caquinof_20080327_wt_2-v4";

    private static final String URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_ASSAY = "experiments/" + E_TABM_1007 + "/assays/" + ACCESSION_OF_THE_ASSAY_WITH_MATCHING_PROPERTIES + "/properties";

    private static final String URI_THAT_SELECTS_PROPERTIES_FOR_ALL_ASSAYS = "experiments/" + E_TABM_1007 + "/assays/properties";

    private static final String URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST = "experiments/" + E_TABM_1007 + ".json";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS = "$..assays.properties[?(@.name=='genotype')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_IN_SAMPLES = "$..samples.properties";


    @Before
    public void addFourTestPropertiesToASpecificAssay(){

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
            .expect().statusCode(HttpStatus.CREATED.value())
            .and()
            .body(isEmptyString())
            .when()
            .put(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_ASSAY);

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment)
            .assertThat("$..assays.properties", hasSize(10)); //6 properties (the original properties) + 4 properties (just added to one specific sample)

    }


    @After
    public void removeTheFourTestPropertiesFromTheSpecificAssay(){

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
            .delete(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_ASSAY);

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment)
            .assertThat("$..assays.properties", hasSize(6)); //the total number of properties is back its original size of 6

    }


    @Test
    public void shouldRemoveTheSpecifiedTermsFromTheMatchingPropertiesForAllAssays() throws Exception {

        deleteTermsFromMatchingPropertiesForAllAssays();

        String modifiedExperiment = get(URI_THAT_SELECTS_THE_EXPERIMENT_UNDER_TEST).asString();

        with(modifiedExperiment)
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_2')].terms", not(hasItem("TERM_ACCESSION_2_1"))) //property 2 doesn't contain anymore term 1
            .and() //and property 2 still contains term 2
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_2')].terms", hasItem("TERM_ACCESSION_2_2"))
            .and() //and property 3 doesn't have any term left
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_3')].terms", is(empty()))
            .and() // and term 1 has not been removed from property 1
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_1')].terms", hasItem("TERM_ACCESSION_1_1"))
            .and() // and term 1 has not been removed from property 4
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_4')].terms", hasItem("TERM_ACCESSION_4_1"));

    }


    private void deleteTermsFromMatchingPropertiesForAllAssays() throws Exception {
        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_DELETED)
            .delete(URI_THAT_SELECTS_PROPERTIES_FOR_ALL_ASSAYS);

    }


}