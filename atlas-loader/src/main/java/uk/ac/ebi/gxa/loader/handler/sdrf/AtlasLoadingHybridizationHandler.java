package uk.ac.ebi.gxa.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ArrayDesignNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.HybridizationHandler;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A dedicated handler for creating assay objects and storing them in the cache whenever a new hybridization node is
 * encountered.
 *
 * @author Tony Burdett
 * @date 27-Aug-2009
 */
public class AtlasLoadingHybridizationHandler extends HybridizationHandler {
    public void writeValues() throws ObjectConversionException {
        if (investigation.accession != null) {
            SDRFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof HybridizationNode) {
                    getLog().debug("Writing assay from hybridization node '" + node.getNodeName() + "'");
                    HybridizationNode hybridizationNode = (HybridizationNode) node;

                    // fetch cache
                    AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);
                    if (cache == null) {
                        String message = "Could not acquire cache of objects: most likely, a parallel handler " +
                                "has caused a failure";

                        ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                .generateErrorItem(message, 999, this.getClass());

                        throw new ObjectConversionException(error, true);
                    }

                    // create/retrieve the new assay
                    Assay assay;
                    if (cache.fetchAssay(AtlasLoaderUtils.getNodeAccession(investigation, node)) != null) {
                        // get the existing sample
                        assay = cache.fetchAssay(AtlasLoaderUtils.getNodeAccession(investigation, node));
                        getLog().debug("Integrated assay with existing assay (" + assay.getAccession() + "), " +
                                "count now = " + cache.fetchAllAssays().size());
                    }
                    else {
                        // create a new sample and add it to the cache
                        assay = new Assay();
                        assay.setAccession(AtlasLoaderUtils.getNodeAccession(investigation, hybridizationNode));
                        assay.setExperimentAccession(investigation.accession);
                        cache.addAssay(assay);
                        getLog().debug("Created new assay (" + assay.getAccession() + "), " +
                                "count now = " + cache.fetchAllAssays().size());

                        // and notify, as the investigation has updated
                        synchronized (investigation) {
                            investigation.notifyAll();
                        }
                    }

                    // add array design accession
                    List<String> arrayDesignAccessions = new ArrayList<String>();
                    for (ArrayDesignNode arrayDesignNode : hybridizationNode.arrayDesigns) {
                        arrayDesignAccessions.add(arrayDesignNode.getNodeName());
                    }

                    // spec allows multiple array design references, but atlas allows one
                    if (arrayDesignAccessions.size() > 1) {
                        String message = "Assay references more than one array design, " +
                                "this is disallowed";

                        ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                .generateErrorItem(message, 1018, this.getClass());

                        throw new ObjectConversionException(error, true);
                    }
                    else {
                        // only one, so set the accession
                        if (assay.getArrayDesignAccession() == null) {
                            assay.setArrayDesignAcession(arrayDesignAccessions.get(0));
                        }
                        else if (!assay.getArrayDesignAccession().equals(arrayDesignAccessions.get(0))) {
                            String message = "The same assay in the SDRF references two different array designs";

                            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(message, 1018, this.getClass());

                            throw new ObjectConversionException(error, true);
                        }
                        else {
                            // already set, and equal, so ignore
                        }
                    }

                    // now record any properties
                    SDRFWritingUtils.writeHybridizationProperties(investigation, assay, hybridizationNode);

                    // finally, assays must be linked to their upstream samples
                    Collection<SourceNode> upstreamSources = SDRFWritingUtils.findUpstreamNodes(
                            investigation.SDRF, hybridizationNode, SourceNode.class);

                    for (SourceNode source : upstreamSources) {
                        // retrieve the samples with the matching accession
                        Sample sample = cache.fetchSample(source.getNodeName());
                        if (sample == null) {
                            // no sample to link to in the cache - generate error item and throw exception
                            String message = "Assay " + assay.getAccession() + " is linked to sample " +
                                    source.getNodeName() + " but this sample is not due to be loaded. " +
                                    "This assay will not be linked to a sample";
                            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(message, 511, this.getClass());

                            throw new ObjectConversionException(error, false);
                        }
                        else {
                            if (sample.getAssayAccessions() != null &&
                                    !sample.getAssayAccessions().contains(assay.getAccession())) {
                                sample.addAssayAccession(assay.getAccession());
                            }
                        }
                    }
                }
                else {
                    // generate error item and throw exception
                    String message =
                            "Unexpected node type - AssayHandler should only make assay " +
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
            String message =
                    "There is no accession number defined - cannot load to the Atlas " +
                            "without an accession, use Comment[ArrayExpressAccession]";

            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
