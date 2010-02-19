package uk.ac.ebi.gxa.loader.handler.sdrf;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.FactorValueNodeHandler;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 19-Feb-2010
 */
public class AtlasLoadUpdatingFactorValueNodeHandler extends FactorValueNodeHandler {
    protected void writeValues() throws ObjectConversionException {
        // get the cache
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

        // lookup hyb/assay nodes in the graph
        List<AssayNode> assayNodes = investigation.SDRF.lookupNodes(AssayNode.class);
        List<HybridizationNode> hybridizationNodes = investigation.SDRF.lookupNodes(HybridizationNode.class);

        // now, diff assay nodes with the assays in the cache
        for (AssayNode assayNode : assayNodes) {
            Assay assay = cache.fetchAssay(assayNode.getNodeName());

            if (assay != null) {
                if (assay.getProperties() == null) {
                    if (assayNode.factorValues.size() != 0) {
                        // todo - add new properties
                        System.out.println("Factor Values need updating for " + assay.getAccession());
                    }
                }
                else {
                    if (assay.getProperties().size() != assayNode.factorValues.size()) {
                        // todo - add extra properties
                        System.out.println("Factor Values need adding for " + assay.getAccession());
                    }
                }
            }
        }

        // now, diff hyb nodes with the assays in the cache
        for (HybridizationNode hybridizationNode : hybridizationNodes) {
            System.out.println("Looking for assays with name " + hybridizationNode.getNodeName());
            Assay assay = cache.fetchAssay(hybridizationNode.getNodeName());

            if (assay != null) {
                if (assay.getProperties() == null) {
                    if (hybridizationNode.factorValues.size() != 0) {
                        // todo - add new properties
                        System.out.println("Factor Values need updating for " + assay.getAccession());
                    }
                }
                else {
                    if (assay.getProperties().size() != hybridizationNode.factorValues.size()) {
                        // todo - add extra properties
                        System.out.println("Factor Values need adding for " + assay.getAccession());
                    }
                }
            }
        }
    }
}
