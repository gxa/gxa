package uk.ac.ebi.gxa.loader.handler.adf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ADFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ReporterNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.ReporterHandler;
import uk.ac.ebi.gxa.loader.utils.ADFWritingUtils;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 23-Feb-2010
 */
public class AtlasLoadingReporterHandler extends ReporterHandler {
    protected void writeValues() throws ObjectConversionException {
        try {
            // wait at least until we have the accession
            AtlasLoaderUtils.waitForArrayDesignAccession(arrayDesign);

            // get the array design bundle
            ArrayDesignBundle arrayBundle = AtlasLoaderUtils.waitForArrayDesignBundle(
                    arrayDesign.accession, arrayDesign, this.getClass().getSimpleName(), getLog());

            ADFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof ReporterNode) {
                    ReporterNode reporter = (ReporterNode) node;

                    // write a design element matching the details from this node
                    String deName = reporter.getNodeName();
                    getLog().debug("Writing design element from composite element node '" + deName + "'");

                    if (!arrayBundle.getDesignElementNames().contains(reporter.getNodeName())) {
                        // add new design element
                        arrayBundle.addDesignElementName(reporter.getNodeName());
                    }

                    // now write all database entries
                    ADFWritingUtils.writeReporterDatabaseEntries(arrayBundle, deName, reporter);
                }
                else {
                    // generate error item and throw exception
                    String message =
                            "Unexpected node type - ReporterHandler should only make reporter " +
                                    "nodes available for writing, but actually got " + node.getNodeType();
                    ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 999, this.getClass());

                    throw new ObjectConversionException(error, true);
                }
            }
        }
        catch (LookupException e) {
            // generate error item and throw exception
            String message =
                    "There is no accession number defined - cannot load to the Atlas " +
                            "without an accession, use Comment[ArrayExpressAccession]";

            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
