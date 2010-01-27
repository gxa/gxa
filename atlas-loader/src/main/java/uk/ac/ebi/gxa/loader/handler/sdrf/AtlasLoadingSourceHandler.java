package uk.ac.ebi.gxa.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.SourceHandler;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.ArrayList;
import java.util.List;

/**
 * A dedicated handler for creating sample objects and storing them in the cache whenever a new source node is
 * encountered.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingSourceHandler extends SourceHandler {
    protected void writeValues() throws ObjectConversionException {
        if (investigation.accession != null) {
            SDRFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof SourceNode) {
                    getLog().debug("Writing sample from source node '" + node.getNodeName() + "'");

                    AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

                    Sample sample;
                    if (cache.fetchSample(AtlasLoaderUtils.getNodeAccession(investigation, node)) != null) {
                        // get the existing sample
                        sample = cache.fetchSample(AtlasLoaderUtils.getNodeAccession(investigation, node));
                    }
                    else {
                        // create a new sample and add it to the cache
                        sample = new Sample();
                        sample.setAccession(AtlasLoaderUtils.getNodeAccession(investigation, node));
                        cache.addSample(sample);
                        // and notify, as the investigation has updated
                        synchronized (investigation) {
                            investigation.notifyAll();
                        }
                    }

                    // write the characteristic values as properties
                    SDRFWritingUtils.writeSampleProperties(investigation, sample, (SourceNode) node);
                }
                else {
                    // generate error item and throw exception
                    String message =
                            "Unexpected node type - SourceHandler should only make source " +
                                    "nodes available for writing, but actually " +
                                    "got " + node.getNodeType();
                    ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 999, this.getClass());

                    throw new ObjectConversionException(error, true);
                }
            }
        }
        else {
            // generate error item and throw exception
            String message = "There is no accession number defined - cannot load to the Atlas " +
                    "without an accession, use Comment[ArrayExpressAccession]";

            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
