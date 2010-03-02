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

package uk.ac.ebi.gxa.netcdf.generator.helper;

import junit.framework.TestCase;
import uk.ac.ebi.microarray.atlas.model.*;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlice;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlicingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestDataSlice extends TestCase {
  private DataSlice dataSlice;

  private Experiment experiment;
  private ArrayDesign arrayDesign;

  public void setUp() {
    experiment = new Experiment();
    arrayDesign = new ArrayDesign();

    dataSlice = new DataSlice(experiment, arrayDesign);
  }

  public void tearDown() {
    dataSlice = null;

    arrayDesign = null;
    experiment = null;
  }

    public void testStoreAssays() {
    Assay ass1 = new Assay();
    Assay ass2 = new Assay();
    List<Assay> storage = new ArrayList<Assay>();
    storage.add(ass1);
    storage.add(ass2);
    dataSlice.storeAssays(storage);

    // now get assays
    assertSame("Wrong number of assays", dataSlice.getAssays().size(), 2);
  }

  public void testStoreSamplesAssociatedWithAssay() {
    try {
// store an assay
      Assay ass1 = new Assay();
      ass1.setAccession("test-assay-1");
      List<Assay> storage = new ArrayList<Assay>();
      storage.add(ass1);
      dataSlice.storeAssays(storage);

      // store a sample
      Sample sample1 = new Sample();
      Sample sample2 = new Sample();
      dataSlice.storeSample(ass1, sample1);
      dataSlice.storeSample(ass1, sample2);

      // check we get 2 samples back
      assertSame(
        "Wrong number of assay-associated samples",
          dataSlice.getSampleMappings().get(ass1).size(),
          2);
    }
    catch (DataSlicingException e) {
      e.printStackTrace();
      fail();
    }
  }

    public void testReset() {

    Assay ass1 = new Assay();
    Assay ass2 = new Assay();
    List<Assay> storage2 = new ArrayList<Assay>();
    storage2.add(ass1);
    storage2.add(ass2);
    dataSlice.storeAssays(storage2);

    dataSlice.reset();

    // dataslice never returns null, just empty collections
    assertSame("Assays was not null", dataSlice.getAssays().size(), 0);
    assertSame("Sample/Assay association was not empty",
               dataSlice.getSampleMappings().size(), 0);
    assertSame("Assays to sample mapping was not empty",
               dataSlice.getSampleMappings().size(), 0);
    assertSame("Samples was not null", dataSlice.getSamples().size(), 0);
  }
}
