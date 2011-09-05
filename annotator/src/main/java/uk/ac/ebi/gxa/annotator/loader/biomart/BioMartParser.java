package uk.ac.ebi.gxa.annotator.loader.biomart;

import au.com.bytecode.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityDataBuilder;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 26/08/2011
 */
public class BioMartParser<T extends BioEntityData> {

    final private List<BioEntityType> bioEntityTypes;
    private BioEntityDataBuilder<T> builder;

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    public static  <T extends BioEntityData> BioMartParser initParser(List<BioEntityType> types, BioEntityDataBuilder<T> builder) {
        BioMartParser<T> parser = new BioMartParser<T>(types);
        parser.setBuilder(builder);
        parser.createNewBioEntityData();
        return parser;
    }

    BioMartParser(List<BioEntityType> bioEntityTypes) {
        this.bioEntityTypes = bioEntityTypes;
    }

    public void createNewBioEntityData() {
        builder.createNewData(bioEntityTypes);
    }

    public void parseBioEntities(URL url, Organism organism) throws AtlasAnnotationException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(getReader(url), '\t', '"');
            String[] line;

            while ((line = csvReader.readNext()) != null) {
                if (line.length < bioEntityTypes.size() || line[0].contains("Exception")) {
                    throw new AtlasAnnotationException("Cannot update Bioentities for Organism.Problem when connecting to biomart: " + organism.getName());
                }

                int columnCount = 0;
                for (BioEntityType type : bioEntityTypes) {
                    String beIdentifier = line[columnCount++];
                    String beName = line[columnCount++];
                    builder.addBioEntity(beIdentifier, beName, type, organism);

                    //ToDo: to be decided if we need to keep be2be relations and in which form
                }
            }
        } catch (IOException e) {
            throw new AtlasAnnotationException("Cannot update Bioentities for Organism.Problem when connecting to biomart: " + organism.getName(), e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    public void parseBioMartPropertyValues(BioEntityProperty property, URL url) throws AtlasAnnotationException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(getReader(url), '\t', '"');

            String[] line;
            int lineCount = 0;

            while ((line = csvReader.readNext()) != null) {
                if (line.length < bioEntityTypes.size() + 1 || line[0].contains("Exception")) {
                    log.debug("Cannot get property " + property.getName() + " line: " + line.toString());
                    throw new AtlasAnnotationException("Cannot get property " + property.getName());
                }

                BEPropertyValue propertyValue = new BEPropertyValue(property, line[bioEntityTypes.size()]);
                int count = 0;
                for (BioEntityType type : bioEntityTypes) {
                    builder.addPropertyValue(line[count++], type, propertyValue);
                }

                if (lineCount++ % 1000 == 0) {
                    log.info("Parsed " + lineCount + " properties values");
                }

            }
        } catch (IOException e) {
            throw new AtlasAnnotationException("Cannot get property " + property.getName(), e);
        } finally {
            closeQuietly(csvReader);
        }

    }

    public void parseDesignElementMappings(URL url) throws AtlasAnnotationException {
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(getReader(url), '\t', '"');

            String[] line;
            int lineCount = 0;

            while ((line = csvReader.readNext()) != null) {
                if (line.length < bioEntityTypes.size() + 1 || line[0].contains("Exception")) {
                    throw new AtlasAnnotationException("Cannot update design element mappings");
                }

                String deAcc = line[bioEntityTypes.size()];
                int count = 0;
                for (BioEntityType type : bioEntityTypes) {
                    builder.addBEDesignElementMapping(line[count++], type, deAcc);
                }

                if (lineCount++ % 1000 == 0) {
                    log.info("Parsed " + lineCount + " properties values");
                }

            }
        } catch (IOException e) {
            throw new AtlasAnnotationException("Cannot update design element mappings", e);
        } finally {
            closeQuietly(csvReader);
        }

    }

    void setBuilder(BioEntityDataBuilder builder) {
        this.builder = builder;
    }

    public T getData() throws AtlasAnnotationException {
        return builder.getBioEntityData();
    }

    private Reader getReader(URL url) throws IOException {
        return new InputStreamReader(url.openStream());
    }
}
