package uk.ac.ebi.gxa.loader.utils;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.CompositeElementNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ReporterNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 23-Feb-2010
 */
public class ADFWritingUtils {
    /**
     * Write out the database entries from an ADF graph, associated with a design element from an {@link
     * ArrayDesignBundle}.  These database entries are obtained by looking at the "composite database entry" columns in
     * the ADF, extracting the type and linking this type to the value of the database entry provided.
     *
     * @param arrayBundle   the array design data being loaded
     * @param designElement the design element you want to attach database entries to
     * @param compositeNode the reporter being read
     * @throws ObjectConversionException if there is a problem creating the database entries
     */
    public static void writeCompositeElementDatabaseEntries(
            ArrayDesignBundle arrayBundle,
            String designElement,
            CompositeElementNode compositeNode)
            throws ObjectConversionException {

    }

    /**
     * Write out the database entries from an ADF graph, associated with a design element from an {@link
     * ArrayDesignBundle}.  These properties are obtained by looking at the "reporter database entry" columns in the
     * ADF, extracting the type and linking this type to the value of the database entry provided.
     *
     * @param arrayBundle   the array design data being loaded
     * @param designElement the design element you want to attach database entries to
     * @param reporterNode  the reporter being read
     * @throws ObjectConversionException if there is a problem creating the database entries
     */
    public static void writeReporterDatabaseEntries(
            ArrayDesignBundle arrayBundle,
            String designElement,
            ReporterNode reporterNode)
            throws ObjectConversionException {

    }
}
