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

package uk.ac.ebi.gxa.netcdf.generator.service;

import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.netcdf.generator.service.ExperimentNetCDFGeneratorService;

import java.io.File;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestExperimentNetCDFGeneratorService
    extends NetCDFGeneratorServiceTestCase {
  public void setUp() throws Exception {
    super.setUp();

    ExperimentNetCDFGeneratorService engs =
        new ExperimentNetCDFGeneratorService(getAtlasDAO(), getRepoLocation(), 1);
    engs.versionDescriptor = "Atlas NetCDF Experiment Generator Version [TEST]";
    setNetCDFGenerator(engs);
  }

  public void tearDown() throws Exception {
    super.tearDown();

    setNetCDFGenerator(null);
  }

  public void testCreateNetCDFDocs() {
    try {
      getNetCDFGenerator().createNetCDFDocs();

      System.out.println("Wrote NetCDFs to " +
          getRepoLocation().getAbsolutePath());
      System.out.println("Created NetCDFS:");
      for (File f : getRepoLocation().listFiles()) {
        System.out.println("\t" + f.getName());
      }
    }
    catch (NetCDFGeneratorException e) {
      e.printStackTrace();
      fail();
    }
  }
}
