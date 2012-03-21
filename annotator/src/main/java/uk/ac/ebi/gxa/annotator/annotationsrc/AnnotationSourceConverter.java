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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.annotationsrc.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.difference;
import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.*;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
abstract class AnnotationSourceConverter<T extends AnnotationSource> {

    @Autowired
    protected AnnotationSourceDAO annSrcDAO;
    @Autowired
    protected OrganismDAO organismDAO;
    @Autowired
    protected SoftwareDAO softwareDAO;
    @Autowired
    protected BioEntityTypeDAO typeDAO;
    @Autowired
    protected BioEntityPropertyDAO propertyDAO;
    @Autowired
    protected ArrayDesignService arrayDesignService;

    public String convertToString(T annSrc) {
        if (annSrc == null) {
            return "";
        }
        AnnotationSourceProperties properties = new AnnotationSourceProperties();
        generateString(annSrc, properties);
        return properties.serializeToString();
    }


    public T editOrCreateAnnotationSource(T annSrc, String text, ValidationReportBuilder reportBuilder) throws AnnotationLoaderException {
        AnnotationSourceProperties properties = new AnnotationSourceProperties();
        properties.initFromText(text);
//        if (!annotationSourceInputValidator.isValidInputText(annSrc, properties, reportBuilder)) return null;

        if (annSrc == null) {
            annSrc = initAnnotationSource(properties);
            if (annSrcExists(annSrc)) {
                reportBuilder.addMessage("Annotation source  " + annSrc.getName() + " already exists. If you need to " +
                        "change it use Edit button");
                return null;
            }
        }
        updateAnnotationSource(properties, annSrc);
        return annSrc;

    }

    public T editAnnotationSource(T annSrc, String text, ValidationReportBuilder reportBuilder) throws AnnotationLoaderException {
        AnnotationSourceProperties properties = new AnnotationSourceProperties();
        properties.initFromText(text);
//        if (!annotationSourceInputValidator.isValidInputText(annSrc, properties, reportBuilder)) return null;

        if (annSrc == null) {
            annSrc = initAnnotationSource(properties);
            if (annSrcExists(annSrc)) {
                reportBuilder.addMessage("Annotation source  " + annSrc.getName() + " already exists. If you need to " +
                        "change it use Edit button");
                return null;
            }
        }
        updateAnnotationSource(properties, annSrc);
        return annSrc;

    }

//    protected boolean isValidInputText(T annSrc, AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) throws AnnotationLoaderException {
//
//        return annotationSourceInputValidator.isValidInputText(annSrc, properties, reportBuilder);
//    }
//
//    protected abstract void validateStableFields(T annSrc, AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder);
//
//    private void validateRequiredFields(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
//
//        annotationSourceInputValidator.validateRequiredFields(properties, reportBuilder);
//    }
//
//    private void validateTypes(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
//
//        annotationSourceInputValidator.validateTypes(properties, reportBuilder);
//    }
//
//    protected abstract Collection<String> getRequiredProperties();
//
//    private void validateURL(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
//        annotationSourceInputValidator.validateURL(properties, reportBuilder);
//    }

    protected void updateTypes(AnnotationSourceProperties properties, AnnotationSource annotationSource) throws AnnotationLoaderException {
        Set<BioEntityType> newTypes = new HashSet<BioEntityType>();

        for (String typeName : properties.getListPropertiesOfType(TYPES_PROPNAME)) {
            newTypes.add(typeDAO.findOrCreate(typeName));
        }

        Set<BioEntityType> removedTypes = new HashSet<BioEntityType>(difference(annotationSource.getTypes(), newTypes));
        for (BioEntityType removedType : removedTypes) {
            annotationSource.removeBioEntityType(removedType);
        }

        Set<BioEntityType> addedTypes = new HashSet<BioEntityType>(difference(newTypes, annotationSource.getTypes()));
        for (BioEntityType addedType : addedTypes) {
            annotationSource.addBioEntityType(addedType);
        }
    }


    protected void writeExternalProperties(T annSrc, AnnotationSourceProperties properties) {
        Multimap<String, String> bePropToBmProp = TreeMultimap.create();
        for (ExternalBioEntityProperty externalBioEntityProperty : annSrc.getExternalBioEntityProperties()) {
            bePropToBmProp.put(externalBioEntityProperty.getBioEntityProperty().getName(), externalBioEntityProperty.getName());
        }

        properties.addListPropertiesWithPrefix(EXTPROPERTY_PROPNAME, bePropToBmProp);
    }

    protected void writeExternalArrayDesign(T annSrc, AnnotationSourceProperties properties) {
        Multimap<String, String> bePropToBmProp = TreeMultimap.create();

        for (ExternalArrayDesign externalArrayDesign : annSrc.getExternalArrayDesigns()) {
            bePropToBmProp.put(externalArrayDesign.getArrayDesign().getAccession(), externalArrayDesign.getName());
        }

        properties.addListPropertiesWithPrefix(ARRAYDESIGN_PROPNAME, bePropToBmProp);
    }

