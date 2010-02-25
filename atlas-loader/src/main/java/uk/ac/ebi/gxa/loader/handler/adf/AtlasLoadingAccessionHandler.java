package uk.ac.ebi.gxa.loader.handler.adf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.AccessionHandler;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 22-Feb-2010
 */
public class AtlasLoadingAccessionHandler extends AccessionHandler {
    protected void writeValues() throws ObjectConversionException {
        // now, pull out the bits we need to create experiment objects
        if (arrayDesign.accession != null) {
            ArrayDesignBundle arrayBundle = new ArrayDesignBundle();
            arrayBundle.setAccession(arrayDesign.accession);

            // add the experiment to the cache
            AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
                    .retrieveAtlasLoadCache(arrayDesign);
            cache.addArrayDesignBundle(arrayBundle);
            synchronized (arrayDesign) {
                arrayDesign.notifyAll();
            }
        }
        else {
            // generate error item and throw exception
            String message = "There is no accession number defined - " +
                    "cannot load to the Atlas without an accession, " +
                    "use Comment[ArrayExpressAccession]";

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
