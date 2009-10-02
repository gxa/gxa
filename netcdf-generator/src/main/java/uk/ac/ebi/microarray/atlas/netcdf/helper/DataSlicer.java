package uk.ac.ebi.microarray.atlas.netcdf.helper;

import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.netcdf.helper.DataSlice;

import java.util.*;

/**
 * A device for slicing the data in a single experiment into bits that are
 * suitable for storing in NetCDFs.  Generally, this means a single slice of
 * data per array design, and each slice indexes every sample by the assay it is
 * associated with.  Only the assays appropriate for the paired array design are
 * present.  This class basically takes an AtlasDAO it can use to fetch any
 * additional data, and performs the slicing on a supplied experiment.
 *
 * @author Tony Burdett
 * @date 30-Sep-2009
 */
public class DataSlicer {
  private AtlasDAO atlasDAO;

  public DataSlicer(AtlasDAO atlasDAO) {
    this.atlasDAO = atlasDAO;
  }

  public AtlasDAO getAtlasDAO() {
    return atlasDAO;
  }

  public void setAtlasDAO(AtlasDAO atlasDAO) {
    this.atlasDAO = atlasDAO;
  }

  public Set<DataSlice> sliceExperiment(Experiment experiment) {
    Set<DataSlice> results = new HashSet<DataSlice>();

    // list of all unique array designs in this experiment
    List<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
    // map from array design accession to assays - this minimises DB calls
    Map<String, List<Assay>> arrayToAssays =
        new HashMap<String, List<Assay>>();

    // get the assays for this experiment
    List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(
        experiment.getAccession());

    // loop over assays to get array designs
    for (Assay assay : assays) {
      // get the accession
      String arrayDesignAccession = assay.getArrayDesignAccession();

      // have we seen this array before?
      if (!arrayToAssays.containsKey(arrayDesignAccession)) {
        // if not, fetch it
        ArrayDesign arrayDesign = getAtlasDAO()
            .getArrayDesignByAccession(arrayDesignAccession);

        // add to our set of array designs
        arrayDesigns.add(arrayDesign);

        // and init a new list of assays
        List<Assay> assaySet = new ArrayList<Assay>();
        assaySet.add(assay);
        // store in map, so we don't have to fetch assays again
        arrayToAssays.put(arrayDesignAccession, assaySet);
      }
      else {
        // or, just add this assay to the list
        arrayToAssays.get(arrayDesignAccession).add(assay);
      }
    }

    // now our data is appropriately sliced so start building up NetCDFs properly
    for (ArrayDesign arrayDesign : arrayDesigns) {
      String arrayDesignAccession = arrayDesign.getAccession();

      // create a new data slice, for this experiment and arrayDesign
      DataSlice dataSlice = new DataSlice(experiment, arrayDesign);
      // store the assays for this array design on it
      dataSlice.storeAssays(arrayToAssays.get(arrayDesignAccession));
      // store each sample associated with it's downstream assay too
      for (Assay assay : arrayToAssays.get(arrayDesignAccession)) {
        dataSlice.storeSamplesAssociatedWithAssay(
            assay.getAccession(),
            getAtlasDAO().getSamplesByAssayAccession(assay.getAccession()));
      }

      // store design elements
      System.out.println("Searching for design element IDs for " + arrayDesignAccession);
      dataSlice.storeDesignElementIDs(getAtlasDAO()
          .getDesignElementsByArrayAccession(arrayDesignAccession));

      // store genes
      dataSlice.storeGenes(getAtlasDAO().getGenesByExperimentAccession(
          experiment.getAccession()));

      // save this dataslice
      results.add(dataSlice);
    }

    return results;
  }
}
