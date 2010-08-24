package uk.ac.ebi.arrayexpress2.magetab.datamodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.lang.AbstractProgressibleStatifiableFromTasks;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

import java.net.URL;
import java.util.*;

/**
 * A datastructure that simply models IDF in the MAGE-TAB spec.  All fields are
 * public, but there are some accessors provided to determine the current status
 * of the IDF.
 *
 * @author Tony Burdett
 * @date 12-Feb-2009
 * @see SDRF
 * @see ADF
 */
public class IDF extends AbstractProgressibleStatifiableFromTasks {
  // fields that represent the data structure of MAGE-TAB pretty precisely
  public volatile String magetabVersion = "1.1";

  public volatile String investigationTitle = "";

  public volatile List<String> experimentalDesign = new ArrayList<String>();
  public volatile List<String> experimentalDesignTermSourceREF =
      new ArrayList<String>();
  public volatile List<String> experimentalDesignTermAccession =
      new ArrayList<String>();

  public volatile List<String> experimentalFactorName = new ArrayList<String>();
  public volatile List<String> experimentalFactorType = new ArrayList<String>();
  public volatile List<String> experimentalFactorTermSourceREF =
      new ArrayList<String>();
  public volatile List<String> experimentalFactorTermAccession =
      new ArrayList<String>();

  public volatile List<String> personLastName = new ArrayList<String>();
  public volatile List<String> personFirstName = new ArrayList<String>();
  public volatile List<String> personMidInitials = new ArrayList<String>();
  public volatile List<String> personEmail = new ArrayList<String>();
  public volatile List<String> personPhone = new ArrayList<String>();
  public volatile List<String> personFax = new ArrayList<String>();
  public volatile List<String> personAddress = new ArrayList<String>();
  public volatile List<String> personAffiliation = new ArrayList<String>();
  public volatile List<String> personRoles = new ArrayList<String>();
  public volatile List<String> personRolesTermSourceREF =
      new ArrayList<String>();
  public volatile List<String> personRolesTermAccession =
      new ArrayList<String>();

  public volatile List<String> qualityControlType =
      new ArrayList<String>();
  public volatile List<String> qualityControlTermSourceREF =
      new ArrayList<String>();
  public volatile List<String> qualityControlTermAccession =
      new ArrayList<String>();
  public volatile List<String> replicateType =
      new ArrayList<String>();
  public volatile List<String> replicateTermSourceREF =
      new ArrayList<String>();
  public volatile List<String> replicateTermAccession =
      new ArrayList<String>();
  public volatile List<String> normalizationType =
      new ArrayList<String>();
  public volatile List<String> normalizationTermSourceREF =
      new ArrayList<String>();
  public volatile List<String> normalizationTermAccession =
      new ArrayList<String>();

  public volatile String dateOfExperiment = "";

  public volatile String publicReleaseDate = "";
  public volatile List<String> pubMedId = new ArrayList<String>();
  public volatile List<String> publicationDOI = new ArrayList<String>();
  public volatile List<String> publicationAuthorList = new ArrayList<String>();
  public volatile List<String> publicationTitle = new ArrayList<String>();
  public volatile List<String> publicationStatus = new ArrayList<String>();
  public volatile List<String> publicationStatusTermSourceREF =
      new ArrayList<String>();
  public volatile List<String> publicationStatusTermAccession =
      new ArrayList<String>();

  public volatile String experimentDescription = "";
  public volatile List<String> protocolName = new ArrayList<String>();
  public volatile List<String> protocolType = new ArrayList<String>();
  public volatile List<String> protocolTermSourceREF = new ArrayList<String>();
  public volatile List<String> protocolTermAccession = new ArrayList<String>();
  public volatile List<String> protocolDescription = new ArrayList<String>();
  public volatile List<String> protocolParameters = new ArrayList<String>();
  public volatile List<String> protocolHardware = new ArrayList<String>();
  public volatile List<String> protocolSoftware = new ArrayList<String>();
  public volatile List<String> protocolContact = new ArrayList<String>();

  public volatile List<String> sdrfFile = new ArrayList<String>();

  public volatile List<String> termSourceName = new ArrayList<String>();
  public volatile List<String> termSourceFile = new ArrayList<String>();
  public volatile List<String> termSourceVersion = new ArrayList<String>();

  private volatile Map<String, Set<String>> comments =
      new HashMap<String, Set<String>>();

//  private final MAGETABInvestigation investigation;
  private URL location;

  protected Log log = LogFactory.getLog(this.getClass());

  /**
   * Get the known location of this IDF, being the location of the file which
   * was parsed to generate it.  This may be used by handlers wishing to read
   * files declared with some relative location to the IDF.
   *
   * @return the location of the IDF file
   */
  public URL getLocation() {
    return location;
  }

  /**
   * Get the known location of this IDF, being the location of the file which
   * was parsed to generate it.  This may be used by handlers wishing to read
   * files declared with some relative location to the IDF.
   *
   * @param location the full URL of the IDF file represented by this IDF
   *                 object
   */
  public void setLocation(URL location) {
    this.location = location;
  }

