package uk.ac.ebi.arrayexpress2.magetab.datamodel;

import uk.ac.ebi.arrayexpress2.magetab.handler.visitor.LocationTracker;
import uk.ac.ebi.arrayexpress2.magetab.lang.AbstractStatifiable;
import uk.ac.ebi.arrayexpress2.magetab.lang.Progressible;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

/**
 * An object that models a MAGE-TAB document, with elements for each type of
 * structure within the MAGE-TAB specification.  Note that this does not attempt
 * to model MAGE objects, simply providing placeholders for structuring the text
 * from a MAGE-TAB file in a way that conforms to the MAGE-TAB specification.
 * <p/>
 * This class is an attempt to model the simple data structure, rather than a
 * class with complex interation methods.  As such, fields are public and
 * classes are nested - so for instance you can access <code>MAGETABInvestigation.IDF.investigationTitle</code>
 * directly.  All fields are volatile, and as such this class should be thread
 * safe and usable in multithreaded environments.  Care should nevertheless
 * still be taken as this object is obviously highly mutable and cannot be
 * easily locked.
 *
 * @author Tony Burdett
 * @date 20-Jan-2009
 * @see uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser
 */
public class MAGETABInvestigation extends AbstractStatifiable
    implements Progressible {
  public volatile String accession;
  public final IDF IDF;

  public final SDRF SDRF;

  private volatile int numberOfFiles;
  private final LocationTracker locationTracker;

  public MAGETABInvestigation() {
    IDF = new IDF();
    SDRF = new SDRF();

    locationTracker = new LocationTracker();
  }

  /**
   * Returns a {@link uk.ac.ebi.arrayexpress2.magetab.handler.visitor.LocationTracker}
   * utility, that can be used for recovering the location of IDF tags and SDRF
   * nodes in the original source document.
   *
   * @return the location tracker object to recover mappings to locations in the
   *         source document
   */
  public LocationTracker getLocationTracker() {
    return this.locationTracker;
  }

  /**
   * Sets the total number of files present for this MAGETABInvestigation.
   * Normally, this will be 2: one IDF and one SDRF file.  However, in some
   * cases the IDF and SDRF are merged, in other cases the SDRF is spread across
   * several files, or in other cases there is no SDRF at all.  Updating this
   * parameter ensures progress monitoring remains up-to-date.
   *
   * @param numberOfFiles the number of files this investigation is parsed from
   */
  public void setNumberOfFiles(int numberOfFiles) {
    this.numberOfFiles = numberOfFiles;
  }

  /**
   * Gets the total number of files present for this MAGETABInvestigation.
   * Normally, this will be 2: one IDF and one SDRF file.  However, in some
   * cases the IDF and SDRF are merged, in other cases the SDRF is spread across
   * several files, or in other cases there is no SDRF at all.  This parameter
   * is primarily used to ensure that the progress monitoring is kept as
   * accurate as possible.
   *
   * @return the total number of files this MAGETABInvestigation was parsed
   *         from
   */
  public int getNumberOfFiles() {
    return numberOfFiles;
  }

  /**
   * An integer represent the degree of completion of this investigation.  This
   * is intended to be used when parsing IDF and SDRF into memory - each
   * component represents exactly half of the total size of the document.  This
   * method will round off the actual progress to the nearest whole integer.
   *
   * @return the percentage progress
   */
  public synchronized int getProgress() {
    // get progress of IDF, SDRF
    int idfProgress = IDF.getProgress();
    int sdrfProgress = SDRF.getProgress();

    return (numberOfFiles == 0 ? 0
        : (idfProgress + sdrfProgress) / numberOfFiles);
  }

  /**
   * This does nothing - the progress for a MAGETABInvestigation is derived from
   * the progress of its constituent IDF and SDRF objects.
   *
   * @param increase the amount to increment the current progress by
   */
  public void increaseProgressBy(double increase) {
    // does nothing
  }

  /**
   * This does nothing - the status of a MAGETABInvestigation is taken from the
   * statuses of its constituent IDF and SDRF components
   *
   * @param nextStatus the status to set for this object
   */
  protected void setStatus(Status nextStatus) {
    // does nothing
  }

  /**
   * Returns the status of the MAGETABInvestigation parsing operation as a
   * whole.  This is effectively the same as the lowest status of the IDF and
   * SDRF currently configured.
   *
   * @return the status of the MAGE-TAB parse operation
   */
  public Status getStatus() {
    Status status = Status.COMPLETE;
    // is the IDF status is lower than our current status
    if (this.IDF.ranksBelow(status)) {
      // if so, revise downwards
      status = this.IDF.getStatus();
    }
    // is the SDRF status is lower than our current status
    if (this.SDRF.ranksBelow(status)) {
      // if so, revise downwards
      status = this.SDRF.getStatus();
    }

    return status;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("\n");
    if (accession == null) {
      sb.append("MAGETAB Document, no accession").append("\n");
      sb.append("==============================");
      sb.append("\n\n");
    }
    else {
      sb.append("MAGETAB Document ").append(accession).append("\n");
      sb.append("=================");
      for (int i = 0; i < accession.length(); i++) {
        sb.append("=");
      }
      sb.append("\n\n");
    }

    sb.append(" IDF: \n\n");

    sb.append(this.IDF.toString());
    sb.append("\n\n");

    sb.append(" SDRF: \n\n");

    sb.append(this.SDRF.toString());
    sb.append("\n\n");

    return sb.toString();
  }
}