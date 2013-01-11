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

package uk.ac.ebi.gxa.annotator.annotationsrc.arraydesign;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import static com.google.common.io.Closeables.closeQuietly;

public class SynonymsServiceClient {
    final private Logger LOGGER = LoggerFactory.getLogger(this.getClass().getSimpleName());

    private static final String ARRAY_DESIGN_SYNONYMS_URL_TEMPLATE = "http://peach.ebi.ac.uk:8480/api/arrays-secondary-accessions.txt?acc={0}";

    private static final String UNKNOWN_SYNONYM = "UNKNOWN";

    public String fetchSynonym(String arrayDesignAccession){

        InputStream inputStream = null;
        try {
            URL arrayDesignSynonymsServiceURL = new URL(synonymsServiceURL(arrayDesignAccession));
            HttpURLConnection urlConnection = (HttpURLConnection) arrayDesignSynonymsServiceURL.openConnection();
            if (urlConnection.getResponseCode() != 200) {
                return UNKNOWN_SYNONYM;
            }
            String responseMessage = urlConnection.getResponseMessage();

            LOGGER.debug("<fetchSynonym> response: " + responseMessage);

            if (StringUtils.isNotBlank(responseMessage)){
                String[] accessions = responseMessage.trim().split("\\s");
                if (accessions.length > 1){
                    return accessions[1];
                }
            }

            return StringUtils.EMPTY;

        } catch (MalformedURLException e){
            LOGGER.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return UNKNOWN_SYNONYM;
        }finally{
            closeQuietly(inputStream);
        }
    }

    protected String synonymsServiceURL(String arrayDesignAccession) {
        return MessageFormat.format(ARRAY_DESIGN_SYNONYMS_URL_TEMPLATE, arrayDesignAccession);
    }

}
