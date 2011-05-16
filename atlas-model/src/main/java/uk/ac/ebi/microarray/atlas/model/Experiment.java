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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import uk.ac.ebi.gxa.Temporary;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.*;

import static uk.ac.ebi.gxa.utils.DateUtil.copyOf;

@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Experiment {
    @Id
    private Long experimentid;
    private String accession;

    private String description;

    @Column(name = "ABSTRACT")
    private String articleAbstract;
    private String performer;
    private String lab;

    private Date loadDate;
    private Date releaseDate;
    private String pmid;

    @OneToMany(targetEntity = Asset.class, mappedBy = "experiment")
    private List<Asset> assets = new ArrayList<Asset>();

    @OneToMany(targetEntity = Assay.class, mappedBy = "experiment")
    private List<Assay> assays = new ArrayList<Assay>();

    @OneToMany(targetEntity = Sample.class, mappedBy = "experiment")
    private List<Sample> samples = new ArrayList<Sample>();

    @Column(name = "PRIVATE")
    private boolean isprivate;

    private boolean curated;

    Experiment() {
    }

    @Deprecated
    @Temporary
    public Experiment(Long id, String accession) {
        this.accession = accession;
        this.experimentid = id;
    }

    public String getAccession() {
        return accession;
    }

    public long getId() {
        return experimentid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAbstract() {
        return articleAbstract;
    }

    public void setAbstract(String articleAbstract) {
        this.articleAbstract = articleAbstract;
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

    public Date getLoadDate() {
        return copyOf(loadDate);
    }

    public void setLoadDate(Date loadDate) {
        this.loadDate = copyOf(loadDate);
    }

    public Date getReleaseDate() {
        return copyOf(releaseDate);
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = copyOf(releaseDate);
    }

    public String getPubmedId() {
        return pmid;
    }

    public void setPubmedId(String pubmedId) {
        this.pmid = pubmedId;
    }

    public List<Asset> getAssets() {
        return Collections.unmodifiableList(assets);
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public List<Assay> getAssays() {
        return Collections.unmodifiableList(assays);
    }

    public void setAssays(List<Assay> assays) {
        this.assays = assays;
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public List<String> getSpecies() {
        Set<String> species = new HashSet<String>();
        for (Sample sample : samples) {
            species.add(sample.getOrganism().getName());
        }
        return new ArrayList<String>(species);
    }

    @Temporary
    public ArrayDesign getArrayDesign(String accession) {
        for (Assay assay : assays) {
            ArrayDesign arrayDesign = assay.getArrayDesign();
            if (arrayDesign.getAccession().equals(accession)) {
                return arrayDesign;
            }
        }
        return null;
    }

    public boolean isPrivate() {
        return isprivate;
    }

    public void setPrivate(boolean isprivate) {
        this.isprivate = isprivate;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }

    @Override
    public String toString() {
        return "Experiment{" +
                "accession='" + getAccession() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", performer='" + getPerformer() + '\'' +
                ", lab='" + getLab() + '\'' +
                '}';
    }

    public Assay getAssay(String accession) {
        for (Assay assay : assays) {
            if (assay.getAccession().equals(accession))
                return assay;
        }
        return null;
    }
}
