package uk.ac.ebi.microarray.atlas.loader.handler.idf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.utils.AtlasLoaderUtils;

/**
 * A dedicated handler for creating experiment objects and storing them in the
 * cache whenever a new investigation accession is encountered.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingAccessionHandler extends AccessionHandler {
  protected void writeValues() throws ObjectConversionException {
    // make sure we wait until IDF has finsihed reading
    AtlasLoaderUtils.waitWhilstIDFCompiles(
        investigation, this.getClass().getSimpleName(), getLog());

    // now, pull out the bits we need to create experiment objects
    if (investigation.accession != null) {
      Experiment experiment = new Experiment();
      experiment.setAccession(investigation.accession);

      // add the experiment to the cache
      AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
          .retrieveAtlasLoadCache(investigation);
      cache.addExperiment(experiment);
      synchronized (investigation) {
        investigation.notifyAll();
      }
    }
    else {
      // generate error item and throw exception
      String message = "There is no accession number defined - " +
          "cannot load to the Atlas without an accession, " +
          "use Comment[ArrayExpressAccession]";

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
