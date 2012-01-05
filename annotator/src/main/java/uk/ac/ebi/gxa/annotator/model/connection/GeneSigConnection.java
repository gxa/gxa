package uk.ac.ebi.gxa.annotator.model.connection;

import au.com.bytecode.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
public class GeneSigConnection implements AnnotationSourceConnection<GeneSigAnnotationSource> {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    //Example:  http://compbio.dfci.harvard.edu/genesigdb/download/GeneSigDBv4.0_STANDARDIZED_GENELIST.csv
    private final String urlLocation;

    public GeneSigConnection(String url) {
        this.urlLocation = url;
    }

    @Override
    public String getOnlineSoftwareVersion() throws AnnotationSourceAccessException {
        //ToDo: find a way to check for a current GeneSigDb version
        return "4.0";
    }

    @Override
    public Collection<String> validateAttributeNames(Set<String> properties) throws AnnotationSourceAccessException {
        List<String> missingAttrs = new ArrayList<String>();
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(getURL().openStream()), '\t', '"');

            String[] line = csvReader.readNext();
            if (line.length < 1 || line[0].contains("Exception")) {
                throw new IOException("Cannot get attributes from " + urlLocation);
            }

            final List<String> foundProperties = Arrays.asList(line);
            for (String property : properties) {
                if (!foundProperties.contains(property)) {
                    missingAttrs.add(property);
                }
            }

        } catch (IOException e) {
            throw new AnnotationSourceAccessException("Cannot load data from " + urlLocation, e);
        } finally {
            log.info("Finished reading from " + urlLocation + ", closing");
            closeQuietly(csvReader);
        }
        return missingAttrs;
    }

    public URL getURL() throws AnnotationSourceAccessException {
        try {
            return new URI(urlLocation).toURL();
        } catch (MalformedURLException e) {
            throw new AnnotationSourceAccessException("Cannot read data from " + urlLocation, e);
        } catch (URISyntaxException e) {
            throw new AnnotationSourceAccessException("Cannot read data from " + urlLocation, e);
        }
    }
}
