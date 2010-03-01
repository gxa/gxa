/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://ostolop.github.com/gxa/
 */

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
  private int experimentID;

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

  public int getExperimentID() {
    return experimentID;
  }

  public void setExperimentID(int experimentID) {
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
