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

import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static uk.ac.ebi.gxa.utils.DateUtil.copyOf;

/**
 * User: nsklyar
 * Date: 09/05/2011
 */
//@Entity
//@Table(name = "A2_ANNOTATIONSRC")
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@DiscriminatorColumn(
//        name = "annsrctype",
//        discriminatorType = DiscriminatorType.STRING
//)
 @MappedSuperclass
public abstract class AnnotationSource implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annSrcSeq")
    @SequenceGenerator(name = "annSrcSeq", sequenceName = "A2_ANNOTATIONSRC_SEQ", allocationSize = 1)
    protected Long annotationSrcId;

    @ManyToOne()
    protected Software software;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinTable(name = "A2_ANNSRC_BIOENTITYTYPE",
            joinColumns = @JoinColumn(name = "annotationsrcid", referencedColumnName = "annotationsrcid"),
            inverseJoinColumns = @JoinColumn(name = "bioentitytypeid", referencedColumnName = "bioentitytypeid"))
    protected Set<BioEntityType> types = new HashSet<BioEntityType>();

    @Temporal(TemporalType.DATE)
    protected Date loadDate;

    @org.hibernate.annotations.Type(type="true_false")
    private boolean isApplied = false;

    protected AnnotationSource() {
    }

    public AnnotationSource(Software software) {
        this.software = software;
    }

    public Long getAnnotationSrcId() {
        return annotationSrcId;
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

    public Date getLoadDate() {
        return copyOf(loadDate);
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
}
