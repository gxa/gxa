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

import uk.ac.ebi.gxa.Temporary;

import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class Experiment {
    private final String accession;
    private final long id;

    private String description;
    private String articleAbstract;
    private String performer;
    private String lab;

    private Date loadDate;
    private Date releaseDate;
    private Long pubmedId;

    private List<Asset> assets;
    private List<Assay> assays;
    private List<Sample> samples;

    private boolean isprivate;
    private boolean curated;

    private Map<String, Object> userData;

    @Deprecated
    @Temporary
    public Experiment(long id, String accession) {
        this.accession = accession;
        this.id = id;
    }

    public String getAccession() {
        return accession;
    }

    public long getId() {
        return id;
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

    public Long getPubmedId() {
        return pubmedId;
    }

    public void setPubmedIdString(String pubmedIdString) {
        if (pubmedIdString != null) {
            try {
                final long pubmedId = Long.parseLong(pubmedIdString);
                setPubmedId(pubmedId);
            } catch (NumberFormatException e) {
                throw createUnexpected("Couldn't parse " + pubmedIdString + " as long", e);
            }
        }
    }

    public void setPubmedId(Long pubmedId) {
        this.pubmedId = pubmedId;
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
        return Collections.unmodifiableList(samples);
    }

    public void setSamples(List<Sample> samples) {
        this.samples = samples;
    }

    public List<String> getSpecies() {
        ArrayList<String> species = new ArrayList<String>();
        for (Assay assay : assays) {
            for (Sample sample : assay.getSamples()) {
                species.add(sample.getOrganism().getName());
            }
        }
        return species;
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

    public void setUserData(String key, Object value) {
        if (userData == null) {
            userData = new HashMap<String, Object>();
        }
        userData.put(key, value);
    }

    public boolean getBooleanUserData(String key, boolean defaultValue) {
        if (userData == null) {
            return defaultValue;
        }
        final Object value = userData.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
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
