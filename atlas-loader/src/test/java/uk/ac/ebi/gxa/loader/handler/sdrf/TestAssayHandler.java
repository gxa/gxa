package uk.ac.ebi.gxa.loader.handler.sdrf;

import junit.framework.TestCase;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.util.List;

public class TestAssayHandler extends TestCase {
    protected AtlasLoadCache cache;

    protected void checkAssaysInCache() {
        // parsing finished, look in our cache...
        // expect 404 assays
        assertEquals("Local cache doesn't contain correct number of assays",
                404, cache.fetchAllAssays().size());

//        assertEquals("Should have rejected 404 assay to sample links, as samples aren't loaded", 404,
//                     counter.intValue());

        // get the title of the experiment
        for (Assay assay : cache.fetchAllAssays()) {
            String acc = assay.getAccession();
            assertNotNull("Sample acc is null", acc);
        }

        // test properties of each assay
        for (Assay assay : cache.fetchAllAssays()) {
            List<Property> props = assay.getProperties();

            // should have one property, organism
            assertNotNull("Assay " + assay.getAccession() + " properties list is null", props);
            assertEquals("More than one property observed for assay " + assay.getAccession() +
                    ", should be Factor Value[Ecotype] only", 1, props.size());

            assertEquals("Property name is not 'Ecotype'", "Ecotype", props.get(0).getName());

            // test some property values at random
            if (assay.getAccession().equals("E-GEOD-3790::hybridizationname::11 CN A")) {
                assertEquals("Property value should be 'Cape Verde Islands'", "Cape Verde Islands",
                        props.get(0).getValue());
            }

            if (assay.getAccession().equals("E-GEOD-3790::hybridizationname::81 CB A")) {
                assertEquals("Property value should be 'Vancouver-0'", "Vancouver-0", props.get(0).getValue());
            }

            if (assay.getAccession().equals("E-GEOD-3790::hybridizationname::106 FC BA9  A")) {
                assertEquals("Property value should be 'Shahdara'", "Shahdara", props.get(0).getValue());
            }
        }
    }
}
