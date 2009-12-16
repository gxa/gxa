package uk.ac.ebi.gxa.analytics.generator.service;

import org.kchine.r.RDataFrame;
import org.kchine.r.RList;
import org.kchine.r.RObject;
import org.kchine.r.server.RServices;
import server.DirectJNI;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRServicesException;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.dao.LoadStage;
import uk.ac.ebi.microarray.atlas.dao.LoadStatus;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ExperimentAnalyticsGeneratorService extends AnalyticsGeneratorService<File> {
    private static final int NUM_THREADS = 64;

    public ExperimentAnalyticsGeneratorService(AtlasDAO atlasDAO,
                                               File repositoryLocation,
                                               AtlasRFactory atlasRFactory) {
        super(atlasDAO, repositoryLocation, atlasRFactory);
    }

    protected void createAnalytics() throws AnalyticsGeneratorException {
        // do initial setup - build executor service
        ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);

        // fetch experiments - check if we want all or only the pending ones
        List<Experiment> experiments = getPendingOnly()
                ? getAtlasDAO().getAllExperimentsPendingAnalytics()
                : getAtlasDAO().getAllExperiments();

        // the list of futures - we need these so we can block until completion
        List<Future<Boolean>> tasks =
                new ArrayList<Future<Boolean>>();

        try {
            // process each experiment to build the netcdfs
            for (final Experiment experiment : experiments) {
                // run each experiment in parallel
                tasks.add(tpool.submit(new Callable<Boolean>() {

                    public Boolean call() throws Exception {
                        boolean success = false;
                        try {
                            // update loadmonitor - experiment is indexing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.RANKING, LoadStatus.WORKING);

                            createAnalyticsForExperiment(experiment.getAccession());
                            success = true;

                            // update loadmonitor - experiment is indexing
                            getAtlasDAO().writeLoadDetails(
                                    experiment.getAccession(), LoadStage.RANKING, LoadStatus.DONE);

                            return success;
                        }
                        finally {
                            // if success if true, everything completed as expected, but if it's false we got
                            // an uncaught exception, so make sure we update loadmonitor to reflect that this failed
                            if (!success) {
                                getAtlasDAO().writeLoadDetails(
                                        experiment.getAccession(), LoadStage.RANKING, LoadStatus.FAILED);
                            }
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
                    if (e.getCause() instanceof AnalyticsGeneratorException) {
                        throw (AnalyticsGeneratorException) e.getCause();
                    }
                    else {
                        throw new AnalyticsGeneratorException(
                                "An error occurred updating Analytics", e);
                    }
                }
                catch (InterruptedException e) {
                    throw new AnalyticsGeneratorException(
                            "An error occurred updating Analytics", e);
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
                    throw new AnalyticsGeneratorException(
                            "Failed to terminate service for " + getClass().getSimpleName() +
                                    " cleanly - suspended tasks were found");
                }
                else {
                    getLog().debug("Executor service exited cleanly");
                }
            }
            catch (InterruptedException e) {
                //noinspection ThrowFromFinallyBlock
                throw new AnalyticsGeneratorException(
                        "Failed to terminate service for " + getClass().getSimpleName() +
                                " cleanly - suspended tasks were found");
            }
        }
    }

    protected void createAnalyticsForExperiment(String experimentAccession) throws AnalyticsGeneratorException {
        try {
            getLog().info("Generating analytics for experiment " + experimentAccession);

            // create a R service - DirectJNI gets an R service on the local machine
            RServices rs = getAtlasRFactory().createRServices();
//            RServices rs = DirectJNI.getInstance().getRServices();

            // load the R code that runs the analytics
            rs.sourceFromBuffer(getRCodeFromResource("R/analytics.R"));

            // work out where the NetCDF(s) are located
            final Experiment experiment = getAtlasDAO().getExperimentByAccession(experimentAccession);
            File[] netCDFs = getRepositoryLocation().listFiles(new FilenameFilter() {

                public boolean accept(File file, String name) {
                    return name.matches("^" + experiment.getExperimentID() + "_[0-9]+(_ratios)?\\.nc$");
                }
            });

            for (File netCDF : netCDFs) {
//                String callSim = "computeAnalytics(" + netCDF + "')";
//                RList analytics = (RList) rs.getObject(callSim);

                rs.getObject("computeAnalytics("  + netCDF.getAbsolutePath() + ")");

                RObject r = rs.getObject("computeAnalytics('/ebi/ArrayExpress-files/NetCDFs.ATLAS.OTTO/325701228_170473054.nc')");//rs.getObject("2+2");

                // experiment design
//                > pData(eset)
//             ba_genmodif        ba_genotype
//325701235 gene_knock_out miRNA-1-2 knockout
//325701236 gene_knock_out miRNA-1-2 knockout
//325701237           none          wild_type
//325701238 gene_knock_out miRNA-1-2 knockout
//325701239           none          wild_type
//325701240           none          wild_type


                // after analytics, variable length as long as nr of EFs
//               > names(res)
//[1] "ba_genmodif" "ba_genotype"

//                for(i = 0 ... ///String ef : analytics.getNames()) {
//                    RObject[] analyticsFrames = analytics.getValue();


                    // for each EF get a data frame looking like

//                > head(res[["ba_genotype"]],n=2)
//                 A        t.1        t.2 p.value.1 p.value.2 p.value.adj.1
//172585796 8.514054 -0.1442487  0.1442487 0.8884583 0.8884583     0.9996772
//172611612 6.596359  0.2683077 -0.2683077 0.7944649 0.7944649     0.9996772
//          p.value.adj.2 Res.miRNA-1-2 knockout Res.wild_type          F
//172585796     0.9996772                      0             0 0.02080768
//172611612     0.9996772                      0             0 0.07198903
//          F.p.value F.p.value.adj  Genes.gn  Genes.de  Genes.ID
//172585796 0.8884583     0.9996772 169918541 172585796 172585796
//172611612 0.7944649     0.9996772 169918541 172611612 172611612


                // ((RDataFrame) analyticsFrames[0]).getRowNames();
//                ((RDataFrame) analyticsFrames[0]).getData();
                //   these are the column headings above - .N corresponds to the EFV numbers in the EF
                //   Res.XYZ to the EFV names, Res.XYZ contains -1,0,1 up/dn, t.N - tstat, p.value.adj.N - p vals, Genes.gn - gene id,
                //   genes.de - designelt id, F.p.value.adj - per EF.
  //              }
            }

            getLog().info("Finalising analytics changes for " + experimentAccession);
        }
        catch (IOException e) {
            throw new AnalyticsGeneratorException(e);
        }
        catch (AtlasRServicesException e) {
            throw new AnalyticsGeneratorException(e);
        }
    }

    private String getRCodeFromResource(String resourcePath) throws IOException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

        // create a reader to read in code
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        String rCode = sb.toString();
        System.out.println(rCode);

        return rCode;
    }
}
