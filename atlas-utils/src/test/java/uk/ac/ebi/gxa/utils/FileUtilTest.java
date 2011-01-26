package uk.ac.ebi.gxa.utils;

import org.junit.Test;

import java.io.FilenameFilter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.gxa.utils.FileUtil.extension;

public class FileUtilTest {
    @Test
    public void testBinaries() {
        final FilenameFilter filter = extension("nc", false);
        assertTrue("Proper filename", filter.accept(null, "test.nc"));
        assertFalse("Unwanted .txt", filter.accept(null, "test.nc.txt"));
        assertFalse("Different extension", filter.accept(null, "test.nc1"));
    }

    @Test
    public void testTexts() {
        final FilenameFilter filter = extension("idf", true);
        assertTrue("Proper filename", filter.accept(null, "test.idf"));
        assertTrue("Allowed .txt", filter.accept(null, "test.idf.txt"));
        assertFalse("Different extension 1", filter.accept(null, "test.idf1"));
        assertFalse("Different extension 2", filter.accept(null, "test.idf1.txt"));
        assertFalse("Different extension 3", filter.accept(null, "test.idf.txt1"));
    }
}
