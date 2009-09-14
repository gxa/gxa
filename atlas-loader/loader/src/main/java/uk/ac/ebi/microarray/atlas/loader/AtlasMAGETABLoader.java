package uk.ac.ebi.microarray.atlas.loader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.InvestigationTitleHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonAffiliationHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonLastNameHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.HybridizationHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.SourceHandler;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.loader.db.utils.AtlasDB;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingAccessionHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingInvestigationTitleHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingPersonAffiliationHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingPersonLastNameHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingAssayHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingDerivedArrayDataMatrixHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingHybridizationHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingSourceHandler;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.model.Sample;

import javax.sql.DataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * A Loader application that will insert data from MAGE-TAB format files into
 * the Atlas backend database.
 * <p/>
 * This class should be configured with a {@link javax.sql.DataSource} in order
 * to load data.  This datasource should be an oracle database conforming to the
 * Atlas DB schema, and connection pooling should be externally managed.
 * <p/>
 * This loader can be used either as a standalone application or as part of the
 * atlas-web infrastructure.  IN the first case, using spring to configure the
 * datasource and connection pooling is probably the easiest way to ensure good
 * performance.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasMAGETABLoader {
  private DataSource dataSource;

  // logging
  private Log log = LogFactory.getLog(this.getClass().getSimpleName());

  public DataSource getDataSource() {
    return dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Load a MAGE-TAB format document at the given URL into the Atlas DB.
   *
   * @param idfFileLocation the location of the idf part of the MAGETAB document
   *                        you want to load.
   * @return true if loading suceeded, false if loading failed
   */
  public boolean load(URL idfFileLocation) {
    // create a cache for our objects
    AtlasLoadCache cache = new AtlasLoadCache();

    // create an investigation ready to parse to
    MAGETABInvestigation investigation = new MAGETABInvestigation();

    // pair this cache and this investigation in the registry
    AtlasLoadCacheRegistry.getRegistry().register(investigation, cache);

    // configure the handlers so we write out the right bits
    configureHandlers();

    // now, perform the parse - with registered handlers, our cache will be populated
    MAGETABParser parser = new MAGETABParser();
    parser.setParsingMode(ParserMode.READ_AND_WRITE);

    // register an error item listener
    parser.addErrorItemListener(new ErrorItemListener() {
      public void errorOccurred(ErrorItem item) {
        // lookup message
        String message = "";
        for (ErrorCode ec : ErrorCode.values()) {
          if (item.getErrorCode() == ec.getIntegerValue()) {
            message = ec.getErrorMessage();
            break;
          }
        }
        if (message.equals("")) {
          if (item.getComment().equals("")) {
            message = "Unknown error";
          }
          else {
            message = item.getComment();
          }
        }

        // log the error
        // todo: this should go to a different log stream, part of loader report
        log.error(
            "Parser reported:\n\t" +
                item.getErrorCode() + ": " + message + "\n\t\t- " +
                "occurred in parsing " + item.getParsedFile() + " " +
                "[line " + item.getLine() + ", column " + item.getCol() + "].");
      }
    });

    try {
      parser.parse(idfFileLocation, investigation);
    }
    catch (ParseException e) {
      // something went wrong - no objects have been created though
      log.error(
          "There was a problem whilst trying to parse " + idfFileLocation);
      return false;
    }

    // parsing completed, so now write the objects in the cache
    return writeObjects(cache);
  }

  protected void configureHandlers() {
    HandlerPool pool = HandlerPool.getInstance();

    // calibrate the parser with the relevent handlers that can load atlas data
    pool.replaceHandlerClass(AccessionHandler.class,
                             AtlasLoadingAccessionHandler.class);
    pool.replaceHandlerClass(InvestigationTitleHandler.class,
                             AtlasLoadingInvestigationTitleHandler.class);
    pool.replaceHandlerClass(PersonAffiliationHandler.class,
                             AtlasLoadingPersonAffiliationHandler.class);
    pool.replaceHandlerClass(PersonLastNameHandler.class,
                             AtlasLoadingPersonLastNameHandler.class);
    pool.replaceHandlerClass(SourceHandler.class,
                             AtlasLoadingSourceHandler.class);
    pool.replaceHandlerClass(AssayHandler.class,
                             AtlasLoadingAssayHandler.class);
    pool.replaceHandlerClass(HybridizationHandler.class,
                             AtlasLoadingHybridizationHandler.class);
    pool.replaceHandlerClass(DerivedArrayDataMatrixHandler.class,
                             AtlasLoadingDerivedArrayDataMatrixHandler.class);
  }

  protected boolean writeObjects(AtlasLoadCache cache) {
    int numOfObjects =
        cache.fetchAllExperiments().size() +
            cache.fetchAllSamples().size() +
            cache.fetchAllAssays().size();
    log.info(
        "Writing " + numOfObjects + " objects to " + dataSource.toString());

    Connection conn = null;
    Savepoint initState = null;

    try {
      // get a connection from the datasource
      conn = dataSource.getConnection();

      // create the savepoint
      initState = conn.setSavepoint();

      // first, load experiments
      for (Experiment experiment : cache.fetchAllExperiments()) {
        AtlasDB.writeExperiment(conn, experiment);
      }

      // next, load samples
      for (Sample sample : cache.fetchAllSamples()) {
        AtlasDB.writeSample(conn, sample);
      }

      // finally, load assays
      // test: system.out for checking which assays fail
      System.out.println("Writing assays...");
      for (Assay assay : cache.fetchAllAssays()) {
        System.out.println("\t" + assay.toString());
        AtlasDB.writeAssay(conn, assay);
      }

      // everything saved ok, so commit
      conn.commit();

      // now, close the connection
      conn.close();

      // and return true - everything loaded ok
      log.info("Writing " + numOfObjects + " completed successfully");
      return true;
    }
    catch (SQLException e) {
      // something went wrong with our load, try and rollback
      try {
        if (conn != null && initState != null) {
          conn.rollback(initState);
        }
        else if (conn != null) {
          // initState is null, so we might not be able to completely rollback
          // but try rollback anyway
          log.warn("Savepoint was not correctly generated, " +
              "rollback may not complete reverse all changes");
          conn.rollback();
        }
        else {
          // connection is null, nothing to rollback
        }
      }
      catch (SQLException e1) {
        // we've done the best we can
        log.error("Rollback after a load error failed.  " +
            "No changes should have been committed, " +
            "but there may be other problems");
        e1.printStackTrace();
      }
      log.error("An SQL exception occurred. " + e.getMessage());
      e.printStackTrace();

      // and because the write failed, return false
      return false;
    }
    finally {
      // finally clear the cache, as we're done with this run
      cache.clear();

      log.debug("Emptied cache, cleaning up connections");

      // clean up resources
      try {
        if (conn != null) {
          conn.close();
        }
      }
      catch (SQLException e) {
        // we did our best!
      }
    }
  }
}
