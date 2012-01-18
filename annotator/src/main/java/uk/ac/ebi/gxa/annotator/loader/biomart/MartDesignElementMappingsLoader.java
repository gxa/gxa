package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.util.CSVBasedReader;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 * @version 1/15/12 3:04 PM
 */
class MartDesignElementMappingsLoader {

    private final Map<String, BioEntityType> name2Type;
    private final MartServiceClient martClient;

    public MartDesignElementMappingsLoader(BioMartAnnotationSource annotSource, MartServiceClient martClient) {
        this.name2Type = annotSource.getExternalName2TypeMap();
        this.martClient = martClient;
    }

    public void load(ExternalArrayDesign externalArrayDesign, DesignElementMappingData.Builder builder)
            throws BioMartException, IOException, InvalidCSVColumnException {
        List<String> columns = new ArrayList<String>();
        columns.addAll(name2Type.keySet());
        columns.add(externalArrayDesign.getName());
        int expectedRowCount = martClient.runCountQuery(columns);
        int actualRowCount = parse(martClient.runQuery(columns), builder);
        if (actualRowCount != expectedRowCount) {
            throw new BioMartException("DesignElement mappings data is not completed: expected_row_count = " +
                    expectedRowCount + ", actual_row_count = " + actualRowCount);
        }
    }

    private int parse(InputStream in, DesignElementMappingData.Builder builder) throws IOException, InvalidCSVColumnException {
        CSVBasedReader reader = null;
        try {
            reader = CSVBasedReader.tsvReader(in);

            int rc = 0;
            CSVBasedReader.Row row;
            while ((row = reader.readNext()) != null) {
                rc ++;
                int col = 0;
                String deAcc = row.getLast();
                for (BioEntityType type : name2Type.values()) {
                    builder.addBEDesignElementMapping(row.get(col++), type, deAcc);
                }
            }
            return rc;
        } finally {
            closeQuietly(reader);
        }
    }
}
