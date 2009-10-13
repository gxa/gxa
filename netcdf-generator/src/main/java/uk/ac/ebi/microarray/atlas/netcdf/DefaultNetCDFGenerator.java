package uk.ac.ebi.microarray.atlas.netcdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.netcdf.listener.NetCDFGenerationEvent;
import uk.ac.ebi.microarray.atlas.netcdf.listener.NetCDFGeneratorListener;
import uk.ac.ebi.microarray.atlas.netcdf.service.ExperimentNetCDFGeneratorService;
import uk.ac.ebi.microarray.atlas.netcdf.service.NetCDFGeneratorService;

import javax.sql.DataSource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A default implementation of {@link NetCDFGenerator} that builds a NetCDF
 * repository at a given {@link File} on the local filesystem.
 *
 * @author Tony Burdett
 * @date 17-Sep-2009
 */
public class DefaultNetCDFGenerator
    implements NetCDFGenerator<File>, InitializingBean {
  private DataSource dataSource;
  private File repositoryLocation;

  private NetCDFGeneratorService netCDFService;

  private ExecutorService service;

  // logging
  private static final Logger log =
      LoggerFactory.getLogger(DefaultNetCDFGenerator.class);

  public void setAtlasDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public DataSource getAtlasDataSource() {
    return dataSource;
  }

  public void setRepositoryLocation(File repositoryLocation) {
    this.repositoryLocation = repositoryLocation;
  }

  public File getRepositoryLocation() {
    return repositoryLocation;
  }

  public void afterPropertiesSet() throws Exception {
    startup();
  }

  public void startup() throws NetCDFGeneratorException {
    // do some initialization...

    // check the repository location exists, or else create it
    if (!repositoryLocation.exists()) {
      if (!repositoryLocation.mkdirs()) {
        log.error("Couldn't create " + repositoryLocation.getAbsolutePath());
        throw new NetCDFGeneratorException("Unable to create NetCDF " +
            "repository at " + repositoryLocation.getAbsolutePath());
      }
    }

    // create a spring jdbc template
    JdbcTemplate template = new JdbcTemplate(dataSource);

    // create an atlas dao
    AtlasDAO dao = new AtlasDAO();
    dao.setJdbcTemplate(template);

    // create the service
    netCDFService =
        new ExperimentNetCDFGeneratorService(dao, repositoryLocation);

    // finally, create an executor service for processing calls to build the index
    service = Executors.newCachedThreadPool();
  }

  public void shutdown() throws NetCDFGeneratorException {
    // todo - really nothing to shutdown?
  }

  public void generateNetCDFs() {
    generateNetCDFs(null);
  }

  public void generateNetCDFs(final NetCDFGeneratorListener listener) {
    final long startTime = System.currentTimeMillis();
    final List<Future<Boolean>> buildingTasks =
        new ArrayList<Future<Boolean>>();

    buildingTasks.add(service.submit(new Callable<Boolean>() {
      public Boolean call() throws NetCDFGeneratorException {
        try {
        log.info("Starting NetCDF generations");

        netCDFService.generateNetCDFs();

        return true;
        }
        catch (NetCDFGeneratorException e) {
          e.printStackTrace();
          throw e;
        }
      }
    }));

    // this tracks completion, if a listener was supplied
    if (listener != null) {
      service.submit(new Runnable() {
        public void run() {
          boolean success = true;
          List<Throwable> observedErrors = new ArrayList<Throwable>();

          // wait for expt and gene indexes to build
          for (Future<Boolean> indexingTask : buildingTasks) {
            try {
              success = success && indexingTask.get();
            }
            catch (Exception e) {
              observedErrors.add(e);
              success = false;
            }
          }

          // now we've finished - get the end time, calculate runtime and fire the event
          long endTime = System.currentTimeMillis();
          long runTime = (endTime - startTime) / 1000;

          // create our completion event
          if (success) {
            listener.buildSuccess(new NetCDFGenerationEvent(
                runTime, TimeUnit.SECONDS));
          }
          else {
            listener.buildError(new NetCDFGenerationEvent(
                runTime, TimeUnit.SECONDS, observedErrors));
          }
        }
      });
    }
  }

  public void generateNetCDFsForExperiment(String experimentAccession) {
    generateNetCDFsForExperiment(experimentAccession, null);
  }

  public void generateNetCDFsForExperiment(String experimentAccession,
                                           NetCDFGeneratorListener listener) {
    // todo - run the generator with argument "experimentAccession"
  }
}
