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

public class RemapTermsOnMatchingPropertiesForAllExperiments extends CuratorApiTestExternal {

    private static final String JSON_PROPERTIES_TO_BE_REMAPPED = new TestData().readJSon("four_test_properties.json");

    private static final String JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST = new TestData().readJSon("property_2_and_3.json");

    private static final String URI_THAT_SELECTS_ALL_PROPERTY_NAMES = "properties.json";

    private static final String URI_THAT_SELECTS_PROPERTIES_FOR_ALL_EXPERIMENTS = "experiments/properties";

    private static final String URI_THAT_SELECTS_EXPERIMENTS_BY_PROPERTY_VALUE = "experiments/properties/PROPERTY_NAME_2.json?propertyValue=PROPERTY_VALUE_2";

    private static final String E_TABM_1007 = "E-TABM-1007";

    private static final String ACCESSION_OF_THE_ASSAY_WITH_MATCHING_PROPERTIES = "caquinof_20080327_wt_2-v4";

    private static final String URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_ASSAY = "experiments/" + E_TABM_1007 + "/assays/" + ACCESSION_OF_THE_ASSAY_WITH_MATCHING_PROPERTIES + "/properties";

    private static final String URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_SAMPLE = "experiments/" + E_TABM_1007 + "/samples/" + ACCESSION_OF_THE_ASSAY_WITH_MATCHING_PROPERTIES + "/properties";

    private static final String MODIFIED_EXPERIMENT_URI = "experiments/" + E_TABM_1007 + ".json";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS = "$..assays.properties[?(@.name=='genotype')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_SAMPLES = "$..samples.properties[?(@.name=='genotype')]";


    @Before
    public void addProperty2And3ToASpecificAssay() throws Exception {

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
            .put(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_ASSAY);

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
            .put(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_SAMPLE);

    }

    @After
    public void deleteProperty2And3FromAllExperiments() throws Exception {

        given().header("Content-Type", "application/json")
           .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
           .delete(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_ASSAY);

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_ADDED_BEFORE_THE_TEST)
            .delete(URI_THAT_SELECTS_PROPERTIES_OF_A_SPECIFIC_SAMPLE);

    }


    @Test
    public void shouldRemapTermsOnAllTheMatchingPropertiesForAllAssaysAndSamplesInAllExperiments() throws Exception {

        remapTerms();

        String modifiedExperiments = get(URI_THAT_SELECTS_EXPERIMENTS_BY_PROPERTY_VALUE).asString();

        with(modifiedExperiments)
            .assertThat("$.apiShallowExperimentList", hasSize(1)) //only one experiment has at least one of the 4 input properties
            .and() //that experiment is the one we prepared in the test fixture
            .assertThat("$.apiShallowExperimentList[0].accession", equalTo(E_TABM_1007))
            .and() //and the terms of assay property 2 have been replaced with the two new values
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_2')][?(@.value=='PROPERTY_VALUE_2')].terms", contains("TERM_ACCESSION_2_1", "TERM_ACCESSION_2_2"))
            .and() //and the terms of assay property 3 have been replaced with the one new value
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_3')][?(@.value=='PROPERTY_VALUE_3')].terms", hasItem("TERM_ACCESSION_3_1"))
            .and() //and the terms of sample property 2 have been replaced with the two new values
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_2')][?(@.value=='PROPERTY_VALUE_2')].terms", contains("TERM_ACCESSION_2_1", "TERM_ACCESSION_2_2"))
            .and() //and the terms of sample property 3 have been replaced with the one new value
            .assertThat("$..assays.properties[?(@.name=='PROPERTY_NAME_3')][?(@.value=='PROPERTY_VALUE_3')].terms", hasItem("TERM_ACCESSION_3_1"));

    }


    private void remapTerms(){

        given().header("Content-Type", "application/json")
            .body(JSON_PROPERTIES_TO_BE_REMAPPED)
            .expect().statusCode(HttpStatus.CREATED.value())
            .and()
            .body(isEmptyString())
            .when()
            .put(URI_THAT_SELECTS_PROPERTIES_FOR_ALL_EXPERIMENTS);

    }

}
