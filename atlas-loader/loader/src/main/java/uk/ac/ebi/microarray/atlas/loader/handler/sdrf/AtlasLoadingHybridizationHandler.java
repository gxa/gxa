package uk.ac.ebi.microarray.atlas.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.HybridizationHandler;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import uk.ac.ebi.microarray.atlas.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.microarray.atlas.loader.utils.SDRFWritingUtils;

/**
 * A dedicated handler for creating assay objects and storing them in the
 * cache whenever a new hybridization node is encountered.
 *
 * @author Tony Burdett
 * @date 27-Aug-2009
 */
public class AtlasLoadingHybridizationHandler extends HybridizationHandler {
  public void writeValues() throws ObjectConversionException {
    // make sure we wait until IDF has finsihed reading
    AtlasLoaderUtils.waitWhilstSDRFCompiles(
        investigation, this.getClass().getSimpleName(), getLog());

    if (investigation.accession != null) {
      SDRFNode node;
      while ((node = getNextNodeForCompilation()) != null) {
        if (node instanceof HybridizationNode) {
          Assay assay = new Assay();
          assay.setAccession(
              AtlasLoaderUtils.getNodeAccession(investigation, node));
          assay.setExperimentAccession(investigation.accession);

          SDRFWritingUtils.writeProperties(investigation, assay,
                                           (HybridizationNode) node);

          // add the experiment to the cache
          AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
              .retrieveAtlasLoadCache(investigation);
          cache.addAssay(assay);
          synchronized (investigation) {
            investigation.notifyAll();
          }
        }
        else {
          // generate error item and throw exception
          String message =
              "Unexpected node type - HybridizationHandler should only make " +
                  "hyb nodes available for writing, but actually " +
                  "got " + node.getNodeType();
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
