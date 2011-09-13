package uk.ac.ebi.gxa.annotator.loader.arraydesign;

import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

/**
 * User: nsklyar
 * Date: 02/09/2011
 */
public class ArrayExpressConnectionTest extends TestCase {

    @Test
    public void testFetchArrayDesignData() throws Exception {
        ArrayExpressConnection connection = new ArrayExpressConnection("A-AFFY-2");

        assertEquals("Affymetrix GeneChip Arabidopsis Genome [ATH1-121501]", connection.getName());
        assertEquals("Affymetrix, Inc. (support@affymetrix.com)", connection.getProvider());
        assertEquals("in_situ_oligo_features", connection.getType());

        connection = new ArrayExpressConnection("WRONG ACC");
        assertEquals(StringUtils.EMPTY, connection.getName());
    }
}
