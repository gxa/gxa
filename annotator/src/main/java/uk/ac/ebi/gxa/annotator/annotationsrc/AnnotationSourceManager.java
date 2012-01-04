package uk.ac.ebi.gxa.annotator.annotationsrc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.connection.AnnotationSourceAccessException;
import uk.ac.ebi.gxa.annotator.model.connection.AnnotationSourceConnection;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.HashSet;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
@Service
public class AnnotationSourceManager {

    @Autowired
    protected AnnotationSourceDAO annSrcDAO;
    @Autowired
    protected SoftwareDAO softwareDAO;

    @Autowired
    protected ConverterFactory annotationSourceConverterFactory;

    public Collection<AnnotationSource> getCurrentAnnotationSources() {
        final Collection<AnnotationSource> result = new HashSet<AnnotationSource>();
        for (AnnotationSourceType sourceType : AnnotationSourceType.values()) {
            final Collection<? extends AnnotationSource> currentAnnotationSourcesOfType = getCurrentAnnotationSourcesOfType(sourceType.getClazz());
            result.addAll(currentAnnotationSourcesOfType);
        }
        return result;
    }

    @Transactional
    public <T extends AnnotationSource> Collection<AnnotationSource> getCurrentAnnotationSourcesOfType(Class<T> type) {
        final Collection<AnnotationSource> result = new HashSet<AnnotationSource>();
        final Collection<T> currentAnnSrcs = annSrcDAO.getAnnotationSourcesOfType(type);
        final Collection<AnnotationSource> oldSources = new HashSet<AnnotationSource>(currentAnnSrcs.size());
        for (AnnotationSource annSrc : currentAnnSrcs) {
            try {
                AnnotationSourceConnection connection = annSrc.createConnection();
                String newVersion = connection.getOnlineSoftwareVersion();

                if (annSrc.getSoftware().getVersion().equals(newVersion)) {
                    result.add(annSrc);
                } else {
                    //check if AnnotationSource exists for new version
                    Software newSoftware = softwareDAO.findOrCreate(annSrc.getSoftware().getName(), newVersion);
                    AnnotationSource newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
                    annSrcDAO.save(newAnnSrc);
                    oldSources.add(annSrc);
                    result.add(newAnnSrc);
                }
            } catch (AnnotationSourceAccessException e) {
                throw LogUtil.createUnexpected("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }
        removeAnnSrcs(oldSources);
        return result;
    }

        public String getAnnSrcString(String id, AnnotationSourceType type) {
        final AnnotationSourceConverter converter = type.createConverter(annotationSourceConverterFactory);
        return converter.convertToString(id);
    }

    @Transactional
    public void saveAnnSrc(String id, AnnotationSourceType type, String text) {
        final AnnotationSourceConverter converter = type.createConverter(annotationSourceConverterFactory);
        try {
            final AnnotationSource annotationSource = converter.editOrCreateAnnotationSource(id, text);
            annSrcDAO.save(annotationSource);
        } catch (AnnotationLoaderException e) {
            throw LogUtil.createUnexpected("Cannot save Annotation Source: " + e.getMessage(), e);
        }
    }

    private <T extends AnnotationSource> void removeAnnSrcs(final Collection<T> annSrcs) {
        for (T annSrc : annSrcs) {
            annSrcDAO.remove(annSrc);
        }
    }

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public void setSoftwareDAO(SoftwareDAO softwareDAO) {
        this.softwareDAO = softwareDAO;
    }

    public void setAnnotationSourceConverterFactory(ConverterFactory annotationSourceConverterFactory) {
        this.annotationSourceConverterFactory = annotationSourceConverterFactory;
    }
}
