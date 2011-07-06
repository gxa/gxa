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

package uk.ac.ebi.gxa.netcdf;

import java.util.*;

import java.io.IOException;

import ucar.nc2.*;
import ucar.ma2.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Migrator {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private void splitNetCDFFile(String originalFilePath, String dataFilePath, String statisticsFilePath) throws AtlasDataException {
        log.info("Splitting " + originalFilePath + " into " + dataFilePath + " and " + statisticsFilePath);
        final Set<String> dataDimensions = new TreeSet<String>();
        final Set<String> statsDimensions = new TreeSet<String>();

        dataDimensions.add("AS");
        dataDimensions.add("ASlen");
        dataDimensions.add("BS");
        dataDimensions.add("BSlen");
        dataDimensions.add("DE");
        dataDimensions.add("DElen");
        dataDimensions.add("GN");
        dataDimensions.add("EF");
        dataDimensions.add("EFlen");
        dataDimensions.add("EFVlen");
        dataDimensions.add("SC");
        dataDimensions.add("SClen");
        dataDimensions.add("SCVlen");

        statsDimensions.add("DE");
        statsDimensions.add("uVAL");

        final Set<String> dataVariables = new TreeSet<String>();
        final Set<String> statsVariables = new TreeSet<String>();
        final Set<String> dropVariables = new TreeSet<String>();

        dataVariables.add("ASacc");
        dataVariables.add("BDC");
        dataVariables.add("BS2AS");
        dataVariables.add("BSacc");
        dataVariables.add("DEacc");
        dataVariables.add("GN");
        dataVariables.add("EF");
        dataVariables.add("EFV");
        dataVariables.add("SC");
        dataVariables.add("SCV");

        statsVariables.add("ORDER_ANY");
        statsVariables.add("ORDER_DOWN");
        statsVariables.add("ORDER_NON_D_E");
        statsVariables.add("ORDER_UP");
        statsVariables.add("ORDER_UP_DOWN");
        statsVariables.add("PVAL");
        statsVariables.add("TSTAT");
        //statsVariables.add("uVAL");

        dropVariables.add("EFSC");
        dropVariables.add("EFVO");
        dropVariables.add("SCVO");
        dropVariables.add("uVALnum");

        NetcdfFile infile = null;
        NetcdfFileWriteable outfileData = null;
        NetcdfFileWriteable outfileStats = null;
        try {
            infile = NetcdfFile.open(originalFilePath);
            outfileData = NetcdfFileWriteable.createNew(dataFilePath, false);
            outfileStats = NetcdfFileWriteable.createNew(statisticsFilePath, false);
            final List<Attribute> attributes = infile.getGlobalAttributes();
            log.info("Writing attributes");
            for (Attribute a : attributes) {
                outfileData.addGlobalAttribute(a.getName(), a.getValues());
                outfileStats.addGlobalAttribute(a.getName(), a.getValues());
            }
            final List<Dimension> dimensions = infile.getDimensions();
            log.info("Writing dimensions");
            for (Dimension d : dimensions) {
                final String name = d.getName();
                if (dataDimensions.contains(name)) {
                    outfileData.addDimension(name, d.getLength());
                }
                if (statsDimensions.contains(name)) {
                    outfileStats.addDimension(name, d.getLength());
                }
            }

            final List<String> pNames = new ArrayList<String>();
            final List<String> pValues = new ArrayList<String>();
            final Variable uVALVariable = infile.findVariable("uVAL");
            Dimension namesDimension = null;
            Dimension valuesDimension = null;
            if (uVALVariable != null) {
                final ArrayChar values = (ArrayChar)uVALVariable.read();
                for (Object text : (Object[])values.make1DStringArray().get1DJavaArray(String.class)) {
                    final String[] data = ((String)text).split("\\|\\|");
                    if (data.length != 2) {
                        throw new AtlasDataException("Invalid uVAL element: " + text);
                    }
                    pNames.add(data[0]);
                    pValues.add(data[1]);
                }
                namesDimension = outfileStats.addDimension("propertyNAMElen", maxLength(pNames));
                valuesDimension = outfileStats.addDimension("propertyVALUElen", maxLength(pValues));
            }

            log.info("Writing variables");
            final List<Variable> variables = infile.getVariables();
            for (Variable v : variables) {
                final String name = v.getName();
                if (dataVariables.contains(name)) {
                    outfileData.addVariable(name, v.getDataType(), v.getDimensions());
                } else if (statsVariables.contains(name)) {
                    outfileStats.addVariable(name, v.getDataType(), v.getDimensions());
                } else if ("uVAL".equals(name)) {
                    outfileStats.addVariable(
                        "propertyNAME",
                        v.getDataType(),
                        new Dimension[] {
                            infile.findDimension("uVAL"),
                            namesDimension
                        }
                    );
                    outfileStats.addVariable(
                        "propertyVALUE",
                        v.getDataType(),
                        new Dimension[] {
                            infile.findDimension("uVAL"),
                            valuesDimension
                        }
                    );
                } else if (!dropVariables.contains(name)) {
                    throw new AtlasDataException("Unexpected variable name " + name + " in file " + originalFilePath);
                }
            }
            outfileData.create();
            outfileStats.create();
            log.info("Writing data");
            for (Variable v : variables) {
                final String name = v.getName();
                try {
                    if (dataVariables.contains(name)) {
                        outfileData.write(name, v.read());
                    } else if (statsVariables.contains(name)) {
                        outfileStats.write(name, v.read());
                    } else if ("uVAL".equals(name)) {
                        writeList(outfileStats, "propertyNAME", pNames);
                        writeList(outfileStats, "propertyVALUE", pValues);
                    }
                } catch (InvalidRangeException e) {
                    e.printStackTrace();
                }
            }
            outfileData.flush();
            outfileStats.flush();
        } catch (IOException e) {
            throw new AtlasDataException(e);
        } finally {
            closeNetcdfFile(infile);
            closeNetcdfFile(outfileData);
            closeNetcdfFile(outfileStats);
        }
    }

    private static void closeNetcdfFile(NetcdfFile file) {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
            }
        }
    }

    private static int maxLength(List<String> values) {
        int maxValueLength = 0;
        for (String value : values) {
            if (null != value && value.length() > maxValueLength) {
                maxValueLength = value.length();
            }
        }
        return maxValueLength;
    }

    private static void writeList(NetcdfFileWriteable file, String variable, List<String> values) throws IOException, InvalidRangeException {
        ArrayChar valueBuffer = new ArrayChar.D2(1, maxLength(values));
        int i = 0;
        for (String value : values) {
            valueBuffer.setString(0, (null == value ? "" : value));
            file.write(variable, new int[]{i, 0}, valueBuffer);
            ++i;
        }
    }
}
