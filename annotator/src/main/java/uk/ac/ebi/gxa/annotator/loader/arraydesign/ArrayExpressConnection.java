package uk.ac.ebi.gxa.annotator.loader.arraydesign;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

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

    public ArrayDesign fetchArrayDesignData(String accession) {
        log.info("Fetching Array Design data from ArrayExpress " + accession);
        String name = StringUtils.EMPTY;
        String provider = StringUtils.EMPTY;
        String type = StringUtils.EMPTY;

        try {
            URL url = new URL(ADF_URL_TEMPLATE.replace(ACC_TEML, accession));
            CSVReader csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                if (line.length == 2 && line[0].equals(AD_NAME)) {
                    name = line[1];
                }
                if (line.length == 2 && line[0].equals(PROVIDER)) {
                    provider = line[1];
                }
                if (line.length == 2 && line[0].equals(TYPE)) {
                    type = line[1];
                }
                if (count++ > 10) break;
            }
        } catch (IOException e) {
            log.error("Cannot fetch ADF for array design " + accession, e);
        }
        if (StringUtils.isNotEmpty(name)) {
            ArrayDesign arrayArrayDesign = new ArrayDesign(accession);
            arrayArrayDesign.setName(name);
            arrayArrayDesign.setProvider(provider);
            arrayArrayDesign.setType(type);
            return arrayArrayDesign;
        } else
            return null;
    }
}
