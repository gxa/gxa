package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An {@link IndexBuilderService} that generates index documents from the
 * experiments in the Atlas database.
 *
 * @author Tony Burdett
 * @date 22-Sep-2009
 */
public class ExperimentAtlasIndexBuilder extends IndexBuilderService {
  private static final int NUM_THREADS = 64;

  public ExperimentAtlasIndexBuilder(AtlasDAO atlasDAO,
                                     EmbeddedSolrServer solrServer) {
    super(atlasDAO, solrServer);
  }

  protected void createIndexDocs() throws IndexBuilderException {
    try {
      // do initial setup - build executor service
      ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

      // fetch experiments - check if we want all or only the pending ones
      List<Experiment> experiments = getPendingOnly()
          ? getAtlasDAO().getAllPendingExperiments()
          : getAtlasDAO().getAllExperiments();

      for (final Experiment experiment : experiments) {
        tpool.submit(new Callable<UpdateResponse>() {
          public UpdateResponse call() throws IOException, SolrServerException {
            // Create a new solr document
            SolrInputDocument solrInputDoc = new SolrInputDocument();

            // Add field "exp_in_dw" = true, to show this experiment is present
            getLog().debug(
                "Updating index - experiment " + experiment.getAccession() +
                    " is in DB");
            solrInputDoc.addField(Constants.FIELD_EXP_IN_DW, true);

            // now, fetch assays for this experiment
            List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(
                experiment.getAccession());
            for (Assay assay : assays) {
              // get assay properties and values
              for (Property prop : assay.getProperties()) {
                String p = prop.getName();
                String pv = prop.getValue();

                getLog()
                    .debug("Updating index, assay property " + p + " = " + pv);
                solrInputDoc.addField(Constants.PREFIX_DWE + p, pv); // fixme: format of property names in index?
              }

              // now get samples
              List<Sample> samples = getAtlasDAO().getSamplesByAssayAccession(
                  assay.getAccession());
              for (Sample sample : samples) {
                // get sample properties and values
                for (Property prop : sample.getProperties()) {
                  String p = prop.getName();
                  String pv = prop.getValue();

                  getLog().debug(
                      "Updating index, sample property " + p + " = " + pv);
                  solrInputDoc.addField(Constants.PREFIX_DWE + p, pv); // fixme: format of property names in index?
                }
              }
            }

            // todo - in old index builder, we'd do some stuff for genes here...
            // is this still valid? or do we use GeneAtlasIndexBuilder for that?


            // finally, add the document to the index
            getLog().debug("Finalising changes for " +
                experiment.getAccession());
            return getSolrServer().add(solrInputDoc);
          }
        });
      }
    }
    catch (Exception e) {
      throw new IndexBuilderException(e);
    }
  }
}
