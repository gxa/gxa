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
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.fail;

public class GetExperimentTestExternal extends CuratorApiTestExternal {

    private static final String E_TABM_1007 = "E-TABM-1007";

    private static final String EXPERIMENT_URI = "experiments/" + E_TABM_1007 + ".json";

    private static final String A_NON_EXISTING_EXPERIMENT_URI = "experiments/Un-known.json";


    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void statusCodeShouldBeNotFoundWhenExperimentIsNotFound() throws Exception {

        given().expect().statusCode(HttpStatus.NOT_FOUND.value())
            .when().get(A_NON_EXISTING_EXPERIMENT_URI);

    }

    @Test
    public void statusCodeShuldBeOkWhenExperimentExists() throws Exception {

        expect().statusCode(HttpStatus.OK.value())
            .when().get(EXPERIMENT_URI);

    }

    @Test
    public void bodyShouldContainAccessionWhenExperimentExists() throws Exception {

        expect().body("accession", equalTo(E_TABM_1007))
            .when().get(EXPERIMENT_URI);

    }

    @Test
    public void experimentShouldContainSomeSamplesAndSomeAssays() throws Exception {

        expect().body("samples", not(empty()))
            .and()
            .body("assays", not(empty()))
            .when().get(EXPERIMENT_URI);

    }

    @Test
    public void samplesShouldContainSomeProperties() throws Exception {

        expect().body("samples[0].properties[0].name", is("organism_part"))
            .and()
            .body("samples[0].properties[0].value", is("seed"))
            .when().get(EXPERIMENT_URI);

    }

    private static final String JSONPATH_FOR_A_SPECIFIC_PROPERTY_OF_A_SPECIFIC_ASSAY =
                                "$.apiShallowExperiment.assays[?(@.accession=='caquinof_20080327_fis2_1-v4')]" +
                                ".properties[?(@.name=='genotype')]";

    @Test
    public void aSpecificAssaysShouldContainAGenotypePropertyWithAGivenValue() throws Exception {

        String modifiedExperiment = get(EXPERIMENT_URI).asString();

        with(modifiedExperiment)
            .assertThat(JSONPATH_FOR_A_SPECIFIC_PROPERTY_OF_A_SPECIFIC_ASSAY + ".value", hasItem("fis2"))
            .and()
            .assertThat(JSONPATH_FOR_A_SPECIFIC_PROPERTY_OF_A_SPECIFIC_ASSAY+".terms", is(empty()));

    }

}
