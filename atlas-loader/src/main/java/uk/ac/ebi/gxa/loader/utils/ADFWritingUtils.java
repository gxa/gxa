/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.loader.utils;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.CompositeElementNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ReporterNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

import java.util.*;

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
        if (arrayBundle.getDesignElementNames().contains(designElement)) {
            // this design element was already added, so update properties
            for (String databaseEntryType : compositeNode.compositeElementDatabaseEntries.keySet()) {
                if (arrayBundle.getDatabaseEntriesForDesignElement(designElement).containsKey(databaseEntryType)) {
                    // this entry name has already been added - overwrite?
                    // generate error item - duplicates should not be present
                    // no preexisting design element - generate error item and throw exception
                    String message = "Attempting to add duplicate database entries with different values for " +
                            "design element '" + designElement + "', database entry type '" +
                            databaseEntryType + "'";

                    ErrorItem error =
                            ErrorItemFactory.getErrorItemFactory(ADFWritingUtils.class.getClassLoader())
                                    .generateErrorItem(message, ErrorCode.DUPLICATED_FEATURES,
                                                       ADFWritingUtils.class);

                    throw new ObjectConversionException(error, false);
                }
                else {
                    // add new entry
                    List<String> originalValues = new ArrayList<String>();
                    // grab a snapshot of current contents, may still be updating
                    originalValues.addAll(
                            compositeNode.compositeElementDatabaseEntries.get(databaseEntryType));
                    List<String> databaseEntryValues = new ArrayList<String>();

                    for (String originalValue : originalValues) {
                        // split on commas
                        String[] realValues = originalValue.split(",");
                        Collections.addAll(databaseEntryValues, realValues);
                    }

                    arrayBundle.addDatabaseEntryForDesignElement(designElement,
                                                                 databaseEntryType,
                                                                 databaseEntryValues.toArray(
                                                                         new String[originalValues.size()]));
                }
            }
        }
        else {
            // no preexisting design element - generate error item and throw exception
            String message = "Cannot add database entries for design element '" + designElement + "', this design " +
                    "element is not present on this array design data bundle";

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(ADFWritingUtils.class.getClassLoader())
                            .generateErrorItem(message, ErrorCode.NULL_REQUIRED_ADF_TAG, ADFWritingUtils.class);

            throw new ObjectConversionException(error, false);
        }
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
        if (arrayBundle.getDesignElementNames().contains(designElement)) {
            // this design element was already added, so update properties
            for (String databaseEntryType : reporterNode.reporterDatabaseEntries.keySet()) {
                if (arrayBundle.getDatabaseEntriesForDesignElement(designElement).containsKey(databaseEntryType)) {
                    // this entry name has already been added - overwrite?
                    // generate error item - duplicates should not be present
                    // no preexisting design element - generate error item and throw exception
                    String message = "Attempting to add duplicate database entries with different values for " +
                            "design element '" + designElement + "', database entry type '" +
                            databaseEntryType + "'";

                    ErrorItem error =
                            ErrorItemFactory.getErrorItemFactory(ADFWritingUtils.class.getClassLoader())
                                    .generateErrorItem(message, ErrorCode.DUPLICATED_FEATURES,
                                                       ADFWritingUtils.class);

                    throw new ObjectConversionException(error, false);
                }
                else {
                    // add new entry
                    List<String> originalValues =
                            reporterNode.reporterDatabaseEntries.get(databaseEntryType);
                    List<String> databaseEntryValues = new ArrayList<String>();

                    for (String originalValue : originalValues) {
                        // split on commas
                        String[] realValues = originalValue.split(",");
                        Collections.addAll(databaseEntryValues, realValues);
                    }

                    arrayBundle.addDatabaseEntryForDesignElement(designElement,
                                                                 databaseEntryType,
                                                                 databaseEntryValues.toArray(
                                                                         new String[originalValues.size()]));
                }
            }
        }
        else {
            // no preexisting design element - generate error item and throw exception
            String message = "Cannot add database entries for design element '" + designElement + "', this design " +
                    "element is not present on this array design data bundle";

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(ADFWritingUtils.class.getClassLoader())
                            .generateErrorItem(message, ErrorCode.NULL_REQUIRED_ADF_TAG, ADFWritingUtils.class);

            throw new ObjectConversionException(error, false);
        }
    }
}
