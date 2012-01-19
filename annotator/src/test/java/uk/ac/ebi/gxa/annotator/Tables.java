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

    public static Map<String, Collection<String>> convert2map(List<String[]> table) {
        Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
        for (String[] row : table) {
            map.put(row[0], asList(row));
        }
        return map;
    }

}
