package uk.ac.ebi.microarray.atlas.loader.handler.sdrf;

import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.microarray.atlas.loader.model.Sample;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;

/**
 * todo: Javadocs go here!
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingAssayHandler extends AssayHandler {
  public void writeValues() throws ObjectConversionException {
    // make sure we wait until IDF has finsihed reading
    AtlasLoaderUtils.waitWhilstSDRFCompiles(
        investigation, this.getClass().getSimpleName(), getLog());

    if (investigation.accession != null) {
      SDRFNode node;
      while ((node = getNextNodeForCompilation()) != null) {
        Assay assay = new Assay();
        assay.setAccession(
            AtlasLoaderUtils.getNodeAccession(investigation, node));
        assay.setExperimentAccession(investigation.accession);

        // todo - set properties of this assay (attributes of assay node)

        // add the experiment to the cache
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
            .retrieveAtlasLoadCache(investigation);
        cache.addAssay(assay);

        // todo - read data files for expression values
        
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
