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

package uk.ac.ebi.gxa.annotator.loader.util;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public class CSVBasedReader implements Closeable {
    private final static Logger log = LoggerFactory.getLogger(CSVBasedReader.class);

    private final CSVReader csvReader;
    private final Map<String, Integer> columnIndexes = new HashMap<String, Integer>();
    private int rowCount;

    public CSVBasedReader(InputStream in, char separator, char quotechar) {
        csvReader = new CSVReader(new InputStreamReader(in), separator, quotechar);
    }

    public static CSVBasedReader tsvReader(InputStream in) {
        return new CSVBasedReader(in, '\t', '"');
    }

    public Row readNext() throws IOException, InvalidCSVColumnException {
        String[] line = csvReader.readNext();
        if (rowCount == 0) {
            fillInColumnNames(line);
            line = csvReader.readNext();
        }
        if (rowCount++ % 2000 == 0) {
            log.debug("Parsed [" + rowCount + "] rows");
        }
        if (line == null) {
            return null;
        } else if (line.length < Sets.newHashSet(columnIndexes).size()) {
            return readNext();
        } else {
            return new Row(line);
        }
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
    }

    private void fillInColumnNames(String[] line) throws InvalidCSVColumnException {
        if (line == null || line.length == 0 || Arrays.toString(line).contains("Exception")
                || Arrays.toString(line).contains("ERROR")) {
            throw new InvalidCSVColumnException("There is no data, or error occurred. " +  (line != null?Arrays.toString(line):""));
        }

        for (int i = 0; i < line.length; i++) {
            String cName = line[i];
            columnIndexes.put(cName, i);
            columnIndexes.put(cName.toLowerCase(), i);
        }
    }

    public class Row {
        private final String[] line;

        private Row(String[] line) {
            this.line = line;
        }

        public String get(String colName) throws InvalidCSVColumnException {
            Integer colIndex = columnIndexes.get(colName);
            if (colIndex == null) {
                throw new InvalidCSVColumnException(
                        "Invalid column name: " + colName + "; valid names are " + columnIndexes.keySet());
            }
            return line[colIndex];
        }

        public String get(int colIndex) throws InvalidCSVColumnException {
            if (colIndex < 0 || colIndex >= line.length) {
                throw new InvalidCSVColumnException(
                        "Invalid column index: " + colIndex + "; valid range is [0, " + (line.length - 1) + "]");
            }
            return line[colIndex];
        }

        public String getLast() throws InvalidCSVColumnException {
            return get(line.length - 1);
        }
    }
}
