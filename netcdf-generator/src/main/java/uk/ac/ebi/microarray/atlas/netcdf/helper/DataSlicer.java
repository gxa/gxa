package uk.ac.ebi.microarray.atlas.netcdf.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.*;

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

  private Log log = LogFactory.getLog(getClass());

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
    // the set of data slices we'll be returning
    Set<DataSlice> results = new HashSet<DataSlice>();

    // start fetching data...

    // prefetch assays for this experiment, and populate expression values
    List<Assay> allAssays = getAtlasDAO().getAssaysByExperimentAccession(
        experiment.getAccession());
    getAtlasDAO().getExpressionValuesForAssays(allAssays);

    // prefetch array designs linked to this experiment
    List<ArrayDesign> allArrayDesigns = new ArrayList<ArrayDesign>();
    // map from array design accession to assays for faster indexing
    Map<String, List<Assay>> assayMap = new HashMap<String, List<Assay>>();

    // prefetch samples for this experiment
    // map from assay id to sample for faster indexising
    Map<Integer, List<Sample>> sampleMap =
        new HashMap<Integer, List<Sample>>();
    for (Assay assay : allAssays) {
      // get the accession
      String arrayDesignAccession = assay.getArrayDesignAccession();

      // have we seen this array before?
      if (!assayMap.containsKey(arrayDesignAccession)) {
        // if not, fetch it
        ArrayDesign arrayDesign = getAtlasDAO()
            .getArrayDesignByAccession(arrayDesignAccession);

        // add to our set of array designs
        allArrayDesigns.add(arrayDesign);

        // and init a new list of assays
        List<Assay> assaySet = new ArrayList<Assay>();
        assaySet.add(assay);
        // store in map, so we don't have to fetch assays again
        assayMap.put(arrayDesignAccession, assaySet);
      }
      else {
        // or, just add this assay to the list
        assayMap.get(arrayDesignAccession).add(assay);
      }

      // fetch any samples for this assay
      if (sampleMap.containsKey(assay.getAssayID())) {
        sampleMap.get(assay.getAssayID()).addAll(
            getAtlasDAO().getSamplesByAssayAccession(assay.getAccession()));
      }
      else {
        sampleMap.put(
            assay.getAssayID(),
            getAtlasDAO().getSamplesByAssayAccession(assay.getAccession()));
      }
    }

    // prefetch the expression analytics for our experiment
    List<ExpressionAnalysis> allAnalytics =
        getAtlasDAO().getExpressionAnalyticsByExperimentID(
            experiment.getExperimentID());
    // map analysis to designelementid for fast indexing
    Map<Integer, List<ExpressionAnalysis>> analyticsMap =
        new HashMap<Integer, List<ExpressionAnalysis>>();
    for (ExpressionAnalysis analysis : allAnalytics) {
      if (analyticsMap.containsKey(analysis.getDesignElementID())) {
        analyticsMap.get(analysis.getDesignElementID()).add(analysis);
      }
      else {
        List<ExpressionAnalysis> addAnalytics =
            new ArrayList<ExpressionAnalysis>();
        addAnalytics.add(analysis);
        analyticsMap.put(analysis.getDesignElementID(), addAnalytics);
      }
    }

    // prefetch genes for this experiment
    List<Gene> allGenes = getAtlasDAO().getGenesByExperimentAccession(
        experiment.getAccession());
    // map gene to designelementid for fast indexing
    Map<Integer, Gene> geneMap = new HashMap<Integer, Gene>();
    for (Gene gene : allGenes) {
      if (!geneMap.containsKey(gene.getDesignElementID())) {
        geneMap.put(gene.getDesignElementID(), gene);
      }
      else {
        log.warn("Design Element " + gene.getDesignElementID() + " is mapped " +
            "to multiple genes! Mapping to " + gene.getIdentifier() + " " +
            "will not be preserved");
      }
    }


    // we've prefetched all the data we can (i.e. per experiment queries),
    // so now store the data in the slice in the correct configuration...
    for (ArrayDesign arrayDesign : allArrayDesigns) {
      String arrayDesignAccession = arrayDesign.getAccession();

      // create a new data slice, for this experiment and arrayDesign
      DataSlice dataSlice = new DataSlice(experiment, arrayDesign);

      // prefetch design elements specific to this array design
      List<Integer> designElements = getAtlasDAO()
          .getDesignElementIDsByArrayAccession(arrayDesignAccession);

      // store the assays for this array design in the data slice
      dataSlice.storeAssays(assayMap.get(arrayDesignAccession));

      // store each sample associated with an assay too
      for (Assay assay : assayMap.get(arrayDesignAccession)) {
        // check that this assay maps to samples
        if (sampleMap.get(assay.getAssayID()) != null) {
          // store them, keyed by assay accession
          dataSlice.storeSamplesAssociatedWithAssay(
              assay.getAccession(),
              sampleMap.get(assay.getAssayID()));
        }
        else {
          log.warn("Assay " + assay.getAccession() +
              " does not have any linked samples!");
        }
      }

      // store the design elements for this array design in the data slice
      dataSlice.storeDesignElementIDs(designElements);

      // loop over these design elements and store mapped genes and analytics
      for (int designElementID : designElements) {
        // store mapped genes
        if (geneMap.get(designElementID) != null) {
          dataSlice.storeGene(designElementID, geneMap.get(designElementID));
        }
        else {
          log.warn("Cannot store unmapped genes - " +
              "design element id " + designElementID + " " +
              "has no mapped gene");
        }

        // store mapped analytics
        if (analyticsMap.get(designElementID) != null) {
          dataSlice.storeExpressionAnalyses(designElementID,
                                            analyticsMap.get(designElementID));
        }
        else {
          log.warn("Cannot store unmapped expression analyses - " +
              "design element id " + designElementID + " " +
              "has no mapped analysis");
        }
      }

      // save this dataslice
      results.add(dataSlice);
    }

    return results;
  }
}
