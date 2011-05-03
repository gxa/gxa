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

package uk.ac.ebi.gxa.impl;

import uk.ac.ebi.gxa.Asset;
import uk.ac.ebi.gxa.Experiment;

import java.util.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class ExperimentImpl implements Experiment {
    private final ModelImpl model;

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

    private boolean isprivate;
    private boolean curated;

    private Map<String,Object> userData;

    // TODO: temporary constructor; must be deleted after refactoring
    protected ExperimentImpl(String accession, long id) {
        this(null, accession, id);
    }

    ExperimentImpl(ModelImpl model, String accession, long id) {
        this.model = model;
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

    private void initAssetsList() {
        if (assets == null) {
            assets = model.loadAssetsForExperiment(this);
        }
    }

    public void addAssets(List<Asset> assets) {
        initAssetsList();
        this.assets.addAll(assets);
    }

    // TODO: lazy collection
    public List<Asset> getAssets() {
        initAssetsList();
        return Collections.unmodifiableList(assets);
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
            userData = new HashMap<String,Object>();
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

    public void deleteFromStorage() {
        model.deleteExperiment(accession);
    }

    public void save() {
        model.writeExperiment(this);
    }

    /*
    //Collection<Assay> getAssays();
    //Collection<Sample> getSamples();
    */

    @Override
    public String toString() {
        return "Experiment{" +
                "accession='" + getAccession() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", performer='" + getPerformer() + '\'' +
                ", lab='" + getLab() + '\'' +
                '}';
    }
}
