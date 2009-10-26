package uk.ac.ebi.microarray.atlas.netcdf.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  public Set<DataSlice> sliceExperiment(Experiment experiment)
      throws DataSlicingException {
    // the set of data slices we'll be returning
    Set<DataSlice> results = new HashSet<DataSlice>();

    // start fetching data...

    // fetch genes and expression analysis - these are fetched by experiment
    log.debug("Fetching genes data for " + experiment.getAccession());
    List<Gene> allGenes = getAtlasDAO().getGenesByExperimentAccession(
        experiment.getAccession());
    log.debug("Fetching analytics data for " + experiment.getAccession());
    List<ExpressionAnalysis> allAnalytics =
        getAtlasDAO().getExpressionAnalyticsByExperimentID(
            experiment.getExperimentID());

    // create dumps for things that didn't resolve to a design element for any data slice
    Set<Gene> unmappedGenes =
        new HashSet<Gene>();
    Set<ExpressionAnalysis> unmappedAnalytics =
        new HashSet<ExpressionAnalysis>();

    // fetch array designs and iterate
    log.debug("Fetching array design data for " + experiment.getAccession());
    List<ArrayDesign> arrays = getAtlasDAO()
        .getArrayDesignByExperimentAccession(experiment.getAccession());
    for (ArrayDesign arrayDesign : arrays) {
      // create a new data slice, for this experiment and arrayDesign
      DataSlice dataSlice = new DataSlice(experiment, arrayDesign);

      // fetch assays for this array
      log.debug("Fetching assay data for " + arrayDesign.getAccession());
      List<Assay> assays = getAtlasDAO()
          .getAssaysByExperimentAndArray(
              experiment.getAccession(), arrayDesign.getAccession());
      // and store
      dataSlice.storeAssays(assays);

      // fetch expression values for this array
      log.debug("Fetch expression values for " + experiment.getAccession() +
          " and " + arrayDesign.getArrayDesignID());
      Map<Integer, Map<String, Float>> expressionValues = getAtlasDAO()
          .getExpressionValuesByExperimentAndArray(
              experiment.getExperimentID(), arrayDesign.getArrayDesignID());
      // and store
      dataSlice.storeExpressionValues(expressionValues);

      log.debug("Fetching samples data for each assay on " +
          arrayDesign.getAccession());
      for (Assay assay : assays) {
        // fetch samples for this assay
        List<Sample> samples = getAtlasDAO()
            .getSamplesByAssayAccession(assay.getAccession());
        for (Sample sample : samples) {
          // and store
          dataSlice.storeSample(assay, sample);
        }
      }

      // fetch design elements specific to this array design
      log.debug(
          "Fetching design element data for " + arrayDesign.getAccession());
      Map<Integer, String> designElements = getAtlasDAO()
          .getDesignElementsByArrayAccession(arrayDesign.getAccession());
      // and store
      dataSlice.storeDesignElements(designElements);

      // genes for this experiment were prefetched -
      // compare to design elements and store, correctly indexed
      log.debug("Indexing gene data by design element ID");
      for (Gene gene : allGenes) {
        // check this gene maps to a stored design element
        if (dataSlice.getDesignElements()
            .containsKey(gene.getDesignElementID())) {
          dataSlice.storeGene(gene.getDesignElementID(), gene);

          // remove from the unmapped list if necessary
          if (unmappedGenes.contains(gene)) {
            unmappedGenes.remove(gene);
          }
        }
        else {
          // exclude this gene - design element not resolvable,
          // or maybe it's just from a different array design
          unmappedGenes.add(gene);
        }
      }

      // expression analyses for this experiment were prefetched -
      // compare to design elements and store, correctly indexed
      log.debug("Indexing analytics data by design element ID");
      for (ExpressionAnalysis analysis : allAnalytics) {
        if (dataSlice.getDesignElements()
            .containsKey(analysis.getDesignElementID())) {
          dataSlice.storeExpressionAnalysis(
              analysis.getDesignElementID(), analysis);

          // remove from the unmapped list if necessary
          if (unmappedAnalytics.contains(analysis)) {
            unmappedAnalytics.remove(analysis);
          }
        }
        else {
          // exclude this gene - design element not resolvable,
          // or maybe it's just from a different array design
          unmappedAnalytics.add(analysis);
        }
      }

      // evaluate property mappings
      log.debug("Evaluating property/value/assay indices for " +
          dataSlice.toString());
      dataSlice.evaluatePropertyMappings();

      // save this dataslice
      log.debug("Compiled dataslice " + dataSlice.toString());
      results.add(dataSlice);
    }

    // compile report on unmapped genes and analytics...
    if (unmappedGenes.size() > 0 || unmappedAnalytics.size() > 0) {
      log.warn(unmappedGenes.size() + "/" + allGenes.size() + " genes and " +
          unmappedAnalytics.size() + "/" + allAnalytics.size() +
          " expression analytics that were recovered for " +
          experiment.getAccession() + " could not be mapped to known " +
          "design elements");

      // todo - generate a log file of unmapped genes and expression analytics
    }

    return results;
  }
}
