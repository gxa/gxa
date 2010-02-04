package uk.ac.ebi.gxa.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.Status;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.DerivedArrayDataMatrixNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.MissingDataFile;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler;
import uk.ac.ebi.arrayexpress2.magetab.utils.ParsingUtils;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        if (investigation.accession != null) {
            SDRFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof DerivedArrayDataMatrixNode) {
                    getLog().info("Writing expression values from data file referenced by " +
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

                        // now, obtain a buffer for this dataMatrixFile
                        getLog().debug("Opening buffer of data matrix file at " + dataMatrixURL);
                        DataMatrixFileBuffer buffer = DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

                        // find the type of nodes we need - lookup from data matrix buffer
                        String refNodeName = buffer.readReferenceColumnName();

                        // fetch the references from the buffer
                        Set<String> refNames = buffer.readReferences();

                        // prefetch the map of all expression values in this file
                        Map<String, Map<String, Float>> expressionValues =
                                buffer.readExpressionValues(refNames.toArray(new String[refNames.size()]));

                        // for each refName, identify the assay the expression values relate to
                        int done = 0;
                        int total = refNames.size();
                        for (String refName : refNames) {
                            getLog().debug("Attempting to attach expression values to next reference " + refName);
                            String assayName;
                            if (refNodeName.equals("scanname")) {
                                // this requires mapping the assay upstream of this node to the scan
                                SDRFNode refNode = null;
                                while (refNode == null) {
                                    try {
                                        refNode = ParsingUtils.waitForSDRFNode(
                                                refName, refNodeName, investigation.SDRF);
                                    }
                                    catch (InterruptedException e) {
                                        if (investigation.SDRF.getStatus() == Status.FAILED) {
                                            // generate error item and throw exception
                                            String message =
                                                    "SDRF graph failed to parse before scans could be read";
                                            ErrorItem error =
                                                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                                            .generateErrorItem(message, 511, this.getClass());

                                            throw new ObjectConversionException(error, true);
                                        }
                                    }
                                }

                                // collect all the possible 'assay' forming nodes
                                Collection<HybridizationNode> hybTypeNodes = SDRFUtils.findUpstreamNodes(
                                        investigation.SDRF, refNode, HybridizationNode.class);
                                Collection<AssayNode> assayTypeNodes = SDRFUtils.findUpstreamNodes(
                                        investigation.SDRF, refNode, AssayNode.class);

                                // lump the two together
                                Collection<SDRFNode> assayNodes = new HashSet<SDRFNode>();
                                assayNodes.addAll(hybTypeNodes);
                                assayNodes.addAll(assayTypeNodes);

                                // now check we have 1:1 mappings so that we can resolve our scans
                                if (assayNodes.size() == 1) {
                                    SDRFNode assayNode = assayNodes.iterator().next();
                                    getLog().debug("Scan node " + refNodeName + " resolves to " +
                                            assayNode.getNodeName());

                                    assayName = assayNode.getNodeName();
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
                            else {
                                assayName = refName;
                            }

                            getLog().trace(
                                    "Updating assay " + assayName + " with expression values, must be stored first...");

                            // try to acquire the assay node from the graph
                            while (true) {
                                try {
                                    if (ParsingUtils.waitForSDRFNode(
                                            assayName, "hybridizationname", investigation.SDRF) == null) {
                                        // try again for assay, but if this throws a lookup exception we really throw it
                                        ParsingUtils.waitForSDRFNode(assayName, "assayname", investigation.SDRF);
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                                catch (InterruptedException e) {
                                    if (investigation.SDRF.getStatus() == Status.FAILED) {
                                        break;
                                    }
                                }
                            }

                            // now we have the name of the assay to attach EVs to, so lookup
                            Assay assay = AtlasLoaderUtils.waitForAssay(
                                    assayName, investigation, getClass().getSimpleName(), getLog());

                            done++;
                            if (assay != null) {
                                // extract twice, cos we're reading only one node at a time
                                assay.setExpressionValuesByDesignElementReference(
                                        buffer.readExpressionValues(refName).get(refName));

                                getLog().debug("Updated assay " + assayName + " with " +
                                        expressionValues.get(refName).size() + " expression values. " +
                                        "Now done " + done + "/" + total + " expression value updates");
                            }
                            else {
                                // generate error item and throw exception
                                String message =
                                        "Data file references elements that are not present in the SDRF";
                                ErrorItem error =
                                        ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                                .generateErrorItem(message, 511, this.getClass());

                                throw new ObjectConversionException(error, true);
                            }
                        }
                    }
                    catch (LookupException e) {
                        // generate error item and throw exception
                        String message =
                                "Data file references elements that are not present in the SDRF";
                        ErrorItem error =
                                ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                        .generateErrorItem(message, 511, this.getClass());

                        throw new ObjectConversionException(error, true);
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