    protected void updateExternalArrayDesigns(AnnotationSourceProperties properties, T annotationSource) {
        Set<ExternalArrayDesign> externalArrayDesigns = new HashSet<ExternalArrayDesign>();
        final Multimap<String, String> propertyValues = properties.getListPropertiesWithPrefix(ARRAYDESIGN_PROPNAME);
        for (String propertyName : propertyValues.keySet()) {
            ArrayDesign arrayDesign = arrayDesignService.findOrCreateArrayDesignShallow(propertyName);
            for (String value : propertyValues.get(propertyName)) {
                externalArrayDesigns.add(new ExternalArrayDesign(value.trim(), arrayDesign, annotationSource));
            }
        }

        Set<ExternalArrayDesign> removedProperties = new HashSet<ExternalArrayDesign>(difference(annotationSource.getExternalArrayDesigns(), externalArrayDesigns));
        Set<ExternalArrayDesign> addedProperties = new HashSet<ExternalArrayDesign>(difference(externalArrayDesigns, annotationSource.getExternalArrayDesigns()));

        for (ExternalArrayDesign removedProperty : removedProperties) {
            annotationSource.removeExternalArrayDesign(removedProperty);
        }

        for (ExternalArrayDesign addedProperty : addedProperties) {
            annotationSource.addExternalArrayDesign(addedProperty);
        }
    }

    protected void updateExternalProperties(AnnotationSourceProperties properties, T annotationSource) {
        Set<ExternalBioEntityProperty> externalBioEntityProperties = new HashSet<ExternalBioEntityProperty>();
        final Multimap<String, String> propertyValues = properties.getListPropertiesWithPrefix(EXTPROPERTY_PROPNAME);
        for (String propertyName : propertyValues.keySet()) {
            BioEntityProperty beProperty = propertyDAO.findOrCreate(propertyName);
            for (String value : propertyValues.get(propertyName)) {
                externalBioEntityProperties.add(new ExternalBioEntityProperty(value.trim(), beProperty, annotationSource));
            }
        }

        Set<ExternalBioEntityProperty> removedPropertyExternals = new HashSet<ExternalBioEntityProperty>(difference(annotationSource.getExternalBioEntityProperties(), externalBioEntityProperties));
        Set<ExternalBioEntityProperty> addedPropertyExternals = new HashSet<ExternalBioEntityProperty>(difference(externalBioEntityProperties, annotationSource.getExternalBioEntityProperties()));

        for (ExternalBioEntityProperty removedPropertyExternal : removedPropertyExternals) {
            annotationSource.removeExternalProperty(removedPropertyExternal);
        }

        for (ExternalBioEntityProperty addedPropertyExternal : addedPropertyExternals) {
            annotationSource.addExternalProperty(addedPropertyExternal);
        }
    }

    protected void updateAnnotationSource(AnnotationSourceProperties properties, T annotationSource) throws AnnotationLoaderException {
        updateTypes(properties, annotationSource);

        annotationSource.setUrl(properties.getProperty(URL_PROPNAME));

        updateExtraProperties(properties, annotationSource);

        updateExternalProperties(properties, annotationSource);

        updateExternalArrayDesigns(properties, annotationSource);
    }

    protected abstract void updateExtraProperties(AnnotationSourceProperties properties, T annotationSource) throws AnnotationLoaderException;

    protected void generateString(T annSrc, AnnotationSourceProperties properties) {

        properties.addProperty(SOFTWARE_NAME_PROPNAME, annSrc.getSoftware().getName());
        properties.addProperty(SOFTWARE_VERSION_PROPNAME, annSrc.getSoftware().getVersion());
        properties.addProperty(URL_PROPNAME, annSrc.getUrl());

        writeExtraProperties(annSrc, properties);

        //Write bioentity types
        properties.addListProperties(TYPES_PROPNAME, Collections2.transform(annSrc.getTypes(), new Function<BioEntityType, String>() {
            @Override
            public String apply(@Nullable BioEntityType bioEntityType) {
                assert bioEntityType != null;
                return bioEntityType.getName();
            }
        }));

        writeExternalProperties(annSrc, properties);

        writeExternalArrayDesign(annSrc, properties);
    }

    protected abstract void writeExtraProperties(T annSrc, AnnotationSourceProperties properties);

    protected abstract T initAnnotationSource(AnnotationSourceProperties properties);

    protected abstract boolean annSrcExists(T annSrc);

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public void setOrganismDAO(OrganismDAO organismDAO) {
        this.organismDAO = organismDAO;
    }

    public void setSoftwareDAO(SoftwareDAO softwareDAO) {
        this.softwareDAO = softwareDAO;
    }

    public void setTypeDAO(BioEntityTypeDAO typeDAO) {
        this.typeDAO = typeDAO;
    }

    public void setPropertyDAO(BioEntityPropertyDAO propertyDAO) {
        this.propertyDAO = propertyDAO;
    }

    public void setArrayDesignService(ArrayDesignService arrayDesignService) {
        this.arrayDesignService = arrayDesignService;
    }

}
