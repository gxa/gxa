/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.model.biomart;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
@Entity
@DiscriminatorValue("biomart")
public class BioMartAnnotationSource extends AnnotationSource {
    /**
     * Location of biomart martservice, e.g.:
     * "http://www.ensembl.org/biomart/martservice?"
     * "http://plants.ensembl.org/biomart/martservice?"
     */

    @Column(name = "url")
    private String url;

    /**
     * e.g. "hsapiens_gene_ensembl", "spombe_eg_gene"
     */
    @Column(name = "biomartorganismname")
    private String datasetName;

    /**
     * Value of property "database" in BioMart registry, version number is removed.
     * e.g. "metazoa", "fungal"
     */
    @Column(name = "databaseName")
    private String databaseName;

    @OneToMany(targetEntity = BioMartProperty.class
            , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<BioMartProperty> bioMartProperties = new HashSet<BioMartProperty>();

    @OneToMany(targetEntity = BioMartArrayDesign.class
            , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<BioMartArrayDesign> bioMartArrayDesigns = newHashSet();

    private String mySqlDbName;

    private String mySqlDbUrl;

    BioMartAnnotationSource() {
    }

    public BioMartAnnotationSource(Software software, Organism organism) {
        super(software, organism);
    }

    public BioEntityType getBioEntityType(final String name) {
        for (BioEntityType type : types) {
            if (type.getName().equals(name))
                return type;
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<BioMartProperty> getBioMartProperties() {
        return Collections.unmodifiableSet(bioMartProperties);
    }

    public Set<String> getBioMartPropertyNames() {
        Set<String> answer = new HashSet<String>(bioMartProperties.size());
        for (BioMartProperty bioMartProperty : bioMartProperties) {
            answer.add(bioMartProperty.getName());
        }
        return answer;
    }

    public void addBioMartProperty(String biomartPropertyName, BioEntityProperty bioEntityProperty) {
        BioMartProperty bioMartProperty = new BioMartProperty(biomartPropertyName, bioEntityProperty, this);
        this.bioMartProperties.add(bioMartProperty);
    }

    public void addBioMartProperty(BioMartProperty bioMartProperty) {
        bioMartProperty.setAnnotationSrc(this);
        this.bioMartProperties.add(bioMartProperty);
    }

    public boolean removeBioMartProperty(BioMartProperty property) {
        return bioMartProperties.remove(property);
    }

    public Set<BioMartArrayDesign> getBioMartArrayDesigns() {
        return bioMartArrayDesigns;
    }

    public void addBioMartArrayDesign(BioMartArrayDesign bioMartArrayDesign) {
        bioMartArrayDesign.setAnnotationSrc(this);
        this.bioMartArrayDesigns.add(bioMartArrayDesign);
    }

    public boolean removeBioMartArrayDesign(BioMartArrayDesign bioMartArrayDesign) {
        return bioMartArrayDesigns.remove(bioMartArrayDesign);
    }

    public Set<String> getBioMartArrayDesignNames() {
        Set<String> answer = newHashSet();
        for (BioMartArrayDesign bioMartArrayDesign : bioMartArrayDesigns) {
            answer.add(bioMartArrayDesign.getName());
        }
        return answer;
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


    /////////////////////////
    //  Helper methods
    ////////////////////////
    public BioMartAnnotationSource createCopyForNewSoftware(Software newSoftware) {
        BioMartAnnotationSource result = new BioMartAnnotationSource(newSoftware, this.organism);
        result.setDatasetName(this.datasetName);
        result.setUrl(this.url);
        for (BioMartProperty bioMartProperty : bioMartProperties) {
            result.addBioMartProperty(bioMartProperty.getName(), bioMartProperty.getBioEntityProperty());
        }
        for (BioMartArrayDesign bioMartArrayDesign : bioMartArrayDesigns) {
            result.addBioMartArrayDesign(new BioMartArrayDesign(bioMartArrayDesign.getName(), bioMartArrayDesign.getArrayDesign(), result));
        }
        for (BioEntityType type : types) {
            result.addBioEntityType(type);
        }
        result.setDatabaseName(this.databaseName);
        result.setMySqlDbName(this.mySqlDbName);
        result.setMySqlDbUrl(this.mySqlDbUrl);

        return result;
    }

    @Override
    public String toString() {
        return "BioMartAnnotationSource{" + '\'' +
                super.toString() + '\'' +
                "url='" + url + '\'' +
                ", datasetName='" + datasetName + '\'' +
                ", bioMartProperties=" + bioMartProperties +
                "} ";
    }
}
