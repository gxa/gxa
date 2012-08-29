package acceptance.rest;/*
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

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import uk.ac.ebi.microarray.atlas.api.ApiProperty;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

public class TestData {

    public String readJSon(String filename) {
        try {
            final URL resource = TestData.class.getResource(filename);

            StringWriter writer = new StringWriter();
            IOUtils.copy(resource.openStream(), writer, "UTF-8");

            return writer.toString();
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
