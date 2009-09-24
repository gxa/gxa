package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.List;

/**
 * An {@link IndexBuilderService} that generates index documents from the
 * experiments in the Atlas database.
 *
 * @author Tony Burdett
 * @date 22-Sep-2009
 */
public class ExperimentAtlasIndexBuilder extends IndexBuilderService {
  public ExperimentAtlasIndexBuilder(AtlasDAO atlasDAO,
                                     EmbeddedSolrServer solrServer) {
    super(atlasDAO, solrServer);
  }

  protected void createIndexDocs() throws IndexBuilderException {
    try {
      // fetch experiments - check if we want all or only the pending ones
      List<Experiment> experiments = getPendingOnly()
          ? getAtlasDAO().getAllPendingExperiments()
          : getAtlasDAO().getAllExperiments();

      for (Experiment experiment : experiments) {
        // Create a new solr document
        SolrInputDocument solrInputDoc = new SolrInputDocument();

        // Add field "exp_in_dw" = true, to show this experiment is present
        solrInputDoc.addField(Constants.FIELD_EXP_IN_DW, true);

        // now, fetch assays for this experiment
        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(
            experiment.getAccession());
        for (Assay assay : assays) {
          // get assay properties and values
          for (Property prop : assay.getProperties()) {
            String p = prop.getName();
            String pv = prop.getValue();

            solrInputDoc.addField(Constants.PREFIX_DWE + p, pv);
          }

          // now get samples
          List<Sample> samples = getAtlasDAO().getSamplesByAssayAccession(
              assay.getAccession());
          for (Sample sample : samples) {
            // get sample properties and values
            for (Property prop : sample.getProperties()) {
              String p = prop.getName();
              String pv = prop.getValue();

              solrInputDoc.addField(Constants.PREFIX_DWE + p, pv);
            }
          }
        }

        // todo - in old index builder, we'd do some stuff for genes here... 
        // is this still valid? or do we use GeneAtlasIndexBuilder for that?


        // finally, add the document to the index
        getSolrServer().add(solrInputDoc);
      }
    }
    catch (Exception e) {
      throw new IndexBuilderException(e);
    }
  }
}
