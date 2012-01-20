package uk.ac.ebi.gxa.annotator.loader.biomart;

import com.google.common.base.Joiner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * @author Olga Melnichuk
 * @version 1/19/12 11:37 PM
 */
public class MartServiceClientFactory {

    public static MartServiceClient newMartClient(List<String[]> table) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : table) {
            sb.append(Joiner.on("\t").join(row)).append("\n");
        }
        final String columns = sb.toString();
        final int size = table.size() - 1;

        return new MartServiceClient() {
            @Override
            public InputStream runQuery(Collection<String> attributes) throws BioMartException, IOException {
                return new ByteArrayInputStream(columns.getBytes("UTF-8"));
            }

            @Override
            public int runCountQuery(Collection<String> attributes) throws BioMartException, IOException {
                return size;
            }

            @Override
            public InputStream runAttributesQuery() throws BioMartException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream runDatasetListQuery() throws BioMartException, IOException {
                throw new UnsupportedOperationException();
            }
        };
    }
}
