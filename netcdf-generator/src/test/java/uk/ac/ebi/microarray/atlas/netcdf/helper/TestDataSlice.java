package uk.ac.ebi.microarray.atlas.netcdf.helper;

import junit.framework.TestCase;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

  public void testStoreGenes() {
    int deID1 = 1;
    Gene gene1 = new Gene();
    int deID2 = 2;
    Gene gene2 = new Gene();

    Map<Integer, Gene> genes = new HashMap<Integer, Gene>();
    genes.put(deID1, gene1);
    genes.put(deID2, gene2);

    dataSlice.storeGenes(genes);

    // now get genes
    assertSame("Wrong number of genes", dataSlice.getGenes().keySet().size(), 2);
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
    // store an assay
    Assay ass1 = new Assay();
    ass1.setAccession("test-assay-1");
    List<Assay> storage = new ArrayList<Assay>();
    storage.add(ass1);
    dataSlice.storeAssays(storage);

    // store a sample
    Sample sample1 = new Sample();
    Sample sample2 = new Sample();
    List<Sample> storage2 = new ArrayList<Sample>();
    storage2.add(sample1);
    storage2.add(sample2);
    dataSlice.storeSamplesAssociatedWithAssay(ass1.getAccession(), storage2);

    // check we get 2 samples back
    assertSame(
        "Wrong number of assay-associated samples",
        dataSlice.getSamplesAssociatedWithAssay(ass1.getAccession()).size(),
        2);
  }

  public void testStoreDesignElementIDs() {
    Integer int1 = 1;
    Integer int2 = 2;
    List<Integer> storage = new ArrayList<Integer>();
    storage.add(int1);
    storage.add(int2);
    dataSlice.storeDesignElementIDs(storage);

    // now get design elements
    assertSame("Wrong number of design elements", dataSlice.getDesignElementIDs().size(), 2);
  }

  public void testReset() {
    Integer int1 = 1;
    Integer int2 = 2;
    List<Integer> storage = new ArrayList<Integer>();
    storage.add(int1);
    storage.add(int2);
    dataSlice.storeDesignElementIDs(storage);

    Assay ass1 = new Assay();
    Assay ass2 = new Assay();
    List<Assay> storage2 = new ArrayList<Assay>();
    storage2.add(ass1);
    storage2.add(ass2);
    dataSlice.storeAssays(storage2);

    dataSlice.reset();

    assertNull("Assays was not null", dataSlice.getAssays());
    assertSame("Sample/Assay association was not empty", dataSlice.getSamplesAssociatedWithAssay("test-1").size(), 0);
    assertSame("Assays to sample mapping was not empty", dataSlice.getAssayToSampleMapping().size(), 0);
    assertNull("Design Elements was not null", dataSlice.getDesignElementIDs());
    assertNull("Genes was not null", dataSlice.getGenes());
    assertNull("Samples was not null", dataSlice.getSamples());
  }
}
