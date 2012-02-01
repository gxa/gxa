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

package uk.ac.ebi.gxa.annotator;

import java.util.*;

import static java.util.Arrays.asList;

/**
 * @author Olga Melnichuk
 * @version 1/19/12 11:08 PM
 */
public class Tables {

    public static List<String[]> transpose(List<String[]> inTable) {
        int ncol = inTable.size();
        int nrow = inTable.get(0).length;
        List<String[]> out = new ArrayList<String[]>();
        for (int i = 0; i < nrow; i++) {
            String[] row = new String[ncol];
            out.add(row);
            for (int j = 0; j < ncol; j++) {
                row[j] = inTable.get(j)[i];
            }
        }
        return out;
    }

    public static Map<String, List<String>> convert2map(List<String[]> table) {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (String[] row : table) {
            map.put(row[0], asList(row));
        }
        return map;
    }

}
