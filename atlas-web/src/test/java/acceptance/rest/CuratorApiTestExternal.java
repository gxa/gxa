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
import org.junit.Before;

import static com.jayway.restassured.RestAssured.basic;

public class CuratorApiTestExternal {


    @Before
    public void initRestAssured() {

        RestAssured.basePath = "/gxa/api/curators/v1";

        RestAssured.authentication = basic("curator", "password");

        RestAssured.rootPath = "apiShallowExperiment";

        String baseURI = System.getProperty("acceptance.rest.baseURI");

        if (baseURI != null) {

            RestAssured.baseURI = baseURI;

        }
    }


}
