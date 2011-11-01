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

package uk.ac.ebi.gxa.data;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

public class NetCDFStatisticsCreator {
    private final Logger log = LoggerFactory.getLogger(NetCDFStatisticsCreator.class);

    private final AtlasDataDAO dataDAO;
    private final Experiment experiment;
    private final ArrayDesign arrayDesign;

    // maps of properties
    private Multimap<String, String> propertyToUnsortedUniqueValues = LinkedHashMultimap.create(); // sc/ef -> unsorted scvs/efvs
    private Map<String, List<String>> propertyToSortedUniqueValues = new LinkedHashMap<String, List<String>>(); // sc/ef -> sorted scs/efvs

    private NetcdfFileWriteable statisticsNetCdf;

    private int totalUniqueValues; // scvs/efvs
    private int maxNameLength;
    private int maxValueLength;

    NetCDFStatisticsCreator(AtlasDataDAO dataDAO, Experiment experiment, ArrayDesign arrayDesign) {
        this.dataDAO = dataDAO;
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
    }

    private LinkedHashMap<String, List<String>> extractAssayProperties() {
        final LinkedHashMap<String, List<String>> result = new LinkedHashMap<String, List<String>>();

        final SortedSet<String> propertyNames = new TreeSet<String>();

        final Collection<Assay> assays = experiment.getAssaysForDesign(arrayDesign);
        for (Assay a : assays) {
            propertyNames.addAll(a.getPropertyNames());
        }

        for (String propertyName : propertyNames) {
            final List<String> propertyList = new ArrayList<String>(assays.size());

            for (final Assay a : assays) {
                propertyList.add(a.getPropertySummary(propertyName));
            }

            result.put(propertyName, propertyList);
        }

        return result;
    }

    private LinkedHashMap<String, List<String>> extractSampleProperties() {
        final LinkedHashMap<String, List<String>> result = new LinkedHashMap<String, List<String>>();

        final LinkedHashSet<Sample> samples = new LinkedHashSet<Sample>();
        final SortedSet<String> propertyNames = new TreeSet<String>();

        for (Assay a : experiment.getAssaysForDesign(arrayDesign)) {
            for (Sample s : a.getSamples()) {
                samples.add(s);
                propertyNames.addAll(s.getPropertyNames());
            }
        }

        for (final String propertyName : propertyNames) {
            final List<String> propertyList = new ArrayList<String>(samples.size());

            for (final Sample s : samples) {
                propertyList.add(s.getPropertySummary(propertyName));
            }

            result.put(propertyName, propertyList);
        }

        return result;
    }

    void prepareData() {
        final LinkedHashMap<String, List<String>> efvMap = extractAssayProperties();
        final LinkedHashMap<String, List<String>> scvMap = extractSampleProperties();

        // Merge efvMap and scvMap into propertyToUnsortedUniqueValues that will store all scv/efv properties
        for (Map.Entry<String, List<String>> efToEfvs : efvMap.entrySet()) {
            propertyToUnsortedUniqueValues.putAll(efToEfvs.getKey(), efToEfvs.getValue());
        }
        for (Map.Entry<String, List<String>> scToScvs : scvMap.entrySet()) {
            propertyToUnsortedUniqueValues.putAll(scToScvs.getKey(), scToScvs.getValue());
        }

        // find maximum lengths for ef/efv/sc/scv strings
        totalUniqueValues = populateUniqueValues(propertyToUnsortedUniqueValues, propertyToSortedUniqueValues);

        maxNameLength = 0;
        maxValueLength = 0;

        for (String ef : efvMap.keySet()) {
            maxNameLength = Math.max(maxNameLength, ef.length());
            for (String efv : efvMap.get(ef)) {
                maxValueLength = Math.max(maxValueLength, efv.length());
            }
        }

        for (String sc : scvMap.keySet()) {
            maxNameLength = Math.max(maxNameLength, sc.length());
            for (String scv : scvMap.get(sc)) {
                maxValueLength = Math.max(maxValueLength, scv.length());
            }
        }
    }

    /**
     * @param unsortedUniqueValueMap source of non-unique data from which sortedUniqueValueMap is populated;
     *                               ef or sc -> list of non-unique efvs/scvs corresponding to ef/sc key respectively
     * @param sortedUniqueValueMap   populated by this method; ef or sc -> list of unique efvs/scvs corresponding to ef/sc key respectively
     * @return total number of unique values in uniqueValueMap
     */
    private int populateUniqueValues(
            final Multimap<String, String> unsortedUniqueValueMap,
            final Map<String, List<String>> sortedUniqueValueMap
    ) {
        int totalUniqueValues = 0;
        for (final Map.Entry<String, Collection<String>> efOrSc : unsortedUniqueValueMap.asMap().entrySet()) {
            final List<String> efvsOrScvs = new ArrayList<String>(new HashSet<String>(efOrSc.getValue()));
            Collections.sort(efvsOrScvs);
            sortedUniqueValueMap.put(efOrSc.getKey(), efvsOrScvs);
            totalUniqueValues += sortedUniqueValueMap.get(efOrSc.getKey()).size();

        }
        return totalUniqueValues;
    }

