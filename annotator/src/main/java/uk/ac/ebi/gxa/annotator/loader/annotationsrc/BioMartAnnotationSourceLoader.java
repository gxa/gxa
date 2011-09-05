package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnection;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnectionFactory;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartArrayDesign;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import static com.google.common.collect.Sets.difference;
import static com.google.common.io.Closeables.closeQuietly;

@Service
public class BioMartAnnotationSourceLoader {

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

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    @Autowired
    private ArrayDesignService arrayDesignService;

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public String getAnnSrcAsStringById(String id) {
        Long aLong = Long.parseLong(id);
        BioMartAnnotationSource annotationSource = (BioMartAnnotationSource) annSrcDAO.getById(aLong);
        Writer writer = new CharArrayWriter();
        writeSource(annotationSource, writer);
        return writer.toString();
    }

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
            Organism organism = annSrcDAO.findOrCreateOrganism(getProperty(ORGANISM_PROPNAME, properties));
            Software software = annSrcDAO.findOrCreateSoftware(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

            annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
            if (annotationSource == null) {
                annotationSource = new BioMartAnnotationSource(null, software, organism);
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

    protected void writeSource(BioMartAnnotationSource annSrc, Writer out) {
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

        try {
            properties.save(out);

        } catch (ConfigurationException e) {
            log.error("Cannot write Annotation Source " + annSrc.getAnnotationSrcId(), e);
        } finally {
            closeQuietly(out);
        }
    }

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
                    //ToDo: Too slow - find faster solution
//                    annSrc.setApplied(annSrcDAO.isAnnSrcApplied(annSrc));
                    result.add(annSrc);
                } else {
                    //check if AnnotationSource exists for new version
                    Software newSoftware = annSrcDAO.findOrCreateSoftware(annSrc.getSoftware().getName(), newVersion);
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
                log.error("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }
        removeAnnSrcs(oldSources);
        return result;
    }

    @Transactional
    private void removeAnnSrcs(final Collection<BioMartAnnotationSource> annSrcs) {
        for (BioMartAnnotationSource annSrc : annSrcs) {
            annSrcDAO.remove(annSrc);
        }
    }

    private void updateBioMartProperties(Properties properties, BioMartAnnotationSource annotationSource) {
        Set<BioMartProperty> bioMartProperties = new HashSet<BioMartProperty>();
        for (String propName : properties.stringPropertyNames()) {

            if (propName.startsWith(BIOMARTPROPERTY_PROPNAME)) {
                BioEntityProperty beProperty = annSrcDAO.findOrCreateBEProperty(propName.substring(BIOMARTPROPERTY_PROPNAME.length() + 1));
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
            newTypes.add(annSrcDAO.findOrCreateBioEntityType(tokenizer.nextToken().trim()));
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

