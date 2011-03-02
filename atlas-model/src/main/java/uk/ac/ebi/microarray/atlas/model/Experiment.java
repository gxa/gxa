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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Asset asset = (Asset) o;

            if (description != null ? !description.equals(asset.description) : asset.description != null) return false;
            if (fileName != null ? !fileName.equals(asset.fileName) : asset.fileName != null) return false;
            if (name != null ? !name.equals(asset.name) : asset.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            return result;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Experiment that = (Experiment) o;

        if (experimentID != that.experimentID) return false;
        if (accession != null ? !accession.equals(that.accession) : that.accession != null) return false;
        if (articleAbstract != null ? !articleAbstract.equals(that.articleAbstract) : that.articleAbstract != null)
            return false;
        if (assets != null ? !assets.equals(that.assets) : that.assets != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (lab != null ? !lab.equals(that.lab) : that.lab != null) return false;
        if (loadDate != null ? !loadDate.equals(that.loadDate) : that.loadDate != null) return false;
        if (performer != null ? !performer.equals(that.performer) : that.performer != null) return false;
        if (pubmedID != null ? !pubmedID.equals(that.pubmedID) : that.pubmedID != null) return false;
        if (releaseDate != null ? !releaseDate.equals(that.releaseDate) : that.releaseDate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = accession != null ? accession.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (performer != null ? performer.hashCode() : 0);
        result = 31 * result + (lab != null ? lab.hashCode() : 0);
        result = 31 * result + (loadDate != null ? loadDate.hashCode() : 0);
        result = 31 * result + (releaseDate != null ? releaseDate.hashCode() : 0);
        result = 31 * result + (pubmedID != null ? pubmedID.hashCode() : 0);
        result = 31 * result + (int) (experimentID ^ (experimentID >>> 32));
        result = 31 * result + (assets != null ? assets.hashCode() : 0);
        result = 31 * result + (articleAbstract != null ? articleAbstract.hashCode() : 0);
        return result;
    }
}
