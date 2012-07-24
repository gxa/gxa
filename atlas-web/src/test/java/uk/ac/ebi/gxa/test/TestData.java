package uk.ac.ebi.gxa.test;/*
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

import com.google.common.io.Files;
import com.google.gson.Gson;
import uk.ac.ebi.microarray.atlas.api.ApiProperty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class TestData {

    private static final String TEST_DATA_RELATIVE_PATH = "atlas-web/src/test/resources/testdata/" ;



    public File getDataFile(String filename) {

        return new File(TEST_DATA_RELATIVE_PATH + filename);

    }



    public String readJSon(String filename) {

        try {

            File testFile = getDataFile(filename);

            return Files.toString(testFile, Charset.forName("UTF-8"));

        } catch (IOException e) {

            throw new AssertionError(e);

        }

    }


    public ApiProperty[] readJSonProperties(String filename) {

        String json = readJSon(filename);

        Gson gson = new Gson();

        ApiProperty[] properties = gson.fromJson(json, ApiProperty[].class);

        return properties;

    }

}