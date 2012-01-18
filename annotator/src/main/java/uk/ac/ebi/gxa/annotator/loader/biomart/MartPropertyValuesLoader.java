package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.util.CSVBasedReader;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 * @version 1/15/12 1:10 PM
 */
class MartPropertyValuesLoader {

    private final MartServiceClient martClient;
    private final Map<String, BioEntityType> name2Type;

    public MartPropertyValuesLoader(BioMartAnnotationSource annotSource, MartServiceClient martClient) {
        this.martClient = martClient;
        this.name2Type = annotSource.getExternalName2TypeMap();
    }

    public void load(ExternalBioEntityProperty property, BioEntityAnnotationData.Builder builder)
            throws BioMartException, IOException, InvalidCSVColumnException {
        List<String> columns = new ArrayList<String>();
        columns.addAll(name2Type.keySet());
        columns.add(property.getName());
        int expectedRowCount = martClient.runCountQuery(columns);
        int actualRowCount = parse(martClient.runQuery(columns), property, builder);
        if (actualRowCount != expectedRowCount) {
            throw new BioMartException("BioEntityPropertyValues data is not completed: expected_row_count = " +
                    expectedRowCount + ", actual_row_count = " + actualRowCount);
        }
    }

    private int parse(InputStream in, ExternalBioEntityProperty property, BioEntityAnnotationData.Builder builder)
            throws IOException, InvalidCSVColumnException {
        CSVBasedReader reader = null;
        try {
            reader = CSVBasedReader.tsvReader(in);

            int rc = 0;
            CSVBasedReader.Row row;
            while ((row = reader.readNext()) != null) {
                rc ++;
                int col = 0;
                BEPropertyValue propertyValue = new BEPropertyValue(property.getBioEntityProperty(), row.getLast());
                for (BioEntityType type: name2Type.values()) {
                    builder.addPropertyValue(row.get(col++), type, propertyValue);
                }
            }
            return rc;
        } finally {
            closeQuietly(reader);
        }
    }
}
