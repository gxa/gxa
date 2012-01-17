package uk.ac.ebi.gxa.annotator.loader.util;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.io.Closeables.closeQuietly;
import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 * @version 1/17/12 1:59 PM
 */
public class CSVBasedReaderTest {

    @Test
    public void testEmptyFile() throws IOException {
        File emptyFile = File.createTempFile("empty-file-test", ".txt");
        CSVBasedReader reader = null;
        try {
            reader = CSVBasedReader.tsvReader(new FileInputStream(emptyFile));
            CSVBasedReader.Row row = reader.readNext();
            assertNull(row);
        } finally {
            closeQuietly(reader);
        }
    }

    @Test
    public void testTSVFormat() throws IOException, InvalidCSVColumnException {
        CSVBasedReader reader = null;
        try {
            reader = CSVBasedReader.tsvReader(getClass().getResourceAsStream("csv-reader-test.tsv"));
            CSVBasedReader.Row row;
            int rc = 0;
            while ((row = reader.readNext()) != null) {
                rc++;
                String v1 = row.get(0);
                String v2 = row.get("ENSEMBL_GENE_ID");
                String v3 = row.get("ensembl_gene_id");
                assertEquals(v1, v2);
                assertEquals(v1, v3);
            }
            assertEquals(25, rc);
        } finally {
            closeQuietly(reader);
        }
    }

    @Test
    public void testCSVFormat() throws IOException, InvalidCSVColumnException {
        CSVBasedReader reader = null;
        try {
            reader = new CSVBasedReader(getClass().getResourceAsStream("csv-reader-test.csv"), ',', '"');
            CSVBasedReader.Row row = reader.readNext();
            assertNotNull(row);
            String v1 = row.get(0);
            String v2 = row.get("ENSEMBL_GENE_ID");
            String v3 = row.get("ensembl_gene_id");
            assertEquals("ENSMUSG00000015355", v1);
            assertEquals("ENSMUSG00000015355", v2);
            assertEquals("ENSMUSG00000015355", v3);
        } finally {
            closeQuietly(reader);
        }
    }

    @Test
    public void testInvalidColumnException() throws IOException {
        CSVBasedReader reader = null;
        try {
            reader = CSVBasedReader.tsvReader(getClass().getResourceAsStream("csv-reader-test.tsv"));
            CSVBasedReader.Row row = reader.readNext();
            assertNotNull(row);
            try {
                row.get("FakeColumnName");
                fail();
            } catch (InvalidCSVColumnException e) {
                // ok
            }

            try {
                row.get(3);
                fail();
            } catch (InvalidCSVColumnException e) {
                // ok
            }
        } finally {
            closeQuietly(reader);
        }
    }
}
