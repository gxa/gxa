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

import com.google.common.base.Predicate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import uk.ac.ebi.gxa.Temporary;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static uk.ac.ebi.gxa.utils.DateUtil.copyOf;

@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Experiment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "experimentSeq")
    @SequenceGenerator(name = "experimentSeq", sequenceName = "A2_EXPERIMENT_SEQ")
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
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<Asset> assets = new ArrayList<Asset>();

    @OneToMany(targetEntity = Assay.class, mappedBy = "experiment")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<Assay> assays = new ArrayList<Assay>();

    @OneToMany(targetEntity = Sample.class, mappedBy = "experiment")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
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

    public Experiment(String accession) {
        if (accession == null)
            throw new IllegalArgumentException("Cannot add experiment with null accession!");

        this.accession = accession;
    }

    public String getAccession() {
        return accession;
    }

    public Long getId() {
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

    public boolean isRNASeq() {
        // TODO: see ticket #2706
        for (Assay assay : assays) {
            ArrayDesign design = assay.getArrayDesign();
            String designType = design == null ? "" : design.getType();
            if (designType != null && designType.contains("virtual")) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getExperimentFactors() {
        Set<String> result = newTreeSet();
        for (Assay assay : assays) {
            result.addAll(assay.getPropertyNames());
        }
        return result;
    }

    public void addAssay(Assay assay) {
        final Assay oldAssay = getAssay(assay.getAccession());
        if (oldAssay != null && oldAssay != assay) {
            throw new IllegalArgumentException("Attempting to store a new assay with a non-unique accession");
        }
        assays.add(assay);
        assay.setExperiment(this);
    }

    public Sample getSample(String accession) {
        for (Sample sample : samples) {
            if (sample.getAccession().equals(accession))
                return sample;
        }
        return null;
    }

    public void addSample(Sample sample) {
        final Sample oldSample = getSample(sample.getAccession());
        if (oldSample != null && oldSample != sample) {
            throw new IllegalArgumentException("Attempting to store a new sample with a non-unique accession");
        }
        sample.setExperiment(this);
        samples.add(sample);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Experiment that = (Experiment) o;

        return !(accession != null ? !accession.equals(that.accession) : that.accession != null) &&
                !(experimentid != null ? !experimentid.equals(that.experimentid) : that.experimentid != null);

    }

    @Override
    public int hashCode() {
        int result = experimentid != null ? experimentid.hashCode() : 0;
        result = 31 * result + (accession != null ? accession.hashCode() : 0);
        return result;
    }

    public Collection<ArrayDesign> getArrayDesigns() {
        Set<ArrayDesign> result = newHashSet();
        for (Assay assay : assays) {
            result.add(assay.getArrayDesign());
        }
        return result;
    }

    public Collection<Assay> getAssaysForDesign(final ArrayDesign arrayDesign) {
        return filter(getAssays(), new Predicate<Assay>() {
            @Override
            public boolean apply(@Nullable Assay input) {
                return input != null && input.getArrayDesign().equals(arrayDesign);
            }
        });
    }
}
