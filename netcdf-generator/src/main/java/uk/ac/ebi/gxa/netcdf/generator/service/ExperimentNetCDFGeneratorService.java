package uk.ac.ebi.gxa.netcdf.generator.service;

import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlice;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlicer;
import uk.ac.ebi.gxa.netcdf.generator.helper.NetCDFFormatter;
import uk.ac.ebi.gxa.netcdf.generator.helper.NetCDFWriter;
import uk.ac.ebi.gxa.utils.Deque;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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
    private final int maxThreads;

    public ExperimentNetCDFGeneratorService(AtlasDAO atlasDAO,
                                            File repositoryLocation,
                                            int maxThreads) {
        super(atlasDAO, repositoryLocation);
        this.maxThreads = maxThreads;
    }

    protected void createNetCDFDocs() throws NetCDFGeneratorException {
        // do initial setup - build executor service
        ExecutorService tpool = Executors.newFixedThreadPool(maxThreads);

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = getPendingOnly()
                ? getAtlasDAO().getAllExperimentsPendingNetCDFs()
                : getAtlasDAO().getAllExperiments();

        // create a timer, so we can track time to create NetCDFs
        final NetCDFTimer timer = new NetCDFTimer(experiments);

        // the list of futures - we need these so we can block until completion
        Deque<Future<Boolean>> tasks = new Deque<Future<Boolean>>(10);

        // start the timer
        timer.start();

        try {
            // process each experiment to build the netcdfs
            for (final Experiment experiment : experiments) {
                // run each experiment in parallel
                tasks.offerLast(tpool.submit(new Callable<Boolean>() {

                    public Boolean call() throws Exception {
                        boolean success = false;
                        try {
                            long start = System.currentTimeMillis();

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

                                try {
                                    // write the data from our data slice to this netCDF
                                    NetCDFWriter writer = new NetCDFWriter();
                                    writer.writeNetCDF(netCDF, dataSlice);
                                }
                                finally {
                                    // save and close the netCDF
                                    netCDF.close();
                                }
                            }
                            // update the timer, this experiment is done
                            timer.completed(experiment.getExperimentID());

                            long end = System.currentTimeMillis();
                            String total = new DecimalFormat("#.##").format((end - start) / 1000);
                            String estimate = new DecimalFormat("#.##").format(timer.getCurrentEstimate() / 60000);


                            getLog().info(
                                    "\n\tNetCDF(s) for " + experiment.getAccession() + " created in " + total + "s." +
                                            "\n\tCompleted " + timer.getCompletedExperimentCount() + "/" +
                                            timer.getTotalExperimentCount() + "." +
                                            "\n\tEstimated time remaining: " + estimate + " mins.");
                            success = true;
                            return success;
                        }
                        catch (NetCDFGeneratorException e) {
                            getLog().error("Experiment " + experiment.getAccession() + " NetCDF generation failed.");
                            throw e;
                        }
                        finally {
                            // update loadmonitor - experiment has completed netcdf-ing
                            if (success) {
                                getAtlasDAO().writeLoadDetails(experiment.getAccession(), LoadStage.NETCDF,
                                                               LoadStatus.DONE);
                            }
                            else {
                                getAtlasDAO().writeLoadDetails(experiment.getAccession(), LoadStage.NETCDF,
                                                               LoadStatus.FAILED);
                            }
                        }
                    }
                }));
            }

            experiments.clear();

            // block until completion, and throw any errors
            while (true) {
                Future<Boolean> task = tasks.poll();
                if (task != null) {
                    try {
                        task.get();
                    }
                    catch (ExecutionException e) {
                        getLog().error("NetCDF generation exception", e);
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
                else {
                    break;
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
        ExecutorService tpool = Executors.newFixedThreadPool(maxThreads);

        // fetch experiment - ignore pending if explicitly building by accession
        Experiment experiment = getAtlasDAO().
                getExperimentByAccession(experimentAccession);

        // the list of futures - we need these so we can block until completion
        Deque<Future<Boolean>> tasks =
                new Deque<Future<Boolean>>(5);

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
                tasks.offerLast(tpool.submit(new Callable<Boolean>() {

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
                        try {
                            NetCDFWriter writer = new NetCDFWriter();
                            writer.writeNetCDF(netCDF, dataSlice);
                        }
                        finally {
                            // save and close the netCDF
                            netCDF.close();
                        }
                        getLog().info("Finalising NetCDF changes for " + dataSlice.getExperiment().getAccession() +
                                " and " + dataSlice.getArrayDesign().getAccession());
                        return true;
                    }
                }));
            }

            // block until completion, and throw any errors
            boolean success = true;
            try {
                while (true) {
                    Future<Boolean> task = tasks.poll();
                    if (task == null) {
                        break;
                    }
                    success = task.get() && success;
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

    private class NetCDFTimer {
        private int[] experimentIDs;
        private boolean[] completions;
        private int completedCount;
        private long startTime;
        private long lastEstimate;

        public NetCDFTimer(List<Experiment> experiments) {
            experimentIDs = new int[experiments.size()];
            completions = new boolean[experiments.size()];
            int i = 0;
            for (Experiment exp : experiments) {
                experimentIDs[i] = exp.getExperimentID();
                completions[i] = false;
                i++;
            }

        }

        public synchronized NetCDFTimer start() {
            startTime = System.currentTimeMillis();
            return this;
        }

        public synchronized NetCDFTimer completed(int experimentID) {
            for (int i = 0; i < experimentIDs.length; i++) {
                if (experimentIDs[i] == experimentID) {
                    if (!completions[i]) {
                        completions[i] = true;
                        completedCount++;
                    }
                    break;
                }
            }

            // calculate estimate of time
            long timeWorking = System.currentTimeMillis() - startTime;
            lastEstimate = (timeWorking / completedCount) * (completions.length - completedCount);

            return this;
        }

        public synchronized long getCurrentEstimate() {
            return lastEstimate;
        }

        public synchronized int getCompletedExperimentCount() {
            return completedCount;
        }

        public synchronized int getTotalExperimentCount() {
            return completions.length;
        }
    }
}
