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

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 02/09/2011
 */
class ArrayExpressConnection {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ACC_TEML = "$ACC";
    private static final String ADF_URL_TEMPLATE = "http://www.ebi.ac.uk/arrayexpress/files/" + ACC_TEML + "/" + ACC_TEML + ".adf.txt";

    private static final String AD_NAME = "Array Design Name";
    private static final String PROVIDER = "Provider";
    private static final String TYPE = "Technology Type";

    private String name = StringUtils.EMPTY;
    private String provider = StringUtils.EMPTY;
    private String type = StringUtils.EMPTY;

    public ArrayExpressConnection(String accession) {
        fetchArrayDesignData(accession);
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getType() {
        return type;
    }

    private void fetchArrayDesignData(String accession) {
        log.info("Fetching Array Design data from ArrayExpress " + accession);

        CSVReader csvReader = null;
        URL url = null;
        try {
            url = new URL(ADF_URL_TEMPLATE.replace(ACC_TEML, accession));
            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                if (line.length >= 2) {
                    if (line[0].equals(AD_NAME)) {
                        name = line[1];
                    }
                    if (line[0].equals(PROVIDER)) {
                        provider = line[1];
                    }
                    if (line[0].equals(TYPE)) {
                        type = line[1];
                    }
                }
                //We don't need to read more then 10 line to find data we are interested in
                if (count++ > 10) break;
            }
        } catch (FileNotFoundException e) {
            log.warn("Cannot fetch ADF for array design " + accession, e.getMessage());
            setNonArrayExpressFields(accession);

        } catch (MalformedURLException e) {
            log.warn("Connection problem. Cannot fetch ADF for array design " + accession + " URL = " + url, e.getMessage());
            setFailedToFatchFields(accession);
        } catch (IOException e) {
            log.warn("Cannot fetch ADF for array design " + accession, e.getMessage());
            setFailedToFatchFields(accession);
        } finally {
            closeQuietly(csvReader);
        }
    }

    static final String NONAE_AD_NAME = "Non-ArrayExpress Array Design for accession ";
    private static final String UKNOWN_AD_NAME = "Provisional Array Design for accession ";
    private static final String UKNOWN_AD_TYPE = "PROVISIONAL";

    private void setNonArrayExpressFields(String accession) {
        name = NONAE_AD_NAME + accession;
    }

    private void setFailedToFatchFields(String accession) {
        name = UKNOWN_AD_NAME + accession;
        type = UKNOWN_AD_TYPE;
    }

}
