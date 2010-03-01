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
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:30:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionValue {
  private int designElementID;
  private int assayID;
  private String designElementAccession;
  private Float value;

  public int getDesignElementID() {
    return designElementID;
  }

  public void setDesignElementID(int designElementID) {
    this.designElementID = designElementID;
  }

  public int getAssayID() {
    return assayID;
  }

  public void setAssayID(int assayID) {
    this.assayID = assayID;
  }

  public String getDesignElementAccession() {
    return designElementAccession;
  }

  public void setDesignElementAccession(String designElementAccession) {
    this.designElementAccession = designElementAccession;
  }

  public Float getValue() {
    return value;
  }

  public void setValue(Float value) {
    this.value = value;
  }

  public String toString() {
    return "ExpressionValue{" +
        "designElementAccession='" + designElementAccession + '\'' +
        ", value=" + value +
        '}';
  }
}
