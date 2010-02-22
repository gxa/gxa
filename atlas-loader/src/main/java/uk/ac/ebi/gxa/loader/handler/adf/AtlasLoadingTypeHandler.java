package uk.ac.ebi.gxa.loader.handler.adf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TechnologyTypeHandler;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 22-Feb-2010
 */
public class AtlasLoadingTypeHandler extends TechnologyTypeHandler {
    protected void writeValues() throws ObjectConversionException {
        try {
            ArrayDesignBundle arrayBundle = AtlasLoaderUtils.waitForArrayDesignBundle(
                    arrayDesign.accession, arrayDesign, this.getClass().getSimpleName(), getLog());
//            arrayBundle.setType(arrayDesign.ADF.technologyType);
        }
        catch (LookupException e) {
            // generate error item and throw exception
            String message =
                    "Can't lookup array desgin bundle, no accession.  Creation will fail";
            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
