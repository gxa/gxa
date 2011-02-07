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

package uk.ac.ebi.microarray.atlas.model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class Experiment {
    public static class Asset {
        private String name;
        private String fileName;
        private String description;

        public Asset(String name, String fileName, String description) {
            this.name = name;
            this.fileName = fileName;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getFileName() {
            return fileName;
        }

        public String getDescription() {
            return description;
        }
    }

    private String accession;
    private String description;
    private String performer;
    private String lab;
    private Date loadDate;
    private Date releaseDate;

    private String pubmedID;

    private long experimentID;
    private List<Asset> assets = new ArrayList<Asset>();
    private String articleAbstract;

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

    public long getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(long experimentID) {
        this.experimentID = experimentID;
    }

    public Date getLoadDate() {
        return loadDate;
    }

    public void setLoadDate(Date loadDate) {
        this.loadDate = loadDate;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPubmedID() {
        return pubmedID;
    }

    public void setPubmedID(String pubmedID) {
        this.pubmedID = pubmedID;
    }

    public void addAssets(List<Asset> assets) {
        this.assets.addAll(assets);
    }

    public List<Asset> getAssets() {
        return unmodifiableList(assets);
    }

    public String getArticleAbstract() {
        return articleAbstract;
    }

    public void setArticleAbstract(String articleAbstract) {
        this.articleAbstract = articleAbstract;
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
