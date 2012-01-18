package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.util.CSVBasedReader;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 * @version 1/14/12 7:29 PM
 */
class MartBioEntitiesLoader {
    private final BioMartAnnotationSource annotSource;
    private final MartServiceClient martClient;
    private final Map<String, BioEntityType> name2Type;

    public MartBioEntitiesLoader(BioMartAnnotationSource annotSource, MartServiceClient martClient) {
        this.annotSource = annotSource;
        this.martClient = martClient;
        this.name2Type = annotSource.getExternalName2TypeMap();
    }

    public void load(BioEntityData.Builder builder) throws BioMartException, IOException, InvalidCSVColumnException {
        Set<String> columns = name2Type.keySet();
        int expectedRowCount = martClient.runCountQuery(columns);
        int actualRowCount = parse(martClient.runQuery(columns), builder);
        if (actualRowCount != expectedRowCount) {
            throw new BioMartException("BioEntities data is not completed: expected_row_count = " + expectedRowCount +
                    ", actual_row_count = " + actualRowCount);
        }
    }

    private int parse(InputStream in, BioEntityData.Builder dataBuilder) throws IOException, InvalidCSVColumnException {
        CSVBasedReader reader = null;

        try {
            reader = CSVBasedReader.tsvReader(in);

            int rc = 0;
            CSVBasedReader.Row row;
            while ((row = reader.readNext()) != null) {
                rc ++;
                int col = 0;
                for (BioEntityType beType : name2Type.values()) {
                    dataBuilder.addBioEntity(new BioEntity(row.get(col++), beType, annotSource.getOrganism()));
                }
            }
            return rc;
        } finally {
            closeQuietly(reader);
        }
    }

}