  /**
   * Explicitly sets the last status of the IDF parsing operation.  You should
   * not normally use this, unless you have a reason to explicitly set the
   * status of IDF parsing to "FAILED".  Setting the status will override the
   * last known status, so that when the next handler updates no notifications
   * may occur, depending on whether this will result in a status update.
   *
   * @param nextStatus the status to set for this IDF
   */
  public void setStatus(Status nextStatus) {
    super.setStatus(nextStatus);
  }

  /**
   * Add a comment to the investigation, keyed by type.
   *
   * @param type    the type of the IDF comment
   * @param comment the value of the comment
   */
  public synchronized void addComment(String type, String comment) {
    if (!comments.containsKey(type)) {
      comments.put(type, new HashSet<String>());
    }
    comments.get(type).add(comment);
  }

  /**
   * Get the map of all current comments.
   *
   * @return the comments on this IDF
   */
  public synchronized Map<String, Set<String>> getComments() {
    return comments;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("MAGE-TAB Version\t").append(magetabVersion).append("\n\n");
    sb.append("Investigation Title\t").append(investigationTitle)
        .append("\n\n");
    sb.append("Experimental Design\t");
    for (String s : experimentalDesign) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Experimental Design Term Source REF\t");
    for (String s : experimentalDesignTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Experimental Design Term Accession Number\t");
    for (String s : experimentalDesignTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n\n");
    sb.append("Experimental Factor Name\t");
    for (String s : experimentalFactorName) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Experimental Factor Type\t");
    for (String s : experimentalFactorType) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Experimental Factor Term Source REF\t");
    for (String s : experimentalFactorTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Experimental Factor Term Accession Number\t");
    for (String s : experimentalFactorTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n\n");
    sb.append("Person Last Name\t");
    for (String s : personLastName) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person First Name\t");
    for (String s : personFirstName) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Mid Initials\t");
    for (String s : personMidInitials) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Email\t");
    for (String s : personEmail) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Phone\t");
    for (String s : personPhone) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Fax\t");
    for (String s : personFax) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Address\t");
    for (String s : personAddress) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Affiliation\t");
    for (String s : personAffiliation) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Roles\t");
    for (String s : personRoles) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Roles Term Source REF\t");
    for (String s : personRolesTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Person Roles Term Accession Number\t");
    for (String s : personRolesTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n\n");
    sb.append("Quality Control Type\t");
    for (String s : qualityControlType) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Quality Control Term Source REF\t");
    for (String s : qualityControlTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Quality Control Term Accession Number\t");
    for (String s : qualityControlTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Replicate Type\t");
    for (String s : replicateType) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Replicate Type Term Source REF\t");
    for (String s : replicateTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Replicate Type Term Accession NUmber\t");
    for (String s : replicateTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Normalization Type\t");
    for (String s : normalizationType) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Normalization Term Source REF\t");
    for (String s : normalizationTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Normalization Term Accession Number\t");
    for (String s : normalizationTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n\n");
    sb.append("Date of Experiment\t").append(dateOfExperiment);
    sb.append("\n");
    sb.append("Public Release Date\t").append(publicReleaseDate);
    sb.append("\n\n");
    sb.append("PubMed ID\t");
    for (String s : pubMedId) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Publication DOI\t");
    for (String s : publicationDOI) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Publication Author List\t");
    for (String s : publicationAuthorList) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Publication Title\t");
    for (String s : publicationTitle) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Publication Status\t");
    for (String s : publicationStatus) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Publication Status Term Source REF\t");
    for (String s : publicationStatusTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Publication Status Term Accession Number\t");
    for (String s : publicationStatusTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n\n");
    sb.append("Experiment Description\t").append(experimentDescription);
    sb.append("\n\n");
    sb.append("Protocol Name\t");
    for (String s : protocolName) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Type\t");
    for (String s : protocolType) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Term Source REF\t");
    for (String s : protocolTermSourceREF) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Term Accession Number\t");
    for (String s : protocolTermAccession) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Description\t");
    for (String s : protocolDescription) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Parameters\t");
    for (String s : protocolParameters) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Hardware\t");
    for (String s : protocolHardware) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Software\t");
    for (String s : protocolSoftware) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Protocol Contact\t");
    for (String s : protocolContact) {
      sb.append(s).append("\t");
    }
    sb.append("\n\n");
    sb.append("SDRF File\t");
    for (String s : sdrfFile) {
      sb.append(s).append("\t");
    }
    sb.append("\n\n");
    sb.append("Term Source Name\t");
    for (String s : termSourceName) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Term Source File\t");
    for (String s : termSourceFile) {
      sb.append(s).append("\t");
    }
    sb.append("\n");
    sb.append("Term Source Version\t");
    for (String s : termSourceVersion) {
      sb.append(s).append("\t");
    }
    sb.append("\n");

    return sb.toString();
  }
}
