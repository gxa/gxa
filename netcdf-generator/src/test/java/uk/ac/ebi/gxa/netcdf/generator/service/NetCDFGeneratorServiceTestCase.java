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

import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.netcdf.generator.service.NetCDFGeneratorService;
import uk.ac.ebi.gxa.utils.FileUtil;

import java.io.File;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public abstract class NetCDFGeneratorServiceTestCase extends AtlasDAOTestCase {
  private File repoLocation;

  private NetCDFGeneratorService netCDFGenerator;

  public NetCDFGeneratorService getNetCDFGenerator() {
    return netCDFGenerator;
  }

  public void setNetCDFGenerator(NetCDFGeneratorService netCDFGenerator) {
    this.netCDFGenerator = netCDFGenerator;
  }

  public File getRepoLocation() {
    return repoLocation;
  }

  public void setUp() throws Exception {
    super.setUp();

    repoLocation = new File(
        System.getProperty("java.io.tmpdir") + File.separator + "test" + File.separator + "netcdfs");
    if (!repoLocation.exists()) {
      repoLocation.mkdirs();
    }
  }

  public void tearDown() throws Exception {
    super.tearDown();

    // delete the repo
    if (!FileUtil.deleteDirectory(repoLocation)) {
      fail("Failed to delete " + repoLocation.getAbsolutePath());
    }

    repoLocation = null;
  }
}
