package uk.ac.ebi.microarray.atlas.loader.utils;

import org.apache.commons.logging.Log;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.Status;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

/**
 * Simple utilities classes dealing with common functions that are required in
 * loading to the Atlas DB.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoaderUtils {
  public static boolean waitWhilstIDFCompiles(
      final MAGETABInvestigation investigation, String handlerName, Log log) {
    // compile objects
    while (investigation.IDF.getStatus().ordinal() < Status.COMPILING.ordinal()
        && investigation.getStatus() != Status.FAILED) {
      synchronized (investigation.IDF) {
        try {
          investigation.IDF.wait(1000);
          log.debug(handlerName + " polling for status");
        }
        catch (InterruptedException e) {
          // ignore this
        }
      }
    }

    // exited the loop, check whether this is due to fail or complete
    return investigation.getStatus() != Status.FAILED;
  }

  public static boolean waitWhilstSDRFCompiles(
      final MAGETABInvestigation investigation, String handlerName, Log log) {
    // compile objects
    while (investigation.SDRF.getStatus().ordinal() < Status.COMPILING.ordinal()
        && investigation.getStatus() != Status.FAILED) {
      synchronized (investigation.SDRF) {
        try {
          investigation.SDRF.wait();
          log.debug(handlerName + " polling for status");
        }
        catch (InterruptedException e) {
          // ignore this
        }
      }
    }

    // exited the loop, check whether this is due to fail or complete
    return investigation.getStatus() != Status.FAILED;
  }

  public static Experiment waitForExperiment(
      String accession,
      final MAGETABInvestigation investigation,
      String handlerName,
      Log log) throws LookupException {
    // retrieve object bag
    AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
        .retrieveAtlasLoadCache(investigation);

    // check the identifier is not null
    if (accession == null) {
      throw new LookupException(
          "Cannot lookup an object using a null accession");
    }

    log.debug(handlerName + " doing lookup for experiment " + accession);
    log.debug("Thread [" + Thread.currentThread().getName() +
        "] polling for dependent object");
    // fetch from the bag
    while (cache.fetchExperiment(accession) == null
        && investigation.getStatus() != Status.COMPLETE) {
      // object isn't in the bag yet, so wait
      synchronized (investigation) {
        try {
          log.debug("Thread [" + Thread.currentThread().getName() +
              "] waiting, no result yet");
          // wait for new objects to be available
          investigation.wait();
          log.debug(
              "Thread [" + Thread.currentThread().getName() + "] resumed");
        }
        catch (InterruptedException e) {
          if (investigation.getStatus() == Status.FAILED) {
            log.warn(
                handlerName + " was interrupted by a failure elsewhere " +
                    "whilst waiting for experiment " + accession +
                    " and is terminating");
            throw new LookupException(
                "Interrupted by a fail whilst waiting " +
                    "for experiment " + accession);
          }
          else {
            // interrupted but no fail, so safe to continue
          }
        }
      }
    }
    log.debug("Thread [" + Thread.currentThread().getName() +
        "] resumed after dependent object obtained");
    return cache.fetchExperiment(accession);
  }

  public static Assay waitForAssay(
      String accession,
      final MAGETABInvestigation investigation,
      String handlerName,
      Log log) throws LookupException {
    // retrieve object bag
    AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
        .retrieveAtlasLoadCache(investigation);

    // check the identifier is not null
    if (accession == null) {
      throw new LookupException(
          "Cannot lookup an object using a null accession");
    }

    log.debug(handlerName + " doing lookup for assay " + accession + " in " +
        cache.toString());
    log.debug("Thread [" + Thread.currentThread().getName() +
        "] polling for assay");
    // fetch from the bag
    while (cache.fetchAssay(accession) == null
        && investigation.getStatus() != Status.COMPLETE) {
      // object isn't in the bag yet, so wait
      synchronized (investigation) {
        try {
          log.debug("Thread [" + Thread.currentThread().getName() +
              "] waiting, no result yet");
          // wait for new objects to be available
          investigation.wait();
          log.debug(
              "Thread [" + Thread.currentThread().getName() + "] resumed");
        }
        catch (InterruptedException e) {
          if (investigation.getStatus() == Status.FAILED) {
            log.warn(
                handlerName + " was interrupted by a failure elsewhere " +
                    "whilst waiting for assay " + accession +
                    " and is terminating");
            throw new LookupException(
                "Interrupted by a fail whilst waiting " +
                    "for assay " + accession);
          }
          else {
            // interrupted but no fail, so safe to continue
          }
        }
      }
    }
    log.debug("Thread [" + Thread.currentThread().getName() +
        "] resumed after assay obtained");
    return cache.fetchAssay(accession);
  }

  public static Sample waitForSample(
      String accession,
      final MAGETABInvestigation investigation,
      String handlerName,
      Log log) throws LookupException {
    // retrieve object bag
    AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
        .retrieveAtlasLoadCache(investigation);

    // check the identifier is not null
    if (accession == null) {
      throw new LookupException(
          "Cannot lookup an object using a null accession");
    }

    log.debug(handlerName + " doing lookup for sample " + accession);
    log.debug("Thread [" + Thread.currentThread().getName() +
        "] polling for sample");
    // fetch from the bag
    while (cache.fetchSample(accession) == null
        && investigation.getStatus() != Status.COMPLETE) {
      // object isn't in the bag yet, so wait
      synchronized (investigation) {
        try {
          log.debug("Thread [" + Thread.currentThread().getName() +
              "] waiting, no result yet");
          // wait for new objects to be available
          investigation.wait();
          log.debug(
              "Thread [" + Thread.currentThread().getName() + "] resumed");
        }
        catch (InterruptedException e) {
          if (investigation.getStatus() == Status.FAILED) {
            log.warn(
                handlerName + " was interrupted by a failure elsewhere " +
                    "whilst waiting for sample " + accession +
                    " and is terminating");
            throw new LookupException(
                "Interrupted by a fail whilst waiting " +
                    "for sample " + accession);
          }
          else {
            // interrupted but no fail, so safe to continue
          }
        }
      }
    }
    log.debug("Thread [" + Thread.currentThread().getName() +
        "] resumed after sample obtained");
    return cache.fetchSample(accession);
  }

  public static String getNodeAccession(MAGETABInvestigation investigation,
                                        SDRFNode node) {
    String accession;
    if (investigation.accession != null) {
      accession = investigation.accession + "::" +
          node.getNodeType() + "::" +
          node.getNodeName();
    }
    else {
      accession = "UNKNOWN::" + node.getNodeType() + "::" + node.getNodeName();
    }
    return accession;
  }
}
