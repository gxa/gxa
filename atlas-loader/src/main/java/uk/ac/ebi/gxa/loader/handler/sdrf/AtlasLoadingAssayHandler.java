package uk.ac.ebi.gxa.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ArrayDesignNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.ArrayList;
import java.util.List;

/**
 * A dedicated handler for creating assay objects and storing them in the cache whenever a new assay node is
 * encountered.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingAssayHandler extends AssayHandler {
    public void writeValues() throws ObjectConversionException {
        // make sure we wait until IDF has finsihed reading
        AtlasLoaderUtils.waitWhilstSDRFCompiles(investigation, this.getClass().getSimpleName(), getLog());

        if (investigation.accession != null) {
            SDRFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof AssayNode) {
                    AssayNode assayNode = (AssayNode) node;

                    Assay assay = new Assay();
                    assay.setAccession(AtlasLoaderUtils.getNodeAccession(investigation, assayNode));
                    assay.setExperimentAccession(investigation.accession);


                    // add array design accession
                    List<String> arrayDesignAccessions = new ArrayList<String>();
                    for (ArrayDesignNode arrayDesignNode : assayNode.arrayDesigns) {
                        arrayDesignAccessions.add(arrayDesignNode.getNodeName());
                    }

                    // spec allows multiple array design references, but atlas allows one
                    if (arrayDesignAccessions.size() > 1) {
                        String message = "Assay references more than one array design, " +
                                "this is disallowed";

                        ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                .generateErrorItem(message, 1018, this.getClass());

                        throw new ObjectConversionException(error, true);
                    }
                    else {
                        // only one, so set the accession
                        assay.setArrayDesignAcession(arrayDesignAccessions.get(0));
                    }

                    // now record any properties
                    SDRFWritingUtils.writeAssayProperties(investigation, assay, assayNode);

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
                            "Unexpected node type - AssayHandler should only make assay " +
                                    "nodes available for writing, but actually " +
                                    "got " + node.getNodeType();
                    ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 999, this.getClass());

                    throw new ObjectConversionException(error, true);
                }
            }
        }
        else {
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
