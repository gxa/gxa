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

import javax.persistence.*;

/**
 * User: nsklyar
 * Date: 23/05/2011
 */
@Entity
@Table(name= "A2_EXTERNAL_BEPROPERTY")
public class ExternalBioEntityProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bmPropSeq")
    @SequenceGenerator(name = "bmPropSeq", sequenceName = "A2_EXTERNAL_BEPROPERTY_SEQ", allocationSize = 1)
    @Column(name = "EXTBEPROPERTYID")
    private Long externalpropertyId;
    private String name;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private BioEntityProperty bioEntityProperty;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private AnnotationSource annotationSrc;

    ExternalBioEntityProperty() {
    }

    public ExternalBioEntityProperty(String propertyName, BioEntityProperty bioEntityProperty, AnnotationSource annSrc) {
        this.name = propertyName;
        this.bioEntityProperty = bioEntityProperty;
        this.annotationSrc = annSrc;
    }

    public String getName() {
        return name;
    }

    public BioEntityProperty getBioEntityProperty() {
        return bioEntityProperty;
    }

    public void setAnnotationSrc(AnnotationSource annotationSrc) {
        this.annotationSrc = annotationSrc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExternalBioEntityProperty that = (ExternalBioEntityProperty) o;

        if (annotationSrc != null ? !annotationSrc.equals(that.annotationSrc) : that.annotationSrc != null)
            return false;
        if (bioEntityProperty != null ? !bioEntityProperty.equals(that.bioEntityProperty) : that.bioEntityProperty != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (bioEntityProperty != null ? bioEntityProperty.hashCode() : 0);
        result = 31 * result + (annotationSrc != null ? annotationSrc.hashCode() : 0);
        return result;
    }
}
