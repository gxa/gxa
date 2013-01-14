/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.dao.arraydesign;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

import static com.google.common.io.Closeables.closeQuietly;

public class SynonymsServiceClient{
    final private Logger LOGGER = LoggerFactory.getLogger(this.getClass().getSimpleName());

    private static final int OK_RESPONSE_CODE = 200;

    private static final String ARRAY_DESIGN_SYNONYMS_URL_TEMPLATE = "http://peach.ebi.ac.uk:8480/api/arrays-secondary-accessions.txt?acc={0}";

    public String fetchAccessionMaster(String arrayDesignAccession) throws IOException{

        BufferedReader reader = null;

        try {

            InputStream inputStream = getUrlConnection(arrayDesignAccession).getInputStream();

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String firstResponseLine = reader.readLine();

            LOGGER.debug("<fetchAccessionMaster> firstResponseLine body: " + firstResponseLine);

            if (StringUtils.isNotBlank(firstResponseLine)){
                String[] accessions = firstResponseLine.trim().split("\\s");
                if (accessions.length > 1){
                    return accessions[1];
                }
            }

            return null;

        }finally{
            closeQuietly(reader);
        }
    }

    protected HttpURLConnection getUrlConnection(String arrayDesignAccession) throws IOException{

        URL arrayDesignSynonymsServiceURL = new URL(synonymsServiceURL(arrayDesignAccession));
        HttpURLConnection urlConnection = (HttpURLConnection) arrayDesignSynonymsServiceURL.openConnection();
        int responseCode = urlConnection.getResponseCode();

        if (responseCode != OK_RESPONSE_CODE) {
            throw new IllegalStateException("Response code not OK");
        }

        return urlConnection;
    }

    protected String synonymsServiceURL(String arrayDesignAccession) {
        return MessageFormat.format(ARRAY_DESIGN_SYNONYMS_URL_TEMPLATE, arrayDesignAccession);
    }

}
