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
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.*;
import java.net.URI;
import java.util.*;

import static com.google.common.collect.Sets.difference;
import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
abstract class AnnotationSourceConverter<T extends AnnotationSource> {

    protected static final String SOFTWARE_NAME_PROPNAME = "software.name";
    protected static final String SOFTWARE_VERSION_PROPNAME = "software.version";
    protected static final String TYPES_PROPNAME = "types";
    protected static final String URL_PROPNAME = "url";

    protected static final List<String> PROPNAMES = Arrays.asList(SOFTWARE_NAME_PROPNAME,
            SOFTWARE_VERSION_PROPNAME,
            TYPES_PROPNAME,
            URL_PROPNAME);

    private static final String EXTPROPERTY_PROPNAME = "property";
    private static final String ARRAYDESIGN_PROPNAME = "arrayDesign";

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

        Writer writer = new StringWriter();
        try {
            generateString(annSrc, writer);
            return writer.toString();
        } catch (ConfigurationException e) {
            throw LogUtil.createUnexpected("Cannot write Annotation Source " + annSrc.getAnnotationSrcId(), e);
        } finally {
            closeQuietly(writer);
        }
    }

    public T editOrCreateAnnotationSource(T annSrc, String text, ValidationReportBuilder reportBuilder) throws AnnotationLoaderException {
        Reader input = new StringReader(text);
        Properties properties = new Properties();
        try {
            properties.load(input);
            if (!isValidInputText(annSrc, properties, reportBuilder)) return null;

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
        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        } finally {
            closeQuietly(input);
        }
    }

    protected boolean isValidInputText(T annSrc, Properties properties, ValidationReportBuilder reportBuilder) throws AnnotationLoaderException {

        validateRequiredFields(properties, reportBuilder);
        validateURL(properties, reportBuilder);
        validateTypes(properties, reportBuilder);
        if (annSrc != null) {
            validateStableFields(annSrc, properties, reportBuilder);
        }

        return reportBuilder.isEmpty();
    }

    protected abstract void validateStableFields(T annSrc, Properties properties, ValidationReportBuilder reportBuilder);

    private void validateRequiredFields(Properties properties, ValidationReportBuilder reportBuilder) {
        List<String> propertyNames = new ArrayList<String>(getRequiredProperties());

        for (String propertyName : propertyNames) {
            final String property = getProperty(propertyName, properties);
            if (Strings.isNullOrEmpty(property)) {
                reportBuilder.addMessage("Required property " + propertyName + " is missing");
            }
        }
    }

    private void validateTypes(Properties properties, ValidationReportBuilder reportBuilder) {
        String typesString = getProperty(TYPES_PROPNAME, properties);
        if (Strings.isNullOrEmpty(typesString)) {
            reportBuilder.addMessage("Required property \"types\" is missing");
            return;
        }
        StringTokenizer tokenizer = new StringTokenizer(typesString, ",");
        String currentType = "";
        boolean valid = true;
        while (tokenizer.hasMoreElements()) {
            try {
                currentType = tokenizer.nextToken().trim();
                typeDAO.getByName(currentType);
            } catch (RecordNotFoundException e) {
                reportBuilder.addMessage("Unknown bioentity type " + currentType);
                valid = false;
            }

        }
         if (!valid) {
            final List<BioEntityType> all = typeDAO.getAll();
            reportBuilder.addMessage("Valid biorntity types are: " +
            StringUtils.collectionToCommaDelimitedString(Collections2.transform(all, new Function<BioEntityType, String>() {
                @Override
                public String apply(BioEntityType bioEntityType) {
                    return bioEntityType.getName();
                }
            })));
        }
    }

    protected abstract Collection<String> getRequiredProperties();

    private void validateURL(Properties properties, ValidationReportBuilder reportBuilder) {
        final String urlString = getProperty(URL_PROPNAME, properties);
        try {
            final URI uri = new URI(urlString);
            uri.toURL();
        } catch (Exception e) {
            reportBuilder.addMessage("Invalid software url");
        }
    }

    protected void addCommaSeparatedProperties(String propNamePrefix, PropertiesConfiguration properties, Multimap<String, String> bePropToBmProp) {
        int count;
        for (String beProp : bePropToBmProp.keySet()) {
            count = 1;
            StringBuilder bmProperties = new StringBuilder();
            Collection<String> bmPropCollection = bePropToBmProp.get(beProp);
            for (String bmProp : bmPropCollection) {
                bmProperties.append(bmProp);
                if (count++ < bmPropCollection.size()) {
                    bmProperties.append(",");
                }
            }
            properties.addProperty(propNamePrefix + "." + beProp, bmProperties.toString());
        }
    }

    protected void updateTypes(Properties properties, AnnotationSource annotationSource) throws AnnotationLoaderException {
        String typesString = getProperty(TYPES_PROPNAME, properties);
        Set<BioEntityType> newTypes = new HashSet<BioEntityType>();

        StringTokenizer tokenizer = new StringTokenizer(typesString, ",");
        while (tokenizer.hasMoreElements()) {
            newTypes.add(typeDAO.findOrCreate(tokenizer.nextToken().trim()));
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

    private Set<BioEntityType> parseTypes(Properties properties) throws RecordNotFoundException {
        String typesString = getProperty(TYPES_PROPNAME, properties);
        Set<BioEntityType> newTypes = new HashSet<BioEntityType>();

        StringTokenizer tokenizer = new StringTokenizer(typesString, ",");
        while (tokenizer.hasMoreElements()) {
            newTypes.add(typeDAO.getByName(tokenizer.nextToken().trim()));
        }
        return newTypes;
    }

    protected String getProperty(String name, Properties properties) {
        return properties.getProperty(name);
    }

    protected void writeExternalProperties(T annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = TreeMultimap.create();
        for (ExternalBioEntityProperty externalBioEntityProperty : annSrc.getExternalBioEntityProperties()) {
            bePropToBmProp.put(externalBioEntityProperty.getBioEntityProperty().getName(), externalBioEntityProperty.getName());
        }

        addCommaSeparatedProperties(EXTPROPERTY_PROPNAME, properties, bePropToBmProp);

    }

    protected void writeExternalArrayDesign(T annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = TreeMultimap.create();

        for (ExternalArrayDesign externalArrayDesign : annSrc.getExternalArrayDesigns()) {
            bePropToBmProp.put(externalArrayDesign.getArrayDesign().getAccession(), externalArrayDesign.getName());
        }

        addCommaSeparatedProperties(ARRAYDESIGN_PROPNAME, properties, bePropToBmProp);
    }

    protected void updateExternalArrayDesigns(Properties properties, T annotationSource) {
        Set<ExternalArrayDesign> externalArrayDesigns = new HashSet<ExternalArrayDesign>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(ARRAYDESIGN_PROPNAME)) {
                ArrayDesign arrayDesign = arrayDesignService.findOrCreateArrayDesignShallow(propName.substring(ARRAYDESIGN_PROPNAME.length() + 1));
                externalArrayDesigns.add(new ExternalArrayDesign(properties.getProperty(propName).trim(), arrayDesign, annotationSource));
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

    protected void updateExternalProperties(Properties properties, T annotationSource) {
        Set<ExternalBioEntityProperty> externalBioEntityProperties = new HashSet<ExternalBioEntityProperty>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(EXTPROPERTY_PROPNAME)) {
                BioEntityProperty beProperty = propertyDAO.findOrCreate(propName.substring(EXTPROPERTY_PROPNAME.length() + 1));
                StringTokenizer tokenizer = new StringTokenizer(properties.getProperty(propName), ",");
                while (tokenizer.hasMoreElements()) {
                    externalBioEntityProperties.add(new ExternalBioEntityProperty(tokenizer.nextToken().trim(), beProperty, annotationSource));
                }
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

    protected void updateAnnotationSource(Properties properties, T annotationSource) throws AnnotationLoaderException {
        updateTypes(properties, annotationSource);

        annotationSource.setUrl(getProperty(URL_PROPNAME, properties));

        updateExtraProperties(properties, annotationSource);

        updateExternalProperties(properties, annotationSource);

        updateExternalArrayDesigns(properties, annotationSource);
    }

    protected abstract void updateExtraProperties(Properties properties, T annotationSource) throws AnnotationLoaderException;

    protected void generateString(T annSrc, Writer out) throws ConfigurationException {
        PropertiesConfiguration properties = new PropertiesConfiguration();

        properties.addProperty(SOFTWARE_NAME_PROPNAME, annSrc.getSoftware().getName());
        properties.addProperty(SOFTWARE_VERSION_PROPNAME, annSrc.getSoftware().getVersion());
        properties.addProperty(URL_PROPNAME, annSrc.getUrl());

        writeExtraProperties(annSrc, properties);

        //Write bioentity types
        StringBuilder types = new StringBuilder();
        int count = 1;
        for (BioEntityType type : annSrc.getTypes()) {
            types.append(type.getName());
            if (count++ < annSrc.getTypes().size()) {
                types.append(",");
            }
        }
        properties.addProperty(TYPES_PROPNAME, types.toString());

        writeExternalProperties(annSrc, properties);

        writeExternalArrayDesign(annSrc, properties);
        properties.save(out);
    }

    protected abstract void writeExtraProperties(T annSrc, PropertiesConfiguration properties);

    protected abstract T initAnnotationSource(Properties properties);

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
