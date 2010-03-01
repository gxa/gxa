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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.netcdf.generator.helper;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlicer;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlice;
import uk.ac.ebi.gxa.netcdf.generator.helper.NetCDFFormatter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestNetCDFFormatter extends AtlasDAOTestCase {
  private File repositoryLocation;

  private Map<DataSlice, NetcdfFileWriteable> dataSlices;

  private NetCDFFormatter formatter;

  public void setUp() throws Exception {
    super.setUp();

    repositoryLocation = new File(
        "target" + File.separator + "test" + File.separator + "netcdfs");
    if (!repositoryLocation.exists()) {
      repositoryLocation.mkdirs();
    }
    dataSlices = new HashMap<DataSlice, NetcdfFileWriteable>();

    DataSlicer dataSlicer = new DataSlicer(getAtlasDAO());
    Experiment experiment =
        getAtlasDAO().getExperimentByAccession("E-ABCD-1234");
    Set<DataSlice> slices = dataSlicer.sliceExperiment(experiment);

    // map slices for the given experiment
    for (DataSlice dataSlice : slices) {
      String netcdfName =
          experiment.getExperimentID() + "_" +
              dataSlice.getArrayDesign().getArrayDesignID() + ".nc";
      File f = new File(repositoryLocation, netcdfName);
      String netcdfPath = f.getAbsolutePath();
      NetcdfFileWriteable netcdfFile =
          NetcdfFileWriteable.createNew(netcdfPath, false);

      // add metadata global attributes
      netcdfFile.addGlobalAttribute(
          "CreateNetCDF_VERSION",
          "test-version");
      netcdfFile.addGlobalAttribute(
          "experiment_accession",
          dataSlice.getExperiment().getAccession());
      netcdfFile.addGlobalAttribute(
          "ADaccession",
          dataSlice.getArrayDesign().getAccession());
      netcdfFile.addGlobalAttribute(
          "ADname",
          dataSlice.getArrayDesign().getName());

      dataSlices.put(dataSlice, netcdfFile);
    }
    formatter = new NetCDFFormatter();
  }

  public void tearDown() throws Exception {
    super.tearDown();

    repositoryLocation = null;
    dataSlices = null;

    formatter = null;
  }

  public void testFormatNetCDF() {
    try {
      for (DataSlice dataSlice : dataSlices.keySet()) {
        NetcdfFileWriteable netcdfFile = dataSlices.get(dataSlice);

        // format the netcdf
        formatter.formatNetCDF(netcdfFile, dataSlice);

        // create it
        netcdfFile.create();

        // todo - now profile the netcdf against dataset

        // check global attributes
        for (Attribute att : netcdfFile.getGlobalAttributes()) {
          System.out.println("Next attribute: " + att.toString());
        }

        System.out.println();

        // check dimensions
        for (Dimension d : netcdfFile.getDimensions()) {
          System.out.println("Next dimension: " + d.toString());
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
