package uk.ac.ebi.microarray.atlas.netcdf.service;

import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ExperimentNetCDFGeneratorService
    extends NetCDFGeneratorService<File> {
  private static final int NUM_THREADS = 64;

  public ExperimentNetCDFGeneratorService(AtlasDAO atlasDAO,
                                          File repositoryLocation) {
    super(atlasDAO, repositoryLocation);
  }

  protected void createNetCDFDocs() throws NetCDFGeneratorException {
    // do initial setup - build executor service
    ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

    // fetch experiments - check if we want all or only the pending ones
    List<Experiment> experiments = getPendingOnly()
        ? getAtlasDAO().getAllExperimentsPendingNetCDFs()
        : getAtlasDAO().getAllExperiments();
    // fixme - implement update checks required?


    // the list of futures - we need these so we can block until completion
    List<Future<Boolean>> tasks =
        new ArrayList<Future<Boolean>>();

    try {
      for (final Experiment experiment : experiments) {
        // track arraydesigns that we have done
        Set<String> doneArrays = new HashSet<String>();

        // get all assays
        List<Assay> assays = getAtlasDAO()
            .getAssaysByExperimentAccession(experiment.getAccession());
        for (Assay assay : assays) {
          String accession = assay.getArrayDesignAccession();

          // have we done this array already?
          if (doneArrays.contains(accession)) {
            // todo - should we be doing something cleverer with this assay?
            getLog().warn("Skipping ArrayDesign " + accession + ", " +
                "a NetCDF for this combination of array and experiment has " +
                "already been created (or is being created now)");
          }
          else {
            doneArrays.add(accession);

            final ArrayDesign array =
                getAtlasDAO().getArrayDesignByAccession(accession);

            tasks.add(tpool.submit(new Callable<Boolean>() {
              public Boolean call() throws Exception {
                return createNetCDFForExperimentAndArray(experiment, array);
              }
            }));
          }
        }
      }

      // block until completion, and throw any errors
      for (Future<Boolean> task : tasks) {
        try {
          task.get();
        }
        catch (ExecutionException e) {
          if (e.getCause() instanceof NetCDFGeneratorException) {
            throw (NetCDFGeneratorException) e.getCause();
          }
          else {
            throw new NetCDFGeneratorException(
                "An error occurred updating Experiments SOLR index", e);
          }
        }
        catch (InterruptedException e) {
          throw new NetCDFGeneratorException(
              "An error occurred updating Experiments SOLR index", e);
        }
      }
    }
    finally {
      // shutdown the service
      tpool.shutdown();
    }
  }

  private boolean createNetCDFForExperimentAndArray(Experiment experiment,
                                                    ArrayDesign array) {
    // make a new file
    getLog().info("Generating NetCDF for " +
        "Experiment: " + experiment.getAccession() + ", " +
        "Array Design: " + array.getAccession());

    String netcdfName =
        experiment.getExperimentID() + "_" +
            array.getArrayDesignID() + ".nc";
    String netcdfPath =
        new File(getRepositoryLocation(), netcdfName).getAbsolutePath();
    NetcdfFileWriteable netcdfFile =
        NetcdfFileWriteable.createNew(netcdfPath, false);

    // add global attributes
    netcdfFile.addGlobalAttribute(
        "CreateNetCDF_VERSION",
        versionDescriptor);
    netcdfFile.addGlobalAttribute(
        "experiment_accession",
        experiment.getAccession());
//    netcdfFile.addGlobalAttribute(
//        "quantitationType",
//        qtType); // fixme: quantitation type lookup required
    netcdfFile.addGlobalAttribute(
        "ADaccession",
        array.getAccession());
    netcdfFile.addGlobalAttribute(
        "ADname",
        array.getName());

    // todo - do more stuff!

    // if we got to here without problems, return true
    return true;
  }
}
