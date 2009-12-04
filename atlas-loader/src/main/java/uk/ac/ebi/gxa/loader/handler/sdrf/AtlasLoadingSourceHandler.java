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
        // make sure we wait until IDF has finsihed reading
        AtlasLoaderUtils.waitWhilstSDRFCompiles(investigation, this.getClass().getSimpleName(), getLog());

        if (investigation.accession != null) {
            SDRFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof SourceNode) {
                    Sample sample = new Sample();
                    sample.setAccession(AtlasLoaderUtils.getNodeAccession(investigation, node));

                    // add the sample to the cache
                    AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);
                    cache.addSample(sample);
                    synchronized (investigation) {
                        investigation.notifyAll();
                    }

                    // write the characterstic values as properties
                    SDRFWritingUtils.writeSampleProperties(investigation, sample, (SourceNode) node);

                    // now we've created the sample, wait for donwstream assays and link them

                    // walk down the graph to get to child hyb/assay nodes
                    List<SDRFNode> assayNodes = findDownstreamAssayNode(investigation.SDRF, (SourceNode) node);

                    for (SDRFNode assayNode : assayNodes) {
                        // set up link between sample and assay
                        String assayAccession = AtlasLoaderUtils.getNodeAccession(investigation, assayNode);
                        sample.addAssayAccession(assayAccession);
                    }
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

    private List<SDRFNode> findDownstreamAssayNode(SDRF sdrf, SourceNode node) {
        List<SDRFNode> foundNodes = new ArrayList<SDRFNode>();

        for (String nodeName : node.getChildNodeValues()) {
            SDRFNode nextNode = sdrf.lookupNode(nodeName, node.getChildNodeType());
            walkDownGraph(sdrf, nextNode, foundNodes);
        }

        return foundNodes;
    }

    private void walkDownGraph(SDRF sdrf, SDRFNode node,
                               List<SDRFNode> foundNodes) {
        // check current node
        if (node instanceof AssayNode || node instanceof HybridizationNode) {
            // we want this node
            foundNodes.add(node);
        }
        else {
            // we don't want this, but do walk to children
            for (String nodeName : node.getChildNodeValues()) {
                SDRFNode nextNode = sdrf.lookupNode(nodeName, node.getChildNodeType());
                walkDownGraph(sdrf, nextNode, foundNodes);
            }
        }
    }
}
