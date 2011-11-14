package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import com.google.common.collect.Multimap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

import static com.google.common.collect.Sets.difference;
import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
public abstract class AnnotationSourceConverter<T extends AnnotationSource> {

    protected static final String SOFTWARE_NAME_PROPNAME = "software.name";
    protected static final String SOFTWARE_VERSION_PROPNAME = "software.version";
    protected static final String TYPES_PROPNAME = "types";
    protected static final String URL_PROPNAME = "url";

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

    public String convertToString(String id) {
        final T annSrc = fetchAnnSrc(id);
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

    public abstract T editOrCreateAnnotationSource(String id, String text) throws AnnotationLoaderException;

    protected abstract Class<T> getClazz();

    protected T fetchAnnSrc(String id) {
        T annSrc = null;
        if (!StringUtils.EMPTY.equals(id)) {
            try {
                final long idL = Long.parseLong(id.trim());
                annSrc = annSrcDAO.getById(idL, getClazz());
            } catch (NumberFormatException e) {
                throw LogUtil.createUnexpected("Cannot save Annotation Source. Wrong ID ", e);
            }
        }
        return annSrc;
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

    protected String getProperty(String name, Properties properties) throws AnnotationLoaderException {
        String property = properties.getProperty(name);
        if (property == null) {
            throw new AnnotationLoaderException("Required property " + name + " is missing");
        }
        return property;
    }

    protected abstract void generateString(T annSrc, Writer out) throws ConfigurationException;
}
