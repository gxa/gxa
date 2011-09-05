package uk.ac.ebi.gxa.annotator.loader.arraydesign;

import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

/**
 * User: nsklyar
 * Date: 02/09/2011
 */
public class ArrayExpressConnectionTest  extends TestCase {

    @Test
    public void testFetchArrayDesignData() throws Exception{
        ArrayExpressConnection connection = new ArrayExpressConnection();
        ArrayDesign arrayDesign = connection.fetchArrayDesignData("A-AFFY-2");
        assertNotNull("ArrayDesign should not be null ", arrayDesign);
        assertEquals("Affymetrix GeneChip Arabidopsis Genome [ATH1-121501]", arrayDesign.getName());
        assertEquals("Affymetrix, Inc. (support@affymetrix.com)", arrayDesign.getProvider());
        assertEquals("in_situ_oligo_features", arrayDesign.getType());

        ArrayDesign nullArrayDesign = connection.fetchArrayDesignData("WRONG ACC");
        assertNull(nullArrayDesign);

    }
}
