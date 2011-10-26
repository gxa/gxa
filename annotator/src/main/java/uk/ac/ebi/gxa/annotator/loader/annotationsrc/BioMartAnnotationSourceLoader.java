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

package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnection;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnectionFactory;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartArrayDesign;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartProperty;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.*;
import java.util.*;

import static com.google.common.collect.Sets.difference;
import static com.google.common.io.Closeables.closeQuietly;

@Service
public class BioMartAnnotationSourceLoader extends AnnotationSourceLoader<BioMartAnnotationSource>{

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ORGANISM_PROPNAME = "organism";
    private static final String SOFTWARE_NAME_PROPNAME = "software.name";
    private static final String SOFTWARE_VERSION_PROPNAME = "software.version";
    private static final String TYPES_PROPNAME = "types";
    private static final String URL_PROPNAME = "url";
    private static final String MYSQLDBNAME_PROPNAME = "mySqlDbName";
    private static final String MYSQLDBURL_PROPNAME = "mySqlDbUrl";
    private static final String DATASET_NAME_PROPNAME = "datasetName";
    private static final String DATABASE_NAME_PROPNAME = "databaseName";
    private static final String BIOMARTPROPERTY_PROPNAME = "biomartProperty";
    private static final String ARRAYDESIGN_PROPNAME = "arrayDesign";



    @Override
    public String getAnnSrcAsStringById(String id) {
        Long aLong = Long.parseLong(id);
        BioMartAnnotationSource annotationSource = annSrcDAO.getById(aLong, BioMartAnnotationSource.class);

        if (annotationSource == null) {
            return "";
        }

        Writer writer = new StringWriter();
        try {
            writeSource(annotationSource, writer);
            return writer.toString();
        } catch (ConfigurationException e) {
            throw LogUtil.createUnexpected("Cannot write Annotation Source " + annotationSource.getAnnotationSrcId(), e);
        } finally {
            closeQuietly(writer);
        }
    }

    @Override
    @Transactional
    public void saveAnnSrc(String text) throws AnnotationLoaderException {
        Reader reader = new StringReader(text);
        BioMartAnnotationSource annotationSource = readSource(reader);
        annSrcDAO.save(annotationSource);
    }

    protected BioMartAnnotationSource readSource(Reader input) throws AnnotationLoaderException {
        Properties properties = new Properties();
        BioMartAnnotationSource annotationSource = null;
        try {
            properties.load(input);
            Organism organism = organismDAO.getOrCreateOrganism(getProperty(ORGANISM_PROPNAME, properties));
            Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

            annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
            if (annotationSource == null) {
                annotationSource = new BioMartAnnotationSource(software, organism);
            }

            updateTypes(properties, annotationSource);

            annotationSource.setUrl(getProperty(URL_PROPNAME, properties));
            annotationSource.setDatabaseName(getProperty(DATABASE_NAME_PROPNAME, properties));
            annotationSource.setDatasetName(getProperty(DATASET_NAME_PROPNAME, properties));
            annotationSource.setMySqlDbName(getProperty(MYSQLDBNAME_PROPNAME, properties));
            annotationSource.setMySqlDbUrl(getProperty(MYSQLDBURL_PROPNAME, properties));

            updateBioMartProperties(properties, annotationSource);

            updateBioMartArrayDesigns(properties, annotationSource);

        } catch (IOException e) {
            throw new AnnotationLoaderException("Cannot read annotation properties", e);
        }

        return annotationSource;
    }

    protected void writeSource(BioMartAnnotationSource annSrc, Writer out) throws ConfigurationException {
        PropertiesConfiguration properties = new PropertiesConfiguration();
        properties.addProperty(ORGANISM_PROPNAME, annSrc.getOrganism().getName());
        properties.addProperty(SOFTWARE_NAME_PROPNAME, annSrc.getSoftware().getName());
        properties.addProperty(SOFTWARE_VERSION_PROPNAME, annSrc.getSoftware().getVersion());
        properties.addProperty(URL_PROPNAME, annSrc.getUrl());
        properties.addProperty(DATABASE_NAME_PROPNAME, annSrc.getDatabaseName());
        properties.addProperty(DATASET_NAME_PROPNAME, annSrc.getDatasetName());
        properties.addProperty(MYSQLDBNAME_PROPNAME, annSrc.getMySqlDbName());
        properties.addProperty(MYSQLDBURL_PROPNAME, annSrc.getMySqlDbUrl());

        //Write bioentity types
        StringBuffer types = new StringBuffer();
        int count = 1;
        for (BioEntityType type : annSrc.getTypes()) {
            types.append(type.getName());
            if (count++ < annSrc.getTypes().size()) {
                types.append(",");
            }
        }
        properties.addProperty(TYPES_PROPNAME, types.toString());

        writeBioMartProperties(annSrc, properties);

        writeBioMartArrayDesign(annSrc, properties);
        properties.save(out);
    }

