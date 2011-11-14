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

package uk.ac.ebi.gxa.annotator.model;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import uk.ac.ebi.gxa.annotator.loader.AnnotationSourceConnection;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static uk.ac.ebi.gxa.utils.DateUtil.copyOf;

/**
 * User: nsklyar
 * Date: 09/05/2011
 */
@Entity
@Table(name = "A2_ANNOTATIONSRC")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "annsrctype",
        discriminatorType = DiscriminatorType.STRING
)
// @MappedSuperclass
public abstract class AnnotationSource implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annSrcSeq")
    @SequenceGenerator(name = "annSrcSeq", sequenceName = "A2_ANNOTATIONSRC_SEQ", allocationSize = 1)
    protected Long annotationSrcId;

    /**
     * Location of biomart martservice, e.g.:
     * "http://www.ensembl.org/biomart/martservice?"
     * "http://plants.ensembl.org/biomart/martservice?"
     */

    @Column(name = "url")
    protected String url;

    @ManyToOne()
    protected Software software;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinTable(name = "A2_ANNSRC_BIOENTITYTYPE",
            joinColumns = @JoinColumn(name = "annotationsrcid", referencedColumnName = "annotationsrcid"),
            inverseJoinColumns = @JoinColumn(name = "bioentitytypeid", referencedColumnName = "bioentitytypeid"))
    protected Set<BioEntityType> types = new HashSet<BioEntityType>();

    @Temporal(TemporalType.DATE)
    protected Date loadDate;

    @org.hibernate.annotations.Type(type = "true_false")
    private boolean isApplied = false;
    @OneToMany(targetEntity = AnnotatedBioEntityProperty.class
            , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    protected Set<AnnotatedBioEntityProperty> annotatedBioEntityProperties = new HashSet<AnnotatedBioEntityProperty>();
    @OneToMany(targetEntity = AnnotatedArrayDesign.class
            , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    protected Set<AnnotatedArrayDesign> annotatedArrayDesigns = newHashSet();

    protected AnnotationSource() {
    }

    public AnnotationSource(Software software) {
        this.software = software;
    }

    public Long getAnnotationSrcId() {
        return annotationSrcId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Set<BioEntityType> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    public void addBioEntityType(BioEntityType type) {
        types.add(type);
    }

    public boolean removeBioEntityType(BioEntityType type) {
        return types.remove(type);
    }


    public Software getSoftware() {
        return software;
    }

    public void setLoadDate(Date loadDate) {
        this.loadDate = copyOf(loadDate);
    }

    @Override
    public String toString() {
        return "AnnotationSource{" +
                "annotationSrcId=" + annotationSrcId +
                ", software=" + software +
                ", types=" + types +
                ", loadDate=" + loadDate +
                '}';
    }

    public boolean isApplied() {
        return isApplied;
    }

    public void setApplied(boolean applied) {
        isApplied = applied;
    }

    public abstract AnnotationSource createCopyForNewSoftware(Software newSoftware);

    public abstract AnnotationSourceConnection createConnection();

    public abstract Collection<String> findInvalidProperties();

    public Set<AnnotatedBioEntityProperty> getAnnotatedBioEntityProperties() {
        return Collections.unmodifiableSet(annotatedBioEntityProperties);
    }

    public Set<String> getBioMartPropertyNames() {
        Set<String> answer = new HashSet<String>(annotatedBioEntityProperties.size());
        for (AnnotatedBioEntityProperty annotatedBioEntityProperty : annotatedBioEntityProperties) {
            answer.add(annotatedBioEntityProperty.getName());
        }
        return answer;
    }

    public void addBioMartProperty(String biomartPropertyName, BioEntityProperty bioEntityProperty) {
        AnnotatedBioEntityProperty annotatedBioEntityProperty = new AnnotatedBioEntityProperty(biomartPropertyName, bioEntityProperty, this);
        this.annotatedBioEntityProperties.add(annotatedBioEntityProperty);
    }

    public void addBioMartProperty(AnnotatedBioEntityProperty annotatedBioEntityProperty) {
        annotatedBioEntityProperty.setAnnotationSrc(this);
        this.annotatedBioEntityProperties.add(annotatedBioEntityProperty);
    }

    public boolean removeBioMartProperty(AnnotatedBioEntityProperty propertyAnnotated) {
        return annotatedBioEntityProperties.remove(propertyAnnotated);
    }

    public Set<AnnotatedArrayDesign> getAnnotatedArrayDesigns() {
        return annotatedArrayDesigns;
    }

    public void addBioMartArrayDesign(AnnotatedArrayDesign annotatedArrayDesign) {
        annotatedArrayDesign.setAnnotationSrc(this);
        this.annotatedArrayDesigns.add(annotatedArrayDesign);
    }

    public boolean removeBioMartArrayDesign(AnnotatedArrayDesign annotatedArrayDesign) {
        return annotatedArrayDesigns.remove(annotatedArrayDesign);
    }

    public Set<String> getBioMartArrayDesignNames() {
        Set<String> answer = newHashSet();
        for (AnnotatedArrayDesign annotatedArrayDesign : annotatedArrayDesigns) {
            answer.add(annotatedArrayDesign.getName());
        }
        return answer;
    }
}
