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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import au.com.bytecode.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 19/01/2012
 */
class MartAttributes {

    /**
     * Parses attributes from Biomart, e.g. http://www.ensembl.org/biomart/martservice?type=attributes&dataset=hsapiens_gene_ensembl
     * @param inputStream contains TSV data
     * @return a Collection of Biomart attributes
     * @throws IOException when cannot read attributes
     */
    public static Collection<String> parseAttributes(InputStream inputStream) throws IOException {
        return parseValues(inputStream, 0);
    }

    /**
     * Parses dataset names from Biomart, e.g. http://www.ensembl.org/biomart/martservice?type=datasets&mart=ENSEMBL_MART_ENSEMBL
     * @param inputStream contains TSV data
     * @return a Collection of Biomart attributes
     * @throws IOException when cannot read attributes
     */
    public static Collection<String> parseDataSets(InputStream inputStream) throws IOException {
        return parseValues(inputStream, 1);
    }

    private static Collection<String> parseValues(InputStream inputStream, int column) throws IOException {
        Set<String> martAttributes = new HashSet<String>();
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(inputStream), '\t', '"');

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 1 || line[0].contains("Exception")) {
                    throw new IOException("Cannot get attributes");
                }
                if (line.length > column) {
                    martAttributes.add(line[column]);
                }
            }

        } finally {
            closeQuietly(csvReader);
        }

        return martAttributes;
    }
}
