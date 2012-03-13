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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.*;
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
public abstract class AnnotationSource {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annSrcSeq")
    @SequenceGenerator(name = "annSrcSeq", sequenceName = "A2_ANNOTATIONSRC_SEQ", allocationSize = 1)
    private Long annotationSrcId = null;

    @Column(name = "url")
    private String url;

    @ManyToOne()
    private Software software;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinTable(name = "A2_ANNSRC_BIOENTITYTYPE",
            joinColumns = @JoinColumn(name = "annotationsrcid", referencedColumnName = "annotationsrcid"),
            inverseJoinColumns = @JoinColumn(name = "bioentitytypeid", referencedColumnName = "bioentitytypeid"))
    private Set<BioEntityType> types = new HashSet<BioEntityType>();

    @Temporal(TemporalType.DATE)
    private Date loadDate;

    @org.hibernate.annotations.Type(type = "true_false")
    private boolean annotationsApplied = false;

    @org.hibernate.annotations.Type(type = "true_false")
    private boolean mappingsApplied = false;

    @OneToMany(targetEntity = ExternalBioEntityProperty.class
            , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<ExternalBioEntityProperty> externalBioEntityProperties = new HashSet<ExternalBioEntityProperty>();
    @OneToMany(targetEntity = ExternalArrayDesign.class
            , mappedBy = "annotationSrc"
            , cascade = {CascadeType.ALL}
            , fetch = FetchType.EAGER
            , orphanRemoval = true
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<ExternalArrayDesign> externalArrayDesigns = newHashSet();

    private String name;

    @org.hibernate.annotations.Type(type = "true_false")
    private boolean isObsolete = false;

    AnnotationSource() {
        /*used by hibernate only*/
    }

    public AnnotationSource(Software software, String name) {
        this.software = software;
        this.name = name;
    }

    public Long getAnnotationSrcId() {
        return annotationSrcId;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    /**
     * Location of biomart martservice, e.g.:
     * "http://www.ensembl.org/biomart/martservice?"
     * "http://plants.ensembl.org/biomart/martservice?"
     * or location of other annotations
     *
     * @return location
     */
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

    public boolean isAnnotationsApplied() {
        return annotationsApplied;
    }

    public void setAnnotationsApplied(boolean annotationsApplied) {
        this.annotationsApplied = annotationsApplied;
    }

    public boolean isMappingsApplied() {
        return mappingsApplied;
    }

    public void setMappingsApplied(boolean mappingsApplied) {
        this.mappingsApplied = mappingsApplied;
    }

    public boolean isObsolete() {
        return isObsolete;
    }

    public void setObsolete(boolean obsolete) {
        isObsolete = obsolete;
    }

    public Set<ExternalBioEntityProperty> getExternalBioEntityProperties() {
        return Collections.unmodifiableSet(externalBioEntityProperties);
    }

    public Set<String> getExternalPropertyNames() {
        Set<String> answer = new HashSet<String>(externalBioEntityProperties.size());
        for (ExternalBioEntityProperty externalBioEntityProperty : externalBioEntityProperties) {
            answer.add(externalBioEntityProperty.getName());
        }
        return answer;
    }

    public void addExternalProperty(String externalPropertyName, BioEntityProperty bioEntityProperty) {
        ExternalBioEntityProperty externalBioEntityProperty = new ExternalBioEntityProperty(externalPropertyName, bioEntityProperty, this);
        this.externalBioEntityProperties.add(externalBioEntityProperty);
    }

    public void addExternalProperty(ExternalBioEntityProperty externalBioEntityProperty) {
        externalBioEntityProperty.setAnnotationSrc(this);
        this.externalBioEntityProperties.add(externalBioEntityProperty);
    }

    public boolean removeExternalProperty(ExternalBioEntityProperty propertyExternal) {
        return externalBioEntityProperties.remove(propertyExternal);
    }

    public Set<ExternalArrayDesign> getExternalArrayDesigns() {
        return externalArrayDesigns;
    }

    public void addExternalArrayDesign(ExternalArrayDesign externalArrayDesign) {
        externalArrayDesign.setAnnotationSrc(this);
        this.externalArrayDesigns.add(externalArrayDesign);
    }

    public boolean removeExternalArrayDesign(ExternalArrayDesign externalArrayDesign) {
        return externalArrayDesigns.remove(externalArrayDesign);
    }

    public Set<String> getExternalArrayDesignNames() {
        Set<String> answer = newHashSet();
        for (ExternalArrayDesign externalArrayDesign : externalArrayDesigns) {
            answer.add(externalArrayDesign.getName());
        }
        return answer;
    }

    /**
     * @return a map of external identifier property name to bio-entity type;
     *         an order of map entries is fixed, as it can be used as a column set when
     *         querying bioMart service
     */
    public Map<String, BioEntityType> getExternalName2TypeMap() {
        Map<BioEntityProperty, String> map = new HashMap<BioEntityProperty, String>();
        for (ExternalBioEntityProperty extProp : externalBioEntityProperties) {
            map.put(extProp.getBioEntityProperty(), extProp.getName());
        }

        Map<String, BioEntityType> names = new LinkedHashMap<String, BioEntityType>();
        for (BioEntityType type : types) {
            BioEntityProperty beProperty = type.getIdentifierProperty();
            String name = map.get(beProperty);
            if (name == null) {
                //TODO put it in the proper annotation source validator
                throw new IllegalStateException("Annotation is no valid: no external name was found for property " + beProperty);
            }
            names.put(name, type);
        }
        return names;
    }

    public List<BioEntityProperty> getNonIdentifierProperties() {
        List<BioEntityProperty> properties = new ArrayList<BioEntityProperty>();
        Map<String, BioEntityType> identifierNames = getExternalName2TypeMap();
        for (ExternalBioEntityProperty extProp : externalBioEntityProperties) {
            if (identifierNames.containsKey(extProp.getName())) {
                continue;
            }
            properties.add(extProp.getBioEntityProperty());
        }
        return properties;
    }

    public List<ExternalBioEntityProperty> getNonIdentifierExternalProperties() {
        List<ExternalBioEntityProperty> properties = new ArrayList<ExternalBioEntityProperty>();
        Map<String, BioEntityType> identifierNames = getExternalName2TypeMap();
        for (ExternalBioEntityProperty extProp : externalBioEntityProperties) {
            if (identifierNames.containsKey(extProp.getName())) {
                continue;
            }
            properties.add(extProp);
        }
        return properties;
    }

    public Set<BioEntityProperty> getBioEntityPropertiesOfExternalProperties() {
        Set<BioEntityProperty> answer = newHashSet();
        for (ExternalBioEntityProperty externalBioEntityProperty : externalBioEntityProperties) {
            answer.add(externalBioEntityProperty.getBioEntityProperty());
        }
        return answer;
    }

    /////////////////////////
    //  Helper methods
    ////////////////////////
//    public abstract <T extends AnnotationSource> T createCopyForNewSoftware(Software newSoftware);

    protected AnnotationSource updateProperties(AnnotationSource result) {
        result.setUrl(this.url);
        for (ExternalBioEntityProperty externalBioEntityProperty : externalBioEntityProperties) {
            result.addExternalProperty(externalBioEntityProperty.getName(), externalBioEntityProperty.getBioEntityProperty());
        }
        for (ExternalArrayDesign externalArrayDesign : externalArrayDesigns) {
            result.addExternalArrayDesign(new ExternalArrayDesign(externalArrayDesign.getName(), externalArrayDesign.getArrayDesign(), result));
        }
        for (BioEntityType type : types) {
            result.addBioEntityType(type);
        }

        return result;
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

}
