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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;

/**
 * any local resource associated with experiment
 * for example, pictures from published articles
 */
@Entity
@Table(name = "A2_EXPERIMENTASSET")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assetSeq")
    @SequenceGenerator(name = "assetSeq", sequenceName = "A2_ASSET_SEQ", allocationSize = 1)
    private Long experimentassetid;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private Experiment experiment;
    private String name;
    private String fileName;
    private String description;

    Asset() {
    }

    public Asset(long id, Experiment experiment, String name, String fileName, String description) {
        this.experimentassetid = id;
        this.experiment = experiment;
        this.name = name;
        this.fileName = fileName;
        this.description = description;
    }

    public Long getId() {
        return experimentassetid;
    }

    public Experiment getExperiment() {
        return experiment;
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
    public String toString() {
        return name;
    }
}
