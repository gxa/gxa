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
import com.jayway.jsonpath.JsonPath;
import com.jayway.restassured.RestAssured;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.io.File;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

public class PutAssayPropertiesTestExternal extends CuratorApiTestExternal {

    private static final String JSON_PROPERTIES = new TestData().readJSon("sex_property.json");

    private static final String E_TABM_1007 = "E-TABM-1007";

    private static final String A_SPECIFIC_ASSAY = "caquinof_20080327_fis2_2-v4";

    private static final String PUT_PROPERTIES_TO_ASSAY_URI = "experiments/" + E_TABM_1007 + "/assays/" + A_SPECIFIC_ASSAY + "/properties";

    private static final String MODIFIED_EXPERIMENT_URI = "experiments/" + E_TABM_1007 + ".json";

    private static final String JSONPATH_FOR_A_SPECIFIC_PROPERTY_OF_A_SPECIFIC_ASSAY =
            "$.apiShallowExperiment.assays[?(@.accession=='caquinof_20080327_fis2_2-v4')].properties[?(@.name=='sex')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_SEX_IN_ASSAYS = "$..assays.properties[?(@.name=='sex')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS = "$..assays.properties[?(@.name=='genotype')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_SEX_IN_SAMPLES = "$..samples.properties[?(@.name=='sex')]";

    private static final String JSONPATH_FOR_ALL_PROPERTIES_IN_SAMPLES = "$..samples.properties";



    @After
    public void removeProperties() throws Exception {
        given().header("Content-Type", "application/json")
                .body(JSON_PROPERTIES)
                .delete(PUT_PROPERTIES_TO_ASSAY_URI);

    }



    @Test
    public void shouldAddThePropertiesToTheSpecifiedAssayOnly() throws Exception {

        putAssayProperties();

        String jsonString = get(MODIFIED_EXPERIMENT_URI).asString();

        JsonAssert.with(jsonString)
                .assertThat(JSONPATH_FOR_A_SPECIFIC_PROPERTY_OF_A_SPECIFIC_ASSAY + ".value", hasItem("female"))
                .and()
                .assertThat(JSONPATH_FOR_A_SPECIFIC_PROPERTY_OF_A_SPECIFIC_ASSAY + ".terms", hasItem("EFO_0001265"));

    }


    @Test
    public void shouldAddThePropertiesOnlyToOneAssay() throws Exception {

        putAssayProperties();

        String jsonString = get(MODIFIED_EXPERIMENT_URI).asString();

        JsonAssert.with(jsonString)
                .assertThat(JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_SEX_IN_ASSAYS, hasSize(1));

    }


    @Test
    public void shouldNotReplaceOtherExistingPropertiesInAssays() throws Exception {

        putAssayProperties();

        String jsonString = get(MODIFIED_EXPERIMENT_URI).asString();

        JsonAssert.with(jsonString)
                .assertThat(JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_GENOTYPE_IN_ASSAYS, hasSize(6));

    }


    @Test
    public void shouldNotAddPropertiesToSamples() throws Exception {

        putAssayProperties();

        String jsonString = get(MODIFIED_EXPERIMENT_URI).asString();

        JsonAssert.with(jsonString)
                .assertThat(JSONPATH_FOR_ALL_PROPERTIES_WITH_NAME_SEX_IN_SAMPLES, is(empty()))
                .and()
                .assertThat(JSONPATH_FOR_ALL_PROPERTIES_IN_SAMPLES, hasSize(24));
    }


    private void putAssayProperties(){

        given().header("Content-Type", "application/json")
                .body(JSON_PROPERTIES)
                .expect().statusCode(HttpStatus.CREATED.value())
                .and()
                .body(isEmptyString())
                .when()
                .put(PUT_PROPERTIES_TO_ASSAY_URI);

    }


}