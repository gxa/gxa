package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 25-Sep-2009
 */
public class OntologyMapping {
  private String experimentID;
  private String efName;
  private String efvName;
  private String[] ontologyTermAccessions;

  public String getExperimentID() {
    return experimentID;
  }

  public void setExperimentID(String experimentID) {
    this.experimentID = experimentID;
  }

  public String getEfName() {
    return efName;
  }

  public void setEfName(String efName) {
    this.efName = efName;
  }

  public String getEfvName() {
    return efvName;
  }

  public void setEfvName(String efvName) {
    this.efvName = efvName;
  }

  public String[] getOntologyTermAccessions() {
    return ontologyTermAccessions;
  }

  public void setOntologyTermAccessions(String... ontologyTermAccessions) {
    this.ontologyTermAccessions = ontologyTermAccessions;
  }
}
