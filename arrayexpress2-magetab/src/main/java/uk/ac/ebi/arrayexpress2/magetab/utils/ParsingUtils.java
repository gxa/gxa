package uk.ac.ebi.arrayexpress2.magetab.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.ADF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.IDF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

/**
 * A class containing utility methods for monitoring the status of MAGE-TAB
 * parsing.  This contains methods that block until IDF, SDRF and SDRF graph
 * objects achieve some given criteria.  You should use these methods with
 * caution, as misusing them will likely result in applications that deadlock.
 *
 * @author Tony Burdett
 * @date 04-Feb-2010
 */
public class ParsingUtils {
  private static Log log = LogFactory.getLog(ParsingUtils.class);

  /**
   * This method will block until the IDF has achieved the specified parsing
   * status, or until parsing fails.
   * <p/>
   * It will often be acceptable to use this method inside {@link
   * uk.ac.ebi.arrayexpress2.magetab.handler.idf.IDFHandler} implementations, as
   * long as the current handler has already achieved the state you wish to wait
   * for.  If not, this will obviously result in a deadlock.  If this is the
   * case, using this method inside a handler is normally permissible as the
   * IDFParser creates a limited number of threads in parallel, which should all
   * be executing at any one time, meaning the potential for deadlock is
   * limited.
   *
   * @param idf    the IDF to wait for
   * @param status the status the IDF must have obtained before this method
   *               resumes
   * @return true when the IDF has achieved the given status, or false if it
   *         failed
   */
  public static boolean waitUntilIDFAchievesStatus(final IDF idf,
                                                   Status status) {
    // compile objects
    while (idf.getStatus().ordinal() < status.ordinal()
        && idf.getStatus() != Status.FAILED) {
      synchronized (idf) {
        try {
          log.trace("Waiting for IDF to achieve " + status + " status");
          idf.wait(1000);
        }
        catch (InterruptedException e) {
          // ignore this
        }
      }
    }

    // exited the loop, check whether this is due to fail or complete
    return idf.getStatus() != Status.FAILED;
  }

  /**
   * This method will block until the IDF has been completely read into memory,
   * releasing prior to any writing or validation events.  This is a convenience
   * method equivalent to calling <code>waitUntilIDFAchievesStatus(idf,
   * Status.COMPILING)</code>
   *
   * @param idf the IDF to wait for
   * @return true when the IDF has finished reading, or false if it failed
   */
  public static boolean waitForIDFToParse(final IDF idf) {
    return waitUntilIDFAchievesStatus(idf, Status.COMPILING);
  }

  /**
   * This method will block until the IDF has been completely parsed.  This is a
   * convenience method equivalent to calling <code>waitUntilIDFAchievesStatus(idf,
   * Status.COMPLETE)</code>
   *
   * @param idf the IDF to wait for
   * @return true when the IDF has finished reading, or false if it failed
   */
  public static boolean waitForIDF(final IDF idf) {
    return waitUntilIDFAchievesStatus(idf, Status.COMPLETE);
  }

  /**
   * This method will block until the SDRF has achieved the specified parsing
   * status, or until parsing fails.
   * <p/>
   * Unlike the IDF equivalent, it will normally never be acceptable to use this
   * method inside {@link uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler}
   * implementations.  SDRF parsing procedes from left to right, and due to the
   * number of possible nodes in the SDRF graph it is highly unlikely that one
   * handler can block until all others have achieved a given status: this will
   * almost always result in a deadlock situation arising.  You should only ever
   * call this method from some monitor thread not involved in parsing of the
   * SDRF.
   *
   * @param sdrf   the SDRF to wait for
   * @param status the status the SDRF must have obtained before this method
   *               resumes
   * @return true when the SDRF has achieved the given status, or false if it
   *         failed
   */
  public static boolean waitUntilSDRFAchievesStatus(final SDRF sdrf,
                                                    Status status) {
    // compile objects
    while (sdrf.getStatus().ordinal() < status.ordinal()
        && sdrf.getStatus() != Status.FAILED) {
      synchronized (sdrf) {
        try {
          log.trace("Waiting for IDF to achieve " + status + " status");
          sdrf.wait(1000);
        }
        catch (InterruptedException e) {
          // ignore this
        }
      }
    }

    // exited the loop, check whether this is due to fail or complete
    return sdrf.getStatus() != Status.FAILED;
  }

  /**
   * This method will block until the SDRF has been completely read into memory,
   * releasing prior to any writing or validation events.  This is a convenience
   * method equivalent to calling <code>waitUntilSDRFAchievesStatus(sdrf,
   * Status.COMPILING)</code>
   *
   * @param sdrf the SDRF to wait for
   * @return true when the SDRF has finished reading, or false if it failed
   */
  public static boolean waitForSDRFToParse(final SDRF sdrf) {
    return waitUntilSDRFAchievesStatus(sdrf, Status.COMPILING);
  }

