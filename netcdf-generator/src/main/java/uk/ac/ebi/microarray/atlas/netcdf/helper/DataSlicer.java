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
    Set<DataSlice> results = new HashSet<DataSlice>();

    // list of all unique array designs in this experiment
    List<ArrayDesign> arrayDesigns = new ArrayList<ArrayDesign>();
    // map from array design accession to assays - this minimises DB calls
    Map<String, List<Assay>> arrayToAssays =
        new HashMap<String, List<Assay>>();

    // get the assays for this experiment
    List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(
        experiment.getAccession());

    // fetch the expression analytics for our experiment
    List<ExpressionAnalysis> analytics =
        getAtlasDAO().getExpressionAnalyticsByExperimentID(
            experiment.getExperimentID());

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
      // make sure we've fetched expression values for all assays before we store them
      getAtlasDAO().getExpressionValuesForAssays(assays);
      // store the assays for this array design on it
      dataSlice.storeAssays(arrayToAssays.get(arrayDesignAccession));
      // store each sample associated with it's downstream assay too
      for (Assay assay : arrayToAssays.get(arrayDesignAccession)) {
        // fetch any samples for this assay
        List<Sample> samples =
            getAtlasDAO().getSamplesByAssayAccession(assay.getAccession());
        // store them, keyed by assay accession
        dataSlice.storeSamplesAssociatedWithAssay(
            assay.getAccession(), samples);
      }

      // store design elements
      dataSlice.storeDesignElementIDs(getAtlasDAO()
          .getDesignElementIDsByArrayAccession(arrayDesignAccession));

      // map analysis to geneid for fast indexing
      Map<Integer, List<ExpressionAnalysis>> analyticsMap =
          new HashMap<Integer, List<ExpressionAnalysis>>();
      for (ExpressionAnalysis analysis : analytics) {
        if (analyticsMap.containsKey(analysis.getGeneID())) {
          analyticsMap.get(analysis.getGeneID()).add(analysis);
        }
        else {
          List<ExpressionAnalysis> addAnalytics =
              new ArrayList<ExpressionAnalysis>();
          addAnalytics.add(analysis);
          analyticsMap.put(analysis.getGeneID(), addAnalytics);
        }
      }

      // fetch genes
      List<Gene> genes = getAtlasDAO().getGenesByExperimentAccession(
          experiment.getAccession());
      // map ready for storage
      Map<Integer, Gene> geneMap = new HashMap<Integer, Gene>();
      for (Gene gene : genes) {
        // retrieve the expression analytics for each gene
        List<ExpressionAnalysis> analysis = analyticsMap.get(gene.getGeneID());
        dataSlice.storeExpressionAnalyses(gene.getDesignElementID(), analysis);

        // get design element id and check its ok to map
        if (dataSlice.getDesignElementIDs()
            .contains(gene.getDesignElementID())) {
          // check for one to many mapping
          if (geneMap.containsKey(gene.getDesignElementID())) {
            log.warn("Design Element " + gene.getDesignElementID() +
                " maps to multiple genes!");
          }
          else {
            geneMap.put(gene.getDesignElementID(), gene);
          }
        }
      }
      dataSlice.storeGenes(geneMap);

      // save this dataslice
      results.add(dataSlice);
    }

    return results;
  }
}
