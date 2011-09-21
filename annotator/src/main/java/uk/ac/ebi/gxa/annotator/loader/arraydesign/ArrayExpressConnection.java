package uk.ac.ebi.gxa.annotator.loader.arraydesign;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 02/09/2011
 */
public class ArrayExpressConnection {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ACC_TEML = "$ACC";
    private static final String ADF_URL_TEMPLATE = "http://www.ebi.ac.uk/arrayexpress/files/" + ACC_TEML + "/" + ACC_TEML + ".adf.txt";

    public static final String AD_NAME = "Array Design Name";
    public static final String PROVIDER = "Provider";
    public static final String TYPE = "Technology Type";

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
        try {
            URL url = new URL(ADF_URL_TEMPLATE.replace(ACC_TEML, accession));
            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                if (line.length == 2) {
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
        } catch (IOException e) {
            log.info("Cannot fetch ADF for array design " + accession, e);
        } finally {
            closeQuietly(csvReader);
        }
    }
}
