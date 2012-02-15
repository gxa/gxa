/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.*;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
@Entity
@DiscriminatorValue("biomart")
@SecondaryTable(name = "A2_BIOMART_ANNSRC",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "annotationsrcid"))
public class BioMartAnnotationSource extends AnnotationSource {

    @JoinColumn(table = "A2_BIOMART_ANNSRC")
    @ManyToOne()
    private Organism organism = null;

    /**
     * e.g. "hsapiens_gene_ensembl", "spombe_eg_gene"
     */
    @Column(table = "A2_BIOMART_ANNSRC", name = "biomartorganismname")
    private String datasetName = null;

    /**
     * Value of property "database" in BioMart registry, version number is removed.
     * e.g. "metazoa", "fungal"
     */
    @Column(table = "A2_BIOMART_ANNSRC", name = "databaseName")
    private String databaseName = null;

    @Column(table = "A2_BIOMART_ANNSRC")
    private String mySqlDbName = null;

    @Column(table = "A2_BIOMART_ANNSRC")
    private String mySqlDbUrl = null;

    BioMartAnnotationSource() {
        /*used by hibernate only*/
    }

    public BioMartAnnotationSource(Software software, Organism organism) {
        super(software, createName(software, organism));
        this.organism = organism;
    }

    public Organism getOrganism() {
        return organism;
    }

    public BioEntityType getBioEntityType(final String name) {
        for (BioEntityType type : getTypes()) {
            if (type.getName().equals(name))
                return type;
        }
        return null;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getMySqlDbName() {
        return mySqlDbName;
    }

    public void setMySqlDbName(String mySqlDbName) {
        this.mySqlDbName = mySqlDbName;
    }

    public String getMySqlDbUrl() {
        return mySqlDbUrl;
    }

    public void setMySqlDbUrl(String mySqlDbUrl) {
        this.mySqlDbUrl = mySqlDbUrl;
    }

    private static String createName(Software software, Organism organism) {
        return new StringBuilder()
                .append(organism.getName())
                .append(" / ")
                .append(software.getFullName())
                .toString();
    }

    /////////////////////////
    //  Helper methods
    ////////////////////////
    public BioMartAnnotationSource createCopyForNewSoftware(Software newSoftware) {
        BioMartAnnotationSource result = new BioMartAnnotationSource(newSoftware, this.organism);
        updateProperties(result);
        result.setDatasetName(this.datasetName);
        result.setDatabaseName(this.databaseName);
        result.setMySqlDbName(this.mySqlDbName);
        result.setMySqlDbUrl(this.mySqlDbUrl);
        return result;
    }

    @Override
    public String toString() {
        return "BioMartAnnotationSource{" + '\'' +
                super.toString() + '\'' +
                "url='" + getUrl() + '\'' +
                ", datasetName='" + datasetName + '\'' +
                ", externalBioEntityProperties=" + getExternalBioEntityProperties() +
                "} ";
    }
}