    @Override
    @Transactional
    public Collection<BioMartAnnotationSource> getCurrentAnnotationSources() {
        final Collection<BioMartAnnotationSource> result = new HashSet<BioMartAnnotationSource>();
        final Collection<BioMartAnnotationSource> currentAnnSrcs = annSrcDAO.getAnnotationSourcesOfType(BioMartAnnotationSource.class);
        final Collection<BioMartAnnotationSource> oldSources = new HashSet<BioMartAnnotationSource>(currentAnnSrcs.size());
        for (BioMartAnnotationSource annSrc : currentAnnSrcs) {
            try {
                BioMartConnection connection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);
                String newVersion = connection.getOnlineMartVersion();

                if (annSrc.getSoftware().getVersion().equals(newVersion)) {
                    result.add(annSrc);
                } else {
                    //check if AnnotationSource exists for new version
                    Software newSoftware = softwareDAO.findOrCreate(annSrc.getSoftware().getName(), newVersion);
                    BioMartAnnotationSource newAnnSrc = annSrcDAO.findAnnotationSource(newSoftware, annSrc.getOrganism(), BioMartAnnotationSource.class);
                    //create and Save new AnnotationSource
                    if (newAnnSrc == null) {
                        newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
                        annSrcDAO.save(newAnnSrc);
                    }
                    oldSources.add(annSrc);
                    result.add(newAnnSrc);
                }
            } catch (BioMartAccessException e) {
                throw LogUtil.createUnexpected("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }
        removeAnnSrcs(oldSources);
        return result;
    }

    private void removeAnnSrcs(final Collection<BioMartAnnotationSource> annSrcs) {
        for (BioMartAnnotationSource annSrc : annSrcs) {
            annSrcDAO.remove(annSrc);
        }
    }

    private void updateBioMartProperties(Properties properties, BioMartAnnotationSource annotationSource) {
        Set<BioMartProperty> bioMartProperties = new HashSet<BioMartProperty>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(BIOMARTPROPERTY_PROPNAME)) {
                BioEntityProperty beProperty = propertyDAO.findOrCreate(propName.substring(BIOMARTPROPERTY_PROPNAME.length() + 1));
                StringTokenizer tokenizer = new StringTokenizer(properties.getProperty(propName), ",");
                while (tokenizer.hasMoreElements()) {
                    bioMartProperties.add(new BioMartProperty(tokenizer.nextToken().trim(), beProperty, annotationSource));
                }
            }
        }

        Set<BioMartProperty> removedProperties = new HashSet<BioMartProperty>(difference(annotationSource.getBioMartProperties(), bioMartProperties));
        Set<BioMartProperty> addedProperties = new HashSet<BioMartProperty>(difference(bioMartProperties, annotationSource.getBioMartProperties()));

        for (BioMartProperty removedProperty : removedProperties) {
            annotationSource.removeBioMartProperty(removedProperty);
        }

        for (BioMartProperty addedProperty : addedProperties) {
            annotationSource.addBioMartProperty(addedProperty);
        }
    }

    private void updateBioMartArrayDesigns(Properties properties, BioMartAnnotationSource annotationSource) {
        Set<BioMartArrayDesign> bioMartArrayDesigns = new HashSet<BioMartArrayDesign>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(ARRAYDESIGN_PROPNAME)) {
                ArrayDesign arrayDesign = arrayDesignService.findOrCreateArrayDesignShallow(propName.substring(ARRAYDESIGN_PROPNAME.length() + 1));
                bioMartArrayDesigns.add(new BioMartArrayDesign(null, properties.getProperty(propName).trim(), arrayDesign, annotationSource));
            }
        }

        Set<BioMartArrayDesign> removedProperties = new HashSet<BioMartArrayDesign>(difference(annotationSource.getBioMartArrayDesigns(), bioMartArrayDesigns));
        Set<BioMartArrayDesign> addedProperties = new HashSet<BioMartArrayDesign>(difference(bioMartArrayDesigns, annotationSource.getBioMartArrayDesigns()));

        for (BioMartArrayDesign removedProperty : removedProperties) {
            annotationSource.removeBioMartArrayDesign(removedProperty);
        }

        for (BioMartArrayDesign addedProperty : addedProperties) {
            annotationSource.addBioMartArrayDesign(addedProperty);
        }
    }

    private void updateTypes(Properties properties, BioMartAnnotationSource annotationSource) throws AnnotationLoaderException {
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

    private void writeBioMartProperties(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = HashMultimap.create();
        for (BioMartProperty bioMartProperty : annSrc.getBioMartProperties()) {
            bePropToBmProp.put(bioMartProperty.getBioEntityProperty().getName(), bioMartProperty.getName());
        }

        addCommaSeparatedProperties(BIOMARTPROPERTY_PROPNAME, properties, bePropToBmProp);

    }

    private void writeBioMartArrayDesign(BioMartAnnotationSource annSrc, PropertiesConfiguration properties) {
        Multimap<String, String> bePropToBmProp = HashMultimap.create();

        for (BioMartArrayDesign bioMartArrayDesign : annSrc.getBioMartArrayDesigns()) {
            bePropToBmProp.put(bioMartArrayDesign.getArrayDesign().getAccession(), bioMartArrayDesign.getName());
        }

        addCommaSeparatedProperties(ARRAYDESIGN_PROPNAME, properties, bePropToBmProp);
    }

    private void addCommaSeparatedProperties(String propNamePrefix, PropertiesConfiguration properties, Multimap<String, String> bePropToBmProp) {
        int count;
        for (String beProp : bePropToBmProp.keySet()) {
            count = 1;
            StringBuffer bmProperties = new StringBuffer();
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

    private String getProperty(String name, Properties properties) throws AnnotationLoaderException {
        String property = properties.getProperty(name);
        if (property == null) {
            throw new AnnotationLoaderException("Required property " + name + " is missing");
        }
        return property;
    }
}

