package uk.ac.ebi.gxa.loader.utils;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * User: nsklyar
 * Date: Oct 7, 2010
 */
public class AnnotationsLoader {

//    private static Logger log = LoggerFactory.getLogger(AnnotationsLoader.class);

    public ArrayDesignBundle readAnnotationToADBundle(File file) {

        ArrayDesignBundle bundle = new ArrayDesignBundle();

        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new FileReader(file), '\t', '"');
            String[] headers = csvReader.readNext();

            Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);

            //Start from the 2nd column because the 1st one has transcripts IDs
            for (int i = 1; i < headers.length; i++) {
                dbRefToColumn.put(i, headers[i]);
            }

            //read reference
            String[] line;
            int count = 0;
            while ((line = csvReader.readNext()) != null){
                String de = line[0];
                for (int i = 1; i < line.length; i++) {
                    bundle.addDatabaseEntryForDesignElement(de, dbRefToColumn.get(i), StringUtils.split(line[i], ","));

                }
                if (count++ > 100) break;
            }

            System.out.println("bundle de size= " + bundle.getDesignElementNames().size());
//            for (String deName : bundle.getDesignElementNames()) {
//                System.out.println("deName = " + deName);
//                Map<String, List<String>> dbEntries = bundle.getDatabaseEntriesForDesignElement(deName);
//                System.out.println("dbEntries = " + dbEntries);
//            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            try {
//                log.info("Finished reading from " + file.getName() + ", closing");
                if (csvReader != null) {
                    csvReader.close();
                }
            }
            catch (IOException e) {
               e.printStackTrace();
            }
        }


        return bundle;
    }

    private String[] parseValue(String s) {
        StringTokenizer tokenizer = new StringTokenizer(s, ",");
        List<String> values = new ArrayList<String>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) {
            values.add(tokenizer.nextToken());
        }
        return values.toArray(new String[tokenizer.countTokens()]);
    }


    public static void main(String[] args) {
        AnnotationsLoader loader = new AnnotationsLoader();
        loader.readAnnotationToADBundle(new File("/Users/nsklyar/Data/annotations/human.adf.txt"));
    }
}
