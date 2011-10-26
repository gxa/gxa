package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.loader.biomart.AnnotationSourceConnectionFactory;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartConnection;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.HashSet;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
public class AnnotationSourceManager {

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


    public Collection<AnnotationSource> getCurrentAnnotationSources() {
        final Collection<AnnotationSource> result = new HashSet<AnnotationSource>();
        final Collection<BioMartAnnotationSource> currentAnnSrcs = annSrcDAO.getAnnotationSourcesOfType(BioMartAnnotationSource.class);
        final Collection<BioMartAnnotationSource> oldSources = new HashSet<BioMartAnnotationSource>(currentAnnSrcs.size());
        for (BioMartAnnotationSource annSrc : currentAnnSrcs) {
            try {
                BioMartConnection connection = AnnotationSourceConnectionFactory.createConnectionForAnnSrc(annSrc);
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

    public <T extends AnnotationSource> Collection<AnnotationSource> getCurrentAnnotationSourcesOfType(Class<T> type) {
        final Collection<AnnotationSource> result = new HashSet<AnnotationSource>();
        final Collection<T> currentAnnSrcs = annSrcDAO.getAnnotationSourcesOfType(type);
        final Collection<T> oldSources = new HashSet<T>(currentAnnSrcs.size());
        for (T annSrc : currentAnnSrcs) {
            try {
                BioMartConnection connection = AnnotationSourceConnectionFactory.createConnectionForAnnSrc((BioMartAnnotationSource) annSrc);
                String newVersion = connection.getOnlineMartVersion();

                if (annSrc.getSoftware().getVersion().equals(newVersion)) {
                    result.add(annSrc);
                } else {
                    //check if AnnotationSource exists for new version
                    Software newSoftware = softwareDAO.findOrCreate(annSrc.getSoftware().getName(), newVersion);
//                    BioMartAnnotationSource newAnnSrc = annSrcDAO.findAnnotationSource(newSoftware, annSrc.getOrganism(), BioMartAnnotationSource.class);
//                    //create and Save new AnnotationSource
//                    if (newAnnSrc == null) {
                        AnnotationSource newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
                        annSrcDAO.save(newAnnSrc);
//                    }
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

    private <T extends AnnotationSource> void removeAnnSrcs(final Collection<T> annSrcs) {
        for (T annSrc : annSrcs) {
            annSrcDAO.remove(annSrc);
        }
    }

    public String getAnnSrcString(String id) {
        return null;
    }

    public void saveAnnSrc(String text) {

    }
}