    private void create(ExperimentWithData ewd) throws IOException, AtlasDataException {
        final Dimension designElementDimension =
            statisticsNetCdf.addDimension("DE", ewd.getDesignElementAccessions(arrayDesign).length);

        if (totalUniqueValues != 0) {
            // Now add unique values and stats dimensions
            final Dimension uvalDimension = statisticsNetCdf
                .addDimension("uVAL", totalUniqueValues);
            final Dimension propertyNameLenDimension = statisticsNetCdf
                .addDimension("propertyNAMElen", maxNameLength);
            final Dimension propertyValueLenDimension = statisticsNetCdf
                .addDimension("propertyVALUElen", maxValueLength);
            statisticsNetCdf.addVariable(
                "propertyNAME", DataType.CHAR,
                new Dimension[]{uvalDimension, propertyNameLenDimension}
            );
            statisticsNetCdf.addVariable(
                "propertyVALUE", DataType.CHAR,
                new Dimension[]{uvalDimension, propertyValueLenDimension}
            );
            statisticsNetCdf.addVariable(
                "PVAL", DataType.FLOAT,
                new Dimension[]{designElementDimension, uvalDimension}
            );
            statisticsNetCdf.addVariable(
                "TSTAT", DataType.FLOAT,
                new Dimension[]{designElementDimension, uvalDimension}
            );

            final String[] sortOrders = new String[]{"ANY", "UP_DOWN", "UP", "DOWN", "NON_D_E"};
            for (String orderName : sortOrders) {
                statisticsNetCdf.addVariable(
                    "ORDER_" + orderName, DataType.INT,
                    new Dimension[]{designElementDimension}
                );
            }
        }

        // add metadata global attributes
        safeAddGlobalAttribute(
                "CreateNetCDF_VERSION",
                "2.0-statistics");
        safeAddGlobalAttribute(
                "experiment_accession",
                experiment.getAccession());
        safeAddGlobalAttribute(
                "ADaccession",
                arrayDesign.getAccession());

        statisticsNetCdf.create();
    }

    /**
     * Write out unique ef-efvs/sc-scvs
     *
     * @throws IOException
     * @throws InvalidRangeException
     */
    private void writeUVals() throws IOException, InvalidRangeException {
        final ArrayChar namesArray = new ArrayChar.D2(totalUniqueValues, maxNameLength);
        final ArrayChar valuesArray = new ArrayChar.D2(totalUniqueValues, maxValueLength);

        int count = 0;
        for (Map.Entry<String, List<String>> entry : propertyToSortedUniqueValues.entrySet()) {
            List<String> values = entry.getValue();
            for (String value : values) {
                namesArray.setString(count, entry.getKey());
                valuesArray.setString(count, value);
                ++count;
            }
        }
        statisticsNetCdf.write("propertyNAME", namesArray);
        statisticsNetCdf.write("propertyVALUE", valuesArray);
    }

    public void createNetCdf() throws AtlasDataException {
        final ExperimentWithData ewd = dataDAO.createExperimentWithData(experiment);
        try {
            prepareData();

            final File statisticsFile = dataDAO.getStatisticsFile(experiment, arrayDesign);
            if (!statisticsFile.getParentFile().exists() && !statisticsFile.getParentFile().mkdirs()) {
                throw new AtlasDataException("Cannot create folder for the output file" + statisticsFile);
            }

            final File tempStatisticsFile = File.createTempFile(statisticsFile.getName(), ".tmp");
            log.info("Writing NetCDF file to " + tempStatisticsFile);
            statisticsNetCdf = NetcdfFileWriteable.createNew(tempStatisticsFile.getAbsolutePath(), true);
            try {
                create(ewd);
                if (totalUniqueValues != 0) {
                    writeUVals();
                }
            } catch (InvalidRangeException e) {
                throw new AtlasDataException(e);
            } finally {
                statisticsNetCdf.close();
            }
            log.info("Renaming " + tempStatisticsFile + " to " + statisticsFile);
            if (!tempStatisticsFile.renameTo(statisticsFile)) {
                throw new AtlasDataException("Can't rename " + tempStatisticsFile + " to " + statisticsFile);
            }
        } catch (IOException e) {
            throw new AtlasDataException(e);
        } finally {
            closeQuietly(ewd);
        }
    }

    private void safeAddGlobalAttribute(String attribute, String value) {
        if (attribute != null && value != null) {
            statisticsNetCdf.addGlobalAttribute(attribute, value);
        }
    }

    /**
     * @param efs
     * @param scs
     * @return merged LinkedHashSet of efs and scs keySets
     */
    private LinkedHashSet<String> getEfScs(LinkedHashMap<String, List<String>> efs,
                                           LinkedHashMap<String, List<String>> scs) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        result.addAll(efs.keySet());
        result.addAll(scs.keySet());
        return result;
    }
}
