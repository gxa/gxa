package uk.ac.ebi.gxa.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.*;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.MissingDataFile;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A dedicated handler that parses expression data from a specified data matrix file, referenced in the SDRF.  This
 * handler populates expression value objects and attaches them to the assay object that is upstream of the
 * DerivedArrayDataMatrix node that this handler is dealing with.
 *
 * @author Tony Burdett
 * @date 01-Sep-2009
 */
public class AtlasLoadingDerivedArrayDataMatrixHandler extends DerivedArrayDataMatrixHandler {
    public void writeValues() throws ObjectConversionException {
        // make sure we wait until IDF has finished reading
        AtlasLoaderUtils.waitWhilstSDRFCompiles(investigation, this.getClass().getSimpleName(), getLog());

        if (investigation.accession != null) {
            SDRFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof DerivedArrayDataMatrixNode) {
                    getLog().debug("Writing expression values from data file referenced by " +
                            "derived array data matrix node '" + node.getNodeName() + "'");

                    if (node.getNodeName().equals(MissingDataFile.DERIVED_ARRAY_DATA_MATRIX_FILE)) {
                        // this data matrix is missing, no expression values present - so simply continue to next
                        continue;
                    }

                    // sdrf location
                    URL sdrfURL = investigation.SDRF.getLocation();

                    File sdrfFilePath = new File(sdrfURL.getFile());
                    File relPath = new File(sdrfFilePath.getParentFile(), node.getNodeName());

                    // try to get the relative filename
                    URL dataMatrixURL = null;
                    try {
                        // NB. making sure we replace File separators with '/' to guard against windows issues
                        dataMatrixURL = sdrfURL.getPort() == -1
                                ? new URL(sdrfURL.getProtocol(),
                                          sdrfURL.getHost(),
                                          relPath.toString().replaceAll("\\\\", "/"))
                                : new URL(sdrfURL.getProtocol(),
                                          sdrfURL.getHost(),
                                          sdrfURL.getPort(),
                                          relPath.toString().replaceAll("\\\\", "/"));

                        // simple counts
                        int refNodeCount;
                        int evCount = 0;

                        // now, obtain a buffer for this dataMatrixFile
                        getLog().debug("Opening buffer of data matrix file at " + dataMatrixURL);
                        DataMatrixFileBuffer buffer = DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

                        // find the type of nodes we need - lookup from data matrix buffer
                        String refNodeName = buffer.readReferenceColumnName();

                        // grab the class associated with the refNodeName type
                        Class<? extends SDRFNode> refNodeType =
                                investigation.SDRF.lookupNodes(refNodeName).iterator().next().getClass();

                        // first, find all upstream nodes of the refNodeType
                        getLog().debug("Locating upstream nodes of type: " + refNodeType.getSimpleName());

                        // look for ref nodes
                        Collection<? extends SDRFNode> referenceNodes = SDRFWritingUtils.findUpstreamNodes(
                                investigation.SDRF, node, refNodeType);

                        // if our ref nodes are Scan nodes, we need to map to the first assay node directly upstream of it
                        boolean refsAreScans;
                        Map<SDRFNode, SDRFNode> scanToAssayMapping = new HashMap<SDRFNode, SDRFNode>();
                        if (refNodeType == ScanNode.class) {
                            getLog().debug(
                                    "Data matrix file references Scan nodes - resolving scans to associated assays");
                            refsAreScans = true;

                            // and map each scan
                            for (SDRFNode referenceNode : referenceNodes) {
                                // collect all the possible 'assay' forming nodes
                                Collection<HybridizationNode> hybTypeNodes = SDRFWritingUtils.findUpstreamNodes(
                                        investigation.SDRF, referenceNode, HybridizationNode.class);
                                Collection<AssayNode> assayTypeNodes = SDRFWritingUtils.findUpstreamNodes(
                                        investigation.SDRF, referenceNode, AssayNode.class);

                                // lump the two together
                                Collection<SDRFNode> assayNodes = new HashSet<SDRFNode>();
                                assayNodes.addAll(hybTypeNodes);
                                assayNodes.addAll(assayTypeNodes);

                                // now check we have 1:1 mappings so that we can resolve our scans
                                if (assayNodes.size() == 1) {
                                    SDRFNode assayNode = assayNodes.iterator().next();
                                    getLog().debug("Scan node " + referenceNode.getNodeName() + " resolves to " +
                                            assayNode.getNodeName());
                                    scanToAssayMapping.put(referenceNode, assayNode);
                                }
                                else {
                                    // many to one scan-to-assay, we can't load this
                                    // generate error item and throw exception
                                    String message = "Unable to update resolve expression values to assays for " +
                                            investigation.accession + " - data matrix file references scans, " +
                                            "and in this experiment scans do not map one to one with assays.  " +
                                            "This is not supported, as it would result in " +
                                            (assayNodes.size() == 0 ? "zero" : "multiple") + " expression " +
                                            "values per assay.";

                                    ErrorItem error =
                                            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                                    .generateErrorItem(message, 1023, this.getClass());

                                    throw new ObjectConversionException(error, true);
                                }
                            }
                        }
                        else {
                            refsAreScans = false;
                        }

                        // fetch the ids
                        String[] refNodes = new String[referenceNodes.size()];
                        int i = 0;
                        for (SDRFNode refNode : referenceNodes) {
                            refNodes[i] = refNode.getNodeName();
                            i++;
                        }
                        refNodeCount = refNodes.length;

                        getLog().debug("Got " + refNodes.length + " assays that require expression values");

                        // and read out all expression values
                        Map<String, Map<String, Float>> evMap = buffer.readAssayExpressionValues(refNodes);

                        // now fetch each assay and add expression values
                        for (SDRFNode referenceNode : referenceNodes) {
                            // reference node name (might be a scan)
                            getLog().debug("Reference node " + referenceNode.getNodeName() + " is a scan - " +
                                    "resolving to assay before setting expression values");
                            SDRFNode assayNode = refsAreScans
                                    ? scanToAssayMapping.get(referenceNode)
                                    : referenceNode;

                            String accession = AtlasLoaderUtils.getNodeAccession(investigation, assayNode);
                            String assayRef = assayNode.getNodeName();

                            Map<String, Float> evs = evMap.get(referenceNode.getNodeName());

                            // now we have then next expression value - if refsAreScans, map to the right assay, else just set
                            try {
                                Assay assay;
                                getLog().debug("Retrieving assay " + assayRef + ", ready to update expression values");
                                assay = AtlasLoaderUtils.waitForAssay(accession,
                                                                      investigation,
                                                                      this.getClass().getSimpleName(),
                                                                      getLog());
                                getLog().debug("Updating assay " + assayRef + " with " + evs.size() +
                                        " expression values");
                                assay.setExpressionValuesByAccession(evs);
                            }
                            catch (LookupException e) {
                                // generate error item and throw exception
                                String message = "Unable to update assay with expression values - " +
                                        "failed whilst attempting to lookup " + assayRef;

                                ErrorItem error =
                                        ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                                .generateErrorItem(message, 1032, this.getClass());

                                throw new ObjectConversionException(error, true, e);
                            }
                            evCount = evs.size();
                        }

                        getLog().info("Updated " + refNodeCount + " assays with " + evCount * refNodeCount +
                                " expression values");
                    }
                    catch (MalformedURLException e) {
                        // generate error item and throw exception
                        String message = "Cannot formulate the URL to retrieve the " +
                                "DerivedArrayDataMatrix from " + node.getNodeName() + ", " +
                                "this file could not be found relative to " + sdrfURL;
                        ErrorItem error =
                                ErrorItemFactory
                                        .getErrorItemFactory(getClass().getClassLoader())
                                        .generateErrorItem(message, 1023, this.getClass());

                        throw new ObjectConversionException(error, true);
                    }
                    catch (ParseException e) {
                        getLog().error(
                                "Could not create ExpressionValue items, due to failure to read from " + dataMatrixURL);
                        throw new ObjectConversionException(e.getErrorItem(), true, e);
                    }
                }
                else {
                    // generate error item and throw exception
                    String message = "Unexpected node type - DerivedArrayDataMatrixHandler should only " +
                            "make derived array data matrix nodes available for writing, " +
                            "but actually " + "got " + node.getNodeType();
                    ErrorItem error =
                            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(message, 999, this.getClass());

                    throw new ObjectConversionException(error, true);
                }
            }
        }
        else {
            // generate error item and throw exception
            String message = "There is no accession number defined - cannot load to the Atlas " +
                    "without an accession, use Comment[ArrayExpressAccession]";

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