  /**
   * This method will block until the SDRF has been completely parsed.  This is
   * a convenience method equivalent to calling <code>waitUntilSDRFAchievesStatus(sdrf,
   * Status.COMPLETE)</code>
   *
   * @param sdrf the SDRF to wait for
   * @return true when the SDRF has finished reading, or false if it failed
   */
  public static boolean waitForSDRF(final SDRF sdrf) {
    return waitUntilSDRFAchievesStatus(sdrf, Status.COMPLETE);
  }

  /**
   * This method will block until a node with the supplied SDRF node name and
   * type appears in the SDRF graph.  Note that you should use this method
   * inside handler implementations with extreme caution - if you wait for a
   * node downstream of the one being handled, you will create a deadlock.
   *
   * @param nodeName the name of the SDRF node to wait for
   * @param nodeType the type of the SDRF node to wait for
   * @param sdrf     the SDRF graph we're waiting on
   * @return the sdrf node, once it appears.
   * @throws InterruptedException if the current thread is interrupted whilst
   *                              blocking on the SDRF object supplied
   */
  public static SDRFNode waitForSDRFNode(String nodeName,
                                         String nodeType,
                                         final SDRF sdrf)
      throws InterruptedException {
    if (nodeName == null) {
      throw new IllegalArgumentException(
          "Cannot lookup an object using a null nodeName");
    }

    log.debug("Doing lookup for " + nodeType + " " + nodeName);
    log.trace("Thread [" + Thread.currentThread().getName() + "] " +
        "polling for dependent object");
    // fetch from the graph
    while (sdrf.lookupNode(nodeName, nodeType) == null &&
        sdrf.getStatus().ordinal() < Status.COMPILING.ordinal() &&
        sdrf.getStatus() != Status.FAILED) {
      // object isn't in the bag yet, so wait
      synchronized (sdrf) {
        log.trace("Thread [" + Thread.currentThread().getName() +
            "] waiting, no result yet");
        // wait for new objects to be available
        sdrf.wait(1000);
        log.trace(
            "Thread [" + Thread.currentThread().getName() + "] resumed");
      }
    }
    log.trace("Thread [" + Thread.currentThread().getName() + "] " +
        "finished waiting after dependent object obtained");
    // if status got to compiling, all nodes have been read
    // and this might return null
    return sdrf.lookupNode(nodeName, nodeType);
  }

  /**
   * This method will block until the ADF has achieved the specified parsing
   * status, or until parsing fails.
   * <p/>
   * It will often be acceptable to use this method inside {@link
   * uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFHandler} implementations, as
   * long as the current handler has already achieved the state you wish to wait
   * for.  If not, this will obviously result in a deadlock.  If this is the
   * case, using this method inside a handler is normally permissible as the
   * ADFParser creates a limited number of threads in parallel, which should all
   * be executing at any one time, meaning the potential for deadlock is
   * limited.
   *
   * @param adf    the ADF to wait for
   * @param status the status the ADF must have obtained before this method
   *               resumes
   * @return true when the ADF has achieved the given status, or false if it
   *         failed
   */
  public static boolean waitUntilADFAchievesStatus(final ADF adf,
                                                   Status status) {
    // compile objects
    while (adf.getStatus().ordinal() < status.ordinal()
        && adf.getStatus() != Status.FAILED) {
      synchronized (adf) {
        try {
          log.trace("Waiting for ADF to achieve " + status + " status");
          adf.wait(1000);
        }
        catch (InterruptedException e) {
          // ignore this
        }
      }
    }

    // exited the loop, check whether this is due to fail or complete
    return adf.getStatus() != Status.FAILED;
  }

  /**
   * This method will block until the ADF has been completely read into memory,
   * releasing prior to any writing or validation events.  This is a convenience
   * method equivalent to calling <code>waitUntilADFAchievesStatus(adf,
   * Status.COMPILING)</code>
   *
   * @param adf the ADF to wait for
   * @return true when the ADF has finished reading, or false if it failed
   */
  public static boolean waitForADFToParse(final ADF adf) {
    return waitUntilADFAchievesStatus(adf, Status.COMPILING);
  }

  /**
   * This method will block until the ADF has been completely parsed.  This is a
   * convenience method equivalent to calling <code>waitUntilADFAchievesStatus(adf,
   * Status.COMPLETE)</code>
   *
   * @param adf the ADF to wait for
   * @return true when the ADF has finished reading, or false if it failed
   */
  public static boolean waitForADF(final ADF adf) {
    return waitUntilADFAchievesStatus(adf, Status.COMPLETE);
  }
}
