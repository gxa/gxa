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
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.loader.db.utils.AtlasDB;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.model.Sample;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;

import javax.sql.DataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

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
    configureHandlers(cache);

    // now, perform the parse - with registered handlers, our cache will be populated
    MAGETABParser parser = new MAGETABParser();
    parser.setParsingMode(ParserMode.READ_AND_WRITE);

    // register an error item listener
    final String idfString = idfFileLocation.toString();
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
          message = "Unknown error";
        }

        // log the error
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
          "Parse exception occurred whilst trying to parse " + idfFileLocation);
      return false;
    }

    // parsing completed, so now write the objects in the cache
    return writeObjects(cache);
  }

  protected void configureHandlers(AtlasLoadCache cache) {
    HandlerPool handlerPool = HandlerPool.getInstance();

    // calibrate the parser with the relevent handlers that can load atlas data
//    handlerPool.replaceHandlerClass()
  }

  protected boolean writeObjects(AtlasLoadCache cache) {
    Connection conn = null;
    try {
      // get a connection from the datasource
      conn = dataSource.getConnection();

      // first, load experiments
      for (Experiment experiment : cache.fetchAllExperiments()) {
        AtlasDB.writeExperiment(conn, experiment);
      }

      // next, load samples
      for (Sample sample : cache.fetchAllSamples()) {
        AtlasDB.writeSample(conn, sample);
      }

      // finally, load assays
      for (Assay assay : cache.fetchAllAssays()) {
        AtlasDB.writeAssay(conn, assay);
      }

      // now, close the connection
      conn.close();

      // and return true - everything loaded ok
      return true;
    }
    catch (SQLException e) {
      // something went wrong with our load, need to restore

      // todo - do unloads on each object up to the one where things went wrong

      // and because the write failed, return false
      return false;
    }
    finally {
      // finally clear the cache, aswe're done with this run
      cache.clear();

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
