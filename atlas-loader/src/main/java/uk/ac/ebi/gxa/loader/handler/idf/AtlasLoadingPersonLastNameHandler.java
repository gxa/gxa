package uk.ac.ebi.gxa.loader.handler.idf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonLastNameHandler;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * A dedicated handler for attaching a persons name to the appropriate experiment object.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingPersonLastNameHandler extends PersonLastNameHandler {
    protected void writeValues() throws ObjectConversionException {
        // make sure we wait until IDF has finsihed reading
        AtlasLoaderUtils.waitWhilstIDFCompiles(
                investigation, this.getClass().getSimpleName(), getLog());

        // read off name fields
        String firstName = (investigation.IDF.personFirstName.size() > 0)
                ? investigation.IDF.personFirstName.get(0) + " "
                : "";
        String midInitial = (investigation.IDF.personMidInitials.size() > 0)
                ? investigation.IDF.personMidInitials.get(0) + " "
                : "";
        String lastName = (investigation.IDF.personLastName.size() > 0)
                ? investigation.IDF.personLastName.get(0)
                : "";

        String performer = firstName.concat(midInitial).concat(lastName);

        try {
            Experiment expt = AtlasLoaderUtils.waitForExperiment(
                    investigation.accession, investigation,
                    this.getClass().getSimpleName(), getLog());
            expt.setPerformer(performer);
        }
        catch (LookupException e) {
            // generate error item and throw exception
            String message =
                    "Can't lookup experiment, no accession.  Creation will fail";
            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}

