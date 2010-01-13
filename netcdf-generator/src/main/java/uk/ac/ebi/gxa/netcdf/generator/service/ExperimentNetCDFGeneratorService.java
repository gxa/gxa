package uk.ac.ebi.gxa.netcdf.generator.service;

import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlice;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlicer;
import uk.ac.ebi.gxa.netcdf.generator.helper.NetCDFFormatter;
import uk.ac.ebi.gxa.netcdf.generator.helper.NetCDFWriter;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.dao.LoadStage;
import uk.ac.ebi.microarray.atlas.dao.LoadStatus;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

        // the list of futures - we need these so we can block until completion
        List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();

        try {
            // process each experiment to build the netcdfs
            for (final Experiment experiment : experiments) {
                // run each experiment in parallel
                tasks.add(tpool.submit(new Callable<Boolean>() {

                    public Boolean call() throws Exception {
                        boolean success = false;
                        try {
                            // update loadmonitor - experiment is netcdf-ing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.NETCDF, LoadStatus.WORKING);

                            getLog().info("Generating NetCDFs - experiment " +
                                    experiment.getAccession());

                            // create a data slicer to slice up this experiment
                            DataSlicer slicer = new DataSlicer(getAtlasDAO());

                            // slice our experiments - ok to do these one by one
                            for (DataSlice dataSlice : slicer.sliceExperiment(experiment)) {
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

                                // save and close the netCDF
                                netCDF.close();
                            }

                            getLog().info("Finalising NetCDF changes for " +
                                    experiment.getAccession());
                            success = true;

                            // update loadmonitor - experiment has completed netcdf-ing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.NETCDF, LoadStatus.DONE);

                            return success;
                        }
                        finally {
                            // if success if true, everything completed as expected, but if it's false we got
                            // an uncaught exception, so make sure we update loadmonitor to reflect that this failed
                            if (!success) {
                                getAtlasDAO().writeLoadDetails(
                                        experiment.getAccession(), LoadStage.NETCDF, LoadStatus.FAILED);
                            }

                            // perform an explicit garbage collection to make sure all refs to large datasets are cleaned up
                            System.gc();
                        }
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
                                "An error occurred updating NetCDFs", e);
                    }
                }
                catch (InterruptedException e) {
                    throw new NetCDFGeneratorException(
                            "An error occurred updating NetCDFs", e);
                }
            }
        }
        finally {
            // shutdown the service
            getLog().debug("Shutting down executor service in " +
                    getClass().getSimpleName());

            try {
                tpool.shutdown();
                tpool.awaitTermination(60, TimeUnit.SECONDS);
                if (!tpool.isTerminated()) {
                    //noinspection ThrowFromFinallyBlock
                    throw new NetCDFGeneratorException(
                            "Failed to terminate service for " + getClass().getSimpleName() +
                                    " cleanly - suspended tasks were found");
                }
                else {
                    getLog().debug("Executor service exited cleanly");
                }
            }
            catch (InterruptedException e) {
                //noinspection ThrowFromFinallyBlock
                throw new NetCDFGeneratorException(
                        "Failed to terminate service for " + getClass().getSimpleName() +
                                " cleanly - suspended tasks were found");
            }
        }
    }

    protected void createNetCDFDocsForExperiment(String experimentAccession)
            throws NetCDFGeneratorException {
        // do initial setup - build executor service
        ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

        // fetch experiment - ignore pending if explicitly building by accession
        Experiment experiment = getAtlasDAO().
                getExperimentByAccession(experimentAccession);

        // the list of futures - we need these so we can block until completion
        List<Future<Boolean>> tasks =
                new ArrayList<Future<Boolean>>();

        try {
            getLog().info("Generating NetCDFs - experiment " +
                    experiment.getAccession());

            // update loadmonitor - experiment is netcdf-ing
            getAtlasDAO().writeLoadDetails(
                    experiment.getAccession(), LoadStage.NETCDF, LoadStatus.WORKING);

            // create a data slicer to slice up this experiment
            DataSlicer slicer = new DataSlicer(getAtlasDAO());

            // slice our experiment first
            for (final DataSlice dataSlice : slicer.sliceExperiment(experiment)) {
                // run each experiment in parallel
                tasks.add(tpool.submit(new Callable<Boolean>() {

                    public Boolean call() throws Exception {
                        try {
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

                            // save and close the netCDF
                            netCDF.close();

                            getLog().info("Finalising NetCDF changes for " + dataSlice.getExperiment().getAccession() +
                                    " and " + dataSlice.getArrayDesign().getAccession());
                            return true;
                        }
                        finally {
                            // perform an explicit garbage collection to make sure all refs to large datasets are cleaned up
                            System.gc();
                        }
                    }
                }));
            }

            // block until completion, and throw any errors
            boolean success = true;
            try {
                for (Future<Boolean> task : tasks) {
                    success = success && task.get();
                }
            }
            catch (ExecutionException e) {
                success = false;
                if (e.getCause() instanceof NetCDFGeneratorException) {
                    throw (NetCDFGeneratorException) e.getCause();
                }
                else {
                    throw new NetCDFGeneratorException("An error occurred updating NetCDFs", e);
                }
            }
            catch (InterruptedException e) {
                success = false;
                throw new NetCDFGeneratorException("An error occurred updating NetCDFs", e);
            }
            finally {
                // update loadmonitor - experiment has completed netcdf-ing
                if (success) {
                    getAtlasDAO().writeLoadDetails(experiment.getAccession(), LoadStage.NETCDF, LoadStatus.DONE);
                }
                else {
                    getAtlasDAO().writeLoadDetails(experiment.getAccession(), LoadStage.NETCDF, LoadStatus.FAILED);
                }
            }
        }
        finally {
            // shutdown the service
            getLog().debug("Shutting down executor service in " +
                    getClass().getSimpleName() + " (" + tpool.toString() + ") for " +
                    experimentAccession);

            try {
                tpool.shutdown();
                tpool.awaitTermination(60, TimeUnit.SECONDS);
                if (!tpool.isTerminated()) {
                    //noinspection ThrowFromFinallyBlock
                    throw new NetCDFGeneratorException(
                            "Failed to terminate service for " + getClass().getSimpleName() +
                                    " cleanly - suspended tasks were found");
                }
                else {
                    getLog().debug("Executor service exited cleanly");
                }
            }
            catch (InterruptedException e) {
                //noinspection ThrowFromFinallyBlock
                throw new NetCDFGeneratorException(
                        "Failed to terminate service for " + getClass().getSimpleName() +
                                " cleanly - suspended tasks were found");
            }
        }
    }

    private NetcdfFileWriteable createNetCDF(Experiment experiment,
                                             ArrayDesign arrayDesign)
            throws IOException {
        // repository location exists?
        if (!getRepositoryLocation().exists()) {
            if (!getRepositoryLocation().mkdirs()) {
                throw new IOException("Could not read create directory at " +
                        getRepositoryLocation().getAbsolutePath());
            }
        }

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
                "ADid",
                arrayDesign.getArrayDesignID());
        netcdfFile.addGlobalAttribute(
                "ADname",
                arrayDesign.getName());

        return netcdfFile;
    }
}
