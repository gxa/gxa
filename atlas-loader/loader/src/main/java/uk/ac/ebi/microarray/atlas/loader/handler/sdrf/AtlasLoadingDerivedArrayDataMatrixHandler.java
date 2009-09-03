package uk.ac.ebi.microarray.atlas.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.DerivedArrayDataMatrixNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler;
import uk.ac.ebi.microarray.atlas.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.microarray.atlas.loader.utils.DataMatrixFileBuffer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * todo: Javadocs go here!
 *
 * @author Tony Burdett
 * @date 01-Sep-2009
 */
public class AtlasLoadingDerivedArrayDataMatrixHandler
    extends DerivedArrayDataMatrixHandler {
  public void writeValues() throws ObjectConversionException {
    // make sure we wait until IDF has finsihed reading
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
          try {
            // NB. making sure we replace File separators with '/' to guard against windows issues
            URL dataMatrixURL = sdrfURL.getPort() == -1 ?
                new URL(sdrfURL.getProtocol(),
                        sdrfURL.getHost(),
                        relPath.toString().replaceAll("\\\\", "/")) :
                new URL(sdrfURL.getProtocol(),
                        sdrfURL.getHost(),
                        sdrfURL.getPort(),
                        relPath.toString().replaceAll("\\\\", "/"));

            // now, obtain a buffer for this dataMatrixFile
            DataMatrixFileBuffer buffer =
                DataMatrixFileBuffer.getDataMatrixFileBuffer(dataMatrixURL);

            // do a lookup for the assay this data matrix is associated with

            // and read out expression values
//            buffer.readAssayExpressionValues(assayRef);
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

            throw new ObjectConversionException(error);
          }
//          catch (ParseException e) {
//            getLog().error("DataMatrixFileBuffer cannot be initialised");
//          }
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

          throw new ObjectConversionException(error);
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

      throw new ObjectConversionException(error);
    }
  }
}
