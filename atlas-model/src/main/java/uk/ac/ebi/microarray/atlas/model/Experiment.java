package uk.ac.ebi.microarray.atlas.model;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:31:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class Experiment {
  private String accession;
  private String description;
  private String performer;
  private String lab;
  private String experimentID;

  public String getAccession() {
    return accession;
  }

  public void setAccession(String accession) {
    this.accession = accession;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPerformer() {
    return performer;
  }

  public void setPerformer(String performer) {
    this.performer = performer;
  }

  public String getLab() {
    return lab;
  }

  public void setLab(String lab) {
    this.lab = lab;
  }

  public String getExperimentID() {
    return experimentID;
  }

  public void setExperimentID(String experimentID) {
    this.experimentID = experimentID;
  }

  @Override
  public String toString() {
    return "Experiment{" +
        "accession='" + accession + '\'' +
        ", description='" + description + '\'' +
        ", performer='" + performer + '\'' +
        ", lab='" + lab + '\'' +
        '}';
  }
}
