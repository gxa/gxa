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
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa;

import java.util.*;

public interface Experiment {
    String getAccession();
    long getId();

    String getDescription();
    void setDescription(String description);
    String getAbstract();
    void setAbstract(String articleAbstract);
    String getLab();
    void setLab(String lab);
    String getPerformer();
    void setPerformer(String performer);

    Date getLoadDate();
    void setLoadDate(Date loadDate);
    Date getReleaseDate();
    void setReleaseDate(Date loadDate);

    Long getPubmedId();
    public void setPubmedId(Long pubmedId);
    public void setPubmedIdString(String pubmedIdString);

    List<Asset> getAssets();
    void addAssets(List<Asset> assets);

    //Collection<Assay> getAssays();
    //Collection<Sample> getSamples();
}
