package uk.ac.ebi.microarray.atlas.netcdf.service;

import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;
import uk.ac.ebi.microarray.atlas.netcdf.helper.DataSlicer;
import uk.ac.ebi.microarray.atlas.netcdf.helper.NetCDFFormatter;
import uk.ac.ebi.microarray.atlas.netcdf.helper.NetCDFWriter;
import uk.ac.ebi.microarray.atlas.netcdf.helper.DataSlice;

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
    // fixme - implement update checks, if required?

    // the list of futures - we need these so we can block until completion
    List<Future<Boolean>> tasks =
        new ArrayList<Future<Boolean>>();

    try {
      // create a data slicer to slice up our experiments
      DataSlicer slicer = new DataSlicer(getAtlasDAO());
      // slice our experiments
      Set<DataSlice> dataSlices = new HashSet<DataSlice>();
      for (Experiment experiment : experiments) {
        dataSlices.addAll(slicer.sliceExperiment(experiment));
      }

      // process each dataslice to build the netcdf
      for (final DataSlice dataSlice : dataSlices) {
        // run slices in parallel
        tasks.add(tpool.submit(new Callable<Boolean>() {

          public Boolean call() throws Exception {
            // create a new NetCDF document
            NetcdfFileWriteable netCDF = createNetCDF(
                dataSlice.getExperiment(),
                dataSlice.getArrayDesign());

            // format it with paramaters suitable for our data
            NetCDFFormatter formatter = new NetCDFFormatter();
            formatter.formatNetCDF(netCDF, dataSlice);

            // actually create the netCDF
            netCDF.create();

            // write the data from our data slice to this netCDF
            NetCDFWriter writer = new NetCDFWriter();
            writer.writeNetCDF(netCDF, dataSlice);

            // todo - still need to write matrices for...
            // BDC - DE/AS
            // PVAL - DE/uEFV
            // TSTAT - DE/uEFV

            // save and close the netCDF
            netCDF.close();

            return true;
          }
        }));
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

  private NetcdfFileWriteable createNetCDF(Experiment experiment,
                                           ArrayDesign arrayDesign) {
    // make a new file
    getLog().info("Generating NetCDF for " +
        "Experiment: " + experiment.getAccession() + ", " +
        "Array Design: " + arrayDesign.getAccession());

    String netcdfName =
        experiment.getExperimentID() + "_" +
            arrayDesign.getArrayDesignID() + ".nc";
    String netcdfPath =
        new File(getRepositoryLocation(), netcdfName).getAbsolutePath();
    NetcdfFileWriteable netcdfFile =
        NetcdfFileWriteable.createNew(netcdfPath, false);

    // add metadata global attributes
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
        arrayDesign.getAccession());
    netcdfFile.addGlobalAttribute(
        "ADname",
        arrayDesign.getName());

    return netcdfFile;
  }
}
