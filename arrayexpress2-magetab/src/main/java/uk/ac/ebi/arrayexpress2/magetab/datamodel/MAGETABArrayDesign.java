package uk.ac.ebi.arrayexpress2.magetab.datamodel;

import uk.ac.ebi.arrayexpress2.magetab.handler.visitor.LocationTracker;
import uk.ac.ebi.arrayexpress2.magetab.lang.AbstractStatifiable;
import uk.ac.ebi.arrayexpress2.magetab.lang.Progressible;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

/**
 * An object that models a MAGE-TAB representation of an array design, with a
 * single ADF element that corresponds to the MAGE-TAB specification.  This
 * class does not attempt to model array designs directly, but simply provides
 * placeholders for structuring the text from a MAGE-TAB ADF file in a way that
 * conforms to the MAGE-TAB specification.
 * <p/>
 * Fields are public and classes are nested - so for instance you can access
 * <code>MAGETABArrayDesign.ADF.investigationTitle</code> directly.  All fields
 * are volatile, and as such this class should be thread safe and usable in
 * multithreaded environments.  Care should nevertheless still be taken as this
 * object is obviously highly mutable and cannot be easily locked.
 *
 * @author Tony Burdett
 * @date 04-Feb-2010
 * @see uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABArrayParser
 */
public class MAGETABArrayDesign extends AbstractStatifiable
    implements Progressible {
  public volatile String accession;

  public final ADF ADF;

  private final LocationTracker locationTracker;


  public MAGETABArrayDesign() {
    ADF = new ADF();

    locationTracker = new LocationTracker();
  }

  /**
   * Returns a {@link uk.ac.ebi.arrayexpress2.magetab.handler.visitor.LocationTracker}
   * utility, that can be used for recovering the location of ADF tags in the
   * original source document.
   *
   * @return the location tracker object to recover mappings to locations in the
   *         source document
   */
  public LocationTracker getLocationTracker() {
    return this.locationTracker;
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
    return ADF.getProgress();
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
    // is the ADF status is lower than our current status
    if (this.ADF.ranksBelow(status)) {
      // if so, revise downwards
      status = this.ADF.getStatus();
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

    sb.append(" ADF: \n\n");

    sb.append(this.ADF.toString());
    sb.append("\n\n");

    return sb.toString();
  }
}
