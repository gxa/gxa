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

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;

public class PutAllAssaysPropertiesTestExternal extends CuratorApiTestExternal {

    private static final String JSON_PROPERTIES = new TestData().readJSon("sex_property.json");

    private static final String E_MEXP_1007 = "E-TABM-1007";

    private static final String EXPERIMENT_URI = "experiments/" + E_MEXP_1007 + ".json";

    @After
    public void removeProperties() throws Exception {
    }

    @Test
    public void shouldAddThePropertiesToAllAssaysInTheExperiment() throws Exception {

    }


}
