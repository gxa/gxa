package uk.ac.ebi.microarray.atlas.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.DerivedArrayDataMatrixNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler;
import uk.ac.ebi.microarray.atlas.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.microarray.atlas.loader.utils.DataMatrixFileBuffer;
import uk.ac.ebi.microarray.atlas.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A dedicated handler that parses expression data from a specified data matrix
 * file, referenced in the SDRF.  This handler populates expression value
 * objects and attaches them to the assay object that is upstream of the
 * DerivedArrayDataMatrix node that this handler is dealing with.
 *
 * @author Tony Burdett
 * @date 01-Sep-2009
 */
public class AtlasLoadingDerivedArrayDataMatrixHandler
    extends DerivedArrayDataMatrixHandler {
  public void writeValues() throws ObjectConversionException {
    // make sure we wait until IDF has finished reading
    AtlasLoaderUtils.waitWhilstSDRFCompiles(
        investigation, this.getClass().getSimpleName(), getLog());

    if (investigation.accession != null) {
      SDRFNode node;
      while ((node = getNextNodeForCompilation()) != null) {
        if (node instanceof DerivedArrayDataMatrixNode) {
          // sdrf location
          URL sdrfURL = investigation.SDRF.getLocation();

          File sdrfFilePath = new File(sdrfURL.getFile());
          File relPath = new File(sdrfFilePath.getParentFile(),
                                  node.getNodeName());

          // try to get the relative filename
          URL dataMatrixURL = null;
          try {
            // NB. making sure we replace File separators with '/' to guard against windows issues
            dataMatrixURL = sdrfURL.getPort() == -1 ?
                new URL(sdrfURL.getProtocol(),
                        sdrfURL.getHost(),
                        relPath.toString().replaceAll("\\\\", "/")) :
                new URL(sdrfURL.getProtocol(),
                        sdrfURL.getHost(),
                        sdrfURL.getPort(),
                        relPath.toString().replaceAll("\\\\", "/"));

            // simple counts
            int assayCount;
            int evCount = 0;

            // now, obtain a buffer for this dataMatrixFile
            getLog().debug("Opening buffer of data matrix file at " +
                dataMatrixURL);
            DataMatrixFileBuffer buffer =
                DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

            // find all upstream assay nodes
            getLog().debug("Locating upstream assay nodes");
            List<SDRFNode> assayNodes = findUpstreamAssays(
                investigation.SDRF, (DerivedArrayDataMatrixNode) node);

            // fetch the ids
            String[] assayRefs = new String[assayNodes.size()];
            int i = 0;
            for (SDRFNode assayNode : assayNodes) {
              assayRefs[i] = assayNode.getNodeName();
              i++;
            }
            assayCount = assayRefs.length;

            getLog().debug("Got " + assayRefs.length + " assays that " +
                "require expression values");

            // and read out all expression values
//            Map<String, List<ExpressionValue>> evMap =
//                buffer.readAssayExpressionValues(assayRefs);
            Map<String, Map<String, Float>> evMap =
                buffer.readAssayExpressionValues(assayRefs);

            // now fetch each assay and add expression values
            for (SDRFNode assayNode : assayNodes) {
              String assayRef = assayNode.getNodeName();
              String accession = AtlasLoaderUtils.getNodeAccession(
                  investigation, assayNode);

              Map<String, Float> evs = evMap.get(assayRef);
              try {
                getLog().debug("Retrieving assay " + assayRef +
                    " ready for updates...");
                Assay assay = AtlasLoaderUtils.waitForAssay(
                    accession, investigation, this.getClass().getSimpleName(),
                    getLog());
                getLog().debug("Updating assay " + assayRef + " with " +
                    evs.size() + " expression values");
                assay.setExpressionValuesMap(evs);
              }
              catch (LookupException e) {
                // generate error item and throw exception
                String message = "Unable to update assay with expression " +
                    "values - failed whilst attempting to lookup " + assayRef;

                ErrorItem error =
                    ErrorItemFactory
                        .getErrorItemFactory(getClass().getClassLoader())
                        .generateErrorItem(
                            message,
                            1023,
                            this.getClass());

                throw new ObjectConversionException(error, true);
              }
              evCount = evs.size();
            }

            getLog().info("Updated " + assayCount + " assays with " +
                evCount * assayCount + " expression values");
          }
          catch (MalformedURLException e) {
            // generate error item and throw exception
            String message = "Cannot formulate the URL to retrieve the " +
                "DerivedArrayDataMatrix from " + node.getNodeName() + ", " +
                "this file could not be found relative to " + sdrfURL;
            ErrorItem error =
                ErrorItemFactory
                    .getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(
                        message,
                        1023,
                        this.getClass());

            throw new ObjectConversionException(error, true);
          }
          catch (ParseException e) {
            getLog().error("Could not create ExpressionValue items, due to " +
                "failure to read from " + dataMatrixURL);
            throw new ObjectConversionException(e.getErrorItem(), true, e);
          }
        }
        else {
          // generate error item and throw exception
          String message =
              "Unexpected node type - DerivedArrayDataMatrixHandler should only " +
                  "make derived array data matrix nodes available for writing, " +
                  "but actually " + "got " + node.getNodeType();
          ErrorItem error =
              ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                  .generateErrorItem(
                      message,
                      999,
                      this.getClass());

          throw new ObjectConversionException(error, true);
        }
      }
    }
    else {
      // generate error item and throw exception
      String message =
          "There is no accession number defined - cannot load to the Atlas " +
              "without an accession, use Comment[ArrayExpressAccession]";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  501,
                  this.getClass());

      throw new ObjectConversionException(error, true);
    }
  }

  private List<SDRFNode> findUpstreamAssays(SDRF sdrf,
                                            DerivedArrayDataMatrixNode node) {
    List<SDRFNode> foundNodes = new ArrayList<SDRFNode>();

    for (HybridizationNode hybNode :
        investigation.SDRF.lookupNodes(HybridizationNode.class)) {
      // walk downstream, if there is a child matching this node then we want this
      if (hasDataMatrixNodeAsChild(sdrf, hybNode, node)) {
        foundNodes.add(hybNode);
      }
    }

    for (AssayNode assayNode :
        investigation.SDRF.lookupNodes(AssayNode.class)) {
      // walk downstream, if there is a child matching this node then we want this
      if (hasDataMatrixNodeAsChild(sdrf, assayNode, node)) {
        foundNodes.add(assayNode);
      }
    }

    // return all the nodes we found
    return foundNodes;
  }

  private boolean hasDataMatrixNodeAsChild(SDRF sdrf, SDRFNode node,
                                           DerivedArrayDataMatrixNode target) {
    // check current node
    if (node.getChildNodeValues().contains(target.getNodeName())) {
      // does have the target as a child
      return true;
    }
    else {
      // we don't want this node, but now walk to children
      for (String nodeName : node.getChildNodeValues()) {
        SDRFNode nextNode = sdrf.lookupNode(nodeName, node.getChildNodeType());
        // if we found the child here, return true
        if (nextNode != null &&
            hasDataMatrixNodeAsChild(sdrf, nextNode, target)) {
          return true;
        }
      }
    }

    // if we got to here, no children of node match, so return false
    return false;
  }
}
