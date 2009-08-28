package uk.ac.ebi.microarray.atlas.loader.handler.idf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.InvestigationTitleHandler;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.microarray.atlas.loader.utils.LookupException;

/**
 * todo: Javadocs go here!
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingInvestigationTitleHandler
    extends InvestigationTitleHandler {
  protected void writeValues() throws ObjectConversionException {
    // make sure we wait until IDF has finsihed reading
    AtlasLoaderUtils.waitWhilstIDFCompiles(
        investigation, this.getClass().getSimpleName(), getLog());

    try {
      Experiment expt = AtlasLoaderUtils.waitForExperiment(
          investigation.accession, investigation,
          this.getClass().getSimpleName(), getLog());
      expt.setDescription(investigation.IDF.investigationTitle);
    }
    catch (LookupException e) {
      // generate error item and throw exception
      String message =
          "Can't lookup experiment, no accession.  Creation will fail";
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
