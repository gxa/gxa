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

    // maps of properties  ef -> sorted alphabetically list of efvs
    private Map<String, List<String>> propertyToSortedUniqueEFVs = new LinkedHashMap<String, List<String>>();

    private NetcdfFileWriteable statisticsNetCdf;

    private int totalUniqueEFVs;
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

        for (Map.Entry<String, List<String>> efToEfvs : efvMap.entrySet()) {
            List<String> efvs = new ArrayList<String>(new HashSet<String>(efToEfvs.getValue()));
            Collections.sort(efvs);
            propertyToSortedUniqueEFVs.put(efToEfvs.getKey(), efvs);
            totalUniqueEFVs += propertyToSortedUniqueEFVs.get(efToEfvs.getKey()).size();
        }

        maxNameLength = 0;
        maxValueLength = 0;

        for (Map.Entry<String, List<String>> ef : efvMap.entrySet()) {
            maxNameLength = Math.max(maxNameLength, ef.getKey().length());
            for (String efv : ef.getValue()) {
                maxValueLength = Math.max(maxValueLength, efv.length());
            }
        }

        for (Map.Entry<String, List<String>> sc : scvMap.entrySet()) {
            maxNameLength = Math.max(maxNameLength, sc.getKey().length());
            for (String scv : sc.getValue()) {
                maxValueLength = Math.max(maxValueLength, scv.length());
            }
        }
    }

    private void create(ExperimentWithData ewd) throws IOException, AtlasDataException {
        final Dimension designElementDimension =
                statisticsNetCdf.addDimension("DE", ewd.getDesignElementAccessions(arrayDesign).length);

        if (totalUniqueEFVs != 0) {
            // Now add unique values and stats dimensions
            final Dimension uEFVDimension = statisticsNetCdf
                .addDimension("uEFV", totalUniqueEFVs);
            final Dimension propertyNameLenDimension = statisticsNetCdf
                    .addDimension("propertyNAMElen", maxNameLength);
            final Dimension propertyValueLenDimension = statisticsNetCdf
                    .addDimension("propertyVALUElen", maxValueLength);
            statisticsNetCdf.addVariable(
                    "propertyNAME", DataType.CHAR,
                new Dimension[]{uEFVDimension, propertyNameLenDimension}
            );
            statisticsNetCdf.addVariable(
                    "propertyVALUE", DataType.CHAR,
                new Dimension[]{uEFVDimension, propertyValueLenDimension}
            );
            statisticsNetCdf.addVariable(
                    "PVAL", DataType.FLOAT,
                new Dimension[]{designElementDimension, uEFVDimension}
            );
            statisticsNetCdf.addVariable(
                    "TSTAT", DataType.FLOAT,
                new Dimension[]{designElementDimension, uEFVDimension}
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
    private void writeUEFVs() throws IOException, InvalidRangeException {
        final ArrayChar namesArray = new ArrayChar.D2(totalUniqueEFVs, maxNameLength);
        final ArrayChar valuesArray = new ArrayChar.D2(totalUniqueEFVs, maxValueLength);

        int count = 0;
        for (Map.Entry<String, List<String>> entry : propertyToSortedUniqueEFVs.entrySet()) {
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

            final File tempStatisticsFile = File.createTempFile(statisticsFile.getName(), ".tmp", statisticsFile.getParentFile());
            log.info("Writing NetCDF file to " + tempStatisticsFile);
            statisticsNetCdf = NetcdfFileWriteable.createNew(tempStatisticsFile.getAbsolutePath(), true);
            try {
                create(ewd);
                if (totalUniqueEFVs != 0) {
                    writeUEFVs();
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
}
